#!/usr/bin/env python3
"""qa_diff.py -- compare a current capture against a baseline reference of the
same situation; report a similarity score and highlight differing regions.

Two engines, picked automatically:
  - imagemagick `compare -metric RMSE/AE` (preferred; also writes a heatmap)
  - pure-PIL RMSE + per-tile difference grid (fallback)

It tiles the image into an NxM grid and reports which tiles differ most, so the
orchestrator can see *where* the render diverged (e.g. "tile row2,col1 differs"
== the unit/building area), not just a global number.

Usage:
  qa_diff.py <current.png> <baseline.png> [--grid 6x6] [--heatmap OUT.png]
             [--json] [--threshold 0.07]
Exit status 0 always; `similar` boolean in output reflects the threshold.
"""
import argparse
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import qa_common as qc
from PIL import Image, ImageChops


def magick_compare(cur_path, base_path, heatmap):
    """Return (rmse_fraction, normalized) via `compare -metric RMSE`, or None."""
    from shutil import which
    if which("compare") is None:
        return None
    out = heatmap or os.devnull
    # RMSE prints like "1234.5 (0.0188)" on stderr; the parenthesised value is
    # the normalized [0,1] RMSE.
    p = subprocess.run(["compare", "-metric", "RMSE", cur_path, base_path, out],
                       capture_output=True, text=True)
    err = p.stderr.strip()
    norm = None
    if "(" in err and ")" in err:
        try:
            norm = float(err[err.index("(") + 1:err.index(")")])
        except ValueError:
            norm = None
    return norm


def tile_diffs(cur, base, gx, gy):
    if cur.size != base.size:
        base = base.resize(cur.size)
    w, h = cur.size
    tiles = []
    diff = ImageChops.difference(cur, base)
    for j in range(gy):
        for i in range(gx):
            x0 = i * w // gx; x1 = (i + 1) * w // gx
            y0 = j * h // gy; y1 = (j + 1) * h // gy
            t = diff.crop((x0, y0, x1, y1))
            # mean abs diff across channels
            stat = 0
            cnt = 0
            for band in t.split():
                hist = band.histogram()
                for v, c in enumerate(hist):
                    stat += v * c; cnt += c
            mean = stat / cnt if cnt else 0
            tiles.append({"row": j, "col": i, "mean_diff": round(mean, 2),
                          "bbox": [x0, y0, x1 - x0, y1 - y0]})
    tiles.sort(key=lambda t: t["mean_diff"], reverse=True)
    return tiles


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("current")
    ap.add_argument("baseline")
    ap.add_argument("--grid", default="6x6")
    ap.add_argument("--heatmap", default=None)
    ap.add_argument("--threshold", type=float, default=0.07,
                    help="normalized RMSE below which images count as 'similar'")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    cur = qc.load_rgb(args.current)
    base = qc.load_rgb(args.baseline)
    gx, gy = (int(v) for v in args.grid.lower().split("x"))

    norm = magick_compare(args.current, args.baseline, args.heatmap)
    engine = "imagemagick"
    if norm is None:
        rmse, sim = qc.rmse_score(cur, base)
        norm = rmse / 255.0
        engine = "pil"

    sim = round(1.0 - norm, 4)
    tiles = tile_diffs(cur, base, gx, gy)
    result = {
        "engine": engine,
        "size_current": list(cur.size),
        "size_baseline": list(base.size),
        "normalized_rmse": round(norm, 4),
        "similarity": sim,
        "similar": norm <= args.threshold,
        "worst_tiles": tiles[:5],
    }
    if args.json:
        print(json.dumps(result, indent=2))
    else:
        print("engine=%s similarity=%.3f normalized_rmse=%.4f -> %s (thr=%.3f)"
              % (engine, sim, norm,
                 "SIMILAR" if result["similar"] else "DIFFERENT", args.threshold))
        print("worst-differing tiles (row,col mean_diff bbox):")
        for t in tiles[:5]:
            print("   r%d c%d  diff=%.1f  bbox=%s" %
                  (t["row"], t["col"], t["mean_diff"], t["bbox"]))
        if args.heatmap:
            print("heatmap written:", args.heatmap)


if __name__ == "__main__":
    main()
