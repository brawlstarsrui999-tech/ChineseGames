#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Быстрая проверка синтаксиса Kotlin-файлов проекта (без Gradle и Android SDK).

Парсер `kopyt` разбирает исходники целиком: падают только настоящие
синтаксические ошибки — незакрытые скобки, пропущенные запятые в enum,
оборванные объявления. Семантику (типы, импорты) он не проверяет —
для этого нужен `./gradlew :app:assembleDebug`.

Запуск:  python3 tools/check_syntax.py
Нужен:   pip install kopyt
"""

import glob
import os
import sys

try:
    from kopyt import Parser
except ImportError:
    print("нужен kopyt: pip install kopyt")
    raise SystemExit(2)

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))


def main() -> int:
    files = sorted(glob.glob(os.path.join(ROOT, "app", "src", "main", "java", "**", "*.kt"), recursive=True))
    bad = 0
    for path in files:
        with open(path, encoding="utf-8") as fh:
            source = fh.read()
        try:
            Parser(source).parse()
        except Exception as err:  # noqa: BLE001
            bad += 1
            print(f"✗ {os.path.relpath(path, ROOT)}: {str(err)[:200]}")
    print(f"Проверено файлов: {len(files)}, с ошибками: {bad}")
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
