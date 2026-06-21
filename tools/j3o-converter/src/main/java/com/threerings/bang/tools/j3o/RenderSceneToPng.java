//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;

import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

import com.threerings.jme.model.Model;

/**
 * Offscreen render-to-PNG harness for a multi-model "scene" (Phase&nbsp;5, harness&nbsp;#1,
 * deliverable&nbsp;#2): poses N models on a ground grid and renders them together — e.g. a couple of
 * units plus a building — so a small composed scene can be snapshotted, not just one isolated model.
 *
 * <p>Each model is loaded through the real runtime path ({@link Models#loadModel}) and laid out on a
 * square grid on a neutral ground plane, lit and framed like the board (Z-up) via
 * {@link OffscreenRenderApp}. A per-model {@code @anim} suffix poses that model's animation frame.
 *
 * <p>Usage: {@code RenderSceneToPng <rsrc root> <output png> <spec> [spec ...]} where each
 * {@code spec} is {@code <model type path>[@<anim>[:<time>]]}, e.g.
 * {@code RenderSceneToPng .../staging/rsrc /tmp/scene.png units/frontier_town/shotgunner@standing
 * units/frontier_town/cavalry@standing props/frontier_town/buildings/saloon}. Specs may also be
 * passed as one comma-separated argument.
 */
public class RenderSceneToPng extends OffscreenRenderApp
{
    public static void main (String[] args)
    {
        if (args.length < 3) {
            System.err.println("Usage: RenderSceneToPng <rsrc root> <output png> " +
                "<modelType[@anim[:time]]> [more specs ...]");
            System.exit(1);
        }
        String rsrc = args[0], out = args[1];
        // remaining args are specs; allow a single comma-separated arg too
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (sb.length() > 0) sb.append(',');
            sb.append(args[i]);
        }
        String[] specs = sb.toString().split(",");
        launch(new RenderSceneToPng(rsrc, out, specs));
    }

    public RenderSceneToPng (String rsrcRoot, String outPng, String[] specs)
    {
        super(new File(outPng));
        _rsrcRoot = new File(rsrcRoot).getAbsoluteFile();
        _specs = specs;
    }

    @Override protected Node buildScene ()
    {
        assetManager.registerLocator(_rsrcRoot.getPath(), FileLocator.class);

        Node scene = new Node("scene");

        // lay the models out on a square grid; size the cell to the largest model so they don't
        // overlap. First load+pose them all, measuring footprint, then place on the grid.
        int n = _specs.length;
        int cols = (int)Math.ceil(Math.sqrt(n));
        Model[] models = new Model[n];
        float cell = 1f;
        for (int i = 0; i < n; i++) {
            models[i] = loadSpec(_specs[i]);
            // a freshly loaded/cloned model has a null world bound until its geometric state is
            // computed; an un-posed spec never gets that update from poseAnimation, so measure it
            // here or its footprint reads null and it would not grow the grid cell (-> overlap).
            models[i].updateGeometricState();
            BoundingBox bb = (BoundingBox)models[i].getWorldBound();
            if (bb != null) {
                cell = Math.max(cell, Math.max(bb.getXExtent(), bb.getYExtent()) * 2.6f);
            }
        }

        for (int i = 0; i < n; i++) {
            int col = i % cols, row = i / cols;
            // grid laid out in the board's XY plane (Z up); model footprint dropped onto the ground
            models[i].setLocalTranslation(col * cell, row * cell, 0f);
            scene.attachChild(models[i]);
        }

        scene.attachChild(buildGround((cols) * cell));
        lightLikeBoard(scene);

        System.out.println("RenderSceneToPng: " + n + " models on a " + cols + "-wide grid");
        return scene;
    }

    /** Parses one {@code modelType[@anim[:time]]} spec, loads and poses it. */
    protected Model loadSpec (String spec)
    {
        String type = spec, anim = null;
        float time = 0f;
        int at = spec.indexOf('@');
        if (at >= 0) {
            type = spec.substring(0, at);
            String rest = spec.substring(at + 1);
            int colon = rest.indexOf(':');
            if (colon >= 0) {
                anim = rest.substring(0, colon);
                time = Float.parseFloat(rest.substring(colon + 1));
            } else {
                anim = rest;
            }
        }
        Model model = Models.loadModel(assetManager, type);
        if (anim != null) {
            Models.poseAnimation(model, anim, time);
        }
        return model;
    }

    /** Builds a neutral ground plane (a dirt-coloured quad) in the board's XY plane. */
    protected Geometry buildGround (float size)
    {
        Geometry ground = new Geometry("ground", new Quad(size, size));
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", new ColorRGBA(0.42f, 0.36f, 0.28f, 1f));
        mat.setColor("Ambient", new ColorRGBA(0.42f, 0.36f, 0.28f, 1f));
        mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        ground.setMaterial(mat);
        // a Quad lies in the XY plane facing +Z — exactly the board ground convention; drop it
        // slightly below z=0 so models sitting at z=0 rest on it.
        ground.setLocalTranslation(-size * 0.25f, -size * 0.25f, -0.05f);
        return ground;
    }

    protected final File _rsrcRoot;
    protected final String[] _specs;
}
