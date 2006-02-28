//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.util.TutorialUtil;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays a list of completed and uncompleted tutorials and allows the user
 * to play and replay them.
 */
public class PickTutorialView extends BDecoratedWindow
    implements ActionListener
{
    /**
     * Creates the pick tutorial view.
     *
     * @param completed the identifier for the just completed tutorial, or null
     * if we're displaying this view from in town.
     */
    public PickTutorialView (BangContext ctx, String completed)
    {
        super(ctx.getStyleSheet(), null);
        setLayoutManager(GroupLayout.makeVert(
                             GroupLayout.NONE, GroupLayout.CENTER,
                             GroupLayout.NONE));
        ((GroupLayout)getLayoutManager()).setGap(25);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);

        String tmsg, hmsg;
        if (completed == null) {
            tmsg = "m.tut_title";
            hmsg = "m.tut_intro";
        } else {
            tmsg = "m.tut_completed_title";
            hmsg = "m.tut_completed_intro";
        }
        add(new BLabel(_msgs.get(tmsg), "window_title"));
        add(new BLabel(_msgs.get(hmsg), "dialog_text"));

        ImageIcon comp = new ImageIcon(
            ctx.loadImage("ui/tutorials/complete.png"));
        ImageIcon incomp = new ImageIcon(
            ctx.loadImage("ui/tutorials/incomplete.png"));

        BContainer table = new BContainer(new TableLayout(2, 5, 15));
        add(table);
        for (int ii = 0; ii < BangCodes.TUTORIALS.length; ii++) {
            ImageIcon icon;
            String btext;
            if (false) { // check for completed tutorial
                icon = comp;
                btext = "m.tut_replay";
            } else {
                icon = incomp;
                btext = "m.tut_play";
            }

            String ttext = _msgs.get("m.tut_" + BangCodes.TUTORIALS[ii]);
            BLabel tlabel = new BLabel(ttext, "tutorial_text");
            tlabel.setIcon(icon);
            table.add(tlabel);

            BButton play = new BButton(
                _msgs.get(btext), this, BangCodes.TUTORIALS[ii]);
            play.setStyleClass("alt_button");
            table.add(play);
        }

        if (completed == null) {
            add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        } else {
            add(new BButton(_msgs.get("m.to_town"), this, "to_town"));
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);

        } else if (action.equals("to_town")) {
            dismiss();
            if (!_ctx.getLocationDirector().moveBack()) {
                _ctx.getLocationDirector().leavePlace();
                _ctx.getBangClient().showTownView();
            }

        } else {
            // TODO: tidy this up; make special start tutorial service
            TutorialConfig tconfig =
                TutorialUtil.loadTutorial(_ctx.getResourceManager(), action);
            BangConfig config = new BangConfig();
            config.rated = false;
            config.players = new Name[] {
                _ctx.getUserObject().getVisibleName(),
                new Name("Larry") };
            config.ais = new GameAI[] {
                null, new GameAI(1, 50) };
            config.scenarios = new String[] { tconfig.ident };
            config.tutorial = true;
            config.board = tconfig.board;
            ConfirmListener cl = new ConfirmListener() {
                public void requestProcessed () {
                }
                public void requestFailed (String reason) {
                    log.warning("Failed to start tutorial: " + reason);
                }
            };
            _ctx.getParlorDirector().startSolitaire(config, cl);
        }
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return super.computePreferredSize(400, -1);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
