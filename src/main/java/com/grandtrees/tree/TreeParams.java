package com.grandtrees.tree;

import net.minecraft.util.RandomSource;

/**
 * Size-dependent base numbers every style scales from.
 *
 * @param height        overall tree height in blocks
 * @param trunkRadius   trunk radius at the base (0.5 = 1 block, 2.5 = round 5x5)
 * @param canopyRadius  horizontal reach of the crown from the trunk center
 * @param branches      number of main branches
 * @param roots         number of surface roots (0 for small trees)
 */
public record TreeParams(int height, double trunkRadius, double canopyRadius, int branches, int roots) {

    public static TreeParams roll(TreeSize size, RandomSource r) {
        return switch (size) {
            case SMALL -> new TreeParams(between(r, 6, 9), 0.5, range(r, 3.0, 4.2), between(r, 2, 3), 0);
            case MEDIUM -> new TreeParams(between(r, 10, 15), 1.0, range(r, 5.0, 6.5), between(r, 3, 4), between(r, 0, 2));
            case BIG -> new TreeParams(between(r, 16, 22), 1.5, range(r, 7.0, 9.0), between(r, 4, 6), between(r, 3, 4));
            case LARGE -> new TreeParams(between(r, 23, 32), range(r, 2.0, 2.3), range(r, 10.0, 13.0), between(r, 5, 7), between(r, 4, 6));
            case EXTRA_LARGE -> new TreeParams(between(r, 33, 46), range(r, 2.5, 3.1), range(r, 14.0, 17.5), between(r, 7, 9), between(r, 6, 8));
        };
    }

    /** Leaf-cluster radius: capped so cluster centers stay within leaf-decay distance of a log. */
    public double clusterRadius() {
        return Math.max(2.2, Math.min(5.0, canopyRadius * 0.38));
    }

    private static int between(RandomSource r, int min, int max) {
        return min + r.nextInt(max - min + 1);
    }

    private static double range(RandomSource r, double min, double max) {
        return min + r.nextDouble() * (max - min);
    }
}
