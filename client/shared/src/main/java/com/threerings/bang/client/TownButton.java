//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Displays a "To Town" button that leaves the current place when clicked.
 */
public class TownButton extends BButton
    implements ActionListener
{
    public TownButton (BangContext ctx)
    {
        super(ctx.xlate(BangCodes.BANG_MSGS, "m.to_town"));
        _ctx = ctx;
        addListener(this);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        setEnabled(false);
        _ctx.getBangClient().showTownView();
    }

    protected BangContext _ctx;
}
