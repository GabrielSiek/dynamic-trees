package com.grandtrees.registry;

import com.grandtrees.GrandTrees;
import com.grandtrees.worldgen.ConfigChancePlacement;
import com.grandtrees.worldgen.GrandTreeConfiguration;
import com.grandtrees.worldgen.GrandTreeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, GrandTrees.MODID);
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, GrandTrees.MODID);

    public static final RegistryObject<Feature<GrandTreeConfiguration>> GRAND_TREE = FEATURES.register("grand_tree", GrandTreeFeature::new);
    public static final RegistryObject<PlacementModifierType<ConfigChancePlacement>> CONFIG_CHANCE =
            PLACEMENTS.register("config_chance", () -> () -> ConfigChancePlacement.CODEC);

    private ModWorldgen() {
    }
}
