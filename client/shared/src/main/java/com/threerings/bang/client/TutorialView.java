//
// $Id$

package com.threerings.bang.client;

import java.util.ArrayList;

import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Rectangle;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.util.ReportingListener;
import com.threerings.bang.client.bui.SteelWindow;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.data.TutorialCodes;

/**
 * A list of available tutorials.
 */
public class TutorialView extends SteelWindow
    implements ActionListener
{
    /** The width to hint when laying out this window. */
    public static final int WIDTH_HINT = 875;

    public static class UnitView extends BComponent
    {
        public UnitView (BasicContext ctx, String unit)
        {
            _images = new BImage[] {
                ctx.getImageCache().getBImage("ui/office/outlaw_bg.png"),
                ctx.getImageCache().getBImage("ui/office/frame.png"),
                ctx.getImageCache().getBImage(unit),
            };
        }

        @Override // from BComponent
        protected Dimension computePreferredSize (int whint, int hhint)
        {
            return new Dimension(_images[FRAME].getWidth(), _images[FRAME].getHeight());
        }

        @Override // from BComponent
        protected void wasAdded ()
        {
            for (BImage image : _images) {
                image.reference();
            }
        }

        @Override // from BComponent
        protected void wasRemoved ()
        {
            for (BImage image : _images) {
                image.release();
            }
        }

        @Override // from BComponent
        protected void renderComponent (Renderer renderer)
        {
            super.renderComponent(renderer);
            _images[BACKGROUND].render(renderer, 0, 0, _alpha);
            int frame = 3;
            int bheight = _images[BACKGROUND].getHeight(), bwidth = _images[BACKGROUND].getWidth();
            int aheight = _images[AVATAR].getHeight(), awidth = _images[AVATAR].getWidth();
            int xoff = (awidth - bwidth) / 2 + frame, yoff = (aheight - bheight) / 2 + frame;
            _images[AVATAR].render(renderer, xoff, yoff, bwidth - frame * 2, bheight - frame * 2,
                    frame, frame, _alpha);
            _images[FRAME].render(renderer, 0, 0, _alpha);
        }

        protected BImage[] _images;

        protected static final int BACKGROUND = 0;
        protected static final int FRAME = 1;
        protected static final int AVATAR = 2;
    }

    public TutorialView (BangContext ctx)
    {
        super(ctx, ctx.xlate(BangCodes.BANG_MSGS, "m.tutorials"));

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        setModal(true);
        _contents.setLayoutManager(new BorderLayout());
        PlayerObject self = _ctx.getUserObject();
        _townIdx = BangUtil.getTownIndex(self.townId);

        final String firstTutId = TutorialCodes.NEW_TUTORIALS[_townIdx][0];
        if (!self.stats.containsValue(StatType.TUTORIALS_COMPLETED, firstTutId)) {
            showTutorialIntro();

        } else {
            showTutorialList();
            // only show the tutorial tip disabler after you've done the very first tutorial, and
            // don't allow demo accounts to disable the tutorial popup
            if (BangPrefs.shouldShowTutIntro(self) && !_ctx.getUserObject().tokens.isDemo()) {
                BContainer checkbox = GroupLayout.makeHBox(GroupLayout.CENTER);
                checkbox.add(_notuts = new BCheckBox(_msgs.get("m.no_tuts")));
                _contents.add(checkbox, BorderLayout.SOUTH);
            }
        }
    }

    public void showTutorialList ()
    {
        PlayerObject self = _ctx.getUserObject();

        ((BorderLayout)_contents.getLayoutManager()).setGaps(50, 20);
        _contents.add(new Spacer(1, 1), BorderLayout.EAST);
        _contents.add(new Spacer(1, 1), BorderLayout.WEST);

        BContainer center = new BContainer(GroupLayout.makeHoriz(
                    GroupLayout.CENTER).setOffAxisJustification(GroupLayout.TOP).setGap(30));
        center.add(new UnitView(_ctx, TutorialCodes.TUTORIAL_UNIT[_townIdx]));
        center.setStyleClass("padded");
        BContainer right = new BContainer(GroupLayout.makeVert(
                    GroupLayout.TOP).setOffAxisJustification(GroupLayout.CENTER).setGap(20));
        right.setPreferredSize(new Dimension(400, -1));
        center.add(right);
        BLabel desc = new BLabel("", "tview_desc");
        right.add(desc);
        BContainer list = new BContainer(new TableLayout(3, 5, 10));
        right.add(list);

        String[] tutorials = TutorialCodes.NEW_TUTORIALS[_townIdx];
        String currentTut = null;

        for (String tid : tutorials) {
            BIcon icon;
            String btext, bstyle;
            if (self.stats.containsValue(StatType.TUTORIALS_COMPLETED, tid)) {
                icon = BangUI.completed;
                btext = (tid.startsWith(TutorialCodes.PRACTICE_PREFIX) ?
                        "m.tut_practice" : "m.tut_replay");
                bstyle = "alt_button";
            } else {
                icon = new ImageIcon(_ctx.loadImage("ui/tutorials/star_small.png"));
                btext = "m.tut_start";
                bstyle = "big_button";
            }
            list.add(new BLabel(icon));
            list.add(new BLabel(_msgs.get("m.tut_" + tid), "tview_list"));
            BButton play = new BButton(_msgs.get(btext), this, tid);
            play.setStyleClass(bstyle);
            list.add(play);
            _enabledButtons.add(play);
            currentTut = tid;
            if (icon != BangUI.completed && !self.tokens.isAdmin()) {
                break;
            } else {
                currentTut = self.townId;
            }
        }

        desc.setText(_msgs.get("m.tut_" + currentTut + ".desc"));

        _contents.add(center, BorderLayout.CENTER);
        if (currentTut != self.townId) {
            currentTut = _msgs.get("m.tut_title", _msgs.get("m.tut_" + currentTut));
        } else {
            currentTut = _msgs.get("m.tut_" + currentTut);
        }
        _contents.add(new BLabel(currentTut, "tview_title"), BorderLayout.NORTH);

        _buttons.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
    }

    public void showTutorialIntro ()
    {
        _main = new BContainer(new AbsoluteLayout());
        _main.setPreferredSize(new Dimension(775, 460));
        _contents.add(_main, BorderLayout.CENTER);

        BContainer cont = new BContainer(GroupLayout.makeHoriz(
                    GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.CONSTRAIN));
        BImage star = _ctx.loadImage("ui/tutorials/star_big.png");
        cont.add(new BLabel(new ImageIcon(star)), GroupLayout.FIXED);
        BButton start = new BButton(_msgs.get("m.start_tutorials"), this,
                    TutorialCodes.NEW_TUTORIALS[_townIdx][0]);
        start.setStyleClass("big_button");
        cont.add(start);
        _enabledButtons.add(start);
        cont.add(new BLabel(new ImageIcon(star)), GroupLayout.FIXED);
        _main.add(cont, new Rectangle(410, 90, 300, 60));

        _buttons.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();

        if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);

        } else if (action.equals("to_town")) {
            _ctx.getBangClient().clearPopup(this, true);
            _ctx.getLocationDirector().leavePlace();
            _ctx.getBangClient().showTownView();

        } else {
            PlayerService psvc = _ctx.getClient().requireService(PlayerService.class);
            ReportingListener rl = new ReportingListener(
                _ctx, BangCodes.BANG_MSGS, "m.start_tut_failed") {
                public void requestFailed (String cause) {
                    super.requestFailed(cause);
                    enableButtons(true);
                }
            };
            psvc.playTutorial(action, rl);
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

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        if (_main != null) {
            _main.setBackground(BComponent.DEFAULT, new ImageBackground(ImageBackground.CENTER_XY,
                _ctx.loadImage("ui/tutorials/tutorials_" + _ctx.getUserObject().townId + ".jpg")));
        }
    }

    @Override // from BContainer
    protected void wasRemoved ()
    {
        super.wasRemoved();
        if (_notuts != null && _notuts.isSelected()) {
            BangPrefs.setNoTutIntro(_ctx.getUserObject());
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected BContainer _main;
    protected BCheckBox _notuts;
    protected ArrayList<BButton> _enabledButtons = new ArrayList<BButton>();
    protected int _townIdx;
}
