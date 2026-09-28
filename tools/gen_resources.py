"""Generates the mod's JSON resources (models, recipes, loot, tags, worldgen, lang).

Run after changing species or biome lists: python tools/gen_resources.py
The output is committed, so the mod builds without running this script.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
MOD = "grandtrees"

# species -> vanilla sapling used in the recipe (plus extra ingredients)
SPECIES = {
    "oak": ["minecraft:oak_sapling"],
    "birch": ["minecraft:birch_sapling"],
    "spruce": ["minecraft:spruce_sapling"],
    "jungle": ["minecraft:jungle_sapling"],
    "acacia": ["minecraft:acacia_sapling"],
    "dark_oak": ["minecraft:dark_oak_sapling"],
    "cherry": ["minecraft:cherry_sapling"],
    "willow": ["minecraft:oak_sapling", "minecraft:vine"],
}

NAMES_EN = {
    "oak": "Oak", "birch": "Birch", "spruce": "Spruce", "jungle": "Jungle", "acacia": "Acacia",
    "dark_oak": "Dark Oak", "cherry": "Cherry", "willow": "Willow",
}
NAMES_PT = {
    "oak": "Carvalho", "birch": "Bétula", "spruce": "Pinheiro", "jungle": "Selva", "acacia": "Acácia",
    "dark_oak": "Carvalho Escuro", "cherry": "Cerejeira", "willow": "Salgueiro",
}

# Biomes where each species generates naturally (new chunks only). Pack makers can extend the
# grandtrees:has_grand_<species> biome tags with a data pack.
BIOMES = {
    "oak": ["minecraft:forest", "minecraft:flower_forest"],
    "birch": ["minecraft:birch_forest", "minecraft:old_growth_birch_forest"],
    "spruce": ["minecraft:taiga", "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga",
               "minecraft:old_growth_spruce_taiga", "minecraft:grove", "minecraft:windswept_forest"],
    "jungle": ["minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle"],
    "acacia": ["minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna"],
    "dark_oak": ["minecraft:dark_forest"],
    "cherry": ["minecraft:cherry_grove"],
    "willow": ["minecraft:swamp"],
}

# Size weights for natural generation (capped further by the config's worldgen.maxSize).
WORLDGEN_SIZES = {
    "default": {"small": 1, "medium": 4, "big": 4, "large": 2, "extra_large": 1},
    "spruce": {"medium": 3, "big": 4, "large": 3, "extra_large": 1},
    "birch": {"small": 1, "medium": 4, "big": 3, "large": 1},
}

# Leaves that occasionally drop a tuned sapling (1%).
LEAF_DROPS = {
    "oak": "oak_leaves", "birch": "birch_leaves", "spruce": "spruce_leaves", "jungle": "jungle_leaves",
    "acacia": "acacia_leaves", "dark_oak": "dark_oak_leaves", "cherry": "cherry_leaves",
}


def write(rel, data):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main():
    saplings = [f"{MOD}:tuned_{s}_sapling" for s in SPECIES]
    lang_en = {"itemGroup.grandtrees": "Grand Trees"}
    lang_pt = {"itemGroup.grandtrees": "Grand Trees"}
    gl_entries = []

    for s, ingredients in SPECIES.items():
        name = f"tuned_{s}_sapling"
        write(f"assets/{MOD}/blockstates/{name}.json", {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
        write(f"assets/{MOD}/models/block/{name}.json", {
            "parent": "minecraft:block/cross",
            "render_type": "minecraft:cutout",
            "textures": {"cross": f"{MOD}:block/{name}"},
        })
        write(f"assets/{MOD}/models/item/{name}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{MOD}:block/{name}"},
        })
        write(f"data/{MOD}/loot_tables/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1.0,
                "bonus_rolls": 0.0,
                "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
            "random_sequence": f"{MOD}:blocks/{name}",
        })
        write(f"data/{MOD}/recipes/{name}.json", {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [{"item": i} for i in ingredients] + [{"item": "minecraft:bone_meal"}],
            "result": {"item": f"{MOD}:{name}"},
        })

        # World generation
        write(f"data/{MOD}/worldgen/configured_feature/{s}.json", {
            "type": f"{MOD}:grand_tree",
            "config": {"species": s, "sizes": WORLDGEN_SIZES.get(s, WORLDGEN_SIZES["default"])},
        })
        write(f"data/{MOD}/worldgen/placed_feature/{s}.json", {
            "feature": f"{MOD}:{s}",
            "placement": [
                {"type": f"{MOD}:config_chance", "species": s},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
                {"type": "minecraft:block_predicate_filter", "predicate": {
                    "type": "minecraft:would_survive",
                    "state": {"Name": "minecraft:oak_sapling", "Properties": {"stage": "0"}},
                }},
                {"type": "minecraft:biome"},
            ],
        })
        write(f"data/{MOD}/tags/worldgen/biome/has_grand_{s}.json", {"replace": False, "values": BIOMES[s]})
        write(f"data/{MOD}/forge/biome_modifier/add_{s}.json", {
            "type": "forge:add_features",
            "biomes": f"#{MOD}:has_grand_{s}",
            "features": f"{MOD}:{s}",
            "step": "vegetal_decoration",
        })

        if s in LEAF_DROPS:
            leaves = LEAF_DROPS[s]
            write(f"data/{MOD}/loot_modifiers/{name}_from_{leaves}.json", {
                "type": f"{MOD}:add_item",
                "conditions": [
                    {"condition": "forge:loot_table_id", "loot_table_id": f"minecraft:blocks/{leaves}"},
                    {"condition": "minecraft:random_chance", "chance": 0.01},
                ],
                "item": f"{MOD}:{name}",
            })
            gl_entries.append(f"{MOD}:{name}_from_{leaves}")

        lang_en[f"block.{MOD}.{name}"] = f"Tuned {NAMES_EN[s]} Sapling"
        lang_pt[f"block.{MOD}.{name}"] = f"Muda Afinada de {NAMES_PT[s]}"

    write("data/forge/loot_modifiers/global_loot_modifiers.json", {"replace": False, "entries": gl_entries})
    write("data/minecraft/tags/blocks/saplings.json", {"replace": False, "values": saplings})
    write("data/minecraft/tags/items/saplings.json", {"replace": False, "values": saplings})
    write(f"assets/{MOD}/lang/en_us.json", lang_en)
    write(f"assets/{MOD}/lang/pt_br.json", lang_pt)
    print("resources generated")


if __name__ == "__main__":
    main()
