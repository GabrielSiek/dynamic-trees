# Grand Trees

A Forge 1.20.1 mod that grows big, natural-looking trees: kinked and twisted trunks, flared roots,
clumpy crowns, weeping curtains. Plant more saplings to grow a bigger tree.

## Getting a tree

Craft a **Tuned Sapling** (shapeless): vanilla sapling + bone meal.

| Tuned sapling | Recipe | Style |
|---|---|---|
| Oak | oak sapling + bone meal | round broadleaf, clumpy crown |
| Birch | birch sapling + bone meal | tall and slender |
| Spruce | spruce sapling + bone meal | tall conifer with drooping tiers |
| Jungle | jungle sapling + bone meal | giant on buttress roots, vines, cocoa |
| Acacia | acacia sapling + bone meal | leaning trunk, wide flat pads |
| Dark Oak | dark oak sapling + bone meal | twisted fantasy trunk, glow berries |
| Cherry | cherry sapling + bone meal | wide blossom clouds, petals on the ground |
| Willow | oak sapling + vine + bone meal | weeping curtains, mangrove-wood trunk |

Leaves of vanilla trees also drop a Tuned sapling 1% of the time (not willow).

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

If there is not enough room, for example under a ceiling or next to another trunk, the tree tries
the next smaller size, and it does not grow at all if even Small does not fit. Grown trees keep
their leaves; after you chop the trunk, the leaves decay like vanilla leaves.

## Natural generation

Grand trees also generate in **newly generated chunks** of matching biomes:

- oak: forest, flower forest
- birch: birch forests
- spruce: taiga, grove, windswept forest
- jungle: jungles
- acacia: savannas
- dark oak: dark forest
- cherry: cherry grove
- willow: swamp

Chunks that already exist are never changed, so adding the mod to an existing world is safe;
you'll find grand trees when you explore new land.

The biome lists are the tags `grandtrees:has_grand_<species>`, so a data pack can add modded
biomes (Biomes O' Plenty, Terralith, ...) to them.

## Config

`config/grandtrees-common.toml`:

- `worldgen.enabled`: turn natural generation on or off.
- `worldgen.maxSize`: the largest size world generation places (default `LARGE`).
- `worldgen.chancePerChunk.<species>`: density, from 0 to 1.
- `growth.allowRoundPatterns`: accept the corner-less 4x4 and 5x5 patterns.
- `growth.proceduralWeight`: how often procedural trees are chosen over your templates.
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

- Grows every species at every size and checks that no leaf would decay.
- Checks sapling pattern detection, recipes and loot tables.
- Checks the command, and a template round trip (capture, reload, place).
- Generates real forest, taiga and swamp chunks and audits every leaf the mod placed.
- Writes a front and top view of each tree to `run/gametest-previews/`.

Delete `run/world` before each run so old trees don't block new ones.

The `tools/` folder has three helper scripts:

- `gen_resources.py` regenerates the JSON (models, recipes, loot, tags, world generation, lang).
- `gen_textures.py` regenerates the sapling textures from the vanilla sprites.
- `check_resources.py` checks that every model and texture reference resolves.
"# dinamyc-trees" 
