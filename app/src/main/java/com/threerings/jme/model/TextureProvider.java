//
// $Id$

package com.threerings.jme.model;

import com.jme3.scene.Geometry;
import com.jme3.texture.Texture;

/**
 * Provides a means for a {@link Model} instance to (re-)resolve its texture references at
 * instance time — the jME3-side analogue of the fork's {@code TextureProvider}.
 *
 * <p>jME3 cutover (Phase 2): the fork interface returned a fork {@code TextureState} keyed by a
 * texture <em>name</em>. The fork-free facade instead drives the
 * {@link com.threerings.bang.jme3.model.ModelTextureResolver} (promoted from
 * {@code tools/j3o-converter}): the converter preserves the candidate texture list as the
 * {@code bang.textures} user data on each {@link Geometry}, and a re-resolve walks those
 * geometries and rebinds their material's {@code DiffuseMap} for the requested
 * variant/colorization. Implementers are supplied a {@link Geometry} and the stored texture
 * name, and return the {@link Texture} to bind (or {@code null} to leave it unchanged).
 *
 * <p>The effect/sprite framework's own {@code TextureProvider} consumers (Gunshot/SmokePlume/…
 * emissions) are part of the effects REBUILD (Phase 4) and recode their texture binding against
 * this interface there; the facade only needs the type to exist so the model API compiles.
 */
public interface TextureProvider
{
    /**
     * Resolves the texture for the named reference on the given geometry.
     *
     * @param geom the geometry whose material is being (re-)resolved.
     * @param name the stored texture name (absolute resource path if it starts with {@code /},
     * otherwise relative to the model directory).
     * @return the texture to bind, or {@code null} to leave the geometry's texture unchanged.
     */
    public Texture getTexture (Geometry geom, String name);
}
