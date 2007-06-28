//
// $Id$

package com.threerings.bang.client.bui;

import com.jme.scene.Controller;

import com.jmex.bui.BWindow;

import com.threerings.bang.util.BasicContext;

/**
 * Fades out and removes {@link BWindow}s.
 */
public class WindowFader extends Controller
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
        ctx.getRootNode().addController(new WindowFader(ctx, window,
            duration));
    }

    protected WindowFader (BasicContext ctx, BWindow window, float duration)
    {
        _ctx = ctx;
        _window = window;
        _duration = duration;
    }

    // documentation inherited
    public void update (float time)
    {
        if ((_elapsed += time) >= _duration) {
            _ctx.getRootNode().removeWindow(_window);
            _ctx.getRootNode().removeController(this);
            return;
        }
        _window.setAlpha(1f - _elapsed / _duration);
    }

    protected BasicContext _ctx;
    protected BWindow _window;
    protected float _elapsed, _duration;
}
