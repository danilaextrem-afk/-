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
            if label in (node.get("text"), node.get("content-desc")):
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

tap("Главная")
tap("Настроить поиск")
capture("autopilot", "Автопилот")
