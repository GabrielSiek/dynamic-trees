"""Checks client assets without launching the game: every registered sapling has a blockstate,
block model, item model, texture and lang entry, and every model reference resolves.

Usage: python tools/check_resources.py   (exit code 1 on problems)
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "grandtrees")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "grandtrees", "tree", "TreeSpecies.java")


def species_names():
    src = open(JAVA, encoding="utf-8").read()
    return re.findall(r'^\s+[A-Z_]+\("([a-z_]+)"', src, re.M)


def resolve(ref, kind):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    if ns != "grandtrees":
        return True  # vanilla assets are always present
    ext = ".png" if kind == "textures" else ".json"
    return os.path.exists(os.path.join(ASSETS, kind, path + ext))


def main():
    problems = []
    lang = json.load(open(os.path.join(ASSETS, "lang", "en_us.json"), encoding="utf-8"))
    names = species_names()
    if not names:
        problems.append("could not read species from TreeSpecies.java")
    for s in names:
        name = f"tuned_{s}_sapling"
        bs_path = os.path.join(ASSETS, "blockstates", name + ".json")
        if not os.path.exists(bs_path):
            problems.append(f"missing blockstate {name}")
            continue
        for variant in json.load(open(bs_path))["variants"].values():
            if not resolve(variant["model"], "models"):
                problems.append(f"{name}: blockstate model {variant['model']} missing")
        for kind in ("block", "item"):
            mp = os.path.join(ASSETS, "models", kind, name + ".json")
            if not os.path.exists(mp):
                problems.append(f"missing {kind} model {name}")
                continue
            model = json.load(open(mp))
            for tex in model.get("textures", {}).values():
                if not resolve(tex, "textures"):
                    problems.append(f"{name}: {kind} model texture {tex} missing")
        if f"block.grandtrees.{name}" not in lang:
            problems.append(f"missing en_us name for {name}")
    for p in problems:
        print("PROBLEM:", p)
    print(f"checked {len(names)} saplings, {len(problems)} problem(s)")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
