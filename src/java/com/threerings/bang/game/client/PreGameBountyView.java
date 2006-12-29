//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bounty.client.OutlawView;
import com.threerings.bang.bounty.data.BountyConfig;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.Criterion;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays our bounty requirements before a bounty game.
 */
public class PreGameBountyView extends SteelWindow
{
    public PreGameBountyView (final BangContext ctx, BangController ctrl,
                              BountyConfig bounty, String gameId, BangConfig config)
    {
        super(ctx, bounty.title + " - " + bounty.getGame(gameId).name);
        _ctrl = ctrl;
        setPreferredSize(770, -1);

        _contents.setStyleClass("bounty_pregame");
        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(25));

        BContainer main = new BContainer(GroupLayout.makeHStretch().setGap(15));
        OutlawView oview = new OutlawView(ctx, 1f);
        main.add(oview, GroupLayout.FIXED);
        oview.setOutlaw(ctx, bounty.outlawPrint, false);

        BContainer vert = new BContainer(GroupLayout.makeVStretch());
        vert.add(new BLabel(ctx.xlate(GameCodes.GAME_MSGS, "m.bounty_pregame"),
                            "bounty_pregame_title"), GroupLayout.FIXED);

        BContainer ccont = new BContainer(GroupLayout.makeVert(GroupLayout.CENTER).
                                          setOffAxisJustification(GroupLayout.LEFT));
        PlayerObject user = ctx.getUserObject();
        ImageIcon star = new ImageIcon(ctx.loadImage("ui/pregame/star.png"));
        for (Criterion crit : config.criteria) {
            String msg = ctx.xlate(GameCodes.GAME_MSGS, crit.getDescription());
            BLabel clabel = new BLabel(msg, "bounty_pregame_crit");
            clabel.setIcon(star);
            ccont.add(clabel);
        }
        vert.add(ccont);
        main.add(vert);
        _contents.add(main);

        _contents.add(new BLabel(bounty.getGame(gameId).preGameQuote, "bounty_pregame_quote"));

        _buttons.add(new BButton(ctx.xlate(GameCodes.GAME_MSGS, "m.ready"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                ctx.getBangClient().clearPopup(PreGameBountyView.this, true);
            }
        }, ""));
    }

    @Override // from BComponent
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctrl.playerReadyFor(BangObject.SKIP_SELECT_PHASE);
    }

    protected BangController _ctrl;
}
