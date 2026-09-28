package com.grandtrees.worldgen;

import com.grandtrees.tree.GrandTreeGrower;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** A rough, partly buried rock of mixed stone, sometimes with moss on top. */
public class BoulderFeature extends Feature<BoulderConfiguration> {
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public BoulderFeature() {
        super(BoulderConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<BoulderConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BoulderConfiguration config = ctx.config();
        BlockPos origin = ctx.origin();
        if (!level.getFluidState(origin).isEmpty() || !isSoil(level.getBlockState(origin.below()))) return false;

        double r = config.minRadius() + random.nextDouble() * Math.max(0, config.maxRadius() - config.minRadius());
        double rx = r * (0.9 + random.nextDouble() * 0.4);
        double ry = r * (0.65 + random.nextDouble() * 0.3);
        double rz = r * (0.9 + random.nextDouble() * 0.4);
        double cx = origin.getX() + 0.5, cz = origin.getZ() + 0.5;
        double cy = origin.getY() + 0.5 - ry * 0.35; // about a third sits in the ground

        int minX = (int) Math.floor(cx - rx), maxX = (int) Math.floor(cx + rx);
        int minZ = (int) Math.floor(cz - rz), maxZ = (int) Math.floor(cz + rz);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int top = origin.getY() - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = (int) Math.floor(cy - ry); y <= (int) Math.floor(cy + ry); y++) {
                    double nx = (x + 0.5 - cx) / rx, ny = (y + 0.5 - cy) / ry, nz = (z + 0.5 - cz) / rz;
                    double jag = 1 + (random.nextDouble() - 0.5) * 0.35;
                    if (nx * nx + ny * ny + nz * nz > jag * jag) continue;
                    m.set(x, y, z);
                    BlockState world = level.getBlockState(m);
                    boolean ok = y < origin.getY() ? isSoil(world)
                            : (world.isAir() || world.canBeReplaced()) && world.getFluidState().isEmpty();
                    if (!ok) continue;
                    level.setBlock(m, config.blocks().getState(random, m), FLAGS);
                    top = Math.max(top, y);
                }
            }
        }
        if (top < origin.getY()) return false;

        if (config.mossChance() > 0) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = top + 1; y >= origin.getY(); y--) {
                        m.set(x, y, z);
                        if (!level.isEmptyBlock(m)) break;
                        BlockState below = level.getBlockState(m.below());
                        if (isRock(below)) {
                            if (random.nextFloat() < config.mossChance()) {
                                level.setBlock(m, Blocks.MOSS_CARPET.defaultBlockState(), FLAGS);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }

    /** Ground a rock may sink into. */
    private static boolean isSoil(BlockState s) {
        return GrandTreeGrower.isGround(s) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.MOSS_BLOCK);
    }

    private static boolean isRock(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE);
    }
}
