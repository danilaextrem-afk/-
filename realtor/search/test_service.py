import asyncio
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from fastapi.testclient import TestClient

from service import app, search, Query


class SearchTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.env = patch.dict(os.environ, {
            "REALTOR_DB": str(Path(self.tmp.name) / "test.sqlite3"),
            "REALTOR_SEARCH_TOKEN": "test-token", "GROQ_API_KEY": "",
            "REALTOR_SOURCES": '{"cian":{"feed":"https://source.invalid/feed","format":"yrl"}}',
        })
        self.env.start()
        self.addCleanup(self.env.stop)
        self.headers = {"Authorization": "Bearer test-token"}

    def test_only_complete_live_rows_pass_filters(self):
        xml = """<realty-feed><offer><category>квартира</category><type>аренда</type>
            <url>https://www.cian.ru/rent/flat/1234567/?track=1</url>
            <location><region>Москва</region><address>ул. Тестовая, 1</address></location>
            <price><value>60000</value><currency>RUR</currency></price><area><value>40</value></area>
            <rooms>2</rooms><image>https://images.invalid/1.jpg?token=abc</image></offer></realty-feed>"""
        with patch("service.fetch", return_value=xml.encode()):
            result = asyncio.run(search(Query(text="Снять двушку в Москве до 65 тысяч")))
            self.assertEqual(1, len(result["offers"]))
            offer = result["offers"][0]
            self.assertEqual("https://www.cian.ru/rent/flat/1234567/", offer["url"])
            self.assertTrue(offer["photos"][0].endswith("token=abc"))
            self.assertEqual([], asyncio.run(search(Query(text="Снять двушку в Москве до 55 тысяч")))["offers"])
            self.assertEqual([], asyncio.run(search(Query(text="Купить двушку в Москве")))["offers"])

    def test_auth_watch_idempotence_and_unsubscribe(self):
        with TestClient(app) as client:
            self.assertEqual(401, client.post("/v1/search", json={}).status_code)
            args = {"headers": self.headers, "json": {"text": "Студия в Москве"}}
            first = client.post("/v1/watch", **args).json()["id"]
            self.assertEqual(first, client.post("/v1/watch", **args).json()["id"])
            self.assertEqual({"events": []}, client.get(f"/v1/events?watch={first}", headers=self.headers).json())
            self.assertEqual({"ok": True}, client.delete(f"/v1/watch/{first}", headers=self.headers).json())


if __name__ == "__main__":
    unittest.main()
