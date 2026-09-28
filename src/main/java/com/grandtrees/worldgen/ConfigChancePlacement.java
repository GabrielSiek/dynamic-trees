package com.grandtrees.worldgen;

import com.grandtrees.config.GrandTreesConfig;
import com.grandtrees.registry.ModWorldgen;
import com.grandtrees.tree.TreeSpecies;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/** Per-chunk chance read from the config, so pack makers can tune density without a data pack. */
public class ConfigChancePlacement extends PlacementFilter {
    public static final Codec<ConfigChancePlacement> CODEC = TreeSpecies.CODEC.fieldOf("species")
            .xmap(ConfigChancePlacement::new, p -> p.species).codec();

    private final TreeSpecies species;

    public ConfigChancePlacement(TreeSpecies species) {
        this.species = species;
    }

    @Override
    protected boolean shouldPlace(PlacementContext ctx, RandomSource random, BlockPos pos) {
        return random.nextDouble() < GrandTreesConfig.chance(species);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.CONFIG_CHANCE.get();
    }
}
