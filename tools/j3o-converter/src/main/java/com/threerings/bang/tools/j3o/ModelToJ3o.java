//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryLoader;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.plugins.AWTLoader;

import com.threerings.bang.jme3.model.BangModelLoader;
import com.threerings.bang.jme3.model.ModelConverter;

/**
 * Build-time exporter / debugging aid: converts a compiled Bang model ({@code model.dat}) into
 * a jMonkeyEngine&nbsp;3 {@code .j3o} via the shared {@link ModelConverter}, then re-imports
 * the result headlessly and verifies mesh-stat parity (vertex/triangle counts, texture refs).
 *
 * <p>The primary runtime path is {@link BangModelLoader} (read {@code model.dat} directly);
 * this exporter exists to "freeze" a model to pure jME3 and to spot-check parity. It shares
 * 100% of its conversion logic with the runtime loader.
 *
 * <p>Usage: {@code ModelToJ3o <model.dat> <output dir> <rsrc root>}.
 */
public class ModelToJ3o
{
    public static void main (String[] args)
        throws Exception
    {
        if (args.length != 3) {
            System.err.println("Usage: ModelToJ3o <model.dat> <output dir> <rsrc root>");
            System.exit(1);
        }
        File datFile = new File(args[0]).getAbsoluteFile();
        File outDir = new File(args[1]).getAbsoluteFile();
        File rsrcRoot = new File(args[2]).getAbsoluteFile();
        if (!datFile.isFile() || !rsrcRoot.isDirectory()) {
            System.err.println("model.dat and rsrc root must exist.");
            System.exit(1);
        }
        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            System.err.println("Unable to create output directory " + outDir + ".");
            System.exit(1);
        }

        DesktopAssetManager assetManager = createAssetManager(rsrcRoot, outDir);

        // convert through the shared loader/converter
        BangModelLoader loader = new BangModelLoader();
        Spatial converted = loader.loadModel(assetManager, datFile, null);
        ModelConverter.Result result = loader.lastResult();
        reportCoverage(result);

        // export to .j3o
        String j3oName = converted.getName() + ".j3o";
        File j3oFile = new File(outDir, j3oName);
        BinaryExporter.getInstance().save(converted, j3oFile);
        System.out.println("Wrote " + j3oFile + " (" + j3oFile.length() + " bytes).");

        // re-import headlessly through the stock jME3 pipeline and verify parity
        Spatial reloaded = assetManager.loadModel(new ModelKey(j3oName));
        List<ModelConverter.GeoStats> actual = new ArrayList<>();
        collectStats(reloaded, "", actual);

        boolean ok = verify(result.stats, actual);
        System.exit(ok ? 0 : 2);
    }

    protected static void reportCoverage (ModelConverter.Result r)
    {
        if (!r.animations.isEmpty()) {
            System.out.println("Converted " + r.animations.size() + " animation(s): " +
                String.join(", ", r.animations));
        }
        if (r.skinnedMeshes > 0) {
            System.out.println("Converted " + r.skinnedMeshes + " skinned mesh(es)" +
                (r.clampedInfluences ? " (some vertices clamped to 4 influences)." : "."));
        }
        if (!r.droppedAnimations.isEmpty()) {
            System.out.println("NOTE: " + r.droppedAnimations.size() + " animation(s) dropped " +
                "(no keyframe data / no converted target): " +
                String.join(", ", r.droppedAnimations));
        }
        for (String c : r.droppedControllers) {
            System.out.println("NOTE: controller not converted (effects port): " + c);
        }
        for (String s : r.skippedSpatials) {
            System.out.println("NOTE: skipping procedural spatial: " + s);
        }
    }

    protected static DesktopAssetManager createAssetManager (File rsrcRoot, File outDir)
    {
        DesktopAssetManager am = new DesktopAssetManager();
        am.registerLocator("/", ClasspathLocator.class);
        am.registerLocator(rsrcRoot.getPath(), FileLocator.class);
        am.registerLocator(outDir.getPath(), FileLocator.class);
        am.registerLoader(J3MLoader.class, "j3m", "j3md");
        am.registerLoader(AWTLoader.class, "png", "jpg", "jpeg", "gif", "bmp");
        am.registerLoader(BinaryLoader.class, "j3o");
        return am;
    }

    protected static void collectStats (
        Spatial spatial, String path, List<ModelConverter.GeoStats> stats)
    {
        String childPath =
            path.isEmpty() ? spatial.getName() : (path + "/" + spatial.getName());
        if (spatial instanceof Geometry geom) {
            stats.add(ModelConverter.statsOf(childPath, geom));
        } else if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                collectStats(child, childPath, stats);
            }
        }
    }

    protected static boolean verify (
        List<ModelConverter.GeoStats> expected, List<ModelConverter.GeoStats> actual)
    {
        System.out.println();
        System.out.println("Verification (fork model vs re-imported .j3o):");
        boolean ok = true;
        if (expected.size() != actual.size()) {
            System.out.println("  MISMATCH: geometry count " + expected.size() +
                " != " + actual.size());
            ok = false;
        }
        // Skinned meshes are deliberately reparented to the model root (their vertices are baked
        // into model space; see ModelConverter.applySkinning), so the path/traversal-order is not
        // a stable identity any more. Match expected->actual as a multiset on content-minus-path
        // so a dropped texture/flipped flag/lost skin-width is still caught, but reparenting is not
        // reported as a mismatch.
        List<ModelConverter.GeoStats> pool = new ArrayList<>(actual);
        int totalVerts = 0, totalTris = 0;
        for (ModelConverter.GeoStats exp : expected) {
            int found = -1;
            for (int jj = 0; jj < pool.size(); jj++) {
                if (sameContent(exp, pool.get(jj))) {
                    found = jj;
                    break;
                }
            }
            boolean match = found >= 0;
            System.out.println("  " + (match ? "OK " : "MISMATCH ") + exp.path() +
                ": " + exp.vertices() + "v/" + exp.triangles() + "t/" + exp.texture() +
                (match ? "" : "\n      no actual geometry with matching content for " + exp));
            if (match) {
                pool.remove(found);
            }
            ok &= match;
            totalVerts += exp.vertices();
            totalTris += exp.triangles();
        }
        System.out.println("  " + expected.size() + " geometries, " + totalVerts +
            " vertices, " + totalTris + " triangles: " + (ok ? "PARITY OK" : "FAILED"));
        return ok;
    }

    /** Geometry parity on everything except the scene-graph path (which legitimately changes
     * when skinned meshes are reparented to the model root). */
    protected static boolean sameContent (ModelConverter.GeoStats a, ModelConverter.GeoStats b)
    {
        return a.vertices() == b.vertices() && a.triangles() == b.triangles()
            && java.util.Objects.equals(a.texture(), b.texture())
            && java.util.Objects.equals(a.glowTexture(), b.glowTexture())
            && java.util.Objects.equals(a.blend(), b.blend())
            && a.cullOff() == b.cullOff() && a.transparentBucket() == b.transparentBucket()
            && a.maxWeights() == b.maxWeights();
    }
}
