package com.grandtrees.dev;

import com.grandtrees.GrandTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dev tool: renders a grown tree as a front view (looking north) and a top view, colored with each
 * block's map color and shaded by depth, into run/gametest-previews/&lt;name&gt;.png.
 */
public final class TreePreview {
    private static final int SCALE = 6;

    private TreePreview() {
    }

    public static void render(ServerLevel level, BlockPos center, int radius, int minY, int maxY, String name) {
        int w = radius * 2 + 1, h = maxY - minY + 1;
        BufferedImage img = new BufferedImage((w * 2 + 2) * SCALE, Math.max(h, w) * SCALE, BufferedImage.TYPE_INT_RGB);
        fill(img, 0x2b2d35);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        // Front view: for each (x, y) the first non-air block walking from the viewer (south) to north.
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int d = 0; d < w; d++) {
                    m.set(center.getX() - radius + x, minY + y, center.getZ() + radius - d);
                    BlockState s = level.getBlockState(m);
                    if (visible(s)) {
                        plot(img, x, h - 1 - y, shade(s.getMapColor(level, m), d, w));
                        break;
                    }
                }
            }
        }
        // Top view: highest non-air block per column.
        int ox = w + 2;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < w; z++) {
                for (int y = maxY; y >= minY; y--) {
                    m.set(center.getX() - radius + x, y, center.getZ() - radius + z);
                    BlockState s = level.getBlockState(m);
                    if (visible(s)) {
                        plot(img, ox + x, z, shade(s.getMapColor(level, m), maxY - y, h));
                        break;
                    }
                }
            }
        }
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("gametest-previews");
            Files.createDirectories(dir);
            ImageIO.write(img, "png", dir.resolve(name + ".png").toFile());
        } catch (IOException e) {
            GrandTrees.LOGGER.warn("Could not write preview {}", name, e);
        }
    }

    private static boolean visible(BlockState s) {
        return !s.isAir() && !s.is(Blocks.GRASS_BLOCK) && !s.is(Blocks.DIRT);
    }

    private static int shade(MapColor color, int depth, int range) {
        int c = color.col;
        double k = 1.0 - 0.55 * Math.min(1.0, (double) depth / Math.max(1, range));
        int r = (int) (((c >> 16) & 0xff) * k), g = (int) (((c >> 8) & 0xff) * k), b = (int) ((c & 0xff) * k);
        return (r << 16) | (g << 8) | b;
    }

    private static void plot(BufferedImage img, int x, int y, int rgb) {
        for (int i = 0; i < SCALE; i++)
            for (int j = 0; j < SCALE; j++) {
                int px = x * SCALE + i, py = y * SCALE + j;
                if (px < img.getWidth() && py < img.getHeight()) img.setRGB(px, py, rgb);
            }
    }

    private static void fill(BufferedImage img, int rgb) {
        for (int x = 0; x < img.getWidth(); x++)
            for (int y = 0; y < img.getHeight(); y++) img.setRGB(x, y, rgb);
    }
}
