package com.grandtrees.tree;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

/**
 * Finds the largest sapling pattern containing a given sapling. Sizes follow the pattern width:
 * 1 = Small, 2x2 = Medium, 3x3 = Big, 4x4 = Large, 5x5 = Extra Large. Large and Extra Large also
 * accept a round pattern: the square with its four corners left empty.
 */
public final class SizeDetector {
    private SizeDetector() {
    }

    /**
     * @param corner min-x/min-z corner of the pattern square (even for round patterns)
     * @param cells  every sapling that belongs to the pattern and will be consumed
     */
    public record Pattern(TreeSize size, BlockPos corner, LongSet cells) {
    }

    public static Pattern detect(LevelReader level, BlockPos pos, Block sapling, TreeSize max, boolean allowRound) {
        for (int n = max.width; n >= 2; n--) {
            TreeSize size = TreeSize.values()[n - 1];
            Pattern square = find(level, pos, sapling, n, size, false);
            if (square != null) return square;
            if (allowRound && n >= 4) {
                Pattern round = find(level, pos, sapling, n, size, true);
                if (round != null) return round;
            }
        }
        LongSet single = new LongOpenHashSet();
        single.add(pos.asLong());
        return new Pattern(TreeSize.SMALL, pos, single);
    }

    private static Pattern find(LevelReader level, BlockPos pos, Block sapling, int n, TreeSize size, boolean round) {
        for (int ox = 0; ox < n; ox++) {
            for (int oz = 0; oz < n; oz++) {
                if (round && isCorner(ox, oz, n)) continue;
                BlockPos corner = pos.offset(-ox, 0, -oz);
                LongSet cells = match(level, corner, sapling, n, round);
                if (cells != null) return new Pattern(size, corner, cells);
            }
        }
        return null;
    }

    private static LongSet match(LevelReader level, BlockPos corner, Block sapling, int n, boolean round) {
        LongSet cells = new LongOpenHashSet();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (round && isCorner(i, j, n)) continue;
                m.set(corner.getX() + i, corner.getY(), corner.getZ() + j);
                if (!level.getBlockState(m).is(sapling)) return null;
                cells.add(m.asLong());
            }
        }
        return cells;
    }

    private static boolean isCorner(int i, int j, int n) {
        return (i == 0 || i == n - 1) && (j == 0 || j == n - 1);
    }
}
