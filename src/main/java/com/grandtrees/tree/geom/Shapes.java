package com.grandtrees.tree.geom;

import com.grandtrees.tree.TreeCanvas;
import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Voxel drawing primitives shared by all tree styles. */
public final class Shapes {
    private Shapes() {
    }

    // ---------------------------------------------------------------- wood

    /**
     * Draws a tapered limb from a to b. Thick sections are filled as discs (near-vertical) or balls;
     * thin sections become a face-connected 1-block line so branches read as natural staircases.
     */
    public static void limb(TreeContext c, Vec3 a, Vec3 b, double ra, double rb, Kind kind) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        Direction.Axis axis = dominant(d);
        boolean vertical = len < 1e-6 || Math.abs(d.y) / len > 0.75;
        int steps = Math.max(1, (int) Math.ceil(len / 0.35));
        int lx = Integer.MIN_VALUE, ly = 0, lz = 0;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = a.x + d.x * t, y = a.y + d.y * t, z = a.z + d.z * t;
            double r = ra + (rb - ra) * t;
            if (r < 0.8) {
                int bx = floor(x), by = floor(y), bz = floor(z);
                if (lx == Integer.MIN_VALUE) {
                    c.canvas.put(bx, by, bz, woodState(c, r, axis, kind), kind);
                } else {
                    // Step one axis at a time so consecutive blocks always share a face.
                    while (lx != bx || ly != by || lz != bz) {
                        if (lx != bx && (Math.abs(bx - lx) >= Math.abs(bz - lz) || lz == bz)) lx += Integer.signum(bx - lx);
                        else if (lz != bz) lz += Integer.signum(bz - lz);
                        else ly += Integer.signum(by - ly);
                        c.canvas.put(lx, ly, lz, woodState(c, r, axis, kind), kind);
                    }
                }
                lx = bx;
                ly = by;
                lz = bz;
            } else {
                lx = Integer.MIN_VALUE;
                if (vertical) disc(c, x, floor(y), z, r, woodState(c, r, axis, kind), kind);
                else ball(c, x, y, z, r, woodState(c, r, axis, kind), kind);
            }
        }
    }

    private static BlockState woodState(TreeContext c, double r, Direction.Axis axis, Kind kind) {
        return r >= 1.0 || kind == Kind.ROOT ? c.wood() : c.log(axis);
    }

    /** Cubic Bezier limb with radius tapering from r0 to r1. */
    public static void bezier(TreeContext c, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double r0, double r1, Kind kind) {
        double approx = p0.distanceTo(p1) + p1.distanceTo(p2) + p2.distanceTo(p3);
        int n = Math.max(3, (int) Math.ceil(approx / 1.5));
        Vec3 prev = p0;
        for (int i = 1; i <= n; i++) {
            double t = (double) i / n;
            Vec3 cur = bez(p0, p1, p2, p3, t);
            limb(c, prev, cur, lerp(r0, r1, (double) (i - 1) / n), lerp(r0, r1, t), kind);
            prev = cur;
        }
    }

    public static Vec3 bez(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double u = 1 - t;
        double a = u * u * u, b = 3 * u * u * t, cc = 3 * u * t * t, d = t * t * t;
        return new Vec3(a * p0.x + b * p1.x + cc * p2.x + d * p3.x,
                a * p0.y + b * p1.y + cc * p2.y + d * p3.y,
                a * p0.z + b * p1.z + cc * p2.z + d * p3.z);
    }

    /** Smooth Catmull-Rom path through the points, radius tapering by path length. */
    public static void path(TreeContext c, List<Vec3> pts, double r0, double r1, Kind kind) {
        List<Vec3> samples = catmullRom(pts, 6);
        double total = 0;
        for (int i = 1; i < samples.size(); i++) total += samples.get(i).distanceTo(samples.get(i - 1));
        double acc = 0;
        for (int i = 1; i < samples.size(); i++) {
            Vec3 a = samples.get(i - 1), b = samples.get(i);
            double seg = a.distanceTo(b);
            double ra = lerp(r0, r1, total == 0 ? 0 : acc / total);
            acc += seg;
            double rb = lerp(r0, r1, total == 0 ? 1 : acc / total);
            limb(c, a, b, ra, rb, kind);
        }
    }

    public static List<Vec3> catmullRom(List<Vec3> pts, int perSegment) {
        List<Vec3> out = new ArrayList<>();
        if (pts.size() < 2) {
            out.addAll(pts);
            return out;
        }
        out.add(pts.get(0));
        for (int i = 0; i < pts.size() - 1; i++) {
            Vec3 p0 = pts.get(Math.max(0, i - 1)), p1 = pts.get(i), p2 = pts.get(i + 1), p3 = pts.get(Math.min(pts.size() - 1, i + 2));
            for (int s = 1; s <= perSegment; s++) {
                double t = (double) s / perSegment, t2 = t * t, t3 = t2 * t;
                out.add(new Vec3(
                        cr(p0.x, p1.x, p2.x, p3.x, t, t2, t3),
                        cr(p0.y, p1.y, p2.y, p3.y, t, t2, t3),
                        cr(p0.z, p1.z, p2.z, p3.z, t, t2, t3)));
            }
        }
        return out;
    }

    private static double cr(double p0, double p1, double p2, double p3, double t, double t2, double t3) {
        return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    /** Evaluates a Catmull-Rom path at a height fraction, for attaching branches to a trunk. */
    public static Vec3 pointAtHeight(List<Vec3> pts, double y) {
        List<Vec3> s = catmullRom(pts, 6);
        for (int i = 1; i < s.size(); i++) {
            Vec3 a = s.get(i - 1), b = s.get(i);
            if ((a.y <= y && b.y >= y) || (a.y >= y && b.y <= y)) {
                double t = Math.abs(b.y - a.y) < 1e-6 ? 0 : (y - a.y) / (b.y - a.y);
                return new Vec3(lerp(a.x, b.x, t), y, lerp(a.z, b.z, t));
            }
        }
        return y <= s.get(0).y ? s.get(0) : s.get(s.size() - 1);
    }

    public static void disc(TreeContext c, double cx, int y, double cz, double r, BlockState state, Kind kind) {
        double r2 = r * r + 1e-6;
        for (int x = floor(cx - r); x <= floor(cx + r); x++) {
            for (int z = floor(cz - r); z <= floor(cz + r); z++) {
                double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
                if (dx * dx + dz * dz <= r2) c.canvas.put(x, y, z, state, kind);
            }
        }
    }

    public static void ball(TreeContext c, double cx, double cy, double cz, double r, BlockState state, Kind kind) {
        double r2 = r * r + 1e-6;
        for (int x = floor(cx - r); x <= floor(cx + r); x++)
            for (int y = floor(cy - r); y <= floor(cy + r); y++)
                for (int z = floor(cz - r); z <= floor(cz + r); z++) {
                    double dx = x + 0.5 - cx, dy = y + 0.5 - cy, dz = z + 0.5 - cz;
                    if (dx * dx + dy * dy + dz * dz <= r2) c.canvas.put(x, y, z, state, kind);
                }
    }

    /**
     * Round, flared trunk base plus surface roots that snake outward and dip into the ground.
     *
     * @param buttress start roots higher up the trunk (jungle-style buttress roots)
     */
    public static void rootFlare(TreeContext c, double trunkR, int count, double lengthScale, boolean buttress) {
        if (trunkR < 0.99) return;
        Vec3 b = c.base;
        int by = c.baseY();
        int layers = 1 + (int) Math.round(trunkR * 0.6);
        for (int layer = 0; layer < layers; layer++) {
            double r = trunkR + (1.1 - layer * (0.9 / layers)) * (0.5 + trunkR * 0.25);
            int y = by + layer;
            for (int x = floor(b.x - r - 1); x <= floor(b.x + r + 1); x++) {
                for (int z = floor(b.z - r - 1); z <= floor(b.z + r + 1); z++) {
                    double dx = x + 0.5 - b.x, dz = z + 0.5 - b.z;
                    double n = c.shapeNoise.getValue(x * 0.45, y * 0.45, z * 0.45);
                    double rr = r + n * 0.7;
                    if (dx * dx + dz * dz <= rr * rr) c.canvas.put(x, y, z, c.wood(), Kind.ROOT);
                }
            }
        }
        if (count <= 0) return;
        double a0 = c.randAngle();
        for (int i = 0; i < count; i++) {
            double ang = a0 + i * TreeContext.TAU / count + c.rand(-0.3, 0.3);
            Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
            double len = trunkR + c.rand(1.5, 3.0) * lengthScale * (1 + trunkR * 0.45);
            double startH = buttress ? trunkR * 2.0 + c.rand(1.5, 3.5) : trunkR * 0.8 + c.rand(0.3, 1.2);
            Vec3 s = b.add(dir.scale(trunkR * 0.5)).add(0, startH, 0);
            double ex = b.x + dir.x * len, ez = b.z + dir.z * len;
            int gy = c.groundY(ex, ez);
            Vec3 e = new Vec3(ex, gy - 0.6, ez);
            Vec3 c1 = b.add(dir.scale(trunkR * 0.95)).add(0, startH * 0.45, 0);
            Vec3 c2 = b.add(dir.scale(len * 0.65)).add(0, (gy - by) * 0.6 + 0.35, 0);
            bezier(c, s, c1, c2, e, Math.max(0.6, trunkR * 0.5), 0.5, Kind.ROOT);
        }
    }

    // ---------------------------------------------------------------- leaves

    /**
     * Noisy ellipsoid of leaves.
     *
     * @param flatBottom fraction of the lower half kept (1 = full ellipsoid, 0.3 = flat pad bottom)
     * @param roughness  how strongly noise erodes/extends the surface
     */
    public static void blob(TreeContext c, Vec3 center, double rx, double ry, double rz, double flatBottom, double roughness) {
        for (int x = floor(center.x - rx - 1); x <= floor(center.x + rx + 1); x++) {
            for (int y = floor(center.y - ry - 1); y <= floor(center.y + ry + 1); y++) {
                double ny = (y + 0.5 - center.y) / ry;
                if (ny < -flatBottom) continue;
                for (int z = floor(center.z - rz - 1); z <= floor(center.z + rz + 1); z++) {
                    double nx = (x + 0.5 - center.x) / rx, nz = (z + 0.5 - center.z) / rz;
                    double d = nx * nx + ny * ny + nz * nz;
                    double n = c.shapeNoise.getValue(x * 0.3, y * 0.3, z * 0.3);
                    double limit = 1 + roughness * n;
                    if (d <= limit * limit) c.canvas.put(x, y, z, c.leaf(x, y, z), Kind.LEAF);
                }
            }
        }
    }

    // ---------------------------------------------------------------- decorations

    /** Vines hanging from the sides of exposed leaves (they chain downward like vanilla vines). */
    public static void sideVines(TreeContext c, double chance, int maxLen) {
        TreeCanvas canvas = c.canvas;
        for (long leaf : canvas.keys(Kind.LEAF)) {
            if (c.random.nextDouble() >= chance) continue;
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(c.random);
            long start = BlockPos.offset(leaf, dir);
            if (canvas.has(start)) continue;
            hangVine(c, start, dir.getOpposite(), c.randInt(1, maxLen));
        }
    }

    /** Hangs a vine chain starting at {@code start}, attached toward {@code face}. */
    public static void hangVine(TreeContext c, long start, Direction face, int len) {
        BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(face), true);
        long pos = start;
        for (int i = 0; i < len; i++) {
            if (c.canvas.has(pos)) return;
            c.canvas.put(pos, vine, Kind.DECOR);
            pos = BlockPos.offset(pos, Direction.DOWN);
        }
    }

    /** Glow-berry vines hanging below leaves. */
    public static void caveVines(TreeContext c, double chance, int maxLen, double berryChance) {
        TreeCanvas canvas = c.canvas;
        for (long leaf : canvas.keys(Kind.LEAF)) {
            long below = BlockPos.offset(leaf, Direction.DOWN);
            if (canvas.has(below) || c.random.nextDouble() >= chance) continue;
            int want = c.randInt(1, maxLen);
            int len = 0;
            long p = below;
            while (len < want && !canvas.has(p)) {
                len++;
                p = BlockPos.offset(p, Direction.DOWN);
            }
            p = below;
            for (int i = 1; i <= len; i++) {
                boolean berries = c.random.nextDouble() < berryChance;
                BlockState s = i == len
                        ? Blocks.CAVE_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 25).setValue(CaveVines.BERRIES, berries)
                        : Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVines.BERRIES, berries);
                canvas.put(p, s, Kind.DECOR);
                p = BlockPos.offset(p, Direction.DOWN);
            }
        }
    }

    /** Short hanging leaf strands under the canopy that respect leaf-decay distance. */
    public static void leafStrands(TreeContext c, double chanceInner, double chanceOuter, int maxLeaves,
                                   int maxVines, Long2IntMap dist) {
        TreeCanvas canvas = c.canvas;
        double reach = c.params.canopyRadius();
        for (long leaf : canvas.keys(Kind.LEAF)) {
            long below = BlockPos.offset(leaf, Direction.DOWN);
            if (canvas.has(below)) continue;
            double hx = BlockPos.getX(leaf) + 0.5 - c.base.x, hz = BlockPos.getZ(leaf) + 0.5 - c.base.z;
            double frac = Math.min(1, Math.sqrt(hx * hx + hz * hz) / reach);
            if (c.random.nextDouble() >= lerp(chanceInner, chanceOuter, frac)) continue;

            int d0 = dist.get(leaf);
            int leaves = Math.min(c.randInt(0, maxLeaves), 6 - d0);
            long bottom = leaf;
            for (int i = 0; i < leaves; i++) {
                long next = BlockPos.offset(bottom, Direction.DOWN);
                if (canvas.has(next)) break;
                int x = BlockPos.getX(next), y = BlockPos.getY(next), z = BlockPos.getZ(next);
                canvas.put(next, c.leaf(x, y, z), Kind.LEAF);
                bottom = next;
            }
            if (maxVines > 0) {
                Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(c.random);
                long start = BlockPos.offset(bottom, dir);
                int ground = c.groundY(BlockPos.getX(start) + 0.5, BlockPos.getZ(start) + 0.5);
                int room = BlockPos.getY(start) - ground;
                int len = Math.min(c.randInt(1, maxVines), room);
                if (len > 0) hangVine(c, start, dir.getOpposite(), len);
            }
        }
    }

    /** Scattered pink petals on the ground under the canopy. */
    public static void petals(TreeContext c, int count, double radius) {
        for (int i = 0; i < count; i++) {
            double ang = c.randAngle(), r = Math.sqrt(c.random.nextDouble()) * radius;
            double x = c.base.x + Math.cos(ang) * r, z = c.base.z + Math.sin(ang) * r;
            int y = c.groundY(x, z);
            BlockState s = Blocks.PINK_PETALS.defaultBlockState()
                    .setValue(PinkPetalsBlock.AMOUNT, c.randInt(1, 4))
                    .setValue(PinkPetalsBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(c.random));
            c.canvas.put(floor(x), y, floor(z), s, Kind.DECOR);
        }
    }

    /** Cocoa pods on the trunk surface between two heights. */
    public static void cocoa(TreeContext c, int count, int minY, int maxY) {
        var logs = c.canvas.keys(Kind.LOG);
        if (logs.isEmpty()) return;
        int placed = 0;
        for (int tries = 0; tries < count * 12 && placed < count; tries++) {
            long log = logs.getLong(c.random.nextInt(logs.size()));
            int y = BlockPos.getY(log);
            if (y < minY || y > maxY) continue;
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(c.random);
            long pos = BlockPos.offset(log, dir);
            if (c.canvas.has(pos)) continue;
            c.canvas.put(pos, Blocks.COCOA.defaultBlockState()
                    .setValue(CocoaBlock.FACING, dir.getOpposite())
                    .setValue(CocoaBlock.AGE, c.randInt(0, 2)), Kind.DECOR);
            placed++;
        }
    }

    // ---------------------------------------------------------------- math

    public static Direction.Axis dominant(Vec3 d) {
        double ax = Math.abs(d.x), ay = Math.abs(d.y), az = Math.abs(d.z);
        if (ay >= ax && ay >= az) return Direction.Axis.Y;
        return ax >= az ? Direction.Axis.X : Direction.Axis.Z;
    }

    public static Vec3 horizontal(double angle) {
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static int floor(double v) {
        return (int) Math.floor(v);
    }
}
