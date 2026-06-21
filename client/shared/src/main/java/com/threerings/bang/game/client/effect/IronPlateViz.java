//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.AbstractControl;

import com.threerings.jme.util.SpatialVisitor;

import com.threerings.bang.game.client.sprite.ActiveSprite;
import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.BasicContext;

/**
 * A visualization for the iron plate.  When activated and when the unit is
 * shot, temporarily gives the unit a metallic appearance.
 *
 * <h3>jME3 cutover (Phase 2, cluster 2)</h3>
 *
 * The fork pushed a multi-pass <em>render-state overlay</em> ({@code mesh.addOverlay(RenderState[])}
 * with a sphere-map {@code TextureState} + a {@code MaterialState} + additive blend) onto every
 * {@code ModelMesh} of the unit and ramped the overlay material's alpha via a
 * {@code com.jme.scene.Controller}. jME3 has no fixed-function render-state overlay stack: a
 * sphere-mapped metallic pass is a re-authored {@code Material}/{@code MatDef} effect.
 *
 * <p>The metallic sheen is now reproduced two ways, ramped together by the reacting-action flash:
 * <ul>
 *   <li><b>Sphere-map additive overlay</b> ({@link SphereMapOverlay}) — the re-authored
 *   {@code shaders/SphereMapOverlay.j3md} additive pass over the unit's non-skinned geometries,
 *   sampling {@code textures/environ/spheremap.png} with GL_SPHERE_MAP texgen (the fork's
 *   {@code EM_SPHERE} look); its {@code Intensity} is ramped by this viz's controller.</li>
 *   <li><b>In-place white brighten</b> — the unit's own {@code Lighting.j3md} materials are pushed
 *   above white via {@code UseMaterialColors} + {@code Ambient}/{@code Diffuse} (the same lever the
 *   {@code TownView} hover-highlight uses). This rides the GPU skinning correctly and, crucially, is
 *   <em>visible on the baked, {@code DiffuseMap}-textured unit materials that carry no {@code Color}
 *   param</em> — removing the previous total no-op. It is also the standalone metallic stand-in for
 *   skinned meshes, which the overlay pass does not yet cover (see {@link SphereMapOverlay}).</li>
 * </ul>
 */
public class IronPlateViz extends InfluenceViz
{
    @Override // documentation inherited
    public void init (BasicContext ctx, PieceSprite target)
    {
        super.init(ctx, target);
        collectMaterials();
        // attach the additive sphere-map metallic overlay (non-skinned geometries); skinned meshes
        // rely on the in-place brighten below.
        _overlay = new SphereMapOverlay(ctx, target, new ColorRGBA(ColorRGBA.White));
        _overlay.attach(target);
        ((MobileSprite)_target).addActionHandler(_handler);
        flashOverlay(0.5f);
    }

    @Override // documentation inherited
    public void destroy ()
    {
        ((MobileSprite)_target).removeActionHandler(_handler);
        setBrighten(0f);
        if (_overlay != null) {
            _overlay.detach();
            _overlay = null;
        }
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
     * Applies a metallic-brighten amount to the gathered materials and ramps the sphere-map overlay.
     *
     * <p>Brighten pushes each material above white via {@code UseMaterialColors} +
     * {@code Ambient}/{@code Diffuse} (the {@code TownView} hover-highlight lever) so the lit,
     * baked {@code DiffuseMap} units actually brighten — the old {@code Color}-param multiply was a
     * no-op on those. {@code Color}-param materials (unlit) are still handled for completeness.
     */
    protected void setBrighten (float amount)
    {
        float v = 1f + amount; // > 1 brightens toward an over-white metallic sheen
        ColorRGBA col = new ColorRGBA(v, v, v, 1f);
        for (Material mat : _mats) {
            if (mat.getMaterialDef().getMaterialParam("Color") != null) {
                mat.setColor("Color", col.clone());
            } else if (mat.getMaterialDef().getMaterialParam("UseMaterialColors") != null) {
                if (amount <= 0f) {
                    restoreLit(mat);
                } else {
                    saveLit(mat);
                    mat.setBoolean("UseMaterialColors", true);
                    mat.setColor("Ambient", col.clone());
                    mat.setColor("Diffuse", col.clone());
                }
            }
        }
        if (_overlay != null) {
            _overlay.setIntensity(amount);
        }
    }

    /** Saves a lit material's original colour state once, so brighten can restore it. */
    protected void saveLit (Material mat)
    {
        if (_saved.containsKey(mat)) {
            return;
        }
        MatParam umc = mat.getParam("UseMaterialColors");
        MatParam amb = mat.getParam("Ambient");
        MatParam dif = mat.getParam("Diffuse");
        _saved.put(mat, new Object[] {
            Boolean.valueOf(umc != null && Boolean.TRUE.equals(umc.getValue())),
            amb == null ? null : ((ColorRGBA)amb.getValue()).clone(),
            dif == null ? null : ((ColorRGBA)dif.getValue()).clone() });
    }

    /** Restores a lit material's saved colour state. */
    protected void restoreLit (Material mat)
    {
        Object[] s = _saved.get(mat);
        if (s == null) {
            return;
        }
        mat.setBoolean("UseMaterialColors", ((Boolean)s[0]).booleanValue());
        if (s[1] != null) {
            mat.setColor("Ambient", (ColorRGBA)s[1]);
        }
        if (s[2] != null) {
            mat.setColor("Diffuse", (ColorRGBA)s[2]);
        }
    }

    /**
     * Flashes the overlay state over the specified duration (which includes
     * the time it takes to ramp in and out).
     */
    protected void flashOverlay (final float duration)
    {
        _target.addControl(new AbstractControl() {
            @Override protected void controlUpdate (float time) {
                _elapsed = Math.min(_elapsed + time, duration);
                float amount;
                if (_elapsed < FADE_DURATION) {
                    amount = _elapsed / FADE_DURATION;
                } else if (_elapsed <= duration - FADE_DURATION) {
                    amount = 1f;
                } else {
                    amount = (duration - _elapsed) / FADE_DURATION;
                }
                setBrighten(amount);
                if (_elapsed >= duration) {
                    _target.removeControl(this);
                    setBrighten(0f);
                }
            }
            @Override protected void controlRender (RenderManager rm, ViewPort vp) {
            }
            protected float _elapsed;
        });
    }

    /** Flashes the overlay when the reacting animation plays. */
    protected ActiveSprite.ActionHandler _handler =
        new ActiveSprite.ActionHandler() {
        public String handleAction (ActiveSprite sprite, String action) {
            if ("reacting".equals(action)) {
                flashOverlay(sprite.getAction(action).getDuration());
                return action;
            } else {
                return null;
            }
        }
    };

    /** The materials of the target's model geometries (brightened by this effect). */
    protected java.util.ArrayList<Material> _mats = new java.util.ArrayList<Material>();

    /** The additive sphere-map metallic overlay (non-skinned geometries). */
    protected SphereMapOverlay _overlay;

    /** Per-material saved lit colour state: {UseMaterialColors, Ambient, Diffuse}. */
    protected java.util.IdentityHashMap<Material, Object[]> _saved =
        new java.util.IdentityHashMap<Material, Object[]>();

    /** The time to take fading in and out of the overlay. */
    protected static final float FADE_DURATION = 0.1f;
}
