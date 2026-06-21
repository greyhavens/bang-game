#!/usr/bin/env python3
"""build_templates.py -- (re)build the QA template library from baseline crops.

Extracts:
  templates/digits/<d>.png   -- single-digit exemplars (colour mask, white-on-black)
  templates/icons/*.png      -- icon crops (skull, reticle, star-burst, influence)

Digit exemplars are taken from known baseline images where the value is known,
by colour-masking, finding glyph components, and slicing them left-to-right.
Icon crops are hand-specified pixel boxes from baseline images.

Run from anywhere:  python3 build_templates.py
Re-run after adding new labelled baselines (edit DIGIT_SOURCES / ICON_SOURCES).
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import qa_common as qc
from PIL import Image

BASE = os.path.normpath(os.path.join(HERE, "..", "..", "baseline"))
DIG = os.path.join(HERE, "templates", "digits")
ICO = os.path.join(HERE, "templates", "icons")

# (path, color, value) -- value's digits map left-to-right to detected glyphs.
DIGIT_SOURCES = [
    ("correct/CorrectDamage.png", "blue", "17"),
    ("correct/ShotgunnerDeathAndDamageCorrect.png", "green", "50"),
    ("incorrect/DamageAndSkullIncorrectLocation.png", "red", "25"),
    ("incorrect/DamageOffCenter.png", "blue", "10"),
]

# (out_name, path, (x,y,w,h)) icon crops. Boxes chosen from visual inspection.
ICON_SOURCES = [
    ("skull",      "correct/ShotgunnerDeathAndDamageCorrect.png", (28, 47, 48, 56)),
    ("skull_white", "incorrect/DamageAndSkullIncorrectLocation.png", (122, 46, 44, 50)),
    ("reticle",    "incorrect/AttackIconOffcenter.png",            (70, 55, 58, 58)),
    ("starburst",  "/tmp/get_bonus_after.png",                      (300, 210, 190, 200)),
]


def extract_digits():
    os.makedirs(DIG, exist_ok=True)
    have = {}
    for rel, color, value in DIGIT_SOURCES:
        path = rel if os.path.isabs(rel) else os.path.join(BASE, rel)
        if not os.path.exists(path):
            print("  skip (missing):", path)
            continue
        img = qc.load_rgb(path)
        mask = qc.mask_fast(img, color)
        comps = qc.connected_components(mask, min_area=20)
        # digit glyphs: roughly digit-shaped, reasonable height, and NOT a wide
        # reticle blob (those have w/h ~1 but huge area + big width).
        glyphs = [c for c in comps
                  if 0.15 < c["bbox"][2] / max(1, c["bbox"][3]) < 1.6
                  and 18 <= c["bbox"][3] <= 60
                  and c["bbox"][2] <= 40]
        # Damage numbers float at the top: find the row of glyphs whose tops
        # cluster together and sit highest. Group by similar y-top (+-8px).
        glyphs.sort(key=lambda c: c["bbox"][1])
        rows = []
        for g in glyphs:
            gy = g["bbox"][1]
            placed = False
            for row in rows:
                if abs(row[0]["bbox"][1] - gy) <= 10:
                    row.append(g); placed = True; break
            if not placed:
                rows.append([g])
        # choose the topmost row that has exactly len(value) members, else the
        # topmost row with at least len(value).
        rows = [r for r in rows if len(r) >= len(value)]
        if not rows:
            print("  %s: no glyph row of >=%d found -- skipping" % (rel, len(value)))
            continue
        row = sorted(rows, key=lambda r: min(g["bbox"][1] for g in r))[0]
        glyphs = sorted(row, key=lambda c: c["bbox"][0])[:len(value)]
        glyphs = sorted(glyphs, key=lambda c: c["bbox"][0])
        for d, g in zip(value, glyphs):
            x, y, w, h = g["bbox"]
            crop = mask.crop((x, y, x + w, y + h))
            # prefer first/cleanest exemplar per digit
            if d not in have:
                crop.save(os.path.join(DIG, "%s.png" % d))
                have[d] = rel
    print("  digit exemplars:", sorted(have.keys()))
    return have


def extract_icons():
    os.makedirs(ICO, exist_ok=True)
    saved = []
    for name, rel, box in ICON_SOURCES:
        path = rel if os.path.isabs(rel) else os.path.join(BASE, rel)
        if not os.path.exists(path):
            print("  skip (missing):", path)
            continue
        img = qc.load_rgb(path)
        x, y, w, h = box
        x = max(0, x); y = max(0, y)
        w = min(w, img.size[0] - x); h = min(h, img.size[1] - y)
        crop = img.crop((x, y, x + w, y + h))
        crop.save(os.path.join(ICO, "%s.png" % name))
        saved.append("%s (%dx%d)" % (name, w, h))
    print("  icons:", saved)
    return saved


if __name__ == "__main__":
    print("env:", qc.env_summary())
    print("extracting digit templates...")
    extract_digits()
    print("extracting icon templates...")
    extract_icons()
    print("done. templates under", os.path.join(HERE, "templates"))
