//
// $Id$

package com.threerings.bang.saloon.client;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.client.PlayerPopupMenu;

import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.MatchObject;

import static com.threerings.bang.Log.log;

/**
 * Displays a player during match-making.
 */
public class PlayerSlot extends AvatarView
{
    public PlayerSlot (BangContext ctx)
    {
        super(ctx, 8, false, true);
        setStyleClass("match_slot");
        _ctx = ctx;

        // we want our icon to overlap our text
        setIconTextGap(-15);

        // load up some images
        _silhouette = ctx.loadImage("ui/saloon/silhouette.png");
        _emptyScroll = ctx.loadImage("ui/frames/tall_tiny_scroll0.png");

        // AvatarView sets a preferred size, but we want to override that
        setPreferredSize(null);
        setText(_ctx.xlate(SaloonCodes.SALOON_MSGS, "m.waiting_for_player"));
    }

    /**
     * Sets the playerOid for this slot.
     */
    public void setPlayerOid (int playerOid)
    {
        if (_playerOid == playerOid) {
            return;
        }
        _playerOid = playerOid;
        if (playerOid <= 0) {
            setPlayerInfo(null);
            return;
        }

        BangOccupantInfo boi = (BangOccupantInfo)
            _ctx.getOccupantDirector().getOccupantInfo(playerOid);
        if (boi == null) {
            setPlayerInfo(null);
        } else {
            setPlayerInfo(new MatchObject.PlayerInfo((Handle)boi.username, boi.avatar));
        }
    }

    /**
     * Sets the playerinfo for this slot.
     */
    public void setPlayerInfo (MatchObject.PlayerInfo pinfo)
    {
        if (pinfo == _pinfo) {
            return;
        }
        _pinfo = pinfo;

        if (pinfo == null) {
            setText(_ctx.xlate(SaloonCodes.SALOON_MSGS, "m.waiting_for_player"));
            return;
        }

        setText(pinfo.handle.toString());
        setAvatar(pinfo.avatar);
    }

    public ColorRGBA getColor ()
    {
        return _pinfo != null ? super.getColor() : GREY_ALPHA;
    }

    @Override // from BComponent
    public boolean dispatchEvent (BEvent event)
    {
        // pop up a player menu if they click the mouse
        return (_pinfo != null ?
            PlayerPopupMenu.checkPopup(_ctx, getWindow(), event, _pinfo.handle, false) : false) ||
            super.dispatchEvent(event);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // reference our images
        _silhouette.reference();
        _emptyScroll.reference();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // release our images
        _silhouette.release();
        _emptyScroll.release();
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(96, 90);
    }

    @Override // documentation inherited
    protected void renderImage (Renderer renderer)
    {
        if (_pinfo != null) {
            super.renderImage(renderer);
        } else {
            int ix = (getWidth() - _silhouette.getWidth())/2;
            _silhouette.render(renderer, ix, _emptyScroll.getHeight()-1, 1f);
        }
    }

    @Override // documentation inherited
    protected void renderScroll (Renderer renderer)
    {
        if (_pinfo != null) {
            super.renderScroll(renderer);
        } else {
            int ix = (getWidth() - _emptyScroll.getWidth())/2;
            _emptyScroll.render(renderer, ix, 0, 1f);
        }
    }

    protected BangContext _ctx;
    protected MatchObject.PlayerInfo _pinfo;
    protected int _playerOid;
    protected BImage _silhouette, _emptyScroll;

    protected static final ColorRGBA GREY_ALPHA = new ColorRGBA(0f, 0f, 0f, 0.25f);
}
