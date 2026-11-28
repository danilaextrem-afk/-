import asyncio
import json
import os
import re
import secrets
import sqlite3
import time
from contextlib import asynccontextmanager, closing, suppress
from datetime import datetime, timezone
from html.parser import HTMLParser
from urllib.parse import urlsplit, urlunsplit, urljoin

import httpx
from defusedxml import ElementTree
from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

NAMES = {"avito": "Авито", "cian": "Циан", "domclick": "Домклик",
         "yandex": "Яндекс Недвижимость", "m2": "М2", "developer": "Застройщики"}
HOSTS = {"avito": "avito.ru", "cian": "cian.ru", "domclick": "domclick.ru",
         "yandex": "realty.yandex.ru", "m2": "m2.ru"}
PATHS = {
    "avito": r"/[^/]+/kvartiry/[^/]+_\d+/?",
    "cian": r"/(?:sale|rent)/flat/\d+/?",
    "domclick": r"/card/(?:sale|rent)/[\da-f-]{36}/?",
    "yandex": r"/offer/\d+/?",
    "m2": r"/[^/]+/nedvizhimost/[^/]+/[^/]+/?",
}
GEO = re.compile(r"\b(?:москва|московская область|московской области|подмосковье|мо)\b", re.I)
CLOSED = re.compile(r"объявление (?:снято|в архиве|закрыто)|квартира продана|объект недоступен|captcha|just a moment", re.I)
AGENT = "RealtorAI/1.0"
MAX_BODY = 8_000_000


class Query(BaseModel):
    text: str = Field(default="", max_length=400)
    place: str = Field(default="", max_length=160)
    deal: str | None = Field(default=None, pattern="^(sale|rent)$")
    market: str | None = Field(default=None, pattern="^(primary|secondary)$")
    seller: str | None = Field(default=None, pattern="^(owner|developer|agent)$")
    budget: int | None = Field(default=None, ge=1, le=10**12)
    rooms: int | None = Field(default=None, ge=0, le=20)
    minArea: float = Field(default=0, ge=0, le=100000)


def config():
    return json.loads(os.getenv("REALTOR_SOURCES", "{}"))


def db():
    conn = sqlite3.connect(os.getenv("REALTOR_DB", "realtor-search.sqlite3"))
    conn.row_factory = sqlite3.Row
    conn.executescript("""
        CREATE TABLE IF NOT EXISTS watches(id INTEGER PRIMARY KEY, query TEXT UNIQUE, due INTEGER);
        CREATE TABLE IF NOT EXISTS seen(watch INTEGER, identity TEXT, price INTEGER, PRIMARY KEY(watch,identity));
        CREATE TABLE IF NOT EXISTS events(id INTEGER PRIMARY KEY AUTOINCREMENT, watch INTEGER, kind TEXT, offer TEXT);
    """)
    return conn


def url(value, host=None, keep_query=False):
    try:
        parts = urlsplit(str(value))
        name = parts.hostname
        if (parts.scheme != "https" or not name or parts.username or parts.password
                or parts.port not in (None, 443) or not re.fullmatch(r"[a-z0-9.-]+\.[a-z]{2,}", name)
                or (host and name != host and not name.endswith("." + host))):
            return None
        return urlunsplit(("https", name, parts.path or "/", parts.query if keep_query else "", ""))
    except ValueError:
        return None


def direct(value, source, cfg):
    link = url(value, HOSTS.get(source, cfg.get("domain")))
    if not link or not re.fullmatch(PATHS.get(source, cfg.get("path", r"(?!)")), urlsplit(link).path):
        return None
    return link


def as_list(value):
    return value if isinstance(value, list) else [value]


def number(value):
    return str(value or "").replace(" ", "").replace("\xa0", "").replace(",", ".")


def normalize(row, source, cfg):
    link = direct(row.get("url"), source, cfg)
    if not link or str(row.get("status", "active")).lower() not in {"active", "available", "1", "true"}:
        return None
    expiry = row.get("expires_at")
    if expiry:
        try:
            if datetime.fromisoformat(expiry.replace("Z", "+00:00")).astimezone(timezone.utc) <= datetime.now(timezone.utc):
                return None
        except (ValueError, AttributeError):
            return None
    address = str(row.get("address") or "").strip()
    region = str(row.get("region") or "").strip()
    photos = list(dict.fromkeys(s for v in as_list(row.get("photos")) if v and (s := url(v, keep_query=True))))
    try:
        price = int(float(number(row.get("price"))))
        area = float(number(row.get("area")))
        rooms = int(number(row.get("rooms")))
    except (ValueError, OverflowError):
        return None
    deal = row.get("deal")
    market = row.get("market")
    seller = row.get("seller")
    if not (address and GEO.search(region + " " + address) and photos and 0 < price < 10**12
            and 0 < area <= 100000 and 0 <= rooms <= 20 and deal in {"sale", "rent"}
            and market in {"primary", "secondary"} and str(row.get("currency", "RUB")).upper() in {"RUB", "RUR"}):
        return None
    return dict(url=link, id=link, address=address, region=region, locality=row.get("locality") or "",
                photos=photos[:12], title=str(row.get("title") or address)[:250], price=price, area=area,
                rooms=rooms, deal=deal, market=market, seller=seller, housing_kind="квартира",
                status="active", unit=str(row.get("unit") or "").strip(), source_id=source, source=NAMES[source],
                updated_at=datetime.now(timezone.utc).isoformat())


async def fetch(client, link, **kwargs):
    async with client.stream("GET", link, **kwargs) as response:
        response.raise_for_status()
        body = bytearray()
        async for chunk in response.aiter_bytes():
            body.extend(chunk)
            if len(body) > MAX_BODY:
                raise ValueError("Ответ источника превышает 8 МБ")
        return bytes(body)


def yrl(raw):
    root = ElementTree.fromstring(raw)
    for part in root.iter():
        part.tag = part.tag.split("}")[-1]
    if root.tag != "realty-feed":
        raise ValueError("Ожидался XML в формате YRL")
    for offer in root.findall("offer"):
        get = offer.findtext
        if get("category", "").lower() != "квартира":
            continue
        address = ", ".join(dict.fromkeys(filter(None, [get("location/region"), get("location/locality-name"),
                                                        get("location/address")])))
        raw_deal = get("type", "")
        building = get("new-flat", "").lower()
        market = "primary" if building in {"1", "true", "да"} else "secondary" if building in {"0", "false", "нет"} or raw_deal == "аренда" else None
        yield dict(url=get("url"), address=address, region=get("location/region"), locality=get("location/locality-name"),
                   price=get("price/value"), currency=get("price/currency"), area=get("area/value"),
                   rooms="0" if get("studio", "").lower() in {"да", "true", "1"} else get("rooms"),
                   status=get("status", "active"), expires_at=get("expire-date"), unit=get("apartment"),
                   photos=[i.text for i in offer.findall("image")], market=market,
                   deal={"продажа": "sale", "аренда": "rent"}.get(raw_deal),
                   seller={"застройщик": "developer", "собственник": "owner", "агентство": "agent"}.get(get("sales-agent/category", "").lower()))


class StructuredPage(HTMLParser):
    scripts = None

    def __init__(self):
        super().__init__()
        self.scripts = []
        self.active = False

    def handle_starttag(self, tag, attrs):
        if tag == "script":
            self.active = dict(attrs).get("type") == "application/ld+json"
            if self.active:
                self.scripts.append("")

    def handle_data(self, data):
        if self.active:
            self.scripts[-1] += data

    def handle_endtag(self, tag):
        if tag == "script":
            self.active = False


def structured(html, link, cfg):
    if CLOSED.search(html[:3000]):
        return
    parser = StructuredPage()
    parser.feed(html)
    for script in parser.scripts:
        with suppress(ValueError):
            for graph in as_list(json.loads(script)):
                if not isinstance(graph, dict):
                    continue
                for item in as_list(graph.get("@graph", graph)):
                    if not isinstance(item, dict) or not set(as_list(item.get("@type"))) & {"Apartment", "RealEstateListing"}:
                        continue
                    main = item.get("mainEntity")
                    main = main if isinstance(main, dict) else item
                    address = main.get("address")
                    if not isinstance(address, dict):
                        continue
                    for offer in as_list(item.get("offers") or main.get("offers")):
                        if not isinstance(offer, dict) or str(offer.get("availability")).split("/")[-1] not in {"InStock", "LimitedAvailability"}:
                            continue
                        area = main.get("floorSize")
                        photos = main.get("image") or item.get("image")
                        yield dict(url=link, address=", ".join(filter(None, [address.get("addressLocality"), address.get("streetAddress")])),
                                   region=address.get("addressRegion"), locality=address.get("addressLocality"),
                                   price=offer.get("price"), currency=offer.get("priceCurrency"),
                                   area=area.get("value") if isinstance(area, dict) and area.get("unitCode") in {"MTK", "m²"} else None,
                                   rooms=main.get("numberOfRooms"), title=main.get("name") or item.get("name"),
                                   photos=[urljoin(link, str(p.get("url") if isinstance(p, dict) else p)) for p in as_list(photos) if p],
                                   deal=cfg.get("deal"), market=cfg.get("market"), seller=cfg.get("seller"),
                                   expires_at=offer.get("validThrough"))


async def groq(client, query):
    key = os.getenv("GROQ_API_KEY")
    if not query.text or not key:
        return query, "Не настроен" if not key else "Ключ настроен · разбор не запрашивался"
    try:
        result = await client.post("https://api.groq.com/openai/v1/chat/completions",
            headers={"Authorization": "Bearer " + key},
            json={"model": "openai/gpt-oss-120b", "temperature": 0, "response_format": {"type": "json_object"},
                  "messages": [{"role": "system", "content": "Преобразуй запрос в JSON фильтров Москвы и МО: place, deal sale/rent, market primary/secondary, seller owner/developer/agent, budget целое в рублях, rooms (0=студия), minArea. Используй только явно названные условия. Никогда не создавай объявления."},
                               {"role": "user", "content": query.text}]})
        result.raise_for_status()
        params = Query.model_validate(json.loads(result.json()["choices"][0]["message"]["content"]))
        return Query.model_validate({**params.model_dump(exclude_unset=True), **query.model_dump(exclude_unset=True)}), "Ответ получен"
    except (ValueError, KeyError, IndexError, httpx.HTTPError):
        return query, "Недоступен · локальный разбор"


def fallback(query):
    text = query.text.lower().replace("ё", "е")
    values = {}
    if query.deal is None:
        values["deal"] = "rent" if re.search(r"аренд|снять|сниму", text) else "sale"
    if query.market is None:
        if "новострой" in text or "первич" in text:
            values["market"] = "primary"
        elif "вторич" in text:
            values["market"] = "secondary"
    if query.seller is None and re.search(r"собственник|от хозя", text):
        values["seller"] = "owner"
    if query.rooms is None:
        for n, pattern in enumerate(["студи", "однуш|однокомнат|1[- ]?комн", "двуш|двухкомнат|2[- ]?комн", "треш|трехкомнат|3[- ]?комн"]):
            if re.search(pattern, text):
                values["rooms"] = n
                break
    if query.budget is None and (m := re.search(r"(?:до|не дороже)\s+(\d+(?:[ ,.]\d+)*)(?:\s*(млн|тыс|миллион|тысяч))?", text)):
        with suppress(ValueError, OverflowError):
            amount = int(float(number(m[1])) * (10**6 if (m[2] or "").startswith("м") else 1000 if m[2] else 1))
            if 0 < amount <= 10**12:
                values["budget"] = amount
    if not query.place:
        if re.search(r"в москве|г\. москва", text):
            values["place"] = "Москва"
        elif re.search(r"подмосков|московской област", text):
            values["place"] = "Московская область"
    return query.model_copy(update=values)


def matches(item, q):
    place = q.place.lower().replace("ё", "е").strip()
    if place in {"мо", "подмосковье"}:
        place = "московская область"
    if place == "москва":
        location_ok = "москва" in item["region"].lower() or "москва" in item["locality"].lower() or bool(re.match(r"^(?:г\.\s*)?москва\b", item["address"].lower()))
    else:
        location_ok = place in " ".join(item[k] for k in ("address", "region", "locality")).lower().replace("ё", "е")
    return (location_ok and q.deal == item["deal"] and (not q.market or q.market == item["market"])
            and (not q.seller or q.seller == item["seller"]) and (q.rooms is None or item["rooms"] == q.rooms)
            and (not q.budget or item["price"] <= q.budget) and item["area"] >= q.minArea)


def identity(item):
    # Match across sources only when a full address and a unit are explicit.
    if item["unit"]:
        address = re.sub(r"[^\w]+", " ", item["address"].lower().replace("ё", "е")).strip()
        unit = re.sub(r"^кв\.?\s*", "", item["unit"].lower()).strip()
        return f"{item['deal']}:{address}:{unit}"
    return item["url"]


async def discover(client, engine, phrase):
    address = os.getenv("SEARXNG_URL") if engine == "SearXNG" else os.getenv("FIRECRAWL_API_KEY")
    if not address:
        raise ValueError("Не настроен")
    if engine == "SearXNG":
        result = json.loads(await fetch(client, address.rstrip("/") + "/search", params={"q": phrase, "format": "json", "language": "ru"}))
        return [r.get("url") for r in result.get("results", [])]
    result = await client.post("https://api.firecrawl.dev/v2/search", headers={"Authorization": "Bearer " + address},
                               json={"query": phrase, "limit": 5, "sources": ["web"]})
    result.raise_for_status()
    return [r.get("url") for r in result.json().get("data", {}).get("web", [])]


async def verify_page(client, link, cfg):
    parsed = urlsplit(link)
    from urllib.robotparser import RobotFileParser
    robots = RobotFileParser()
    robots.parse((await fetch(client, f"https://{parsed.netloc}/robots.txt")).decode(errors="replace").splitlines())
    if not robots.can_fetch(AGENT, link):
        raise ValueError("Доступ к URL запрещён robots.txt")
    html = (await fetch(client, link)).decode(errors="replace")
    if CLOSED.search(html[:3000]):
        return []
    mode = cfg.get("renderer")
    if mode in {"playwright", "crawl4ai"}:
        projection = StructuredPage()
        projection.feed(html)
        html = "<html><head><meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'\"></head><body>"
        for document in projection.scripts:
            html += '<script type="application/ld+json">' + document.replace("<", "\\u003c") + "</script>"
        html += "</body></html>"
    if mode == "playwright":
        from playwright.async_api import async_playwright
        async with async_playwright() as runner:
            engine = await runner.chromium.launch()
            try:
                context = await engine.new_context(java_script_enabled=False, service_workers="block")
                await context.route("**/*", lambda route: route.abort())
                page = await context.new_page()
                await page.set_content(html, timeout=8000)
                html = await page.content()
            finally:
                await engine.close()
    if mode == "crawl4ai":
        from crawl4ai import AsyncWebCrawler, CrawlerRunConfig
        async with AsyncWebCrawler() as crawler:
            result = await crawler.arun(url="raw:" + html, config=CrawlerRunConfig(
                js_code="window.stop()", process_iframes=False, remove_overlay_elements=False))
            if not result.success:
                raise ValueError("Crawl4AI не обработал страницу")
            html = result.html
    return list(structured(html, link, cfg))


async def search(query):
    statuses = {}
    async with httpx.AsyncClient(timeout=10, follow_redirects=False, headers={"User-Agent": AGENT},
                                limits=httpx.Limits(max_connections=12)) as client:
        query, statuses["Groq"] = await groq(client, query)
        query = fallback(query)
        sources = config()
        for name, env in [("SearXNG", "SEARXNG_URL"), ("Firecrawl", "FIRECRAWL_API_KEY")]:
            statuses[name] = "Ожидает запроса" if os.getenv(env) else "Не настроен"
        for engine in ("Playwright", "Crawl4AI"):
            statuses[engine] = "Нет разрешённого источника с этим обработчиком"

        async def one(source, name):
            cfg = sources.get(source, {})
            rows = []
            errors = []
            if feed := cfg.get("feed"):
                try:
                    token = os.getenv(cfg.get("token_env", ""), "")
                    raw = await fetch(client, feed, headers={"Authorization": "Bearer " + token} if token else {})
                    if cfg.get("format") == "yrl":
                        rows.extend(yrl(raw))
                    else:
                        data = json.loads(raw)
                        data = data.get("offers") if isinstance(data, dict) else data
                        if not isinstance(data, list):
                            raise ValueError("Ожидалась выгрузка offers")
                        rows.extend(r for r in data if isinstance(r, dict))
                except (httpx.HTTPError, ValueError, ElementTree.ParseError) as err:
                    errors.append(f"выгрузка: {getattr(getattr(err, 'response', None), 'status_code', type(err).__name__)}")
            if cfg.get("allow_page_read") and (domain := HOSTS.get(source, cfg.get("domain"))):
                phrase = f"site:{domain} {'снять' if query.deal == 'rent' else 'купить'} квартиру Москва Московская область {query.place}"
                links = list(cfg.get("pages", []))
                for engine in ("SearXNG", "Firecrawl"):
                    try:
                        links.extend(await discover(client, engine, phrase))
                        statuses[engine] = "Ответ получен · ссылки требуют проверки"
                    except (httpx.HTTPError, ValueError, KeyError):
                        if not statuses[engine].startswith("Ответ получен") and statuses[engine] != "Не настроен":
                            statuses[engine] = "Недоступен или исчерпана квота"
                for link in list(dict.fromkeys(filter(None, (direct(x, source, cfg) for x in links))))[:5]:
                    try:
                        rows.extend(await asyncio.wait_for(verify_page(client, link, cfg), timeout=18))
                        if cfg.get("renderer"):
                            statuses[{"crawl4ai": "Crawl4AI", "playwright": "Playwright"}[cfg["renderer"]]] = "Страница обработана"
                    except Exception as err:
                        errors.append(f"страница: {getattr(getattr(err, 'response', None), 'status_code', type(err).__name__)}")
                        if cfg.get("renderer") in {"crawl4ai", "playwright"}:
                            key = {"crawl4ai": "Crawl4AI", "playwright": "Playwright"}[cfg["renderer"]]
                            if statuses[key] != "Страница обработана":
                                statuses[key] = "Страница не обработана"
            offers = [o for r in rows if (o := normalize(r, source, cfg)) and matches(o, query)]
            status = "Требуется разрешённый фид или доступ к API" if not cfg else "Проверено · совпадений нет"
            if errors:
                status = ("Частично доступен · " if offers else "Недоступен · ") + ", ".join(dict.fromkeys(errors))
            elif offers:
                status = "Проверено по разрешённому источнику"
            elif not cfg.get("feed") and not cfg.get("allow_page_read"):
                status = "Требуется разрешённый фид или доступ к API"
            return offers, {"source": name, "count": len(offers), "status": status}

        results = await asyncio.gather(*(one(k, name) for k, name in NAMES.items()))
    grouped = {}
    for offers, _ in results:
        for offer in offers:
            grouped.setdefault(identity(offer), offer)
    return {"offers": sorted(grouped.values(), key=lambda o: o["price"])[:80],
            "sources": [status for _, status in results] +
                [{"source": k, "count": 0, "status": v} for k, v in statuses.items()]}


def auth(authorization: str = Header(default="")):
    token = os.getenv("REALTOR_SEARCH_TOKEN", "")
    if not token or not secrets.compare_digest(authorization.encode(), ("Bearer " + token).encode()):
        raise HTTPException(401, "Требуется токен сервера")


async def monitor():
    while True:
        tick = int(time.time())
        with closing(db()) as conn, conn:
            watches = conn.execute("SELECT * FROM watches WHERE due <= ? ORDER BY due LIMIT 5", (tick,)).fetchall()
        for watch in watches:
            try:
                data = await asyncio.wait_for(search(Query.model_validate_json(watch["query"])), 115)
            except Exception:
                data = {"offers": []}
            with closing(db()) as conn, conn:
                if not conn.execute("SELECT id FROM watches WHERE id=?", (watch["id"],)).fetchone():
                    continue
                conn.execute("UPDATE watches SET due=? WHERE id=?", (tick + 1800, watch["id"]))
                for item in data["offers"]:
                    ident = identity(item)
                    old = conn.execute("SELECT price FROM seen WHERE watch=? AND identity=?", (watch["id"], ident)).fetchone()
                    kind = "new" if old is None else "price_drop" if old["price"] > item["price"] else None
                    if kind:
                        conn.execute("INSERT INTO events(watch,kind,offer) VALUES(?,?,?)", (watch["id"], kind, json.dumps(item, ensure_ascii=False)))
                    conn.execute("INSERT OR REPLACE INTO seen VALUES(?,?,?)", (watch["id"], ident, item["price"]))
                conn.execute("DELETE FROM events WHERE id < (SELECT MAX(id)-5000 FROM events)")
        await asyncio.sleep(15)


@asynccontextmanager
async def lifespan(_):
    with closing(db()):
        pass
    task = asyncio.create_task(monitor())
    try:
        yield
    finally:
        task.cancel()
        with suppress(asyncio.CancelledError):
            await task


app = FastAPI(lifespan=lifespan, docs_url=None, redoc_url=None, openapi_url=None)


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/v1/search", dependencies=[Depends(auth)])
async def run_search(query: Query):
    try:
        return await asyncio.wait_for(search(query), 115)
    except TimeoutError:
        raise HTTPException(504, "Истекло время поиска")


@app.post("/v1/watch", dependencies=[Depends(auth)])
def subscribe(query: Query):
    wire = query.model_dump_json(exclude_unset=True)
    with closing(db()) as conn, conn:
        previous = conn.execute("SELECT id FROM watches WHERE query=?", (wire,)).fetchone()
        if previous:
            return {"id": previous["id"]}
        if conn.execute("SELECT count(*) FROM watches").fetchone()[0] >= 20:
            raise HTTPException(409, "Достигнут лимит подписок")
        return {"id": conn.execute("INSERT INTO watches(query,due) VALUES(?,0)", (wire,)).lastrowid}


@app.delete("/v1/watch/{watch}", dependencies=[Depends(auth)])
def unsubscribe(watch: int):
    with closing(db()) as conn, conn:
        for table, column in [("watches", "id"), ("events", "watch"), ("seen", "watch")]:
            conn.execute(f"DELETE FROM {table} WHERE {column}=?", (watch,))
    return {"ok": True}


@app.get("/v1/events", dependencies=[Depends(auth)])
def events(after: int = 0, watch: int = 0):
    with closing(db()) as conn, conn:
        rows = conn.execute("SELECT * FROM events WHERE id>? AND watch=? ORDER BY id LIMIT 100", (after, watch))
        return {"events": [{"id": r["id"], "watch": r["watch"], "kind": r["kind"], "offer": json.loads(r["offer"])} for r in rows]}
