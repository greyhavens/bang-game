//
// $Id$

package com.threerings.bang.client;

import com.jme.bui.BButton;
import com.jme.bui.BContainer;
import com.jme.bui.BLabel;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.TableLayout;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.surprise.Surprise;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays the name, score, cash and surprises held by a particular player.
 */
public class PlayerStatusView extends BContainer
    implements AttributeChangeListener, ElementUpdateListener, SetListener,
               ActionListener
{
    public PlayerStatusView (BangContext ctx, BangObject bangobj,
                             BangController ctrl, int pidx)
    {
        super(new TableLayout(1, 5, 5));

        _ctx = ctx;
        _bangobj = bangobj;
        _bangobj.addListener(this);
        _ctrl = ctrl;
        _pidx = pidx;

        BContainer top = new BContainer(new TableLayout(3, 5, 5));
        top.add(new BLabel(_bangobj.players[_pidx].toString()));
        top.add(_status = new BLabel(""));
        add(top);

        _surprises = new BContainer(new TableLayout(3, 5, 5));
        add(_surprises);

        updateStatus();
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
        if (event.getName().equals(BangObject.SURPRISES)) {
            Surprise s = (Surprise)event.getEntry();
            if (s.owner == _pidx) {
                _surprises.add(createButton(s));
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
        if (!event.getName().equals(BangObject.SURPRISES)) {
            return;
        }
        Surprise s = (Surprise)event.getOldEntry();
        if (s.owner != _pidx) {
            return;
        }
        String sid = "" + s.surpriseId;
        for (int ii = 0; ii < _surprises.getComponentCount(); ii++) {
            BButton button = (BButton)_surprises.getComponent(ii);
            if (sid.equals(button.getAction())) {
                _surprises.remove(button);
                return;
            }
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        try {
            _ctrl.placeSurprise(Integer.parseInt(event.getAction()));
        } catch (Exception e) {
            log.warning("Bogus surprise '" + event.getAction() + "': " + e);
        }
    }

    protected void updateStatus ()
    {
        String status = "S:" + _bangobj.points[_pidx];
        if (_bangobj.isInPlay() || _bangobj.state == BangObject.POST_ROUND) {
            status += " P:" + _bangobj.countLivePieces(_pidx) +
                " $:" + _bangobj.funds[_pidx] +
                " (" + _bangobj.reserves[_pidx] + ")";
        } else {
            status += " $:" + _bangobj.reserves[_pidx];
        }
        _status.setText(status);
    }

    protected BButton createButton (Surprise s)
    {
        BButton btn = new BButton(s.getIconPath());
        btn.setAction("" + s.surpriseId);
        btn.addListener(this);
        return btn;
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected BangController _ctrl;
    protected int _pidx;
    protected BContainer _surprises;
    protected BLabel _status;
}
