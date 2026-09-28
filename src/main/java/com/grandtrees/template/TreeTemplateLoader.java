package com.grandtrees.template;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grandtrees.GrandTrees;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.TreeSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads tree templates from two places:
 * <ul>
 *   <li>data packs: {@code data/<ns>/grandtrees/templates/*.json} pointing at {@code data/<ns>/structures/*.nbt}</li>
 *   <li>the config folder: {@code config/grandtrees/structures/<species>/<size>/*.nbt} (no JSON needed)</li>
 * </ul>
 * Runs on every /reload.
 */
public final class TreeTemplateLoader extends SimplePreparableReloadListener<Map<TreeSpecies, Map<TreeSize, List<TreeTemplate>>>> {
    private static final String DIR = "grandtrees/templates";

    @Override
    protected Map<TreeSpecies, Map<TreeSize, List<TreeTemplate>>> prepare(ResourceManager rm, ProfilerFiller profiler) {
        Map<TreeSpecies, Map<TreeSize, List<TreeTemplate>>> out = new EnumMap<>(TreeSpecies.class);
        for (Map.Entry<ResourceLocation, Resource> e : rm.listResources(DIR, rl -> rl.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = e.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loadFromJson(rm, e.getKey(), json, out);
            } catch (Exception ex) {
                GrandTrees.LOGGER.error("Failed to load tree template {}", e.getKey(), ex);
            }
        }
        loadConfigFolder(out);
        int total = out.values().stream().flatMap(m -> m.values().stream()).mapToInt(List::size).sum();
        if (total > 0) GrandTrees.LOGGER.info("Loaded {} Grand Trees templates", total);
        return out;
    }

    @Override
    protected void apply(Map<TreeSpecies, Map<TreeSize, List<TreeTemplate>>> loaded, ResourceManager rm, ProfilerFiller profiler) {
        TreeTemplates.set(loaded);
    }

    private static void loadFromJson(ResourceManager rm, ResourceLocation file, JsonObject json,
                                     Map<TreeSpecies, Map<TreeSize, List<TreeTemplate>>> out) throws IOException {
        TreeSpecies species = TreeSpecies.byName(GsonHelper.getAsString(json, "species"));
        TreeSize size = TreeSize.byName(GsonHelper.getAsString(json, "size"));
        if (species == null || size == null) {
            GrandTrees.LOGGER.warn("Tree template {} has an unknown species or size", file);
            return;
        }
        int weight = GsonHelper.getAsInt(json, "weight", 1);
        ResourceLocation structure = new ResourceLocation(GsonHelper.getAsString(json, "structure"));
        BlockPos anchor = null;
        if (json.has("anchor")) {
            JsonArray a = GsonHelper.getAsJsonArray(json, "anchor");
            anchor = new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
        }
        ResourceLocation nbtPath = new ResourceLocation(structure.getNamespace(), "structures/" + structure.getPath() + ".nbt");
        Optional<Resource> res = rm.getResource(nbtPath);
        if (res.isEmpty()) {
            GrandTrees.LOGGER.warn("Tree template {} points at missing structure {}", file, nbtPath);
            return;
        }
        try (InputStream in = res.get().open()) {
            add(out, TreeTemplate.fromNbt(structure.toString(), NbtIo.readCompressed(in), species, size, weight, anchor));
        }
    }

    private static void loadConfigFolder(Map<TreeSpecies, Map<TreeSize, List<TreeTemplate>>> out) {
        Path root = FMLPaths.CONFIGDIR.get().resolve(GrandTrees.MODID).resolve("structures");
        for (TreeSpecies species : TreeSpecies.values()) {
            for (TreeSize size : TreeSize.values()) {
                Path dir = root.resolve(species.getSerializedName()).resolve(size.getSerializedName());
                try {
                    Files.createDirectories(dir);
                } catch (IOException ex) {
                    GrandTrees.LOGGER.warn("Could not create {}", dir, ex);
                    continue;
                }
                try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.nbt")) {
                    for (Path f : files) {
                        try (InputStream in = Files.newInputStream(f)) {
                            String id = "config/" + species.getSerializedName() + "/" + size.getSerializedName() + "/" + f.getFileName();
                            add(out, TreeTemplate.fromNbt(id, NbtIo.readCompressed(in), species, size, 1, null));
                        } catch (Exception ex) {
                            GrandTrees.LOGGER.error("Failed to load tree template {}", f, ex);
                        }
                    }
                } catch (IOException ex) {
                    GrandTrees.LOGGER.warn("Could not list {}", dir, ex);
                }
            }
        }
    }

    private static void add(Map<TreeSpecies, Map<TreeSize, List<TreeTemplate>>> out, TreeTemplate t) {
        if (t == null) return;
        out.computeIfAbsent(t.species(), k -> new EnumMap<>(TreeSize.class))
                .computeIfAbsent(t.size(), k -> new ArrayList<>())
                .add(t);
    }
}
