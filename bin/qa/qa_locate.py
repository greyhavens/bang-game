#!/usr/bin/env python3
"""qa_locate.py -- locate an icon/reticle in a frame via template matching and
report whether it sits centred over the nearest unit.

Templates live in templates/icons/*.png (skull, skull_white, reticle,
starburst, ...). You can also pass an arbitrary --template path.

For each requested template we report the best match: score (NCC, [-1,1]),
bbox, centre. We then estimate the nearest "unit" (a dark, high-contrast blob
distinct from the tan/ground) and report the offset of the icon centre from
that unit's centre -- so the orchestrator can answer "is the reticle centred
over the target, or offset like the AttackIconOffcenter bug?".

Usage:
  qa_locate.py <frame.png> [--template NAME|PATH ...] [--json]
                [--scales 0.6,0.8,1.0,1.25] [--save-crops DIR]
If no --template is given, all templates in templates/icons are tried.
A score >= 0.45 is a confident hit for these distinctive icons.
"""
import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import qa_common as qc
from PIL import Image

ICON_DIR = os.path.join(HERE, "templates", "icons")
HIT = 0.45


def resolve_templates(names):
    out = []
    if not names:
        if os.path.isdir(ICON_DIR):
            for fn in sorted(os.listdir(ICON_DIR)):
                if fn.endswith(".png"):
                    out.append((fn[:-4], os.path.join(ICON_DIR, fn)))
        return out
    for n in names:
        if os.path.exists(n):
            out.append((os.path.splitext(os.path.basename(n))[0], n))
        else:
            p = os.path.join(ICON_DIR, n if n.endswith(".png") else n + ".png")
            if os.path.exists(p):
                out.append((n, p))
            else:
                print("warning: template not found: %s" % n, file=sys.stderr)
    return out


def match_multiscale(frame, tmpl, scales):
    best = None
    for s in scales:
        tw = max(4, int(tmpl.size[0] * s))
        th = max(4, int(tmpl.size[1] * s))
        if tw >= frame.size[0] or th >= frame.size[1]:
            continue
        t = tmpl.resize((tw, th), Image.LANCZOS)
        res = qc.match_template(frame, t)
        res["scale"] = s
        if best is None or res["score"] > best["score"]:
            best = res
    return best


def nearest_unit_center(frame, near_xy, search=140):
    """Estimate the centre of the nearest unit to point near_xy. Units are
    darker, high-contrast against the tan ground. We score a window's columns/
    rows by local contrast and return the contrast centroid."""
    fw, fh = frame.size
    nx, ny = near_xy
    x0 = int(max(0, nx - search)); x1 = int(min(fw, nx + search))
    y0 = int(max(0, ny - search)); y1 = int(min(fh, ny + search))
    win = frame.crop((x0, y0, x1, y1)).convert("L")
    wpx = win.load()
    ww, wh = win.size
    if ww < 4 or wh < 4:
        return None
    # darkness + edge mass: ground is mid/bright tan (~150), units darker.
    sx = sy = tot = 0.0
    for y in range(wh):
        for x in range(ww):
            v = wpx[x, y]
            w = max(0, 150 - v)  # how much darker than ground
            sx += x * w; sy += y * w; tot += w
    if tot <= 0:
        return None
    return (x0 + sx / tot, y0 + sy / tot)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("frame")
    ap.add_argument("--template", action="append", default=[])
    ap.add_argument("--scales", default="0.8,1.0,1.25")
    ap.add_argument("--hit", type=float, default=HIT)
    ap.add_argument("--json", action="store_true")
    ap.add_argument("--save-crops", default=None)
    args = ap.parse_args()

    frame = qc.load_rgb(args.frame)
    scales = [float(s) for s in args.scales.split(",") if s.strip()]
    templates = resolve_templates(args.template)
    if not templates:
        sys.exit("no templates to match (build with build_templates.py)")

    out = []
    for name, path in templates:
        tmpl = qc.load_rgb(path)
        res = match_multiscale(frame, tmpl, scales)
        if res is None:
            continue
        cx, cy = res["center"]
        found = res["score"] >= args.hit
        rec = {
            "template": name,
            "score": round(res["score"], 3),
            "found": found,
            "scale": res.get("scale", 1.0),
            "bbox": [int(v) for v in res["bbox"]],
            "center": [round(cx, 1), round(cy, 1)],
        }
        if found:
            uc = nearest_unit_center(frame, (cx, cy))
            if uc:
                off = ((cx - uc[0]) ** 2 + (cy - uc[1]) ** 2) ** 0.5
                rec["nearest_unit_center"] = [round(uc[0], 1), round(uc[1], 1)]
                rec["offset_px"] = round(off, 1)
                rec["offset_xy"] = [round(cx - uc[0], 1), round(cy - uc[1], 1)]
                rec["centered"] = off <= max(18, 0.4 * max(res["bbox"][2], res["bbox"][3]))
        out.append(rec)
        if args.save_crops and found:
            os.makedirs(args.save_crops, exist_ok=True)
            qc.crop_upscale(frame, res["bbox"]).save(
                os.path.join(args.save_crops, "loc_%s.png" % name))

    out.sort(key=lambda r: r["score"], reverse=True)
    if args.json:
        print(json.dumps({"env": qc.env_summary(), "matches": out}, indent=2))
    else:
        for r in out:
            tail = ""
            if r.get("found") and "offset_px" in r:
                tail = " unit=%s off=%.0fpx %s" % (
                    r["nearest_unit_center"], r["offset_px"],
                    "CENTERED" if r.get("centered") else "OFFSET")
            print("%-12s score=%.3f %-9s scale=%.2f center=%s bbox=%s%s" % (
                r["template"], r["score"],
                "FOUND" if r["found"] else "not-found",
                r["scale"], r["center"], r["bbox"], tail))


if __name__ == "__main__":
    main()
