package com.grandtrees.tree;

import com.grandtrees.tree.style.ConiferStyle;
import com.grandtrees.tree.style.JungleStyle;
import com.grandtrees.tree.style.RoundBroadleafStyle;
import com.grandtrees.tree.style.SlenderStyle;
import com.grandtrees.tree.style.SpreadingBlossomStyle;
import com.grandtrees.tree.style.TreeStyle;
import com.grandtrees.tree.style.TwistedFantasyStyle;
import com.grandtrees.tree.style.UmbrellaStyle;
import com.grandtrees.tree.style.WeepingStyle;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Each species pairs a vanilla wood set with a procedural style. Block fields are suppliers so the
 * enum can be touched during mod construction without forcing any registry lookups.
 */
public enum TreeSpecies implements StringRepresentable {
    OAK("oak", () -> Blocks.OAK_SAPLING, () -> Blocks.OAK_LOG, () -> Blocks.OAK_WOOD,
            () -> Blocks.OAK_LEAVES, () -> Blocks.AZALEA_LEAVES, 0.10, RoundBroadleafStyle.INSTANCE),
    BIRCH("birch", () -> Blocks.BIRCH_SAPLING, () -> Blocks.BIRCH_LOG, () -> Blocks.BIRCH_WOOD,
            () -> Blocks.BIRCH_LEAVES, null, 0, SlenderStyle.INSTANCE),
    SPRUCE("spruce", () -> Blocks.SPRUCE_SAPLING, () -> Blocks.SPRUCE_LOG, () -> Blocks.SPRUCE_WOOD,
            () -> Blocks.SPRUCE_LEAVES, null, 0, ConiferStyle.INSTANCE),
    JUNGLE("jungle", () -> Blocks.JUNGLE_SAPLING, () -> Blocks.JUNGLE_LOG, () -> Blocks.JUNGLE_WOOD,
            () -> Blocks.JUNGLE_LEAVES, null, 0, JungleStyle.INSTANCE),
    ACACIA("acacia", () -> Blocks.ACACIA_SAPLING, () -> Blocks.ACACIA_LOG, () -> Blocks.ACACIA_WOOD,
            () -> Blocks.ACACIA_LEAVES, null, 0, UmbrellaStyle.INSTANCE),
    DARK_OAK("dark_oak", () -> Blocks.DARK_OAK_SAPLING, () -> Blocks.DARK_OAK_LOG, () -> Blocks.DARK_OAK_WOOD,
            () -> Blocks.DARK_OAK_LEAVES, () -> Blocks.OAK_LEAVES, 0.08, TwistedFantasyStyle.INSTANCE),
    CHERRY("cherry", () -> Blocks.CHERRY_SAPLING, () -> Blocks.CHERRY_LOG, () -> Blocks.CHERRY_WOOD,
            () -> Blocks.CHERRY_LEAVES, null, 0, SpreadingBlossomStyle.INSTANCE),
    WILLOW("willow", () -> Blocks.OAK_SAPLING, () -> Blocks.MANGROVE_LOG, () -> Blocks.MANGROVE_WOOD,
            () -> Blocks.OAK_LEAVES, () -> Blocks.AZALEA_LEAVES, 0.18, WeepingStyle.INSTANCE);

    public static final Codec<TreeSpecies> CODEC = StringRepresentable.fromEnum(TreeSpecies::values);

    private final String name;
    private final Supplier<Block> vanillaSapling;
    private final Supplier<Block> log;
    private final Supplier<Block> wood;
    private final Supplier<Block> leaves;
    @Nullable
    private final Supplier<Block> secondaryLeaves;
    /** Rough fraction of the canopy that uses the secondary leaves (in noise patches). */
    public final double secondaryFraction;
    private final TreeStyle style;

    TreeSpecies(String name, Supplier<Block> vanillaSapling, Supplier<Block> log, Supplier<Block> wood,
                Supplier<Block> leaves, @Nullable Supplier<Block> secondaryLeaves, double secondaryFraction,
                TreeStyle style) {
        this.name = name;
        this.vanillaSapling = vanillaSapling;
        this.log = log;
        this.wood = wood;
        this.leaves = leaves;
        this.secondaryLeaves = secondaryLeaves;
        this.secondaryFraction = secondaryFraction;
        this.style = style;
    }

    public Block vanillaSapling() {
        return vanillaSapling.get();
    }

    public Block log() {
        return log.get();
    }

    public Block wood() {
        return wood.get();
    }

    public Block leaves() {
        return leaves.get();
    }

    @Nullable
    public Block secondaryLeaves() {
        return secondaryLeaves == null ? null : secondaryLeaves.get();
    }

    public TreeStyle style() {
        return style;
    }

    @Nullable
    public static TreeSpecies byName(String name) {
        for (TreeSpecies s : values()) {
            if (s.name.equals(name)) return s;
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
