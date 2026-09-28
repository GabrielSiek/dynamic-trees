package com.grandtrees.worldgen;

import com.grandtrees.config.GrandTreesConfig;
import com.grandtrees.registry.ModWorldgen;
import com.grandtrees.tree.TreeSpecies;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.placement.RepeatingPlacement;

/**
 * Attempts per chunk read from the config, so pack makers can tune density without a data pack. The
 * fraction is the chance of one extra attempt.
 */
public class ConfigCountPlacement extends RepeatingPlacement {
    public static final Codec<ConfigCountPlacement> CODEC = TreeSpecies.CODEC.fieldOf("species")
            .xmap(ConfigCountPlacement::new, p -> p.species).codec();

    private final TreeSpecies species;

    public ConfigCountPlacement(TreeSpecies species) {
        this.species = species;
    }

    @Override
    protected int count(RandomSource random, BlockPos pos) {
        double perChunk = GrandTreesConfig.treesPerChunk(species);
        int whole = (int) perChunk;
        return whole + (random.nextDouble() < perChunk - whole ? 1 : 0);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.CONFIG_COUNT.get();
    }
}
