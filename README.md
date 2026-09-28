# Grand Trees

A Forge 1.20.1 mod that grows big, natural-looking trees: kinked and twisted trunks, flared roots,
clumpy crowns, weeping curtains. Plant more saplings to grow a bigger tree.

## Getting a tree

Plant a **vanilla sapling**. Every sapling below grows a grand tree instead of the vanilla one.

| Sapling | Style |
|---|---|
| Oak | round broadleaf, clumpy crown |
| Birch | tall and slender |
| Spruce | tall conifer with drooping tiers |
| Jungle | giant on buttress roots, vines, cocoa |
| Acacia | leaning trunk, wide flat pads |
| Dark Oak | twisted fantasy trunk, glow berries |
| Cherry | wide blossom clouds, petals on the ground |
| Oak, planted in a swamp | willow: weeping curtains, mangrove-wood trunk |

Mangrove and azalea saplings grow their vanilla trees as usual.

## Sizes: plant a pattern

Plant saplings of the same kind in a pattern, then wait or use bone meal on any of them. The
largest complete pattern wins, and all of its saplings are used up.

| Pattern | Size | Height |
|---|---|---|
| 1 sapling | Small | 6-9 |
| 2x2 | Medium | 10-15 |
| 3x3 | Big | 16-22 |
| 4x4, or 4x4 without the corners | Large | 23-32 |
| 5x5, or 5x5 without the corners | Extra Large | 33-46 |

A single dark oak sapling does not grow, just like in vanilla: dark oak starts at 2x2 (Medium).

If there is not enough room, for example under a ceiling or next to another trunk, the tree tries
the next smaller size, and it does not grow at all if even Small does not fit. Grown trees keep
their leaves; after you chop the trunk, the leaves decay like vanilla leaves.

## Natural generation

In **newly generated chunks**, grand trees replace the vanilla trees of these biomes:

- oak: forest, flower forest
- birch: birch forests
- spruce: taiga, grove, windswept forest
- jungle: jungles
- acacia: savannas
- dark oak: dark forest (its huge mushrooms go too, they come from the same vanilla feature)
- cherry: cherry grove
- willow: swamp

Most natural trees are Medium and Big. Small trees fill the gaps between them, Large ones are a
little rare, and Extra Large ones are rare and stand spread out (roughly one every dozen forest
chunks). The weights are in `data/grandtrees/worldgen/configured_feature/<species>.json`.

Other biomes (plains, meadows, mangrove swamps, ...) keep their vanilla trees.

Chunks that already exist are never changed, so adding the mod to an existing world is safe;
you'll find grand trees when you explore new land.

The biome lists are the tags `grandtrees:has_grand_<species>`, so a data pack can add modded
biomes (Biomes O' Plenty, Terralith, ...) to them. Vanilla trees are only removed from vanilla
biomes; a modded biome keeps its own trees next to the grand ones.

## Forest floor

The grand-tree biomes also get vanilla-block decorations in new chunks:

| Biome | Fallen trunks | Bushes | Rocks |
|---|---|---|---|
| Forest | oak, mossy, mushrooms | oak with some azalea leaves | mossy cobblestone, cobblestone, stone |
| Birch forest | birch | birch | stone, andesite |
| Taiga, grove | spruce, mossy | spruce | mossy cobblestone, cobblestone, stone (common) |
| Jungle | jungle, very mossy | many jungle bushes | mossy cobblestone (rare) |
| Savanna | acacia, dry, with stumps | acacia | granite, stone, andesite |
| Dark forest | dark oak, mossy, mushrooms | dark oak | mossy cobblestone; huge red and brown mushrooms |
| Cherry grove | cherry | none | stone, andesite |
| Swamp | mangrove, mossy, mushrooms | oak | none |

Fallen trunks sometimes lie next to the broken stump they fell from. Snowy biomes cover them in
snow like any other block. The amounts are in `tools/gen_resources.py` (`DECOR`).

## Lumber Saw

Crafted from 3 iron ingots (top row), a stick and a plank (second row). Cutting a log of a natural
tree fells the whole tree, roots included, and the wood drops at your feet. Sneak to cut a single
block. Leaves are left to decay on their own.

Only natural trees fall: the logs have to touch leaves that can decay, and leaves a player placed
never decay, so log cabins are cut one block at a time. A cut costs 1 durability per 8 logs
(750 durability, about 4 of the biggest trees). The saw is not an axe for other mods, so axe-based
tree choppers don't also react to it.

## Config

`config/grandtrees-common.toml`:

- `worldgen.enabled`: turn natural generation on or off.
- `worldgen.replaceVanillaTrees`: remove vanilla trees from the grand-tree biomes (default on;
  needs a restart).
- `worldgen.decorations`: fallen trunks, bushes, rocks and huge mushrooms (default on).
- `worldgen.maxSize`: the largest size world generation places (default `EXTRA_LARGE`).
- `worldgen.treesPerChunk.<species>`: grand trees attempted per chunk. `2.5` means 2 or 3.
  Attempts that would overlap another tree grow smaller or are skipped.
- `growth.vanillaSaplings`: vanilla saplings grow grand trees (default on).
- `growth.allowRoundPatterns`: accept the corner-less 4x4 and 5x5 patterns.
- `growth.proceduralWeight`: how often procedural trees are chosen over your templates.
- `saw.maxLogs`: the most logs one Lumber Saw cut fells (default 2048).
- `debug.logTreeTiming`: log every tree with its block count and generation time.

## Commands (operators)

- `/grandtrees place <x y z> <species> <size> [seed]` grows a tree instantly at the position,
  ignoring obstructions. The same seed always gives the same tree.
- `/grandtrees templates` shows how many hand-built templates are loaded.

## Adding your own hand-built trees (templates)

Hand-built trees mix into sapling growth and world generation next to the procedural ones.

**Easiest: the config folder.** No data pack or JSON is needed.

1. Build the tree in a creative world, then save it with a **Structure Block** (Save mode,
   max 48x48x48). The file lands in `saves/<world>/generated/minecraft/structures/`.
2. Copy the `.nbt` into `config/grandtrees/structures/<species>/<size>/`, for example
   `config/grandtrees/structures/oak/large/my_oak.nbt`. The mod creates these folders on the
   first launch.
3. Run `/reload` or restart.

The trunk base is found automatically: it's the center of the lowest logs. Ground blocks (dirt,
grass, stone, sand) in the save are ignored, and leaves are turned into natural (decaying) leaves.
Each placement is randomly rotated and mirrored.

**Data pack / modpack option.** Add `data/<ns>/structures/trees/my_oak.nbt` plus
`data/<ns>/grandtrees/templates/my_oak.json`:

```json
{ "species": "oak", "size": "large", "weight": 3,
  "structure": "<ns>:trees/my_oak", "anchor": [7, 0, 7] }
```

`anchor` (the trunk base inside the structure) is optional.

**Trees downloaded from the internet.** Schematics (`.schem`, `.schematic`) have to be converted
first: paste them in a creative world with WorldEdit and save them again with a Structure Block.
Only use builds whose license lets you redistribute them. Paid tree packs (Patreon and similar)
can't be bundled into a public CurseForge mod or modpack.

## Building from source

Requires JDK 17.

```
./gradlew build                # -> build/libs/grandtrees-<version>.jar
./gradlew runClient            # dev client
./gradlew runGameTestServer    # automated tests (see below)
```

`runGameTestServer` does the following:

- Grows every species at every size and checks that no leaf would decay, and that spruce,
  acacia and dark oak crowns show no wood from above.
- Grows vanilla saplings (a 5x5 oak, a 2x2 dark oak) into grand trees and checks pattern detection.
- Checks the command, and a template round trip (capture, reload, place).
- Fells a tree with the Lumber Saw and checks a log wall is left standing.
- Places every decoration of every species and renders it.
- Generates real forest, taiga and swamp chunks, audits every leaf the mod placed and counts
  vanilla leaves left in the biome (should be 0).
- Writes a front and top view of each tree to `run/gametest-previews/`.

Delete `run/world` before each run so old trees don't block new ones.

`tools/gen_resources.py` regenerates the world generation JSON, biome tags and the Lumber Saw files.

"# dinamyc-trees" 
