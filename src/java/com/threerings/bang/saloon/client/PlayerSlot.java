//
// $Id$

package com.threerings.bang.saloon.client;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonCodes;

import static com.threerings.bang.Log.log;

/**
 * Displays a player during match-making.
 */
public class PlayerSlot extends BLabel
{
    public PlayerSlot (BangContext ctx)
    {
        super("");
        setStyleClass("match_slot");
        _ctx = ctx;

        // load up some images
        _silhouette = ctx.loadImage("ui/saloon/silhouette.png");
        _playerScroll = ctx.loadImage("ui/frames/tiny_scroll.png");
        _emptyScroll = ctx.loadImage("ui/frames/tall_tiny_scroll.png");
    }

    public void setPlayerOid (int playerOid)
    {
        if (playerOid == _playerOid) {
            return;
        }
        _playerOid = playerOid;

        if (playerOid <= 0) {
            setText(_ctx.xlate(SaloonCodes.SALOON_MSGS,
                               "m.waiting_for_player"));
            return;
        }

        BangOccupantInfo boi = (BangOccupantInfo)
            _ctx.getOccupantDirector().getOccupantInfo(playerOid);
        if (boi == null) {
            log.warning("Missing occupant info for player " +
                        "[oid=" + playerOid + "].");
            setText("???");
        } else {
            setText(boi.username.toString());
            if (_avatar != null) {
                _avatar.release();
            }
            _avatar = AvatarView.getImage(
                _ctx, boi.avatar,
                AvatarLogic.WIDTH/8, AvatarLogic.HEIGHT/8);
            _avatar.reference();
        }
    }

    public ColorRGBA getColor ()
    {
        return _playerOid > 0 ? super.getColor() : GREY_ALPHA;
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // reference our images
        _silhouette.reference();
        _playerScroll.reference();
        _emptyScroll.reference();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // release our images
        _silhouette.release();
        _playerScroll.release();
        _emptyScroll.release();

        if (_avatar != null) {
            _avatar.release();
        }
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(120, 75);
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);

        BImage icon, scroll;
        int offy = 0;
        if (_playerOid > 0) {
            icon = (_avatar == null) ? _silhouette : _avatar;
            scroll = _playerScroll;
        } else {
            icon = _silhouette;
            scroll = _emptyScroll;
            offy = 5;
        }
        int ix = (getWidth() - icon.getWidth())/2;
        int iy = getHeight() - icon.getHeight() - offy;
        icon.render(renderer, ix, iy, 1f);
        scroll.render(renderer, 0, 0, 1f);
    }

    protected BangContext _ctx;
    protected int _playerOid = -1;
    protected BImage _silhouette, _playerScroll, _emptyScroll, _avatar;

    protected static final ColorRGBA GREY_ALPHA =
        new ColorRGBA(0f, 0f, 0f, 0.25f);
}
