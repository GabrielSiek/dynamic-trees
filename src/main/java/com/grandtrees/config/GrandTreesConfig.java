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
    private static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_TREES;
    private static final ForgeConfigSpec.BooleanValue DECORATIONS;
    private static final ForgeConfigSpec.EnumValue<TreeSize> WORLDGEN_MAX_SIZE;
    private static final Map<TreeSpecies, ForgeConfigSpec.DoubleValue> TREES_PER_CHUNK = new EnumMap<>(TreeSpecies.class);
    private static final ForgeConfigSpec.BooleanValue VANILLA_SAPLINGS;
    private static final ForgeConfigSpec.BooleanValue ALLOW_ROUND;
    private static final ForgeConfigSpec.IntValue PROCEDURAL_WEIGHT;
    private static final ForgeConfigSpec.IntValue SAW_MAX_LOGS;
    private static final ForgeConfigSpec.BooleanValue DEBUG_TIMING;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Natural generation. Only affects chunks generated after the mod is installed.").push("worldgen");
        WORLDGEN_ENABLED = b.comment("Generate grand trees in newly generated chunks.")
                .define("enabled", true);
        REPLACE_VANILLA_TREES = b.comment("Remove vanilla trees from the biomes where grand trees generate",
                        "(the grandtrees:has_grand_<species> biome tags). Needs a restart.")
                .define("replaceVanillaTrees", true);
        DECORATIONS = b.comment("Fallen trunks, bushes, rocks and (in dark forests) huge mushrooms in the grand-tree biomes.")
                .define("decorations", true);
        WORLDGEN_MAX_SIZE = b.comment("Largest size that world generation may place (sizes above are downgraded).")
                .defineEnum("maxSize", TreeSize.EXTRA_LARGE);
        b.comment("Grand trees attempted per chunk in a matching biome. The fraction is the chance of one more",
                "attempt (2.5 = 2 or 3). Attempts that would overlap another tree shrink or are skipped.")
                .push("treesPerChunk");
        for (TreeSpecies s : TreeSpecies.values()) {
            TREES_PER_CHUNK.put(s, b.defineInRange(s.getSerializedName(), defaultTreesPerChunk(s), 0.0, 32.0));
        }
        b.pop().pop();

        b.comment("Sapling growth").push("growth");
        VANILLA_SAPLINGS = b.comment("Vanilla saplings grow grand trees (an oak sapling in a swamp grows a willow).")
                .define("vanillaSaplings", true);
        ALLOW_ROUND = b.comment("Allow Large/Extra Large patterns shaped like a circle (square without corners).")
                .define("allowRoundPatterns", true);
        PROCEDURAL_WEIGHT = b.comment("Weight of the procedural generator when .nbt templates exist for a species/size.",
                        "Each template has its own weight (1 for files in config/grandtrees/structures).")
                .defineInRange("proceduralWeight", 10, 0, 1000);
        b.pop();

        b.comment("Lumber Saw").push("saw");
        SAW_MAX_LOGS = b.comment("Most logs one cut fells (the biggest grand trees have about 1400).")
                .defineInRange("maxLogs", 2048, 1, 16384);
        b.pop();

        b.push("debug");
        DEBUG_TIMING = b.comment("Log every grown tree with its block count and generation time.")
                .define("logTreeTiming", false);
        b.pop();

        SPEC = b.build();
    }

    private GrandTreesConfig() {
    }

    private static double defaultTreesPerChunk(TreeSpecies s) {
        return switch (s) {
            case OAK, BIRCH, SPRUCE -> 6;
            case JUNGLE, DARK_OAK -> 8;
            case CHERRY -> 4;
            case WILLOW -> 2;
            case ACACIA -> 1.5;
        };
    }

    public static boolean worldgenEnabled() {
        return get(WORLDGEN_ENABLED::get, true);
    }

    public static boolean replaceVanillaTrees() {
        return get(REPLACE_VANILLA_TREES::get, true);
    }

    public static boolean decorations() {
        return get(DECORATIONS::get, true);
    }

    public static int sawMaxLogs() {
        return get(SAW_MAX_LOGS::get, 2048);
    }

    public static TreeSize worldgenMaxSize() {
        return get(WORLDGEN_MAX_SIZE::get, TreeSize.EXTRA_LARGE);
    }

    public static double treesPerChunk(TreeSpecies s) {
        return get(TREES_PER_CHUNK.get(s)::get, defaultTreesPerChunk(s));
    }

    public static boolean vanillaSaplings() {
        return get(VANILLA_SAPLINGS::get, true);
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
