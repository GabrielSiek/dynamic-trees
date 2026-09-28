package com.grandtrees.tree;

import com.grandtrees.GrandTrees;
import com.grandtrees.config.GrandTreesConfig;
import com.grandtrees.template.TreeTemplate;
import com.grandtrees.template.TreeTemplates;
import com.grandtrees.tree.TreeCanvas.Kind;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Builds a tree plan (procedural or template), validates it against the world and commits it. */
public final class GrandTreeGrower {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Why the most recent {@link #tryGrow} refused to place, for command feedback and debugging. */
    private static volatile String lastFailure = "";

    /** Dev hook: receives every committed tree (used by GameTests to audit placed leaves). */
    @Nullable
    private static volatile java.util.function.Consumer<TreeCanvas> placementListener;

    public static void setPlacementListener(@Nullable java.util.function.Consumer<TreeCanvas> listener) {
        placementListener = listener;
    }

    private GrandTreeGrower() {
    }

    /** Sapling entry point: tries the detected size first, then smaller patterns if there is no room. */
    public static boolean growFromSapling(ServerLevel level, BlockPos pos, Block sapling, TreeSpecies species, RandomSource random) {
        TreeSize max = TreeSize.EXTRA_LARGE;
        boolean allowRound = GrandTreesConfig.allowRoundPatterns();
        while (max != null) {
            SizeDetector.Pattern pattern = SizeDetector.detect(level, pos, sapling, max, allowRound);
            if (tryGrow(level, pattern.corner(), pattern.size().width, species, pattern.size(), random.nextLong(),
                    null, pattern.cells(), false)) {
                return true;
            }
            max = pattern.size().smaller();
        }
        return false;
    }

    /**
     * @param corner     min corner of the sapling pattern; the trunk is centered on the pattern
     * @param width      pattern width (1-5)
     * @param clip       world-gen write region, or null for no limit
     * @param consumable positions (saplings) that are cleared/overwritten when the tree grows
     * @param force      skip the obstruction limits (commands)
     */
    public static boolean tryGrow(LevelAccessor level, BlockPos corner, int width, TreeSpecies species, TreeSize size,
                                  long seed, @Nullable BoundingBox clip, LongSet consumable, boolean force) {
        return grow(level, corner, width, species, size, seed, clip, consumable, force, null);
    }

    /** Grows one specific template, bypassing the weighted choice (tests and tooling). */
    public static boolean growTemplate(LevelAccessor level, BlockPos corner, int width, TreeTemplate template, long seed,
                                       boolean force) {
        return grow(level, corner, width, template.species(), template.size(), seed, null, noConsumables(), force, template);
    }

    private static boolean grow(LevelAccessor level, BlockPos corner, int width, TreeSpecies species, TreeSize size,
                                long seed, @Nullable BoundingBox clip, LongSet consumable, boolean force,
                                @Nullable TreeTemplate forced) {
        long start = System.nanoTime();
        RandomSource random = RandomSource.create(seed);
        Vec3 base = new Vec3(corner.getX() + width / 2.0, corner.getY(), corner.getZ() + width / 2.0);
        TreeCanvas canvas = new TreeCanvas();
        TreeContext ctx = new TreeContext(level, clip, random, species, size, TreeParams.roll(size, random), base, canvas);

        String source = "procedural";
        TreeTemplate template = forced != null ? forced : pickTemplate(species, size, random);
        if (template != null) {
            template.placeInto(ctx);
            source = "template " + template.id();
        } else {
            species.style().generate(ctx);
        }

        boolean ok = place(ctx, consumable, force);
        if (GrandTreesConfig.debugTiming()) {
            GrandTrees.LOGGER.info("[grandtrees] {} {} {} ({} blocks) at {} -> {} in {} ms",
                    size.getSerializedName(), species.getSerializedName(), source, canvas.size(), corner,
                    ok ? "placed" : "blocked", (System.nanoTime() - start) / 1_000_000.0);
        }
        return ok;
    }

    @Nullable
    private static TreeTemplate pickTemplate(TreeSpecies species, TreeSize size, RandomSource random) {
        List<TreeTemplate> templates = TreeTemplates.get(species, size);
        if (templates.isEmpty()) return null;
        int total = 0;
        for (TreeTemplate t : templates) total += t.weight();
        int roll = random.nextInt(total + GrandTreesConfig.proceduralWeight());
        for (TreeTemplate t : templates) {
            roll -= t.weight();
            if (roll < 0) return t;
        }
        return null;
    }

    /** Blocks a tree may grow into. */
    public static boolean isReplaceable(BlockState s) {
        if (s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.SAPLINGS)) {
            return true;
        }
        return s.canBeReplaced() && s.getFluidState().isEmpty();
    }

    /** Ground that roots may sink into. */
    public static boolean isGround(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY) || s.is(Blocks.SNOW_BLOCK);
    }

    private static boolean place(TreeContext ctx, LongSet consumable, boolean force) {
        LevelAccessor level = ctx.level;
        TreeCanvas canvas = ctx.canvas;
        BoundingBox clip = ctx.clip;
        int baseY = ctx.baseY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        // 1. Validate against the world and drop what cannot be placed.
        int logs = 0, logsBlocked = 0, leaves = 0, leavesBlocked = 0;
        BlockState firstBlocker = null;
        BlockPos firstBlockedAt = null;
        LongArrayList drop = new LongArrayList();
        for (long key : canvas.keys()) {
            m.set(key);
            if (!inBounds(level, clip, m)) {
                drop.add(key);
                continue;
            }
            Kind kind = canvas.kind(key);
            BlockState world = level.getBlockState(m);
            boolean ok = consumable.contains(key) || isReplaceable(world) || (kind == Kind.ROOT && isGround(world));
            if (kind == Kind.LOG) {
                logs++;
                if (!ok) logsBlocked++;
            } else if (kind == Kind.LEAF && m.getY() >= baseY + 2) {
                leaves++;
                if (!ok) leavesBlocked++;
            }
            if (!ok) {
                drop.add(key);
                if (firstBlocker == null && kind != Kind.DECOR) {
                    firstBlocker = world;
                    firstBlockedAt = m.immutable();
                }
            }
        }
        if (!force && (logsBlocked > Math.max(2, logs * 0.03) || leavesBlocked > leaves * 0.35)) {
            lastFailure = String.format("%d/%d logs and %d/%d leaves obstructed (first: %s at %s)",
                    logsBlocked, logs, leavesBlocked, leaves, firstBlocker, firstBlockedAt);
            return false;
        }
        for (long key : drop) canvas.remove(key);

        // 2. Leaf distances (hidden twigs where needed, so vanilla decay leaves the tree alone).
        canvas.fixLeaves(key -> {
            BlockPos p = BlockPos.of(key);
            return inBounds(level, clip, p) && isReplaceable(level.getBlockState(p));
        }, ctx::log);

        // 3. Clear consumed saplings that the tree does not cover.
        for (long key : consumable) {
            if (!canvas.has(key)) level.setBlock(m.set(key), Blocks.AIR.defaultBlockState(), FLAGS);
        }

        // 4. Wood and leaves.
        LongArrayList decor = new LongArrayList();
        for (long key : canvas.keys()) {
            Kind kind = canvas.kind(key);
            if (kind == Kind.DECOR) {
                decor.add(key);
                continue;
            }
            m.set(key);
            BlockState state = canvas.get(key);
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                state = state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(m).is(FluidTags.WATER)
                        && level.getFluidState(m).isSource());
            }
            level.setBlock(m, state, FLAGS);
        }

        // 5. Grass under wood turns to dirt, like vanilla trees.
        for (long key : canvas.keys()) {
            if (!canvas.isWood(key)) continue;
            long below = BlockPos.offset(key, Direction.DOWN);
            if (canvas.has(below)) continue;
            m.set(below);
            if (!inBounds(level, clip, m)) continue;
            BlockState s = level.getBlockState(m);
            if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.MYCELIUM) || s.is(Blocks.PODZOL)) {
                level.setBlock(m, Blocks.DIRT.defaultBlockState(), FLAGS);
            }
        }

        // 6. Decorations top-down, so hanging chains find their support already placed.
        decor.sort((a, b) -> Integer.compare(BlockPos.getY(b), BlockPos.getY(a)));
        BoundingBox inner = clip == null ? null : clip.inflatedBy(-1);
        for (long key : decor) {
            m.set(key);
            if (!inBounds(level, inner, m)) continue;
            BlockState state = canvas.get(key);
            if (isReplaceable(level.getBlockState(m)) && level.getFluidState(m).isEmpty() && state.canSurvive(level, m)) {
                level.setBlock(m, state, FLAGS);
            }
        }
        java.util.function.Consumer<TreeCanvas> listener = placementListener;
        if (listener != null) listener.accept(canvas);
        return true;
    }

    private static boolean inBounds(LevelAccessor level, @Nullable BoundingBox clip, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return false;
        return clip == null || clip.isInside(pos);
    }

    public static String lastFailure() {
        return lastFailure;
    }

    public static LongSet noConsumables() {
        return LongSets.EMPTY_SET;
    }
}
