package com.grandtrees.worldgen;

import com.grandtrees.config.GrandTreesConfig;
import com.grandtrees.registry.ModWorldgen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Removes the listed vanilla tree features from the biomes where grand trees take over. Like
 * forge:remove_features, but skipped when the config turns replacement (or world generation) off.
 *
 * @param biomes   biomes whose vanilla trees are removed
 * @param features vegetation features to remove from those biomes
 */
public record RemoveVanillaTreesModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features) implements BiomeModifier {
    public static final Codec<RemoveVanillaTreesModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(RemoveVanillaTreesModifier::biomes),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(RemoveVanillaTreesModifier::features)
    ).apply(i, RemoveVanillaTreesModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.REMOVE || !biomes.contains(biome)) return;
        if (!GrandTreesConfig.worldgenEnabled() || !GrandTreesConfig.replaceVanillaTrees()) return;
        builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION).removeIf(features::contains);
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return ModWorldgen.REMOVE_VANILLA_TREES.get();
    }
}
