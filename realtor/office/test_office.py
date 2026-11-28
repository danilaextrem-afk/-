import http.client
import json
import sqlite3
import tempfile
import threading
import unittest
from unittest.mock import patch
from concurrent.futures import ThreadPoolExecutor
from contextlib import closing
from http.server import ThreadingHTTPServer
from pathlib import Path
from xml.etree import ElementTree

from advertising import NAMESPACE

from office import Office, handler

PROPERTY = dict(title="Двухкомнатная квартира", address="Москва", market="secondary", price=14_800_000, area=51.2, rooms=2)
BUYER = dict(name="Тестовый покупатель", district="Мытищи", budget=14_800_000, rooms=2, min_area=49)


class OfficeTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.path = Path(directory.name) / "office.sqlite3"
        self.feed_key = "k" * 43
        self.cian_key = "c" * 43
        self.office = Office(self.path, self.feed_key, self.cian_key)
        self.one = self.office.issue("one", "Первый", "agent")
        self.two = self.office.issue("two", "Второй", "agent")
        self.admin = self.office.issue("admin", "Администратор", "admin")
        self.manager = self.office.issue("manager", "Руководитель", "manager")
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), handler(self.office))
        self.thread = threading.Thread(target=self.server.serve_forever, kwargs={"poll_interval": 0.02}, daemon=True)
        self.thread.start()
        self.addCleanup(self.server.server_close)
        self.addCleanup(self.server.shutdown)

    def request(self, verb, path, token=None, data=None, **headers):
        if token:
            headers["Authorization"] = "Bearer " + token
        payload = None
        if data is not None:
            headers["Content-Type"] = "application/json"
            payload = json.dumps(data).encode()
        with closing(http.client.HTTPConnection("127.0.0.1", self.server.server_port, timeout=3)) as connection:
            connection.request(verb, path, body=payload, headers=headers)
            response = connection.getresponse()
            self.assertEqual("no-store", response.getheader("Cache-Control"))
            return response.status, json.loads(response.read())

    def create(self, collection, data):
        return self.request("PUT", f"/v1/{collection}/entry", self.one, data, **{"If-None-Match": "*"})

    def test_agents_see_shared_property_but_only_assigned_buyers(self):
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        self.assertEqual(200, self.create("buyers", BUYER)[0])
        self.assertEqual([], self.request("GET", "/v1/buyers", self.two)[1]["items"])
        self.assertEqual(404, self.request("GET", "/v1/buyers/entry", self.two)[0])
        self.assertEqual(404, self.request("PUT", "/v1/buyers/entry", self.two, BUYER, **{"If-Match": '"1"'})[0])
        self.assertEqual(200, self.request("GET", "/v1/properties/entry", self.two)[0])
        self.assertEqual(403, self.request("PUT", "/v1/properties/entry", self.two, PROPERTY, **{"If-Match": '"1"'})[0])
        self.assertEqual(403, self.request("GET", "/v1/employees", self.two)[0])
        self.assertEqual("manager", self.request("GET", "/v1/me", self.manager)[1]["role"])

    def test_assignment_and_conflicts_are_audited_and_durable(self):
        self.assertEqual(200, self.create("buyers", BUYER)[0])
        args = {"assigned_to": "two"}
        self.assertEqual(403, self.request("PATCH", "/v1/buyers/entry", self.one, args, **{"If-Match": '"1"'})[0])
        code, assigned = self.request("PATCH", "/v1/buyers/entry", self.manager, args, **{"If-Match": '"1"'})
        self.assertEqual((200, "two", 2), (code, assigned["assigned_to"], assigned["revision"]))
        self.assertEqual([], self.request("GET", "/v1/buyers", self.one)[1]["items"])
        self.assertEqual(412, self.request("PUT", "/v1/buyers/entry", self.two, BUYER, **{"If-Match": '"1"'})[0])
        self.assertEqual(200, self.request("PUT", "/v1/buyers/entry", self.two, BUYER, **{"If-Match": '"2"'})[0])
        with closing(Office(self.path).connect()) as db:
            self.assertEqual(3, db.execute("SELECT revision FROM record").fetchone()[0])
            self.assertEqual(["save", "assign", "save"], [r[0] for r in db.execute(
                "SELECT action FROM audit WHERE kind='buyers' ORDER BY seq")])
            self.assertNotEqual(self.one, db.execute("SELECT token_hash FROM employee WHERE id='one'").fetchone()[0])

    def test_revoke_and_validation(self):
        self.assertEqual(401, self.request("GET", "/v1/properties")[0])
        self.assertEqual(400, self.create("buyers", dict(BUYER, phone="+79991112233"))[0])
        self.assertEqual(400, self.create("properties", dict(PROPERTY, area=float("nan")))[0])
        self.assertTrue(self.office.revoke("one"))
        self.assertEqual(401, self.request("GET", "/v1/me", self.one)[0])
        new_token = self.office.issue("one", "Первый", "agent")
        self.assertNotEqual(new_token, self.one)
        self.assertEqual(200, self.request("GET", "/v1/me", new_token)[0])

    def test_deletion_cannot_reuse_prior_revision(self):
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        route = "/v1/properties/entry"
        self.assertEqual(200, self.request("DELETE", route, self.one, **{"If-Match": '"1"'})[0])
        self.assertEqual(404, self.request("GET", route, self.one)[0])
        self.assertEqual([], self.request("GET", "/v1/properties", self.one)[1]["items"])
        self.assertEqual(3, self.create("properties", PROPERTY)[1]["revision"])
        self.assertEqual(412, self.request("PUT", route, self.one, PROPERTY, **{"If-Match": '"1"'})[0])

    def test_deal_visibility_follows_buyer_and_rejects_stale_writes(self):
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        self.assertEqual(200, self.create("buyers", BUYER)[0])
        deal = dict(property_id="entry", buyer_id="entry", stage="qualification", amount=0, commission=0)
        create = {"If-None-Match": "*"}
        rev1 = {"If-Match": '"1"'}
        route = "/v1/deals/entry"
        self.assertEqual(404, self.request("PUT", route, self.two, deal, **create)[0])
        status, result = self.request("PUT", route, self.manager, deal, **create)
        self.assertEqual((200, "one", BUYER["name"], PROPERTY["title"]),
                         (status, result["assigned_to"], result["buyer_name"], result["property_title"]))
        self.assertEqual(409, self.request("PUT", "/v1/deals/another", self.one, deal, **create)[0])
        self.assertEqual([], self.request("GET", "/v1/deals", self.two)[1]["items"])
        self.assertEqual(404, self.request("GET", route, self.two)[0])
        self.assertEqual(200, self.request("PATCH", "/v1/buyers/entry", self.manager, {"assigned_to": "two"}, **rev1)[0])
        self.assertEqual(404, self.request("GET", route, self.one)[0])
        result = self.request("GET", "/v1/deals", self.two)[1]["items"][0]
        self.assertEqual(("two", 2), (result["assigned_to"], result["revision"]))
        showing = dict(deal, stage="showing")
        self.assertEqual(404, self.request("PUT", route, self.one, showing, **rev1)[0])
        self.assertEqual(412, self.request("PUT", route, self.two, showing, **rev1)[0])
        self.assertEqual(200, self.request("PUT", route, self.two, showing, **{"If-Match": '"2"'})[0])
        with closing(self.office.connect()) as db:
            self.assertEqual(["save", "assign", "save"], [r[0] for r in db.execute(
                "SELECT action FROM audit WHERE kind='deals' ORDER BY seq")])

    def test_deal_lifecycle_validates_price_and_retains_history(self):
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        self.assertEqual(200, self.create("buyers", BUYER)[0])
        deal = dict(property_id="entry", buyer_id="entry", stage="qualification", amount=0, commission=0)
        create = {"If-None-Match": "*"}
        route = "/v1/deals/entry"
        self.assertEqual(409, self.request("PUT", route, self.one, dict(deal, stage="deposit"), **create)[0])
        self.assertEqual(200, self.request("PUT", route, self.one, deal, **create)[0])
        self.assertEqual(409, self.request("DELETE", "/v1/properties/entry", self.one, **{"If-Match": '"1"'})[0])
        self.assertEqual(409, self.request("DELETE", "/v1/buyers/entry", self.one, **{"If-Match": '"1"'})[0])
        self.assertEqual(409, self.request("PUT", route, self.one, dict(deal, stage="deposit"), **{"If-Match": '"1"'})[0])
        self.assertEqual(409, self.request("PUT", route, self.one, dict(deal, stage="showing", buyer_id="another"), **{"If-Match": '"1"'})[0])
        for revision, stage in enumerate(("showing", "deposit"), start=1):
            self.assertEqual(200, self.request("PUT", route, self.one, dict(deal, stage=stage), **{"If-Match": f'"{revision}"'})[0])
        closed = dict(deal, stage="closed", amount=14_500_000, commission=250_000)
        self.assertEqual(400, self.request("PUT", route, self.one, dict(closed, amount=200_000), **{"If-Match": '"3"'})[0])
        self.assertEqual(400, self.request("PUT", route, self.one, dict(closed, amount=0, commission=0), **{"If-Match": '"3"'})[0])
        self.assertEqual(closed, self.request("PUT", route, self.one, closed, **{"If-Match": '"3"'})[1]["data"])
        self.assertEqual(409, self.request("PUT", route, self.one, dict(deal, stage="cancelled"), **{"If-Match": '"4"'})[0])
        self.assertEqual(405, self.request("DELETE", route, self.manager, **{"If-Match": '"4"'})[0])

    def yandex(self, expected=200, key=None):
        with closing(http.client.HTTPConnection("127.0.0.1", self.server.server_port, timeout=3)) as connection:
            connection.request("GET", "/feeds/yandex/" + (self.feed_key if key is None else key))
            response = connection.getresponse()
            self.assertEqual(expected, response.status)
            self.assertEqual("no-store", response.getheader("Cache-Control"))
            body = response.read()
        if expected == 200:
            return ElementTree.fromstring(body).findall(f"{{{NAMESPACE}}}offer")
        return body

    @staticmethod
    def materials():
        return dict(consent_reference="Agreement 42", description="Прямая продажа & готовность к показу",
                    phone="+74951234567", images=["https://example.test/photo.jpg?a=b&c=d"],
                    floor=3, media_rights=True, property_revision=1)

    def test_approved_yandex_lifecycle_and_access(self):
        policy = "/v1/advertising/policy"
        offer = "/v1/advertising/yandex/entry"
        with_revision = lambda revision: {"If-Match": f'"{revision}"'}
        self.assertEqual(404, self.request("GET", offer, self.one)[0])
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        self.assertEqual(403, self.request("GET", policy, self.one)[0])
        self.assertEqual(403, self.request("PUT", policy, self.manager, dict(enabled=True, active_limit=1))[0])
        self.assertEqual(404, self.request("GET", offer, self.two)[0])
        self.assertEqual(409, self.request("PUT", offer, self.one, self.materials(), **with_revision(0))[0])
        self.assertEqual(200, self.request("PUT", policy, self.admin, dict(enabled=True, active_limit=1))[0])
        self.assertEqual(400, self.request("PUT", offer, self.one,
            dict(self.materials(), media_rights=False), **with_revision(0))[0])
        self.assertEqual(400, self.request("PUT", offer, self.one,
            dict(self.materials(), images=["file:///private"]), **with_revision(0))[0])
        self.assertEqual(412, self.request("PUT", offer, self.one,
            dict(self.materials(), property_revision=41), **with_revision(0))[0])
        status, approved = self.request("PUT", offer, self.one, self.materials(), **with_revision(0))
        self.assertEqual((200, "in_feed", 1), (status, approved["state"], approved["revision"]))
        self.assertEqual(412, self.request("PUT", offer, self.one, self.materials(), **with_revision(0))[0])
        self.assertEqual(200, self.request("PUT", offer, self.one, self.materials(), **with_revision(1))[0])
        self.yandex(404, key="v" * 43)
        record, = self.yandex()
        self.assertEqual("entry", record.attrib["internal-id"])
        self.assertEqual("14800000", record.findtext("r:price/r:value", namespaces={"r": NAMESPACE}))
        self.assertEqual(self.materials()["description"], record.findtext("r:description", namespaces={"r": NAMESPACE}))
        self.assertNotIn("Agreement 42", ElementTree.tostring(record, encoding="unicode"))
        self.assertEqual(200, self.request("PUT", "/v1/properties/second", self.one, PROPERTY, **{"If-None-Match": "*"})[0])
        self.assertEqual((409, {"error": "placement_limit"}), self.request("PUT", "/v1/advertising/yandex/second",
            self.one, self.materials(), **with_revision(0)))
        self.assertEqual(200, self.request("PUT", "/v1/properties/entry", self.one, dict(PROPERTY, price=14_000_000),
            **with_revision(1))[0])
        updated, = self.yandex()
        self.assertEqual("14000000", updated.findtext("r:price/r:value", namespaces={"r": NAMESPACE}))
        self.assertEqual(record.attrib, updated.attrib)
        self.assertEqual(200, self.create("buyers", BUYER)[0])
        deal = dict(property_id="entry", buyer_id="entry", stage="qualification", amount=0, commission=0)
        route = "/v1/deals/yandex"
        self.assertEqual(200, self.request("PUT", route, self.one, deal, **{"If-None-Match": "*"})[0])
        for revision, stage in enumerate(("showing", "deposit", "closed"), start=1):
            data = dict(deal, stage=stage, amount=14_000_000 if stage == "closed" else 0)
            self.assertEqual(200, self.request("PUT", route, self.one, data, **with_revision(revision))[0])
        self.assertEqual([], self.yandex())
        current = self.request("GET", offer, self.one)[1]
        self.assertEqual("withdrawn_from_feed", current["state"])
        self.assertEqual((409, {"error": "property_sold"}), self.request("PUT", offer, self.one,
            dict(self.materials(), property_revision=2), **with_revision(current["revision"])))

    def test_yandex_policy_pause_backup_and_reapproval(self):
        policy = "/v1/advertising/policy"
        offer = "/v1/advertising/yandex/entry"
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        self.assertEqual(200, self.request("PUT", policy, self.admin, dict(enabled=True, active_limit=2))[0])
        self.assertEqual(200, self.request("PUT", offer, self.one, self.materials(), **{"If-Match": '"0"'})[0])
        self.assertEqual(200, self.request("PUT", policy, self.admin, dict(enabled=False, active_limit=0))[0])
        self.assertEqual([], self.yandex())
        self.assertEqual("feed_disabled", self.request("GET", offer, self.one)[1]["state"])
        self.assertEqual(200, self.request("PUT", policy, self.admin, dict(enabled=True, active_limit=1))[0])
        self.assertEqual(1, len(self.yandex()))
        snapshot = self.path.with_name("with-feed.sqlite3")
        self.office.backup(snapshot)
        restored = Office(snapshot, self.feed_key)
        with closing(restored.connect()) as db:
            self.assertIn(b"14800000", restored.advertising.xml(db))
        self.assertEqual(200, self.request("PUT", "/v1/properties/entry", self.one,
            dict(PROPERTY, address="Другой адрес"), **{"If-Match": '"1"'})[0])
        self.assertEqual([], self.yandex())
        self.assertEqual(412, self.request("PUT", offer, self.one, self.materials(), **{"If-Match": '"1"'})[0])

    def cian_xml(self, key, status=200):
        with closing(http.client.HTTPConnection("127.0.0.1", self.server.server_port, timeout=3)) as connection:
            connection.request("GET", "/feeds/cian/" + key)
            response = connection.getresponse()
            self.assertEqual(status, response.status)
            self.assertEqual("no-store", response.getheader("Cache-Control"))
            body = response.read()
        return ElementTree.fromstring(body) if status == 200 else None

    def test_cian_independent_policy_consent_and_lifecycle(self):
        policy = "/v1/advertising/policy/cian"
        offer = "/v1/advertising/cian/entry"
        zero = {"If-Match": '"0"'}
        one = {"If-Match": '"1"'}
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        self.assertEqual(200, self.create("buyers", BUYER)[0])
        self.assertEqual(403, self.request("PUT", policy, self.manager, dict(enabled=True, active_limit=1))[0])
        self.assertEqual(200, self.request("PUT", policy, self.admin, dict(enabled=True, active_limit=1))[0])
        self.assertFalse(self.request("GET", "/v1/advertising/policy", self.manager)[1]["enabled"])
        self.cian_xml(self.feed_key, 404)
        self.assertEqual([], self.cian_xml(self.cian_key).findall("object"))
        self.assertEqual(400, self.request("PUT", offer, self.one, self.materials(), **zero)[0])
        approved = dict(self.materials(), description="Прямая продажа, квартира готова к показам.", floors_total=10)
        self.assertEqual(400, self.request("PUT", offer, self.one, dict(approved, floors_total=2), **zero)[0])
        self.assertEqual(400, self.request("PUT", offer, self.one, dict(approved, description="Кратко"), **zero)[0])
        self.assertEqual(404, self.request("PUT", offer, self.two, approved, **zero)[0])
        self.assertEqual("in_feed", self.request("PUT", offer, self.one, approved, **zero)[1]["state"])
        self.assertEqual(412, self.request("PUT", offer, self.one, approved, **zero)[0])
        self.assertEqual([], self.yandex())
        root = self.cian_xml(self.cian_key)
        self.assertEqual("2", root.findtext("feed_version"))
        apartment, = root.findall("object")
        self.assertEqual("flatSale", apartment.findtext("Category"))
        self.assertEqual("10", apartment.findtext("Building/FloorsCount"))
        self.assertEqual("14800000", apartment.findtext("BargainTerms/Price"))
        self.assertEqual("noPromotion", apartment.findtext("PublishTerms/Terms/PublishTermSchema/PromotionType"))
        self.assertEqual(approved["images"][0], apartment.findtext("Photos/PhotoSchema/FullUrl"))
        fmt = '"{}"'.format
        deal = dict(property_id="entry", buyer_id="entry", stage="qualification", amount=0, commission=0)
        route = "/v1/deals/cian"
        self.assertEqual(200, self.request("PUT", route, self.one, deal, **{"If-None-Match": "*"})[0])
        self.assertEqual(200, self.request("PUT", "/v1/properties/entry", self.one,
            dict(PROPERTY, price=14_500_000), **one)[0])
        updated, = self.cian_xml(self.cian_key).findall("object")
        self.assertEqual("14500000", updated.findtext("BargainTerms/Price"))
        self.assertEqual(apartment.findtext("ExternalId"), updated.findtext("ExternalId"))
        for revision, stage in enumerate(("showing", "deposit", "closed"), start=1):
            entry = dict(deal, stage=stage, amount=14_500_000 if stage == "closed" else 0)
            self.assertEqual(200, self.request("PUT", route, self.one, entry, **{"If-Match": fmt(revision)})[0])
        self.assertEqual([], self.cian_xml(self.cian_key).findall("object"))
        self.assertEqual("withdrawn_from_feed", self.request("GET", offer, self.one)[1]["state"])

    def promotion_fixture(self, limit=100_000):
        budget = "/v1/promotion-budget"
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        for channel, policy in (("yandex", "/v1/advertising/policy"), ("cian", "/v1/advertising/policy/cian")):
            self.assertEqual(200, self.request("PUT", policy, self.admin, dict(enabled=True, active_limit=1))[0])
            materials = self.materials()
            if channel == "cian":
                materials.update(description="Квартира готова к просмотрам.", floors_total=9)
            self.assertEqual(200, self.request("PUT", f"/v1/advertising/{channel}/entry", self.one,
                materials, **{"If-Match": '"0"'})[0])
        self.assertEqual(200, self.request("PUT", budget, self.admin, dict(limit_minor=limit),
            **{"If-Match": '"0"'})[0])
        return budget

    def test_budget_roles_receipts_and_accounting(self):
        budget = self.promotion_fixture()
        changes = f"{budget}/reservations"
        first = changes + "/first"
        second = changes + "/second"
        reservation = dict(property_id="entry", channel="cian", amount_minor=60_000)
        self.assertEqual(403, self.request("GET", budget, self.one)[0])
        self.assertEqual(403, self.request("PUT", budget, self.manager, dict(limit_minor=200_000))[0])
        self.assertEqual(403, self.request("PUT", first + "/reserve", self.one, reservation)[0])
        _, original = self.request("PUT", first + "/reserve", self.manager, reservation)
        self.assertEqual("reserved", original["status"])
        self.assertEqual(original, self.request("PUT", first + "/reserve", self.manager, reservation)[1])
        self.assertEqual("promotion_already_reserved", self.request("PUT", second + "/reserve", self.manager,
            reservation)[1]["error"])
        self.assertEqual("reservation_id_used", self.request("PUT", first + "/reserve", self.manager,
            dict(reservation, amount_minor=40_000))[1]["error"])
        self.assertEqual("budget_below_usage", self.request("PUT", budget, self.admin, dict(limit_minor=50_000),
            **{"If-Match": '"1"'})[1]["error"])
        charge = dict(actual_minor=45_050, receipt="cian/invoice-001")
        self.assertEqual(403, self.request("PUT", first + "/settle", self.manager, charge)[0])
        settled = self.request("PUT", first + "/settle", self.admin, charge)[1]
        self.assertEqual("settled", settled["status"])
        self.assertEqual(settled, self.request("PUT", first + "/settle", self.admin, charge)[1])
        self.assertEqual("reservation_closed", self.request("PUT", first + "/release", self.admin, {})[1]["error"])
        self.assertEqual(200, self.request("PUT", second + "/reserve", self.manager,
            dict(reservation, amount_minor=50_000))[0])
        self.assertEqual("promotion_receipt_used", self.request("PUT", second + "/settle", self.admin, charge)[1]["error"])
        self.assertEqual(200, self.request("PUT", second + "/release", self.admin, {})[0])
        state = self.request("GET", budget, self.manager)[1]
        self.assertEqual((45_050, 0, 54_950), tuple(state[key] for key in
            ("spent_minor", "reserved_minor", "available_minor")))
        self.assertEqual(["released", "settled"], [entry["status"] for entry in
            self.request("GET", changes, self.admin)[1]["items"]])

    def test_concurrent_promotions_cannot_oversubscribe_budget(self):
        budget = self.promotion_fixture()
        def reserve(channel):
            return self.request("PUT", f"{budget}/reservations/{channel}/reserve", self.manager,
                dict(property_id="entry", channel=channel, amount_minor=70_000))
        with ThreadPoolExecutor(2) as pool:
            replies = list(pool.map(reserve, ("yandex", "cian")))
        self.assertEqual([200, 409], sorted(code for code, _ in replies))
        self.assertEqual("budget_exceeded", next(body["error"] for code, body in replies if code == 409))
        state = self.request("GET", budget, self.admin)[1]
        self.assertEqual((70_000, 30_000), (state["reserved_minor"], state["available_minor"]))

    def test_owner_portal_only_exposes_the_shared_property(self):
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        self.assertEqual(200, self.create("buyers", BUYER)[0])
        access = "/v1/owner-portal/entry/access"
        report = "/v1/owner-portal/entry/report"
        confirmed = dict(owner_share_consent=True)
        self.assertEqual(404, self.request("GET", report, self.two)[0])
        self.assertEqual(404, self.request("PUT", access, self.two, confirmed)[0])
        self.assertEqual(400, self.request("PUT", access, self.one, dict(owner_share_consent=1))[0])
        self.assertEqual(401, self.request("GET", "/owner/report", self.one)[0])
        self.assertEqual(401, self.request("GET", "/owner/report")[0])
        token = self.request("PUT", access, self.one, confirmed)[1]["token"]
        self.assertEqual(401, self.request("GET", "/v1/buyers", token)[0])
        self.assertEqual(401, self.request("GET", report, token)[0])
        portal = self.request("GET", "/owner/report", token)[1]
        self.assertEqual(PROPERTY["title"], portal["property"]["title"])
        self.assertIsNone(portal["report"]["views"])
        self.assertIsNone(portal["report"]["inquiries"])
        self.assertNotIn(BUYER["name"], json.dumps(portal, ensure_ascii=False))
        self.assertNotIn("assigned_to", portal)
        with closing(self.office.connect()) as db:
            stored = db.execute("SELECT token_hash FROM owner_access").fetchone()[0]
            self.assertNotEqual(token, stored)
        metrics = dict(views=0, inquiries=None, recommendation="Обновить фотографии")
        self.assertEqual(412, self.request("PUT", report, self.one, metrics)[0])
        self.assertEqual(400, self.request("PUT", report, self.one, dict(metrics, views=-1),
                                         **{"If-Match": '"0"'})[0])
        self.assertEqual(200, self.request("PUT", report, self.one, metrics, **{"If-Match": '"0"'})[0])
        owner_report = self.request("GET", "/owner/report", token)[1]["report"]
        self.assertEqual(0, owner_report["views"])
        self.assertIsNone(owner_report["inquiries"])
        self.assertEqual(metrics["recommendation"], owner_report["recommendation"])
        self.assertEqual(412, self.request("PUT", report, self.manager, metrics, **{"If-Match": '"0"'})[0])
        self.assertEqual(200, self.request("PUT", "/v1/properties/entry", self.one,
            dict(PROPERTY, price=14_200_000), **{"If-Match": '"1"'})[0])
        self.assertEqual(14_200_000, self.request("GET", "/owner/report", token)[1]["property"]["price"])

    def test_owner_link_expires_rotates_and_revokes_on_reassignment(self):
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        access = "/v1/owner-portal/entry/access"
        confirmed = dict(owner_share_consent=True)
        first = self.request("PUT", access, self.one, confirmed)[1]
        with patch("owner_portal.time.time", return_value=first["expires_at"]):
            self.assertEqual(401, self.request("GET", "/owner/report", first["token"])[0])
            self.assertIsNone(self.request("GET", access, self.one)[1]["expires_at"])
        second = self.request("PUT", access, self.one, confirmed)[1]["token"]
        self.assertEqual(401, self.request("GET", "/owner/report", first["token"])[0])
        self.assertEqual(200, self.request("GET", "/owner/report", second)[0])
        self.assertEqual(200, self.request("DELETE", access, self.one)[0])
        self.assertEqual(401, self.request("GET", "/owner/report", second)[0])
        third = self.request("PUT", access, self.one, confirmed)[1]["token"]
        self.assertEqual(200, self.request("PATCH", "/v1/properties/entry", self.manager,
                         dict(assigned_to="two"), **{"If-Match": '"1"'})[0])
        self.assertEqual(401, self.request("GET", "/owner/report", third)[0])
        self.assertEqual(404, self.request("GET", access, self.one)[0])
        fourth = self.request("PUT", access, self.two, confirmed)[1]["token"]
        self.assertTrue(self.office.revoke("two"))
        self.office.issue("two", "Второй", "agent")
        self.assertEqual(401, self.request("GET", "/owner/report", fourth)[0])

    def test_online_backup_preserves_data_and_refuses_overwrite(self):
        self.assertEqual(200, self.create("properties", PROPERTY)[0])
        snapshot = self.path.with_name("snapshot.sqlite3")
        self.office.backup(snapshot)
        with self.assertRaises(FileExistsError):
            self.office.backup(snapshot)
        restored = Office(snapshot)
        with closing(restored.connect()) as db:
            actor = restored.actor(db, "Bearer " + self.one)
            self.assertEqual("one", actor["id"])
            self.assertEqual(PROPERTY, restored.list_records(db, actor, "properties", "")["items"][0]["data"])


if __name__ == "__main__":
    unittest.main()
