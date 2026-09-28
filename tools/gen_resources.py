"""Generates the mod's JSON resources (world generation, biome tags, the Lumber Saw's item files).

Run after changing species, biome or decoration lists: python tools/gen_resources.py
The output is committed, so the mod builds without running this script.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
MOD = "grandtrees"

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

# Size weights for natural generation (capped further by the config's worldgen.maxSize). Medium and
# Big make up the forest; a placement that has no room shrinks, so Small trees fill the gaps. Large
# and Extra Large are rare so they stand spread out. Each chunk makes several placements
# (worldgen.treesPerChunk), so Extra Large ends up in roughly one of every dozen forest chunks.
WORLDGEN_SIZES = {"small": 3, "medium": 60, "big": 60, "large": 7, "extra_large": 2}

# Vanilla tree features removed from the grand-tree biomes (config worldgen.replaceVanillaTrees).
VANILLA_TREES = [
    "minecraft:trees_birch_and_oak", "minecraft:trees_flower_forest", "minecraft:trees_birch",
    "minecraft:birch_tall", "minecraft:trees_taiga", "minecraft:trees_old_growth_pine_taiga",
    "minecraft:trees_old_growth_spruce_taiga", "minecraft:trees_grove", "minecraft:trees_windswept_forest",
    "minecraft:trees_jungle", "minecraft:trees_sparse_jungle", "minecraft:bamboo_vegetation",
    "minecraft:trees_savanna", "minecraft:trees_windswept_savanna", "minecraft:dark_forest_vegetation",
    "minecraft:trees_cherry", "minecraft:trees_swamp",
]

# Forest-floor decorations per species, vanilla blocks only (config worldgen.decorations).
#   fallen_log: trunk lying on the ground; "rarity": one attempt every N chunks
#   bush: one log with a small leaf clump (like the vanilla jungle bush); "count" per chunk
#   rock: partly buried boulder of mixed stone, weights per block; "rarity" as above
#   huge_mushrooms: vanilla huge mushrooms (the dark forest lost them with its vanilla trees)
DECOR = {
    "oak": {
        "fallen_log": {"log": "oak_log", "length": [4, 7], "moss": 0.45, "mushroom": 0.15, "stump": 0.5, "rarity": 2},
        "bush": {"log": "oak_log", "leaves": {"oak_leaves": 5, "azalea_leaves": 1}, "count": 2},
        "rock": {"blocks": {"mossy_cobblestone": 3, "cobblestone": 2, "stone": 2, "andesite": 1},
                 "radius": [0.8, 2.0], "moss": 0.3, "rarity": 5},
    },
    "birch": {
        "fallen_log": {"log": "birch_log", "length": [4, 7], "moss": 0.2, "mushroom": 0.1, "stump": 0.4, "rarity": 3},
        "bush": {"log": "birch_log", "leaves": {"birch_leaves": 1}, "count": 1},
        "rock": {"blocks": {"stone": 3, "andesite": 2, "cobblestone": 1},
                 "radius": [0.8, 1.8], "moss": 0.1, "rarity": 7},
    },
    "spruce": {
        "fallen_log": {"log": "spruce_log", "length": [5, 8], "moss": 0.55, "mushroom": 0.2, "stump": 0.5, "rarity": 2},
        "bush": {"log": "spruce_log", "leaves": {"spruce_leaves": 1}, "count": 1},
        "rock": {"blocks": {"mossy_cobblestone": 3, "cobblestone": 2, "stone": 2, "andesite": 1},
                 "radius": [0.8, 2.2], "moss": 0.35, "rarity": 3},
    },
    "jungle": {
        "fallen_log": {"log": "jungle_log", "length": [5, 8], "moss": 0.6, "mushroom": 0.1, "stump": 0.3, "rarity": 2},
        "bush": {"log": "jungle_log", "leaves": {"oak_leaves": 1}, "count": 6},
        "rock": {"blocks": {"mossy_cobblestone": 3, "stone": 1},
                 "radius": [0.8, 1.8], "moss": 0.5, "rarity": 8},
    },
    "acacia": {
        "fallen_log": {"log": "acacia_log", "length": [3, 6], "moss": 0, "mushroom": 0, "stump": 0.6, "rarity": 4},
        "bush": {"log": "acacia_log", "leaves": {"acacia_leaves": 1}, "count": 1},
        "rock": {"blocks": {"granite": 3, "stone": 2, "andesite": 1},
                 "radius": [0.8, 1.8], "moss": 0, "rarity": 4},
    },
    "dark_oak": {
        "fallen_log": {"log": "dark_oak_log", "length": [4, 7], "moss": 0.6, "mushroom": 0.35, "stump": 0.5, "rarity": 2},
        "bush": {"log": "dark_oak_log", "leaves": {"dark_oak_leaves": 1}, "count": 3},
        "rock": {"blocks": {"mossy_cobblestone": 3, "cobblestone": 1},
                 "radius": [0.8, 1.8], "moss": 0.5, "rarity": 8},
        "huge_mushrooms": {"rarity": 3},
    },
    "cherry": {
        "fallen_log": {"log": "cherry_log", "length": [4, 6], "moss": 0.1, "mushroom": 0, "stump": 0.4, "rarity": 3},
        "rock": {"blocks": {"stone": 3, "andesite": 2},
                 "radius": [0.8, 1.8], "moss": 0.1, "rarity": 5},
    },
    "willow": {
        "fallen_log": {"log": "mangrove_log", "length": [4, 7], "moss": 0.5, "mushroom": 0.3, "stump": 0.3, "rarity": 2},
        "bush": {"log": "oak_log", "leaves": {"oak_leaves": 1}, "count": 1},
    },
}

SAPLING_SURVIVES = {"type": "minecraft:block_predicate_filter", "predicate": {
    "type": "minecraft:would_survive",
    "state": {"Name": "minecraft:oak_sapling", "Properties": {"stage": "0"}},
}}
DECORATIONS_ENABLED = {"type": f"{MOD}:decorations_enabled"}

LANG = {
    "en_us": {
        f"item.{MOD}.lumber_saw": "Lumber Saw",
        f"item.{MOD}.lumber_saw.tooltip": "Fells the whole tree. Sneak to cut a single block.",
    },
    "pt_br": {
        f"item.{MOD}.lumber_saw": "Serra de Lenhador",
        f"item.{MOD}.lumber_saw.tooltip": "Derruba a árvore inteira. Agache para cortar um bloco só.",
    },
}


def write(rel, data):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def mc(name):
    return name if ":" in name else f"minecraft:{name}"


def leaves_state(name):
    return {"Name": mc(name), "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}


def surface(heightmap="MOTION_BLOCKING_NO_LEAVES"):
    return [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": heightmap}]


def write_decorations(s, decor):
    """Writes one species' decoration features; returns (vegetal ids, local_modification ids)."""
    vegetal, local = [], []

    if "fallen_log" in decor:
        d = decor["fallen_log"]
        write(f"data/{MOD}/worldgen/configured_feature/{s}_fallen_log.json", {
            "type": f"{MOD}:fallen_log",
            "config": {"log": {"Name": mc(d["log"]), "Properties": {"axis": "y"}},
                       "min_length": d["length"][0], "max_length": d["length"][1],
                       "moss_chance": d["moss"], "mushroom_chance": d["mushroom"], "stump_chance": d["stump"]},
        })
        write(f"data/{MOD}/worldgen/placed_feature/{s}_fallen_log.json", {
            "feature": f"{MOD}:{s}_fallen_log",
            "placement": [DECORATIONS_ENABLED, {"type": "minecraft:rarity_filter", "chance": d["rarity"]}]
                         + surface() + [{"type": "minecraft:biome"}],
        })
        vegetal.append(f"{MOD}:{s}_fallen_log")

    if "bush" in decor:
        d = decor["bush"]
        foliage = ({"type": "minecraft:simple_state_provider", "state": leaves_state(next(iter(d["leaves"])))}
                   if len(d["leaves"]) == 1 else
                   {"type": "minecraft:weighted_state_provider",
                    "entries": [{"data": leaves_state(b), "weight": w} for b, w in d["leaves"].items()]})
        write(f"data/{MOD}/worldgen/configured_feature/{s}_bush.json", {
            "type": "minecraft:tree",
            "config": {
                "decorators": [],
                "dirt_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:dirt"}},
                "foliage_placer": {"type": "minecraft:bush_foliage_placer", "height": 2, "offset": 1, "radius": 2},
                "foliage_provider": foliage,
                "force_dirt": False,
                "ignore_vines": False,
                "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 0, "lower_size": 0, "upper_size": 0},
                "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 1,
                                 "height_rand_a": 0, "height_rand_b": 0},
                "trunk_provider": {"type": "minecraft:simple_state_provider",
                                   "state": {"Name": mc(d["log"]), "Properties": {"axis": "y"}}},
            },
        })
        write(f"data/{MOD}/worldgen/placed_feature/{s}_bush.json", {
            "feature": f"{MOD}:{s}_bush",
            "placement": [DECORATIONS_ENABLED, {"type": "minecraft:count", "count": d["count"]}]
                         + surface("OCEAN_FLOOR")
                         + [{"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
                            SAPLING_SURVIVES, {"type": "minecraft:biome"}],
        })
        vegetal.append(f"{MOD}:{s}_bush")

    if "huge_mushrooms" in decor:
        d = decor["huge_mushrooms"]
        for color in ("red", "brown"):
            write(f"data/{MOD}/worldgen/placed_feature/{s}_huge_{color}_mushroom.json", {
                "feature": f"minecraft:huge_{color}_mushroom",
                "placement": [DECORATIONS_ENABLED, {"type": "minecraft:rarity_filter", "chance": d["rarity"]}]
                             + surface() + [{"type": "minecraft:biome"}],
            })
            vegetal.append(f"{MOD}:{s}_huge_{color}_mushroom")

    if "rock" in decor:
        d = decor["rock"]
        write(f"data/{MOD}/worldgen/configured_feature/{s}_rock.json", {
            "type": f"{MOD}:boulder",
            "config": {
                "blocks": {"type": "minecraft:weighted_state_provider",
                           "entries": [{"data": {"Name": mc(b)}, "weight": w} for b, w in d["blocks"].items()]},
                "min_radius": d["radius"][0], "max_radius": d["radius"][1], "moss_chance": d["moss"],
            },
        })
        write(f"data/{MOD}/worldgen/placed_feature/{s}_rock.json", {
            "feature": f"{MOD}:{s}_rock",
            "placement": [DECORATIONS_ENABLED, {"type": "minecraft:rarity_filter", "chance": d["rarity"]}]
                         + surface() + [{"type": "minecraft:biome"}],
        })
        local.append(f"{MOD}:{s}_rock")

    return vegetal, local


def write_saw():
    write(f"assets/{MOD}/models/item/lumber_saw.json", {
        "parent": "minecraft:item/handheld",
        "textures": {"layer0": f"{MOD}:item/lumber_saw"},
    })
    write(f"data/{MOD}/recipes/lumber_saw.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": ["III", "SP "],
        "key": {"I": {"tag": "forge:ingots/iron"}, "S": {"item": "minecraft:stick"}, "P": {"tag": "minecraft:planks"}},
        "result": {"item": f"{MOD}:lumber_saw"},
    })
    write(f"data/{MOD}/advancements/recipes/tools/lumber_saw.json", {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_iron": {"trigger": "minecraft:inventory_changed",
                         "conditions": {"items": [{"tag": "forge:ingots/iron"}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": f"{MOD}:lumber_saw"}},
        },
        "requirements": [["has_iron", "has_the_recipe"]],
        "rewards": {"recipes": [f"{MOD}:lumber_saw"]},
    })
    for lang, entries in LANG.items():
        write(f"assets/{MOD}/lang/{lang}.json", entries)


def main():
    for s in BIOMES:
        write(f"data/{MOD}/worldgen/configured_feature/{s}.json", {
            "type": f"{MOD}:grand_tree",
            "config": {"species": s, "sizes": WORLDGEN_SIZES},
        })
        write(f"data/{MOD}/worldgen/placed_feature/{s}.json", {
            "feature": f"{MOD}:{s}",
            "placement": [
                {"type": f"{MOD}:config_count", "species": s},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
                SAPLING_SURVIVES,
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

        vegetal, local = write_decorations(s, DECOR.get(s, {}))
        if vegetal:
            write(f"data/{MOD}/forge/biome_modifier/decorate_{s}.json", {
                "type": "forge:add_features",
                "biomes": f"#{MOD}:has_grand_{s}",
                "features": vegetal,
                "step": "vegetal_decoration",
            })
        if local:
            write(f"data/{MOD}/forge/biome_modifier/rocks_{s}.json", {
                "type": "forge:add_features",
                "biomes": f"#{MOD}:has_grand_{s}",
                "features": local,
                "step": "local_modifications",
            })

    write(f"data/{MOD}/tags/worldgen/biome/replaces_vanilla_trees.json", {
        "replace": False,
        "values": [f"#{MOD}:has_grand_{s}" for s in BIOMES],
    })
    write(f"data/{MOD}/forge/biome_modifier/remove_vanilla_trees.json", {
        "type": f"{MOD}:remove_vanilla_trees",
        "biomes": f"#{MOD}:replaces_vanilla_trees",
        "features": VANILLA_TREES,
    })
    write_saw()
    print("resources generated")


if __name__ == "__main__":
    main()
