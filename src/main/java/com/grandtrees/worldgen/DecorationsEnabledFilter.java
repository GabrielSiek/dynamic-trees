package com.grandtrees.worldgen;

import com.grandtrees.config.GrandTreesConfig;
import com.grandtrees.registry.ModWorldgen;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/** Lets the config switch the decorations off (worldgen.decorations), including the vanilla ones we add. */
public class DecorationsEnabledFilter extends PlacementFilter {
    public static final DecorationsEnabledFilter INSTANCE = new DecorationsEnabledFilter();
    public static final Codec<DecorationsEnabledFilter> CODEC = Codec.unit(INSTANCE);

    @Override
    protected boolean shouldPlace(PlacementContext ctx, RandomSource random, BlockPos pos) {
        return GrandTreesConfig.worldgenEnabled() && GrandTreesConfig.decorations();
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.DECORATIONS_ENABLED.get();
    }
}
