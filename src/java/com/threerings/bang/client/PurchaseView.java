//
// $Id$

package com.threerings.bang.client;

import com.jme.bui.BContainer;
import com.jme.bui.BLabel;
import com.jme.bui.BWindow;
import com.jme.bui.layout.BorderLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangConfig;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays an interface for purchasing units.
 */
public class PurchaseView extends BWindow
{
    public PurchaseView (BangContext ctx, BangConfig config,
                         BangObject bangobj, int pidx)
    {
        super(ctx.getLookAndFeel(), new BorderLayout());

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        _bangobj = bangobj;
        _pidx = pidx;

        // create a header
        BContainer header = new BContainer(new BorderLayout());
        header.add(new BLabel(_msgs.get("m.buying_phase")), BorderLayout.WEST);
        String rmsg = _msgs.get(
            "m.round", ""+_bangobj.roundId, ""+config.rounds);
        header.add(new BLabel(rmsg), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BangObject _bangobj;
    protected int _pidx;
}
