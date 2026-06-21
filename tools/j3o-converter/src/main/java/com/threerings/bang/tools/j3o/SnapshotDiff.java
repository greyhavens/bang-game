//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Snapshot-diff helper (Phase&nbsp;5, harness&nbsp;#1, deliverable&nbsp;#3): given a rendered PNG and
 * a baseline PNG, computes the per-pixel difference, writes a heat-map diff image, and reports
 * pass/fail against a tolerance. This is the piece that makes the render harness a regression gate —
 * an agent or CI renders a deterministic snapshot, diffs it against the pre-cutover reference in
 * {@code baseline/fork-before/}, and fails the check when the mean difference exceeds the tolerance.
 *
 * <p>No GL context is needed (pure {@code ImageIO} + arithmetic), so this runs on any classpath.
 *
 * <p>The metric is the mean absolute per-channel difference normalised to {@code [0,1]} (0 =
 * identical, 1 = maximally different). The diff image highlights changed pixels in red over a dimmed
 * copy of the rendered image so the regions that moved are obvious. The process exits non-zero when
 * the mean difference exceeds the tolerance (default {@value #DEFAULT_TOLERANCE}), so it slots
 * directly into a CI gate.
 *
 * <p>Usage: {@code SnapshotDiff <rendered png> <baseline png> [diff png] [tolerance]}. If the two
 * images differ in size the comparison fails immediately (a resolution change is itself a
 * regression).
 */
public class SnapshotDiff
{
    public static void main (String[] args)
        throws IOException
    {
        if (args.length < 2) {
            System.err.println("Usage: SnapshotDiff <rendered png> <baseline png> " +
                "[diff png] [tolerance]");
            System.exit(2);
        }
        File rendered = new File(args[0]);
        File baseline = new File(args[1]);
        File diffOut = args.length > 2 ? new File(args[2]) : null;
        double tolerance = args.length > 3 ? Double.parseDouble(args[3]) : DEFAULT_TOLERANCE;

        BufferedImage a = ImageIO.read(rendered);
        BufferedImage b = ImageIO.read(baseline);
        if (a == null) {
            System.err.println("FAIL: cannot read rendered image " + rendered);
            System.exit(2);
        }
        if (b == null) {
            System.err.println("FAIL: cannot read baseline image " + baseline);
            System.exit(2);
        }
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            System.err.println("FAIL: size mismatch rendered=" + a.getWidth() + "x" + a.getHeight() +
                " baseline=" + b.getWidth() + "x" + b.getHeight());
            System.exit(1);
        }

        int w = a.getWidth(), h = a.getHeight();
        BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        long total = 0;          // summed absolute channel difference
        long changed = 0;        // count of pixels that differ at all
        double maxPixel = 255.0 * 3;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pa = a.getRGB(x, y), pb = b.getRGB(x, y);
                int dr = Math.abs(((pa >> 16) & 0xFF) - ((pb >> 16) & 0xFF));
                int dg = Math.abs(((pa >> 8) & 0xFF) - ((pb >> 8) & 0xFF));
                int db = Math.abs((pa & 0xFF) - (pb & 0xFF));
                int d = dr + dg + db;
                total += d;
                if (d > 0) {
                    changed++;
                }
                // heat map: dimmed rendered image, changed pixels pushed toward red by magnitude
                int gray = (((pa >> 16) & 0xFF) + ((pa >> 8) & 0xFF) + (pa & 0xFF)) / 6;
                int heat = (int)Math.min(255, d * 255.0 / maxPixel * 4);
                int r = Math.min(255, gray + heat);
                diff.setRGB(x, y, (r << 16) | (gray << 8) | gray);
            }
        }

        double mean = total / (maxPixel * w * h);          // [0,1]
        double changedFrac = (double)changed / (w * h);

        if (diffOut != null) {
            ImageIO.write(diff, "png", diffOut);
            System.out.println("Wrote diff " + diffOut.getAbsolutePath());
        }

        boolean pass = mean <= tolerance;
        System.out.printf("meanDiff=%.6f changedPixels=%.2f%% tolerance=%.6f -> %s%n",
            mean, changedFrac * 100, tolerance, pass ? "PASS" : "FAIL");
        System.exit(pass ? 0 : 1);
    }

    private SnapshotDiff () {}

    /** Default mean-difference tolerance: ~1.5% mean per-channel difference. */
    public static final double DEFAULT_TOLERANCE = 0.015;
}
