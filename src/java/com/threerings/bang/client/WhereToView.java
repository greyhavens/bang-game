//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.TutorialCodes;

import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Shop;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;

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

        setModal(true);
        _contents.setStyleClass("padded");
        BContainer horiz = new BContainer(GroupLayout.makeHStretch().setGap(25));
        _contents.add(new Spacer(0, -10));
        _contents.add(horiz);

        int townIdx = BangUtil.getTownIndex(self.townId);
        BContainer tutcol = new BContainer(GroupLayout.makeVert(
                    GroupLayout.TOP).setGap(5).setOffAxisJustification(GroupLayout.CENTER));
        BLabel tutbubble = new BLabel(_msgs.get("m.whereto_tuts." + self.townId), "where_bubble");
        tutbubble.setPreferredSize(new Dimension(300, -1));
        tutcol.add(new Spacer(0, 15));
        tutcol.add(tutbubble);
        tutcol.add(new Spacer(0, -30));
        tutcol.add(new BLabel(new ImageIcon(_ctx.loadImage(TutorialCodes.TUTORIAL_UNIT[townIdx]))));
        tutcol.add(new BButton(_msgs.get("m.tut_view"), this, "tutorials"));
        horiz.add(tutcol, GroupLayout.FIXED);

        BContainer bldgs = new BContainer(GroupLayout.makeVStretch().setGap(5));
        for (String ident : BLDGS) {
            bldgs.add(new BLabel(_msgs.get("m.bldg_" + ident), "where_title"), GroupLayout.FIXED);
            BContainer brow = new BContainer(GroupLayout.makeHStretch().setGap(10));
            BContainer shop = GroupLayout.makeHBox(GroupLayout.LEFT);
            shop.setStyleClass("where_shop");
            String spath = "ui/" + ident + "/" + _ctx.getUserObject().townId + "/shop.png";
            shop.add(new BLabel(new ImageIcon(_ctx.loadImage(spath))));
            brow.add(shop, GroupLayout.FIXED);
            BContainer box = new BContainer(GroupLayout.makeVStretch());
            box.add(new BLabel(_msgs.get("m.bldg_info_" + ident), "where_info"));
            brow.add(box);
            bldgs.add(brow);
            BButton go = new BButton(_msgs.get("m.bldg_go_" + ident), this, "to_" + ident);
            go.setStyleClass("big_button");
            go.setPreferredSize(new Dimension(140, -1));
            BContainer butrow = GroupLayout.makeHBox(GroupLayout.RIGHT);
            butrow.add(go);
            bldgs.add(butrow, GroupLayout.FIXED);
        }
        horiz.add(bldgs);
        _contents.add(new Spacer(0, 1));

        if (!postGame && BangPrefs.shouldShowWhereTo(_ctx.getUserObject()) &&
            !_ctx.getUserObject().tokens.isDemo()) {
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
        _ctx.getBangClient().clearPopup(this, true);
        if (action.startsWith("to_")) {
            if (action.equals("to_office")) {
                _ctx.getBangClient().goTo(Shop.OFFICE);
            } else if (action.equals("to_saloon")) {
                _ctx.getBangClient().goTo(Shop.SALOON);
            } else if (action.equals("to_town")) {
                _ctx.getBangClient().showTownView();
            }

        } else if (action.startsWith("tutorials")) {
            _ctx.getBangClient().displayPopup(
                new TutorialView(_ctx), true, TutorialView.WIDTH_HINT);
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
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BCheckBox _nowhere;
    protected BTextField _customTut;

    protected static final String[] BLDGS = { "office", "saloon" };
}
