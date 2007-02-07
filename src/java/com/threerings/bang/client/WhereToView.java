//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
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
 * Displays a list of completed and uncompleted tutorials and allows the user to (re)play them
 * along with pointers toward the Saloon and Sheriff's Office.
 */
public class WhereToView extends BDecoratedWindow
    implements ActionListener
{
    /**
     * Creates the view.
     */
    public WhereToView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        setStyleClass("dialog_window");
        setLayoutManager(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.CENTER,
                                              GroupLayout.CONSTRAIN));
        ((GroupLayout)getLayoutManager()).setGap(25);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        PlayerObject self = _ctx.getUserObject();

        add(new BLabel(_msgs.get("m.whereto_title"), "window_title"));
        add(new BLabel(_msgs.get("m.whereto_intro"), "where_info"));

        BContainer table = new BContainer(new TableLayout(3, 0, 25));
        add(table);

        boolean enabled = true;
        int townIdx = BangUtil.getTownIndex(self.townId);
        String[] tutorials = TutorialCodes.TUTORIALS[townIdx];
        if (townIdx == 0) { // frontier town is special
            BContainer box = new BContainer(new TableLayout(2, 5, 5));
            enabled = createTutorialButton(box, tutorials, 0, enabled, false);
            enabled = createTutorialButton(box, tutorials, 1, enabled, false);
            table.add(box);

            box = new BContainer(new TableLayout(3, 5, 5));
            enabled = createTutorialButton(box, tutorials, 2, enabled, true);
            enabled = createTutorialButton(box, tutorials, 4, enabled, true);
            table.add(box);

            box = new BContainer(new TableLayout(3, 5, 5));
            enabled = createTutorialButton(box, tutorials, 6, enabled, true);
            enabled = createTutorialButton(box, tutorials, 8, enabled, true);
            table.add(box);

        } else {
            for (int ii = 0; ii < tutorials.length; ii += 2) {
                BContainer box = new BContainer(new TableLayout(3, 5, 5));
                enabled = createTutorialButton(box, tutorials, ii, enabled, true);
                table.add(box);
            }
        }

        BContainer bldgs = new BContainer(GroupLayout.makeHStretch().setGap(10));
        for (String ident : BLDGS) {
            if (bldgs.getComponentCount() > 0) {
                bldgs.add(new Spacer(5, 0), GroupLayout.FIXED);
            }
            String spath = "ui/" + ident + "/" + _ctx.getUserObject().townId + "/shop.png";
            bldgs.add(new BLabel(new ImageIcon(_ctx.loadImage(spath))), GroupLayout.FIXED);
            BContainer box = new BContainer(GroupLayout.makeVStretch());
            box.add(new BLabel(_msgs.get("m.bldg_" + ident), "where_title"), GroupLayout.FIXED);
            box.add(new BLabel(_msgs.get("m.bldg_info_" + ident), "where_info"));
            BButton go = new BButton(_msgs.get("m.bldg_go"), this, "to_" + ident);
            go.setStyleClass("alt_button");
            BContainer brow = GroupLayout.makeHBox(GroupLayout.CENTER);
            brow.add(go);
            box.add(brow, GroupLayout.FIXED);
            bldgs.add(box);
        }
        add(bldgs);

        add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
    }

    @Override // from BContainer
    public void validate ()
    {
        pack();
        super.validate();
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
            _ctx.getBangClient().checkShowIntro(true);

        } else if (action.startsWith("to_")) {
            _ctx.getBangClient().clearPopup(this, true);
            BangBootstrapData bbd = (BangBootstrapData)_ctx.getClient().getBootstrapData();
            if (action.equals("to_office")) {
                _ctx.getLocationDirector().moveTo(bbd.officeOid);
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

    protected boolean createTutorialButton (BContainer box, String[] tutorials, int idx,
                                            boolean enabled, boolean plusPractice)
    {
        String tid = tutorials[idx];
        BIcon icon;
        String btext;
        boolean unplayed = false;
        if (_ctx.getUserObject().stats.containsValue(Stat.Type.TUTORIALS_COMPLETED, tid)) {
            icon = BangUI.completed;
            btext = "m.tut_replay";
        } else {
            icon = BangUI.incomplete;
            btext = "m.tut_play";
            unplayed = true;
        }

        BLabel tlabel = new BLabel(_msgs.get("m.tut_" + tid), "tutorial_text");
        tlabel.setIcon(icon);
        box.add(tlabel);

        BButton play = new BButton(_msgs.get(btext), this, tid);
        play.setStyleClass("alt_button");
        box.add(play);

        if (plusPractice) {
            BButton practice = new BButton(_msgs.get("m.tut_practice"), this, tutorials[idx+1]);
            practice.setStyleClass("alt_button");
            box.add(practice);
        }

        if (unplayed) {
            tlabel.setEnabled(enabled);
            return false; // everything after the first unplayed tutorial is greyed out
        }

        return true;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected static final String[] BLDGS = { "office", "saloon" };
}
