//
// $Id$

package com.samskivert.bang.client;

import java.util.Iterator;
import javax.swing.BorderFactory;
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

import com.samskivert.bang.client.sprite.UnitSprite;
import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.surprise.Surprise;

/**
 * Displays the surprises held by a particular player in the game.
 */
public class SurprisePanel extends JPanel
    implements PlaceView, SetListener
{
    public SurprisePanel (int playerIdx, int ourIdx)
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

        for (Iterator iter = _bangobj.surprises.iterator(); iter.hasNext(); ) {
            Surprise s = (Surprise)iter.next();
            if (s.owner == _pidx) {
                add(createButton(s));
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
        if (event.getName().equals(BangObject.SURPRISES)) {
            Surprise s = (Surprise)event.getEntry();
            if (s.owner == _pidx) {
                add(createButton(s));
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
        if (!event.getName().equals(BangObject.SURPRISES)) {
            return;
        }
        Surprise s = (Surprise)event.getOldEntry();
        if (s.owner != _pidx) {
            return;
        }
        for (int ii = 0; ii < getComponentCount(); ii++) {
            CommandButton button = (CommandButton)getComponent(ii);
            if ((Integer)button.getActionArgument() == s.surpriseId) {
                remove(button);
                SwingUtil.refresh(this);
                return;
            }
        }
    }

    protected CommandButton createButton (Surprise s)
    {
        CommandButton btn = new CommandButton();
        btn.setText(s.getIconPath());
        btn.setActionCommand(BangController.PLACE_SURPRISE);
        btn.setActionArgument(s.surpriseId);
        btn.addActionListener(BangController.DISPATCHER);
        btn.setEnabled(_pidx == _oidx);
        return btn;
    }

    protected BangObject _bangobj;
    protected int _pidx, _oidx;
}
