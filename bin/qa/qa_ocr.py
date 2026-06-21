#!/usr/bin/env python3
"""qa_ocr.py -- detect and read floating damage numbers in a Bang frame.

Damage numbers are bold, dark-outlined digits in one of a few saturated
colours (red / blue / green / yellow). The approach:

  1. Build a colour mask for each candidate colour.
  2. Find connected components -> candidate digit glyphs.
  3. Group neighbouring glyphs into numbers (a "12" is two glyphs).
  4. Read each number:
       - if pytesseract+tesseract are installed, OCR the cropped number;
       - otherwise fall back to a built-in digit template matcher
         (templates/digits/<d>.png, auto-bootstrapped from baselines).
  5. Report each number: value, colour, bbox (x,y,w,h), centre, glyph height
     (a proxy for "small vs huge" rendering), and how centred it sits over
     the strongest non-number object below it.

Usage:
  qa_ocr.py <frame.png> [--color red,blue,green,yellow] [--min-area N]
            [--json] [--save-crops DIR]

Exit status is always 0; parse stdout (or --json) for results.
"""
import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import qa_common as qc
from PIL import Image

DIGIT_DIR = os.path.join(HERE, "templates", "digits")


def load_digit_templates():
    tmpl = {}
    if not os.path.isdir(DIGIT_DIR):
        return tmpl
    for fn in os.listdir(DIGIT_DIR):
        if fn.endswith(".png") and fn[0].isdigit():
            tmpl[fn[0]] = Image.open(os.path.join(DIGIT_DIR, fn)).convert("L")
    return tmpl


def ocr_tesseract(crop):
    import pytesseract
    txt = pytesseract.image_to_string(
        crop, config="--psm 7 -c tessedit_char_whitelist=0123456789")
    return "".join(ch for ch in txt if ch.isdigit())


def recognize_glyph(glyph_gray, templates):
    """Match one glyph crop against digit templates, return (digit, score)."""
    best = ("?", -2.0)
    for d, t in templates.items():
        # scale template to glyph height for a fair compare
        g = glyph_gray
        th = g.size[1]
        if th < 6:
            continue
        ratio = th / float(t.size[1])
        tt = t.resize((max(1, int(t.size[0] * ratio)), th), Image.NEAREST)
        # pad / crop widths to compare same canvas
        w = max(g.size[0], tt.size[0])
        cg = Image.new("L", (w, th), 0); cg.paste(g, (0, 0))
        ct = Image.new("L", (w, th), 0); ct.paste(tt, (0, 0))
        res = qc.match_template(cg.convert("RGB"), ct.convert("RGB"))
        if res["score"] > best[1]:
            best = (d, res["score"])
    return best


def read_number(crop_rgb, glyph_boxes, templates, color):
    """Read a multi-glyph number. crop_rgb is the full number crop; glyph_boxes
    are per-glyph bboxes relative to the original frame."""
    # tesseract path
    if qc.HAVE_PYTESS and qc.have_tesseract_bin():
        # upscale + binarize for tesseract
        big = crop_rgb.resize((crop_rgb.size[0] * 4, crop_rgb.size[1] * 4),
                              Image.LANCZOS)
        m = qc.mask_fast(big, color).filter(__import__("PIL.ImageFilter",
                                            fromlist=["MaxFilter"]).MaxFilter(3))
        # tesseract wants dark text on light bg
        from PIL import ImageOps
        val = ocr_tesseract(ImageOps.invert(m))
        if val:
            return val, "tesseract"
    # template fallback: recognize each glyph left-to-right
    if not templates:
        return "", "no-templates"
    digits = []
    for gb in sorted(glyph_boxes, key=lambda b: b[0]):
        x, y, w, h = gb
        mask = qc.mask_fast(crop_rgb, color)  # local mask
        # crop glyph from the color mask of the whole frame instead:
        d, sc = recognize_glyph(_glyph_mask_cache(color, x, y, w, h), templates)
        digits.append(d)
    return "".join(digits), "template"


_FRAME_MASK = {}


def _glyph_mask_cache(color, x, y, w, h):
    m = _FRAME_MASK[color]
    return m.crop((x, y, x + w, y + h))


def detect_numbers(frame, colors, min_area):
    """Detect damage-number candidates and score each by how digit-like it is.

    A real damage number is a short horizontal row (1-3 glyphs) of bold, solid
    colour with consistent glyph height. Reticle arcs / skull bones / grass also
    match the colour rules, so each candidate gets a confidence in [0,1] and the
    caller can filter on it. Scoring rewards:
      - glyph height in the typical damage range (~18-45 px on a 1024-crop)
      - per-glyph fill ratio (solid digits fill their bbox; arcs/grass are thin)
      - consistent glyph heights within the group
      - sane group aspect (width ~ n_glyphs * 0.6 * height)
    """
    results = []
    for color in colors:
        mask = qc.mask_fast(frame, color)
        _FRAME_MASK[color] = mask
        mpx = mask.load()
        comps = qc.connected_components(mask, min_area=min_area)
        if not comps:
            continue
        # per-component fill ratio (filled mask px / bbox area)
        glyphs = []
        for c in comps:
            x, y, w, h = c["bbox"]
            ar = w / max(1, h)
            if not (0.12 < ar < 2.8 and h >= 7):
                continue
            fill = c["area"] / float(w * h)
            c["fill"] = fill
            glyphs.append(c)
        groups = qc.group_boxes(glyphs)
        for gx, gy, gw, gh in groups:
            if gw * gh < min_area:
                continue
            members = [c for c in glyphs
                       if gx <= c["bbox"][0] <= gx + gw and gy - 4 <= c["bbox"][1] <= gy + gh]
            # drop thin members (arcs/grass strands have fill < 0.25)
            members = [m for m in members if m["fill"] >= 0.25]
            if not members:
                continue
            total_area = sum(m["area"] for m in members)
            heights = [m["bbox"][3] for m in members]
            fills = [m["fill"] for m in members]
            mh = sum(heights) / len(heights)
            mfill = sum(fills) / len(fills)
            # height-in-range score (real damage digits are ~18..45px tall;
            # sub-14px blobs are almost always reticle/grass speckle).
            if 18 <= mh <= 45:
                h_s = 1.0
            elif mh < 18:
                h_s = max(0.0, (mh - 8) / 10.0)
            else:
                h_s = max(0.0, 1.0 - (mh - 45) / 50.0)
            # fill score (solid glyphs ~0.4..0.9; thin arcs/grass < 0.3)
            fill_s = min(1.0, max(0.0, (mfill - 0.18) / 0.42))
            # height-consistency
            hvar = (max(heights) - min(heights)) / max(1.0, mh)
            cons_s = max(0.0, 1.0 - hvar)
            # group count: 1-3 glyphs ideal, more is suspicious
            n = len(members)
            n_s = 1.0 if 1 <= n <= 3 else max(0.0, 1.0 - (n - 3) * 0.4)
            # total colored area: real damage glyphs are bold (>=120 px total);
            # ground/arc speckle is small.
            area_s = min(1.0, total_area / 160.0)
            conf = round(0.24 * h_s + 0.22 * fill_s + 0.16 * cons_s
                         + 0.14 * n_s + 0.24 * area_s, 3)
            results.append({
                "color": color,
                "bbox": [gx, gy, gw, gh],
                "center": [gx + gw / 2.0, gy + gh / 2.0],
                "glyph_height": int(mh),
                "fill": round(mfill, 2),
                "n_glyphs": n,
                "confidence": conf,
                "members": [m["bbox"] for m in members],
            })
    results.sort(key=lambda r: r["confidence"], reverse=True)
    return results


def centering_report(frame, num):
    """How well is the number centred horizontally over content below it?
    Looks at the non-background object directly beneath the number and reports
    the horizontal offset of number-centre vs object-centre."""
    fw, fh = frame.size
    gx, gy, gw, gh = num["bbox"]
    ncx = gx + gw / 2.0
    # Sample a band below the number for unit/ground content. Restrict to a
    # LOCAL horizontal window (the unit a number floats over sits roughly under
    # it); scanning the whole frame width gives meaningless offsets on a busy
    # board. Window = number centre +- ~2.5 number-widths (min 60px each side).
    half = max(60, int(gw * 2.5))
    x0 = int(max(0, ncx - half)); x1 = int(min(fw, ncx + half))
    y0 = min(fh - 1, gy + gh + 2)
    y1 = min(fh, gy + gh + gh * 3 + 30)
    band = frame.crop((x0, y0, x1, y1)).convert("L")
    # find dark-ish / high-contrast column mass (a unit is usually darker than
    # the tan ground). Use column variance as a cheap "objectness".
    bpx = band.load()
    bw, bh = band.size
    if bh <= 0 or bw <= 0:
        return None
    col_score = [0] * bw
    for x in range(bw):
        vals = [bpx[x, y] for y in range(bh)]
        m = sum(vals) / len(vals)
        col_score[x] = sum((v - m) ** 2 for v in vals)
    total = sum(col_score) or 1
    cx = x0 + sum(x * col_score[x] for x in range(bw)) / total
    return {
        "object_center_x": round(cx, 1),
        "number_center_x": round(ncx, 1),
        "offset_x": round(ncx - cx, 1),
        "centered": abs(ncx - cx) <= max(10, gw * 0.6),
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("frame")
    ap.add_argument("--color", default="red,blue,green,yellow")
    ap.add_argument("--min-area", type=int, default=25)
    ap.add_argument("--min-conf", type=float, default=0.6,
                    help="drop candidates below this confidence (0..1)")
    ap.add_argument("--all", action="store_true",
                    help="show all candidates regardless of confidence")
    ap.add_argument("--json", action="store_true")
    ap.add_argument("--save-crops", default=None)
    args = ap.parse_args()

    frame = qc.load_rgb(args.frame)
    colors = [c.strip() for c in args.color.split(",") if c.strip()]
    templates = load_digit_templates()
    nums = detect_numbers(frame, colors, args.min_area)
    if not args.all:
        nums = [n for n in nums if n["confidence"] >= args.min_conf]

    out = []
    for i, num in enumerate(nums):
        crop = frame.crop((num["bbox"][0], num["bbox"][1],
                           num["bbox"][0] + num["bbox"][2],
                           num["bbox"][1] + num["bbox"][3]))
        value, method = read_number(crop, num["members"], templates, num["color"])
        cen = centering_report(frame, num)
        rec = {
            "index": i,
            "value": value,
            "read_method": method,
            "color": num["color"],
            "bbox": [int(v) for v in num["bbox"]],
            "center": [round(v, 1) for v in num["center"]],
            "glyph_height_px": int(num["glyph_height"]),
            "n_glyphs": num["n_glyphs"],
            "fill": num["fill"],
            "confidence": num["confidence"],
            "centering": cen,
        }
        out.append(rec)
        if args.save_crops:
            os.makedirs(args.save_crops, exist_ok=True)
            up = qc.crop_upscale(frame, num["bbox"])
            up.save(os.path.join(args.save_crops, "num_%d_%s.png" % (i, value or "x")))

    if args.json:
        print(json.dumps({"env": qc.env_summary(), "numbers": out}, indent=2))
    else:
        if not out:
            print("no damage numbers detected (colors=%s)" % ",".join(colors))
        for r in out:
            c = r["centering"]
            cs = ("center_off_x=%+.0f %s" % (c["offset_x"],
                  "CENTERED" if c["centered"] else "OFFSET")) if c else "n/a"
            print("number[%d] value=%-4s color=%-6s conf=%.2f bbox=%s h=%dpx %s (%s)"
                  % (r["index"], r["value"] or "?", r["color"], r["confidence"],
                     r["bbox"], r["glyph_height_px"], cs, r["read_method"]))


if __name__ == "__main__":
    main()
