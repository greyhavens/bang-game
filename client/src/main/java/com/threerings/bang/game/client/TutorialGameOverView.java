//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.TutorialView;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.client.util.ReportingListener;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.avatar.client.CreateAvatarView;

import com.threerings.bang.game.data.Award;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.TutorialCodes;

/**
 * Displays the results of a Tutorial.
 */
public class TutorialGameOverView extends SteelWindow
    implements ActionListener
{
    public TutorialGameOverView (BasicContext ctx, String tutIdent, BangConfig gconfig,
            BangObject bangobj, PlayerObject user)
    {
        super(ctx, ctx.xlate(BangCodes.BANG_MSGS, "m.tut_complete"));

        _ctx = ctx;
        if (_ctx instanceof BangContext) {
            _bctx = (BangContext)ctx;
        }
        MessageBundle msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        int townIdx = BangUtil.getTownIndex(user.townId);

        int pidx = bangobj.getPlayerIndex(user.getVisibleName());
        // locate our award
        Award award = null;
        for (Award a : bangobj.awards) {
            if (pidx == a.pidx) {
                award = a;
                break;
            }
        }

        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(20));
        _contents.add(new BLabel(
                    msgs.get("m.tut_finished", msgs.get("m.tut_" + tutIdent)), "tview_title"));

        BContainer center = new BContainer(GroupLayout.makeHoriz(
                    GroupLayout.CENTER).setOffAxisJustification(GroupLayout.TOP).setGap(30));
        center.add(new TutorialView.UnitView(_ctx, TutorialCodes.TUTORIAL_UNIT[townIdx]));
        BContainer right = new BContainer(GroupLayout.makeVert(
                    GroupLayout.TOP).setOffAxisJustification(GroupLayout.CENTER).setGap(20));
        right.setPreferredSize(new Dimension(400, -1));
        center.add(right);
        BLabel desc = new BLabel(msgs.get("m.tut_" + tutIdent + ".over", "tview_desc"));
        right.add(desc);
        BContainer list = new BContainer(new TableLayout(3, 5, 10));
        right.add(list);

        String[] tutorials = TutorialCodes.NEW_TUTORIALS[townIdx];
        int tutidx = 0;

        for ( ; tutidx < tutorials.length; tutidx++) {
            if (tutorials[tutidx].equals(tutIdent)) {
                tutidx++;
                break;
            }
        }

        if (tutidx < tutorials.length) {
            String nextTut = tutorials[tutidx];
            list.add(new BLabel(new ImageIcon(ctx.loadImage("ui/tutorials/star_big.png"))));
            list.add(new BLabel(msgs.get("m.tut_" + nextTut), "tview_list"));
            _start = new BButton(msgs.get("m.tut_start"), this, nextTut);
            _start.setStyleClass("big_button");
            list.add(_start);

        } else if (townIdx == 0 && !user.hasCharacter()) {
            list.add(new BLabel(new ImageIcon(ctx.loadImage("ui/tutorials/star_big.png"))));
            list.add(new BLabel(msgs.get("m.tut_choose"), "tview_list"));
            _start = new BButton(msgs.get("m.lets_go"), this, "choose");
            _start.setStyleClass("big_button");
            list.add(_start);
        }

        _contents.add(center);

        if (award != null && award.cashEarned > 0) {
            _contents.add(new Spacer(1, 1));
            _contents.add(new AwardView(_ctx, bangobj, gconfig, user, award, true));
        } else {
            _contents.setStyleClass("padded");
        }

        _buttons.add(new BButton(msgs.get("m.to_town"), this, "to_town"));
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();

        if (action.equals("to_town") || action.equals("choose")) {
            if (action.equals("choose")) {
                _bctx.getBangClient().queueTownNotificaton(new Runnable() {
                    public void run () {
                        CreateAvatarView.show(_bctx);
                    }
                });
            }
            _bctx.getBangClient().showTownView();
            _bctx.getBangClient().clearPopup(this, true);

        } else {
            PlayerService psvc = _bctx.getClient().requireService(PlayerService.class);
            ReportingListener rl = new ReportingListener(
                _bctx, BangCodes.BANG_MSGS, "m.start_tut_failed") {
                public void requestFailed (String cause) {
                    super.requestFailed(cause);
                    _start.setEnabled(true);
                }
            };
            psvc.playTutorial(action, rl);
            _start.setEnabled(false);
        }
    }

    protected BasicContext _ctx;
    protected BangContext _bctx;

    protected BButton _start;
}
