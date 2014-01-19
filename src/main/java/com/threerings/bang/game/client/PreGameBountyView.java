//
// $Id$

package com.threerings.bang.game.client;

import com.jme.renderer.Renderer;
import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;

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
    /**
     * Creates the appropriate view depending on the speaker type of the supplied quote.
     */
    public static BComponent createSpeakerView (BasicContext ctx, BountyConfig bounty,
                                                String gameId, BangConfig config,
                                                BountyConfig.Quote quote, boolean completed)
    {
        switch (quote.speaker) {
        case 0:
            return new BigShotPortrait(ctx, config.plist.get(0).bigShot);

        default:
            OutlawView oview = new OutlawView(ctx, 1f);
            BountyConfig.GameInfo info = bounty.getGame(gameId);
            if (quote.speaker >= 0 && info.opponents[quote.speaker] != null) {
                oview.setOutlaw(ctx, info.opponents[quote.speaker], completed, bounty.showBars);
            } else {
                oview.setOutlaw(ctx, bounty.getOutlaw(), completed, bounty.showBars);
            }
            return oview;
        }
    }

    public PreGameBountyView (final BangContext ctx, BangController ctrl,
                              BountyConfig bounty, String gameId, BangConfig config)
    {
        super(ctx, bounty.title + " - " + bounty.getGame(gameId).name);
        _ctrl = ctrl;
        setPreferredSize(770, -1);
        MessageBundle msgs = ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);

        _contents.setStyleClass("bounty_pregame");
        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(25));

        BContainer main = new BContainer(GroupLayout.makeHStretch().setGap(15));
        main.add(createSpeakerView(ctx, bounty, gameId, config,
                                   bounty.getGame(gameId).preGameQuote, false), GroupLayout.FIXED);

        BContainer vert = new BContainer(new BorderLayout());
        vert.add(new BLabel(msgs.get("m.bounty_pregame"), "bounty_title"), BorderLayout.NORTH);

        BContainer ccont = new BContainer(
            GroupLayout.makeVert(GroupLayout.CENTER).setOffAxisJustification(GroupLayout.LEFT));
        ImageIcon star = new ImageIcon(ctx.loadImage("ui/pregame/star.png"));
        for (Criterion crit : config.criteria) {
            BLabel clabel = new BLabel(msgs.xlate(crit.getDescription()), "bounty_pregame_crit");
            clabel.setIcon(star);
            ccont.add(clabel);
        }
        vert.add(GroupLayout.makeHBox(GroupLayout.CENTER, ccont), BorderLayout.CENTER);

        // if there any special game conditions (currently just no respawn), display them
        StringBuilder rules = new StringBuilder();
        if (!config.respawnUnits) {
            rules.append(msgs.get("m.no_respawn"));
        }
        if (rules.length() > 0) {
            vert.add(new BLabel(msgs.get("m.bounty_rules", rules), "left_label"),
                     BorderLayout.SOUTH);
        }

        main.add(vert);
        _contents.add(main);

        _contents.add(new BLabel(bounty.getGame(gameId).preGameQuote.text, "bounty_quote"));

        _buttons.add(new BButton(msgs.get("m.ready"), new ActionListener() {
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

    protected static class BigShotPortrait extends BLabel
    {
        public BigShotPortrait (BasicContext ctx, String bigShot) {
            super("", "bigshot_portrait");
            _frame = ctx.loadImage("ui/frames/small_frame.png");
            setIcon(new ImageIcon(ctx.loadImage("units/" + bigShot + "/portrait.png")));
            setPreferredSize(_frame.getWidth(), _frame.getHeight());
        }

        protected void wasAdded () {
            super.wasAdded();
            _frame.reference();
        }
        protected void wasRemoved () {
            super.wasRemoved();
            _frame.release();
        }
        protected void renderBorder (Renderer renderer) {
            super.renderBorder(renderer);
            _frame.render(renderer, 0, 0, _alpha);
        }

        protected BImage _frame;
    }

    protected BangController _ctrl;
}
