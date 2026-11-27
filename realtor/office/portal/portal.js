"use strict";

const field = (id, value) => { document.getElementById(id).textContent = value; };
const status = document.getElementById("status");
const number = new Intl.NumberFormat("ru-RU");
const money = new Intl.NumberFormat("ru-RU", {style: "currency", currency: "RUB", maximumFractionDigits: 0});
const date = new Intl.DateTimeFormat("ru-RU", {dateStyle: "medium", timeStyle: "short"});
const count = (value) => value === null ? "Нет данных" : number.format(value);
const advertising = {
    not_requested: "Объект ещё не передан в фид Яндекс Недвижимости.",
    in_feed: "Объект в фиде Яндекс Недвижимости. Статус на площадке неизвестен.",
    withdrawn_from_feed: "Объект исключён из фида Яндекс Недвижимости.",
    feed_disabled: "Передача в фид приостановлена офисом.",
};

async function loadReport() {
    const token = location.hash.slice(1);
    if (!/^[A-Za-z0-9_-]{43}$/.test(token)) {
        status.textContent = "Откройте полную ссылку, которую вам отправили.";
        return;
    }
    try {
        const response = await fetch("owner/report", {
            headers: {Authorization: "Bearer " + token},
            credentials: "omit",
            redirect: "error",
            cache: "no-store",
        });
        if (response.status === 401) {
            status.textContent = "Срок действия ссылки истёк или офис закрыл доступ.";
            return;
        }
        if (!response.ok) throw new Error("Report unavailable");
        const data = await response.json();
        const property = data.property;
        const report = data.report;
        field("title", property.title);
        field("address", property.address);
        field("price", property.price > 0 ? money.format(property.price) : "Цена не указана");
        const details = [
            property.area > 0 ? number.format(property.area) + " м²" : "",
            property.rooms > 0 ? number.format(property.rooms) + " комн." : "",
            data.sold ? "Сделка закрыта" : "",
        ].filter(Boolean);
        field("facts", details.join(" · "));
        field("advertising", advertising[data.yandex_feed] || "Статус передачи неизвестен.");
        if (data.cian_feed !== "not_requested") {
            const text = {
                in_feed: "Объект в фиде Циана. Статус на площадке неизвестен.",
                withdrawn_from_feed: "Объект исключён из фида Циана.",
                feed_disabled: "Передача в фид Циана приостановлена офисом.",
            }[data.cian_feed];
            if (text) {
                field("cian-advertising", text);
                document.getElementById("cian-advertising").hidden = false;
            }
        }
        field("views", count(report.views));
        field("inquiries", count(report.inquiries));
        field("measured", report.measured_at ? "Данные от риелтора · " + date.format(report.measured_at * 1000) : "");
        field("recommendation", report.recommendation || "Риелтор ещё не добавил рекомендацию.");
        field("updated", "Обновлено " + date.format(Math.max(data.property_updated_at, report.updated_at || 0) * 1000));
        document.getElementById("report").hidden = false;
        status.hidden = true;
    } catch {
        status.textContent = "Отчёт сейчас недоступен. Попробуйте обновить страницу.";
    }
}

loadReport();
addEventListener("hashchange", () => location.reload());
