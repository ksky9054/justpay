"""Делает из пакета Simple Icons значки и базу брендов для приложения.

Запуск: python tools/make_brands.py <папка пакета simple-icons, где лежат data и icons>
Создаёт app/src/main/res/drawable/brand_*.xml и app/src/main/res/raw/brands.tsv
"""
import io
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ru_aliases import RU

package = sys.argv[1]
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
drawables = os.path.join(root, "drawable")
raw = os.path.join(root, "raw")

VECTOR = ('<?xml version="1.0" encoding="utf-8"?>\n'
          '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
          '    android:width="24dp"\n'
          '    android:height="24dp"\n'
          '    android:viewportWidth="24"\n'
          '    android:viewportHeight="24">\n'
          '    <path\n'
          '        android:fillColor="#FFFFFFFF"\n'
          '        android:pathData="%s" />\n'
          '</vector>\n')


NUMBER = re.compile(r"[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?")
ARGS = {"m": 2, "l": 2, "h": 1, "v": 1, "c": 6, "s": 4, "q": 4, "t": 2, "a": 7, "z": 0}


def explicit_path(d):
    """Android не понимает флаги дуги без пробелов («0 00-4.8»), пишем всё через пробелы"""
    out = []
    i = 0
    command = ""
    count = 0  # сколько чисел уже прочитано в текущей группе команды
    while i < len(d):
        c = d[i]
        if c in " ,\t\r\n":
            i += 1
        elif c.isalpha():
            command = c
            count = 0
            out.append(c)
            i += 1
        else:
            if command.lower() == "a" and count % 7 in (3, 4):
                token = c  # флаг дуги это одна цифра 0 или 1
                i += 1
            else:
                token = NUMBER.match(d, i).group(0)
                i += len(token)
            out.append(token)
            count += 1
    # Проверка: у каждой команды число значений кратно её размеру
    command = ""
    n = 0
    for token in out + ["z"]:
        if token.isalpha() and len(token) == 1:
            size = ARGS[command.lower()] if command else 0
            assert (size == 0 and n == 0) or (size and n % size == 0), (command, n, d[:60])
            command = token
            n = 0
        else:
            n += 1
    result = " ".join(out)
    return result


def clean(text):
    return re.sub(r"[\t|\r\n]+", " ", text).strip()


data = json.load(io.open(os.path.join(package, "data", "simple-icons.json"), encoding="utf-8"))
lines = []
skipped = []
for item in data:
    slug = item["slug"]
    svg = io.open(os.path.join(package, "icons", slug + ".svg"), encoding="utf-8").read()
    paths = re.findall(r' d="([^"]+)"', svg)
    if len(paths) != 1:
        skipped.append(slug)
        continue
    with io.open(os.path.join(drawables, "brand_" + slug + ".xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write(VECTOR % explicit_path(paths[0]))

    names = []
    for aka in item.get("aliases", {}).get("aka", []):
        names.append(aka)
    for loc in item.get("aliases", {}).get("loc", {}).values():
        names.append(loc)
    if slug in RU:
        names.extend(n.strip() for n in RU[slug].split(","))
    lines.append("\t".join([slug, clean(item["title"]), item["hex"].upper(),
                            "|".join(clean(n) for n in names if clean(n))]))

with io.open(os.path.join(raw, "brands.tsv"), "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(lines) + "\n")

known = set(i["slug"] for i in data)
print("значков:", len(lines), "пропущено:", skipped)
print("нет в базе:", [s for s in RU if s not in known])
