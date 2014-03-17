//
// $Id$

package com.threerings.bang.station.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Rectangle;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

/**
 * Displays the map of the different towns and manages the buttons that will
 * take the player between them.
 */
public class MapView extends BContainer
{
    public MapView (BangContext ctx, final StationController ctrl)
    {
        super(new AbsoluteLayout());
        setStyleClass("station_map");
        _ctx = ctx;

        ActionListener disabler = new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                setPending(true);
                ctrl.actionPerformed(event);
            }
        };

        // add buttons or labels for each town
        _tbuts = new BComponent[TBUT_RECTS.length];
        _towns = new PairedButton[_tbuts.length];
        for (int ii = 0; ii < _tbuts.length; ii++) {
            String townId = BangCodes.TOWN_IDS[ii];
            String gocmd = StationController.TAKE_TRAIN + townId;

            _towns[ii] = new PairedButton(disabler, gocmd, "map_" + townId, null);
            add(_towns[ii], TOWN_RECTS[ii]);
            _towns[ii].setEnabled(false);

            if (townId.equals(ctx.getUserObject().townId)) {
                _tbuts[ii] = new BLabel("", "map_here");
            } else {
                _tbuts[ii] = new PairedButton(disabler, gocmd, "map_take", _towns[ii]);
                _towns[ii].setPair((PairedButton)_tbuts[ii]);
                // frontier town is always enabled (if we're not in frontier town), the other towns
                // all start disabled and we'll enable them if the player has a ticket
                boolean enabled = (ii == 0);
                _tbuts[ii].setEnabled(enabled);
                _towns[ii].setEnabled(enabled);
            }
            add(_tbuts[ii], TBUT_RECTS[ii]);
        }

        enableTownButtons();
    }

    /**
     * Notes that we are or are not in the process of moving between towns and enables or disables
     * our train buttons accordingly.
     */
    public void setPending (boolean pendingMove)
    {
        _pendingMove = pendingMove;
        enableTownButtons();
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _ctx.getUserObject().addListener(_enabler);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getUserObject().removeListener(_enabler);
    }

    protected void enableTownButtons ()
    {
        for (int ii = 0; ii < _tbuts.length; ii++) {
            if (!(_tbuts[ii] instanceof BButton)) {
                continue;
            }
            boolean enabled = !_pendingMove &&
                (ii == 0 || _ctx.getUserObject().holdsTicket(BangCodes.TOWN_IDS[ii]) ||
                 _ctx.getUserObject().holdsFreeTicket(BangCodes.TOWN_IDS[ii]));
            _tbuts[ii].setEnabled(enabled);
            _towns[ii].setEnabled(enabled);
        }
    }

    protected class PairedButton extends BButton
    {
        public PairedButton (ActionListener actlist, String command, String styleClass,
                             PairedButton pair) {
            super("", actlist, command);
            setStyleClass(styleClass);
            _pair = pair;
        }

        public void setPair (PairedButton pair) {
            _pair = pair;
        }

        public int getState () {
            int pstate = (_pair == null) ? DEFAULT : _pair.getSelfState();
            return (pstate == HOVER) ? pstate : super.getState();
        }

        protected int getSelfState () {
            return super.getState();
        }

        protected PairedButton _pair;
    }

    /** Listens for additions to the player's inventory and reenables our town buttons if they buy
     * a ticket. */
    protected SetAdapter<DSet.Entry> _enabler = new SetAdapter<DSet.Entry>() {
        public void entryAdded (EntryAddedEvent<DSet.Entry> event) {
            if (event.getName().equals(PlayerObject.INVENTORY)) {
                enableTownButtons();
            }
        }
    };

    protected BangContext _ctx;
    protected boolean _pendingMove;

    protected PairedButton[] _towns;
    protected BComponent[] _tbuts;

    protected static final Rectangle[] TOWN_RECTS = {
        new Rectangle(37, 149, 142, 119),
        new Rectangle(236, 396, 183, 71),
        new Rectangle(250, 135, 183, 71),
    };

    protected static final Rectangle[] TBUT_RECTS = {
        new Rectangle(75, 132, 88, 19),
        // this is a hack; the damned artists put a fucking gap in between the ITP (and Boom) town
        // images and the take train image so we expand the take train label up and down by 23
        // pixels to cover the gap; we'd expand it only up and bottom align everything except that
        // turns out not to be possible without giant fiasco thanks to the way we handle hovering
        new Rectangle(276, 354-23, 88, 19+2*23),
        new Rectangle(291, 61-23, 88, 19+2*23),
    };
}
