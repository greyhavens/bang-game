//
// $Id$

package com.threerings.bang.jme3.mxml;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.scene.Spatial;

import com.samskivert.util.StringUtil;

import com.threerings.jme.model.ModelTextureResolver;

/**
 * Epic 4 Phase 2b-ii <b>round-trip</b> parity harness: for each model it exercises the full glTF
 * cycle
 * <pre>
 *   .mxml -&gt; ModelDef -----(MxmlToGltf)-----&gt; model.gltf+model.bin
 *                                                   |
 *                                        (GltfToModelDef)
 *                                                   v
 *   model.properties + ModelDef' + AnimationDef' --(MxmlToJ3o.bake)--&gt; native model.j3o
 * </pre>
 * and then compares that native {@code .j3o} against the fork-wrapper <b>oracle</b> {@code .j3o} in
 * the staging tree with {@link MxmlParity}'s EXISTING structural ({@code ModelStats}/{@code
 * compare}) and render ({@code renderModel}/{@code meanDiff}) checks — the very same metric the
 * direct {@code .mxml}&rarr;{@code .j3o} parity used (2b-i). A pass means the glTF is a lossless
 * carrier of everything the bake needs: an <em>unedited</em> round-trip reproduces the oracle
 * {@code .j3o} exactly, as the direct path did.
 *
 * <p>{@code model.properties} is NOT round-tripped through glTF (it stays a committed game-config
 * source); the harness reads it straight from {@code srcRoot} for the bake, exactly as the Phase-4
 * build will.
 *
 * <p>Usage (mirrors {@code mxmlParity}):
 * <pre>
 * MxmlGltfParity &lt;src rsrc root&gt; &lt;oracle rsrc root&gt; &lt;work dir&gt;
 *                [--render] [--renderOnly] [--renders=bind|mid|both] [--tolerance=0.005] [--all]
 *                [--gltfRoot=&lt;committed glTF rsrc root&gt;] [modelType ...]
 * </pre>
 *
 * <p>With {@code --gltfRoot} the harness skips the {@link MxmlToGltf} write and reads the given
 * pre-existing {@code model.gltf} tree (normally the committed source tree itself) — the Phase-4
 * mode, verifying <em>committed glTF</em> &rarr; {@code .j3o} against the oracle, which is what
 * the {@link GltfToJ3o} build path actually bakes.
 *
 * <p>With {@code --renderOnly} (implies {@code --render}) a structural mismatch is reported but
 * does not fail the model or block the render step — the render diff alone is the gate. This is
 * the Phase-4 mode for <em>Blender-edited</em> re-exports: Blender re-splits/reorders vertices,
 * so structural parity is impossible by construction and the epic's acceptance metric for edited
 * models is render meanDiff &le; 0.005 (see {@code docs/} Epic 4 Phase 4).
 */
public class MxmlGltfParity
{
    public static void main (String[] args)
        throws Exception
    {
        List<String> types = new ArrayList<>();
        File srcRoot = null, oracleRoot = null, workDir = null, gltfRootArg = null;
        boolean render = false, renderOnly = false, all = false;
        String renders = "both";
        double tolerance = 0.005;
        int positional = 0;
        for (String arg : args) {
            if (arg.equals("--render")) {
                render = true;
            } else if (arg.equals("--renderOnly")) {
                // Blender-edited re-exports: structural parity is impossible (Blender re-splits
                // vertices), the render diff is the gate — report structure informationally only
                render = true;
                renderOnly = true;
            } else if (arg.startsWith("--renders=")) {
                renders = arg.substring("--renders=".length());
            } else if (arg.startsWith("--tolerance=")) {
                tolerance = Double.parseDouble(arg.substring("--tolerance=".length()));
            } else if (arg.equals("--all")) {
                all = true;
            } else if (arg.startsWith("--gltfRoot=")) {
                gltfRootArg = new File(arg.substring("--gltfRoot=".length())).getAbsoluteFile();
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
            System.err.println("Usage: MxmlGltfParity <src rsrc root> <oracle rsrc root> " +
                "<work dir> [--render] [--renderOnly] [--renders=bind|mid|both] " +
                "[--tolerance=0.005] [--all] [--gltfRoot=<committed glTF rsrc root>] " +
                "[modelType ...]");
            System.exit(2);
        }
        if (all) {
            types = MxmlParity.enumerateModels(srcRoot);
            if (types.isEmpty()) {
                System.err.println("FAIL: --all found no models under " + srcRoot);
                System.exit(2);
            }
        }
        // With --gltfRoot the harness reads a pre-existing (committed) model.gltf tree instead of
        // writing its own intermediates — the Phase-4 mode: committed glTF -> .j3o vs the oracle.
        boolean writeGltf = (gltfRootArg == null);
        File gltfRoot = writeGltf ? new File(workDir, "gltf") : gltfRootArg;
        File nativeRoot = new File(workDir, "native-rsrc"); // the round-tripped native .j3o tree
        File pngDir = new File(workDir, "png");
        if (writeGltf) {
            gltfRoot.mkdirs();
        } else if (!gltfRoot.isDirectory()) {
            System.err.println("FAIL: --gltfRoot must exist: " + gltfRoot);
            System.exit(2);
        }
        nativeRoot.mkdirs();
        pngDir.mkdirs();

        String forkCp = System.getProperty("mxml.forkClasspath");
        if (render && (forkCp == null || forkCp.isEmpty())) {
            System.err.println("FAIL: --render needs -Dmxml.forkClasspath");
            System.exit(2);
        }

        DesktopAssetManager bakeAm = MxmlToJ3o.createAssetManager(srcRoot);
        DesktopAssetManager oracleAm = MxmlToJ3o.createAssetManager(oracleRoot);
        DesktopAssetManager nativeAm = MxmlToJ3o.createAssetManager(nativeRoot, oracleRoot);

        int pass = 0, structFail = 0, bakeFail = 0, renderFail = 0;
        List<String> failures = new ArrayList<>();
        System.out.println("MxmlGltfParity: " + types.size() + " model(s), src=" + srcRoot +
            ", oracle=" + oracleRoot + (render ? ", render tolerance=" + tolerance : ""));

        for (String type : types) {
            System.out.println("== " + type);
            boolean ok = true;

            // (a) mxml -> glTF -> ModelDef' -> bake native .j3o
            File outDir = new File(nativeRoot, type);
            outDir.mkdirs();
            File nativeJ3o = new File(outDir, "model.j3o");
            try {
                File gltfDir = new File(gltfRoot, type);
                MxmlToGltf.Result wres = null;
                if (writeGltf) {
                    gltfDir.mkdirs();
                    wres = new MxmlToGltf().writeFromDir(new File(srcRoot, type), gltfDir);
                }
                GltfToModelDef.Parsed parsed = new GltfToModelDef()
                    .read(new File(gltfDir, "model.gltf"));

                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(
                        new File(new File(srcRoot, type), "model.properties"))) {
                    props.load(in);
                }
                String[] animNames = StringUtil.parseStringArray(
                    props.getProperty("animations", ""));
                AnimationDef[] adefs = new AnimationDef[animNames.length];
                for (int ii = 0; ii < animNames.length; ii++) {
                    adefs[ii] = parsed.animations.get(animNames[ii]);
                    if (adefs[ii] == null) {
                        throw new IllegalStateException("glTF is missing animation '" +
                            animNames[ii] + "' listed in model.properties");
                    }
                }
                MxmlToJ3o.Result baked = new MxmlToJ3o(new ModelTextureResolver(bakeAm), type)
                    .bake(props, parsed.model, animNames, adefs);
                BinaryExporter.getInstance().save(baked.root, nativeJ3o);

                if (wres != null && wres.maxInfluences > 4) {
                    System.out.println("  note: " + wres.maxInfluences +
                        " glTF influence slots (>4) preserved across " + (wres.maxInfluences / 4) +
                        " JOINTS/WEIGHTS sets; MxmlToJ3o caps to 4 at bake time");
                }
                // #45 finding 10: surface the otherwise write-only clampedInfluences flag
                if (baked.clampedInfluences) {
                    System.out.println("  note: bake clamped >4 bone influences to the top 4 " +
                        "(renormalised), matching the oracle");
                }
                if (!baked.droppedAnimations.isEmpty()) {
                    System.out.println("  note: dropped animations: " + baked.droppedAnimations);
                }
            } catch (Throwable t) {
                System.out.println("  BAKE-FAIL: " + t);
                t.printStackTrace(System.out);
                bakeFail++;
                failures.add(type + " (bake)");
                continue;
            }

            // (b) structural compare vs the oracle (reusing MxmlParity's metric)
            MxmlParity.ModelStats oracle, nativ;
            try {
                oracle = MxmlParity.ModelStats.of(MxmlParity.loadFresh(oracleAm, type));
                nativ = MxmlParity.ModelStats.of(MxmlParity.loadFresh(nativeAm, type));
            } catch (Throwable t) {
                System.out.println("  LOAD-FAIL: " + t);
                structFail++;
                failures.add(type + " (load)");
                continue;
            }
            List<String> problems = MxmlParity.compare(oracle, nativ);
            if (problems.isEmpty()) {
                System.out.println("  STRUCT OK: " + nativ.summary());
            } else if (renderOnly) {
                System.out.println("  STRUCT MISMATCH (" + problems.size() +
                    ") [informational; --renderOnly gates on the render diff]:");
                for (String p : problems) {
                    System.out.println("    " + p);
                }
            } else {
                System.out.println("  STRUCT MISMATCH (" + problems.size() + "):");
                for (String p : problems) {
                    System.out.println("    " + p);
                }
                ok = false;
            }

            // (c) render compare (reusing MxmlParity's forked renderer + diff metric)
            if (render && ok) {
                List<String[]> poses = MxmlParity.choosePoses(oracle, renders);
                for (String[] pose : poses) {
                    String tag = pose.length == 0 ? "bind" : (pose[0] + "@" + pose[1]);
                    String safe = type.replace('/', '_') + "-" + tag.replace('/', '_');
                    File a = new File(pngDir, safe + "-oracle.png");
                    File b = new File(pngDir, safe + "-native.png");
                    int rc1 = MxmlParity.renderModel(forkCp, oracleRoot.getPath(), type, a, pose);
                    int rc2 = MxmlParity.renderModel(forkCp,
                        nativeRoot.getPath() + File.pathSeparator + oracleRoot.getPath(),
                        type, b, pose);
                    if (rc1 != 0 || rc2 != 0) {
                        System.out.println("  RENDER-FAIL " + tag + " (oracle rc=" + rc1 +
                            ", native rc=" + rc2 + ")");
                        ok = false;
                        continue;
                    }
                    double mean = MxmlParity.meanDiff(b, a, new File(pngDir, safe + "-diff.png"));
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
        System.out.println("GLTF PARITY SUMMARY: " + pass + "/" + types.size() + " PASS (" +
            bakeFail + " bake failures, " + structFail + " structural, " +
            renderFail + " render)");
        if (!failures.isEmpty()) {
            System.out.println("failed: " + String.join(", ", failures));
        }
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    private MxmlGltfParity () { }
}
