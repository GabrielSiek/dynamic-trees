package com.grandtrees.tree;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongPredicate;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * An in-memory plan of a tree. Styles and templates draw into it; nothing touches the world until
 * {@link GrandTreeGrower#place} validates and commits it.
 */
public final class TreeCanvas {

    public enum Kind {
        /** Trunk and branches. Blocked logs count against the obstruction limit. */
        LOG,
        /** Roots and flare: may replace dirt-like ground, never counted as obstruction. */
        ROOT,
        LEAF,
        /** Vines, petals, cocoa... placed last, only where they can survive. */
        DECOR
    }

    private static final Kind[] KINDS = Kind.values();
    private static final int MAX_LEAF_DISTANCE = 6;

    private final Long2ObjectOpenHashMap<BlockState> states = new Long2ObjectOpenHashMap<>();
    private final Long2ByteOpenHashMap kinds = new Long2ByteOpenHashMap();

    public TreeCanvas() {
        kinds.defaultReturnValue((byte) -1);
    }

    /** Writes with precedence LOG/ROOT > LEAF > DECOR; equal-rank writes keep the first block. */
    public void put(int x, int y, int z, BlockState state, Kind kind) {
        put(BlockPos.asLong(x, y, z), state, kind);
    }

    public void put(BlockPos pos, BlockState state, Kind kind) {
        put(pos.asLong(), state, kind);
    }

    public void put(long key, BlockState state, Kind kind) {
        Kind existing = kind(key);
        if (existing != null && rank(existing) >= rank(kind)) return;
        states.put(key, state);
        kinds.put(key, (byte) kind.ordinal());
    }

    /** Unconditional overwrite, used when post-processing replaces a block in place. */
    public void set(long key, BlockState state, Kind kind) {
        states.put(key, state);
        kinds.put(key, (byte) kind.ordinal());
    }

    private static int rank(Kind kind) {
        return switch (kind) {
            case LOG, ROOT -> 2;
            case LEAF -> 1;
            case DECOR -> 0;
        };
    }

    public boolean has(long key) {
        return kinds.containsKey(key);
    }

    public boolean has(int x, int y, int z) {
        return has(BlockPos.asLong(x, y, z));
    }

    @Nullable
    public Kind kind(long key) {
        byte k = kinds.get(key);
        return k < 0 ? null : KINDS[k];
    }

    public boolean isLeaf(long key) {
        return kind(key) == Kind.LEAF;
    }

    public boolean isWood(long key) {
        Kind k = kind(key);
        return k == Kind.LOG || k == Kind.ROOT;
    }

    @Nullable
    public BlockState get(long key) {
        return states.get(key);
    }

    public void remove(long key) {
        states.remove(key);
        kinds.remove(key);
    }

    public int size() {
        return states.size();
    }

    public LongArrayList keys() {
        return new LongArrayList(kinds.keySet());
    }

    public LongArrayList keys(Kind kind) {
        LongArrayList out = new LongArrayList();
        byte want = (byte) kind.ordinal();
        for (Long2ByteMap.Entry e : kinds.long2ByteEntrySet()) {
            if (e.getByteValue() == want) out.add(e.getLongKey());
        }
        return out;
    }

    /**
     * Breadth-first distance from every log (through leaves), matching vanilla leaf-decay rules:
     * a leaf survives when its distance is 6 or less.
     */
    public Long2IntMap computeLeafDistances() {
        Long2IntOpenHashMap dist = new Long2IntOpenHashMap();
        dist.defaultReturnValue(7);
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        for (Long2ByteMap.Entry e : kinds.long2ByteEntrySet()) {
            byte k = e.getByteValue();
            if (k != Kind.LOG.ordinal() && k != Kind.ROOT.ordinal()) continue;
            BlockState s = states.get(e.getLongKey());
            if (s == null || !s.is(BlockTags.LOGS)) continue;
            long key = e.getLongKey();
            for (Direction d : Direction.values()) {
                long n = BlockPos.offset(key, d);
                if (isLeaf(n) && !dist.containsKey(n)) {
                    dist.put(n, 1);
                    queue.enqueue(n);
                }
            }
        }
        while (!queue.isEmpty()) {
            long key = queue.dequeueLong();
            int d = dist.get(key);
            if (d >= MAX_LEAF_DISTANCE) continue;
            for (Direction dir : Direction.values()) {
                long n = BlockPos.offset(key, dir);
                if (isLeaf(n) && !dist.containsKey(n)) {
                    dist.put(n, d + 1);
                    queue.enqueue(n);
                }
            }
        }
        return dist;
    }

    /**
     * Makes every leaf survive vanilla decay: grows hidden twigs toward leaves that are too far from
     * a log, then writes the correct DISTANCE into each leaf. Anything still unreachable is made
     * persistent as a last resort.
     *
     * @param emptyOk whether a twig may be placed at a position the canvas does not cover
     * @param logFor  log state for a twig running along the given axis
     */
    public void fixLeaves(LongPredicate emptyOk, Function<Direction.Axis, BlockState> logFor) {
        Long2IntMap dist = computeLeafDistances();
        for (int pass = 0; pass < 3; pass++) {
            LongArrayList unreached = new LongArrayList();
            for (long key : keys(Kind.LEAF)) {
                if (!dist.containsKey(key)) unreached.add(key);
            }
            if (unreached.isEmpty()) break;

            LongArrayList logList = keys(Kind.LOG);
            if (logList.isEmpty()) break;
            long[] logs = logList.toLongArray();
            LongSet covered = new LongOpenHashSet();
            boolean grew = false;
            for (long leaf : unreached) {
                if (covered.contains(leaf)) continue;
                int lx = BlockPos.getX(leaf), ly = BlockPos.getY(leaf), lz = BlockPos.getZ(leaf);
                long best = logs[0];
                long bestD = Long.MAX_VALUE;
                for (long log : logs) {
                    long dx = BlockPos.getX(log) - lx, dy = BlockPos.getY(log) - ly, dz = BlockPos.getZ(log) - lz;
                    long d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 < bestD) {
                        bestD = d2;
                        best = log;
                    }
                }
                long end = growTwig(best, leaf, emptyOk, logFor);
                if (end != best) grew = true;
                int ex = BlockPos.getX(end), ey = BlockPos.getY(end), ez = BlockPos.getZ(end);
                for (int dx = -3; dx <= 3; dx++)
                    for (int dy = -3; dy <= 3; dy++)
                        for (int dz = -3; dz <= 3; dz++)
                            covered.add(BlockPos.asLong(ex + dx, ey + dy, ez + dz));
                covered.add(leaf);
            }
            dist = computeLeafDistances();
            if (!grew) break;
        }

        for (long key : keys(Kind.LEAF)) {
            BlockState s = states.get(key);
            if (!s.hasProperty(BlockStateProperties.DISTANCE)) continue;
            int d = dist.get(key);
            if (d > MAX_LEAF_DISTANCE) {
                s = s.setValue(BlockStateProperties.DISTANCE, 7);
                if (s.hasProperty(BlockStateProperties.PERSISTENT)) s = s.setValue(BlockStateProperties.PERSISTENT, true);
            } else {
                s = s.setValue(BlockStateProperties.DISTANCE, d);
            }
            states.put(key, s);
        }
    }

    /** Draws a face-connected log line from {@code from} toward {@code target}, stopping 2 blocks short. */
    private long growTwig(long from, long target, LongPredicate emptyOk, Function<Direction.Axis, BlockState> logFor) {
        double x0 = BlockPos.getX(from) + 0.5, y0 = BlockPos.getY(from) + 0.5, z0 = BlockPos.getZ(from) + 0.5;
        double x1 = BlockPos.getX(target) + 0.5, y1 = BlockPos.getY(target) + 0.5, z1 = BlockPos.getZ(target) + 0.5;
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 2.5) return from;
        Direction.Axis axis = Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz) ? Direction.Axis.Y
                : Math.abs(dx) >= Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
        BlockState log = logFor.apply(axis);

        int cx = BlockPos.getX(from), cy = BlockPos.getY(from), cz = BlockPos.getZ(from);
        long last = from;
        int steps = (int) Math.ceil(len / 0.3);
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            int tx = (int) Math.floor(x0 + dx * t), ty = (int) Math.floor(y0 + dy * t), tz = (int) Math.floor(z0 + dz * t);
            while (cx != tx || cy != ty || cz != tz) {
                if (cy != ty) cy += Integer.signum(ty - cy);
                else if (cx != tx) cx += Integer.signum(tx - cx);
                else cz += Integer.signum(tz - cz);
                long key = BlockPos.asLong(cx, cy, cz);
                double rx = x1 - (cx + 0.5), ry = y1 - (cy + 0.5), rz = z1 - (cz + 0.5);
                if (rx * rx + ry * ry + rz * rz <= 4.0) return last;
                Kind k = kind(key);
                if (k == Kind.LOG || k == Kind.ROOT) {
                    last = key;
                    continue;
                }
                // Never turn the top leaf of a column into wood: the crown must not show wood from above.
                boolean exposedTop = !has(cx, cy + 1, cz);
                if (exposedTop && (k == Kind.LEAF || k == null)) return last;
                if (k == Kind.LEAF || (k == null && emptyOk.test(key)) || k == Kind.DECOR) {
                    set(key, log, Kind.LOG);
                    last = key;
                } else {
                    return last;
                }
            }
        }
        return last;
    }

    /** Log state helper that tolerates blocks without an AXIS property. */
    public static BlockState withAxis(BlockState state, Direction.Axis axis) {
        return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
    }
}
