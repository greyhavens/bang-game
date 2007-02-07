//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.parlor.game.data.GameAI;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.util.TutorialUtil;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.util.ReportingListener;
import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Displays a list of completed and uncompleted tutorials and allows the user to (re)play them.
 */
public class PickTutorialView extends BDecoratedWindow
    implements ActionListener
{
    /** The various modes in which we pop up the tutorial view. */
    public static enum Mode { FIRST_TIME, FKEY, COMPLETED };

    /** The width we prefer to have passed to {@link #pack}. */
    public static final int WIDTH_HINT = 500;

    /**
     * Creates the pick tutorial view.
     *
     * @param completed the identifier for the just completed tutorial, or null if we're displaying
     * this view from in town.
     */
    public PickTutorialView (BangContext ctx, Mode mode)
    {
        super(ctx.getStyleSheet(), null);
        setStyleClass("dialog_window");
        setLayoutManager(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.CENTER,
                                              GroupLayout.NONE));
        ((GroupLayout)getLayoutManager()).setGap(25);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        PlayerObject self = _ctx.getUserObject();

        String tmsg, hmsg;
        switch (mode) {
        default:
        case FKEY:
            tmsg = "m.tut_title";
            hmsg = "m.tut_intro";
            break;
        case FIRST_TIME:
            tmsg = "m.tut_title_" + self.townId;
            hmsg = "m.tut_intro_" + self.townId;
            break;
        case COMPLETED:
            tmsg = "m.tut_completed_title";
            hmsg = "m.tut_completed_intro";
            break;
        }
        add(new BLabel(_msgs.get(tmsg), "window_title"));
        add(new BLabel(_msgs.get(hmsg), "dialog_text"));

        BContainer table = new BContainer(new TableLayout(2, 5, 15));
        add(table);

        boolean unplayed = false, enabled = true;
        int townIdx = BangUtil.getTownIndex(self.townId);
        for (int ii = 0; ii < TutorialCodes.TUTORIALS[townIdx].length; ii++) {
            String tid = TutorialCodes.TUTORIALS[townIdx][ii];
            BIcon icon;
            String btext;
            if (self.stats.containsValue(Stat.Type.TUTORIALS_COMPLETED, tid)) {
                icon = BangUI.completed;
                btext = "m.tut_replay";
            } else {
                icon = BangUI.incomplete;
                btext = "m.tut_play";
                unplayed = true;
            }

            BLabel tlabel = new BLabel(_msgs.get("m.tut_" + tid), "tutorial_text");
            tlabel.setIcon(icon);
            table.add(tlabel);

            BButton play = new BButton(_msgs.get(btext), this, tid);
            play.setStyleClass("alt_button");
            table.add(play);

            if (unplayed) {
                tlabel.setEnabled(enabled);
                enabled = false; // everything after the first unplayed tutorial is greyed out
            }
        }

        add(new BLabel(_msgs.get("m.tut_access_key"), "dialog_tip"));

        if (mode != Mode.COMPLETED) {
            add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        } else {
            BContainer bbox = GroupLayout.makeHBox(GroupLayout.CENTER);
            bbox.add(new BButton(_msgs.get("m.to_town"), this, "to_town"));
            bbox.add(new BButton(_msgs.get("m.to_ranch"), this, "to_ranch"));
            bbox.add(new BButton(_msgs.get("m.to_saloon"), this, "to_saloon"));
            add(bbox);
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
            _ctx.getBangClient().checkShowIntro(false);

        } else if (action.startsWith("to_")) {
            _ctx.getBangClient().clearPopup(this, true);
            BangBootstrapData bbd = (BangBootstrapData)_ctx.getClient().getBootstrapData();

            if (action.equals("to_town")) {
                _ctx.getLocationDirector().leavePlace();
                _ctx.getBangClient().showTownView();
            } else if (action.equals("to_ranch")) {
                _ctx.getLocationDirector().moveTo(bbd.ranchOid);
            } else if (action.equals("to_saloon")) {
                _ctx.getLocationDirector().moveTo(bbd.saloonOid);
            }

        } else {
            PlayerService psvc = (PlayerService)
                _ctx.getClient().requireService(PlayerService.class);
            ReportingListener rl = new ReportingListener(
                _ctx, BangCodes.BANG_MSGS, "m.start_tut_failed");
            psvc.playTutorial(_ctx.getClient(), action, rl);
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
