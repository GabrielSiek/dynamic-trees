package com.grandtrees.block;

import com.grandtrees.tree.GrandTreeGrower;
import com.grandtrees.tree.TreeSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * A sapling that grows a grand tree. Neighbouring saplings of the same species in a square (or
 * round) pattern grow one bigger tree together.
 */
public class TunedSaplingBlock extends SaplingBlock {
    /** How far around the sapling must be loaded before a tree is allowed to grow. */
    private static final int LOADED_RADIUS = 24;

    private final TreeSpecies species;

    public TunedSaplingBlock(TreeSpecies species, Properties properties) {
        super(new VanillaFallbackGrower(), properties);
        this.species = species;
    }

    public TreeSpecies species() {
        return species;
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
            return;
        }
        if (!level.hasChunksAt(pos.offset(-LOADED_RADIUS, 0, -LOADED_RADIUS), pos.offset(LOADED_RADIUS, 0, LOADED_RADIUS))) {
            return;
        }
        GrandTreeGrower.growFromSapling(level, pos, this, species, random);
    }

    /**
     * Only used if another mod bypasses {@link #advanceTree} and asks the grower directly.
     */
    private static final class VanillaFallbackGrower extends AbstractTreeGrower {
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return TreeFeatures.OAK;
        }
    }
}
