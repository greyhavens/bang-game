//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

import com.threerings.jme.util.SpatialVisitor;

/**
 * A small, fork-free stand-in for the fork {@code com.jme.scene.state.MaterialState} as the
 * sprite framework used it.
 *
 * <h3>jME3 cutover (Phase 2, cluster 1)</h3>
 *
 * The fork {@code PieceSprite} created a shared {@code MaterialState}, attached it to the sprite
 * node with {@code setRenderState(mstate)}, and then mutated {@code mstate.getDiffuse().a} (and the
 * RGB) to darken a unit by the static-shadow amount or to fade it in/out on spawn/death. jME3 has
 * no fixed-function {@code MaterialState} (migration map §2.4): colour/alpha is a {@code Material}
 * param on each {@link Geometry}. This helper keeps the tiny mutable surface the sprite code
 * relies on ({@link #getDiffuse}/{@link #getAmbient}/{@link #setDiffuse}/{@link #setAmbient}) and,
 * when {@link #apply} is called, pushes the diffuse colour onto every geometry material under the
 * sprite via the {@code Unshaded}/{@code Lighting} {@code Color}/{@code Diffuse} param.
 *
 * <p>The fork rendered units through {@code Lighting} with both an ambient and diffuse term; here
 * we only push the diffuse term (the visually dominant one — shadow darkening and fade alpha both
 * ride on it), which is faithful enough for the Phase-2 compile pass. Full lit-material fidelity
 * (separate ambient, the model's own {@code Lighting.j3md} params) is part of the Phase-4 material
 * upgrade.
 */
public class SpriteMaterialState
{
    /** Returns the (mutable) diffuse colour. Callers mutate {@code .a}/{@code .r}/... directly. */
    public ColorRGBA getDiffuse ()
    {
        return _diffuse;
    }

    /** Returns the (mutable) ambient colour. */
    public ColorRGBA getAmbient ()
    {
        return _ambient;
    }

    /** Sets the diffuse colour (copies the supplied value). */
    public void setDiffuse (ColorRGBA color)
    {
        _diffuse.set(color);
    }

    /** Sets the ambient colour (copies the supplied value). */
    public void setAmbient (ColorRGBA color)
    {
        _ambient.set(color);
    }

    /**
     * Pushes the current diffuse colour onto every geometry material beneath the supplied spatial.
     * Materials that expose a {@code Color} param (the {@code Unshaded} the sprite models default to
     * during the Phase-2 port) get it; {@code Lighting}-based materials get {@code Diffuse}.
     */
    public void apply (Spatial target)
    {
        final ColorRGBA color = _diffuse;
        new SpatialVisitor<Geometry>(Geometry.class) {
            protected void visit (Geometry geom) {
                Material mat = geom.getMaterial();
                if (mat == null) {
                    return;
                }
                if (mat.getMaterialDef().getMaterialParam("Color") != null) {
                    mat.setColor("Color", color.clone());
                } else if (mat.getMaterialDef().getMaterialParam("Diffuse") != null) {
                    mat.setColor("Diffuse", color.clone());
                }
            }
        }.traverse(target);
    }

    /** The diffuse colour (shadow darkening + fade alpha ride here). */
    protected ColorRGBA _diffuse = new ColorRGBA(ColorRGBA.White);

    /** The ambient colour (tracked for API compatibility). */
    protected ColorRGBA _ambient = new ColorRGBA(ColorRGBA.White);
}
