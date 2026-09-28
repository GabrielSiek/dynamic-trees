package com.grandtrees.registry;

import com.grandtrees.GrandTrees;
import com.grandtrees.worldgen.BoulderConfiguration;
import com.grandtrees.worldgen.BoulderFeature;
import com.grandtrees.worldgen.ConfigCountPlacement;
import com.grandtrees.worldgen.DecorationsEnabledFilter;
import com.grandtrees.worldgen.FallenLogConfiguration;
import com.grandtrees.worldgen.FallenLogFeature;
import com.grandtrees.worldgen.GrandTreeConfiguration;
import com.grandtrees.worldgen.GrandTreeFeature;
import com.grandtrees.worldgen.RemoveVanillaTreesModifier;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, GrandTrees.MODID);
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, GrandTrees.MODID);
    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, GrandTrees.MODID);

    public static final RegistryObject<Feature<GrandTreeConfiguration>> GRAND_TREE = FEATURES.register("grand_tree", GrandTreeFeature::new);
    public static final RegistryObject<Feature<FallenLogConfiguration>> FALLEN_LOG = FEATURES.register("fallen_log", FallenLogFeature::new);
    public static final RegistryObject<Feature<BoulderConfiguration>> BOULDER = FEATURES.register("boulder", BoulderFeature::new);
    public static final RegistryObject<PlacementModifierType<ConfigCountPlacement>> CONFIG_COUNT =
            PLACEMENTS.register("config_count", () -> () -> ConfigCountPlacement.CODEC);
    public static final RegistryObject<PlacementModifierType<DecorationsEnabledFilter>> DECORATIONS_ENABLED =
            PLACEMENTS.register("decorations_enabled", () -> () -> DecorationsEnabledFilter.CODEC);
    public static final RegistryObject<Codec<RemoveVanillaTreesModifier>> REMOVE_VANILLA_TREES =
            BIOME_MODIFIERS.register("remove_vanilla_trees", () -> RemoveVanillaTreesModifier.CODEC);

    private ModWorldgen() {
    }
}
