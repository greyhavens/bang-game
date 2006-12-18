//
// $Id$

package com.threerings.bang.bounty.client;

import java.util.Arrays;
import java.util.logging.Level;

import com.jme.renderer.Renderer;
import com.jmex.bui.BComponent;
import com.jmex.bui.BImage;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Handles the loading and display of an Outlaw wanted image. This can be used as a non-component
 * (in which case {@link #reference}, {@link #release} and {@link #render} must be called as
 * apropriate) or it can be used as a component as normal.
 */
public class OutlawView extends BComponent
    implements ResultListener<BImage>
{
    public OutlawView (BangContext ctx, float scale)
    {
        _scale = scale;
        _images = new BImage[] {
            ctx.getImageCache().getBImage("ui/office/background_light.png", scale, false),
            ctx.getImageCache().getBImage("ui/office/background_dark.png", scale, false),
            ctx.getImageCache().getBImage("ui/office/frame.png", scale, false),
            ctx.getImageCache().getBImage("ui/office/sepia.png", scale, false),
            ctx.getImageCache().getBImage("ui/office/check.png", scale, false),
            null };
    }

    public void setOutlaw (BangContext ctx, int[] print, boolean completed)
    {
        _completed = completed;
        if (Arrays.equals(print, _print)) {
            return;
        }
        _images[AVATAR] = null;
        _print = print;
        if (_print != null) {
            AvatarView.getFramableImage(ctx, _print, (int)(4/_scale), this);
        }
    }

    public void reference ()
    {
        _added = true;
        for (BImage image : _images) {
            if (image != null) {
                image.reference();
            }
        }
    }

    public void release ()
    {
        _added = false;
        for (BImage image : _images) {
            if (image != null) {
                image.release();
            }
        }
    }

    public void render (Renderer renderer, int x, int y, float alpha)
    {
        _images[_completed ? DARKBG : LIGHTBG].render(renderer, x, y, alpha);
        if (_images[AVATAR] != null) {
            _images[AVATAR].render(renderer, x, y + (int)(4*_scale), alpha);
        }
        _images[FRAME].render(renderer, x, y, alpha);
        _images[SEPIA].render(renderer, x, y, alpha * (_completed ? 0.6f : 0.3f));
        if (_completed) {
            int ox = (_images[FRAME].getWidth() - _images[CHECK].getWidth())/2;
            int oy = (_images[FRAME].getHeight() - _images[CHECK].getHeight())/2;
            _images[CHECK].render(renderer, x+ox, y+oy, alpha);
        }
    }

    // from interface ResultListener<BImage>
    public void requestCompleted (BImage result)
    {
        if (_added) {
            result.reference();
        }
        _images[AVATAR] = result;
    }

    // from interface ResultListener<BImage>
    public void requestFailed (Exception cause)
    {
        log.log(Level.WARNING, "Failed to load outlaw image " +
                "[print=" + StringUtil.toString(_print) + "].", cause);
    }

    @Override // from BComponent
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(_images[FRAME].getWidth(), _images[FRAME].getHeight());
    }

    @Override // from BComponent
    protected void wasAdded ()
    {
        super.wasAdded();
        reference();
    }

    @Override // from BComponent
    protected void wasRemoved ()
    {
        super.wasRemoved();
        release();
    }

    @Override // from BComponent
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        render(renderer, 0, 0, _alpha);
    }

    protected BImage[] _images;
    protected boolean _added, _completed;
    protected float _scale;
    protected int[] _print;

    protected static final int LIGHTBG = 0;
    protected static final int DARKBG = 1;
    protected static final int FRAME = 2;
    protected static final int SEPIA = 3;
    protected static final int CHECK = 4;
    protected static final int AVATAR = 5;
}
