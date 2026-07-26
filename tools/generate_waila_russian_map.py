#!/usr/bin/env python3
import json
import sys
from pathlib import Path


def main(language_file: Path, output: Path) -> None:
    translations = json.loads(language_file.read_text(encoding="utf-8"))

    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8") as file:
        for key, value in sorted(translations.items()):
            if key.startswith(("block.minecraft.", "item.minecraft.")):
                file.write(f"{key}={value}\n")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: generate_waila_russian_map.py <ru_ru.json> <output.properties>")
    main(Path(sys.argv[1]), Path(sys.argv[2]))
