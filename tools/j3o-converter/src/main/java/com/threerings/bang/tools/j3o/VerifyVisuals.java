//
// $Id$

package com.threerings.bang.tools.j3o;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Phase&nbsp;7d per-town visual-regression driver. Reads the golden manifest
 * ({@code src/test/resources/visual/golden-manifest.txt}) and, for each declared render, re-renders
 * it through the <em>existing</em> Phase-5/6 offscreen harness mains ({@link RenderModelToPng},
 * {@link RenderSceneToPng}, {@link RenderParticleToPng}) and compares the result to the committed
 * golden PNG using the {@link SnapshotDiff} metric. This is the CI-ready regression gate for the
 * migrated renderer: it catches future rendering regressions (missing texture, broken material,
 * wrong framing, an effect that stops emitting) without a live server or the editor.
 *
 * <p>Two modes:
 * <ul>
 *   <li>{@code verify} (default) — re-render each golden, diff against {@code golden/<name>.png},
 *       and exit non-zero if any entry exceeds its per-entry tolerance (or its golden is missing).
 *       This is what {@code ./gradlew :tools:j3o-converter:verifyVisuals} runs.</li>
 *   <li>{@code capture} — (re)render every golden and write {@code golden/<name>.png}. Run this to
 *       seed a new golden or to refresh goldens after an <em>intended</em> render change, then
 *       commit the PNGs. This is {@code ./gradlew :tools:j3o-converter:captureGoldens}.</li>
 * </ul>
 *
 * <p>Each render is run in a <b>forked JVM</b> (the same main the goldens were captured with) rather
 * than inline, because every render needs its own LWJGL3 {@code OffscreenSurface} GL context and a
 * clean teardown; forking guarantees the verify render is bit-identical to the capture render. The
 * fork classpath is supplied by Gradle via {@code -Dvisual.forkClasspath=...} (the isolated
 * {@code renderToPngRuntime} classpath — jme3 + app only, no fork/gdx, no {@code org.lwjgl} sealing
 * clash).
 *
 * <p>The diff metric is duplicated from {@link SnapshotDiff} (mean absolute per-channel difference,
 * normalised to {@code [0,1]}) so the gate stays GL-free and self-contained; {@code SnapshotDiff}
 * remains the standalone single-pair tool.
 *
 * <p>Usage: {@code VerifyVisuals <verify|capture> <rsrcRoot> <goldenDir> <manifest> <tmpDir>}.
 */
public class VerifyVisuals
{
    public static void main (String[] args)
        throws Exception
    {
        if (args.length < 5) {
            System.err.println("Usage: VerifyVisuals <verify|capture> <rsrcRoot> <goldenDir> " +
                "<manifest> <tmpDir>");
            System.exit(2);
        }
        boolean capture = "capture".equals(args[0]);
        File rsrc = new File(args[1]).getAbsoluteFile();
        File goldenDir = new File(args[2]).getAbsoluteFile();
        File manifest = new File(args[3]).getAbsoluteFile();
        File tmpDir = new File(args[4]).getAbsoluteFile();
        goldenDir.mkdirs();
        tmpDir.mkdirs();

        String forkCp = System.getProperty("visual.forkClasspath");
        if (forkCp == null || forkCp.isEmpty()) {
            System.err.println("FAIL: -Dvisual.forkClasspath not set (Gradle supplies the isolated " +
                "renderToPngRuntime classpath)");
            System.exit(2);
        }

        List<Entry> entries = parse(manifest);
        System.out.println((capture ? "Capturing " : "Verifying ") + entries.size() +
            " goldens from " + manifest.getName() + " (rsrc=" + rsrc + ")");

        int failures = 0;
        for (Entry e : entries) {
            File golden = new File(goldenDir, e.name + ".png");
            File target = capture ? golden : new File(tmpDir, e.name + ".png");

            int rc = render(forkCp, rsrc, e, target);
            if (rc != 0) {
                System.out.printf("  %-22s RENDER-FAIL (exit %d)%n", e.name, rc);
                failures++;
                continue;
            }
            if (capture) {
                System.out.printf("  %-22s captured -> %s%n", e.name, golden.getName());
                continue;
            }

            if (!golden.isFile()) {
                System.out.printf("  %-22s MISSING-GOLDEN (run captureGoldens)%n", e.name);
                failures++;
                continue;
            }
            Result r = diff(target, golden, new File(tmpDir, e.name + "-diff.png"));
            boolean pass = r.ok && r.mean <= e.tolerance;
            System.out.printf("  %-22s meanDiff=%.6f changed=%.2f%% tol=%.3f -> %s%n",
                e.name, r.mean, r.changedPct, e.tolerance, pass ? "PASS" : "FAIL");
            if (!pass) {
                failures++;
            }
        }

        if (capture) {
            System.out.println("Captured " + (entries.size() - failures) + " of " + entries.size() +
                " goldens into " + goldenDir +
                (failures == 0 ? "" : " (" + failures + " FAILED to render)"));
            // exit non-zero if any capture render failed, so a broken golden refresh can't pass silently
            System.exit(failures == 0 ? 0 : 1);
        }
        System.out.println(failures == 0
            ? ("PASS: all " + entries.size() + " goldens within tolerance")
            : ("FAIL: " + failures + " of " + entries.size() + " goldens regressed"));
        System.exit(failures == 0 ? 0 : 1);
    }

    /** Forks a JVM running the appropriate render harness main for one manifest entry. */
    protected static int render (String forkCp, File rsrc, Entry e, File out)
        throws IOException, InterruptedException
    {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator +
            "java";
        List<String> cmd = new ArrayList<>();
        cmd.add(java);
        cmd.add("-cp");
        cmd.add(forkCp);
        switch (e.kind) {
        case "model":
            // args: modelType anim time
            cmd.add("com.threerings.bang.tools.j3o.RenderModelToPng");
            cmd.add(rsrc.getPath());
            cmd.add(e.args[0]);
            cmd.add(out.getPath());
            cmd.add(e.args.length > 1 ? e.args[1] : "standing");
            cmd.add(e.args.length > 2 ? e.args[2] : "0");
            break;
        case "prop":
            // static model, bind pose
            cmd.add("com.threerings.bang.tools.j3o.RenderModelToPng");
            cmd.add(rsrc.getPath());
            cmd.add(e.args[0]);
            cmd.add(out.getPath());
            break;
        case "scene":
            cmd.add("com.threerings.bang.tools.j3o.RenderSceneToPng");
            cmd.add(rsrc.getPath());
            cmd.add(out.getPath());
            // remaining specs (comma-separated single arg is accepted by RenderSceneToPng)
            cmd.add(e.args[0]);
            break;
        case "particle":
            cmd.add("com.threerings.bang.tools.j3o.RenderParticleToPng");
            cmd.add(rsrc.getPath());
            cmd.add(e.args[0]);
            cmd.add(out.getPath());
            cmd.add(e.args.length > 1 ? e.args[1] : "40");
            break;
        default:
            System.err.println("  unknown kind '" + e.kind + "' for " + e.name);
            return 3;
        }
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        // drain output (prefix harness chatter so it's attributable but quiet)
        try (var in = p.getInputStream()) {
            byte[] buf = new byte[4096];
            while (in.read(buf) >= 0) { /* discard; render mains are verbose */ }
        }
        return p.waitFor();
    }

    /** SnapshotDiff metric (mean absolute per-channel diff in [0,1]) + a heat-map diff image. */
    protected static Result diff (File rendered, File golden, File diffOut)
        throws IOException
    {
        BufferedImage a = ImageIO.read(rendered);
        BufferedImage b = ImageIO.read(golden);
        if (a == null || b == null ||
            a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return new Result(false, 1.0, 100.0);
        }
        int w = a.getWidth(), h = a.getHeight();
        BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        long total = 0, changed = 0;
        double maxPixel = 255.0 * 3;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pa = a.getRGB(x, y), pb = b.getRGB(x, y);
                int dr = Math.abs(((pa >> 16) & 0xFF) - ((pb >> 16) & 0xFF));
                int dg = Math.abs(((pa >> 8) & 0xFF) - ((pb >> 8) & 0xFF));
                int db = Math.abs((pa & 0xFF) - (pb & 0xFF));
                int d = dr + dg + db;
                total += d;
                if (d > 0) changed++;
                int gray = (((pa >> 16) & 0xFF) + ((pa >> 8) & 0xFF) + (pa & 0xFF)) / 6;
                int heat = (int)Math.min(255, d * 255.0 / maxPixel * 4);
                int r = Math.min(255, gray + heat);
                diff.setRGB(x, y, (r << 16) | (gray << 8) | gray);
            }
        }
        if (diffOut != null) {
            ImageIO.write(diff, "png", diffOut);
        }
        double mean = total / (maxPixel * w * h);
        return new Result(true, mean, (double)changed / (w * h) * 100);
    }

    /** Parses the pipe-separated manifest, skipping blanks and {@code #} comments. */
    protected static List<Entry> parse (File manifest)
        throws IOException
    {
        List<Entry> out = new ArrayList<>();
        for (String raw : java.nio.file.Files.readAllLines(manifest.toPath())) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\\|");
            if (f.length < 4) {
                System.err.println("skipping malformed manifest line: " + raw);
                continue;
            }
            Entry e = new Entry();
            e.name = f[0].trim();
            e.kind = f[1].trim();
            e.tolerance = Double.parseDouble(f[2].trim());
            e.args = f[3].trim().split("\\s+");
            out.add(e);
        }
        return out;
    }

    protected static class Entry
    {
        String name, kind;
        double tolerance;
        String[] args;
    }

    protected static class Result
    {
        final boolean ok;
        final double mean, changedPct;
        Result (boolean ok, double mean, double changedPct) {
            this.ok = ok; this.mean = mean; this.changedPct = changedPct;
        }
    }

    private VerifyVisuals () {}
}
