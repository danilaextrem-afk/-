"""One agency per installation. Local CLI provisions accounts; HTTPS serves the workspace."""
import argparse
import hashlib
import json
import os
import re
import secrets
import sqlite3
import ssl
import time
import tempfile
from contextlib import closing
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

ROLES = {"admin", "manager", "agent"}
PRIVATE_KINDS = {"buyers", "deals"}
KINDS = PRIVATE_KINDS | {"properties"}
DEAL_NEXT = {
    "qualification": "showing",
    "showing": "deposit",
    "deposit": "closed",
}
DEAL_TERMINAL = {"closed", "cancelled"}
SLUG = re.compile(r"[A-Za-z0-9][A-Za-z0-9_-]{0,79}")
TOKEN = re.compile(r"[A-Za-z0-9_-]{43}")
PAGE_SIZE = 100
MAX_BODY = 8192
FIELDS = {
    "properties": {"title": 160, "address": 240, "market": 20, "price": 10**12, "area": 1_000_000, "rooms": 99},
    "buyers": {"name": 120, "district": 160, "budget": 10**12, "rooms": 99, "min_area": 1_000_000},
}


class Problem(Exception):
    def __init__(self, status, code):
        self.status, self.code = status, code


def wire_mutation(request):
    raw = request.headers.get("Content-Length", "")
    if not raw.isascii() or not raw.isdigit() or len(raw) > 5 or not 0 < int(raw) <= MAX_BODY:
        raise Problem(413, "body_size")
    if request.headers.get_content_type() != "application/json":
        raise Problem(415, "json_required")
    try:
        value = json.loads(request.rfile.read(int(raw)))
    except (ValueError, UnicodeError):
        raise Problem(400, "invalid_json") from None
    if not isinstance(value, dict):
        raise Problem(400, "invalid_json")
    return value


def normalize(kind, value):
    fields = FIELDS[kind]
    if set(value) != set(fields):
        raise Problem(400, "invalid_fields")
    name = "title" if kind == "properties" else "name"
    clean = {}
    for key, limit in fields.items():
        item = value[key]
        if key in {"title", "address", "market", "name", "district"}:
            if not isinstance(item, str) or len(item) > limit or any(ord(c) < 32 for c in item):
                raise Problem(400, "invalid_fields")
            try:
                item.encode("utf-8")
            except UnicodeError:
                raise Problem(400, "invalid_fields") from None
            clean[key] = item.strip()
        else:
            if type(item) not in {int, float} or not 0 <= item <= limit:
                raise Problem(400, "invalid_fields")
            if key in {"price", "budget", "rooms"} and type(item) is not int:
                raise Problem(400, "invalid_fields")
            clean[key] = item
    if not clean[name] or (kind == "properties" and clean["market"] not in {"primary", "secondary"}):
        raise Problem(400, "invalid_fields")
    return clean


class Office:
    def __init__(self, path):
        self.path = path
        with closing(self.connect()) as db:
            db.executescript("""
                PRAGMA journal_mode = WAL;
                CREATE TABLE IF NOT EXISTS employee(
                    id TEXT PRIMARY KEY, name TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('admin','manager','agent')), token_hash TEXT UNIQUE);
                CREATE TABLE IF NOT EXISTS record(
                    kind TEXT NOT NULL, id TEXT NOT NULL, assigned_to TEXT NOT NULL REFERENCES employee(id),
                    body TEXT NOT NULL, revision INTEGER NOT NULL, updated_at INTEGER NOT NULL,
                    deleted INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(kind, id));
                CREATE INDEX IF NOT EXISTS employee_records ON record(kind, assigned_to, id);
                CREATE INDEX IF NOT EXISTS deal_links ON record(
                    json_extract(body, '$.buyer_id'), json_extract(body, '$.property_id'))
                    WHERE kind='deals';
                CREATE TABLE IF NOT EXISTS audit(
                    seq INTEGER PRIMARY KEY, at INTEGER NOT NULL, actor TEXT NOT NULL,
                    action TEXT NOT NULL, kind TEXT NOT NULL, target TEXT NOT NULL);
            """)
            with db:
                if "deleted" not in {row["name"] for row in db.execute("PRAGMA table_info(record)")}:
                    db.execute("ALTER TABLE record ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")

    def backup(self, destination):
        target = Path(destination).absolute()
        # Write beside the destination and only publish the completed snapshot.
        fd, temporary = tempfile.mkstemp(prefix=".office-backup-", dir=target.parent)
        os.close(fd)
        try:
            with closing(self.connect()) as source, closing(sqlite3.connect(temporary)) as copy:
                source.backup(copy)
                if copy.execute("PRAGMA quick_check").fetchone()[0] != "ok":
                    raise ValueError("backup verification failed")
            with open(temporary, "rb") as file:
                os.fsync(file.fileno())
            os.link(temporary, target)
            directory = os.open(target.parent, os.O_RDONLY | os.O_DIRECTORY)
            try:
                os.fsync(directory)
            finally:
                os.close(directory)
        finally:
            os.unlink(temporary)

    def connect(self):
        db = sqlite3.connect(self.path, timeout=5)
        db.row_factory = sqlite3.Row
        db.execute("PRAGMA foreign_keys = ON")
        return db

    def issue(self, identifier, name, role):
        if not SLUG.fullmatch(identifier) or not name.strip() or len(name) > 100 or any(ord(c) < 32 for c in name) or role not in ROLES:
            raise ValueError("invalid account")
        token = secrets.token_urlsafe(32)
        with closing(self.connect()) as db, db:
            db.execute("""INSERT INTO employee VALUES (?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET name=excluded.name, role=excluded.role, token_hash=excluded.token_hash""",
                (identifier, name.strip(), role, hashlib.sha256(token.encode()).hexdigest()))
            self.audit(db, "local-cli", "issue", "employee", identifier)
        return token

    def revoke(self, identifier):
        with closing(self.connect()) as db, db:
            changed = db.execute("UPDATE employee SET token_hash=NULL WHERE id=? AND token_hash IS NOT NULL", (identifier,)).rowcount
            if changed:
                self.audit(db, "local-cli", "revoke", "employee", identifier)
            return bool(changed)

    @staticmethod
    def audit(db, actor, action, kind, target):
        db.execute("INSERT INTO audit(at,actor,action,kind,target) VALUES (?,?,?,?,?)",
                   (int(time.time()), actor, action, kind, target))

    @staticmethod
    def actor(db, header):
        prefix, _, token = header.partition(" ")
        if prefix != "Bearer" or not TOKEN.fullmatch(token):
            raise Problem(401, "unauthorized")
        encoded = hashlib.sha256(token.encode()).hexdigest()
        row = db.execute("SELECT id,name,role FROM employee WHERE token_hash=?", (encoded,)).fetchone()
        if row is None:
            raise Problem(401, "unauthorized")
        return row

    @staticmethod
    def record(db, kind, identifier):
        return db.execute("SELECT * FROM record WHERE kind=? AND id=?", (kind, identifier)).fetchone()

    @staticmethod
    def visible(row, actor, kind):
        return row and not row["deleted"] and (
            kind not in PRIVATE_KINDS or actor["role"] != "agent" or row["assigned_to"] == actor["id"])

    def serialize(self, db, row):
        data = json.loads(row["body"])
        result = {"id": row["id"], "assigned_to": row["assigned_to"], "revision": row["revision"],
                  "updated_at": row["updated_at"], "data": data}
        if row["kind"] == "deals":
            property_row = self.record(db, "properties", data["property_id"])
            buyer = self.record(db, "buyers", data["buyer_id"])
            result["property_title"] = json.loads(property_row["body"])["title"]
            result["buyer_name"] = json.loads(buyer["body"])["name"]
        return result

    def deal(self, db, actor, row, payload):
        if set(payload) != {"property_id", "buyer_id", "stage", "amount", "commission"}:
            raise Problem(400, "invalid_fields")
        property_id, buyer_id = payload["property_id"], payload["buyer_id"]
        if any(not isinstance(key, str) or not SLUG.fullmatch(key) for key in (property_id, buyer_id)):
            raise Problem(400, "invalid_fields")
        stage, amount, commission = payload["stage"], payload["amount"], payload["commission"]
        if (not isinstance(stage, str) or stage not in DEAL_NEXT.keys() | DEAL_TERMINAL or
                type(amount) is not int or type(commission) is not int or not 0 <= commission <= amount <= 10**12):
            raise Problem(400, "invalid_fields")
        if stage == "closed" and amount <= 0 or stage != "closed" and (amount or commission):
            raise Problem(400, "invalid_amount")
        if row:
            old = json.loads(row["body"])
            if (property_id, buyer_id) != (old["property_id"], old["buyer_id"]):
                raise Problem(409, "immutable_links")
            if old["stage"] not in DEAL_NEXT or stage not in (DEAL_NEXT[old["stage"]], "cancelled"):
                raise Problem(409, "invalid_stage")
            return row["assigned_to"]
        if stage != "qualification":
            raise Problem(409, "invalid_stage")
        buyer = self.record(db, "buyers", buyer_id)
        if not self.visible(self.record(db, "properties", property_id), actor, "properties") or not self.visible(buyer, actor, "buyers"):
            raise Problem(404, "deal_links_missing")
        if db.execute("""SELECT 1 FROM record WHERE kind='deals'
                         AND json_extract(body, '$.buyer_id')=? AND json_extract(body, '$.property_id')=?
                         AND json_extract(body, '$.stage') NOT IN ('closed', 'cancelled')""",
                      (buyer_id, property_id)).fetchone():
            raise Problem(409, "duplicate_deal")
        return buyer["assigned_to"]

    def list_records(self, db, actor, kind, cursor):
        if kind in PRIVATE_KINDS and actor["role"] == "agent":
            rows = db.execute("SELECT * FROM record WHERE kind=? AND assigned_to=? AND deleted=0 AND id>? ORDER BY id LIMIT ?",
                              (kind, actor["id"], cursor, PAGE_SIZE + 1)).fetchall()
        else:
            rows = db.execute("SELECT * FROM record WHERE kind=? AND deleted=0 AND id>? ORDER BY id LIMIT ?",
                              (kind, cursor, PAGE_SIZE + 1)).fetchall()
        return {"items": [self.serialize(db, row) for row in rows[:PAGE_SIZE]],
                "next": rows[PAGE_SIZE - 1]["id"] if len(rows) > PAGE_SIZE else ""}

    def mutate(self, db, actor, kind, identifier, method, payload, precondition):
        row = self.record(db, kind, identifier)
        if row and row["assigned_to"] != actor["id"] and actor["role"] == "agent":
            raise Problem(404 if kind in PRIVATE_KINDS else 403, "inaccessible")
        if row and not row["deleted"]:
            if precondition != f'"{row["revision"]}"':
                raise Problem(412, "revision_mismatch")
        elif method != "PUT":
            raise Problem(404, "not_found")
        elif precondition != "*":
            raise Problem(412, "create_precondition")
        if kind == "deals" and method != "PUT":
            raise Problem(405, "deal_history_retained")
        if method == "DELETE":
            link = "$.buyer_id" if kind == "buyers" else "$.property_id"
            if db.execute("SELECT 1 FROM record WHERE kind='deals' AND json_extract(body, ?)=?", (link, identifier)).fetchone():
                raise Problem(409, "record_linked_to_deal")
            db.execute("UPDATE record SET deleted=1, body='{}', revision=revision+1, updated_at=? WHERE kind=? AND id=?",
                       (int(time.time()), kind, identifier))
            self.audit(db, actor["id"], "delete", kind, identifier)
            return {"deleted": True}
        if method == "PATCH":
            if actor["role"] == "agent":
                raise Problem(403, "role_required")
            assignee = payload.get("assigned_to")
            if set(payload) != {"assigned_to"} or not isinstance(assignee, str) or not db.execute(
                "SELECT 1 FROM employee WHERE id=? AND token_hash IS NOT NULL", (assignee,)
            ).fetchone():
                raise Problem(400, "invalid_assignee")
            body = row["body"]
            if kind == "buyers" and assignee != row["assigned_to"]:
                affected = db.execute("""UPDATE record SET assigned_to=?, revision=revision+1, updated_at=?
                    WHERE kind='deals' AND json_extract(body, '$.buyer_id')=? RETURNING id""",
                    (assignee, int(time.time()), identifier)).fetchall()
                for deal in affected:
                    self.audit(db, actor["id"], "assign", "deals", deal["id"])
        elif kind == "deals":
            assignee = self.deal(db, actor, row, payload)
            body = json.dumps(payload)
        else:
            assignee = row["assigned_to"] if row else actor["id"]
            body = json.dumps(normalize(kind, payload), ensure_ascii=False)
        revision = (row["revision"] if row else 0) + 1
        now = int(time.time())
        db.execute("""INSERT INTO record(kind,id,assigned_to,body,revision,updated_at,deleted)
                   VALUES (?, ?, ?, ?, ?, ?, 0) ON CONFLICT(kind,id) DO UPDATE SET
                   assigned_to=excluded.assigned_to, body=excluded.body, deleted=0,
                   revision=excluded.revision, updated_at=excluded.updated_at""",
                   (kind, identifier, assignee, body, revision, now))
        self.audit(db, actor["id"], "assign" if method == "PATCH" else "save", kind, identifier)
        return self.serialize(db, self.record(db, kind, identifier))


def handler(office):
    class Handler(BaseHTTPRequestHandler):
        def setup(self):
            super().setup()
            self.connection.settimeout(8)

        def log_message(self, *_):
            pass

        def reply(self, status, data):
            body = json.dumps(data, ensure_ascii=False).encode()
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def route(self):
            path = urlsplit(self.path)
            if self.command == "GET" and path.path == "/health":
                return {"status": "ok"}
            parts = path.path.split("/")[1:]
            method = self.command
            # Read the bounded request before acquiring the SQLite write lock.
            payload = wire_mutation(self) if method in {"PUT", "PATCH"} else None
            with closing(office.connect()) as db, db:
                db.execute("BEGIN" if method == "GET" else "BEGIN IMMEDIATE")
                actor = office.actor(db, self.headers.get("Authorization", ""))
                if method == "GET" and parts == ["v1", "me"]:
                    return dict(actor)
                if method == "GET" and parts == ["v1", "employees"]:
                    if actor["role"] == "agent":
                        raise Problem(403, "role_required")
                    return {"items": [dict(row) for row in db.execute(
                        "SELECT id,name,role FROM employee WHERE token_hash IS NOT NULL ORDER BY id")]}
                if len(parts) < 2 or parts[0] != "v1" or parts[1] not in KINDS:
                    raise Problem(404, "not_found")
                kind = parts[1]
                if len(parts) == 2 and method == "GET":
                    cursor = parse_qs(path.query).get("after", [""])[0]
                    if cursor and not SLUG.fullmatch(cursor):
                        raise Problem(400, "invalid_cursor")
                    return office.list_records(db, actor, kind, cursor)
                if len(parts) != 3 or not SLUG.fullmatch(parts[2]):
                    raise Problem(404, "not_found")
                identifier = parts[2]
                if method == "GET":
                    row = office.record(db, kind, identifier)
                    if not office.visible(row, actor, kind):
                        raise Problem(404, "not_found")
                    return office.serialize(db, row)
                match = self.headers.get("If-Match", "")
                if method == "PUT" and self.headers.get("If-None-Match") == "*" and not match:
                    match = "*"
                return office.mutate(db, actor, kind, identifier, method, payload, match)

        def dispatch(self):
            try:
                self.reply(200, self.route())
            except Problem as e:
                self.reply(e.status, {"error": e.code})
            except sqlite3.Error:
                self.reply(503, {"error": "storage_unavailable"})
            except (BrokenPipeError, ConnectionResetError, TimeoutError):
                pass

        do_GET = do_PUT = do_PATCH = do_DELETE = dispatch

    return Handler


def main():
    os.umask(0o077)
    parser = argparse.ArgumentParser()
    parser.add_argument("--db", default="office.sqlite3")
    sub = parser.add_subparsers(dest="command", required=True)
    issue = sub.add_parser("issue")
    issue.add_argument("id")
    issue.add_argument("name")
    issue.add_argument("role", choices=sorted(ROLES))
    revoke = sub.add_parser("revoke")
    revoke.add_argument("id")
    backup = sub.add_parser("backup")
    backup.add_argument("destination")
    serve = sub.add_parser("serve")
    serve.add_argument("--host", default="127.0.0.1")
    serve.add_argument("--port", default=8443, type=int)
    serve.add_argument("--cert")
    serve.add_argument("--key")
    args = parser.parse_args()
    office = Office(args.db)
    if args.command == "issue":
        print(office.issue(args.id, args.name, args.role))
    elif args.command == "revoke":
        if not office.revoke(args.id):
            parser.error("active account not found")
    elif args.command == "backup":
        try:
            office.backup(args.destination)
        except FileExistsError:
            parser.error("backup destination already exists")
    else:
        if bool(args.cert) != bool(args.key) or not args.cert and args.host not in {"127.0.0.1", "localhost"}:
            parser.error("provide a TLS certificate and key or listen on loopback behind a trusted TLS proxy")
        with ThreadingHTTPServer((args.host, args.port), handler(office)) as server:
            if args.cert:
                tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
                tls.minimum_version = ssl.TLSVersion.TLSv1_2
                tls.load_cert_chain(args.cert, args.key)
                server.socket = tls.wrap_socket(server.socket, server_side=True)
            server.serve_forever()


if __name__ == "__main__":
    main()
