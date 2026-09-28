package com.grandtrees.tree;

import com.grandtrees.GrandTrees;
import com.grandtrees.config.GrandTreesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla saplings grow grand trees. Forge asks before a sapling grows its vanilla tree (random tick
 * or bone meal); we deny that and grow the grand tree for the largest sapling pattern instead.
 * <p>
 * Vanilla never asks for a single dark oak sapling (it needs a 2x2), so dark oak starts at Medium.
 */
public final class VanillaSaplingGrowth {
    /** How far around the sapling must be loaded before a tree is allowed to grow. */
    private static final int LOADED_RADIUS = 24;

    /** An oak sapling planted in these biomes grows a willow. */
    private static final TagKey<Biome> WILLOW_BIOMES =
            TagKey.create(Registries.BIOME, new ResourceLocation(GrandTrees.MODID, "has_grand_willow"));

    private VanillaSaplingGrowth() {
    }

    public static void onSaplingGrow(SaplingGrowTreeEvent event) {
        if (!GrandTreesConfig.vanillaSaplings() || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        Block sapling = level.getBlockState(pos).getBlock();
        TreeSpecies species = speciesFor(level, pos, sapling);
        if (species == null) return;

        event.setResult(Event.Result.DENY);
        if (!level.hasChunksAt(pos.offset(-LOADED_RADIUS, 0, -LOADED_RADIUS), pos.offset(LOADED_RADIUS, 0, LOADED_RADIUS))) {
            return;
        }
        GrandTreeGrower.growFromSapling(level, pos, sapling, species, event.getRandomSource());
    }

    @Nullable
    public static TreeSpecies speciesFor(LevelReader level, BlockPos pos, Block sapling) {
        if (sapling == Blocks.OAK_SAPLING && level.getBiome(pos).is(WILLOW_BIOMES)) return TreeSpecies.WILLOW;
        for (TreeSpecies s : TreeSpecies.values()) {
            if (s != TreeSpecies.WILLOW && s.vanillaSapling() == sapling) return s;
        }
        return null;
    }
}
