package com.grandtrees.tree.style;

import com.grandtrees.tree.TreeCanvas;
import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import com.grandtrees.tree.geom.Shapes;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Building blocks shared by several styles. */
final class StyleUtil {
    private StyleUtil() {
    }

    /** Trunk control points with a few random kinks; the first point is the base. */
    static List<Vec3> kinkedTrunkPoints(TreeContext c, double trunkH, int kinks, double maxOffset) {
        List<Vec3> pts = new ArrayList<>();
        Vec3 base = c.base;
        pts.add(base);
        double ox = 0, oz = 0;
        for (int i = 1; i <= kinks; i++) {
            double h = trunkH * i / (kinks + 1.0);
            double ang = c.randAngle();
            double m = c.rand(0.4, 1.0) * maxOffset;
            ox += Math.cos(ang) * m;
            oz += Math.sin(ang) * m;
            pts.add(base.add(ox, h, oz));
        }
        pts.add(base.add(ox + c.rand(-0.5, 0.5), trunkH, oz + c.rand(-0.5, 0.5)));
        return pts;
    }

    /**
     * Main branches radiating from the upper trunk, arcing up and out. Tips are spread through the
     * crown's height so leaf clusters form several layers instead of one flat disc.
     *
     * @param reach    horizontal branch length (before the leaf cluster)
     * @param startLow lowest branch start, as a fraction of trunk height
     * @param span     crown height above the trunk top
     * @param endMin   lowest tip height, as a fraction of span
     * @param endMax   highest tip height, as a fraction of span
     * @param forks    add a side fork on each branch
     * @param mids     receives mid-branch points (for inner leaf clusters), may be null
     * @return branch tip positions
     */
    static List<Vec3> crownBranches(TreeContext c, List<Vec3> trunk, double trunkH, int count, double reach,
                                    double startLow, double span, double endMin, double endMax, double r0,
                                    boolean forks, List<Vec3> mids) {
        List<Vec3> ends = new ArrayList<>();
        double topY = c.base.y + trunkH;
        double a0 = c.randAngle();
        for (int i = 0; i < count; i++) {
            double ang = a0 + i * TreeContext.TAU / count + c.rand(-0.35, 0.35);
            double len = reach * c.rand(0.65, 1.0);
            double endY = topY + span * c.rand(endMin, endMax);
            double startY = c.base.y + trunkH * c.rand(startLow, 1.0);
            // Keep limbs from getting steeper than ~53 degrees: start them higher if needed.
            startY = Math.min(topY, Math.max(startY, endY - len * 1.3));
            Vec3 s = Shapes.pointAtHeight(trunk, startY);
            double rise = endY - startY;
            Vec3 dir = Shapes.horizontal(ang);
            Vec3 e = s.add(dir.scale(len)).add(0, rise, 0);
            Vec3 c1 = s.add(dir.scale(len * 0.25)).add(0, rise * 0.55, 0);
            Vec3 c2 = s.add(dir.scale(len * 0.65)).add(0, rise * 0.95, 0);
            double r = Math.max(0.5, r0 * c.rand(0.8, 1.0));
            Shapes.bezier(c, s, c1, c2, e, r, 0.5, Kind.LOG);
            ends.add(e);
            Vec3 mid = Shapes.bez(s, c1, c2, e, 0.55);
            if (mids != null) mids.add(mid);
            if (forks) {
                double fa = ang + (c.random.nextBoolean() ? 1 : -1) * c.rand(0.5, 0.9);
                double fl = len * c.rand(0.3, 0.45);
                Vec3 fe = mid.add(Math.cos(fa) * fl, fl * c.rand(0.2, 0.5), Math.sin(fa) * fl);
                Shapes.limb(c, mid, fe, Math.max(0.5, r * 0.5), 0.5, Kind.LOG);
                ends.add(fe);
            }
        }
        return ends;
    }

    /**
     * One flat layer of leaves. {@code ragged} is the chance of leaving out a cell on the outer
     * ring, which softens the outline without opening holes inside (cells next to the center are
     * always kept, so a trunk never shows through).
     */
    static void leafDisc(TreeContext c, double cx, int y, double cz, double r, double ragged) {
        double r2 = r * r + 1e-6;
        double inner = Math.max(1.0, r - 0.8);
        for (int x = Shapes.floor(cx - r); x <= Shapes.floor(cx + r); x++) {
            for (int z = Shapes.floor(cz - r); z <= Shapes.floor(cz + r); z++) {
                double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
                double d2 = dx * dx + dz * dz;
                if (d2 > r2) continue;
                if (d2 > inner * inner && c.random.nextDouble() < ragged) continue;
                c.canvas.put(x, y, z, c.leaf(x, y, z), Kind.LEAF);
            }
        }
    }

    /**
     * Covers every log that would show its top from above (the highest block of its column) with a
     * small cross of leaves, so the crown never shows wood on top.
     *
     * @param minY only logs at or above this height (the crown) are covered
     */
    static void coverTopWood(TreeContext c, double minY) {
        TreeCanvas canvas = c.canvas;
        Long2LongOpenHashMap highest = new Long2LongOpenHashMap();
        for (long key : canvas.keys()) {
            if (canvas.kind(key) == Kind.DECOR) continue;
            long column = BlockPos.asLong(BlockPos.getX(key), 0, BlockPos.getZ(key));
            if (!highest.containsKey(column) || BlockPos.getY(key) > BlockPos.getY(highest.get(column))) {
                highest.put(column, key);
            }
        }
        for (long key : highest.values()) {
            if (canvas.kind(key) != Kind.LOG || BlockPos.getY(key) < minY) continue;
            int x = BlockPos.getX(key), y = BlockPos.getY(key) + 1, z = BlockPos.getZ(key);
            canvas.put(x, y, z, c.leaf(x, y, z), Kind.LEAF);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int nx = x + d.getStepX(), nz = z + d.getStepZ();
                if (!canvas.has(nx, y, nz)) canvas.put(nx, y, nz, c.leaf(nx, y, nz), Kind.LEAF);
            }
        }
    }

    /** A leader limb from the trunk top up into the crown; returns its tip. */
    static Vec3 leader(TreeContext c, Vec3 top, double height, double r0) {
        Vec3 tip = top.add(c.rand(-1, 1), Math.max(1, height), c.rand(-1, 1));
        Shapes.limb(c, top, tip, Math.max(0.5, r0), 0.5, Kind.LOG);
        return tip;
    }
}
