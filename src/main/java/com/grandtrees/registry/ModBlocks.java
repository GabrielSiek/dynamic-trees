package com.grandtrees.registry;

import com.grandtrees.GrandTrees;
import com.grandtrees.block.TunedSaplingBlock;
import com.grandtrees.tree.TreeSpecies;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GrandTrees.MODID);

    public static final Map<TreeSpecies, RegistryObject<TunedSaplingBlock>> SAPLINGS = new EnumMap<>(TreeSpecies.class);

    static {
        for (TreeSpecies species : TreeSpecies.values()) {
            SAPLINGS.put(species, BLOCKS.register(saplingName(species),
                    () -> new TunedSaplingBlock(species, BlockBehaviour.Properties.copy(species.vanillaSapling()))));
        }
    }

    private ModBlocks() {
    }

    public static String saplingName(TreeSpecies species) {
        return "tuned_" + species.getSerializedName() + "_sapling";
    }
}
