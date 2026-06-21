#!/usr/bin/env python3
"""qa_presence.py -- decide whether an expected element actually rendered in a
region, and roughly classify its shape (upright structure vs flat blob).

Used for the "InvisibleHomestead" / "flat-on-ground vs upright billboard"
class of bugs. We do NOT need to know what the element is -- we measure whether
the region contains structured, non-ground content:

  - ground_fraction : fraction of region pixels that look like bare tan ground
  - content_fraction: 1 - ground_fraction (how much "stuff" is present)
  - edge_density    : Sobel-ish edge mass per pixel (structures have many edges)
  - vertical_extent : height of the content bounding box / region height
                      (upright billboards are tall; flat blobs are short/wide)
  - aspect          : content bbox height/width (>1 = upright, <1 = flat)

Then optionally compare against a baseline crop of the same situation
(--baseline correct.png / --baseline-bad incorrect.png) using qa_diff, picking
whichever it resembles more.

Usage:
  qa_presence.py <frame.png> [--box X,Y,W,H] [--baseline GOOD.png]
                 [--baseline-bad BAD.png] [--json]
If --box omitted the whole image is analysed.
A region is judged PRESENT when content_fraction and edge_density both clear
their thresholds.
"""
import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import qa_common as qc
from PIL import Image, ImageFilter

# bare desert ground is mid-bright tan; this rule flags "ground-like" pixels.
def is_ground(r, g, b):
    return 120 < r < 215 and 95 < g < 195 and 60 < b < 175 \
        and r >= g >= b and (r - b) < 95 and abs(r - g) < 55


def analyse(region):
    w, h = region.size
    px = region.load()
    ground = 0
    minx = w; miny = h; maxx = 0; maxy = 0
    content_pts = 0
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            if is_ground(r, g, b):
                ground += 1
            else:
                content_pts += 1
                if x < minx: minx = x
                if x > maxx: maxx = x
                if y < miny: miny = y
                if y > maxy: maxy = y
    n = w * h
    gf = ground / n if n else 1.0
    # edge density via PIL FIND_EDGES on luminance
    edges = region.convert("L").filter(ImageFilter.FIND_EDGES)
    epx = edges.load()
    esum = 0
    for y in range(h):
        for x in range(w):
            if epx[x, y] > 60:
                esum += 1
    ed = esum / n if n else 0.0
    if content_pts > 0:
        cw = maxx - minx + 1
        ch = maxy - miny + 1
        vext = ch / h
        aspect = ch / max(1, cw)
    else:
        cw = ch = 0; vext = 0.0; aspect = 0.0
    return {
        "ground_fraction": round(gf, 3),
        "content_fraction": round(1 - gf, 3),
        "edge_density": round(ed, 3),
        "content_bbox": [minx, miny, cw, ch] if content_pts else None,
        "vertical_extent": round(vext, 3),
        "aspect": round(aspect, 2),
        "shape": ("upright" if aspect >= 0.9 else "flat") if content_pts else "none",
    }


def diff_norm(a_path, b_path):
    import subprocess
    from shutil import which
    if which("compare") is None:
        return None
    p = subprocess.run(["compare", "-metric", "RMSE", a_path, b_path, os.devnull],
                       capture_output=True, text=True)
    e = p.stderr
    if "(" in e and ")" in e:
        try:
            return float(e[e.index("(") + 1:e.index(")")])
        except ValueError:
            return None
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("frame")
    ap.add_argument("--box", help="X,Y,W,H region to analyse")
    ap.add_argument("--baseline", help="known-good crop of same situation")
    ap.add_argument("--baseline-bad", help="known-bad crop of same situation")
    ap.add_argument("--min-content", type=float, default=0.15)
    ap.add_argument("--min-edges", type=float, default=0.04)
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    img = qc.load_rgb(args.frame)
    if args.box:
        x, y, w, h = (int(v) for v in args.box.split(","))
        region = img.crop((x, y, x + w, y + h))
    else:
        region = img
    res = analyse(region)
    res["present"] = (res["content_fraction"] >= args.min_content
                      and res["edge_density"] >= args.min_edges)

    if args.baseline or args.baseline_bad:
        cmp = {}
        if args.baseline:
            cmp["good_rmse"] = diff_norm(args.frame, args.baseline)
        if args.baseline_bad:
            cmp["bad_rmse"] = diff_norm(args.frame, args.baseline_bad)
        if cmp.get("good_rmse") is not None and cmp.get("bad_rmse") is not None:
            cmp["resembles"] = ("good" if cmp["good_rmse"] <= cmp["bad_rmse"]
                                else "bad")
        res["baseline_compare"] = cmp

    if args.json:
        print(json.dumps(res, indent=2))
    else:
        print("present=%s shape=%s content=%.2f edges=%.3f vext=%.2f aspect=%.2f"
              % (res["present"], res["shape"], res["content_fraction"],
                 res["edge_density"], res["vertical_extent"], res["aspect"]))
        if "baseline_compare" in res:
            bc = res["baseline_compare"]
            print("baseline: good_rmse=%s bad_rmse=%s -> resembles %s"
                  % (bc.get("good_rmse"), bc.get("bad_rmse"), bc.get("resembles", "?")))


if __name__ == "__main__":
    main()
