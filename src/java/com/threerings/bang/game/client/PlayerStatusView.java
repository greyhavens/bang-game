//
// $Id$

package com.threerings.bang.game.client;

import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.icon.SubimageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.card.Card;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the name, score, cash and cards held by a particular player.
 */
public class PlayerStatusView extends BContainer
    implements AttributeChangeListener, ElementUpdateListener, SetListener,
               ActionListener
{
    public PlayerStatusView (BangContext ctx, BangObject bangobj,
                             BangController ctrl, int pidx)
    {
        super(new AbsoluteLayout());
        setStyleClass("player_status_cont");

        _ctx = ctx;
        _bangobj = bangobj;
        _bangobj.addListener(this);
        _ctrl = ctrl;
        _pidx = pidx;

        // load up the background and rank images for our player
        _color = new ImageIcon(
            _ctx.loadImage("ui/pstatus/background" + pidx + ".png"));
        _rankimg = _ctx.loadImage("ui/pstatus/rank" + pidx + ".png");

        // create our interface elements
        int selfidx = _bangobj.getPlayerIndex(
            _ctx.getUserObject().getVisibleName());
        _player = new BLabel(_bangobj.players[_pidx].toString(),
                             "player_status" + _pidx);
        addListener(new HoverHelper(_ctrl, _pidx == selfidx ? "you" : "they"));
        add(_player, NAME_RECT);

        _points = new BLabel("");
        _points.addListener(new HoverHelper(_ctrl, "points"));
        add(_points, CASH_LOC);
        add(_ranklbl = new BLabel(createRankIcon(-2)), RANK_RECT);
        _ranklbl.addListener(new HoverHelper(_ctrl, "rank") {
            protected String computeHoveredItem () {
                return (_bangobj.state == BangObject.SELECT_PHASE ||
                        _bangobj.state == BangObject.BUYING_PHASE) ?
                    "pre_round_rank" : super.computeHoveredItem();
            }
        });

        updateAvatar();
        updateStatus();
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

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(BangObject.STATE)) {
            updateStatus();
        } else if (event.getName().equals(BangObject.AVATARS)) {
            updateAvatar();
        }
    }

    // documentation inherited from interface ElementUpdateListener
    public void elementUpdated (ElementUpdatedEvent event)
    {
        if (event.getName().equals(BangObject.AVATARS)) {
            if (event.getIndex() == _pidx) {
                updateAvatar();
            }
        } else {
            updateStatus();
        }
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        String name = event.getName();
        if (name.equals(BangObject.CARDS)) {
            Card card = (Card)event.getEntry();
            if (card.owner != _pidx) {
                return;
            }
            int cidx = -1;
            for (int ii = 0; ii < _cards.length; ii++) {
                if (_cards[ii] == null) {
                    cidx = ii;
                    break;
                }
            }
            if (cidx == -1) {
                return;
            }
            _cards[cidx] = createButton(card);
            _cards[cidx].addListener(
                new HoverHelper(_ctrl, "card_" + card.getType()));
            Rectangle rect = new Rectangle(CARD_RECT);
            rect.x += (rect.width * cidx);
            add(_cards[cidx], rect);
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (!event.getName().equals(BangObject.CARDS)) {
            return;
        }
        Card card = (Card)event.getOldEntry();
        if (card.owner != _pidx) {
            return;
        }
        String cid = "" + card.cardId;
        for (int ii = 0; ii < _cards.length; ii++) {
            if (cid.equals(_cards[ii].getAction())) {
                remove(_cards[ii]);
                _cards[ii] = null;
                return;
            }
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

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        // first draw our color
        _color.render(renderer, BACKGROUND_LOC.x, BACKGROUND_LOC.y, _alpha);

        // then draw our avatar
        if (_avatar != null) {
            _avatar.render(renderer, AVATAR_LOC.x, AVATAR_LOC.y, _alpha);
        }

        // then draw the normal background
        super.renderBackground(renderer);
    }

    protected void updateAvatar ()
    {
        // load up this player's avatar image
        if (_bangobj.avatars != null && _bangobj.avatars[_pidx] != null) {
            _avatar = new ImageIcon(
                AvatarView.getFramableImage(_ctx, _bangobj.avatars[_pidx], 10));
        }
    }

    protected void updateStatus ()
    {
        _points.setText("" + _bangobj.points[_pidx]);

        switch (_bangobj.state) {
        case BangObject.SELECT_PHASE:
            // when we're in the select phase, display whether or not this
            // player has selected their bigshot and cards
            setRank(_bangobj.bigShots[_pidx] == null ? -2 : -1);
            break;

        case BangObject.BUYING_PHASE:
            // TODO: publish something when the player 
            setRank(-2);
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
        BIcon icon = new ImageIcon(
            _ctx.loadImage("cards/" + card.getType() + "/icon.png"));
        BButton btn = new BButton(icon, "" + card.cardId);
        btn.setStyleClass("card_button");
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
    protected BangController _ctrl;
    protected int _pidx, _rank = -2;

    protected ImageIcon _color, _avatar;
    protected BImage _rankimg;

    protected BLabel _player, _points, _ranklbl;
    protected BButton[] _cards = new BButton[GameCodes.MAX_CARDS];

    protected static final Point BACKGROUND_LOC = new Point(33, 13);
    protected static final Point AVATAR_LOC = new Point(33, 8);
    protected static final Point CASH_LOC = new Point(97, 34);

    protected static final Rectangle RANK_RECT = new Rectangle(8, 35, 21, 23);
    protected static final Rectangle NAME_RECT = new Rectangle(11, 0, 100, 16);
    protected static final Rectangle CARD_RECT = new Rectangle(146, 16, 30, 39);
}
