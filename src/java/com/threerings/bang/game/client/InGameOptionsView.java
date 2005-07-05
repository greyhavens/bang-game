//
// $Id$

package com.threerings.bang.game.client;

import com.jme.bui.event.ActionEvent;

import com.threerings.bang.client.EscapeMenuView;
import com.threerings.bang.util.BangContext;

/**
 * Displays options while a player is in a game.
 */
public class InGameOptionsView extends EscapeMenuView
{
    public InGameOptionsView (BangContext ctx)
    {
        super(ctx);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if ("leave_game".equals(action)) {
            _ctx.getLocationDirector().moveBack();
            dismiss();

        } else {
            super.actionPerformed(event);
        }
    }

    protected void addButtons ()
    {
        add(createButton("resume_game", "dismiss"));
        add(createButton("leave_game"));
        super.addButtons();
    }
}
