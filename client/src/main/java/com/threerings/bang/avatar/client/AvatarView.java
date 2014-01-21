//
// $Id$

package com.threerings.bang.avatar.client;

import java.awt.image.BufferedImage;

import com.jmex.bui.BImage;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ResultListener;

import com.threerings.bang.client.PlayerPopupMenu;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.avatar.util.AvatarLogic.*;

/**
 * Displays an avatar.
 */
public class AvatarView extends BaseAvatarView
{
    /**
     * Obtains a coop framable image for the specified avatar, scaled by one over the specified
     * factor.
     */
    public static void getCoopFramableImage (
        BasicContext ctx, AvatarInfo avatar, final int reduction,
        final ResultListener<BImage> receiver)
    {
        getImage(ctx, avatar, new ResultListener<BufferedImage>() {
            public void requestCompleted (BufferedImage base) {
                int sw = WIDTH/reduction, sh = FRAMED_HEIGHT/reduction;
                receiver.requestCompleted(
                    new BImage(base.getScaledInstance(sw, sh, BufferedImage.SCALE_SMOOTH)));
            }
            public void requestFailed (Exception cause) {
                receiver.requestFailed(cause);
            }
        });
    }

    /**
     * Creates a view that can be used to display avatar images.
     *
     * @param scale the image will be one over this value times the "natural" size of the avatar
     * imagery. This should be at least 2.
     * @param framed whether to render a frame around the avatar image.
     * @param named whether to display a banner containing the name of the avatar (which is set
     * with {@link #setHandle}).
     */
    public AvatarView (BasicContext ctx, int scale, boolean framed, boolean named)
    {
        this(ctx, scale, framed, named, 0);
    }

    /**
     * Creates a view that can be used to display avatar images.
     *
     * @param scale the image will be one over this value times the "natural" size of the avatar
     * imagery. This should be at least 2.
     * @param framed whether to render a frame around the avatar image.
     * @param named whether to display a banner containing the name of the avatar (which is set
     * with {@link #setHandle}).
     * @param color the color index of the banner
     */
    public AvatarView (BasicContext ctx, int scale, boolean framed, boolean named, int color)
    {
        super(ctx, scale);
        if (framed) {
            setStyleClass("avatar_view_framed_" + scale);
        } else {
            setStyleClass("avatar_view_unframed_" + scale);
        }

        // set up our dimensions and frame
        int pwid, phei;
        if (framed) {
            switch (scale) {
            case 2: _frame = ctx.loadImage("ui/frames/big_frame.png"); break;
            case 3: _frame = ctx.loadImage("ui/frames/medium_frame.png"); break;
            case 4: _frame = ctx.loadImage("ui/frames/small_frame.png"); break;
            }
            pwid = _frame.getWidth();
            phei = _frame.getHeight();
        } else {
            pwid = WIDTH/scale;
            phei = HEIGHT/scale;
        }

        // if we're going to display a name, load up the appropriate name banner image
        if (named) {
            String type = null;
            switch (scale) {
            case 2: type = "big"; break;
            case 3: type = "medium"; break;
            case 4: type = "small"; break;
            case 8: type = "tiny"; break;
            }
            if (type != null) {
                _scroll = ctx.loadImage("ui/frames/" + type + "_scroll" + color + ".png");
                phei += _scroll.getHeight()/2;
                pwid = Math.max(pwid, _scroll.getWidth());
            }
        }

        setPreferredSize(new Dimension(pwid, phei));
    }

    public AvatarView (BasicContext ctx, float scale)
    {
        super(ctx, scale);
        setStyleClass("avatar_view");

        setPreferredSize(new Dimension((int)(WIDTH*scale), (int)(HEIGHT*scale)));
    }

    /**
     * Sets the avatar to display.
     */
    public void setAvatar (AvatarInfo avatar)
    {
        super.setAvatar(avatar);
    }

    /**
     * Configures the handle of the avatar we're viewing. This will also activate the player popup
     * menu.
     */
    public void setHandle (Handle handle)
    {
        setHandle(handle, handle.toString());
    }

    /**
     * Configures the handle of the avatar we're viewing and potentially modified version of that
     * handle for display.
     */
    public void setHandle (Handle handle, String displayHandle)
    {
        _handle = handle;
        setText(displayHandle);
    }

    @Override // from BComponent
    public boolean dispatchEvent (BEvent event)
    {
        // pop up a player menu if they click the mouse and we know who we're looking at
        boolean handled = false;
        if (_handle != null && _ctx instanceof BangContext) {
            handled = PlayerPopupMenu.checkPopup(
                (BangContext)_ctx, getWindow(), event, _handle, false);
        }
        return handled || super.dispatchEvent(event);
    }

    @Override // from BComponent
    public boolean changeCursor ()
    {
        return super.changeCursor() && _handle != null;
    }

    protected Handle _handle;
}
