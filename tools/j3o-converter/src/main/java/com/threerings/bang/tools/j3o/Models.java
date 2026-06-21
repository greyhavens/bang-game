//
// $Id$

package com.threerings.bang.tools.j3o;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.ModelTextureResolver;
import com.threerings.jme.model.TextureProvider;

/**
 * Shared model-loading helpers for the render harnesses. Loads a baked {@code model.j3o}
 * <em>through the real runtime path</em> — the app-side {@link Model} facade +
 * {@link ModelTextureResolver} re-resolution + a {@link Model#createInstance} clone — exactly what
 * {@code ModelCache.loadPrototype}/{@code createInstance} do in the live client, so the harness
 * renders what the live client renders. Stays on the isolated jME3 + LWJGL3 + {@code project(":app")}
 * classpath (no fork, no LWJGL2).
 */
public final class Models
{
    /**
     * Loads, texture-resolves and clones the model at the given type path (e.g.
     * {@code units/frontier_town/shotgunner}), returning the cloned {@link Model} facade. The facade
     * carries the model's {@link AnimComposer}/{@code SkinningControl} so the caller can pose an
     * animation (see {@link #poseAnimation}).
     */
    public static Model loadModel (AssetManager assetManager, String typePath)
    {
        Spatial content = assetManager.loadModel(typePath + "/model.j3o");
        Model proto = new Model(content);
        final String type = typePath;
        proto.resolveTextures(new TextureProvider() {
            private final ModelTextureResolver _r = new ModelTextureResolver(assetManager);
            public Texture getTexture (Geometry geom, String name) {
                String path = _r.textureAssetPath(type, name);
                try {
                    return assetManager.loadTexture(path);
                } catch (RuntimeException e) {
                    System.out.println("  getTexture FAILED " + path + " : " +
                        e.getClass().getSimpleName());
                    return null;
                }
            }
        });
        return proto.createInstance();
    }

    /**
     * Poses the given model's animation at a fixed time so a single rendered frame shows that pose.
     * Selects the named clip on the {@link AnimComposer} and advances it to {@code time} seconds
     * (clamped to the clip length), then updates the controls so the skinned geometry deforms before
     * the frame is captured. Returns the clip length in seconds, or {@code -1} if the model has no
     * such animation.
     *
     * @param name the animation name, or {@code null}/empty to pose the first available animation.
     * @param time the time within the clip to pose; negative means the clip midpoint.
     */
    public static float poseAnimation (Model model, String name, float time)
    {
        AnimComposer composer = model.getControl(AnimComposer.class);
        if (composer == null) {
            // skinning may live on the content child rather than the facade root
            for (Spatial child : model.getChildren()) {
                composer = child.getControl(AnimComposer.class);
                if (composer != null) {
                    break;
                }
            }
        }
        if (composer == null) {
            return -1f;
        }
        String clipName = (name == null || name.isEmpty()) ? firstClip(composer) : name;
        if (clipName == null || composer.getAnimClip(clipName) == null) {
            return -1f;
        }
        AnimClip clip = composer.getAnimClip(clipName);
        float length = (float)clip.getLength();
        float t = (time < 0f) ? length * 0.5f : Math.min(time, length);
        composer.setCurrentAction(clipName);
        // Tick twice (tpf=0). The bake attaches the SkinningControl before the AnimComposer, so
        // controls run skin-then-pose within a tick: the first tick deforms the skin from the
        // previous (reset) armature pose, then the composer writes the time-t joint transforms; the
        // second tick (after re-pinning the time) lets the SkinningControl consume the posed
        // armature. With a single tick the rendered skin stays frozen at the bind pose for clips
        // whose tracks are joint-only (no rigid spatial tracks) — e.g. the umbrella tactician —
        // masking real posed-skin behaviour. (At t=0 both ticks reproduce the bind pose, so the
        // standing@0 goldens are unaffected.)
        composer.setTime(t);
        model.updateLogicalState(0f);
        composer.setTime(t);
        model.updateLogicalState(0f);
        model.updateGeometricState();
        System.out.println("  posed animation '" + clipName + "' @ " + t + "s of " + length + "s");
        return length;
    }

    /** Returns the name of the first animation clip on the composer, or {@code null}. */
    public static String firstClip (AnimComposer composer)
    {
        for (String n : composer.getAnimClipsNames()) {
            return n;
        }
        return null;
    }

    /** Lists the model's animation names to stdout (for discovery). */
    public static void reportAnimations (Model model)
    {
        String[] names = model.getAnimationNames();
        System.out.println("  animations (" + names.length + "): " + String.join(", ", names));
    }

    private Models () {}
}
