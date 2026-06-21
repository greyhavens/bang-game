//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;

import com.jme3.asset.plugins.FileLocator;
import com.jme3.scene.Node;

import com.threerings.jme.model.Model;

/**
 * Offscreen render-to-PNG harness for a unit / big-shot character model with its skin and a posed
 * animation frame applied (Phase&nbsp;5, harness&nbsp;#1, deliverable&nbsp;#1).
 *
 * <p>This is the mode that directly unblocks the Phase-6 sprite defects (the "shotgun dude" unit
 * sprite and the mounted/horse big-shot rendering wrong). It loads a character model through the
 * real runtime path (the app-side {@link Model} facade + texture re-resolution +
 * {@link Model#createInstance} clone, carrying the model's {@code SkinningControl}/{@code Armature}/
 * {@link com.jme3.anim.AnimComposer}), selects a named animation and poses it to a fixed time so a
 * single deterministic frame shows the skinned, animated pose — then frames and lights it like the
 * board (Z-up) via {@link OffscreenRenderApp}.
 *
 * <p>Usage: {@code RenderModelToPng <rsrc root> <model type path> <output png> [anim] [time]}, e.g.
 * {@code RenderModelToPng .../staging/rsrc units/frontier_town/shotgunner /tmp/shotgunner.png
 * standing 0}. With no {@code anim} the model is posed in its first available animation; pass
 * {@code list} as the anim name to print the available animations and exit (rendering the bind
 * pose). A negative {@code time} poses the clip midpoint.
 */
public class RenderModelToPng extends OffscreenRenderApp
{
    public static void main (String[] args)
    {
        if (args.length < 3) {
            System.err.println("Usage: RenderModelToPng <rsrc root> <model type path> " +
                "<output png> [anim|list] [time]");
            System.exit(1);
        }
        String anim = args.length > 3 ? args[3] : null;
        float time = args.length > 4 ? Float.parseFloat(args[4]) : 0f;
        launch(new RenderModelToPng(args[0], args[1], args[2], anim, time));
    }

    public RenderModelToPng (String rsrcRoot, String modelType, String outPng, String anim,
                             float time)
    {
        super(new File(outPng));
        _rsrcRoot = new File(rsrcRoot).getAbsoluteFile();
        _modelType = modelType;
        _anim = anim;
        _time = time;
    }

    @Override protected Node buildScene ()
    {
        assetManager.registerLocator(_rsrcRoot.getPath(), FileLocator.class);

        Model model = Models.loadModel(assetManager, _modelType);

        System.out.println("RenderModelToPng: " + _modelType);
        Models.reportAnimations(model);

        // "list" just discovers the animations; render the bind pose so the call still produces a PNG
        if (!"list".equals(_anim)) {
            float len = Models.poseAnimation(model, _anim, _time);
            if (len < 0f) {
                System.out.println("  (no animation posed — rendering bind pose)");
            }
        }

        Node scene = new Node("scene");
        scene.attachChild(model);
        lightLikeBoard(scene);
        return scene;
    }

    protected final File _rsrcRoot;
    protected final String _modelType;
    protected final String _anim;
    protected final float _time;
}
