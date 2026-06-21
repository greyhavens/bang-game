//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.AbstractControl;

import com.threerings.jme.util.SpatialVisitor;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

/**
 * A visualization for the Spirit Walk card.  Makes the unit semi-transparent
 * and tints it blue.
 *
 * <h3>jME3 cutover (Phase 2, cluster 2)</h3>
 *
 * The fork attached a shared {@code MaterialState} + {@code blendAlpha} render-state to the sprite
 * and interpolated its diffuse colour each frame via a {@code com.jme.scene.Controller}. jME3 has
 * no fixed-function material state: tint/alpha live on each {@link Geometry}'s {@link Material}
 * {@code Color}/{@code Diffuse} param, and the per-frame step is an {@link AbstractControl}. This
 * port walks the sprite's model geometries, moves them to the transparent bucket with an
 * alpha-blend preset (via {@link RenderUtil}), and fades a blue tint in/out on their materials.
 *
 * <p><b>Phase-4 fidelity deferral:</b> the precise spirit-walk look (the exact translucent-blue
 * shading over the lit unit material) depends on the re-authored unit MatDefs and is a Phase-4
 * visual-review item; here we apply the tint to whatever {@code Color}/{@code Diffuse} param each
 * geometry's material exposes and restore it on destroy.
 */
public class NoncorporealViz extends InfluenceViz
{
    @Override // documentation inherited
    public void init (BasicContext ctx, PieceSprite target)
    {
        super.init(ctx, target);
        collectMaterials();
        for (Material mat : _mats) {
            RenderUtil.applyBlendAlpha(mat);
        }
        if (_target.getModelNode() != null) {
            _target.getModelNode().setQueueBucket(Bucket.Transparent);
        }
        fadeMaterial(true);
    }

    @Override // documentation inherited
    public void destroy ()
    {
        fadeMaterial(false);
    }

    /** Gathers the materials of the target's model geometries. */
    protected void collectMaterials ()
    {
        if (_target.getModelNode() == null) {
            return;
        }
        new SpatialVisitor<Geometry>(Geometry.class) {
            protected void visit (Geometry geom) {
                if (geom.getMaterial() != null) {
                    _mats.add(geom.getMaterial());
                }
            }
        }.traverse(_target.getModelNode());
    }

    /**
     * Sets the noncorporeal tint on the gathered materials at the supplied blend amount.
     */
    protected void applyTint (float amount)
    {
        ColorRGBA c = new ColorRGBA();
        c.interpolateLocal(new ColorRGBA(ColorRGBA.White), NONCORPOREAL_COLOR, amount);
        for (Material mat : _mats) {
            if (mat.getParam("Color") != null) {
                mat.setColor("Color", c.clone());
            } else if (mat.getParam("Diffuse") != null) {
                mat.setColor("Diffuse", c.clone());
            }
        }
    }

    /**
     * Fades into or out of the noncorporeal color.
     */
    protected void fadeMaterial (final boolean in)
    {
        _target.addControl(new AbstractControl() {
            @Override protected void controlUpdate (float time) {
                _elapsed = Math.min(_elapsed + time, FADE_DURATION);
                float alpha = _elapsed / FADE_DURATION;
                applyTint(in ? alpha : (1f - alpha));
                if (_elapsed >= FADE_DURATION) {
                    _target.removeControl(this);
                    if (!in) {
                        // restore opaque white tint + opaque bucket
                        applyTint(0f);
                        if (_target.getModelNode() != null) {
                            _target.getModelNode().setQueueBucket(Bucket.Inherit);
                        }
                    }
                }
            }
            @Override protected void controlRender (RenderManager rm, ViewPort vp) {
            }
            protected float _elapsed;
        });
    }

    /** The materials of the target's model geometries (tinted by this effect). */
    protected java.util.ArrayList<Material> _mats = new java.util.ArrayList<Material>();

    /** The color of noncorporeality. */
    protected static final ColorRGBA NONCORPOREAL_COLOR =
        new ColorRGBA(0f, 1f, 1f, 0.625f);

    /** The time it takes to fade into or out of the noncorporeal color. */
    protected static final float FADE_DURATION = 0.25f;
}
