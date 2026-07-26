#!/usr/bin/env python3
import json
import sys
import zipfile
from functools import lru_cache
from pathlib import Path


ITEM_FIRST_SUFFIXES = (
    "_door", "_trapdoor", "_sign", "_hanging_sign", "_banner", "_head",
    "_skull", "_bed", "_candle", "_flower_pot", "_shulker_box",
)
SPECIAL = {
    "water": "block/water_still",
    "lava": "block/lava_still",
    "grass_block": "block/grass_block_top",
    "bamboo_sapling": "block/bamboo_stage0",
    "potted_bamboo": "block/bamboo_stage0",
    "redstone_wire": "item/redstone",
    "iron_bars": "block/iron_bars",
    "chain": "item/chain",
    "cobweb": "block/cobweb",
}
COLORS = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")
TEXTURE_KEYS = ("layer0", "all", "particle", "top", "side", "front", "end", "down", "up")


def short_ids(names, kind):
    prefix = f"assets/minecraft/textures/{kind}/"
    return {
        name[len(prefix):-4]
        for name in names
        if name.startswith(prefix) and name.endswith(".png") and "/" not in name[len(prefix):-4]
    }


def as_model_path(value):
    value = value.removeprefix("minecraft:")
    if not value.startswith(("block/", "item/")):
        value = "block/" + value
    return "assets/minecraft/models/" + value + ".json"


def first_state_model(state):
    variants = state.get("variants", {})
    for variant in variants.values():
        if isinstance(variant, list):
            variant = variant[0]
        if isinstance(variant, dict) and "model" in variant:
            return variant["model"]
    for entry in state.get("multipart", []):
        apply = entry.get("apply")
        if isinstance(apply, list):
            apply = apply[0]
        if isinstance(apply, dict) and "model" in apply:
            return apply["model"]
    return None


def main(client_jar: Path, output: Path) -> None:
    with zipfile.ZipFile(client_jar) as jar:
        names = set(jar.namelist())
        block_textures = short_ids(names, "block")
        item_textures = short_ids(names, "item")
        blockstates = {
            name[len("assets/minecraft/blockstates/"):-5]
            for name in names
            if name.startswith("assets/minecraft/blockstates/") and name.endswith(".json")
        }

        @lru_cache(maxsize=None)
        def read_json(path):
            try:
                return json.loads(jar.read(path))
            except (KeyError, json.JSONDecodeError):
                return {}

        @lru_cache(maxsize=None)
        def resolve_model(path, inherited=()):
            model = read_json(path)
            textures = dict(inherited)
            textures.update(model.get("textures", {}))
            parent = model.get("parent")
            if parent:
                resolved = resolve_model(as_model_path(parent), tuple(textures.items()))
                if resolved:
                    return resolved
            for key in TEXTURE_KEYS:
                value = textures.get(key)
                while isinstance(value, str) and value.startswith("#"):
                    value = textures.get(value[1:])
                if isinstance(value, str):
                    value = value.removeprefix("minecraft:")
                    if value.startswith(("block/", "item/")) and f"assets/minecraft/textures/{value}.png" in names:
                        return value
            return None

        result = dict(SPECIAL)
        for material in sorted(blockstates):
            if material in result:
                continue
            if material in item_textures:
                result[material] = "item/" + material
                continue
            if material.endswith("_banner"):
                color = material.removesuffix("_banner")
                wool = f"{color}_wool"
                if wool in block_textures:
                    result[material] = "block/" + wool
                    continue
            if material.endswith(("_head", "_skull")):
                result[material] = "block/stone"
                continue
            if material in block_textures:
                result[material] = "block/" + material
                continue

            state = read_json(f"assets/minecraft/blockstates/{material}.json")
            model = first_state_model(state)
            texture = resolve_model(as_model_path(model), ()) if model else None
            if texture:
                result[material] = texture

    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8") as file:
        for material, texture in sorted(result.items()):
            file.write(f"{material}={texture}\n")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: generate_waila_texture_map.py <client.jar> <output.properties>")
    main(Path(sys.argv[1]), Path(sys.argv[2]))
