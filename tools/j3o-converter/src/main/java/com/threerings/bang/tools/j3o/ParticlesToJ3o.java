//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.jme.util.TextureKey;
import com.jme.util.export.binary.BinaryImporter;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryLoader;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.scene.Spatial;
import com.jme3.texture.plugins.AWTLoader;

import com.threerings.bang.jme3.model.ParticleConverter;

/**
 * Build-time batch baker (Phase&nbsp;6a): walks a staged rsrc root, reads every fork-format
 * {@code effects/&lt;key&gt;/particles.jme} (a fork {@code BinaryExporter}
 * {@link com.jmex.effects.particles.ParticleGeometry} graph) through the fork
 * {@code BinaryImporter}, re-authors it as a jMonkeyEngine&nbsp;3 {@link com.jme3.effect.ParticleEmitter}
 * via {@link ParticleConverter}, and writes a sibling {@code particles.j3o}. The runtime
 * {@code ParticleCache} loads these {@code .j3o} through the stock jME3 {@code AssetManager}; no
 * fork dependency reaches the runtime.
 *
 * <p>This is the particle analogue of {@link BakeModels}. It runs in a single headless JVM on the
 * fork-coupled build-time classpath (it reads {@code com.jme.*}/{@code com.jmex.*}).
 *
 * <p>Usage: {@code ParticlesToJ3o <rsrc root> [single effect key]}. With one argument it bakes the
 * whole corpus; with a second argument (e.g. {@code boom_town/barrel_explosion}) it bakes a single
 * effect and prints its conversion report (handy for spot-checking the mapping).
 */
public class ParticlesToJ3o
{
    public static void main (String[] args)
        throws Exception
    {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: ParticlesToJ3o <rsrc root> [effect key]");
            System.exit(1);
        }
        File rsrcRoot = new File(args[0]).getAbsoluteFile();
        if (!rsrcRoot.isDirectory()) {
            System.err.println("rsrc root must exist: " + rsrcRoot);
            System.exit(1);
        }
        // the fork particle/texture reader touches the fork display system; install a headless one
        // (mirrors BangModelLoader.ensureDisplaySystem) before any fork render-state setup runs.
        ensureDisplaySystem();

        DesktopAssetManager am = createAssetManager(rsrcRoot);

        List<Path> jmes = new ArrayList<>();
        if (args.length == 2) {
            jmes.add(rsrcRoot.toPath().resolve("effects").resolve(args[1]).resolve("particles.jme"));
        } else {
            Path effects = rsrcRoot.toPath().resolve("effects");
            if (Files.isDirectory(effects)) {
                try (Stream<Path> walk = Files.walk(effects)) {
                    walk.filter(p -> p.getFileName().toString().equals("particles.jme"))
                        .forEach(jmes::add);
                }
            }
        }

        boolean verbose = args.length == 2;
        int ok = 0, fail = 0, degraded = 0;
        Map<String, Integer> droppedTally = new TreeMap<>();
        BinaryExporter exporter = BinaryExporter.getInstance();

        for (Path jme : jmes) {
            File jmeFile = jme.toFile();
            String key = effectKey(rsrcRoot, jmeFile);
            File outFile = new File(jmeFile.getParentFile(), "particles.j3o");
            try {
                ParticleConverter.Result result = bakeOne(am, jmeFile, key, exporter, outFile);
                ok++;
                boolean clean = result.droppedInfluences.isEmpty() && result.texturesResolved;
                if (!clean) {
                    degraded++;
                }
                for (String d : result.droppedInfluences) {
                    droppedTally.merge(d, 1, Integer::sum);
                }
                if (verbose) {
                    System.out.println("Converted " + key + ": " + result.emitters +
                        " emitter(s) -> " + outFile);
                    for (String n : result.notes) {
                        System.out.println("  note: " + n);
                    }
                    for (String d : result.droppedInfluences) {
                        System.out.println("  DROPPED (no jME3 equivalent): " + d);
                    }
                    if (!result.texturesResolved) {
                        System.out.println("  WARNING: one or more textures unresolved");
                    }
                }
            } catch (Throwable t) {
                fail++;
                System.err.println("FAILED to bake " + jmeFile + ": " + t);
                t.printStackTrace();
            }
        }

        System.out.println("Baked " + ok + " particles.j3o (" + fail + " failed, " + degraded +
            " degraded/lossy) under " + rsrcRoot + "/effects");
        if (!droppedTally.isEmpty()) {
            System.out.println("Fork influences with no jME3 equivalent (dropped):");
            for (Map.Entry<String, Integer> e : droppedTally.entrySet()) {
                System.out.println("  " + e.getKey() + " x" + e.getValue());
            }
        }
        System.exit(fail == 0 ? 0 : 2);
    }

    /** Imports one fork particles.jme, converts it, and writes the sibling .j3o. */
    protected static ParticleConverter.Result bakeOne (
        DesktopAssetManager am, File jmeFile, String key, BinaryExporter exporter, File outFile)
        throws Exception
    {
        final File parent = jmeFile.getParentFile();
        // the fork stored bare texture filenames relative to the effect dir; set the same
        // TextureKey location override the fork ParticleCache used so embedded TextureStates
        // resolve their image location during import.
        TextureKey.setLocationOverride(new TextureKey.LocationOverride() {
            public URL getLocation (String file) throws MalformedURLException {
                return new URL(parent.toURI().toURL(), file);
            }
        });
        com.jme.scene.Spatial forkRoot;
        try (InputStream in = new FileInputStream(jmeFile)) {
            forkRoot = (com.jme.scene.Spatial)BinaryImporter.getInstance().load(in);
        } finally {
            TextureKey.setLocationOverride(null);
        }
        // the runtime ParticleCache applies the particles.properties `scale` as the effect node's
        // localScale, but jME3 ignores the node transform for world-space (non-following) emitters
        // (BUG-7) — so bake that scale into those emitters' per-particle size/velocity/gravity at
        // convert time. Read the same sidecar/ default (0.025) the runtime uses.
        float scale = readEffectScale(parent);
        ParticleConverter converter = new ParticleConverter(am, key, scale);
        ParticleConverter.Result result = converter.convert(forkRoot);
        Spatial root = result.root;
        if (root == null) {
            throw new IllegalStateException("no convertible particle geometry in " + jmeFile);
        }
        exporter.save(root, outFile);
        return result;
    }

    /**
     * Reads the effect's {@code particles.properties} {@code scale} (the value the runtime
     * {@code ParticleCache} applies as the effect node's localScale), defaulting to the same
     * {@code 0.025} the runtime uses when the sidecar is missing or unreadable.
     */
    protected static float readEffectScale (File effectDir)
    {
        File props = new File(effectDir, "particles.properties");
        if (props.isFile()) {
            try (FileInputStream in = new FileInputStream(props)) {
                java.util.Properties p = new java.util.Properties();
                p.load(in);
                return Float.parseFloat(p.getProperty("scale", "0.025"));
            } catch (Exception e) {
                System.err.println("  (could not read " + props + ": " + e + "; using 0.025)");
            }
        }
        return 0.025f;
    }

    protected static DesktopAssetManager createAssetManager (File rsrcRoot)
    {
        DesktopAssetManager am = new DesktopAssetManager();
        am.registerLocator("/", ClasspathLocator.class);
        am.registerLocator(rsrcRoot.getPath(), FileLocator.class);
        am.registerLoader(J3MLoader.class, "j3m", "j3md");
        am.registerLoader(AWTLoader.class, "png", "jpg", "jpeg", "gif", "bmp");
        am.registerLoader(BinaryLoader.class, "j3o");
        return am;
    }

    /** {@code <rsrc>/effects/<key>/particles.jme} -> {@code <key>}. */
    protected static String effectKey (File rsrcRoot, File jmeFile)
    {
        Path effects = rsrcRoot.toPath().resolve("effects");
        Path dir = jmeFile.getParentFile().toPath();
        return effects.relativize(dir).toString().replace(File.separatorChar, '/');
    }

    /** Installs the headless fork display system (mirrors BangModelLoader). */
    protected static synchronized void ensureDisplaySystem ()
    {
        if (!_displayReady) {
            new com.jme.util.DummyDisplaySystem();
            com.jme.util.LoggingSystem.getLogger().setLevel(Level.WARNING);
            _displayReady = true;
        }
    }

    protected static boolean _displayReady;
}
