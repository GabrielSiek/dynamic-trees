package com.grandtrees.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * @param log            log block of the fallen trunk (and its stump)
 * @param minLength      shortest trunk
 * @param maxLength      longest trunk
 * @param mossChance     chance of moss carpet on each trunk block (and, less often, on the ground beside it)
 * @param mushroomChance chance of a mushroom on each trunk block without moss
 * @param stumpChance    chance of a broken stump standing at the base of the trunk
 */
public record FallenLogConfiguration(BlockState log, int minLength, int maxLength, float mossChance,
                                     float mushroomChance, float stumpChance) implements FeatureConfiguration {
    public static final Codec<FallenLogConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockState.CODEC.fieldOf("log").forGetter(FallenLogConfiguration::log),
            Codec.intRange(1, 12).fieldOf("min_length").forGetter(FallenLogConfiguration::minLength),
            Codec.intRange(1, 12).fieldOf("max_length").forGetter(FallenLogConfiguration::maxLength),
            Codec.floatRange(0, 1).fieldOf("moss_chance").forGetter(FallenLogConfiguration::mossChance),
            Codec.floatRange(0, 1).fieldOf("mushroom_chance").forGetter(FallenLogConfiguration::mushroomChance),
            Codec.floatRange(0, 1).fieldOf("stump_chance").forGetter(FallenLogConfiguration::stumpChance)
    ).apply(i, FallenLogConfiguration::new));
}
