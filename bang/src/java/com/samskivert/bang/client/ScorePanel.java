//
// $Id$

package com.samskivert.bang.client;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.HGroupLayout;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;

import com.samskivert.bang.data.BangObject;

/**
 * Displays information on a particular player's score.
 */
public class ScorePanel extends JPanel
    implements AttributeChangeListener, ElementUpdateListener
{
    public ScorePanel (BangObject bangobj, int playerIdx)
    {
        super(new HGroupLayout(HGroupLayout.STRETCH, HGroupLayout.NONE,
                               5, HGroupLayout.LEFT));
        _pidx = playerIdx;
        _bangobj = bangobj;
        _bangobj.addListener(this);

        add(_name = new JLabel(_bangobj.players[_pidx].toString()),
            new HGroupLayout.Constraints(1));
        add(_status = new JLabel(), new HGroupLayout.Constraints(2));
    }

    @Override // documentation inherited
    public void removeNotify ()
    {
        super.removeNotify();
        _bangobj.removeListener(this);
        _bangobj = null;
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

    protected void updateStatus ()
    {
        String status = "P: " + _bangobj.countLivePieces(_pidx) +
            " $:" + _bangobj.funds[_pidx] +
            " (" + _bangobj.reserves[_pidx] + ")";
        _status.setText(status);
    }

    protected int _pidx;
    protected BangObject _bangobj;
    protected JLabel _name, _status;
}
