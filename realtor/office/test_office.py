import http.client
import json
import sqlite3
import tempfile
import threading
import unittest
from contextlib import closing
from http.server import ThreadingHTTPServer
from pathlib import Path

from office import Office, handler

PROPERTY = dict(title="Двухкомнатная квартира", address="Москва", market="secondary", price=14_800_000, area=51.2, rooms=2)
BUYER = dict(name="Тестовый покупатель", district="Мытищи", budget=14_800_000, rooms=2, min_area=49)


class OfficeTest(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.path = Path(directory.name) / "office.sqlite3"
        self.office = Office(self.path)
        self.one = self.office.issue("one", "Первый", "agent")
        self.two = self.office.issue("two", "Второй", "agent")
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
