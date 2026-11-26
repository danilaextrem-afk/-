"""Capture Realtor AI routes from the Android emulator via their accessible names."""
import sys
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

def adb(*args):
    return subprocess.check_output(["adb", *args])

def press(x, y):
    adb("shell", "input", "tap", str(x), str(y))

def point(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1+x2)//2, (y1+y2)//2

def dump():
    adb("shell", "rm", "-f", "/sdcard/view.xml")
    adb("shell", "uiautomator", "dump", "/sdcard/view.xml")
    return list(ET.fromstring(adb("shell", "cat", "/sdcard/view.xml")).iter("node"))

def find(label, attempts=7, scroll=False):
    for i in range(attempts):
        try:
            nodes = dump()
        except (subprocess.CalledProcessError, ET.ParseError):
            time.sleep(1)
            continue
        # Close only a known emulator launcher ANR, never an app crash.
        if any("Pixel Launcher isn't responding" in n.get("text", "") for n in nodes):
            button = next((n for n in nodes if n.get("resource-id", "").endswith(":id/aerr_close")
                           or n.get("text") == "Close app"), None)
            if button is not None:
                press(*point(button)); continue
        for n in nodes:
            if label in (n.get("text", "").strip(), n.get("content-desc", "").strip()):
                return point(n)
        if scroll:
            adb("shell", "input", "swipe", "500", "1650", "500", "750", "340")
        time.sleep(0.4)
    raise RuntimeError("Элемент не найден: " + label)

def tap(label, scroll=False):
    press(*find(label, scroll=scroll))

def screenshot(name):
    Path("captures", name + ".png").write_bytes(adb("exec-out", "screencap", "-p"))

def capture(name, heading, scroll=False):
    find(heading, scroll=scroll)
    screenshot(name)

def type_text(value):
    adb("shell", "input", "text", value.replace(" ", "%s"))
    adb("shell", "input", "keyevent", "KEYCODE_BACK")

failures = {}
def check(name, operation):
    try:
        operation()
    except (RuntimeError, subprocess.CalledProcessError) as error:
        failures[name] = str(error)
        screenshot(name + "-error")

def home():
    tap("Главная")

def open_home_tool(label):
    home()
    tap(label, scroll=True)

def models():
    tap("ИИ")
    capture("assistant", "ИИ-ассистент")
    tap("Выбрать локальную модель")
    capture("models", "Модели на устройстве")

def properties():
    tap("Объекты")
    capture("properties", "Недвижимость")
    tap("Карта")
    capture("map", "Карта объектов")
    tap("Объекты")
    tap("Объект")
    tap("Название")
    type_text("Owner workflow QA")
    tap("Сохранить")
    capture("property", "Сопровождение", scroll=True)
    tap("Сопровождение")
    tap("Начать сопровождение")
    capture("owner", "Подтвердить этап")
    capture("pricing", "Сравнение цен", scroll=True)

def crm():
    tap("CRM")
    capture("crm", "Воронка продаж")
    tap("+ Клиент")
    tap("Имя")
    type_text("CRM contact QA")
    tap("Сохранить")
    tap("CRM contact QA", scroll=True)
    capture("client", "CRM contact QA")

def agent():
    open_home_tool("Поручить агенту")
    capture("agent", "Поручения агенту")
    tap("Составь план на неделю")
    tap("Дать поручение")
    find("Журнал поручений", scroll=True)

def settings():
    home()
    tap("Настройки")
    capture("settings", "Внешний вид")
    tap("Светлая")
    capture("settings-light", "Внешний вид")
    tap("Тёмная")

Path("captures").mkdir(exist_ok=True)
if sys.argv[1:] == ["--preview"]:
    capture("release-candidate", "Мой рабочий день")
    sys.exit(0)
check("onboarding", lambda: capture("onboarding", "Подключите первый источник"))
check("home", lambda: capture("home", "Мой рабочий день"))
check("calendar", lambda: (tap("Задачи"), capture("calendar", "Задачи и встречи")))
check("chat_and_models", models)
check("deals_and_owner", properties)
check("crm_and_client", crm)
check("autopilot", lambda: (open_home_tool("Открыть автопилот"), capture("autopilot", "Автопилот")))
check("agent", agent)
check("leads", lambda: (open_home_tool("Лиды"), capture("leads", "Входящие заявки")))
check("publications", lambda: (open_home_tool("Публикации"), capture("publications", "Публикации")))
check("analytics", lambda: (tap("Объекты"), tap("Аналитика"), capture("analytics", "Аналитика")))
check("settings", settings)
Path("captures", "ui-checks.txt").write_text("\n".join(f"{k}: {v}" for k, v in failures.items()) or "All required captures completed.", encoding="utf-8")
if failures:
    raise RuntimeError(failures)
