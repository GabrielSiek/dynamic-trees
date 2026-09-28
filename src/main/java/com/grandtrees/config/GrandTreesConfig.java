package com.grandtrees.config;

import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.TreeSpecies;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** COMMON config (config/grandtrees-common.toml). */
public final class GrandTreesConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue WORLDGEN_ENABLED;
    private static final ForgeConfigSpec.EnumValue<TreeSize> WORLDGEN_MAX_SIZE;
    private static final Map<TreeSpecies, ForgeConfigSpec.DoubleValue> CHANCE = new EnumMap<>(TreeSpecies.class);
    private static final ForgeConfigSpec.BooleanValue ALLOW_ROUND;
    private static final ForgeConfigSpec.IntValue PROCEDURAL_WEIGHT;
    private static final ForgeConfigSpec.BooleanValue DEBUG_TIMING;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Natural generation. Only affects chunks generated after the mod is installed.").push("worldgen");
        WORLDGEN_ENABLED = b.comment("Generate grand trees in newly generated chunks.")
                .define("enabled", true);
        WORLDGEN_MAX_SIZE = b.comment("Largest size that world generation may place (sizes above are downgraded).")
                .defineEnum("maxSize", TreeSize.LARGE);
        b.comment("Chance per chunk (0-1) that a grand tree is attempted in a matching biome.").push("chancePerChunk");
        for (TreeSpecies s : TreeSpecies.values()) {
            CHANCE.put(s, b.defineInRange(s.getSerializedName(), defaultChance(s), 0.0, 1.0));
        }
        b.pop().pop();

        b.comment("Sapling growth").push("growth");
        ALLOW_ROUND = b.comment("Allow Large/Extra Large patterns shaped like a circle (square without corners).")
                .define("allowRoundPatterns", true);
        PROCEDURAL_WEIGHT = b.comment("Weight of the procedural generator when .nbt templates exist for a species/size.",
                        "Each template has its own weight (1 for files in config/grandtrees/structures).")
                .defineInRange("proceduralWeight", 10, 0, 1000);
        b.pop();

        b.push("debug");
        DEBUG_TIMING = b.comment("Log every grown tree with its block count and generation time.")
                .define("logTreeTiming", false);
        b.pop();

        SPEC = b.build();
    }

    private GrandTreesConfig() {
    }

    private static double defaultChance(TreeSpecies s) {
        return switch (s) {
            case OAK, DARK_OAK, JUNGLE -> 0.35;
            case WILLOW -> 0.5;
            case ACACIA -> 0.25;
            default -> 0.3;
        };
    }

    public static boolean worldgenEnabled() {
        return get(WORLDGEN_ENABLED::get, true);
    }

    public static TreeSize worldgenMaxSize() {
        return get(WORLDGEN_MAX_SIZE::get, TreeSize.LARGE);
    }

    public static double chance(TreeSpecies s) {
        return get(CHANCE.get(s)::get, defaultChance(s));
    }

    public static boolean allowRoundPatterns() {
        return get(ALLOW_ROUND::get, true);
    }

    public static int proceduralWeight() {
        return get(PROCEDURAL_WEIGHT::get, 10);
    }

    public static boolean debugTiming() {
        return get(DEBUG_TIMING::get, false);
    }

    /** Config values throw before the file is loaded; fall back to defaults in that window. */
    private static <T> T get(Supplier<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }
}
