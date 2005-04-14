//
// $Id$

package com.threerings.bang.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.MultiLineLabel;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;

import com.threerings.bang.client.sprite.UnitSprite;
import com.threerings.bang.data.BangObject;

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

        _name = new MultiLineLabel(_bangobj.players[_pidx].toString()) {
            public Dimension getPreferredSize () {
                Dimension d = super.getPreferredSize();
                d.width = 80;
                return d;
            }
        };
        _name.setStyle(MultiLineLabel.OUTLINE);
        _name.setForeground(UnitSprite.PIECE_COLORS[playerIdx]);
        _name.setAlternateColor(Color.black);
        _name.setFont(new Font("Dialog", Font.BOLD, 15));
        _name.setAlignment(MultiLineLabel.LEFT);
        add(_name, HGroupLayout.FIXED);
        add(_status = new JLabel());
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
        String status = "S: " + _bangobj.points[_pidx];
        if (_bangobj.state == BangObject.PRE_ROUND) {
            status += " $:" + _bangobj.reserves[_pidx];
        } else {
            status += " P: " + _bangobj.countLivePieces(_pidx) +
                " $:" + _bangobj.funds[_pidx] +
                " (" + _bangobj.reserves[_pidx] + ")";
        }
        _status.setText(status);
    }

    protected int _pidx;
    protected BangObject _bangobj;
    protected MultiLineLabel _name;
    protected JLabel _status;
}
