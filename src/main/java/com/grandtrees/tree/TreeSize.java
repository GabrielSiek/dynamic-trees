package com.grandtrees.tree;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * Tree sizes. The width is the side of the square sapling pattern that grows the size
 * (1 = single sapling, 5 = 5x5). {@code maxRadius} is a conservative horizontal extent
 * used to keep world-gen trees inside the writable chunk region.
 */
public enum TreeSize implements StringRepresentable {
    SMALL("small", 1, 7),
    MEDIUM("medium", 2, 10),
    BIG("big", 3, 13),
    LARGE("large", 4, 17),
    EXTRA_LARGE("extra_large", 5, 23);

    public static final Codec<TreeSize> CODEC = StringRepresentable.fromEnum(TreeSize::values);

    private final String name;
    public final int width;
    public final int maxRadius;

    TreeSize(String name, int width, int maxRadius) {
        this.name = name;
        this.width = width;
        this.maxRadius = maxRadius;
    }

    @Nullable
    public TreeSize smaller() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }

    public boolean atLeast(TreeSize other) {
        return ordinal() >= other.ordinal();
    }

    @Nullable
    public static TreeSize byName(String name) {
        for (TreeSize s : values()) {
            if (s.name.equals(name)) return s;
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
