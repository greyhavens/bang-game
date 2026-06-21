//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;

import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.ModelTextureResolver;

/**
 * Offscreen render-to-PNG harness for a single baked model — the headless visual-inspection tool
 * the cutover plan flags as the highest-value Phase-5 idea (a scriptable, deterministic snapshot of
 * a model that an agent can diff against a baseline, replacing flaky X-display grabs).
 *
 * <p>It loads one baked {@code model.j3o} <em>through the real runtime path</em> — the app-side
 * {@link Model} facade + {@link ModelTextureResolver} re-resolution + a {@link Model#createInstance}
 * clone, exactly what {@code ModelCache.loadPrototype}/{@code createInstance} do in the live client
 * — then (via {@link OffscreenRenderApp}) lights it like {@code BoardView}, frames it in the board's
 * Z-up convention, renders one frame to an offscreen {@link com.jme3.texture.FrameBuffer} with no
 * visible window, and writes the result to a PNG. So a model's textures, materials and lighting can
 * be inspected in isolation, off the live game flow.
 *
 * <p>This is the harness used to confirm the building models bake and resolve to fully-textured
 * geometry (each building sub-mesh carries its own resolved {@code DiffuseMap}); see
 * {@code docs/jme3-cutover-plan.md} Phase&nbsp;4. For a posed/animated character render use
 * {@link RenderModelToPng}; for a multi-model scene use {@link RenderSceneToPng}.
 *
 * <p>Usage: {@code RenderToPng <rsrc root> <model type path> <output png>}, e.g.
 * {@code RenderToPng .../staging/rsrc props/frontier_town/buildings/saloon /tmp/saloon.png}.
 */
public class RenderToPng extends OffscreenRenderApp
{
    public static void main (String[] args)
    {
        if (args.length != 3) {
            System.err.println("Usage: RenderToPng <rsrc root> <model type path> <output png>");
            System.exit(1);
        }
        launch(new RenderToPng(args[0], args[1], args[2]));
    }

    public RenderToPng (String rsrcRoot, String modelType, String outPng)
    {
        super(new File(outPng));
        _rsrcRoot = new File(rsrcRoot).getAbsoluteFile();
        _modelType = modelType;
    }

    @Override protected Node buildScene ()
    {
        assetManager.registerLocator(_rsrcRoot.getPath(), FileLocator.class);

        // load through the real runtime path: facade-wrap the .j3o, re-resolve textures, clone.
        Spatial model = loadInstance();

        Node scene = new Node("scene");
        scene.attachChild(model);
        lightLikeBoard(scene);

        System.out.println("RenderToPng: " + _modelType + " geometries:");
        reportGeoms(model, "  ");
        return scene;
    }

    /**
     * Loads the model through the real app-side {@link Model} facade + {@link ModelTextureResolver},
     * reproducing the runtime {@code ModelCache.loadPrototype}/{@code createInstance} flow exactly,
     * so the harness renders what the live client renders. Returns the cloned instance.
     */
    protected Spatial loadInstance ()
    {
        return Models.loadModel(assetManager, _modelType);
    }

    protected void reportGeoms (Spatial s, String ind)
    {
        if (s instanceof Geometry g) {
            boolean diffuse = g.getMaterial().getTextureParam("DiffuseMap") != null;
            boolean normals = g.getMesh().getBuffer(VertexBuffer.Type.Normal) != null;
            FaceCullMode cull = g.getMaterial().getAdditionalRenderState().getFaceCullMode();
            System.out.println(ind + g.getName() + " diffuse=" + diffuse +
                " normals=" + normals + " cull=" + cull +
                " bucket=" + g.getLocalQueueBucket());
        } else if (s instanceof Node n) {
            for (Spatial c : n.getChildren()) {
                reportGeoms(c, ind);
            }
        }
    }

    protected final File _rsrcRoot;
    protected final String _modelType;
}
