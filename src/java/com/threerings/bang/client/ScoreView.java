//
// $Id$

package com.threerings.bang.client;

import com.jme.bui.BLabel;
import com.jme.bui.BWindow;
import com.jme.bui.layout.TableLayout;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.util.BangContext;

/**
 * Displays information on a particular player's score.
 */
public class ScoreView extends BWindow
    implements AttributeChangeListener, ElementUpdateListener
{
    public ScoreView (BangContext ctx, BangObject bangobj)
    {
        super(ctx.getLookAndFeel(), new TableLayout(2, 2, 10));

        _bangobj = bangobj;
        _bangobj.addListener(this);

        _status = new BLabel[_bangobj.players.length];
        for (int ii = 0; ii < _bangobj.players.length; ii++) {
            BLabel name = new BLabel(_bangobj.players[ii].toString());
            add(name);
//         _name.setStyle(MultiLineLabel.OUTLINE);
//         _name.setForeground(UnitSprite.PIECE_COLORS[playerIdx]);
//         _name.setAlternateColor(Color.black);
//         _name.setFont(new Font("Dialog", Font.BOLD, 15));
//         _name.setAlignment(MultiLineLabel.LEFT);
            add(_status[ii] = new BLabel(""));
        }
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();
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
        for (int ii = 0; ii < _bangobj.players.length; ii++) {
            String status = "S:" + _bangobj.points[ii];
            if (_bangobj.state == BangObject.SELECT_PHASE ||
                _bangobj.state == BangObject.BUYING_PHASE) {
                status += " $:" + _bangobj.reserves[ii];
            } else {
                status += " P:" + _bangobj.countLivePieces(ii) +
                    " $:" + _bangobj.funds[ii] +
                    " (" + _bangobj.reserves[ii] + ")";
            }
            _status[ii].setText(status);
        }
    }

    protected BangObject _bangobj;
    protected BLabel[] _status;
}
