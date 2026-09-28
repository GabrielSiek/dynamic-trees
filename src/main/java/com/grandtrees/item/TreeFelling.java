package com.grandtrees.item;

import com.grandtrees.config.GrandTreesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cutting a log with the Lumber Saw fells every log connected to it (diagonals included) and drops
 * the wood at the player's feet. Only natural trees fall: the logs must touch leaves that can decay,
 * which player-placed leaves never do, so log houses are cut one block at a time. Sneak to cut a
 * single block. The leaves are left to decay like vanilla leaves.
 */
public final class TreeFelling {
    /** Natural leaves the logs must touch before they count as a tree. */
    private static final int MIN_NATURAL_LEAVES = 4;
    /** Durability used per this many felled logs. */
    private static final int LOGS_PER_DAMAGE = 8;

    private TreeFelling() {
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(event.getLevel() instanceof ServerLevel level) || player == null || player.isShiftKeyDown()) return;
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof LumberSawItem) || !event.getState().is(BlockTags.LOGS)) return;
        fell(level, event.getPos(), player, tool);
    }

    /**
     * Fells the tree that {@code start} belongs to. {@code start} itself is left in place for the
     * normal block break.
     *
     * @return logs removed (not counting {@code start}), 0 if {@code start} is not part of a natural tree
     */
    public static int fell(ServerLevel level, BlockPos start, @Nullable Player player, ItemStack tool) {
        List<BlockPos> logs = collect(level, start, GrandTreesConfig.sawMaxLogs());
        if (logs == null) return 0;

        List<ItemStack> drops = new ArrayList<>();
        int felled = 0;
        for (BlockPos pos : logs) {
            if (pos.equals(start)) continue;
            BlockState state = level.getBlockState(pos);
            for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool)) {
                merge(drops, drop);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            felled++;
        }

        Vec3 at = player != null ? player.position() : Vec3.atCenterOf(start);
        for (ItemStack stack : drops) {
            ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, stack);
            item.setNoPickUpDelay();
            level.addFreshEntity(item);
        }
        if (player != null && !player.getAbilities().instabuild) {
            player.causeFoodExhaustion(0.005F * felled);
            tool.hurtAndBreak(felled / LOGS_PER_DAMAGE, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return felled;
    }

    /** Connected logs, nearest first, or null when they don't touch enough natural leaves. */
    @Nullable
    private static List<BlockPos> collect(ServerLevel level, BlockPos start, int max) {
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> leaves = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && logs.size() < max) {
            BlockPos pos = queue.poll();
            logs.add(pos);
            for (Direction d : Direction.values()) {
                BlockPos n = pos.relative(d);
                BlockState s = level.getBlockState(n);
                if (s.is(BlockTags.LEAVES) && s.hasProperty(BlockStateProperties.PERSISTENT)
                        && !s.getValue(BlockStateProperties.PERSISTENT)) {
                    leaves.add(n);
                }
            }
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = pos.offset(dx, dy, dz);
                        if (seen.add(n) && level.getBlockState(n).is(BlockTags.LOGS)) queue.add(n);
                    }
        }
        return leaves.size() >= MIN_NATURAL_LEAVES ? logs : null;
    }

    private static void merge(List<ItemStack> stacks, ItemStack drop) {
        for (ItemStack s : stacks) {
            if (ItemStack.isSameItemSameTags(s, drop) && s.getCount() < s.getMaxStackSize()) {
                int moved = Math.min(drop.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(moved);
                drop.shrink(moved);
                if (drop.isEmpty()) return;
            }
        }
        stacks.add(drop.copy());
    }
}
