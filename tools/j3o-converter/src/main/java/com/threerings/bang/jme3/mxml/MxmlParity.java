//
// $Id$

package com.threerings.bang.jme3.mxml;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.TransformTrack;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import com.threerings.jme.model.ModelTextureResolver;

/**
 * Epic 4 Phase 2b parity harness: for each requested model it (a) bakes a <b>native</b>
 * {@code model.j3o} from the committed {@code .mxml} source via {@link MxmlToJ3o} into a scratch
 * overlay tree, (b) <b>structurally</b> compares it against the fork-wrapper <b>oracle</b>
 * {@code model.j3o} in the staging tree (geometry count, per-mesh vertex/triangle counts +
 * material/texture/skinning flags as an order-insensitive multiset, armature joint names, clip
 * names/durations/frame/track counts), and (c) optionally <b>render</b>-compares both through the
 * existing {@link com.threerings.bang.tools.j3o.RenderModelToPng} harness (forked JVMs, one GL
 * context each) with the {@link com.threerings.bang.tools.j3o.SnapshotDiff} metric.
 *
 * <p>Per the epic's Phase 3 note the comparison is <em>semantic</em>: ordering (geometry
 * traversal, joint list, clip map) may legitimately differ between two correct importers, so
 * everything is compared as multisets/sets, and renders are the primary gate.
 *
 * <p>Usage:
 * <pre>
 * MxmlParity &lt;src rsrc root&gt; &lt;oracle rsrc root&gt; &lt;work dir&gt;
 *            [--render] [--renders=bind|mid|both] [--tolerance=0.005] [--all]
 *            [modelType ...]
 * </pre>
 * Render mode forks {@code RenderModelToPng} JVMs on the classpath supplied via
 * {@code -Dmxml.forkClasspath} (the isolated {@code renderToPngRuntime} classpath — see
 * {@code build.gradle}'s {@code mxmlParity} task). Exits non-zero on any structural or render
 * failure.
 */
public class MxmlParity
{
    public static void main (String[] args)
        throws Exception
    {
        List<String> types = new ArrayList<>();
        File srcRoot = null, oracleRoot = null, workDir = null;
        boolean render = false, all = false;
        String renders = "both";
        double tolerance = 0.005;
        int positional = 0;
        for (String arg : args) {
            if (arg.equals("--render")) {
                render = true;
            } else if (arg.startsWith("--renders=")) {
                renders = arg.substring("--renders=".length());
            } else if (arg.startsWith("--tolerance=")) {
                tolerance = Double.parseDouble(arg.substring("--tolerance=".length()));
            } else if (arg.equals("--all")) {
                all = true;
            } else if (positional == 0) {
                srcRoot = new File(arg).getAbsoluteFile();
                positional++;
            } else if (positional == 1) {
                oracleRoot = new File(arg).getAbsoluteFile();
                positional++;
            } else if (positional == 2) {
                workDir = new File(arg).getAbsoluteFile();
                positional++;
            } else {
                types.add(arg);
            }
        }
        if (srcRoot == null || oracleRoot == null || workDir == null ||
            (types.isEmpty() && !all)) {
            System.err.println("Usage: MxmlParity <src rsrc root> <oracle rsrc root> <work dir> " +
                "[--render] [--renders=bind|mid|both] [--tolerance=0.005] [--all] [modelType ...]");
            System.exit(2);
        }
        if (all) {
            types = enumerateModels(srcRoot);
        }
        File nativeRoot = new File(workDir, "native-rsrc");
        File pngDir = new File(workDir, "png");
        nativeRoot.mkdirs();
        pngDir.mkdirs();

        String forkCp = System.getProperty("mxml.forkClasspath");
        if (render && (forkCp == null || forkCp.isEmpty())) {
            System.err.println("FAIL: --render needs -Dmxml.forkClasspath (Gradle supplies the " +
                "isolated renderToPngRuntime classpath)");
            System.exit(2);
        }

        // one asset manager per role: bake resolves textures from the committed source tree;
        // the native re-import reads the scratch overlay first, textures falling through
        DesktopAssetManager bakeAm = MxmlToJ3o.createAssetManager(srcRoot);
        DesktopAssetManager oracleAm = MxmlToJ3o.createAssetManager(oracleRoot);
        DesktopAssetManager nativeAm = MxmlToJ3o.createAssetManager(nativeRoot, oracleRoot);

        int pass = 0, structFail = 0, bakeFail = 0, renderFail = 0;
        List<String> failures = new ArrayList<>();
        System.out.println("MxmlParity: " + types.size() + " model(s), src=" + srcRoot +
            ", oracle=" + oracleRoot + (render ? ", render tolerance=" + tolerance : ""));

        for (String type : types) {
            System.out.println("== " + type);
            boolean ok = true;

            // (a) bake the native .j3o
            File outDir = new File(nativeRoot, type);
            outDir.mkdirs();
            File nativeJ3o = new File(outDir, "model.j3o");
            MxmlToJ3o.Result baked;
            try {
                baked = new MxmlToJ3o(new ModelTextureResolver(bakeAm), type)
                    .bake(new File(srcRoot, type));
                BinaryExporter.getInstance().save(baked.root, nativeJ3o);
            } catch (Throwable t) {
                System.out.println("  BAKE-FAIL: " + t);
                t.printStackTrace(System.out);
                bakeFail++;
                failures.add(type + " (bake)");
                continue;
            }
            if (!baked.droppedControllers.isEmpty()) {
                System.out.println("  note: uncompiled controllers (oracle drops them too): " +
                    baked.droppedControllers);
            }
            if (!baked.droppedAnimations.isEmpty()) {
                System.out.println("  note: dropped animations: " + baked.droppedAnimations);
            }

            // (b) structural compare vs the oracle
            ModelStats oracle, nativ;
            try {
                oracle = ModelStats.of(loadFresh(oracleAm, type));
                nativ = ModelStats.of(loadFresh(nativeAm, type));
            } catch (Throwable t) {
                System.out.println("  LOAD-FAIL: " + t);
                structFail++;
                failures.add(type + " (load)");
                continue;
            }
            List<String> problems = compare(oracle, nativ);
            if (problems.isEmpty()) {
                System.out.println("  STRUCT OK: " + nativ.summary());
            } else {
                System.out.println("  STRUCT MISMATCH (" + problems.size() + "):");
                for (String p : problems) {
                    System.out.println("    " + p);
                }
                ok = false;
            }

            // (c) render compare
            if (render && ok) {
                List<String[]> poses = choosePoses(oracle, renders);
                for (String[] pose : poses) {
                    String tag = pose.length == 0 ? "bind" : (pose[0] + "@" + pose[1]);
                    String safe = type.replace('/', '_') + "-" + tag.replace('/', '_');
                    File a = new File(pngDir, safe + "-oracle.png");
                    File b = new File(pngDir, safe + "-native.png");
                    int rc1 = renderModel(forkCp, oracleRoot.getPath(), type, a, pose);
                    int rc2 = renderModel(forkCp,
                        nativeRoot.getPath() + File.pathSeparator + oracleRoot.getPath(),
                        type, b, pose);
                    if (rc1 != 0 || rc2 != 0) {
                        System.out.println("  RENDER-FAIL " + tag + " (oracle rc=" + rc1 +
                            ", native rc=" + rc2 + ")");
                        ok = false;
                        continue;
                    }
                    double mean = meanDiff(b, a, new File(pngDir, safe + "-diff.png"));
                    boolean rpass = mean >= 0 && mean <= tolerance;
                    System.out.printf("  RENDER %-28s meanDiff=%.6f -> %s%n",
                        tag, mean, rpass ? "PASS" : "FAIL");
                    ok &= rpass;
                }
                if (!ok) {
                    renderFail++;
                }
            } else if (!ok) {
                structFail++;
            }

            if (ok) {
                pass++;
            } else if (!failures.contains(type)) {
                failures.add(type);
            }
        }

        System.out.println();
        System.out.println("PARITY SUMMARY: " + pass + "/" + types.size() + " PASS (" +
            bakeFail + " bake failures, " + structFail + " structural, " +
            renderFail + " render)");
        if (!failures.isEmpty()) {
            System.out.println("failed: " + String.join(", ", failures));
        }
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    /** Every directory holding a model.properties, relative to the root, sorted. */
    protected static List<String> enumerateModels (File srcRoot)
        throws IOException
    {
        List<String> out = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(srcRoot.toPath())) {
            walk.filter(p -> p.getFileName().toString().equals("model.properties"))
                .map(p -> srcRoot.toPath().relativize(p.getParent()).toString())
                .sorted()
                .forEach(out::add);
        }
        return out;
    }

    /** Loads {@code <type>/model.j3o} bypassing the asset cache (each compare needs a fresh,
     * unshared spatial). */
    protected static Spatial loadFresh (DesktopAssetManager am, String type)
    {
        ModelKey key = new ModelKey(type + "/model.j3o");
        am.deleteFromCache(key);
        Spatial spatial = am.loadModel(key);
        am.deleteFromCache(key);
        return spatial;
    }

    /** Which animation poses to render-compare: the alphabetically-first oracle clip at t=0
     * ("bind"-ish) and/or midpoint; a model with no clips renders once, unposed. */
    protected static List<String[]> choosePoses (ModelStats oracle, String renders)
    {
        List<String[]> poses = new ArrayList<>();
        if (oracle.clips.isEmpty()) {
            poses.add(new String[0]);
            return poses;
        }
        String first = oracle.clips.keySet().iterator().next(); // TreeMap: alphabetical
        if (!"mid".equals(renders)) {
            poses.add(new String[] { first, "0" });
        }
        if (!"bind".equals(renders)) {
            poses.add(new String[] { first, "-1" }); // negative = clip midpoint
        }
        return poses;
    }

    /** Forks a RenderModelToPng JVM (the VerifyVisuals pattern: drain output, bounded wait). */
    protected static int renderModel (
        String forkCp, String rsrcRoots, String type, File out, String[] pose)
        throws IOException, InterruptedException
    {
        String java = System.getProperty("java.home") + File.separator + "bin" +
            File.separator + "java";
        List<String> cmd = new ArrayList<>(List.of(java, "-cp", forkCp,
            "com.threerings.bang.tools.j3o.RenderModelToPng", rsrcRoots, type, out.getPath()));
        for (String p : pose) {
            cmd.add(p);
        }
        Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        StringBuilder log = new StringBuilder();
        Thread drain = new Thread(() -> {
            try (var in = proc.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    synchronized (log) {
                        if (log.length() < 16384) {
                            log.append(new String(buf, 0, n));
                        }
                    }
                }
            } catch (IOException ignore) { }
        });
        drain.setDaemon(true);
        drain.start();
        if (!proc.waitFor(120, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            return 124;
        }
        drain.join(2000);
        int rc = proc.exitValue();
        if (rc != 0) {
            String s;
            synchronized (log) {
                s = log.toString().strip();
            }
            s = s.substring(Math.max(0, s.length() - 1500));
            for (String line : s.split("\n")) {
                System.out.println("    | " + line);
            }
        }
        return rc;
    }

    /** The SnapshotDiff metric (mean absolute per-channel diff in [0,1]); -1 on read/size
     * problems. Writes the heat-map diff image. */
    protected static double meanDiff (File rendered, File baseline, File diffOut)
        throws IOException
    {
        BufferedImage a = ImageIO.read(rendered);
        BufferedImage b = ImageIO.read(baseline);
        if (a == null || b == null ||
            a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return -1;
        }
        int w = a.getWidth(), h = a.getHeight();
        BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        long total = 0;
        double maxPixel = 255.0 * 3;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pa = a.getRGB(x, y), pb = b.getRGB(x, y);
                int d = Math.abs(((pa >> 16) & 0xFF) - ((pb >> 16) & 0xFF))
                    + Math.abs(((pa >> 8) & 0xFF) - ((pb >> 8) & 0xFF))
                    + Math.abs((pa & 0xFF) - (pb & 0xFF));
                total += d;
                int gray = (((pa >> 16) & 0xFF) + ((pa >> 8) & 0xFF) + (pa & 0xFF)) / 6;
                int heat = (int)Math.min(255, d * 255.0 / maxPixel * 4);
                int r = Math.min(255, gray + heat);
                diff.setRGB(x, y, (r << 16) | (gray << 8) | gray);
            }
        }
        if (diffOut != null) {
            ImageIO.write(diff, "png", diffOut);
        }
        return total / (maxPixel * w * h);
    }

    // ================================================================================
    // structural model stats
    // ================================================================================

    /** Everything the structural compare looks at, extracted from a loaded .j3o. */
    protected static class ModelStats
    {
        /** Per-geometry content stats (order-insensitive multiset). */
        final List<GeoStat> geos = new ArrayList<>();

        /** Armature joint names (multiset — duplicates possible via unique-suffixing). */
        final List<String> jointNames = new ArrayList<>();

        /** Clip name -> [duration, frames, tracks]. */
        final Map<String, double[]> clips = new TreeMap<>();

        /** The advertised animation-name set (bang.animations user data). */
        final Set<String> animNames = new TreeSet<>();

        static ModelStats of (Spatial root)
        {
            ModelStats stats = new ModelStats();
            collectGeos(root, stats.geos);
            SkinningControl skin = root.getControl(SkinningControl.class);
            if (skin != null) {
                for (Joint j : skin.getArmature().getJointList()) {
                    stats.jointNames.add(j.getName());
                }
            }
            AnimComposer composer = root.getControl(AnimComposer.class);
            if (composer != null) {
                for (String name : composer.getAnimClipsNames()) {
                    AnimClip clip = composer.getAnimClip(name);
                    int frames = 0;
                    for (AnimTrack<?> track : clip.getTracks()) {
                        if (track instanceof TransformTrack tt) {
                            frames = Math.max(frames, tt.getTimes().length);
                        }
                    }
                    stats.clips.put(name, new double[] {
                        clip.getLength(), frames, clip.getTracks().length });
                }
            }
            Object names = root.getUserData("bang.animations");
            if (names instanceof String s && !s.isEmpty()) {
                for (String n : s.split(",")) {
                    stats.animNames.add(n);
                }
            }
            return stats;
        }

        String summary ()
        {
            int verts = 0, tris = 0;
            for (GeoStat g : geos) {
                verts += g.vertices;
                tris += g.triangles;
            }
            return geos.size() + " geometries, " + verts + " verts, " + tris + " tris, " +
                jointNames.size() + " joints, " + clips.size() + " clips";
        }
    }

    /** Per-geometry parity content (the ModelConverter.GeoStats fields, minus the path — kept
     * fork-free so this harness runs on the isolated classpath). */
    protected record GeoStat (int vertices, int triangles, String texture, String glowTexture,
                              String blend, boolean cullOff, boolean transparentBucket,
                              int maxWeights)
    {
        static GeoStat of (Geometry geom)
        {
            com.jme3.scene.Mesh m = geom.getMesh();
            Material mat = geom.getMaterial();
            return new GeoStat(m.getVertexCount(), m.getTriangleCount(),
                texName(mat, "DiffuseMap"), texName(mat, "GlowMap"),
                mat.getAdditionalRenderState().getBlendMode().name(),
                mat.getAdditionalRenderState().getFaceCullMode() == FaceCullMode.Off,
                geom.getLocalQueueBucket() == Bucket.Transparent,
                m.getMaxNumWeights());
        }

        static String texName (Material mat, String param)
        {
            MatParamTexture p = mat.getTextureParam(param);
            return (p == null || p.getTextureValue() == null ||
                p.getTextureValue().getKey() == null)
                ? null : p.getTextureValue().getKey().getName();
        }
    }

    protected static void collectGeos (Spatial spatial, List<GeoStat> out)
    {
        if (spatial instanceof Geometry geom) {
            out.add(GeoStat.of(geom));
        } else if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                collectGeos(child, out);
            }
        }
    }

    /** Semantic comparison; returns a list of human-readable problems (empty = parity). */
    protected static List<String> compare (ModelStats oracle, ModelStats nativ)
    {
        List<String> problems = new ArrayList<>();

        // geometries as a content multiset
        if (oracle.geos.size() != nativ.geos.size()) {
            problems.add("geometry count: oracle=" + oracle.geos.size() +
                " native=" + nativ.geos.size());
        }
        List<GeoStat> pool = new ArrayList<>(nativ.geos);
        for (GeoStat exp : oracle.geos) {
            int found = -1;
            for (int jj = 0; jj < pool.size(); jj++) {
                if (exp.equals(pool.get(jj))) {
                    found = jj;
                    break;
                }
            }
            if (found >= 0) {
                pool.remove(found);
            } else {
                problems.add("no native geometry matching oracle " + exp);
            }
        }
        for (GeoStat extra : pool) {
            problems.add("native has unmatched geometry " + extra);
        }

        // armature joints as a name multiset
        Map<String, Integer> oj = countNames(oracle.jointNames), nj = countNames(nativ.jointNames);
        if (!oj.equals(nj)) {
            HashSet<String> onlyOracle = new HashSet<>(oj.keySet());
            onlyOracle.removeAll(nj.keySet());
            HashSet<String> onlyNative = new HashSet<>(nj.keySet());
            onlyNative.removeAll(oj.keySet());
            problems.add("joints: oracle=" + oracle.jointNames.size() +
                " native=" + nativ.jointNames.size() +
                (onlyOracle.isEmpty() ? "" : " missing=" + onlyOracle) +
                (onlyNative.isEmpty() ? "" : " extra=" + onlyNative));
        }

        // clips: names, durations, frame + track counts
        if (!oracle.clips.keySet().equals(nativ.clips.keySet())) {
            problems.add("clip names: oracle=" + oracle.clips.keySet() +
                " native=" + nativ.clips.keySet());
        } else {
            for (Map.Entry<String, double[]> e : oracle.clips.entrySet()) {
                double[] o = e.getValue(), n = nativ.clips.get(e.getKey());
                if (Math.abs(o[0] - n[0]) > 1e-4) {
                    problems.add("clip '" + e.getKey() + "' duration: oracle=" + o[0] +
                        " native=" + n[0]);
                }
                if (o[1] != n[1]) {
                    problems.add("clip '" + e.getKey() + "' frames: oracle=" + (int)o[1] +
                        " native=" + (int)n[1]);
                }
                if (o[2] != n[2]) {
                    problems.add("clip '" + e.getKey() + "' tracks: oracle=" + (int)o[2] +
                        " native=" + (int)n[2]);
                }
            }
        }

        // advertised animation names (bang.animations)
        if (!Objects.equals(oracle.animNames, nativ.animNames)) {
            problems.add("bang.animations: oracle=" + oracle.animNames +
                " native=" + nativ.animNames);
        }
        return problems;
    }

    protected static Map<String, Integer> countNames (List<String> names)
    {
        Map<String, Integer> counts = new HashMap<>();
        for (String n : names) {
            counts.merge(n, 1, Integer::sum);
        }
        return counts;
    }

    private MxmlParity () { }
}
