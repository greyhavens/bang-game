#!/usr/bin/env python3
"""qa_crop.py -- crop + nearest-neighbour upscale a region of interest for quick
human spot-check (pixel-art stays crisp).

Usage:
  qa_crop.py <frame.png> --box X,Y,W,H [--pad 8] [--scale 4] --out OUT.png
  qa_crop.py <frame.png> --center CX,CY --size N ...      (centred square crop)
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import qa_common as qc


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("frame")
    ap.add_argument("--box", help="X,Y,W,H")
    ap.add_argument("--center", help="CX,CY")
    ap.add_argument("--size", type=int, default=80)
    ap.add_argument("--pad", type=int, default=8)
    ap.add_argument("--scale", type=int, default=4)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    img = qc.load_rgb(args.frame)
    if args.box:
        box = tuple(int(v) for v in args.box.split(","))
    elif args.center:
        cx, cy = (int(v) for v in args.center.split(","))
        s = args.size
        box = (cx - s // 2, cy - s // 2, s, s)
    else:
        sys.exit("provide --box or --center")
    out = qc.crop_upscale(img, box, pad=args.pad, scale=args.scale)
    out.save(args.out)
    print("wrote %s (%dx%d, scale x%d) from box %s of %s"
          % (args.out, out.size[0], out.size[1], args.scale, box, args.frame))


if __name__ == "__main__":
    main()
