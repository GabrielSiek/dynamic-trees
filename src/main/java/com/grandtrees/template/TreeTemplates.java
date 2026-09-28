package com.grandtrees.template;

import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.TreeSpecies;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Loaded templates, swapped atomically on every data reload. */
public final class TreeTemplates {
    private static volatile Map<TreeSpecies, Map<TreeSize, List<TreeTemplate>>> templates = new EnumMap<>(TreeSpecies.class);

    private TreeTemplates() {
    }

    public static List<TreeTemplate> get(TreeSpecies species, TreeSize size) {
        Map<TreeSize, List<TreeTemplate>> bySize = templates.get(species);
        if (bySize == null) return List.of();
        return bySize.getOrDefault(size, List.of());
    }

    public static int count() {
        int n = 0;
        for (Map<TreeSize, List<TreeTemplate>> m : templates.values()) {
            for (List<TreeTemplate> l : m.values()) n += l.size();
        }
        return n;
    }

    static void set(Map<TreeSpecies, Map<TreeSize, List<TreeTemplate>>> loaded) {
        templates = loaded;
    }
}
