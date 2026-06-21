#!/usr/bin/env python3
"""Shared helpers for the Bang! Howdy visual-QA toolkit.

Pure-stdlib + PIL only (no numpy/cv2/skimage required). If numpy / cv2 /
pytesseract are present they are used opportunistically for speed/accuracy,
but every code path has a pure-PIL fallback so the toolkit works on the
minimal environment that ships with the dev box.

All geometry is reported in pixel coordinates of the *input image* with the
origin at the top-left corner, (x, y, w, h) for boxes and (cx, cy) for
centroids.
"""

import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageOps

# ---- optional accelerators -------------------------------------------------
try:
    import numpy as _np
    HAVE_NUMPY = True
except Exception:
    _np = None
    HAVE_NUMPY = False

try:
    import cv2 as _cv2
    HAVE_CV2 = True
except Exception:
    _cv2 = None
    HAVE_CV2 = False

try:
    import pytesseract as _pt
    HAVE_PYTESS = True
except Exception:
    _pt = None
    HAVE_PYTESS = False


def have_tesseract_bin():
    """Return True if the `tesseract` CLI is on PATH."""
    from shutil import which
    return which("tesseract") is not None


# ---- image loading ---------------------------------------------------------

def load_rgb(path):
    """Load an image as an RGB PIL Image, raising a friendly error."""
    if not os.path.exists(path):
        sys.exit("error: file not found: %s" % path)
    try:
        return Image.open(path).convert("RGB")
    except Exception as e:
        sys.exit("error: cannot open %s: %s" % (path, e))


# ---- colour masks ----------------------------------------------------------
# Damage numbers are bold, outlined digits in a small set of saturated colours:
#   red (~enemy/hit), blue (friendly), green (heal/special), yellow (bonus).
# We classify by simple per-pixel RGB rules that tolerate the dark outline.

COLOR_RULES = {
    # name : function(r,g,b) -> bool
    "red":    lambda r, g, b: r > 150 and g < 110 and b < 110 and r - max(g, b) > 50,
    "green":  lambda r, g, b: g > 140 and r < 150 and b < 150 and g - max(r, b) > 40,
    "blue":   lambda r, g, b: b > 150 and r < 150 and g < 190 and b - r > 50,
    "yellow": lambda r, g, b: r > 170 and g > 150 and b < 130 and min(r, g) - b > 50,
    "white":  lambda r, g, b: r > 200 and g > 200 and b > 200,
}


def color_mask(img, color):
    """Return an 'L' mode mask (255 where pixel matches `color`)."""
    rule = COLOR_RULES[color]
    src = img.load()
    w, h = img.size
    mask = Image.new("L", (w, h), 0)
    mpx = mask.load()
    for y in range(h):
        for x in range(w):
            r, g, b = src[x, y]
            if rule(r, g, b):
                mpx[x, y] = 255
    return mask


def mask_fast(img, color):
    """numpy-accelerated colour mask when numpy is available, else fallback."""
    if not HAVE_NUMPY:
        return color_mask(img, color)
    a = _np.asarray(img).astype(_np.int16)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    if color == "red":
        m = (r > 150) & (g < 110) & (b < 110) & (r - _np.maximum(g, b) > 50)
    elif color == "green":
        m = (g > 140) & (r < 150) & (b < 150) & (g - _np.maximum(r, b) > 40)
    elif color == "blue":
        m = (b > 150) & (r < 150) & (g < 190) & (b - r > 50)
    elif color == "yellow":
        m = (r > 170) & (g > 150) & (b < 130) & (_np.minimum(r, g) - b > 50)
    elif color == "white":
        m = (r > 200) & (g > 200) & (b > 200)
    else:
        raise ValueError(color)
    return Image.fromarray((m * 255).astype("uint8"), "L")


# ---- connected components (4-conn flood fill, pure python) -----------------

def connected_components(mask, min_area=20):
    """Find connected components of nonzero pixels in an 'L' mask.

    Returns a list of dicts: {bbox:(x,y,w,h), area, centroid:(cx,cy)}
    sorted by area descending. Pure-python flood fill; fine for 1024x768.
    """
    w, h = mask.size
    px = mask.load()
    seen = bytearray(w * h)
    comps = []
    stack = []
    for sy in range(h):
        base = sy * w
        for sx in range(w):
            if px[sx, sy] == 0 or seen[base + sx]:
                continue
            # flood fill
            minx = maxx = sx
            miny = maxy = sy
            area = 0
            sumx = sumy = 0
            stack.append((sx, sy))
            seen[base + sx] = 1
            while stack:
                cx, cy = stack.pop()
                area += 1
                sumx += cx
                sumy += cy
                if cx < minx: minx = cx
                if cx > maxx: maxx = cx
                if cy < miny: miny = cy
                if cy > maxy: maxy = cy
                for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
                    if 0 <= nx < w and 0 <= ny < h:
                        idx = ny * w + nx
                        if not seen[idx] and px[nx, ny]:
                            seen[idx] = 1
                            stack.append((nx, ny))
            if area >= min_area:
                comps.append({
                    "bbox": (minx, miny, maxx - minx + 1, maxy - miny + 1),
                    "area": area,
                    "centroid": (sumx / area, sumy / area),
                })
    comps.sort(key=lambda c: c["area"], reverse=True)
    return comps


def group_boxes(comps, max_gap=14):
    """Merge components whose bounding boxes are close horizontally and overlap
    vertically -- digits of the same number become one group."""
    boxes = [c["bbox"] for c in comps]
    used = [False] * len(boxes)
    groups = []
    for i in range(len(boxes)):
        if used[i]:
            continue
        x, y, w, h = boxes[i]
        gx0, gy0, gx1, gy1 = x, y, x + w, y + h
        used[i] = True
        changed = True
        while changed:
            changed = False
            for j in range(len(boxes)):
                if used[j]:
                    continue
                jx, jy, jw, jh = boxes[j]
                jx1, jy1 = jx + jw, jy + jh
                # vertical overlap?
                voverlap = min(gy1, jy1) - max(gy0, jy) > -8
                hgap = max(gx0, jx) - min(gx1, jx1)
                if voverlap and hgap < max_gap:
                    gx0 = min(gx0, jx); gy0 = min(gy0, jy)
                    gx1 = max(gx1, jx1); gy1 = max(gy1, jy1)
                    used[j] = True
                    changed = True
        groups.append((gx0, gy0, gx1 - gx0, gy1 - gy0))
    return groups


# ---- template matching -----------------------------------------------------

def to_gray(img):
    return img.convert("L")


def normalized_xcorr_pil(scene_gray, tmpl_gray, step=2):
    """Pure-PIL/python normalized cross-correlation template match.

    Returns (best_score, (x, y)) where score in [-1, 1] (higher = better) and
    (x, y) is the top-left of the best match. `step` strides the search to keep
    it fast; templates should be small crops (< ~80px). Coarse but adequate for
    locating distinctive icons (skull, reticle, star-burst).
    """
    sw, sh = scene_gray.size
    tw, th = tmpl_gray.size
    if tw > sw or th > sh:
        return (-1.0, (0, 0))
    spx = scene_gray.load()
    tpx = tmpl_gray.load()
    # precompute template mean/var
    tvals = [tpx[x, y] for y in range(th) for x in range(tw)]
    n = len(tvals)
    tmean = sum(tvals) / n
    tdev = [v - tmean for v in tvals]
    tnorm = sum(d * d for d in tdev) ** 0.5 or 1.0
    best = -2.0
    bestpos = (0, 0)
    for oy in range(0, sh - th + 1, step):
        for ox in range(0, sw - tw + 1, step):
            # window stats
            ssum = 0
            svals = []
            for ty in range(th):
                row = []
                yy = oy + ty
                for tx in range(tw):
                    v = spx[ox + tx, yy]
                    row.append(v)
                    ssum += v
                svals.append(row)
            smean = ssum / n
            num = 0.0
            sden = 0.0
            k = 0
            for ty in range(th):
                row = svals[ty]
                for tx in range(tw):
                    sd = row[tx] - smean
                    num += sd * tdev[k]
                    sden += sd * sd
                    k += 1
            denom = (sden ** 0.5) * tnorm or 1.0
            score = num / denom
            if score > best:
                best = score
                bestpos = (ox, oy)
    return (best, bestpos)


def match_template(scene_img, tmpl_img):
    """Locate `tmpl_img` in `scene_img`. Uses cv2 if available else PIL NCC.

    Returns dict {score, bbox:(x,y,w,h), center:(cx,cy)}.
    score is in [-1,1]; >=0.5 is a confident hit for these icons.
    """
    tw, th = tmpl_img.size
    if HAVE_CV2 and HAVE_NUMPY:
        scene = _cv2.cvtColor(_np.asarray(scene_img), _cv2.COLOR_RGB2GRAY)
        tmpl = _cv2.cvtColor(_np.asarray(tmpl_img), _cv2.COLOR_RGB2GRAY)
        res = _cv2.matchTemplate(scene, tmpl, _cv2.TM_CCOEFF_NORMED)
        _, maxv, _, maxloc = _cv2.minMaxLoc(res)
        x, y = maxloc
        return {"score": float(maxv), "bbox": (x, y, tw, th),
                "center": (x + tw / 2, y + th / 2)}
    sg = to_gray(scene_img)
    tg = to_gray(tmpl_img)
    sw, sh = sg.size
    # Coarse-to-fine: downscale both by `factor`, find the best coarse location,
    # then refine at full res in a small window. Keeps the pure-python NCC
    # tractable on full 1024x768 frames. `factor` is chosen to shrink a large
    # scene but is capped so the downscaled template stays usable (>=8px); we
    # never fall through to a full-resolution brute-force search on a big scene.
    factor = 1
    while (sw // (factor * 2)) * (sh // (factor * 2)) > 12000 \
            and min(tw, th) // (factor * 2) >= 8 and factor < 16:
        factor *= 2
    if factor > 1:
        sg_c = sg.resize((sw // factor, sh // factor), Image.BILINEAR)
        tg_c = tg.resize((max(8, tw // factor), max(8, th // factor)), Image.BILINEAR)
        _, (cx, cy) = normalized_xcorr_pil(sg_c, tg_c, step=1)
        # map back, refine in a +-factor*2 window. To keep refinement cheap for
        # big templates, downscale the refine window + template by `rf` so the
        # template's larger side is ~<=64px, then map the local hit back up.
        fx, fy = cx * factor, cy * factor
        pad = factor * 2
        rx0 = max(0, fx - pad); ry0 = max(0, fy - pad)
        rx1 = min(sw, fx + tw + pad); ry1 = min(sh, fy + th + pad)
        sub = sg.crop((rx0, ry0, rx1, ry1))
        rf = 1
        while max(tw, th) // (rf * 2) > 64:
            rf *= 2
        if rf > 1:
            sub_r = sub.resize((max(1, sub.size[0] // rf), max(1, sub.size[1] // rf)),
                               Image.BILINEAR)
            tg_r = tg.resize((max(8, tw // rf), max(8, th // rf)), Image.BILINEAR)
            score, (lx, ly) = normalized_xcorr_pil(sub_r, tg_r, step=1)
            x, y = rx0 + lx * rf, ry0 + ly * rf
        else:
            score, (lx, ly) = normalized_xcorr_pil(sub, tg, step=1)
            x, y = rx0 + lx, ry0 + ly
    else:
        # small scene: a strided full search then a tight refine.
        step = 2 if sw * sh > 12000 else 1
        s0, (cx, cy) = normalized_xcorr_pil(sg, tg, step=step)
        if step > 1:
            rx0 = max(0, cx - step); ry0 = max(0, cy - step)
            rx1 = min(sw, cx + tw + step); ry1 = min(sh, cy + th + step)
            sub = sg.crop((rx0, ry0, rx1, ry1))
            score, (lx, ly) = normalized_xcorr_pil(sub, tg, step=1)
            x, y = rx0 + lx, ry0 + ly
        else:
            score, (x, y) = s0, (cx, cy)
    return {"score": score, "bbox": (x, y, tw, th),
            "center": (x + tw / 2.0, y + th / 2.0)}


# ---- diff / similarity -----------------------------------------------------

def rmse_score(a, b):
    """Root-mean-square pixel error between two same-size RGB images, 0..255.
    Returns (rmse, similarity) where similarity = 1 - rmse/255."""
    if a.size != b.size:
        b = b.resize(a.size)
    if HAVE_NUMPY:
        aa = _np.asarray(a).astype(_np.float64)
        bb = _np.asarray(b).astype(_np.float64)
        rmse = float(_np.sqrt(((aa - bb) ** 2).mean()))
    else:
        diff = ImageChops_difference(a, b)
        # mean of squared via histogram
        sq = 0
        cnt = 0
        for band in diff.split():
            h = band.histogram()
            for i, c in enumerate(h):
                sq += (i * i) * c
                cnt += c
        rmse = (sq / cnt) ** 0.5 if cnt else 0.0
    return rmse, max(0.0, 1.0 - rmse / 255.0)


def ImageChops_difference(a, b):
    from PIL import ImageChops
    return ImageChops.difference(a, b)


# ---- ROI helpers -----------------------------------------------------------

def crop_upscale(img, bbox, pad=8, scale=4):
    """Crop bbox (with padding) and upscale by `scale` using nearest-neighbour
    so pixel-art stays crisp for human spot-check."""
    x, y, w, h = bbox
    x0 = max(0, x - pad); y0 = max(0, y - pad)
    x1 = min(img.size[0], x + w + pad); y1 = min(img.size[1], y + h + pad)
    crop = img.crop((x0, y0, x1, y1))
    return crop.resize((crop.size[0] * scale, crop.size[1] * scale), Image.NEAREST)


def env_summary():
    return {
        "numpy": HAVE_NUMPY,
        "cv2": HAVE_CV2,
        "pytesseract": HAVE_PYTESS,
        "tesseract_bin": have_tesseract_bin(),
    }
