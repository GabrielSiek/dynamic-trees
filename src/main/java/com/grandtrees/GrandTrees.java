package com.grandtrees;

import com.grandtrees.command.GrandTreesCommand;
import com.grandtrees.config.GrandTreesConfig;
import com.grandtrees.item.TreeFelling;
import com.grandtrees.registry.ModItems;
import com.grandtrees.registry.ModWorldgen;
import com.grandtrees.template.TreeTemplateLoader;
import com.grandtrees.tree.VanillaSaplingGrowth;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(GrandTrees.MODID)
public class GrandTrees {
    public static final String MODID = "grandtrees";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GrandTrees() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        modBus.addListener(ModItems::addToTabs);
        ModWorldgen.FEATURES.register(modBus);
        ModWorldgen.PLACEMENTS.register(modBus);
        ModWorldgen.BIOME_MODIFIERS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GrandTreesConfig.SPEC);

        MinecraftForge.EVENT_BUS.addListener(VanillaSaplingGrowth::onSaplingGrow);
        MinecraftForge.EVENT_BUS.addListener(TreeFelling::onBreak);
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> GrandTreesCommand.register(e.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new TreeTemplateLoader()));
    }
}
