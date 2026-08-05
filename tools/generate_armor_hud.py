#!/usr/bin/env python3
import sys
import struct
import zlib
from pathlib import Path


def chunk(kind: bytes, data: bytes) -> bytes:
    body = kind + data
    return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))


def png(path: Path, rows: list[str], palette: dict[str, tuple[int, int, int, int]]) -> None:
    w, h = len(rows[0]), len(rows)
    raw = b"".join(b"\0" + bytes(c for p in row for c in palette[p]) for row in rows)
    header = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
                     + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))

def canvas(shape: str) -> list[str]:
    H = lambda *parts: "".join(parts)
    shapes = {
        "helmet": [
            H(" " * 4, "a" * 8, " " * 4),
            H(" " * 3, "a", "m" * 8, "a", " " * 3),
            H(" " * 2, "a", "m" * 10, "a", " " * 2),
            H(" " * 2, "a", "m" * 10, "a", " " * 2),
            H(" ", "a", "m" * 12, "a", " "),
            H(" ", "a", "m" * 12, "a", " "),
            H(" ", "a", "m" * 12, "a", " "),
            H(" ", "a", "m" * 12, "a", " "),
            *[H("a", "m" * 14, "a")] * 8,
        ],
        "chestplate": [
            H("a", "m" * 2, "a", " " * 8, "a", "m" * 2, "a"),
            H("a", "m" * 3, "a", " " * 6, "a", "m" * 3, "a"),
            H(" ", "a", "m" * 12, "a", " "),
            *[H(" ", "a", "m" * 12, "a", " ")] * 9,
            H(" " * 2, "a", "m" * 10, "a", " " * 2),
            H(" " * 3, "a", "m" * 8, "a", " " * 3),
            H(" " * 4, "a", "m" * 6, "a", " " * 4),
            H(" " * 5, "a", "m" * 4, "a", " " * 5),
        ],
        "leggings": [
            H(" " * 2, "a", "m" * 10, "a", " " * 2),
            *[H(" " * 2, "a", "m" * 10, "a", " " * 2)] * 5,
            H(" " * 2, "a", "m" * 4, "a", " ", "a", "m" * 4, "a", " " * 2),
            H(" " * 2, "a", "m" * 4, "a", " ", "a", "m" * 4, "a", " " * 2),
            H(" " * 2, "a", "m" * 3, "a", " " * 3, "a", "m" * 3, "a", " " * 2),
            *[H(" " * 3, "a", "m" * 2, "a", " " * 3, "a", "m" * 2, "a", " " * 3)] * 7,
        ],
        "boots": [
            *[" " * 16] * 8,
            H(" " * 3, "a", "m" * 3, "a", " " * 2, "a", "m" * 3, "a", " " * 3),
            H(" " * 2, "a", "m" * 4, "a", " " * 2, "a", "m" * 4, "a", " " * 2),
            *[H(" " * 2, "a", "m" * 4, "a", " " * 2, "a", "m" * 4, "a", " " * 2)] * 4,
            H(" " * 2, "a" * 5, " " * 2, "a" * 5, " " * 2),
            H(" " * 2, "a" * 5, " " * 2, "a" * 5, " " * 2),
        ],
        "empty_helmet": [
            H(" " * 4, "a" * 8, " " * 4),
            H(" " * 3, "a", " " * 8, "a", " " * 3),
            H(" " * 2, "a", " " * 10, "a", " " * 2),
            H(" " * 2, "a", " " * 10, "a", " " * 2),
            H(" ", "a", " " * 12, "a", " "),
            H(" ", "a", " " * 12, "a", " "),
            H(" ", "a", " " * 12, "a", " "),
            H(" ", "a", " " * 12, "a", " "),
            *[H("a", " " * 14, "a")] * 8,
        ],
        "empty_chestplate": [
            H("a", " " * 2, "a", " " * 8, "a", " " * 2, "a"),
            H("a", " " * 3, "a", " " * 6, "a", " " * 3, "a"),
            H(" ", "a", " " * 12, "a", " "),
            *[H(" ", "a", " " * 12, "a", " ")] * 9,
            H(" " * 2, "a", " " * 10, "a", " " * 2),
            H(" " * 3, "a", " " * 8, "a", " " * 3),
            H(" " * 4, "a", " " * 6, "a", " " * 4),
            H(" " * 5, "a", " " * 4, "a", " " * 5),
        ],
        "empty_leggings": [
            H(" " * 2, "a", " " * 10, "a", " " * 2),
            *[H(" " * 2, "a", " " * 10, "a", " " * 2)] * 5,
            H(" " * 2, "a", " " * 4, "a", " ", "a", " " * 4, "a", " " * 2),
            H(" " * 2, "a", " " * 4, "a", " ", "a", " " * 4, "a", " " * 2),
            H(" " * 2, "a", " " * 3, "a", " " * 3, "a", " " * 3, "a", " " * 2),
            *[H(" " * 3, "a", " " * 2, "a", " " * 3, "a", " " * 2, "a", " " * 3)] * 7,
        ],
        "empty_boots": [
            *[" " * 16] * 8,
            H(" " * 3, "a", " " * 3, "a", " " * 2, "a", " " * 3, "a", " " * 3),
            H(" " * 2, "a", " " * 4, "a", " " * 2, "a", " " * 4, "a", " " * 2),
            *[H(" " * 2, "a", " " * 4, "a", " " * 2, "a", " " * 4, "a", " " * 2)] * 4,
            H(" " * 2, "a" * 5, " " * 2, "a" * 5, " " * 2),
            H(" " * 2, "a" * 5, " " * 2, "a" * 5, " " * 2),
        ],
    }
    return shapes.get(shape, [[" " * 16] * 16])

def main():
    if len(sys.argv) < 2:
        print(f"usage: {sys.argv[0]} <output-dir>", file=sys.stderr)
        sys.exit(1)

    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    assets = out / "assets" / "someutils"
    tex = assets / "textures" / "armor_hud"
    tex.mkdir(parents=True, exist_ok=True)

    palette = {
        " ": (0, 0, 0, 0),
        "p": (15, 20, 28, 255),
        "a": (132, 187, 99, 255),
        "m": (174, 192, 181, 180),
        "w": (255, 255, 255, 255),
    }

    # составной glyph сохраняет позицию иконки и полосы
    for slot in ["helmet", "chestplate", "leggings", "boots"]:
        icon_rows = canvas(slot)
        bar_rows = [
            " " + "a" * 14 + " ",
            " " + "p" * 14 + " ",
            " " + "a" * 14 + " ",
        ]
        combined = icon_rows + bar_rows
        png(tex / f"slot_{slot}.png", combined, palette)

        empty_icon_rows = canvas(f"empty_{slot}")
        empty_combined = empty_icon_rows + bar_rows
        png(tex / f"empty_{slot}.png", empty_combined, palette)

    # заполнение полосы использует тот же размер glyph
    bar_width = 16
    for frame in range(16):
        filled = round((bar_width - 2) * frame / 15)
        rows = [" " * bar_width] * 16 + [
            " " * bar_width,
            " " + ("w" * filled) + (" " * (bar_width - 2 - filled)) + " ",
            " " * bar_width,
        ]
        png(tex / f"bar_fill_{frame}.png", rows, palette)

    # отдельный пиксельный шрифт заголовка
    letters = {
        "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
        "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
        "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
        "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    }
    title = "ARMOR"
    scale = 1
    title_w = len(title) * 5 * scale + (len(title) - 1) * scale
    title_h = 7 * scale
    title_rows = [[" "] * title_w for _ in range(title_h)]
    cursor = 0
    for letter in title:
        for y, line in enumerate(letters[letter]):
            for x, bit in enumerate(line):
                if bit == "1":
                    for sy in range(scale):
                        for sx in range(scale):
                            title_rows[y * scale + sy][cursor + x * scale + sx] = "m"
        cursor += 6 * scale

    # рамка разделена на верх, боковые линии и низ
    frame_w, frame_h = 96, 18
    for anim in range(32):
        def edge(kind):
            rows = []
            for y in range(frame_h):
                row = []
                for x in range(frame_w):
                    corner = (x < 2 or x >= frame_w - 2) and (y < 2 or y >= frame_h - 2)
                    active = ((kind == "top" and y == 0 and not corner)
                              or (kind == "bottom" and y == frame_h - 1 and not corner)
                              or (kind == "rail" and (x == 0 or x == frame_w - 1) and not corner))
                    position = x if kind != "rail" else y
                    pulse = (position - anim * 3) % 96
                    row.append("m" if active and pulse < 12 else "a" if active else " ")
                rows.append("".join(row))
            return rows

        png(tex / f"frame_top_{anim}.png", edge("top"), palette)
        png(tex / f"frame_rail_{anim}.png", edge("rail"), palette)
        png(tex / f"frame_bottom_{anim}.png", edge("bottom"), palette)

    png(tex / "armor_title.png", ["".join(row) for row in title_rows], palette)


    mcmeta = out / "pack.mcmeta"
    mcmeta.write_text('{"pack":{"pack_format":75,"description":"SomeUtils Armor HUD"}}')

if __name__ == "__main__":
    main()
