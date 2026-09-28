package com.grandtrees.worldgen;

import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.TreeSpecies;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Map;

/**
 * @param species which grand tree to grow
 * @param sizes   weighted size table, e.g. {"medium": 4, "big": 4, "large": 2}
 */
public record GrandTreeConfiguration(TreeSpecies species, Map<TreeSize, Integer> sizes) implements FeatureConfiguration {
    public static final Codec<GrandTreeConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group(
            TreeSpecies.CODEC.fieldOf("species").forGetter(GrandTreeConfiguration::species),
            Codec.unboundedMap(TreeSize.CODEC, Codec.intRange(0, 1000)).fieldOf("sizes").forGetter(GrandTreeConfiguration::sizes)
    ).apply(i, GrandTreeConfiguration::new));
}
