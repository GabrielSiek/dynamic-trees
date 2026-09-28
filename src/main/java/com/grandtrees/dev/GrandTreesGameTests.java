package com.grandtrees.dev;

import com.grandtrees.GrandTrees;
import com.grandtrees.item.TreeFelling;
import com.grandtrees.registry.ModItems;
import com.grandtrees.template.TreeTemplate;
import com.grandtrees.tree.GrandTreeGrower;
import com.grandtrees.tree.SizeDetector;
import com.grandtrees.tree.TreeCanvas;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.TreeSpecies;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dev-only GameTests (run with {@code ./gradlew runGameTestServer}). Every species x size is grown on
 * its own platform far from the test area; each tree is checked for decaying leaves and rendered to
 * run/gametest-previews/ for visual review.
 */
@GameTestHolder(GrandTrees.MODID)
@PrefixGameTestTemplate(false)
public final class GrandTreesGameTests {
    private static final String TEMPLATE = "empty";
    private static final String TEMPLATE_ID = "grandtrees:empty";
    private static final int SPACING = 64;
    private static final int GROUND = 200;

    /**
     * Every leaf position any grand tree has placed (world-gen runs on worker threads). Vanilla
     * trees generate their own decaying leaves - e.g. swamp oak blob corners - so the world-gen
     * audit only judges leaves we placed.
     */
    private static final Set<Long> PLACED_LEAVES = ConcurrentHashMap.newKeySet();

    static {
        GrandTreeGrower.setPlacementListener(canvas -> {
            for (long key : canvas.keys(TreeCanvas.Kind.LEAF)) PLACED_LEAVES.add(key);
        });
    }

    @GameTestGenerator
    public static Collection<TestFunction> growEverySpeciesAndSize() {
        List<TestFunction> tests = new ArrayList<>();
        for (TreeSpecies species : TreeSpecies.values()) {
            for (TreeSize size : TreeSize.values()) {
                String name = "grow_" + species.getSerializedName() + "_" + size.getSerializedName();
                tests.add(new TestFunction("grandtrees", name, TEMPLATE_ID, 200, 0, true,
                        helper -> growAndCheck(helper, species, size)));
            }
        }
        return tests;
    }

    private static void growAndCheck(GameTestHelper helper, TreeSpecies species, TreeSize size) {
        ServerLevel level = helper.getLevel();
        BlockPos center = new BlockPos(4000 + species.ordinal() * SPACING, GROUND, 4000 + size.ordinal() * SPACING);
        platform(level, center, 30);

        int half = (size.width - 1) / 2;
        long seed = 1234L + species.ordinal() * 31L + size.ordinal();
        long t0 = System.nanoTime();
        boolean ok = GrandTreeGrower.tryGrow(level, center.offset(-half, 0, -half), size.width, species, size, seed,
                null, GrandTreeGrower.noConsumables(), false);
        double ms = (System.nanoTime() - t0) / 1e6;
        if (!ok) {
            helper.fail("tree was blocked on an empty platform: " + GrandTreeGrower.lastFailure());
            return;
        }

        Stats stats = scan(level, center, size);
        TreePreview.render(level, center, size.maxRadius + 3, GROUND - 4, GROUND + previewHeight(size),
                species.getSerializedName() + "_" + size.getSerializedName());
        GrandTrees.LOGGER.info("[gametest] {} {}: {} logs, {} leaves, {} persistent, {} decaying, {} ms",
                species.getSerializedName(), size.getSerializedName(), stats.logs, stats.leaves, stats.persistent,
                stats.decaying, String.format("%.1f", ms));
        if (stats.logs == 0 || stats.leaves == 0) {
            helper.fail("tree has no logs or leaves");
        } else if (stats.decaying > 0) {
            helper.fail(stats.decaying + " leaves would decay");
        } else if (COVERED_CROWNS.contains(species) && topWood(level, center, size) > 0) {
            helper.fail(topWood(level, center, size) + " columns show wood on top of the crown");
        } else {
            helper.succeed();
        }
    }

    /** Species whose crowns must never show wood from above. */
    private static final Set<TreeSpecies> COVERED_CROWNS = Set.of(TreeSpecies.SPRUCE, TreeSpecies.ACACIA, TreeSpecies.DARK_OAK);

    /** Columns whose highest block is a log (wood visible from above), ignoring the root flare at the base. */
    private static int topWood(ServerLevel level, BlockPos center, TreeSize size) {
        int r = size.maxRadius + 3, count = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++)
                for (int y = GROUND + 75; y >= GROUND; y--) {
                    BlockState s = level.getBlockState(m.set(center.getX() + x, y, center.getZ() + z));
                    if (s.isAir() || s.is(Blocks.VINE) || s.is(Blocks.CAVE_VINES) || s.is(Blocks.CAVE_VINES_PLANT)) continue;
                    if (s.is(BlockTags.LOGS) && y >= GROUND + 3) {
                        count++;
                        GrandTrees.LOGGER.info("[gametest] wood on top at {}", m.immutable());
                    }
                    break;
                }
        return count;
    }

    private static int previewHeight(TreeSize size) {
        return switch (size) {
            case SMALL -> 14;
            case MEDIUM -> 22;
            case BIG -> 32;
            case LARGE -> 44;
            case EXTRA_LARGE -> 75;
        };
    }

    /**
     * A 5x5 vanilla sapling pattern grown through the sapling block itself (so through Forge's
     * sapling event); all saplings must be used up.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void saplingPatternGrowsExtraLarge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = new BlockPos(3000, GROUND, 3000);
        platform(level, center, 30);
        SaplingBlock block = (SaplingBlock) Blocks.OAK_SAPLING;
        BlockState sapling = block.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++)
                level.setBlock(center.offset(x, 0, z), sapling, 3);

        SizeDetector.Pattern pattern = SizeDetector.detect(level, center, block, TreeSize.EXTRA_LARGE, true);
        if (pattern.size() != TreeSize.EXTRA_LARGE) {
            helper.fail("expected extra_large, detected " + pattern.size().getSerializedName());
            return;
        }
        block.advanceTree(level, center.offset(1, 0, -2), sapling, level.getRandom());
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++)
                if (level.getBlockState(center.offset(x, 0, z)).is(block)) {
                    helper.fail("sapling left behind at " + x + "," + z);
                    return;
                }
        if (!level.getBlockState(center.above(3)).is(BlockTags.LOGS)) {
            helper.fail("no trunk above the pattern center");
            return;
        }
        helper.succeed();
    }

    /** 4x4 without corners is a round Large pattern; 3x3 plus stray saplings stays Big. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void roundAndSquareDetection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos a = new BlockPos(3200, GROUND, 3000);
        BlockPos b = new BlockPos(3300, GROUND, 3000);
        platform(level, a, 6);
        platform(level, b, 6);
        Block block = Blocks.SPRUCE_SAPLING;
        BlockState s = block.defaultBlockState();
        for (int x = 0; x < 4; x++)
            for (int z = 0; z < 4; z++)
                if (!((x == 0 || x == 3) && (z == 0 || z == 3))) level.setBlock(a.offset(x, 0, z), s, 3);
        for (int x = 0; x < 3; x++)
            for (int z = 0; z < 3; z++)
                level.setBlock(b.offset(x, 0, z), s, 3);
        level.setBlock(b.offset(3, 0, 0), s, 3);

        TreeSize round = SizeDetector.detect(level, a.offset(1, 0, 1), block, TreeSize.EXTRA_LARGE, true).size();
        TreeSize square = SizeDetector.detect(level, b.offset(1, 0, 1), block, TreeSize.EXTRA_LARGE, true).size();
        TreeSize roundOff = SizeDetector.detect(level, a.offset(1, 0, 1), block, TreeSize.EXTRA_LARGE, false).size();
        if (round != TreeSize.LARGE) helper.fail("round 4x4 detected as " + round.getSerializedName());
        else if (square != TreeSize.BIG) helper.fail("3x3 detected as " + square.getSerializedName());
        // Without round patterns the circle has no complete 3x3 square inside it, only 2x2s.
        else if (roundOff != TreeSize.MEDIUM) helper.fail("round disabled, expected medium, got " + roundOff.getSerializedName());
        else helper.succeed();
    }

    /**
     * Grows a tree, captures it the way a Structure Block would, loads it back as a template and
     * grows the template elsewhere (randomly rotated/mirrored): same wood count, no decaying leaves.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void templateRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos a = new BlockPos(3400, GROUND, 3000);
        BlockPos b = new BlockPos(3500, GROUND, 3000);
        platform(level, a, 20);
        platform(level, b, 20);
        if (!GrandTreeGrower.tryGrow(level, a.offset(-1, 0, -1), 3, TreeSpecies.OAK, TreeSize.BIG, 99L, null,
                GrandTreeGrower.noConsumables(), false)) {
            helper.fail("source tree did not grow");
            return;
        }
        BlockPos min = a.offset(-16, -3, -16);
        StructureTemplate captured = new StructureTemplate();
        captured.fillFromWorld(level, min, new Vec3i(33, 45, 33), false, Blocks.STRUCTURE_VOID);
        CompoundTag nbt = captured.save(new CompoundTag());

        TreeTemplate template = TreeTemplate.fromNbt("gametest:oak_big", nbt, TreeSpecies.OAK, TreeSize.BIG, 1, null);
        if (template == null) {
            helper.fail("template did not parse");
            return;
        }
        if (!GrandTreeGrower.growTemplate(level, b.offset(-1, 0, -1), 3, template, 7L, false)) {
            helper.fail("template was blocked: " + GrandTreeGrower.lastFailure());
            return;
        }
        Stats src = scan(level, a, TreeSize.BIG);
        Stats copy = scan(level, b, TreeSize.BIG);
        TreePreview.render(level, b, TreeSize.BIG.maxRadius + 3, GROUND - 4, GROUND + 40, "template_copy");
        GrandTrees.LOGGER.info("[gametest] template round trip: source {} logs / {} leaves, copy {} logs / {} leaves",
                src.logs, src.leaves, copy.logs, copy.leaves);
        if (copy.logs < src.logs * 0.9) helper.fail("copy lost logs: " + copy.logs + " vs " + src.logs);
        else if (copy.decaying > 0) helper.fail(copy.decaying + " template leaves would decay");
        else if (!level.getBlockState(b.above(2)).is(BlockTags.LOGS)) helper.fail("template trunk not on the anchor");
        else helper.succeed();
    }

    /** The operator command grows a tree at the given position. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void placeCommand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = new BlockPos(3600, GROUND, 3000);
        platform(level, at, 16);
        var source = level.getServer().createCommandSourceStack().withLevel(level).withPermission(4).withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(source,
                "grandtrees place " + at.getX() + " " + at.getY() + " " + at.getZ() + " dark_oak big 42");
        if (scan(level, at, TreeSize.BIG).logs == 0) helper.fail("command did not grow a tree");
        else helper.succeed();
    }

    /**
     * A vanilla 2x2 dark oak grows a Medium grand tree instead of the vanilla dark oak (grand trees
     * are recognised by their *_wood blocks, which vanilla trees never place).
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void vanillaDarkOakGrowsGrandTree(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = new BlockPos(3700, GROUND, 3000);
        platform(level, at, 30);
        SaplingBlock block = (SaplingBlock) Blocks.DARK_OAK_SAPLING;
        BlockState sapling = block.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        for (int x = 0; x < 2; x++)
            for (int z = 0; z < 2; z++)
                level.setBlock(at.offset(x, 0, z), sapling, 3);
        block.advanceTree(level, at, sapling, level.getRandom());

        int wood = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -12; x <= 12; x++)
            for (int z = -12; z <= 12; z++)
                for (int y = -3; y <= 30; y++)
                    if (level.getBlockState(m.set(at.getX() + x, at.getY() + y, at.getZ() + z)).is(Blocks.DARK_OAK_WOOD)) wood++;
        if (level.getBlockState(at).is(block)) helper.fail("sapling did not grow");
        else if (wood == 0) helper.fail("grew a vanilla dark oak, not a grand tree");
        else helper.succeed();
    }

    /** Every decoration (fallen log, bush, rock) of every species places on flat ground; previews for review. */
    @GameTestGenerator
    public static Collection<TestFunction> placeEveryDecoration() {
        List<TestFunction> tests = new ArrayList<>();
        String[] kinds = {"fallen_log", "bush", "rock"};
        for (TreeSpecies species : TreeSpecies.values()) {
            for (int k = 0; k < kinds.length; k++) {
                String id = species.getSerializedName() + "_" + kinds[k];
                BlockPos at = new BlockPos(5000 + species.ordinal() * 40, GROUND, 5000 + k * 40);
                tests.add(new TestFunction("grandtrees", "decor_" + id, TEMPLATE_ID, 100, 0, true,
                        helper -> placeDecoration(helper, id, at)));
            }
        }
        return tests;
    }

    private static void placeDecoration(GameTestHelper helper, String id, BlockPos at) {
        ServerLevel level = helper.getLevel();
        var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
        var feature = registry.get(new net.minecraft.resources.ResourceLocation(GrandTrees.MODID, id));
        if (feature == null) {
            // Not every species has every decoration (no rocks in swamps, no bushes in cherry groves).
            helper.succeed();
            return;
        }
        platform(level, at, 12);
        boolean placed = false;
        for (int seed = 0; seed < 8 && !placed; seed++) {
            placed = feature.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(seed), at);
        }
        TreePreview.render(level, at, 8, GROUND - 3, GROUND + 8, "decor_" + id);
        if (!placed) helper.fail(id + " did not place on flat grass");
        else helper.succeed();
    }

    /** The Lumber Saw fells a whole Big oak, roots included; only the cut block itself is left. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sawFellsWholeTree(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = new BlockPos(3800, GROUND, 3000);
        platform(level, at, 30);
        if (!GrandTreeGrower.tryGrow(level, at.offset(-1, 0, -1), 3, TreeSpecies.OAK, TreeSize.BIG, 5L, null,
                GrandTreeGrower.noConsumables(), false)) {
            helper.fail("tree did not grow: " + GrandTreeGrower.lastFailure());
            return;
        }
        int before = scan(level, at, TreeSize.BIG).logs;
        int felled = TreeFelling.fell(level, at.above(2), null, new ItemStack(ModItems.LUMBER_SAW.get()));
        int after = scan(level, at, TreeSize.BIG).logs;
        GrandTrees.LOGGER.info("[gametest] saw: {} logs, felled {}, {} left", before, felled, after);
        if (after != 1) helper.fail(after + " logs left after felling (expected only the cut block)");
        else helper.succeed();
    }

    /** Logs without natural leaves (a log cabin wall) are not felled. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sawSparesLogBuildings(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = new BlockPos(3900, GROUND, 3000);
        platform(level, at, 30);
        for (int x = 0; x < 5; x++)
            for (int y = 0; y < 4; y++)
                level.setBlock(at.offset(x, y, 0), Blocks.OAK_LOG.defaultBlockState(), 3);
        level.setBlock(at.offset(2, 4, 0), Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(BlockStateProperties.PERSISTENT, true), 3);
        int felled = TreeFelling.fell(level, at.offset(2, 1, 0), null, new ItemStack(ModItems.LUMBER_SAW.get()));
        if (felled != 0) helper.fail("felled " + felled + " logs of a wall");
        else helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 2400)
    public static void worldgenForest(GameTestHelper helper) {
        worldgenIn(helper, Biomes.FOREST, TreeSpecies.OAK, "worldgen_forest");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 2400)
    public static void worldgenTaiga(GameTestHelper helper) {
        worldgenIn(helper, Biomes.TAIGA, TreeSpecies.SPRUCE, "worldgen_taiga");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 2400)
    public static void worldgenSwamp(GameTestHelper helper) {
        worldgenIn(helper, Biomes.SWAMP, TreeSpecies.WILLOW, "worldgen_swamp");
    }

    /**
     * Finds the biome, generates a 9x9 chunk area around it and counts our wood blocks (vanilla
     * world generation never places *_wood, so any found come from grand trees).
     */
    private static void worldgenIn(GameTestHelper helper, ResourceKey<Biome> biome, TreeSpecies species, String preview) {
        Block wood = species.wood();
        ServerLevel level = helper.getLevel();
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(h -> h.is(biome), new BlockPos(0, 64, 0), 6400, 32, 64);
        if (found == null) {
            helper.fail("no " + biome.location() + " found");
            return;
        }
        ChunkPos center = new ChunkPos(found.getFirst());
        int woods = 0, grandLeaves = 0, vanillaLeaves = 0, persistent = 0, decaying = 0, rockBlocks = 0, mossCarpets = 0;
        java.util.Map<String, Integer> vanillaKinds = new java.util.TreeMap<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int cx = -4; cx <= 4; cx++) {
            for (int cz = -4; cz <= 4; cz++) {
                LevelChunk chunk = level.getChunk(center.x + cx, center.z + cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                        for (int y = top; y > top - 70 && y > level.getMinBuildHeight(); y--) {
                            BlockState s = chunk.getBlockState(m.set(x, y, z));
                            if (s.is(wood)) woods++;
                            if (s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE)) rockBlocks++;
                            if (s.is(Blocks.MOSS_CARPET)) mossCarpets++;
                            if (s.hasProperty(BlockStateProperties.DISTANCE)) {
                                long world = BlockPos.asLong(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                                if (!PLACED_LEAVES.contains(world)) {
                                    if (level.getBiome(BlockPos.of(world)).is(biome) && !nearGround(chunk, m)) {
                                        vanillaLeaves++;
                                        vanillaKinds.merge(s.getBlock().getDescriptionId(), 1, Integer::sum);
                                    }
                                    continue;
                                }
                                grandLeaves++;
                                if (s.getValue(BlockStateProperties.PERSISTENT)) persistent++;
                                else if (s.getValue(BlockStateProperties.DISTANCE) >= 7) {
                                    decaying++;
                                    GrandTrees.LOGGER.info("[gametest] decaying {} at {}", s, BlockPos.of(world));
                                }
                            }
                        }
                    }
                }
            }
        }
        BlockPos c = center.getMiddleBlockPosition(0);
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
        TreePreview.render(level, c.atY(surface), 72, surface - 20, surface + 60, preview);
        // Vanilla leaves inside the biome itself are trees that should have been removed (a few can spill
        // over from a neighbouring biome; bushes near the ground are ours and not counted).
        GrandTrees.LOGGER.info("[gametest] {} at {}: {} {} blocks, {} grand leaves ({} persistent, {} decaying), "
                        + "{} vanilla leaves in the biome {}, decorations: {} cobblestone blocks, {} moss carpets",
                biome.location(), found.getFirst(), woods, wood, grandLeaves, persistent, decaying, vanillaLeaves, vanillaKinds,
                rockBlocks, mossCarpets);
        if (woods == 0) helper.fail("no grand trees generated in " + biome.location());
        else if (decaying > 0) helper.fail(decaying + " grand-tree leaves would decay");
        else helper.succeed();
    }

    /** A leaf with solid ground at most 4 blocks below it (through leaves and logs) belongs to a bush. */
    private static boolean nearGround(LevelChunk chunk, BlockPos leaf) {
        for (int d = 1; d <= 4; d++) {
            BlockState s = chunk.getBlockState(leaf.below(d));
            if (s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced()) continue;
            return true;
        }
        return false;
    }

    private static void platform(ServerLevel level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -4; y <= -1; y++) {
                    level.setBlock(center.offset(x, y, z), y == -1 ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : Blocks.DIRT.defaultBlockState(), 2);
                }
            }
        }
    }

    private record Stats(int logs, int leaves, int persistent, int decaying) {
    }

    private static Stats scan(ServerLevel level, BlockPos center, TreeSize size) {
        int r = size.maxRadius + 3;
        int logs = 0, leaves = 0, persistent = 0, decaying = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++)
                for (int y = GROUND - 4; y <= GROUND + 75; y++) {
                    BlockState s = level.getBlockState(m.set(center.getX() + x, y, center.getZ() + z));
                    if (s.is(BlockTags.LOGS)) logs++;
                    if (s.hasProperty(BlockStateProperties.DISTANCE)) {
                        leaves++;
                        boolean p = s.getValue(BlockStateProperties.PERSISTENT);
                        if (p) persistent++;
                        else if (s.getValue(BlockStateProperties.DISTANCE) >= 7) decaying++;
                    }
                }
        return new Stats(logs, leaves, persistent, decaying);
    }
}
