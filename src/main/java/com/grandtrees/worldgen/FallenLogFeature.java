package com.grandtrees.worldgen;

import com.grandtrees.tree.GrandTreeGrower;
import com.grandtrees.tree.TreeCanvas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * A trunk lying on the forest floor, sometimes next to the broken stump it fell from, with moss and
 * mushrooms growing on it. Only placed where the whole trunk rests on flat ground.
 */
public class FallenLogFeature extends Feature<FallenLogConfiguration> {
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public FallenLogFeature() {
        super(FallenLogConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<FallenLogConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        FallenLogConfiguration config = ctx.config();
        BlockPos origin = ctx.origin();

        Direction dir = random.nextBoolean() ? Direction.EAST : Direction.SOUTH;
        int len = config.minLength() + random.nextInt(Math.max(1, config.maxLength() - config.minLength() + 1));
        BlockPos start = origin.relative(dir.getOpposite(), len / 2);

        int supported = 0;
        for (int i = 0; i < len; i++) {
            BlockPos p = start.relative(dir, i);
            if (!isFree(level, p)) return false;
            if (GrandTreeGrower.isGround(level.getBlockState(p.below()))) supported++;
        }
        // The ends may overhang a dip by one block each; the rest lies on the ground.
        if (supported < len - 2 || !GrandTreeGrower.isGround(level.getBlockState(start.relative(dir, len / 2).below()))) {
            return false;
        }

        BlockState log = TreeCanvas.withAxis(config.log(), dir.getAxis());
        for (int i = 0; i < len; i++) {
            BlockPos p = start.relative(dir, i);
            clearPlant(level, p);
            level.setBlock(p, log, FLAGS);
            decorateTop(level, p.above(), config, random);
            if (random.nextFloat() < config.mossChance() * 0.3F) {
                BlockPos side = p.relative(random.nextBoolean() ? dir.getClockWise() : dir.getCounterClockWise());
                if (level.isEmptyBlock(side) && GrandTreeGrower.isGround(level.getBlockState(side.below()))) {
                    level.setBlock(side, Blocks.MOSS_CARPET.defaultBlockState(), FLAGS);
                }
            }
        }

        if (random.nextFloat() < config.stumpChance()) {
            BlockPos s = start.relative(dir.getOpposite(), 2);
            s = s.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.getX(), s.getZ()));
            if (Math.abs(s.getY() - origin.getY()) <= 1 && isFree(level, s)
                    && GrandTreeGrower.isGround(level.getBlockState(s.below()))) {
                int h = 1 + random.nextInt(2);
                BlockState stump = TreeCanvas.withAxis(config.log(), Direction.Axis.Y);
                int placed = 0;
                while (placed < h && isFree(level, s.above(placed))) {
                    clearPlant(level, s.above(placed));
                    level.setBlock(s.above(placed), stump, FLAGS);
                    placed++;
                }
                decorateTop(level, s.above(placed), config, random);
            }
        }
        return true;
    }

    private static void decorateTop(WorldGenLevel level, BlockPos above, FallenLogConfiguration config, RandomSource random) {
        if (!level.isEmptyBlock(above)) return;
        if (random.nextFloat() < config.mossChance()) {
            level.setBlock(above, Blocks.MOSS_CARPET.defaultBlockState(), FLAGS);
        } else if (random.nextFloat() < config.mushroomChance()) {
            level.setBlock(above, (random.nextBoolean() ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM).defaultBlockState(), FLAGS);
        }
    }

    /** Air or a small plant; not water and not another log. */
    private static boolean isFree(WorldGenLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return (s.isAir() || s.canBeReplaced()) && s.getFluidState().isEmpty() && !s.is(BlockTags.LOGS);
    }

    /** Removes the upper half of a tall plant too, so none is left floating. */
    private static void clearPlant(WorldGenLevel level, BlockPos p) {
        if (level.getBlockState(p).getBlock() instanceof DoublePlantBlock) {
            level.setBlock(p.above(), Blocks.AIR.defaultBlockState(), FLAGS);
        }
    }
}
