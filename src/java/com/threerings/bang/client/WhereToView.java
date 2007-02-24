//
// $Id$

package com.threerings.bang.client;

import java.util.ArrayList;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
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
import com.threerings.bang.client.bui.SteelWindow;
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
public class WhereToView extends SteelWindow
    implements ActionListener
{
    /** The width to hint when laying out this window. */
    public static final int WIDTH_HINT = 900;

    public WhereToView (BangContext ctx, boolean postGame)
    {
        super(ctx, ctx.xlate(BangCodes.BANG_MSGS, "m.whereto_title"));
        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.CENTER,
                                              GroupLayout.CONSTRAIN).setGap(25));

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        PlayerObject self = _ctx.getUserObject();

        _contents.setStyleClass("padded");
        BContainer horiz = new BContainer(GroupLayout.makeHStretch().setGap(25));
        _contents.add(horiz);

        BImage underline = _ctx.loadImage("ui/window/underline.png");
        BContainer tutcol = new BContainer(
                GroupLayout.makeVert(GroupLayout.TOP).setGap(5).setOffAxisPolicy(GroupLayout.STRETCH));
        tutcol.add(new BLabel(_msgs.get("m.whereto_tuts"), "where_title"));
        tutcol.add(new BLabel(new ImageIcon(underline)));
        tutcol.add(new BLabel(_msgs.get("m.whereto_intro"), "where_info"));

        boolean enabled = true;
        int townIdx = BangUtil.getTownIndex(self.townId);
        String[] tutorials = TutorialCodes.TUTORIALS[townIdx];
        BContainer tuts = new BContainer(new TableLayout(3, 5, 5));
        if (townIdx == 0) { // frontier town is special
            enabled = createTutorialButton(tuts, tutorials, 0, enabled, false);
            enabled = createTutorialButton(tuts, tutorials, 1, enabled, false);
            for (int ii = 2; ii < tutorials.length; ii += 2) {
                enabled = createTutorialButton(tuts, tutorials, ii, enabled, true);
            }
        } else {
            for (int ii = 0; ii < tutorials.length; ii += 2) {
                enabled = createTutorialButton(tuts, tutorials, ii, enabled, true);
            }
        }
        tutcol.add(tuts);
        horiz.add(tutcol);

        BContainer bldgs = new BContainer(GroupLayout.makeVStretch().setGap(5));
        for (String ident : BLDGS) {
            if (bldgs.getComponentCount() > 0) {
                bldgs.add(new Spacer(0, 15), GroupLayout.FIXED);
            }
            bldgs.add(new BLabel(_msgs.get("m.bldg_" + ident), "where_title"), GroupLayout.FIXED);
            bldgs.add(new BLabel(new ImageIcon(underline)), GroupLayout.FIXED);
            BContainer brow = new BContainer(GroupLayout.makeHStretch().setGap(10));
            String spath = "ui/" + ident + "/" + _ctx.getUserObject().townId + "/shop.png";
            brow.add(new BLabel(new ImageIcon(_ctx.loadImage(spath))), GroupLayout.FIXED);
            BContainer box = new BContainer(GroupLayout.makeVStretch());
            box.add(new BLabel(_msgs.get("m.bldg_info_" + ident), "where_info"));
            brow.add(box);
            bldgs.add(brow);
            BButton go = new BButton(_msgs.get("m.bldg_go"), this, "to_" + ident);
            // TEMP: disable sheriff's office in ITP
            if (BangCodes.INDIAN_POST.equals(self.townId) && ident.equals("office")) {
                go.setText(_msgs.get("m.bldg_soon"));
                go.setEnabled(false);
            }
            // END TEMP
            go.setStyleClass("alt_button");
            BContainer butrow = GroupLayout.makeHBox(GroupLayout.CENTER);
            butrow.add(go);
            bldgs.add(butrow, GroupLayout.FIXED);
        }
        horiz.add(bldgs);

        if (!postGame && BangPrefs.shouldShowWhereTo(_ctx.getUserObject())) {
            BContainer row = GroupLayout.makeHBox(GroupLayout.CENTER);
            ((GroupLayout)row.getLayoutManager()).setGap(25);
            row.add(_nowhere = new BCheckBox(_msgs.get("m.no_whereto")));
            _contents.add(row);
        }

        String dmsg = (postGame || _ctx.getBangClient().isShowingTownView()) ?
            "m.to_town" : "m.dismiss";
        _buttons.add(new BButton(_msgs.get(dmsg), this, postGame ? "to_town" : "dismiss"));
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);

        } else if (action.startsWith("to_")) {
            _ctx.getBangClient().clearPopup(this, true);

            BangBootstrapData bbd = (BangBootstrapData)_ctx.getClient().getBootstrapData();
            if (action.equals("to_office")) {
                _ctx.getLocationDirector().moveTo(bbd.officeOid);
            } else if (action.equals("to_saloon")) {
                _ctx.getLocationDirector().moveTo(bbd.saloonOid);
            } else if (action.equals("to_town")) {
                _ctx.getLocationDirector().leavePlace();
                _ctx.getBangClient().showTownView();
            }

        } else {
            PlayerService psvc = (PlayerService)
                _ctx.getClient().requireService(PlayerService.class);
            ReportingListener rl = new ReportingListener(
                _ctx, BangCodes.BANG_MSGS, "m.start_tut_failed") {
                public void requestFailed (String cause) {
                    super.requestFailed(cause);
                    enableButtons(true);
                }
            };
            psvc.playTutorial(_ctx.getClient(), action, rl);
            enableButtons(false);
        }
    }

    /**
     * Helper function to turn on/off tutorial buttons.
     */
    protected void enableButtons (boolean enabled)
    {
        for (BButton button : _enabledButtons) {
            button.setEnabled(enabled);
        }
    }

    @Override // from BContainer
    protected void wasRemoved ()
    {
        super.wasRemoved();

        if (_nowhere != null && _nowhere.isSelected()) {
            BangPrefs.setNoWhereTo(_ctx.getUserObject());
            _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, "m.whereto_byebye");
        }
        _ctx.getBangClient().checkShowIntro(true);
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
        tlabel.setEnabled(enabled);
        box.add(tlabel);

        BButton play = new BButton(_msgs.get(btext), this, tid);
        play.setStyleClass("alt_button");
        box.add(play);
        _enabledButtons.add(play);

        if (unplayed) {
            // practice and labels after the first unplayed tutorial is greyed out
            enabled = false;
        }

        if (plusPractice) {
            BButton practice = new BButton(_msgs.get("m.tut_practice"), this, tutorials[idx+1]);
            practice.setStyleClass("alt_button");
            practice.setEnabled(!unplayed);
            if (!unplayed) {
                _enabledButtons.add(practice);
            }
            box.add(practice);
        } else {
            box.add(new BLabel(""));
        }

        return enabled;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BCheckBox _nowhere;
    protected ArrayList<BButton> _enabledButtons = new ArrayList<BButton>();

    protected static final String[] BLDGS = { "office", "saloon" };
}
