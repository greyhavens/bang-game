//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;

/**
 * Allows the user to page through his gang's history.
 */
public class HistoryDialog extends BDecoratedWindow
    implements ActionListener
{
    public HistoryDialog (BangContext ctx, HideoutObject hideoutobj)
    {
        super(ctx.getStyleSheet(), null);
        setModal(true);
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        _msgs = ctx.getMessageManager().getBundle(HideoutCodes.HIDEOUT_MSGS);
                
        BContainer bcont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
        bcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);
    }
    
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }
    
    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
    protected MessageBundle _msgs;
}
