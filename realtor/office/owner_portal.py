"""Scoped, revocable access to the seller's property report."""
import hashlib
import json
import secrets
import time

SCHEMA = """
CREATE TABLE IF NOT EXISTS owner_access(
    property_id TEXT PRIMARY KEY, token_hash TEXT NOT NULL UNIQUE,
    expires_at INTEGER NOT NULL, issued_by TEXT NOT NULL REFERENCES employee(id));
CREATE TABLE IF NOT EXISTS owner_report(
    property_id TEXT PRIMARY KEY, views INTEGER, inquiries INTEGER,
    measured_at INTEGER, updated_at INTEGER NOT NULL, recommendation TEXT NOT NULL, revision INTEGER NOT NULL);
"""
DURATION = 30 * 86400


def digest(token):
    return hashlib.sha256(token.encode("ascii")).hexdigest()


class OwnerPortal:
    def __init__(self, problem, identifier_pattern, token_pattern):
        self.problem = problem
        self.identifier_pattern = identifier_pattern
        self.token_pattern = token_pattern

    def route(self, office, db, actor, method, parts, payload, match):
        if (len(parts) != 4 or parts[3] not in {"access", "report"}
                or not self.identifier_pattern.fullmatch(parts[2])):
            raise self.problem(404, "not_found")
        identifier, resource = parts[2:]
        property_row = office.record(db, "properties", identifier)
        if (not property_row or property_row["deleted"] or
                actor["role"] == "agent" and actor["id"] != property_row["assigned_to"]):
            raise self.problem(404, "not_found")
        if resource == "report":
            if method == "PUT":
                self.save_report(db, identifier, payload, match)
                office.audit(db, actor["id"], "save", "owner_report", identifier)
            elif method != "GET":
                raise self.problem(405, "method_not_allowed")
            return dict(self.snapshot(office, db, property_row), **self.access(db, identifier))

        if method == "GET":
            return self.access(db, identifier)
        if method == "DELETE":
            self.revoke(office, db, identifier, actor["id"], "revoke")
            return {"expires_at": None}
        if method != "PUT":
            raise self.problem(405, "method_not_allowed")
        if payload != {"owner_share_consent": True} or payload.get("owner_share_consent") is not True:
            raise self.problem(400, "owner_share_consent_required")
        token = secrets.token_urlsafe(32)
        expires = int(time.time()) + DURATION
        db.execute("""INSERT INTO owner_access VALUES (?,?,?,?)
            ON CONFLICT(property_id) DO UPDATE SET token_hash=excluded.token_hash,
            expires_at=excluded.expires_at, issued_by=excluded.issued_by""",
                   (identifier, digest(token), expires, actor["id"]))
        office.audit(db, actor["id"], "issue", "owner_access", identifier)
        return {"token": token, "expires_at": expires}

    @staticmethod
    def access(db, identifier):
        item = db.execute("SELECT expires_at FROM owner_access WHERE property_id=? AND expires_at>?",
                          (identifier, int(time.time()))).fetchone()
        return {"expires_at": item["expires_at"] if item else None}

    @staticmethod
    def revoke(office, db, identifier, actor, action):
        if db.execute("DELETE FROM owner_access WHERE property_id=?", (identifier,)).rowcount:
            office.audit(db, actor, action, "owner_access", identifier)

    def save_report(self, db, identifier, payload, match):
        if set(payload) != {"views", "inquiries", "recommendation"}:
            raise self.problem(400, "invalid_owner_report")
        views, inquiries, note = (payload[key] for key in ("views", "inquiries", "recommendation"))
        if any(v is not None and (type(v) is not int or not 0 <= v <= 10**9) for v in (views, inquiries)):
            raise self.problem(400, "invalid_owner_report")
        if not isinstance(note, str) or len(note) > 1000 or any(
                (ord(c) < 32 and c not in "\n\r\t") or 0xD800 <= ord(c) <= 0xDFFF for c in note):
            raise self.problem(400, "invalid_owner_report")
        existing = db.execute("SELECT * FROM owner_report WHERE property_id=?", (identifier,)).fetchone()
        revision = existing["revision"] if existing else 0
        if match != f'"{revision}"':
            raise self.problem(412, "revision_mismatch")
        now = int(time.time())
        measured = now if views is not None or inquiries is not None else None
        if existing and (views, inquiries) == (existing["views"], existing["inquiries"]):
            measured = existing["measured_at"]
            if note.strip() == existing["recommendation"]:
                return
        db.execute("""INSERT INTO owner_report VALUES (?,?,?,?,?,?,?)
            ON CONFLICT(property_id) DO UPDATE SET views=excluded.views, inquiries=excluded.inquiries,
            measured_at=excluded.measured_at, updated_at=excluded.updated_at,
            recommendation=excluded.recommendation, revision=excluded.revision""",
                   (identifier, views, inquiries, measured, now, note.strip(), revision + 1))

    def authenticate(self, office, db, authorization):
        prefix, _, token = authorization.partition(" ")
        if prefix != "Bearer" or not self.token_pattern.fullmatch(token):
            raise self.problem(401, "owner_access_invalid")
        row = db.execute("""SELECT r.* FROM owner_access a JOIN record r
            ON r.kind='properties' AND r.id=a.property_id AND r.deleted=0
            JOIN employee e ON e.id=a.issued_by AND e.token_hash IS NOT NULL
            WHERE a.token_hash=? AND a.expires_at>?""", (digest(token), int(time.time()))).fetchone()
        if not row:
            raise self.problem(401, "owner_access_invalid")
        return self.snapshot(office, db, row)

    @staticmethod
    def snapshot(office, db, property_row):
        identifier = property_row["id"]
        facts = json.loads(property_row["body"])
        report = db.execute("""SELECT views,inquiries,measured_at,updated_at,recommendation,revision
            FROM owner_report WHERE property_id=?""", (identifier,)).fetchone()
        advertising = office.advertising.offer(db, identifier)
        cian = office.cian.offer(db, identifier)
        sold = db.execute("""SELECT 1 FROM record WHERE kind='deals' AND deleted=0
            AND json_extract(body, '$.property_id')=? AND json_extract(body, '$.stage')='closed'""",
                          (identifier,)).fetchone()
        return {
            "property": {field: facts[field] for field in ("title", "address", "price", "area", "rooms")},
            "property_updated_at": property_row["updated_at"],
            "sold": sold is not None,
            "yandex_feed": office.advertising.status(db, advertising)["state"],
            "cian_feed": office.cian.status(db, cian)["state"],
            "report": dict(report) if report else {
                "views": None, "inquiries": None, "measured_at": None, "updated_at": None, "recommendation": "", "revision": 0,
            },
        }
