"""Capture actual Compose destinations on a running Android emulator."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

def adb(*args):
    return subprocess.check_output(["adb", *args])

def find(label):
    for _ in range(12):
        try:
            adb("shell", "rm", "-f", "/sdcard/view.xml")
            adb("shell", "uiautomator", "dump", "/sdcard/view.xml")
            root = ET.fromstring(adb("shell", "cat", "/sdcard/view.xml"))
        except (subprocess.CalledProcessError, ET.ParseError):
            time.sleep(1)
            continue
        for node in root.iter("node"):
            if node.get("resource-id", "").endswith(":id/permission_allow_button") and node.get("package", "").endswith("permissioncontroller"):
                x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
                adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
                break
            if label in (node.get("text", "").strip(), node.get("content-desc", "").strip()):
                return [int(n) for n in re.findall(r"\d+", node.get("bounds"))]
        time.sleep(0.3)
    raise RuntimeError("Экран не содержит элемент " + label)

def capture(name, heading):
    find(heading)
    Path("captures", name + ".png").write_bytes(adb("exec-out", "screencap", "-p"))

Path("captures").mkdir(exist_ok=True)
capture("home", "Мой рабочий день")
for tab, heading, name in [
    ("CRM", "Воронка продаж", "crm"),
    ("Задачи", "Задачи и встречи", "tasks"),
    ("ИИ", "ИИ-ассистент", "assistant"),
    ("Объекты", "Недвижимость", "properties"),
]:
    x1, y1, x2, y2 = find(tab)
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    capture(name, heading)

def tap(label):
    x1, y1, x2, y2 = find(label)
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))

tap("Объект")
tap("Название")
adb("shell", "input", "text", "Owner%20workflow%20QA")
adb("shell", "input", "keyevent", "KEYCODE_BACK")
tap("Сохранить")
tap("Сопровождение")
tap("Начать сопровождение")
capture("owner", "Подтвердить этап")
tap("Главная")
tap("Поручить агенту")
capture("agent", "Поручения агенту")
tap("Составь план на неделю")
tap("Запустить план")
find("Журнал поручений")

tap("Главная")
tap("Настроить поиск")
capture("autopilot", "Автопилот")

# Exercise the same public search entry point as a user and capture actual results.
def current_tree():
    for _ in range(4):
        try:
            adb("shell", "uiautomator", "dump", "/sdcard/view.xml")
            return ET.fromstring(adb("shell", "cat", "/sdcard/view.xml"))
        except (subprocess.CalledProcessError, ET.ParseError):
            time.sleep(1)
    raise RuntimeError("Эмулятор не вернул дерево экрана")

for _ in range(5):
    if any(n.get("text") == "Запустить сейчас" for n in current_tree().iter("node")):
        break
    adb("shell", "input", "swipe", "500", "1700", "500", "880", "400")
    time.sleep(.5)
tap("Запустить сейчас")
time.sleep(3)
tap("Объекты")
for _ in range(15):
    if any("Ваш доход:" in n.get("text", "") for n in current_tree().iter("node")):
        capture("auction-properties", "Недвижимость")
        break
    time.sleep(1)
