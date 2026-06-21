//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.jme.effect;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Quad;

import com.threerings.jme.util.LinearTimeFunction;
import com.threerings.jme.util.TimeFunction;

/**
 * Fades a supplied quad (or one that covers the screen) in from a solid color or out to a solid
 * color.
 *
 * <p>jME3 cutover (Phase 1): rebuilt off the fixed-function fork (fork {@code Quad} node +
 * {@code AlphaState} + {@code QUEUE_ORTHO} + {@code updateGeometricState} hook) onto jME3 idioms:
 * a {@link Geometry} over a jME3 {@link Quad} mesh with an {@code Unshaded.j3md} material in
 * {@code BlendMode.Alpha}, drawn in the {@link Bucket#Gui} ortho bucket; the per-frame alpha
 * stepping moved from the old {@code updateGeometricState} override into an attached
 * {@link AbstractControl}. Because jME3 has no global {@code DisplaySystem}, the material is built
 * through a caller-supplied {@link AssetManager} and the full-screen "curtain" size is passed in
 * (the fork read it from {@code DisplaySystem.getWidth/Height}).
 */
public class FadeInOutEffect extends Node
{
    public FadeInOutEffect (AssetManager assetManager, float screenWidth, float screenHeight,
                            ColorRGBA color, float startAlpha, float endAlpha, float duration,
                            boolean overUI)
    {
        this(assetManager, color, new LinearTimeFunction(startAlpha, endAlpha, duration), overUI);
        setQuad(createCurtain(screenWidth, screenHeight));
    }

    public FadeInOutEffect (AssetManager assetManager, ColorRGBA color, TimeFunction alphaFunc,
                            boolean overUI)
    {
        this(assetManager, (Geometry)null, color, alphaFunc, overUI);
    }

    public FadeInOutEffect (AssetManager assetManager, Geometry quad, ColorRGBA color,
                            TimeFunction alphaFunc, boolean overUI)
    {
        super("FadeInOut");

        _color = new ColorRGBA(color.r, color.g, color.b, alphaFunc.getValue(0));
        _alphaFunc = alphaFunc;

        _material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        _material.setColor("Color", _color);
        _material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

        setQueueBucket(Bucket.Gui);
        // overUI: behind the UI (z below) vs in front (z above); the Gui bucket sorts by z.
        setLocalTranslation(0, 0, overUI ? -1 : 1);

        if (quad != null) {
            setQuad(quad);
        }

        addControl(new FadeControl());
    }

    /**
     * Configures the quad that will be faded in or out.
     */
    public void setQuad (Geometry quad)
    {
        attachChild(quad);
        quad.setMaterial(_material);
    }

    /**
     * Allows the fade to be paused.
     */
    public void setPaused (boolean paused)
    {
        _paused = paused;
    }

    /**
     * Indicates whether or not the fade is paused.
     */
    public boolean isPaused ()
    {
        return _paused;
    }

    /**
     * Steps the fade by the supplied elapsed time, updating the curtain alpha and detaching the
     * effect when complete. Driven each frame by the attached control.
     */
    protected void updateFade (float time)
    {
        if (_paused) {
            return;
        }
        float alpha = _alphaFunc.getValue(time);
        _color.a = Math.min(1f, Math.max(0f, alpha));
        _material.setColor("Color", _color);
        if (_alphaFunc.isComplete()) {
            fadeComplete();
        }
    }

    /**
     * Called (only once) when we have reached the end of our fade.  Automatically detaches this
     * effect from the hierarchy.
     */
    protected void fadeComplete ()
    {
        if (getParent() != null) {
            getParent().detachChild(this);
        }
    }

    /**
     * Creates a quad geometry that covers the entire screen for full-screen fades.
     */
    protected Geometry createCurtain (float width, float height)
    {
        Geometry curtain = new Geometry("curtain" + hashCode(), new Quad(width, height));
        // jME3 Quad's origin is its lower-left corner; the fork centred the node, so offset to
        // keep the same screen coverage (origin at lower-left fills the screen directly).
        curtain.setLocalTranslation(new Vector3f(0f, 0f, 0f));
        return curtain;
    }

    /** Drives {@link #updateFade} each frame (replaces the fork updateGeometricState hook). */
    protected class FadeControl extends AbstractControl
    {
        @Override protected void controlUpdate (float tpf) {
            updateFade(tpf);
        }
        @Override protected void controlRender (RenderManager rm, ViewPort vp) {
        }
    }

    protected ColorRGBA _color;
    protected Material _material;
    protected TimeFunction _alphaFunc;
    protected boolean _paused;
}
