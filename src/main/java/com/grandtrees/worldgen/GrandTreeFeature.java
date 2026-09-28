package com.grandtrees.worldgen;

import com.grandtrees.config.GrandTreesConfig;
import com.grandtrees.tree.GrandTreeGrower;
import com.grandtrees.tree.TreeSize;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Places a grand tree during world generation. Features may only write to the 3x3 chunks around the
 * chunk being decorated, so the tree is clipped to that box and nudged toward the chunk center when
 * it would otherwise spill over the edge.
 */
public class GrandTreeFeature extends Feature<GrandTreeConfiguration> {

    public GrandTreeFeature() {
        super(GrandTreeConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<GrandTreeConfiguration> ctx) {
        if (!GrandTreesConfig.worldgenEnabled()) return false;
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        GrandTreeConfiguration config = ctx.config();

        TreeSize size = pickSize(config.sizes(), GrandTreesConfig.worldgenMaxSize(), random);
        if (size == null) return false;

        BlockPos origin = ctx.origin();
        ChunkPos chunk = new ChunkPos(origin);
        BoundingBox clip = new BoundingBox(
                chunk.getMinBlockX() - 16, level.getMinBuildHeight(), chunk.getMinBlockZ() - 16,
                chunk.getMaxBlockX() + 16, level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ() + 16);

        // Each placement is one spot (the placed feature makes several per chunk). Without room it
        // shrinks one size only, so crowded spots don't turn most of a forest into Small trees.
        if (tryAt(level, config, size, origin.getX(), origin.getZ(), clip, random)) return true;
        TreeSize smaller = size.smaller();
        return smaller != null && tryAt(level, config, smaller, origin.getX(), origin.getZ(), clip, random);
    }

    private static boolean tryAt(WorldGenLevel level, GrandTreeConfiguration config, TreeSize size, int ax, int az,
                                 BoundingBox clip, RandomSource random) {
        int r = size.maxRadius;
        int x = clampInto(ax, clip.minX() + r, clip.maxX() - r);
        int z = clampInto(az, clip.minZ() + r, clip.maxZ() - r);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos ground = new BlockPos(x, y, z);
        if (!level.getBlockState(ground.below()).is(BlockTags.DIRT) || !level.getFluidState(ground).isEmpty()) {
            return false;
        }
        int half = (size.width - 1) / 2;
        BlockPos corner = ground.offset(-half, 0, -half);
        return GrandTreeGrower.tryGrow(level, corner, size.width, config.species(), size, random.nextLong(),
                clip, GrandTreeGrower.noConsumables(), false);
    }

    private static int clampInto(int v, int min, int max) {
        return min > max ? (min + max) / 2 : Mth.clamp(v, min, max);
    }

    @Nullable
    private static TreeSize pickSize(Map<TreeSize, Integer> weights, TreeSize cap, RandomSource random) {
        int total = 0;
        for (Map.Entry<TreeSize, Integer> e : weights.entrySet()) {
            if (cap.atLeast(e.getKey())) total += e.getValue();
        }
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (Map.Entry<TreeSize, Integer> e : weights.entrySet()) {
            if (!cap.atLeast(e.getKey())) continue;
            roll -= e.getValue();
            if (roll < 0) return e.getKey();
        }
        return null;
    }
}
