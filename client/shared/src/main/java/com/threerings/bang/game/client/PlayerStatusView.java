//
// $Id$

package com.threerings.bang.game.client;

import com.jme.renderer.Renderer;
import com.jme.scene.Controller;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.icon.SubimageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.ResultListener;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.avatar.client.AvatarView;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.card.Card;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the name, score, cash and cards held by a particular player.
 */
public class PlayerStatusView extends BContainer
    implements AttributeChangeListener, ElementUpdateListener, SetListener<DSet.Entry>,
               ActionListener
{
    public PlayerStatusView (BangContext ctx, BangObject bangobj,
                             BangConfig bconfig, BangController ctrl, int pidx)
    {
        super(new AbsoluteLayout());
        setStyleClass("player_status_cont");

        _ctx = ctx;
        _bangobj = bangobj;
        _bangobj.addListener(this);
        _bconfig = bconfig;
        _ctrl = ctrl;
        _pidx = pidx;

        // load up the background and rank images for our player
        int lookup = colorLookup[pidx + 1];
        _color = new ImageIcon(
            _ctx.loadImage("ui/pstatus/background" + lookup + ".png"));
        _rankimg = _ctx.loadImage("ui/pstatus/rank" + lookup + ".png");
        ImageIcon ribbon = new ImageIcon(_ctx.loadImage("ui/pstatus/ribbon" + lookup + ".png"));

        // create our interface elements
        int selfidx = _bangobj.getPlayerIndex(
            _ctx.getUserObject().getVisibleName());
        add(new BLabel(ribbon), RIBBON_LOC);
        _player = new BLabel(_bangobj.players[_pidx].toString(), "player_status_name");
        _player.setFit(BLabel.Fit.SCALE);
        String hmsg = "m.help_" + (_pidx == selfidx ? "you" : "they");
        _player.setTooltipText(ctx.xlate(GameCodes.GAME_MSGS, hmsg));
        add(_player, NAME_RECT);

        _points = new BLabel("", "player_status_score");
        _points.setTooltipText(ctx.xlate(GameCodes.GAME_MSGS, "m.help_points"));
        add(_points, CASH_LOC);
        add(_ranklbl = new BLabel(createRankIcon(-2)) {
            public String getTooltipText () {
                String hmsg = (_bangobj.state == BangObject.SELECT_PHASE) ?
                    "pre_round_rank" : "rank";
                return _ctx.xlate(GameCodes.GAME_MSGS, "m.help_" + hmsg);
            }
        }, RANK_RECT);

        // add a listener for events associated with VS_PLAYER cards
        addListener(new MouseAdapter() {
            public void mousePressed (MouseEvent e) {
                Card card = _ctrl.getPlacingCard();
                if (card == null) {
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (card.isValidPlayer(_bangobj, _pidx)) {
                        _ctrl.activateCard(card.cardId, Integer.valueOf(_pidx));
                    }
                } else {
                    _ctrl.cancelCardPlacement();
                }
            }
        });

        updateAvatar();
        updateStatus();
        checkPlayerHere();
    }

    /**
     * Sets the rank displayed for this player to the specified index (0 is 1st
     * place, 1 is 2nd, etc.). -2 is blank and -1 is a star which are used
     * during the pre-game phase.
     */
    public void setRank (int rank)
    {
        if (_rank != rank) {
            _rank = rank;
            _ranklbl.setIcon(createRankIcon(rank));
        }
    }

    /**
     * Returns the player index for this player status view.
     */
    public int getPidx ()
    {
        return _pidx;
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(BangObject.STATE)) {
            updatePoints();
            updateStatus();

        } else if (name.equals(BangObject.PLAYER_INFO)) {
            updateAvatar();
        }
    }

    // documentation inherited from interface ElementUpdateListener
    public void elementUpdated (ElementUpdatedEvent event)
    {
        if (event.getName().equals(BangObject.PLAYER_INFO)) {
            if (event.getIndex() == _pidx) {
                updateAvatar();
                updateStatus();
            }

        } else if (event.getName().equals(BangObject.PLAYER_STATUS)) {
            updateStatus();

        } else if (event.getName().equals(BangObject.POINTS)) {
            updatePoints();
        }
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent<DSet.Entry> event)
    {
        String name = event.getName();
        if (name.equals(BangObject.CARDS)) {
            Card card = (Card)event.getEntry();
            if (card.owner == _pidx) {
                cardAdded(card, false);
            }
        } else if (name.equals(BangObject.OCCUPANT_INFO)) {
            checkPlayerHere();
        }
    }

    /**
     * Notifies the view that a card has been added to the player's hand.
     *
     * @param drop if true, animate the card dropping into the hand
     */
    public void cardAdded (Card card, boolean drop)
    {
        int cidx = -1;
        for (int ii = 0; ii < _cards.length; ii++) {
            if (_cards[ii] == null) {
                cidx = ii;
                break;
            }
            // if this is not a fake card, replace an older fake card with a
            // real card that arrived when the round actually started
            if (card.cardId != 0 && _cards[ii].getAction().equals("0")) {
                remove(_cards[ii]);
                cidx = ii;
                break;
            }
        }
        if (cidx == -1) {
            return;
        }
        _cards[cidx] = createButton(card);
        Rectangle rect = new Rectangle(CARD_RECT);
        rect.x += (rect.width * cidx);
        add(_cards[cidx], rect);
        if (drop) {
            flyCard(_cards[cidx], DROP_PLAY_HEIGHT, true, 0f,
                DROP_PLAY_DURATION);
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent<DSet.Entry> event)
    {
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent<DSet.Entry> event)
    {
        String name = event.getName();
        if (name.equals(BangObject.CARDS)) {
            Card card = (Card)event.getOldEntry();
            if (card.owner == _pidx) {
                cardRemoved(card, false, false);
            }
        } else if (name.equals(BangObject.OCCUPANT_INFO)) {
            checkPlayerHere();
        }
    }

    /**
     * Notifies the view that a card has been removed from the player's hand.
     *
     * @param fall if true, animate the card falling out of the hand
     * @param play if true, animate the card flying onto the board
     */
    public void cardRemoved (Card card, boolean fall, boolean play)
    {
        String cid = "" + card.cardId;
        for (int ii = 0; ii < _cards.length; ii++) {
            if (_cards[ii] == null) {
                continue;
            }
            if (cid.equals(_cards[ii].getAction())) {
                if (fall) {
                    flyCard(_cards[ii], FALL_DEPTH, false, FALL_DELAY,
                        FALL_DURATION);
                } else if (play) {
                    flyCard(_cards[ii], DROP_PLAY_HEIGHT, false, 0f,
                        DROP_PLAY_DURATION);
                } else {
                    remove(_cards[ii]);
                }
                _cards[ii] = null;
                return;
            }
        }
    }

    /**
     * Performs the card action for the card at this index.
     */
    public void playCardAtIndex (int idx)
    {
        if (idx < _cards.length && _cards[idx] != null) {
            _ctrl.placeCard(Integer.parseInt(_cards[idx].getAction()));
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        try {
            _ctrl.placeCard(Integer.parseInt(event.getAction()));
        } catch (Exception e) {
            log.warning("Bogus card '" + event.getAction() + "': " + e);
        }
    }

    /**
     * Show the friendly folks menu.
     */
    public void showFriendlyFolks ()
    {
        add(new FriendlyFolkButton(_ctx, _bangobj, _pidx), FF_LOC);
    }

    /**
     * Flies a card up or down, fading it in or fading it out and
     * removing it.
     */
    protected void flyCard (
        final BButton card, final int height, final boolean in,
        final float delay, final float duration)
    {
        _ctx.getRootNode().addController(new Controller() {
            public void update (float time) {
                if ((_elapsed += time) >= duration + delay) {
                    _ctx.getRootNode().removeController(this);
                    if (in) {
                        card.setAlpha(1f);
                        card.setLocation(card.getX(), CARD_RECT.y);
                    } else {
                        remove(card);
                    }
                } else if (_elapsed > delay) {
                    float alpha = (_elapsed - delay) / duration,
                        ralpha = 1f - alpha;
                    card.setAlpha(in ? alpha : ralpha);
                    card.setLocation(card.getX(), CARD_RECT.y +
                        (int)(height * (in ? ralpha : alpha)));
                }
            }
            protected float _elapsed;
        });
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        _color.wasAdded();
        if (_avatar != null) {
            _avatar.wasAdded();
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        _color.wasRemoved();
        if (_avatar != null) {
            _avatar.wasRemoved();
        }
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        // first draw our color
        _color.render(renderer, BACKGROUND_LOC.x, BACKGROUND_LOC.y, _alpha);

        // then draw our avatar
        if (_avatar != null && _playerHere) {
            _avatar.render(renderer, AVATAR_LOC.x, AVATAR_LOC.y, _alpha);
        }

        // then draw the normal background
        super.renderBackground(renderer);
    }

    protected void updateAvatar ()
    {
        // load up this player's avatar image
        if (_bangobj.playerInfo != null &&
            _bangobj.playerInfo[_pidx].avatar != null) {
            AvatarView.getFramableImage(_ctx, _bangobj.playerInfo[_pidx].avatar, 9,
                                        new ResultListener<BImage>() {
                public void requestCompleted (BImage image) {
                    setAvatar(new ImageIcon(image));
                }
                public void requestFailed (Exception cause) {
                    // not called
                }
            });
        }
    }

    protected void setAvatar (ImageIcon avatar)
    {
        if (_avatar != null && isAdded()) {
            _avatar.wasRemoved();
        }
        _avatar = avatar;
        if (isAdded()) {
            _avatar.wasAdded();
        }
    }

    protected void checkPlayerHere ()
    {
        _playerHere = (_bconfig.ais != null && _bconfig.ais.length > _pidx &&
                       _bconfig.ais[_pidx] != null) ||
            (_bangobj.getOccupantInfo(_bangobj.players[_pidx]) != null);
    }

    protected void updatePoints ()
    {
        int points = 0;
        if (!_bangobj.isTeamGame()) {
            points = _bangobj.points[_pidx];
        } else {
            int tidx = _bangobj.teams[_pidx];
            for (int ii = 0; ii < _bangobj.teams.length; ii++) {
                if (tidx == _bangobj.teams[ii]) {
                    points += _bangobj.points[ii];
                }
            }
        }
        _points.setText("" + points);
    }

    protected void updateStatus ()
    {
        switch (_bangobj.state) {
        case BangObject.SELECT_PHASE:
            setRank(_bangobj.playerInfo[_pidx].readyState ==
                    BangObject.SELECT_PHASE ? -1 : -2);
            break;

        case BangObject.IN_PLAY:
            // on the first tick we wait for everyone to load their units
            if (_bangobj.tick == 0) {
                setRank(_bangobj.playerInfo[_pidx].readyState ==
                        BangObject.IN_PLAY ? -1 : -2);
            }
            break;

        case BangObject.GAME_OVER:
            // clear out our rankings when the game is over as the real
            // rankings will be displayed
            setRank(-2);
            break;
        }
    }

    protected BButton createButton (Card card)
    {
        BIcon icon = new ImageIcon(_ctx.loadImage(card.getIconPath("icon")));
        BButton btn = new BButton(icon, "" + card.cardId);
        btn.setStyleClass(card.getStyle());
        btn.setTooltipText(_ctx.xlate(BangCodes.CARDS_MSGS,
                                      CardItem.getTooltipText(card.getType())));
        btn.addListener(this);
        return btn;
    }

    protected SubimageIcon createRankIcon (int rank)
    {
        return new SubimageIcon(
            _rankimg, (rank + 2) * RANK_RECT.width, 0,
            RANK_RECT.width, RANK_RECT.height);
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected BangConfig _bconfig;
    protected BangController _ctrl;
    protected int _pidx, _rank = -2;

    protected ImageIcon _color, _avatar;
    protected BImage _rankimg;
    protected boolean _playerHere;

    protected BLabel _player, _points, _ranklbl;
    protected BButton[] _cards = new BButton[GameCodes.MAX_CARDS];

    protected static final Point BACKGROUND_LOC = new Point(33, 15);
    protected static final Point AVATAR_LOC = new Point(33, 4);
    protected static final Point CASH_LOC = new Point(97, 36);
    protected static final Point FF_LOC = new Point(0, 32);
    protected static final Point RIBBON_LOC = new Point(7, 1);

    protected static final Rectangle RANK_RECT = new Rectangle(8, 37, 21, 23);
    protected static final Rectangle NAME_RECT = new Rectangle(12, 2, 98, 16);
    protected static final Rectangle CARD_RECT = new Rectangle(146, 18, 30, 39);

    /** The height from which to drop added cards into the hand (also the
     * height to which to fly played cards). */
    protected static final int DROP_PLAY_HEIGHT = 100;

    /** The duration of the drop into the hand or the flight onto the board. */
    protected static final float DROP_PLAY_DURATION = 0.25f;

    /** The delay before dropped cards fall out of the hand. */
    protected static final float FALL_DELAY = BangView.CARD_FALL_DURATION;

    /** The depth to which cards fall when removed from the hand. */
    protected static final int FALL_DEPTH = -50;

    /** The duration of the fall of cards from the hand. */
    protected static final float FALL_DURATION = 0.125f;
}
