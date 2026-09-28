package com.grandtrees.registry;

import com.grandtrees.GrandTrees;
import com.grandtrees.tree.TreeSpecies;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GrandTrees.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GrandTrees.MODID);

    public static final Map<TreeSpecies, RegistryObject<BlockItem>> SAPLINGS = new EnumMap<>(TreeSpecies.class);

    static {
        for (TreeSpecies species : TreeSpecies.values()) {
            SAPLINGS.put(species, ITEMS.register(ModBlocks.saplingName(species),
                    () -> new BlockItem(ModBlocks.SAPLINGS.get(species).get(), new Item.Properties())));
        }
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("grandtrees", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.grandtrees"))
            .icon(() -> new ItemStack(SAPLINGS.get(TreeSpecies.OAK).get()))
            .displayItems((params, output) -> {
                for (TreeSpecies species : TreeSpecies.values()) output.accept(SAPLINGS.get(species).get());
            })
            .build());

    private ModItems() {
    }
}
