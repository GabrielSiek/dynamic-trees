package com.grandtrees.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * @param blocks     stone mix, e.g. a weighted_state_provider of cobblestone, mossy cobblestone and stone
 * @param minRadius  smallest rock (below 1 gives pebbles of a block or two)
 * @param maxRadius  largest rock
 * @param mossChance chance of moss carpet on each top block
 */
public record BoulderConfiguration(BlockStateProvider blocks, float minRadius, float maxRadius, float mossChance)
        implements FeatureConfiguration {
    public static final Codec<BoulderConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("blocks").forGetter(BoulderConfiguration::blocks),
            Codec.floatRange(0.5F, 4).fieldOf("min_radius").forGetter(BoulderConfiguration::minRadius),
            Codec.floatRange(0.5F, 4).fieldOf("max_radius").forGetter(BoulderConfiguration::maxRadius),
            Codec.floatRange(0, 1).fieldOf("moss_chance").forGetter(BoulderConfiguration::mossChance)
    ).apply(i, BoulderConfiguration::new));
}
