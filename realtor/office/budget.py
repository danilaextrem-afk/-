"""Office promotion budget and auditable reconciliation of external receipts."""
import re
import time

SCHEMA = """
CREATE TABLE IF NOT EXISTS promotion_budget(
    id INTEGER PRIMARY KEY CHECK(id=1),
    limit_minor INTEGER NOT NULL CHECK(limit_minor>=0), revision INTEGER NOT NULL);
INSERT OR IGNORE INTO promotion_budget VALUES (1, 0, 0);
CREATE TABLE IF NOT EXISTS promotion_reservation(
    id TEXT PRIMARY KEY, property_id TEXT NOT NULL, property_title TEXT NOT NULL,
    channel TEXT NOT NULL CHECK(channel IN ('yandex','cian')),
    reserved_minor INTEGER NOT NULL CHECK(reserved_minor>0),
    actual_minor INTEGER CHECK(actual_minor>0),
    status TEXT NOT NULL CHECK(status IN ('reserved','settled','released')),
    receipt TEXT, requested_by TEXT NOT NULL REFERENCES employee(id),
    decided_by TEXT REFERENCES employee(id), created_at INTEGER NOT NULL, decided_at INTEGER,
    UNIQUE(channel, receipt));
CREATE UNIQUE INDEX IF NOT EXISTS one_open_promotion
    ON promotion_reservation(property_id, channel) WHERE status='reserved';

"""
MAX_MINOR = 10**12
RECEIPT = re.compile(r"[A-Za-z0-9][A-Za-z0-9_.:/-]{0,119}")


class PromotionBudget:
    def __init__(self, problem, slug):
        self.problem = problem
        self.slug = slug

    def amount(self, value, minimum):
        if type(value) is not int or not minimum <= value <= MAX_MINOR:
            raise self.problem(400, "invalid_budget_amount")
        return value

    @staticmethod
    def totals(db):
        policy = dict(db.execute("SELECT limit_minor,revision FROM promotion_budget").fetchone())
        usage = dict(db.execute("""
            SELECT coalesce(sum(CASE WHEN status='reserved' THEN reserved_minor END),0) AS reserved_minor,
                   coalesce(sum(CASE WHEN status='settled' THEN actual_minor END),0) AS spent_minor
            FROM promotion_reservation""").fetchone())
        return {**policy, **usage, "currency": "RUB",
                "available_minor": policy["limit_minor"] - usage["reserved_minor"] - usage["spent_minor"]}

    def route(self, office, db, actor, method, parts, payload, match, cursor):
        if actor["role"] == "agent":
            raise self.problem(403, "role_required")
        admin = actor["role"] == "admin"
        if parts == ["v1", "promotion-budget"]:
            if method == "GET":
                return self.totals(db)
            if method != "PUT":
                raise self.problem(405, "method_not_allowed")
            if not admin:
                raise self.problem(403, "role_required")
            return self.set_limit(office, db, actor["id"], payload, match)

        if parts[:3] != ["v1", "promotion-budget", "reservations"]:
            raise self.problem(404, "not_found")
        if len(parts) == 3 and method == "GET":
            if cursor and (not cursor.isascii() or not cursor.isdecimal() or len(cursor) > 18):
                raise self.problem(400, "invalid_cursor")
            rows = db.execute("""SELECT rowid AS position,* FROM promotion_reservation
                WHERE rowid<? ORDER BY rowid DESC LIMIT 51""", (int(cursor) if cursor else 2**63 - 1,)).fetchall()
            return {"items": [dict(row) for row in rows[:50]], "next": str(rows[49]["position"]) if len(rows) > 50 else ""}
        if len(parts) != 5 or not self.slug.fullmatch(parts[3]):
            raise self.problem(404, "not_found")
        if method != "PUT":
            raise self.problem(405, "method_not_allowed")
        identifier, action = parts[3:]
        row = db.execute("SELECT * FROM promotion_reservation WHERE id=?", (identifier,)).fetchone()
        if action == "reserve":
            return self.reserve(office, db, actor["id"], identifier, payload, row)
        if action not in {"settle", "release"}:
            raise self.problem(404, "not_found")
        if not admin:
            raise self.problem(403, "role_required")
        return self.decide(office, db, actor["id"], row, action, payload)

    def set_limit(self, office, db, actor, payload, match):
        if set(payload) != {"limit_minor"}:
            raise self.problem(400, "invalid_fields")
        limit = self.amount(payload["limit_minor"], 0)
        current = self.totals(db)
        if limit == current["limit_minor"]:
            return current
        if match != f'"{current["revision"]}"':
            raise self.problem(412, "revision_mismatch")
        if limit < current["reserved_minor"] + current["spent_minor"]:
            raise self.problem(409, "budget_below_usage")
        db.execute("UPDATE promotion_budget SET limit_minor=?, revision=revision+1 WHERE id=1", (limit,))
        office.audit(db, actor, "set_limit", "promotion_budget", "office")
        return self.totals(db)

    def reserve(self, office, db, actor, identifier, payload, row):
        if set(payload) != {"property_id", "channel", "amount_minor"}:
            raise self.problem(400, "invalid_fields")
        property_id, channel = payload["property_id"], payload["channel"]
        if (not isinstance(property_id, str) or not self.slug.fullmatch(property_id)
                or not isinstance(channel, str) or channel not in ("cian", "yandex")):
            raise self.problem(400, "invalid_fields")
        amount = self.amount(payload["amount_minor"], 1)
        if row:
            if (property_id, channel, amount) != (row["property_id"], row["channel"], row["reserved_minor"]):
                raise self.problem(409, "reservation_id_used")
            return dict(row)
        prop = office.record(db, "properties", property_id)
        if not prop or prop["deleted"]:
            raise self.problem(404, "not_found")
        feed = office.cian if channel == "cian" else office.advertising
        if feed.status(db, feed.offer(db, property_id))["state"] != "in_feed":
            raise self.problem(409, "promotion_offer_required")
        if db.execute("""SELECT 1 FROM promotion_reservation
            WHERE property_id=? AND channel=? AND status='reserved'""", (property_id, channel)).fetchone():
            raise self.problem(409, "promotion_already_reserved")
        if amount > self.totals(db)["available_minor"]:
            raise self.problem(409, "budget_exceeded")
        title = office.serialize(db, prop)["data"]["title"]
        result = db.execute("""INSERT INTO promotion_reservation(
            id,property_id,property_title,channel,reserved_minor,status,requested_by,created_at)
            VALUES (?,?,?,?,?,'reserved',?,?) RETURNING *""",
            (identifier, property_id, title, channel, amount, actor, int(time.time()))).fetchone()
        office.audit(db, actor, "reserve", "promotion_budget", identifier)
        return dict(result)

    def decide(self, office, db, actor, row, action, payload):
        if not row:
            raise self.problem(404, "not_found")
        if action == "settle":
            if set(payload) != {"actual_minor", "receipt"}:
                raise self.problem(400, "invalid_fields")
            amount = self.amount(payload["actual_minor"], 1)
            receipt = payload["receipt"]
            if not isinstance(receipt, str) or not RECEIPT.fullmatch(receipt):
                raise self.problem(400, "invalid_promotion_receipt")
            state = "settled"
        else:
            if payload:
                raise self.problem(400, "invalid_fields")
            amount, receipt, state = None, None, "released"
        if row["status"] != "reserved":
            if (state, amount, receipt) == (row["status"], row["actual_minor"], row["receipt"]):
                return dict(row)
            raise self.problem(409, "reservation_closed")
        if amount is not None and amount > row["reserved_minor"] + self.totals(db)["available_minor"]:
            raise self.problem(409, "budget_exceeded")
        if receipt and db.execute("SELECT 1 FROM promotion_reservation WHERE channel=? AND receipt=?",
                                  (row["channel"], receipt)).fetchone():
            raise self.problem(409, "promotion_receipt_used")
        result = db.execute("""UPDATE promotion_reservation
            SET status=?,actual_minor=?,receipt=?,decided_by=?,decided_at=? WHERE id=? RETURNING *""",
            (state, amount, receipt, actor, int(time.time()), row["id"])).fetchone()
        office.audit(db, actor, action, "promotion_budget", row["id"])
        return dict(result)
