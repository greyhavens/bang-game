//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.BillboardControl;

import com.threerings.jme.util.SpatialVisitor;

import com.threerings.bang.client.BangUI;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.sprite.PieceSprite;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * An influence visualization that shows a floating number.
 *
 * <h3>jME3 cutover (Phase 2, cluster 2)</h3>
 *
 * Two fork mechanisms are ported here:
 * <ul>
 *   <li><b>The floating level count</b> was a fork {@code Quad} + {@code TextureState} (rendered
 *   text) hung on a {@code BillboardNode}. jME3: a text {@link Geometry} from
 *   {@link RenderUtil#createTextQuad} attached to a {@link Node} carrying a {@link BillboardControl}.
 *   </li>
 *   <li><b>The level-up colour overlay</b> was a sphere-map {@code TextureState} + {@code
 *   MaterialState} pushed onto every model {@code ModelMesh} via {@code addOverlay(RenderState[])}
 *   and ramped each frame. jME3 has no render-state overlay stack.</li>
 * </ul>
 *
 * <p>The level-up player-colour sheen is now reproduced two ways, ramped together by the level alpha:
 * <ul>
 *   <li><b>Sphere-map additive overlay</b> ({@link SphereMapOverlay}) — the re-authored
 *   {@code shaders/SphereMapOverlay.j3md} additive pass over the unit's non-skinned geometries,
 *   tinted to the player colour, sampling {@code textures/environ/spheremap.png} with GL_SPHERE_MAP
 *   texgen (the fork's {@code EM_SPHERE} look); its {@code Intensity} is ramped by this viz.</li>
 *   <li><b>In-place player-colour tint</b> — the unit's own {@code Lighting.j3md} materials are
 *   tinted toward the player colour via {@code UseMaterialColors} + {@code Ambient}/{@code Diffuse}
 *   (the {@code TownView} hover-highlight lever). This rides the GPU skinning correctly and is
 *   <em>visible on the baked, {@code DiffuseMap}-textured unit materials that carry no {@code Color}
 *   param</em> — removing the previous no-op, and standing in for skinned meshes the overlay pass
 *   does not yet cover.</li>
 * </ul>
 */
public class HeroInfluenceViz extends InfluenceViz
{
    public HeroInfluenceViz (int level)
    {
        _level = level;
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, PieceSprite target)
    {
        super.init(ctx, target);
        _owner = target.getPiece().owner;

        collectMaterials();

        // attach the additive sphere-map player-colour overlay (non-skinned geometries); skinned
        // meshes rely on the in-place tint below.
        _overlay = new SphereMapOverlay(ctx, target, getJPieceColor(_owner));
        _overlay.attach(target);

        // attach the floating level count on a screen-facing billboard
        _billboard = new Node("hero_influence");
        _billboard.addControl(new BillboardControl());
        _billboard.setLocalTranslation(new Vector3f(0, 0, target.getHeight() + 0.5f * TILE_SIZE));
        target.attachChild(_billboard);

        setLevel(_level);
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
     * Update the level number.
     */
    public void setLevel (int level)
    {
        _level = level;

        // (re)build the floating count text geometry
        if (_count != null) {
            _billboard.detachChild(_count);
        }
        _count = RenderUtil.createTextQuad(
                _ctx, BangUI.COUNTER_FONT, getJPieceColor(_owner), getDarkerPieceColor(_owner),
                String.valueOf(_level));
        // jME3 createTextQuad returns a corner-anchored Quad sized in texture PIXELS (~150 world
        // units), where the fork built a CENTER-anchored Quad resized to a fixed world height
        // (qrat = TILE_SIZE * 0.5 / glyph height). Normalize the pixel quad to the same world
        // height (preserving aspect) and recenter it on its pivot so the number sits centered on
        // the billboard, matching the fork (see the DamageIconViz sibling idiom).
        com.jme3.scene.Mesh cmesh = _count.getMesh();
        cmesh.updateBound();
        com.jme3.bounding.BoundingBox cb = (com.jme3.bounding.BoundingBox)cmesh.getBound();
        float cqh = cb.getYExtent() * 2f, cqw = cb.getXExtent() * 2f;
        float cscale = (cqh > 0f) ? (TILE_SIZE * 0.5f) / cqh : 1f;
        _count.setLocalScale(cscale);
        _count.setLocalTranslation(-cqw * cscale / 2f, -cqh * cscale / 2f, 0f);
        _billboard.attachChild(_count);

        float scale = 1f + _level * 0.05f;
        _target.setLocalScale(new Vector3f(scale, scale, scale));

        // apply the level-up tint toward the player colour
        applyTint(getAlpha());
    }

    /**
     * Tints the gathered model materials toward the player colour by the supplied amount and ramps
     * the sphere-map overlay.
     *
     * <p>The tint pushes the unit's own {@code Lighting.j3md} material toward the player colour via
     * {@code UseMaterialColors} + {@code Ambient}/{@code Diffuse} (the {@code TownView}
     * hover-highlight lever) so the lit, baked {@code DiffuseMap} unit actually tints — the old
     * {@code Diffuse}-without-{@code UseMaterialColors} write was a no-op on those.
     * {@code Color}-param materials (unlit) are still handled for completeness.
     */
    protected void applyTint (float amount)
    {
        ColorRGBA pc = getJPieceColor(_owner);
        ColorRGBA c = new ColorRGBA();
        c.interpolateLocal(new ColorRGBA(ColorRGBA.White), pc, Math.min(1f, amount));
        for (Material mat : _mats) {
            if (mat.getMaterialDef().getMaterialParam("Color") != null) {
                mat.setColor("Color", c.clone());
            } else if (mat.getMaterialDef().getMaterialParam("UseMaterialColors") != null) {
                if (amount <= 0f) {
                    restoreLit(mat);
                } else {
                    saveLit(mat);
                    mat.setBoolean("UseMaterialColors", true);
                    mat.setColor("Ambient", c.clone());
                    mat.setColor("Diffuse", c.clone());
                }
            }
        }
        if (_overlay != null) {
            _overlay.setIntensity(amount);
        }
    }

    /** Saves a lit material's original colour state once, so the tint can restore it. */
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

    @Override // documentation inherited
    public void destroy ()
    {
        if (_target != null) {
            _target.detachChild(_billboard);
        }

        // fade out the colour change before removing
        final float duration = 1f;
        _target.addControl(new AbstractControl() {
            @Override protected void controlUpdate (float time) {
                _elapsed = Math.min(_elapsed + time, duration);
                float amount = getAlpha() * (duration - _elapsed) / duration;
                applyTint(amount);
                if (_elapsed >= duration) {
                    _target.removeControl(this);
                    applyTint(0f);
                    if (_overlay != null) {
                        _overlay.detach();
                        _overlay = null;
                    }
                }
            }
            @Override protected void controlRender (RenderManager rm, ViewPort vp) {
            }
            protected float _elapsed;
        });
    }

    /**
     * Calculates the alpha value for this hero level.
     */
    protected float getAlpha ()
    {
        if (_level < 5) {
            return _level / 5f * .3f;
        }
        return .3f + (_level - 5) / 10f;
    }

    protected int _level, _owner;

    // our floating level indicator
    protected Node _billboard;
    protected Geometry _count;

    /** The materials of the target's model geometries (tinted by this effect). */
    protected java.util.ArrayList<Material> _mats = new java.util.ArrayList<Material>();

    /** The additive sphere-map player-colour overlay (non-skinned geometries). */
    protected SphereMapOverlay _overlay;

    /** Per-material saved lit colour state: {UseMaterialColors, Ambient, Diffuse}. */
    protected java.util.IdentityHashMap<Material, Object[]> _saved =
        new java.util.IdentityHashMap<Material, Object[]>();
}
