"""Capture actual Compose destinations on a running Android emulator."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

def adb(*args):
    return subprocess.check_output(["adb", *args])

def find(label):
    for _ in range(6):
        adb("shell", "uiautomator", "dump", "/sdcard/view.xml")
        root = ET.fromstring(adb("shell", "cat", "/sdcard/view.xml"))
        for node in root.iter("node"):
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
