//
// $Id$

package com.threerings.bang.game.client;

import java.awt.image.BufferedImage;

import com.jme.renderer.Renderer;

import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Rectangle;
import com.jmex.bui.util.Point;

import com.samskivert.util.ResultListener;
import com.threerings.util.Name;
import com.threerings.media.image.Colorization;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.client.PlayerPopupMenu;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a player's avatar and name and a medal indicating their rank at the end of a game.
 */
public class FinalistView extends BContainer
{
    public static BLabel createPopupLabel (
            final BasicContext ctx, String name, final Handle handle, String styleClass)
    {
        return new BLabel(name, styleClass) {
            public boolean dispatchEvent (BEvent event) {
                boolean handled = false;
                if (handle != null && ctx instanceof BangContext) {
                    handled = PlayerPopupMenu.checkPopup(
                            (BangContext)ctx, getWindow(), event, handle, true);
                }
                return handled || super.dispatchEvent(event);
            }
        };
    }

    public static ImageIcon getMedalIcon (BasicContext ctx, int pidx, int rank)
    {
        // load up our medal image and extract the appropriate colored tile
        BufferedImage medal = ctx.getImageCache().getBufferedImage(
            "ui/postgame/medals" + rank + ".png");
        int mwidth = MEDAL_SIZE[rank].width, mheight = MEDAL_SIZE[rank].height;
        return new ImageIcon(new BImage(medal.getSubimage(mwidth * pidx, 0, mwidth, mheight)));
    }
    /**
     * Creates a view for the specified player, rank, etc.
     *
     * @param pidx the player's index in the game (so we can display a medal of the correct player
     * color).
     * @param rank the plaer's rank at the end of the game. If their rank is zero (1st place) the
     * view will be in the large format, otherwise it will be in the small format.
     */
    public FinalistView (BasicContext ctx, BangObject bangobj, BangController ctrl,
                         int pidx, int rank)
    {
        super(new AbsoluteLayout());

        Name name = bangobj.players[pidx];

        // load up our background
        Colorization[] zations = {
            ctx.getAvatarLogic().getColorPository().getColorization("metal", rank + 1)
        };
        _background = ctx.getImageCache().createColorizedBImage(
            "ui/postgame/background.png", zations, false);

        _medal = getMedalIcon(ctx, pidx, rank);

        // create our avatar imagery
        boolean winner = (rank == 0);
        int scale =  winner ? 2 : 4;
        AvatarInfo avatar = (winner && bangobj.playerInfo[pidx].victory != null ?
                             bangobj.playerInfo[pidx].victory : bangobj.playerInfo[pidx].avatar);

        // start with a blank avatar
        setAvatar(new BlankIcon(AvatarLogic.FRAMED_WIDTH/scale, AvatarLogic.FRAMED_HEIGHT/scale));

        // then if we have a real one, load it up in the background
        if (avatar != null) {
            AvatarView.getFramableImage(ctx, avatar, scale, new ResultListener<BImage>() {
                public void requestCompleted (BImage avatar) {
                    setAvatar(new ImageIcon(avatar));
                }
                public void requestFailed (Exception cause) {
                    // never called
                }
            });
        }

        // load up our frame and scroll banner
        String prefix = winner ? "ui/frames/big_" : "ui/frames/small_";
        _frame = new ImageIcon(ctx.loadImage(prefix + "frame.png"));
        _banner = new ImageIcon(ctx.loadImage(prefix + "scroll" + colorLookup[pidx + 1] + ".png"));

        // create a label for their name
        BangContext bctx = (ctx instanceof BangContext) ? (BangContext)ctx : null;
        int myidx = -1;
        if (bctx != null) {
            myidx = bangobj.getPlayerIndex(bctx.getUserObject().getVisibleName());
        }

        Handle phandle = (myidx != -1 && myidx != pidx && bangobj.playerInfo[pidx].playerId > 0) ?
            (Handle)bangobj.players[pidx] : null;
        String sclass = "endgame_player_" + (winner ? "big" : "small") +
            (phandle == null ? "" : "_hand");
        BLabel handle = createPopupLabel(ctx, name.toString(), phandle, sclass);
        handle.setFit(BLabel.Fit.SCALE);
        add(handle, NAME_RECTS[winner ? 0 : 1]);
        if (bctx != null && myidx != -1 && myidx != pidx) {
            BangConfig config = (BangConfig)ctrl.getPlaceConfig();
            if (config.ais[pidx] == null) {
                add(new FriendlyFolkButton(bctx, bangobj, pidx),
                        FF_POS[winner ? 0 : 1]);
            }
        }
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(
            _banner.getWidth(), _avatar.getHeight() + 5*_banner.getHeight()/3);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        _avatar.wasAdded();
        _frame.wasAdded();
        _banner.wasAdded();
        _medal.wasAdded();
        _background.reference();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        _avatar.wasRemoved();
        _frame.wasRemoved();
        _banner.wasRemoved();
        _medal.wasRemoved();
        _background.release();
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);

        int ax = (_width - _avatar.getWidth())/2, ay = _banner.getHeight()/2;
        _background.render(renderer, ax, ay,
                _avatar.getWidth(), _avatar.getHeight(), _alpha);
        _avatar.render(renderer, ax, ay, _alpha);
        _frame.render(renderer, ax-(_frame.getWidth()-_avatar.getWidth())/2,
                      ay-(_frame.getHeight()-_avatar.getHeight())/2, _alpha);
        _banner.render(renderer, 0, 0, _alpha);
        _medal.render(renderer, 0, getHeight()-_medal.getHeight(), _alpha);
    }

    protected void setAvatar (BIcon avatar)
    {
        if (isAdded()) {
            if (_avatar != null) {
                _avatar.wasRemoved();
            }
            avatar.wasAdded();
        }
        _avatar = avatar;
    }

    protected BIcon _avatar, _frame, _banner, _medal;
    protected BImage _background;

    protected static final Dimension[] MEDAL_SIZE = {
        new Dimension(102, 138),
        new Dimension(60, 84),
        new Dimension(58, 76),
        new Dimension(44, 62),
    };

    protected static final Rectangle[] NAME_RECTS = {
        new Rectangle(21, 13, 251, 25),
        new Rectangle(7, 7, 136, 17),
    };

    protected static final Point[] FF_POS = {
        new Point(244, 33),
        new Point(113, 17),
    };
}
