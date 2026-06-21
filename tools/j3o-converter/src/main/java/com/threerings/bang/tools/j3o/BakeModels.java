//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryLoader;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.scene.Spatial;
import com.jme3.texture.plugins.AWTLoader;

import com.threerings.bang.jme3.model.BangModelLoader;

/**
 * Build-time batch baker (Phase 3): walks a staged rsrc root, converts every {@code model.dat}
 * to a sibling {@code model.j3o} via the shared {@link com.threerings.bang.jme3.model.ModelConverter}
 * (the same engine {@link ModelToJ3o} verifies per-file), in a single JVM. The runtime client
 * loads these {@code model.j3o} through the stock jME3 {@code AssetManager} (see
 * {@code ModelCache}); no fork dependency reaches the runtime.
 *
 * <p>Usage: {@code BakeModels <rsrc root>}. The {@code <rsrc root>} is both the texture-resolution
 * root and where the {@code model.dat} files live (e.g. {@code assets/build/staging/rsrc}).
 */
public class BakeModels
{
    public static void main (String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: BakeModels <rsrc root>");
            System.exit(1);
        }
        File rsrcRoot = new File(args[0]).getAbsoluteFile();
        if (!rsrcRoot.isDirectory()) {
            System.err.println("rsrc root must exist: " + rsrcRoot);
            System.exit(1);
        }

        // one AssetManager rooted at the rsrc tree resolves textures for every model.
        DesktopAssetManager am = new DesktopAssetManager();
        am.registerLocator("/", ClasspathLocator.class);
        am.registerLocator(rsrcRoot.getPath(), FileLocator.class);
        am.registerLoader(J3MLoader.class, "j3m", "j3md");
        am.registerLoader(AWTLoader.class, "png", "jpg", "jpeg", "gif", "bmp");
        am.registerLoader(BinaryLoader.class, "j3o");

        List<Path> dats = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(rsrcRoot.toPath())) {
            walk.filter(p -> p.getFileName().toString().equals("model.dat")).forEach(dats::add);
        }

        int ok = 0, fail = 0;
        BangModelLoader loader = new BangModelLoader();
        BinaryExporter exporter = BinaryExporter.getInstance();
        for (Path dat : dats) {
            File datFile = dat.toFile();
            File j3oFile = new File(datFile.getParentFile(), "model.j3o");
            try {
                Spatial converted = loader.loadModel(am, datFile, null);
                exporter.save(converted, j3oFile);
                ok++;
            } catch (Throwable t) {
                fail++;
                System.err.println("FAILED to bake " + datFile + ": " + t);
                t.printStackTrace();
            }
        }
        System.out.println("Baked " + ok + " model.j3o (" + fail + " failed) under " + rsrcRoot);
        System.exit(fail == 0 ? 0 : 2);
    }
}
