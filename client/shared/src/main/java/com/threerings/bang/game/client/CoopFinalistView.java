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
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.ResultListener;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;

import com.threerings.media.image.Colorization;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a player's avatar and name at the end of a game.
 */
public class CoopFinalistView extends BContainer
{
    /**
     * Creates a view showing all the players in one frame.
     */
    public CoopFinalistView (
            BasicContext ctx, BangObject bangobj, BangController ctrl)
    {
        super(new AbsoluteLayout());
        setStyleClass("endgame_border");

        int rank = bangobj.perRoundRanks[0][0] - BangObject.COOP_RANK;
        int slot = 4 - (rank - 1)/20;
        String scid = bangobj.scenario.getIdent();

        _players = bangobj.awards.length;
        _ctx = ctx;

        // load up our background
        Colorization[] zations = {
            ctx.getAvatarLogic().getColorPository().getColorization(
                    "metal", slot + 1)
        };
        _background = ctx.getImageCache().createColorizedBImage(
                "ui/postgame/background.png", zations, false);
        _frame = ctx.loadImage("ui/postgame/coop_frame" + _players + ".png");
        _medal = ctx.loadImage("ui/postgame/" + scid + slot + ".png");

        _avatars = new BIcon[_players];
        // start with a blank avatar
        int scale = 4;
        int ax = (710 - _frame.getWidth()) / 2 - 32;
        for (int ii = 0; ii < _players; ii++) {
            setAvatar(new BlankIcon(AvatarLogic.WIDTH/scale,
                                    AvatarLogic.FRAMED_HEIGHT/scale), ii);

            // then if we have a real one, load it up in the background
            if (bangobj.playerInfo[ii].avatar != null) {
                final int index = ii;
                AvatarView.getCoopFramableImage(
                        ctx, bangobj.playerInfo[ii].avatar, scale,
                new ResultListener<BImage>() {
                    public void requestCompleted (BImage avatar) {
                        setAvatar(new ImageIcon(avatar), index);
                    }
                    public void requestFailed (Exception cause) {
                        // never called
                    }
                });
            }
            ImageIcon ribbon = new ImageIcon(ctx.loadImage(
                        "ui/postgame/small_scroll_coop" + colorLookup[ii + 1] + ".png"));
            add(new BLabel(ribbon), new Point(RIBBON_OFFSET[ii] + ax, 20));
            Handle phandle = (bangobj.playerInfo[ii].playerId > 0) ?
                (Handle)bangobj.players[ii] : null;
            BLabel handle = FinalistView.createPopupLabel(
                    _ctx, bangobj.players[ii].toString(), phandle, "endgame_player_small");
            handle.setFit(BLabel.Fit.SCALE);
            add(handle, new Rectangle(NAME_OFFSET[ii] + ax, 28, 136, 17));
            if (ctx instanceof BangContext) {
                BangContext bctx = (BangContext)ctx;
                int myidx = bangobj.getPlayerIndex(
                        bctx.getUserObject().getVisibleName());
                if (myidx != -1 && myidx != ii) {
                    BangConfig config = (BangConfig)ctrl.getPlaceConfig();
                    if (config.ais[ii] == null) {
                        add(new FriendlyFolkButton(bctx, bangobj, ii),
                                new Point(FF_OFFSET[ii] + ax, 38));
                    }
                }
            }
        }

        MessageBundle msgs = ctx.getMessageManager().getBundle(
            GameCodes.GAME_MSGS);

        BContainer titleCont = GroupLayout.makeHBox(GroupLayout.CENTER);
        add(titleCont, new Rectangle(0, 200, 646, 58));
        titleCont.add(_titleLabel = new BLabel(
                    msgs.get("m.endgame_team_title"), "endgame_title"));
        titleCont.add(GameOverView.createCoopIcon(
                    ctx, bangobj.scenario, rank, false));
    }

    @Override // documentation inherited
    public Dimension getPreferredSize (int whint, int hhint)
    {
        return new Dimension(710, 290);
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
        _medal.reference();
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
        _medal.release();
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);
        int ax = (710 - _frame.getWidth()) / 2, ay = 36;
        int bwidth = _frame.getWidth() - 34;
        _background.render(renderer, ax+17, ay+20, bwidth, 154, _alpha);
        int adelta = _avatars[0].getWidth() -
            (_avatars[0].getWidth()*_players - bwidth)/(_players - 1);
        int avx = ax+17;
        for (int ii = 0; ii < _players; ii++) {
            _avatars[ii].render(renderer, avx, ay+17, _alpha);
            avx += adelta;
        }
        _frame.render(renderer, ax, ay, _alpha);
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        int width = _medal.getWidth();
        int offX = Math.min(
                _titleLabel.getAbsoluteX() - getAbsoluteX() - width - 20,
                        (710 - _frame.getWidth() - width) / 2 + 10);
        _medal.render(renderer, offX, 90, _alpha);
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
    protected BImage _background, _frame, _medal;
    protected int _players;
    protected BLabel _titleLabel;

    protected static final int[] NAME_OFFSET = { 7, 155, 303, 451 };
    protected static final int[] FF_OFFSET = { 112, 260, 408, 556 };
    protected static final int[] RIBBON_OFFSET = { 0, 148, 296, 444 };
}
