//
// $Id$

package com.threerings.bang.client.bui;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;

import com.jmex.bui.BWindow;

import com.threerings.bang.util.BasicContext;

/**
 * Fades out and removes {@link BWindow}s.
 *
 * <p>jME3 cutover (Phase 2): the fork attached this as a {@code com.jme.scene.Controller} to the
 * BUI root node via {@code addController}. jME3's {@code BRootNode} is no longer a scene-graph
 * node and has no controller hook, so this is rebuilt on the same idiom the migrated
 * {@code WindowSlider} uses: a jME3 {@link Node} attached to {@code ctx.getInterface()} carrying
 * an {@link AbstractControl} that steps the window alpha each frame and self-detaches (and removes
 * the window) when the fade completes.
 */
public class WindowFader extends Node
{
    /**
     * Removes the specified window from the interface, fading it out if the
     * given fade duration is greater than zero.
     */
    public static void remove (BasicContext ctx, BWindow window,
        float duration)
    {
        if (duration <= 0f) {
            if (window.isAdded()) {
                ctx.getRootNode().removeWindow(window);
            }
            return;
        }
        ctx.getInterface().attachChild(new WindowFader(ctx, window, duration));
    }

    protected WindowFader (BasicContext ctx, BWindow window, float duration)
    {
        super("window_fader");
        _ctx = ctx;
        _window = window;
        _duration = duration;
        addControl(new FadeControl());
    }

    /**
     * Steps the fade by the supplied elapsed time. Driven each frame by the attached control.
     */
    protected void updateFade (float time)
    {
        if ((_elapsed += time) >= _duration) {
            _ctx.getRootNode().removeWindow(_window);
            if (getParent() != null) {
                getParent().detachChild(this);
            }
            return;
        }
        _window.setAlpha(1f - _elapsed / _duration);
    }

    /** Drives {@link #updateFade} each frame (replaces the fork Controller hook). */
    protected class FadeControl extends AbstractControl
    {
        @Override protected void controlUpdate (float tpf) {
            updateFade(tpf);
        }
        @Override protected void controlRender (RenderManager rm, ViewPort vp) {
        }
    }

    protected BasicContext _ctx;
    protected BWindow _window;
    protected float _elapsed, _duration;
}
