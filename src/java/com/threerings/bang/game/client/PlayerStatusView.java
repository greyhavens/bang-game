//
// $Id$

package com.threerings.bang.game.client;

import com.jme.bui.BButton;
import com.jme.bui.BContainer;
import com.jme.bui.BIcon;
import com.jme.bui.BLabel;
import com.jme.bui.BLookAndFeel;
import com.jme.bui.ImageIcon;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.GroupLayout;
import com.jme.bui.layout.TableLayout;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.game.client.sprite.UnitSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

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
        super(GroupLayout.makeHoriz(GroupLayout.LEFT));

        _ctx = ctx;
        _bangobj = bangobj;
        _bangobj.addListener(this);
        _ctrl = ctrl;
        _pidx = pidx;

        BContainer bits = new BContainer(new TableLayout(2, 5, 5));
        bits.add(_player = new BLabel(_bangobj.players[_pidx].toString()));
        bits.add(_cash = new BLabel(""));
        bits.add(_pieces = new BLabel(""));
        add(bits);

        updateStatus();
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        BLookAndFeel lnf = getLookAndFeel().deriveLookAndFeel();
        lnf.setForeground(UnitSprite.JPIECE_COLORS[_pidx]);
        _player.setLookAndFeel(lnf);
        super.wasAdded();
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(BangObject.STATE)) {
            updateStatus();
        }
    }

    // documentation inherited from interface ElementUpdateListener
    public void elementUpdated (ElementUpdatedEvent event)
    {
        updateStatus();
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(BangObject.CARDS)) {
            Card card = (Card)event.getEntry();
            if (card.owner == _pidx) {
                add(createButton(card));
            }
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        // NOOP
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
        for (int ii = 1; ii < getComponentCount(); ii++) {
            BButton button = (BButton)getComponent(ii);
            if (cid.equals(button.getAction())) {
                remove(button);
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

    protected void updateStatus ()
    {
        if (_bangobj.isInPlay() || _bangobj.state == BangObject.POST_ROUND) {
            _pieces.setText("P" + _bangobj.countLivePieces(_pidx));
        } else {
            _pieces.setText("");
        }
        _cash.setText("$" + _bangobj.funds[_pidx]);
    }

    protected BButton createButton (Card card)
    {
        BIcon icon = new ImageIcon(
            _ctx.loadImage("cards/" + card.getIconPath() + "/icon.png"));
        BButton btn = new BButton(icon, "" + card.cardId);
        btn.addListener(this);
        return btn;
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected BangController _ctrl;
    protected int _pidx;
    protected BLabel _player, _cash, _pieces;
}
