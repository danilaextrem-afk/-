"""Corporate placement through the Yandex Realty XML pull protocol."""
import hmac
import json
import re
import time
from datetime import datetime, timezone
from urllib.parse import urlsplit
from xml.etree.ElementTree import Element, SubElement, tostring

SCHEMA = """
CREATE TABLE IF NOT EXISTS advertising_policy(
    id INTEGER PRIMARY KEY CHECK(id=1), enabled INTEGER NOT NULL, active_limit INTEGER NOT NULL);
INSERT OR IGNORE INTO advertising_policy VALUES (1, 0, 0);
CREATE TABLE IF NOT EXISTS realty_offer(
    property_id TEXT PRIMARY KEY, data TEXT NOT NULL, facts TEXT NOT NULL,
    approved_by TEXT NOT NULL REFERENCES employee(id), approved_at INTEGER NOT NULL,
    active INTEGER NOT NULL, revision INTEGER NOT NULL);
"""
FEED_KEY = re.compile(r"[A-Za-z0-9_-]{43}")
PUBLIC_PHONE = re.compile(r"\+7[0-9]{10}")
NAMESPACE = "http://webmaster.yandex.ru/schemas/feed/realty/2010-06"


def utc(epoch):
    return datetime.fromtimestamp(epoch, timezone.utc).isoformat(timespec="seconds")


def facts(property_data):
    return [property_data[key] for key in ("address", "market", "area", "rooms")]


def clean_media_url(value):
    if not isinstance(value, str) or not 1 <= len(value) <= 512 or any(ord(c) <= 32 for c in value):
        raise ValueError()
    parsed = urlsplit(value)
    if (parsed.scheme != "https" or not parsed.hostname or parsed.username is not None
            or parsed.fragment or parsed.port not in (None, 443)):
        raise ValueError()
    return value


def approval(payload):
    if set(payload) != {"consent_reference", "media_rights", "phone", "description", "images", "floor"}:
        raise ValueError()
    result = {}
    for key, limit in (("consent_reference", 180), ("description", 3000), ("phone", 12)):
        value = payload[key]
        if not isinstance(value, str) or not 1 <= len(value.strip()) <= limit:
            raise ValueError()
        if any((ord(char) < 32 and char not in "\t\n\r") or
               0xD800 <= ord(char) <= 0xDFFF or ord(char) in (0xFFFE, 0xFFFF) for char in value):
            raise ValueError()
        result[key] = value.strip()
    if not PUBLIC_PHONE.fullmatch(result["phone"]):
        raise ValueError()
    images = payload["images"]
    if payload["media_rights"] is not True or not isinstance(images, list) or not 1 <= len(images) <= 10:
        raise ValueError()
    result["images"] = list(dict.fromkeys(map(clean_media_url, images)))
    floor = payload["floor"]
    if type(floor) is not int or not 1 <= floor <= 150:
        raise ValueError()
    result["floor"] = floor
    return result


class Advertising:
    def __init__(self, feed_key, problem):
        if feed_key and not FEED_KEY.fullmatch(feed_key):
            raise ValueError("invalid REALTOR_YANDEX_FEED_KEY")
        self.feed_key = feed_key
        self.problem = problem

    def feed_path(self, path):
        prefix = "/feeds/yandex/"
        key = path.removeprefix(prefix)
        return bool(self.feed_key and key != path and FEED_KEY.fullmatch(key) and
                    hmac.compare_digest(key, self.feed_key))

    def policy(self, db):
        current = dict(db.execute("SELECT enabled, active_limit FROM advertising_policy").fetchone())
        current["enabled"] = bool(current["enabled"])
        current["feed_configured"] = bool(self.feed_key)
        current["active_count"] = db.execute("SELECT count(*) FROM realty_offer WHERE active=1").fetchone()[0]
        return current

    def route(self, office, db, actor, method, parts, payload, match):
        if parts == ["v1", "advertising", "policy"]:
            if actor["role"] not in ("admin", "manager"):
                raise self.problem(403, "role_required")
            if method == "PUT":
                if actor["role"] != "admin":
                    raise self.problem(403, "role_required")
                if (set(payload) != {"enabled", "active_limit"} or type(payload["enabled"]) is not bool
                        or type(payload["active_limit"]) is not int or not 0 <= payload["active_limit"] <= 1000
                        or payload["enabled"] and payload["active_limit"] == 0):
                    raise self.problem(400, "invalid_advertising_policy")
                if payload["enabled"] and not self.feed_key:
                    raise self.problem(409, "feed_not_configured")
                if payload["enabled"] and payload["active_limit"] < self.policy(db)["active_count"]:
                    raise self.problem(409, "limit_below_active_count")
                db.execute("UPDATE advertising_policy SET enabled=?, active_limit=? WHERE id=1",
                           (payload["enabled"], payload["active_limit"]))
                office.audit(db, actor["id"], "policy", "advertising", "yandex")
            elif method != "GET":
                raise self.problem(405, "method_not_allowed")
            return self.policy(db)

        if len(parts) != 4 or parts[2] != "yandex":
            raise self.problem(404, "not_found")
        identifier = parts[3]
        row = office.record(db, "properties", identifier)
        if row is None or row["deleted"] or actor["role"] == "agent" and row["assigned_to"] != actor["id"]:
            raise self.problem(404, "not_found")
        current = db.execute("SELECT * FROM realty_offer WHERE property_id=?", (identifier,)).fetchone()
        if method == "GET":
            return self.status(db, current)
        if match != f'"{current["revision"] if current else 0}"':
            raise self.problem(412, "revision_mismatch")
        if method == "DELETE":
            self.withdraw(office, db, identifier, actor["id"], "withdraw")
            return self.status(db, db.execute("SELECT * FROM realty_offer WHERE property_id=?", (identifier,)).fetchone())
        if method != "PUT":
            raise self.problem(405, "method_not_allowed")
        if not self.feed_key:
            raise self.problem(409, "feed_not_configured")
        policy = self.policy(db)
        if not policy["enabled"]:
            raise self.problem(409, "advertising_disabled")
        if (not current or not current["active"]) and policy["active_count"] >= policy["active_limit"]:
            raise self.problem(409, "placement_limit")
        data = json.loads(row["body"])
        if (data["market"] != "secondary" or not data["address"] or not data["area"]
                or not data["price"] or not data["rooms"]):
            raise self.problem(409, "incomplete_secondary_property")
        if db.execute("""SELECT 1 FROM record WHERE kind='deals' AND deleted=0
            AND json_extract(body, '$.property_id')=? AND json_extract(body, '$.stage')='closed'""",
                      (identifier,)).fetchone():
            raise self.problem(409, "property_sold")
        try:
            property_revision = payload.pop("property_revision", None)
            if type(property_revision) is not int or property_revision != row["revision"]:
                raise self.problem(412, "revision_mismatch")
            approved = json.dumps(approval(payload), ensure_ascii=False)
        except (ValueError, UnicodeError):
            raise self.problem(400, "invalid_advertising_materials") from None
        if current and current["active"] and current["data"] == approved:
            return self.status(db, current)
        approved_at = current["approved_at"] if current and current["active"] else int(time.time())
        revision = (current["revision"] if current else 0) + 1
        db.execute("""INSERT INTO realty_offer VALUES (?,?,?,?,?,1,?)
            ON CONFLICT(property_id) DO UPDATE SET data=excluded.data, facts=excluded.facts,
            approved_by=excluded.approved_by, approved_at=excluded.approved_at, active=1, revision=excluded.revision""",
                   (identifier, approved, json.dumps(facts(data)), actor["id"], approved_at, revision))
        office.audit(db, actor["id"], "approve_feed", "advertising", identifier)
        return self.status(db, {"active": True, "approved_at": approved_at, "revision": revision})

    def status(self, db, offer):
        if not offer:
            state = "not_requested"
        elif not offer["active"]:
            state = "withdrawn_from_feed"
        elif not self.feed_key or not self.policy(db)["enabled"]:
            state = "feed_disabled"
        else:
            state = "in_feed"
        return {"state": state, "created_at": offer["approved_at"] if offer else None,
                "revision": offer["revision"] if offer else 0}

    @staticmethod
    def withdraw(office, db, identifier, actor, reason):
        if db.execute("UPDATE realty_offer SET active=0, revision=revision+1 WHERE property_id=? AND active=1", (identifier,)).rowcount:
            office.audit(db, actor, reason, "advertising", identifier)

    def on_property_change(self, office, db, identifier, actor, data):
        approved = db.execute("SELECT facts FROM realty_offer WHERE property_id=? AND active=1", (identifier,)).fetchone()
        if approved and (facts(data) != json.loads(approved["facts"]) or not data["price"]):
            self.withdraw(office, db, identifier, actor, "reapproval_required")

    def xml(self, db):
        root = Element("realty-feed", xmlns=NAMESPACE)
        SubElement(root, "generation-date").text = utc(int(time.time()))
        if self.policy(db)["enabled"]:
            rows = db.execute("""SELECT a.*, r.body FROM realty_offer a
                JOIN record r ON r.kind='properties' AND r.id=a.property_id
                WHERE a.active=1 AND r.deleted=0 ORDER BY a.property_id""").fetchall()
            for row in rows:
                write_offer(root, row)
        return tostring(root, encoding="utf-8", xml_declaration=True)


def write_offer(root, row):
    data, approved = json.loads(row["body"]), json.loads(row["data"])
    offer = SubElement(root, "offer", {"internal-id": row["property_id"]})
    for key, value in (("type", "продажа"), ("property-type", "жилая"), ("category", "квартира"),
                       ("creation-date", utc(row["approved_at"]))):
        SubElement(offer, key).text = value
    location = SubElement(offer, "location")
    SubElement(location, "country").text = "Россия"
    SubElement(location, "address").text = data["address"]
    seller = SubElement(offer, "sales-agent")
    SubElement(seller, "phone").text = approved["phone"]
    SubElement(seller, "category").text = "agency"
    price = SubElement(offer, "price")
    SubElement(price, "value").text = str(data["price"])
    SubElement(price, "currency").text = "RUB"
    for image in approved["images"]:
        SubElement(offer, "image").text = image
    SubElement(offer, "description").text = approved["description"]
    area = SubElement(offer, "area")
    SubElement(area, "value").text = str(data["area"])
    SubElement(area, "unit").text = "кв. м"
    SubElement(offer, "rooms").text = str(data["rooms"])
    SubElement(offer, "floor").text = str(approved["floor"])
