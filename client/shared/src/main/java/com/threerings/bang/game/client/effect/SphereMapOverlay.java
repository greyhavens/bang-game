//
// $Id$

package com.threerings.bang.game.client.effect;

import java.util.ArrayList;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import com.threerings.jme.util.SpatialVisitor;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

/**
 * The jME3 re-authoring of the fork's sphere-map (environment-map) additive overlay pass used by
 * {@link IronPlateViz} (metallic sheen) and {@link HeroInfluenceViz} (player-colour sheen).
 *
 * <h3>What the fork did</h3>
 *
 * The fork built a shared sphere-map {@code TextureState} from
 * {@code textures/environ/spheremap.png} with {@code Texture.EM_SPHERE} environmental-map mode,
 * paired it with a {@code MaterialState} (its emissive/diffuse carried the tint and the
 * ramped alpha), wrapped them with {@code RenderUtil.addAlpha} (additive) + an overlay z-buffer,
 * and pushed the resulting {@code RenderState[]} onto <em>every</em> model {@code ModelMesh} as a
 * second render pass via {@code mesh.addOverlay(...)}. A {@code Controller} ramped the material
 * alpha in/out to fade the sheen on and off.
 *
 * <h3>What this does on jME3</h3>
 *
 * jME3 has no fixed-function render-state overlay stack and no {@code EM_SPHERE} texgen, so the
 * sphere map is re-authored as {@code shaders/SphereMapOverlay.j3md} (a 2D sphere map sampled with
 * the classic GL_SPHERE_MAP eye-space texgen, the same math as {@code shaders/BangWater.vert},
 * additively blended). For each <em>non-skinned</em> model {@link Geometry} we attach a sibling
 * overlay {@link Geometry} that shares the unit mesh and carries this additive material; the
 * shared {@code Intensity} uniform is ramped by the owning viz (the fork's alpha ramp).
 *
 * <p><b>Skinned meshes (the remaining Phase-4 item).</b> Bang units are GPU-skinned (a
 * {@code SkinningControl} deforms specific registered geometries). A static overlay clone of a
 * skinned mesh would render in bind pose and detach from the animating unit, so this helper does
 * <em>not</em> attach overlay geometry for skinned meshes; the owning viz instead drives the
 * in-place {@code UseMaterialColors} brighten/tint on the unit's own {@code Lighting.j3md}
 * material for those (which rides the skinning correctly). Giving the overlay pass true,
 * skinning-aware geometry needs the model framework to register an extra pass with the
 * {@code SkinningControl}, which is deferred.
 */
public class SphereMapOverlay
{
    /** The sphere-map texture asset (present under {@code assets/rsrc/}). */
    public static final String SPHERE_MAP = "textures/environ/spheremap.png";

    /** The re-authored additive sphere-map overlay MatDef. */
    public static final String MATDEF = "shaders/SphereMapOverlay.j3md";

    public SphereMapOverlay (BasicContext ctx, PieceSprite target, ColorRGBA tint)
    {
        _ctx = ctx;
        _tint = tint;
    }

    /**
     * Attaches an additive sphere-map overlay geometry to every non-skinned model geometry under
     * the target's model node. Returns the number of overlay geometries attached (0 means every
     * mesh was skinned, so the caller should rely on the in-place brighten/tint fallback alone).
     */
    public int attach (PieceSprite target)
    {
        Spatial modelNode = target.getModelNode();
        if (modelNode == null) {
            return 0;
        }
        new SpatialVisitor<Geometry>(Geometry.class) {
            protected void visit (Geometry geom) {
                if (isSkinned(geom) || !(geom.getParent() instanceof Node)) {
                    return; // skinned meshes ride the in-place tint; see class doc
                }
                Material mat = createMaterial();
                Geometry overlay = new Geometry(geom.getName() + "_spheremap", geom.getMesh());
                overlay.setMaterial(mat);
                overlay.setLocalTransform(geom.getLocalTransform());
                overlay.setQueueBucket(Bucket.Transparent);
                overlay.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Off);
                geom.getParent().attachChild(overlay);
                _overlays.add(overlay);
                _mats.add(mat);
            }
        }.traverse(modelNode);
        return _overlays.size();
    }

    /** Ramps the overlay strength (the fork's overlay-material alpha ramp), 0 = invisible. */
    public void setIntensity (float intensity)
    {
        for (Material mat : _mats) {
            mat.setFloat("Intensity", Math.max(0f, intensity));
        }
    }

    /** Detaches all overlay geometries. */
    public void detach ()
    {
        for (Geometry overlay : _overlays) {
            if (overlay.getParent() != null) {
                overlay.removeFromParent();
            }
        }
        _overlays.clear();
        _mats.clear();
    }

    /** Whether any overlay geometry was attached (i.e. the model had non-skinned meshes). */
    public boolean isActive ()
    {
        return !_overlays.isEmpty();
    }

    protected Material createMaterial ()
    {
        Material mat = new Material(_ctx.getAssetManager(), MATDEF);
        mat.setTexture("SphereMap", _ctx.getTextureCache().getTexture(SPHERE_MAP));
        mat.setColor("TintColor", _tint.clone());
        mat.setFloat("Intensity", 0f);
        RenderUtil.applyAddAlpha(mat);
        RenderUtil.applyOverlayZBuf(mat);
        return mat;
    }

    /** A geometry is skinned if it (or an ancestor) carries a {@code SkinningControl}. */
    protected static boolean isSkinned (Spatial spatial)
    {
        for (Spatial s = spatial; s != null; s = s.getParent()) {
            if (s.getControl(com.jme3.anim.SkinningControl.class) != null) {
                return true;
            }
        }
        return false;
    }

    protected BasicContext _ctx;
    protected ColorRGBA _tint;
    protected ArrayList<Geometry> _overlays = new ArrayList<Geometry>();
    protected ArrayList<Material> _mats = new ArrayList<Material>();
}
