//
// $Id$

package com.threerings.bang.jme3.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import com.threerings.jme.tools.model.Model;
import com.threerings.jme.model.ModelTextureResolver;

/**
 * A jMonkeyEngine&nbsp;3 {@link AssetLoader} that reads a Bang {@code model.dat} (the vendored
 * jME-fork binary format) directly and returns a jME3 {@link Spatial}, with materials,
 * rigid keyframe animation, and skinning converted in place by {@link ModelConverter}.
 *
 * <p>This is the runtime path recommended in {@code docs/jme3-model-pipeline.md} §6.3: rather
 * than baking a build-time {@code .j3o}, the live client loads each {@code model.dat} through
 * this loader, keeping the existing (untouched) {@code compileModels} build step and deferring
 * colorization / variant / detail re-resolution to {@link ModelTextureResolver} (the
 * ModelCache-equivalent) exactly as the fork does today.
 *
 * <p>Registration:
 * <pre>
 *   assetManager.registerLoader(BangModelLoader.class, "dat");
 *   Spatial model = assetManager.loadModel("units/frontier_town/gunslinger/model.dat");
 * </pre>
 *
 * <p>Headless / tool use: {@link #loadModel(AssetManager, File, String)} skips the
 * {@link AssetInfo} plumbing and loads straight from a {@link File}, which is what the
 * verification harness and the {@code ModelToJ3o} exporter use.
 *
 * <p><b>Isolation:</b> this class (and the whole module) is the only place jME3 deps meet the
 * fork; nothing in the shipped game depends on it. The fork's {@code com.jme.*} reader classes
 * and jME3's {@code com.jme3.*} classes coexist in different packages.
 */
public class BangModelLoader
    implements AssetLoader
{
    @Override
    public Object load (AssetInfo assetInfo)
        throws IOException
    {
        ensureDisplaySystem();
        String name = assetInfo.getKey().getName(); // e.g. props/frontier_town/saloon/model.dat
        String typePath = typePathFromAssetName(name);
        Model forkModel;
        try (InputStream in = assetInfo.openStream()) {
            forkModel = readForkModel(in);
        }
        ModelTextureResolver resolver = new ModelTextureResolver(assetInfo.getManager());
        return new ModelConverter(resolver, typePath).convert(forkModel).root;
    }

    /**
     * Headless loader entry point: reads {@code datFile} directly and converts it, using the
     * supplied {@code assetManager} for texture/material resolution. The conversion {@link
     * ModelConverter.Result} is available via {@link #lastResult()} for coverage reporting.
     *
     * @param typePath the model dir relative to the rsrc root (texture-resolution base);
     * if null, it is derived from the {@code .dat} path's parent chain up to {@code rsrc}.
     */
    public Spatial loadModel (AssetManager assetManager, File datFile, String typePath)
        throws IOException
    {
        ensureDisplaySystem();
        if (typePath == null) {
            typePath = typePathFromFile(datFile);
        }
        Model forkModel;
        try (InputStream in = new FileInputStream(datFile)) {
            forkModel = readForkModel(in);
        }
        ModelTextureResolver resolver = new ModelTextureResolver(assetManager);
        _lastResult = new ModelConverter(resolver, typePath).convert(forkModel);
        return _lastResult.root;
    }

    /** The conversion result of the most recent {@link #loadModel} call (coverage details). */
    public ModelConverter.Result lastResult ()
    {
        return _lastResult;
    }

    /** Reads + initialises a fork Model from a stream (mirrors {@code Model.readFromFile}). */
    protected static Model readForkModel (InputStream in)
        throws IOException
    {
        Model model = (Model)com.jme.util.export.binary.BinaryImporter.getInstance().load(in);
        model.initPrototype();
        return model;
    }

    /**
     * The fork model reader touches render-state setup; ensure a headless display exists.
     *
     * <p>Constructs a {@link com.jme.util.DummyDisplaySystem} exactly once, which swaps the
     * fork's static {@code SystemProvider} for a headless one. This MUST happen before any
     * call to {@code DisplaySystem.getDisplaySystem()}, because the default provider is the
     * gdx one, whose {@code GDXDisplaySystem} constructor dereferences {@code Gdx.input} (null
     * outside a real gdx app) and NPEs. So we never call {@code getDisplaySystem()} here.
     */
    protected static synchronized void ensureDisplaySystem ()
    {
        if (!_displayReady) {
            new com.jme.util.DummyDisplaySystem();
            com.jme.util.LoggingSystem.getLogger().setLevel(Level.WARNING);
            _displayReady = true;
        }
    }

    /** Strips the trailing {@code /model.dat} from an asset name to get the model dir. */
    protected static String typePathFromAssetName (String name)
    {
        int slash = name.lastIndexOf('/');
        return slash < 0 ? "" : name.substring(0, slash);
    }

    /** Extracts the model dir relative to the rsrc root from a {@code .dat} file path. */
    protected static String typePathFromFile (File datFile)
    {
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (File dir = datFile.getAbsoluteFile().getParentFile();
                dir != null && !dir.getName().equals("rsrc"); dir = dir.getParentFile()) {
            parts.add(0, dir.getName());
        }
        return String.join("/", parts);
    }

    protected ModelConverter.Result _lastResult;

    /** Whether the headless fork display system has been installed (process-wide). */
    protected static boolean _displayReady;
}
