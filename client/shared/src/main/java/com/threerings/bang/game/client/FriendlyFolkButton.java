//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;

import com.threerings.util.MessageBundle;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import static com.threerings.bang.Log.log;

/**
 * Does something extraordinary.
 */
public class FriendlyFolkButton extends BButton
    implements ActionListener, AttributeChangeListener
{
    public static BIcon getFFIcon (
            BangContext ctx, BangObject bangobj, int pidx)
    {
        int playerId = bangobj.playerInfo[pidx].playerId;
        PlayerObject pobj = ctx.getUserObject();
        if (pobj.isFriend(playerId)) {
            return getFFIcon(ctx, PlayerService.FOLK_IS_FRIEND);
        } else if (pobj.isFoe(playerId)) {
            return getFFIcon(ctx, PlayerService.FOLK_IS_FOE);
        }
        return getFFIcon(ctx, PlayerService.FOLK_NEUTRAL);
    }

    public static BIcon getFFIcon (BangContext ctx, int opinion)
    {
        String iconpath = "ui/pstatus/folks/";
        if (opinion == PlayerService.FOLK_IS_FRIEND) {
            iconpath += "thumbs_up.png";
        } else if (opinion == PlayerService.FOLK_IS_FOE) {
            iconpath += "thumbs_down.png";
        } else {
            iconpath += "ff.png";
        }
        return new ImageIcon(ctx.loadImage(iconpath));
    }

    public FriendlyFolkButton (BangContext ctx, BangObject bangobj, int pidx)
    {
        super(getFFIcon(ctx, bangobj, pidx), "ff");
        _bangobj = bangobj;
        _pidx = pidx;
        _ctx = ctx;
        _ctx.getUserObject().addListener(this);
        addListener(this);
        setStyleClass("player_status_ff");
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(PlayerObject.FRIENDS) ||
                event.getName().equals(PlayerObject.FOES)) {
            setIcon(getFFIcon(_ctx, _bangobj, _pidx));
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        new PlayerPopup(_bangobj.playerInfo[_pidx].playerId).popup(
                getAbsoluteX() + 10, getAbsoluteY() + 30, true);
    }

    /**
     * A popup menu to set a player's friendly folks status.
     */
    protected class PlayerPopup extends BPopupMenu
        implements ActionListener
    {
        public PlayerPopup (int playerId)
        {
            super(FriendlyFolkButton.this.getWindow());
            addListener(this);
            setLayer(BangUI.POPUP_MENU_LAYER);

            PlayerObject player = _ctx.getUserObject();
            _playerId = playerId;

            boolean isFriend = player.isFriend(_playerId);
            boolean isFoe = player.isFoe(_playerId);
            BMenuItem menuitem;
            if (!isFriend) {
                add(menuitem = new BMenuItem(
                    _ctx.xlate(GameCodes.GAME_MSGS, "m.folk_thumbs_up"),
                    getFFIcon(
                        _ctx, PlayerService.FOLK_IS_FRIEND), "make_friend"));
                menuitem.setStyleClass("player_status_ff_menuitem");
            }
            if (isFriend || isFoe) {
                add(menuitem = new BMenuItem(
                    _ctx.xlate(GameCodes.GAME_MSGS, "m.folk_neutral"),
                    getFFIcon(
                        _ctx, PlayerService.FOLK_NEUTRAL), "make_neutral"));
                menuitem.setStyleClass("player_status_ff_menuitem");
            }
            if (!isFoe) {
                add(menuitem = new BMenuItem(
                    _ctx.xlate(GameCodes.GAME_MSGS, "m.folk_thumbs_down"),
                    getFFIcon(
                        _ctx, PlayerService.FOLK_IS_FOE), "make_foe"));
                menuitem.setStyleClass("player_status_ff_menuitem");
            }
        }

        // from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            String action = event.getAction();
            int opinion;
            if ("make_friend".equals(action)) {
                opinion = PlayerService.FOLK_IS_FRIEND;
            } else if ("make_foe".equals(action)) {
                opinion = PlayerService.FOLK_IS_FOE;
            } else {
                opinion = PlayerService.FOLK_NEUTRAL;
            }
            InvocationService.ConfirmListener listener =
                new InvocationService.ConfirmListener() {
                public void requestProcessed () {
                    // TODO: confirmation?
                }
                public void requestFailed(String cause) {
                    log.warning("Folk note request failed: " + cause);
                    _ctx.getChatDirector().displayFeedback(
                        GameCodes.GAME_MSGS,
                        MessageBundle.tcompose("m.folk_note_failed", cause));
                }
            };
            _ctx.getClient().requireService(PlayerService.class).noteFolk(
                _playerId, opinion, listener);
        }

        protected int _playerId;
    }

    protected BangObject _bangobj;
    protected BangContext _ctx;
    protected int _pidx;
}
