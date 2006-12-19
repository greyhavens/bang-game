//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.ActionEvent;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.Criterion;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays our bounty requirements before a bounty game.
 */
public class PreGameBountyView extends BDecoratedWindow
{
    public PreGameBountyView (final BangContext ctx, BangController ctrl,
                              BangObject bangobj, BangConfig config)
    {
        super(BangUI.stylesheet, ctx.xlate(GameCodes.GAME_MSGS, bangobj.marquee));
        setLayoutManager(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                              GroupLayout.STRETCH).setGap(15));
        _ctrl = ctrl;

        add(new BLabel(ctx.xlate(GameCodes.GAME_MSGS, "m.bounty_pregame")));

        BContainer ccont = new BContainer(GroupLayout.makeVert(GroupLayout.CENTER).
                                          setOffAxisJustification(GroupLayout.LEFT));
        PlayerObject user = ctx.getUserObject();
        for (Criterion crit : config.criteria) {
            String msg = ctx.xlate(GameCodes.GAME_MSGS, crit.getDescription());
            ccont.add(new BLabel(msg, "bounty_pregame_crit"));
        }
        add(ccont);

        add(new BLabel(ctx.xlate(GameCodes.GAME_MSGS, "m.bounty_goodluck")));

        BContainer butrow = GroupLayout.makeHBox(GroupLayout.CENTER);
        butrow.add(new BButton(ctx.xlate(GameCodes.GAME_MSGS, "m.ready"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                ctx.getBangClient().clearPopup(PreGameBountyView.this, true);
            }
        }, ""));
        add(butrow);
    }

    @Override // from BComponent
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctrl.playerReadyFor(BangObject.SKIP_SELECT_PHASE);
    }

    protected BangController _ctrl;
}
