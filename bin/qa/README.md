# Bang! Howdy visual-QA toolkit

Automated visual checks for the dev loop, so the orchestrator can verify
rendering fixes **without a human eyeballing screenshots**. These are *tooling*
scripts — they read PNGs and print structured reports. They do **not** touch
game code, capture the live window, or launch the client.

All tools live under `bin/qa/`. They are pure-`stdlib` + **PIL/Pillow** +
**ImageMagick** (`compare`), with optional acceleration from `numpy` / `cv2` /
`pytesseract` if those are installed (auto-detected — see *Improving accuracy*).

```
bin/qa/
  qa_common.py        shared helpers (colour masks, connected components,
                      template match, diff, crop/upscale) — not run directly
  build_templates.py  (re)build the template library from baseline crops
  qa_ocr.py           [1] detect + read floating damage numbers
  qa_locate.py        [2] locate skull / reticle / starburst icons, check centring
  qa_presence.py      [3] is an expected element present? upright vs flat?
  qa_diff.py          [4] similarity vs a baseline + worst-differing tiles
  qa_crop.py          [5] crop + nearest-neighbour upscale a ROI for spot-check
  qa_report.py        run a frame through ALL checks, one structured report
  templates/
    digits/<d>.png    single-digit exemplars (colour-mask, white-on-black)
    icons/*.png       skull, skull_white, reticle, starburst crops
```

## Recommended dev-loop usage

After deploying a rendering fix, the orchestrator captures a frame with the
existing window-grabber and runs the master report:

```sh
bin/shot /tmp/frame.png                 # window-only grab (existing tool)
bin/qa/qa_report.py /tmp/frame.png \
    --icons skull,reticle \
    --save-crops /tmp/qa_crops          # crops every hit for optional spot-check
```

Add `--baseline correct/Foo.png` to also get a similarity score against a
known-good reference of the same situation, and `--region X,Y,W,H` to run the
presence/shape check on a specific area (e.g. where a building should be).

Coordinates are pixels of the input image, origin top-left: boxes are
`(x, y, w, h)`, centres are `(cx, cy)`.

---

## [1] Damage numbers — `qa_ocr.py`

Detects floating damage numbers, reads their value, and reports pixel position,
glyph height (size), and horizontal centring over the content below.

How it works: damage numbers are bold dark-outlined digits in a few saturated
colours (red / blue / green / yellow). For each colour we build a pixel mask,
flood-fill connected components into glyph candidates, group neighbours into a
number, and read it — via **tesseract** if installed, otherwise via a built-in
**digit-template matcher** (`templates/digits/`). Each candidate gets a
`confidence` in `[0,1]`; `--min-conf` (default 0.6) drops clutter (reticle arcs,
muzzle flash, grass that happen to match a colour).

```sh
qa_ocr.py <frame.png> [--color red,blue,green,yellow] [--min-conf 0.6]
          [--all] [--json] [--save-crops DIR]
```

Sample (known-good `baseline/correct/CorrectDamage.png`, a blue "17"):

```
number[0] value=17   color=blue   conf=0.95 bbox=[37, 22, 28, 25] h=25px center_off_x=+3 CENTERED (template)
```

Sample (`baseline/incorrect/DamageAndSkullIncorrectLocation.png`, a red "25"
that the bug pushed off-centre):

```
number[0] value=7    color=yellow conf=0.96 bbox=[35, 7, 13, 29] ... OFFSET (template)
number[1] value=25   color=red    conf=0.93 bbox=[97, 9, 49, 35] h=33px center_off_x=+11 CENTERED (template)
```

The reported `h=NNpx` is the size check ("small and centered above a unit");
`center_off_x` + `CENTERED/OFFSET` is the centring check.

---

## [2] Icons / reticle — `qa_locate.py`

Locates an icon by template matching against `templates/icons/*.png` and reports
whether it sits centred over the nearest unit (vs offset, like the
`AttackIconOffcenter` bug). Multi-scale; score is normalized cross-correlation
in `[-1,1]`; **>= 0.45 is a confident hit** for these distinctive icons.

```sh
qa_locate.py <frame.png> [--template skull|reticle|starburst|PATH ...]
             [--scales 0.8,1.0,1.25] [--json] [--save-crops DIR]
```

Samples (matched on their own source baselines):

```
# skull on baseline/correct/ShotgunnerDeathAndDamageCorrect.png
skull        score=1.000 FOUND  scale=1.00 center=[52.0, 75.0] off=21px CENTERED

# reticle on baseline/incorrect/AttackIconOffcenter.png  (the off-centre bug)
reticle      score=1.000 FOUND  scale=1.00 center=[99.0, 84.0] off=40px OFFSET

# starburst (get_bonus) on /tmp/get_bonus_after.png
starburst    score=1.000 FOUND  scale=1.00 center=[395.0, 310.0] off=1px CENTERED

# negative control: skull on a frame with no skull
skull        score=0.392 not-found
```

The "nearest unit" centre is estimated from local darkness/contrast against the
tan ground, so the centring verdict is approximate — treat `off=<~20px` as
centred, large offsets as the off-centre bug.

---

## [3] Element presence / shape — `qa_presence.py`

Decides whether an expected element rendered, and roughly classifies it as an
upright structure vs a flat-on-ground blob (the `InvisibleHomestead` /
collapsed-billboard class of bug). Measures `content_fraction` (non-ground
pixels), `edge_density`, vertical extent and aspect. Optionally compares against
a known-good and known-bad crop and reports which it resembles.

```sh
qa_presence.py <frame.png> [--box X,Y,W,H] [--baseline GOOD.png]
               [--baseline-bad BAD.png] [--json]
```

Samples:

```
# baseline/correct/HomesteadCorrect.png  (building present, tall)
present=True shape=upright content=0.65 edges=0.116 vext=1.00 aspect=1.66

# baseline/incorrect/HomeSteadBad.png    (degraded render — much less content)
present=True shape=upright content=0.19 edges=0.097 vext=0.71 aspect=1.16
```

The strongest signal for this bug class is `content_fraction` / `edge_density`
magnitude and the `--baseline`/`--baseline-bad` resemblance vote; raw "present"
is a coarse gate.

---

## [4] Reference diff — `qa_diff.py`

Similarity score between a current capture and a baseline of the same situation,
plus the worst-differing tiles (so you see *where* it diverged). Uses
ImageMagick `compare -metric RMSE` (and writes a heatmap with `--heatmap`),
falling back to a pure-PIL RMSE. Output `similarity = 1 - normalized_rmse`.

```sh
qa_diff.py <current.png> <baseline.png> [--grid 6x6] [--heatmap OUT.png]
           [--threshold 0.07] [--json]
```

Samples:

```
# identical images
engine=imagemagick similarity=1.000 normalized_rmse=0.0000 -> SIMILAR

# two near-identical gameplay frames
engine=imagemagick similarity=0.954 normalized_rmse=0.0460 -> SIMILAR
worst-differing tiles (row,col mean_diff bbox):
   r2 c2  diff=10.4  bbox=[341, 256, 171, 128]
```

Best used on same-size captures of the same camera/board; differently-sized
hand-crops are auto-resized first (a coarser comparison).

---

## [5] Crop + upscale — `qa_crop.py`

Crop a region and nearest-neighbour upscale it (pixel-art stays crisp) for a
quick human spot-check.

```sh
qa_crop.py <frame.png> --box X,Y,W,H  --out OUT.png [--pad 8] [--scale 4]
qa_crop.py <frame.png> --center CX,CY --size 80 --out OUT.png
```

---

## Master report — `qa_report.py`

Runs [1]+[2] always, plus [3] when `--region` is given and [4] when `--baseline`
is given.

```sh
qa_report.py <frame.png> [--baseline REF.png] [--region X,Y,W,H]
             [--icons skull,skull_white,reticle,starburst]
             [--save-crops DIR] [--min-conf 0.6] [--json]
```

Sample (`baseline/correct/ShotgunnerDeathAndDamageCorrect.png`):

```
================================================================
VISUAL-QA REPORT  ShotgunnerDeathAndDamageCorrect.png  (110x196)
env: numpy=False cv2=False tesseract=False
----------------------------------------------------------------
[1] DAMAGE NUMBERS: 5 detected
    value=10   color=green  size=27px conf=0.95 centring=-9px CENTERED (template)
    value=50   color=green  size=25px conf=0.93 centring=-6px CENTERED (template)
    ...
[2] ICONS / RETICLE:
    skull        FOUND score=1.00 at [52.0, 75.0] off=21px CENTERED
    reticle      not found (best score=0.30)
================================================================
```

---

## Building / updating the template library

```sh
python3 bin/qa/build_templates.py
```

Extracts digit exemplars from labelled baseline damage images and icon crops at
hand-specified boxes (see `DIGIT_SOURCES` / `ICON_SOURCES` in the script). Add a
new labelled baseline there and re-run to extend the library. Current digit
exemplars cover `0,1,2,5,7` (from the "17","50","25","10" baselines); add more
labelled-number baselines to cover `3,4,6,8,9` and improve template-OCR recall.

---

## Accuracy & limitations

- **Damage OCR.** On the cluttered baseline crops the real damage number is
  reliably the top-confidence hit and reads correctly (17/50/25/10 all read).
  False positives still occur from muzzle-flash / grass / reticle arcs that
  share a damage colour — they rank lower; raise `--min-conf` to suppress. The
  template digit-reader only knows the digits present in the exemplar set; until
  tesseract is installed, an unseen digit reads as the closest known one. On a
  real full frame with a single damage number the signal is much cleaner than on
  these effect-dense crops. **On a full 1024x768 board view** with lots of
  coloured terrain/UI, the colour heuristic produces many low-confidence false
  positives — when checking a damage number on a full frame, raise `--min-conf`
  to ~0.85 and read the top candidate(s), or pass `--region`/crop to the area of
  interest first. Centring is measured over content in a *local* window beneath
  the number (not the whole frame width).
- **Template matching.** NCC is robust for the distinctive icons (skull/reticle/
  starburst score ~0.9–1.0 at the right spot, ~0.4 elsewhere). It is *not*
  rotation- or heavy-scale-invariant; the multi-scale sweep covers ~0.8–1.25x.
- **"Nearest unit" centring** is a darkness/contrast heuristic, not object
  detection — good for "centred vs clearly offset", not sub-pixel.
- **Presence/shape** is region statistics; pair with `--baseline`/`--baseline-bad`
  for a more reliable good-vs-bad verdict.
- **Speed (pure-PIL fallback).** Connected-component flood fill and the
  coarse-to-fine NCC are pure Python. A full 1024x768 frame through `qa_report`
  with two icon templates runs in ~18s (each icon multi-scale match ~8-11s; OCR
  is sub-second). Installing `numpy`+`opencv` (`cv2.matchTemplate`) drops the
  icon matching to well under a second (auto-used when present).

## Improving accuracy (optional installs)

The toolkit auto-detects and uses these if present:

- `sudo apt-get install -y tesseract-ocr` + `pip install pytesseract`
  → real OCR for damage numbers (handles all digits, no template library
  needed; current env has neither, so the template reader is used).
- `pip install numpy opencv-python`
  → `cv2.matchTemplate` for icon location and vectorised colour masks/diff:
  large speedup and slightly better sub-pixel matches.
- `pip install scikit-image`
  → enables SSIM-based diff if you extend `qa_diff.py` (currently RMSE-based).

None are required — every check has a working pure-PIL/ImageMagick path.
