//
// $Id$

package com.threerings.bang.client;

import java.awt.Dimension;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.CommandButton;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.sprite.UnitSprite;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.card.Card;

/**
 * Displays the cards held by a particular player in the game.
 */
public class CardPanel extends JPanel
    implements PlaceView, SetListener
{
    public CardPanel (int playerIdx, int ourIdx)
    {
        super(new HGroupLayout(HGroupLayout.NONE, HGroupLayout.LEFT));
        _pidx = playerIdx;
        _oidx = ourIdx;
        setBorder(BorderFactory.createMatteBorder(
                      2, 2, 2, 2, UnitSprite.PIECE_COLORS[_pidx]));
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _bangobj = (BangObject)plobj;
        _bangobj.addListener(this);

        for (Iterator iter = _bangobj.cards.iterator(); iter.hasNext(); ) {
            Card card = (Card)iter.next();
            if (card.owner == _pidx) {
                add(createButton(card));
            }
        }
        SwingUtil.refresh(this);
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        _bangobj.removeListener(this);
        _bangobj = null;
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(BangObject.CARDS)) {
            Card card = (Card)event.getEntry();
            if (card.owner == _pidx) {
                add(createButton(card));
                SwingUtil.refresh(this);
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
        for (int ii = 0; ii < getComponentCount(); ii++) {
            CommandButton button = (CommandButton)getComponent(ii);
            if ((Integer)button.getActionArgument() == card.cardId) {
                remove(button);
                SwingUtil.refresh(this);
                return;
            }
        }
    }

    @Override // documentation inherited
    public Dimension getPreferredSize ()
    {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, 20);
        return d;
    }

    protected CommandButton createButton (Card card)
    {
        CommandButton btn = new CommandButton();
        btn.setText(card.getIconPath());
        btn.setActionCommand(BangController.PLACE_CARD);
        btn.setActionArgument(card.cardId);
        btn.addActionListener(BangController.DISPATCHER);
        btn.setEnabled(_pidx == _oidx);
        return btn;
    }

    protected BangObject _bangobj;
    protected int _pidx, _oidx;
}
