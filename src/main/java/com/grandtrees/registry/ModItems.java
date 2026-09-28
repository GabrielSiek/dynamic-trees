package com.grandtrees.registry;

import com.grandtrees.GrandTrees;
import com.grandtrees.item.LumberSawItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GrandTrees.MODID);

    public static final RegistryObject<LumberSawItem> LUMBER_SAW = ITEMS.register("lumber_saw",
            () -> new LumberSawItem(new Item.Properties().durability(750)));

    private ModItems() {
    }

    public static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(LUMBER_SAW);
    }
}
