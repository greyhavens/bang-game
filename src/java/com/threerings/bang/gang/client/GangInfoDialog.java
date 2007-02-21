//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MainView;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangInfo;

/**
 * Displays information about a gang to any interested parties, much like the Wanted Poster
 * displays information on a player.
 */
public class GangInfoDialog extends BDecoratedWindow
    implements ActionListener, GangCodes
{
    /**
     * Pops up an info dialog for the specified gang if possible at the moment.
     */
    public static void display (BangContext ctx, Handle name)
    {
        if (ctx.getBangClient().canDisplayPopup(MainView.Type.POSTER_DISPLAY)) {
            ctx.getBangClient().displayPopup(new GangInfoDialog(ctx, name), true, 500);
        } else {
            BangUI.play(BangUI.FeedbackSound.INVALID_ACTION);
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    protected GangInfoDialog (BangContext ctx, Handle name)
    {
        super(ctx.getStyleSheet(), name.toString());
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(GANG_MSGS);

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        bcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont);

        // fetch the gang info from the server
        GangService gsvc = (GangService)ctx.getClient().requireService(GangService.class);
        gsvc.getGangInfo(ctx.getClient(), name, new GangService.ResultListener() {
            public void requestProcessed (Object result) {
                populate((GangInfo)result);
            }
            public void requestFailed (String cause) {
                add(getComponentCount() - 1, new BLabel(_msgs.xlate(cause)));
            }
        });
    }

    protected void populate (GangInfo info)
    {
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
