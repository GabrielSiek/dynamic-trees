package com.grandtrees.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Everything a style needs to draw one tree. */
public final class TreeContext {
    public static final double TAU = Math.PI * 2;

    public final LevelAccessor level;
    @Nullable
    public final BoundingBox clip;
    public final RandomSource random;
    public final TreeSpecies species;
    public final TreeSize size;
    public final TreeParams params;
    /** Center of the trunk base; y is the first trunk layer (the sapling layer). */
    public final Vec3 base;
    public final TreeCanvas canvas;
    public final SimplexNoise shapeNoise;
    private final SimplexNoise leafNoise;
    private final BlockState leaf;
    @Nullable
    private final BlockState secondaryLeaf;
    private final double secondaryThreshold;

    public TreeContext(LevelAccessor level, @Nullable BoundingBox clip, RandomSource random, TreeSpecies species,
                       TreeSize size, TreeParams params, Vec3 base, TreeCanvas canvas) {
        this.level = level;
        this.clip = clip;
        this.random = random;
        this.species = species;
        this.size = size;
        this.params = params;
        this.base = base;
        this.canvas = canvas;
        this.shapeNoise = new SimplexNoise(random);
        this.leafNoise = new SimplexNoise(random);
        this.leaf = species.leaves().defaultBlockState();
        Block secondary = species.secondaryLeaves();
        this.secondaryLeaf = secondary == null ? null : secondary.defaultBlockState();
        // Simplex output is roughly bell-shaped in [-1, 1]; this maps a fraction to a cutoff.
        this.secondaryThreshold = 0.9 - species.secondaryFraction * 3.0;
    }

    public BlockState log(Direction.Axis axis) {
        return TreeCanvas.withAxis(species.log().defaultBlockState(), axis);
    }

    public BlockState wood() {
        return species.wood().defaultBlockState();
    }

    /** Leaf for a position; secondary leaves appear in coherent noise patches. */
    public BlockState leaf(int x, int y, int z) {
        if (secondaryLeaf != null && leafNoise.getValue(x * 0.21, y * 0.21, z * 0.21) > secondaryThreshold) {
            return secondaryLeaf;
        }
        return leaf;
    }

    public double rand(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    public int randInt(int min, int max) {
        return max <= min ? min : min + random.nextInt(max - min + 1);
    }

    public double randAngle() {
        return random.nextDouble() * TAU;
    }

    public int baseY() {
        return (int) Math.floor(base.y);
    }

    /**
     * First air-like y above solid ground near (x, z), searched a few blocks around the base height.
     * Reads stay inside the clip box so world-gen never touches unloaded chunks.
     */
    public int groundY(double x, double z) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        int by = baseY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = by + 3; y >= by - 6; y--) {
            m.set(ix, y, iz);
            if (clip != null && !clip.isInside(m)) return by;
            if (level.isOutsideBuildHeight(m)) return by;
            BlockState s = level.getBlockState(m);
            if (!GrandTreeGrower.isReplaceable(s) && !s.is(net.minecraft.tags.BlockTags.LOGS)) return y + 1;
        }
        return by;
    }
}
