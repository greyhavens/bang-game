#!/usr/bin/env python3
"""qa_report.py -- run a single frame through every visual-QA check and print
one structured report. This is the entry point for the dev loop:

    bin/shot /tmp/frame.png            # orchestrator captures (NOT done here)
    bin/qa/qa_report.py /tmp/frame.png # auto-verify the render

Checks:
  1. DAMAGE numbers  : value, colour, size (glyph height px), centring
  2. ICONS/RETICLE   : skull / reticle / starburst presence + centring over unit
  3. PRESENCE        : optional --region content/shape (building present? flat?)
  4. DIFF            : optional --baseline reference similarity + worst tiles

Damage and icon checks always run. Presence/diff run when --region / --baseline
are supplied. With --save-crops DIR, every detected element is cropped+upscaled
there for human spot-check.

Usage:
  qa_report.py <frame.png> [--baseline REF.png] [--region X,Y,W,H]
               [--icons skull,reticle,starburst] [--save-crops DIR]
               [--min-conf 0.6] [--json]
"""
import argparse
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import qa_common as qc

import qa_ocr
import qa_locate
import qa_presence
import qa_diff
from PIL import Image


def run_damage(frame, min_conf, save_crops):
    templates = qa_ocr.load_digit_templates()
    nums = qa_ocr.detect_numbers(frame, ["red", "blue", "green", "yellow"], 25)
    nums = [n for n in nums if n["confidence"] >= min_conf]
    out = []
    for i, num in enumerate(nums):
        crop = frame.crop((num["bbox"][0], num["bbox"][1],
                           num["bbox"][0] + num["bbox"][2],
                           num["bbox"][1] + num["bbox"][3]))
        value, method = qa_ocr.read_number(crop, num["members"], templates, num["color"])
        cen = qa_ocr.centering_report(frame, num)
        out.append({"value": value, "color": num["color"],
                    "bbox": num["bbox"], "glyph_height_px": num["glyph_height"],
                    "confidence": num["confidence"], "read_method": method,
                    "centering": cen})
        if save_crops:
            os.makedirs(save_crops, exist_ok=True)
            qc.crop_upscale(frame, num["bbox"]).save(
                os.path.join(save_crops, "damage_%d_%s.png" % (i, value or "x")))
    return out


def run_icons(frame, names, save_crops):
    templates = qa_locate.resolve_templates(names)
    scales = [0.8, 1.0, 1.25]
    out = []
    for name, path in templates:
        tmpl = qc.load_rgb(path)
        res = qa_locate.match_multiscale(frame, tmpl, scales)
        if res is None:
            continue
        cx, cy = res["center"]
        found = res["score"] >= qa_locate.HIT
        rec = {"template": name, "score": round(res["score"], 3), "found": found,
               "bbox": [int(v) for v in res["bbox"]],
               "center": [round(cx, 1), round(cy, 1)]}
        if found:
            uc = qa_locate.nearest_unit_center(frame, (cx, cy))
            if uc:
                off = ((cx - uc[0]) ** 2 + (cy - uc[1]) ** 2) ** 0.5
                rec["offset_px"] = round(off, 1)
                rec["centered"] = off <= max(18, 0.4 * max(res["bbox"][2:]))
            if save_crops:
                os.makedirs(save_crops, exist_ok=True)
                qc.crop_upscale(frame, res["bbox"]).save(
                    os.path.join(save_crops, "icon_%s.png" % name))
        out.append(rec)
    out.sort(key=lambda r: r["score"], reverse=True)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("frame")
    ap.add_argument("--baseline", default=None)
    ap.add_argument("--region", default=None, help="X,Y,W,H for presence check")
    ap.add_argument("--icons", default="skull,skull_white,reticle,starburst")
    ap.add_argument("--save-crops", default=None)
    ap.add_argument("--min-conf", type=float, default=0.6)
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    frame = qc.load_rgb(args.frame)
    icon_names = [n.strip() for n in args.icons.split(",") if n.strip()]

    report = {"frame": args.frame, "size": list(frame.size), "env": qc.env_summary()}
    report["damage"] = run_damage(frame, args.min_conf, args.save_crops)
    report["icons"] = run_icons(frame, icon_names, args.save_crops)

    if args.region:
        x, y, w, h = (int(v) for v in args.region.split(","))
        report["presence"] = qa_presence.analyse(frame.crop((x, y, x + w, y + h)))
        report["presence"]["region"] = [x, y, w, h]

    if args.baseline:
        norm = qa_diff.magick_compare(args.frame, args.baseline, None)
        if norm is None:
            base = qc.load_rgb(args.baseline)
            rmse, _ = qc.rmse_score(frame, base)
            norm = rmse / 255.0
        report["diff"] = {"baseline": args.baseline,
                          "normalized_rmse": round(norm, 4),
                          "similarity": round(1 - norm, 4),
                          "similar": norm <= 0.07}

    if args.json:
        print(json.dumps(report, indent=2))
        return

    print("=" * 64)
    print("VISUAL-QA REPORT  %s  (%dx%d)" % (args.frame, frame.size[0], frame.size[1]))
    print("env: numpy=%s cv2=%s tesseract=%s" %
          (report["env"]["numpy"], report["env"]["cv2"],
           report["env"]["tesseract_bin"]))
    print("-" * 64)
    print("[1] DAMAGE NUMBERS: %d detected" % len(report["damage"]))
    for d in report["damage"]:
        c = d["centering"]
        cs = ("%+.0fpx %s" % (c["offset_x"], "CENTERED" if c["centered"] else "OFFSET")) if c else "?"
        print("    value=%-4s color=%-6s size=%dpx conf=%.2f centring=%s (%s)"
              % (d["value"] or "?", d["color"], d["glyph_height_px"],
                 d["confidence"], cs, d["read_method"]))
    print("[2] ICONS / RETICLE:")
    for r in report["icons"]:
        if r["found"]:
            tail = ""
            if "offset_px" in r:
                tail = " off=%.0fpx %s" % (r["offset_px"],
                       "CENTERED" if r.get("centered") else "OFFSET")
            print("    %-12s FOUND score=%.2f at %s%s"
                  % (r["template"], r["score"], r["center"], tail))
        else:
            print("    %-12s not found (best score=%.2f)" % (r["template"], r["score"]))
    if "presence" in report:
        p = report["presence"]
        print("[3] PRESENCE region=%s: present=%s shape=%s content=%.2f edges=%.3f"
              % (p["region"], p["present"], p["shape"], p["content_fraction"],
                 p["edge_density"]))
    if "diff" in report:
        dd = report["diff"]
        print("[4] DIFF vs %s: similarity=%.3f -> %s"
              % (dd["baseline"], dd["similarity"],
                 "SIMILAR" if dd["similar"] else "DIFFERENT"))
    print("=" * 64)


if __name__ == "__main__":
    main()
