//
// $Id$

package com.threerings.jme.post;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

/**
 * A jME3 post-process {@link Filter} that tints the rendered scene toward a warm sepia (the old
 * fork's aged-photo look). Installed on the main 3D viewport via a {@code FilterPostProcessor}, so
 * it covers the ground, sky dome, buildings and town backdrop -- everything in the world viewport
 * -- but not the separate BUI overlay (which renders on the GUI viewport).
 *
 * <p>The strength is driven live from the Esc-options "Sepia tone" slider (see
 * {@code BangPrefs.getSepia} and {@code BoardView.applySepiaPref}); {@code intensity == 0} is a pure
 * pass-through (the shader still runs but emits the original color).
 */
public class SepiaFilter extends Filter
{
    public SepiaFilter ()
    {
        super("SepiaFilter");
    }

    /**
     * Sets the sepia strength, 0 (no tint) .. 1 (full sepia). Values are clamped.
     */
    public void setIntensity (float intensity)
    {
        _intensity = Math.max(0f, Math.min(1f, intensity));
        if (_material != null) {
            _material.setFloat("Intensity", _intensity);
        }
    }

    public float getIntensity ()
    {
        return _intensity;
    }

    @Override // from Filter
    protected void initFilter (
        AssetManager manager, RenderManager rm, ViewPort vp, int w, int h)
    {
        _material = new Material(manager, "shaders/Sepia.j3md");
        _material.setFloat("Intensity", _intensity);
    }

    @Override // from Filter
    protected Material getMaterial ()
    {
        return _material;
    }

    protected Material _material;
    protected float _intensity;
}
