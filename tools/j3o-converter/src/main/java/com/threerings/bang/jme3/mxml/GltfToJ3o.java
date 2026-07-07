//
// $Id$

package com.threerings.bang.jme3.mxml;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;

import com.samskivert.util.StringUtil;

import com.threerings.jme.model.ModelTextureResolver;

/**
 * Build-time batch baker for the Phase-4 glTF source path: walks a source rsrc root, converts
 * every committed {@code model.gltf} (+{@code model.bin}) to a staged {@code model.j3o} via
 * {@link GltfToModelDef} feeding {@link MxmlToJ3o} — the exact round-trip pipeline
 * {@code MxmlGltfParity} verified at 310/310 against the fork-wrapper oracle. This replaces
 * BOTH legacy steps ({@code CompileModelTask}'s {@code .mxml}&rarr;{@code model.dat} compile and
 * {@code BakeModels}' {@code .dat}&rarr;{@code .j3o} bake) with a single fork-free one.
 *
 * <p>{@code model.properties} is read from the source tree (it stays committed game-side config,
 * never baked into the glTF), exactly as the parity harness does.
 *
 * <p>Before baking, verifies the migration is complete: every directory carrying a
 * {@code model.properties} must also carry a {@code model.gltf} (the moral equivalent of
 * {@code compileModels}' staged-{@code model.dat} check — {@code CompileModelTask} enumerates by
 * {@code model.properties}, so that is the authoritative model list).
 *
 * <p>Usage: {@code GltfToJ3o <src rsrc root> <staging rsrc root>} (e.g. {@code assets/rsrc}
 * {@code assets/build/staging/rsrc}).
 */
public class GltfToJ3o
{
    public static void main (String[] args)
        throws Exception
    {
        if (args.length != 2) {
            System.err.println("Usage: GltfToJ3o <src rsrc root> <staging rsrc root>");
            System.exit(1);
        }
        File srcRoot = new File(args[0]).getAbsoluteFile();
        File stagingRoot = new File(args[1]).getAbsoluteFile();
        if (!srcRoot.isDirectory()) {
            System.err.println("src rsrc root must exist: " + srcRoot);
            System.exit(1);
        }

        // enumerate by model.properties (the authoritative list CompileModelTask used) and
        // require a committed model.gltf beside every one — a partial migration must fail loudly,
        // not quietly stage fewer models than the .mxml path did.
        List<String> types = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(srcRoot.toPath())) {
            walk.filter(p -> p.getFileName().toString().equals("model.properties"))
                .sorted()
                .forEach(p -> {
                    String type = srcRoot.toPath().relativize(p.getParent()).toString();
                    if (new File(p.getParent().toFile(), "model.gltf").isFile()) {
                        types.add(type);
                    } else {
                        missing.add(type);
                    }
                });
        }
        if (!missing.isEmpty()) {
            System.err.println("FAIL: glTF migration incomplete: no model.gltf for " +
                missing.size() + " model(s), starting with: " +
                missing.subList(0, Math.min(5, missing.size())));
            System.exit(2);
        }
        if (types.isEmpty()) {
            System.err.println("FAIL: found no models under " + srcRoot);
            System.exit(2);
        }

        // one AssetManager rooted at the source tree resolves textures for every model
        DesktopAssetManager am = MxmlToJ3o.createAssetManager(srcRoot);
        ModelTextureResolver resolver = new ModelTextureResolver(am);
        BinaryExporter exporter = BinaryExporter.getInstance();

        int ok = 0, fail = 0;
        for (String type : types) {
            File modelDir = new File(srcRoot, type);
            File outDir = new File(stagingRoot, type);
            try {
                GltfToModelDef.Parsed parsed = new GltfToModelDef()
                    .read(new File(modelDir, "model.gltf"));

                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(
                        new File(modelDir, "model.properties"))) {
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
                MxmlToJ3o.Result baked = new MxmlToJ3o(resolver, type)
                    .bake(props, parsed.model, animNames, adefs);
                outDir.mkdirs();
                exporter.save(baked.root, new File(outDir, "model.j3o"));
                ok++;
            } catch (Throwable t) {
                fail++;
                System.err.println("FAILED to bake " + type + ": " + t);
                t.printStackTrace();
            }
        }
        System.out.println("GltfToJ3o: baked " + ok + "/" + types.size() +
            " model.j3o (" + fail + " failed) into " + stagingRoot);
        System.exit(fail == 0 ? 0 : 2);
    }
}
