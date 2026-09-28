"""Generates the Tuned Sapling textures from the vanilla sapling sprites.

Each texture is the vanilla sprite made a little brighter and more saturated, plus a small golden
sparkle so tuned saplings are easy to tell apart. Willow is a yellow-green shifted oak sapling.

Usage: python tools/gen_textures.py [path/to/client-extra.jar]
(defaults to the ForgeGradle cache for 1.20.1)
"""
import colorsys
import io
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "grandtrees", "textures", "block")
DEFAULT_JAR = os.path.expanduser("~/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar")

# species -> (vanilla sprite, hue shift in degrees)
SPECIES = {
    "oak": ("oak_sapling", 0),
    "birch": ("birch_sapling", 0),
    "spruce": ("spruce_sapling", 0),
    "jungle": ("jungle_sapling", 0),
    "acacia": ("acacia_sapling", 0),
    "dark_oak": ("dark_oak_sapling", 0),
    "cherry": ("cherry_sapling", 0),
    "willow": ("oak_sapling", 22),
}

GOLD = (255, 214, 92, 255)
PALE = (255, 246, 196, 255)


def tune(img, hue_shift):
    img = img.convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            h = (h + hue_shift / 360) % 1.0
            s = min(1.0, s * 1.15)
            v = min(1.0, v * 1.08)
            r2, g2, b2 = colorsys.hsv_to_rgb(h, s, v)
            px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    return img


def sparkle(img, cx, cy):
    """4-point star, only drawn on transparent pixels so the sapling art stays intact."""
    px = img.load()
    for (x, y, c) in ((cx, cy, PALE), (cx - 1, cy, GOLD), (cx + 1, cy, GOLD), (cx, cy - 1, GOLD), (cx, cy + 1, GOLD)):
        if 0 <= x < img.width and 0 <= y < img.height and px[x, y][3] == 0:
            px[x, y] = c


def find_empty_spot(img, candidates):
    px = img.load()
    for (cx, cy) in candidates:
        cells = [(cx, cy), (cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)]
        if all(0 <= x < img.width and 0 <= y < img.height and px[x, y][3] == 0 for x, y in cells):
            return cx, cy
    return candidates[0]


def main():
    jar = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_JAR
    os.makedirs(OUT, exist_ok=True)
    with zipfile.ZipFile(jar) as z:
        for species, (sprite, hue) in SPECIES.items():
            data = z.read(f"assets/minecraft/textures/block/{sprite}.png")
            img = tune(Image.open(io.BytesIO(data)), hue)
            sparkle(img, *find_empty_spot(img, [(2, 2), (13, 2), (2, 5), (13, 5)]))
            sparkle(img, *find_empty_spot(img, [(13, 3), (12, 6), (3, 7), (1, 9)]))
            path = os.path.join(OUT, f"tuned_{species}_sapling.png")
            img.save(path)
            print("wrote", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    main()
