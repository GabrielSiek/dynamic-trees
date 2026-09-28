package com.grandtrees.template;

import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.TreeSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A hand-built tree loaded from a vanilla structure (.nbt) file. Positions are stored relative to
 * the anchor, which is the trunk base that lands on the sapling pattern's center.
 */
public record TreeTemplate(String id, TreeSpecies species, TreeSize size, int weight,
                           BlockPos[] positions, BlockState[] states) {

    private static final Mirror[] MIRRORS = Mirror.values();

    public void placeInto(TreeContext ctx) {
        Rotation rotation = Rotation.getRandom(ctx.random);
        Mirror mirror = MIRRORS[ctx.random.nextInt(MIRRORS.length)];
        BlockPos origin = BlockPos.containing(ctx.base.x, ctx.base.y, ctx.base.z);
        for (int i = 0; i < positions.length; i++) {
            BlockPos rel = transform(positions[i], mirror, rotation);
            BlockState state = states[i].mirror(mirror).rotate(rotation);
            Kind kind;
            if (state.is(BlockTags.LOGS)) {
                kind = rel.getY() < 0 ? Kind.ROOT : Kind.LOG;
            } else if (state.hasProperty(BlockStateProperties.DISTANCE)) {
                kind = Kind.LEAF;
            } else {
                kind = Kind.DECOR;
            }
            ctx.canvas.put(origin.offset(rel), state, kind);
        }
    }

    private static BlockPos transform(BlockPos p, Mirror mirror, Rotation rotation) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        switch (mirror) {
            case LEFT_RIGHT -> z = -z;
            case FRONT_BACK -> x = -x;
            default -> {
            }
        }
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(-z, y, x);
            case CLOCKWISE_180 -> new BlockPos(-x, y, -z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, -x);
            default -> new BlockPos(x, y, z);
        };
    }

    /**
     * Parses a structure-block NBT. Air, structure voids and ground blocks are skipped; leaves are
     * made non-persistent so they behave like natural leaves (their distance is recomputed on place).
     *
     * @param anchor trunk base inside the structure, or null to detect it from the lowest logs
     */
    @Nullable
    public static TreeTemplate fromNbt(String id, CompoundTag tag, TreeSpecies species, TreeSize size, int weight,
                                       @Nullable BlockPos anchor) {
        ListTag paletteTag;
        if (tag.contains("palettes", Tag.TAG_LIST)) {
            ListTag palettes = tag.getList("palettes", Tag.TAG_LIST);
            if (palettes.isEmpty()) return null;
            paletteTag = palettes.getList(0);
        } else {
            paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);
        }
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), paletteTag.getCompound(i)));
        }

        List<BlockPos> positions = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompound(i);
            int idx = b.getInt("state");
            if (idx < 0 || idx >= palette.size()) continue;
            BlockState state = palette.get(idx);
            if (skip(state)) continue;
            if (state.hasProperty(BlockStateProperties.PERSISTENT)) {
                state = state.setValue(BlockStateProperties.PERSISTENT, false);
            }
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            positions.add(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)));
            states.add(state);
        }
        if (positions.isEmpty()) return null;

        BlockPos a = anchor != null ? anchor : detectAnchor(positions, states);
        BlockPos[] rel = new BlockPos[positions.size()];
        for (int i = 0; i < rel.length; i++) rel[i] = positions.get(i).subtract(a);
        return new TreeTemplate(id, species, size, Math.max(1, weight), rel, states.toArray(new BlockState[0]));
    }

    private static boolean skip(BlockState s) {
        Block b = s.getBlock();
        return s.isAir() || b == Blocks.STRUCTURE_VOID || b == Blocks.STRUCTURE_BLOCK || b == Blocks.JIGSAW
                || s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND);
    }

    /**
     * Trunk base: the (x, z) centroid of the logs in the lowest few log layers (roots spread
     * roughly evenly, so they average out to the trunk), and the lowest log in that column. Roots
     * that dip below the trunk base therefore end up underground instead of lifting the tree.
     */
    private static BlockPos detectAnchor(List<BlockPos> positions, List<BlockState> states) {
        int minY = Integer.MAX_VALUE;
        for (int i = 0; i < positions.size(); i++) {
            if (states.get(i).is(BlockTags.LOGS)) minY = Math.min(minY, positions.get(i).getY());
        }
        boolean hasLogs = minY != Integer.MAX_VALUE;
        if (!hasLogs) {
            for (BlockPos p : positions) minY = Math.min(minY, p.getY());
        }
        long sx = 0, sz = 0;
        int n = 0;
        for (int i = 0; i < positions.size(); i++) {
            BlockPos p = positions.get(i);
            if (p.getY() <= minY + 3 && (!hasLogs || states.get(i).is(BlockTags.LOGS))) {
                sx += p.getX();
                sz += p.getZ();
                n++;
            }
        }
        int cx = (int) Math.round((double) sx / n), cz = (int) Math.round((double) sz / n);
        int baseY = Integer.MAX_VALUE;
        for (int i = 0; i < positions.size(); i++) {
            BlockPos p = positions.get(i);
            if (Math.abs(p.getX() - cx) <= 1 && Math.abs(p.getZ() - cz) <= 1 && (!hasLogs || states.get(i).is(BlockTags.LOGS))) {
                baseY = Math.min(baseY, p.getY());
            }
        }
        return new BlockPos(cx, baseY == Integer.MAX_VALUE ? minY : baseY, cz);
    }
}
