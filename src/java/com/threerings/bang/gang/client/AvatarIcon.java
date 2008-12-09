//
// $Id$

package com.threerings.bang.gang.client;

import com.jme.renderer.Renderer;

import com.jmex.bui.BImage;
import com.jmex.bui.icon.ImageIcon;

import com.samskivert.util.ResultListener;

import com.threerings.bang.avatar.client.AvatarView;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.*;

/**
 * An icon that displays an avatar in a small frame.
 */
public class AvatarIcon extends ImageIcon
    implements ResultListener<BImage>
{
    public AvatarIcon (BangContext ctx)
    {
        super(ctx.loadImage("ui/hideout/leader_frame.png"));
        _ctx = ctx;
    }

    /**
     * Sets the avatar image to use.
     */
    public void setAvatar (AvatarInfo avatar)
    {
        if (avatar.equals(_avatar)) {
            return;
        }
        AvatarView.getImage(_ctx, _avatar = avatar, 65, 82, false, this);
    }

    // documentation inherited from interface ResultListener
    public void requestCompleted (BImage result)
    {
        if (_aimg != null && _added) {
            _aimg.release();
        }
        _aimg = result;
        if (_added) {
            _aimg.reference();
        }
    }

    // documentation inherited from interface ResultListener
    public void requestFailed (Exception cause)
    {
        log.warning("Failed to retrieve avatar image for leader", "cause", cause);
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();
        if (_aimg != null) {
            _aimg.reference();
        }
        _added = true;
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();
        if (_aimg != null) {
            _aimg.release();
        }
        _added = false;
    }

    @Override // documentation inherited
    public void render (Renderer r, int x, int y, float alpha)
    {
        super.render(r, x, y, alpha);
        if (_aimg != null) {
            _aimg.render(r, x+3, y+3, alpha);
        }
    }

    protected BangContext _ctx;
    protected AvatarInfo _avatar;
    protected BImage _aimg;
    protected boolean _added;
}
