//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.util.BangContext;

/**
 * Displays options while a player is in a game.
 */
public class InGameOptionsView extends BDecoratedWindow
    implements ActionListener
{
    public InGameOptionsView (BangContext ctx, BangObject bangobj)
    {
        super(ctx.getStyleSheet(), null);

        _modal = true;
        _ctx = ctx;
        _bangobj = bangobj;

        add(_title = new BLabel("", "window_title"), GroupLayout.FIXED);

        MessageBundle msgs = ctx.getMessageManager().getBundle("options");
        BContainer box = GroupLayout.makeHBox(GroupLayout.CENTER);
        box.add(new BButton(msgs.get("m.exit"), this, "exit"));
        box.add(new BButton(msgs.get("m.to_town"), this, "to_town"));
        box.add(new BButton(msgs.get("m.resume"), this, "dismiss"));
        add(box, GroupLayout.FIXED);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if ("to_town".equals(action)) {
            if (_ctx.getLocationDirector().leavePlace()) {
                _ctx.getBangClient().clearPopup(this, true);
                _ctx.getBangClient().showTownView();
            }

        } else if ("dismiss".equals(action)) {
            _ctx.getBangClient().clearPopup(this, true);

        } else if ("exit".equals(action)) {
            _ctx.getApp().stop();
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // update our title with the current board info
        _title.setText(_bangobj.boardName + " - " +
                       _ctx.xlate(GameCodes.GAME_MSGS,
                                  "m.scenario_" + _bangobj.scenarioId));
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected BLabel _title;
}
