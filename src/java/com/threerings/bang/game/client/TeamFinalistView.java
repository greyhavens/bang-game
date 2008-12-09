//
// $Id$

package com.threerings.bang.game.client;

import com.jme.renderer.Renderer;

import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Rectangle;
import com.jmex.bui.util.Point;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;

import com.samskivert.util.ResultListener;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.BangConfig;

import com.threerings.media.image.Colorization;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a team's avatars and names at the end of a game.
 */
public class TeamFinalistView extends BContainer
{
    /**
     * Creates a view showing all the players in one frame.
     */
    public TeamFinalistView (
            BasicContext ctx, BangObject bangobj, BangController ctrl, int tidx, int rank)
    {
        super(new AbsoluteLayout());

        _ctx = ctx;
        _winner = rank == 0;

        // load up our background
        Colorization[] zations = {
            ctx.getAvatarLogic().getColorPository().getColorization("metal", rank + 1)
        };
        _background = ctx.getImageCache().createColorizedBImage(
                "ui/postgame/background.png", zations, false);
        _frame = ctx.loadImage("ui/postgame/coop_frame2" + (_winner ? "_big.png" : ".png"));
        _medal = FinalistView.getMedalIcon(ctx, tidx + 4, rank);
        _avatars = new BIcon[2];

        // start with a blank avatar
        int scale = _winner ? 2 : 4;
        int idx = 0;
        _offx = _medal.getWidth() / 3;
        String ribimg = "ui/postgame/" + (_winner ? "big" : "small") + "_scroll_coop";
        String hclass = "endgame_player_" + (_winner ? "big" : "small");

        for (int ii = 0; ii < bangobj.playerInfo.length; ii++) {
            if (bangobj.teams[ii] != tidx) {
                continue;
            }
            AvatarInfo avatar = (rank == 0 && bangobj.playerInfo[ii].victory != null ?
                    bangobj.playerInfo[ii].victory : bangobj.playerInfo[ii].avatar);
            setAvatar(new BlankIcon(AvatarLogic.WIDTH/scale,
                                    AvatarLogic.FRAMED_HEIGHT/scale), idx);

            // then if we have a real one, load it up in the background
            if (avatar != null) {
                final int index = idx;
                AvatarView.getCoopFramableImage(ctx, avatar, scale, new ResultListener<BImage>() {
                    public void requestCompleted (BImage avatar) {
                        setAvatar(new ImageIcon(avatar), index);
                    }
                    public void requestFailed (Exception cause) {
                        // never called
                    }
                });
            }
            ImageIcon ribbon = new ImageIcon(ctx.loadImage(ribimg + colorLookup[ii + 1] + ".png"));
            add(new BLabel(ribbon), new Point(
                        RIBBON_OFFSET[_winner ? 0 : 1][idx] + _offx, _winner ? 9 : 0));
            Handle phandle = (bangobj.playerInfo[ii].playerId > 0) ?
                (Handle)bangobj.players[ii] : null;
            String sclass = hclass + (phandle == null ? "" : "_hand");
            BLabel handle = FinalistView.createPopupLabel(
                    _ctx, bangobj.players[ii].toString(), phandle, sclass);
            handle.setFit(BLabel.Fit.SCALE);
            Rectangle rect = NAME_RECTS[_winner ? 0 : 1];
            add(handle, new Rectangle(_offx + NAME_OFFSET[_winner ? 0 : 1][idx],
                        rect.y, rect.width, rect.height));
            if (ctx instanceof BangContext) {
                BangContext bctx = (BangContext)ctx;
                int myidx = bangobj.getPlayerIndex(
                        bctx.getUserObject().getVisibleName());
                if (myidx != -1 && myidx != ii) {
                    BangConfig config = (BangConfig)ctrl.getPlaceConfig();
                    if (config.ais[ii] == null) {
                        add(new FriendlyFolkButton(bctx, bangobj, ii),
                                new Point(FF_OFFSET[_winner ? 0 : 1][idx] + _offx, 22));
                    }
                }
            }
            idx++;
        }
    }

    @Override // documentation inherited
    public Dimension getPreferredSize (int whint, int hhint)
    {
        return new Dimension(_frame.getWidth() + _offx, _frame.getHeight() + _medal.getHeight()/3);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        for (int ii = 0; ii < _avatars.length; ii++) {
            _avatars[ii].wasAdded();
        }
        _frame.reference();
        _background.reference();
        _medal.wasAdded();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        for (int ii = 0; ii < _avatars.length; ii++) {
            _avatars[ii].wasRemoved();
        }
        _frame.release();
        _background.release();
        _medal.wasRemoved();
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);
        int edge = _winner ? 20 : 17;
        int bwidth = _frame.getWidth() - edge * 2;
        int bheight = _avatars[0].getHeight();
        _background.render(renderer, _offx+edge, _winner ? 25 : 23, bwidth, bheight, _alpha);
        int adelta = bwidth - _avatars[0].getWidth();
        int avx = _offx + edge;
        for (int ii = 0; ii < 2; ii++) {
            _avatars[ii].render(renderer, avx, _winner ? 25 : 20, _alpha);
            avx += adelta;
        }
        _frame.render(renderer, _offx, 0, _alpha);
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        _medal.render(renderer, 0, getHeight()-_medal.getHeight(), _alpha);
    }

    protected void setAvatar (BIcon avatar, int idx)
    {
        if (isAdded()) {
            if (_avatars[idx] != null) {
                _avatars[idx].wasRemoved();
            }
            avatar.wasAdded();
        }
        _avatars[idx] = avatar;
    }

    protected BasicContext _ctx;
    protected BIcon[] _avatars;
    protected ImageIcon _medal;
    protected BImage _background, _frame;
    protected int _offx;
    protected boolean _winner;

    protected static final int[][] NAME_OFFSET = { { 14, 256 }, { 7, 155 } };
    protected static final Rectangle[] NAME_RECTS = {
        new Rectangle(0, 13, 236, 25), new Rectangle(0, 8, 136, 17) };
    protected static final int[][] FF_OFFSET = { { 215, 468}, { 112, 260 } };
    protected static final int[][] RIBBON_OFFSET = { { 11, 256 }, { 0, 148 } };
}
