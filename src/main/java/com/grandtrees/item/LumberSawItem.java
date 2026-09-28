package com.grandtrees.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Mines like an iron axe; cutting a log of a natural tree fells the whole tree ({@link TreeFelling}).
 * Deliberately not an AxeItem, so axe-only tree choppers from other mods leave it alone.
 */
public class LumberSawItem extends DiggerItem {
    public LumberSawItem(Properties properties) {
        super(4.0F, -3.0F, Tiers.IRON, BlockTags.MINEABLE_WITH_AXE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.grandtrees.lumber_saw.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
