//
// $Id$

package com.threerings.bang.game.client;

import com.jme.bui.BButton;
import com.jme.bui.BContainer;
import com.jme.bui.BDecoratedWindow;
import com.jme.bui.BLabel;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.BorderLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.ranch.client.UnitIcon;
import com.threerings.bang.ranch.client.UnitPalette;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.util.BangContext;

/**
 * Displays an interface for selecting a big shot and a starting hand of
 * cards from a player's inventory.
 */
public class SelectionView extends BDecoratedWindow
    implements ActionListener
{
    public SelectionView (BangContext ctx, BangConfig config,
                          BangObject bangobj, int pidx)
    {
        super(ctx.getLookAndFeel(), null);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        _bangobj = bangobj;
        _pidx = pidx;

        BContainer header = new BContainer(new BorderLayout(10, 0));
        header.add(new BLabel(_msgs.get("m.select_phase")), BorderLayout.WEST);
        String rmsg = _msgs.get(
            "m.round", ""+_bangobj.roundId, ""+config.rounds);
        header.add(new BLabel(rmsg), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // create the big shots display
        _units = new UnitPalette(ctx, null);
        _units.setUser(_ctx.getUserObject());
        add(_units, BorderLayout.CENTER);

        BContainer footer = new BContainer(new BorderLayout(10, 0));
        _ready = new BButton(_msgs.get("m.ready"));
        _ready.addListener(this);
//         _ready.setEnabled(false);
        footer.add(_ready, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent e)
    {
        UnitIcon icon = _units.getSelectedUnit();
        if (icon == null) {
            return;
        }

        // TODO: allow for selection of cards...

        int bigShotId = icon.getItemId();
        _bangobj.service.selectStarters(_ctx.getClient(), bigShotId, null);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _units.shutdown();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BangObject _bangobj;
    protected int _pidx;

    protected UnitPalette _units;
    protected BButton _ready;
}
