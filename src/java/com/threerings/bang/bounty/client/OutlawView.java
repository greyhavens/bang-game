//
// $Id$

package com.threerings.bang.bounty.client;

import com.jme.renderer.Renderer;
import com.jmex.bui.BComponent;
import com.jmex.bui.BImage;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ResultListener;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Handles the loading and display of an Outlaw wanted image. This can be used as a non-component
 * (in which case {@link #reference}, {@link #release} and {@link #render} must be called as
 * apropriate) or it can be used as a component as normal.
 */
public class OutlawView extends BComponent
    implements ResultListener<BImage>
{
    public OutlawView (BasicContext ctx, float scale)
    {
        _scale = scale;
        _images = new BImage[] {
            ctx.getImageCache().getBImage("ui/office/outlaw_bg.png", scale, false),
            ctx.getImageCache().getBImage("ui/office/frame.png", scale, false),
            ctx.getImageCache().getBImage("ui/office/sepia.png", scale, false),
            ctx.getImageCache().getBImage("ui/office/dark.png", scale, false),
            ctx.getImageCache().getBImage("ui/office/check.png", 1f, false), // not scaled
            ctx.getImageCache().getBImage("ui/office/frame_bars.png", 1f, false), // not scaled
            null };
    }

    public void setOutlaw (BasicContext ctx, BountyConfig.Opponent outlaw, boolean completed,
                           boolean showBars)
    {
        _completed = completed;
        _showBars = showBars;
        if (_outlaw != null && _outlaw.name.equals(outlaw.name)) {
            return;
        }
        _outlaw = outlaw;

        // clear out any existing outlaw image
        if (_images[AVATAR] != null && _added) {
            _images[AVATAR].release();
        }
        _images[AVATAR] = null;
        AvatarView.getFramableImage(ctx, _outlaw.avatar, (int)(4/_scale), this);
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
        _images[BACKGROUND].render(renderer, x, y, alpha);
        if (_images[AVATAR] != null) {
            int offset = (int)(4*_scale);
            _images[AVATAR].render(renderer, x + offset, y + offset, alpha);
        }
        if (_scale != 1f || !_completed) {
            _images[FRAME].render(renderer, x, y, alpha);
        }
        if (_scale == 1f && _showBars) {
            if (_completed) {
                _images[BARS].render(renderer, 0, 0, alpha);
            } else {
                _images[SEPIA].render(renderer, x, y, alpha * 0.3f);
            }
        } else if (_completed) {
            _images[DARK].render(renderer, x, y, alpha * 0.6f);
            int ox = (_images[FRAME].getWidth() - _images[CHECK].getWidth()) + (int)(10*_scale);
            _images[CHECK].render(renderer, x+ox, y+(int)(8*_scale), alpha);
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
        log.warning("Failed to load outlaw image " + _outlaw + ".", cause);
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
    protected boolean _added, _completed, _showBars;
    protected float _scale;
    protected BountyConfig.Opponent _outlaw;

    protected static final int BACKGROUND = 0;
    protected static final int FRAME = 1;
    protected static final int SEPIA = 2;
    protected static final int DARK = 3;
    protected static final int CHECK = 4;
    protected static final int BARS = 5;
    protected static final int AVATAR = 6;
}
