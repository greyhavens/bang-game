//
// $Id$

package com.threerings.bang.avatar.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Point;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.Look;

/**
 * Displays an interface for configuring poses and purchasing a name change.
 */
public class EditCharacterView extends BContainer
{
    public EditCharacterView (BangContext ctx, StatusLabel status)
    {
        super(new AbsoluteLayout());

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(BarberCodes.BARBER_MSGS);
        _status = status;

        // add a display of our current look
        add(new PickLookView(ctx, true), new Point(707, 135));

        // everything else needs to go in the main content area
        BContainer contents = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                GroupLayout.STRETCH));
        contents.setStyleClass("barber_char_content");
        add(contents, WearClothingView.CONTENT_RECT);

        // create the UI for configuring our poses
        contents.add(createHeader("poses"));

        PlayerObject user = ctx.getUserObject();
        BContainer poses = new BContainer(new TableLayout(2, 5, 5));
        for (Look.Pose pose : Look.Pose.values()) {
            String pname = pose.toString().toLowerCase();
            poses.add(new BLabel(_msgs.get("m.pose_" + pname), "right_label"));
            final LookComboBox looks = new LookComboBox(ctx);
            looks.selectLook(user.getLook(pose));
            final Look.Pose fpose = pose;
            looks.addListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    configureLook(fpose, looks.getSelectedLook());
                }
            });
            poses.add(looks);
        }
        contents.add(poses);

        contents.add(new Spacer(25, 25));

        // create the UI for changing our handle
        contents.add(createHeader("handle"));

        BContainer ncont = GroupLayout.makeHBox(GroupLayout.LEFT);
        ncont.add(new BLabel(_msgs.get("m.handle")));
        ncont.add(_handle = new BTextField(
                      _ctx.getUserObject().handle.toString()));
        _handle.setPreferredWidth(150);
        ncont.add(new BLabel(_msgs.get("m.handle_cost"), "barber_char_cost"));
        MoneyLabel cost = new MoneyLabel(ctx);
        cost.setMoney(1000, 1, false);
        cost.setStyleClass("m.barber_char_cost");
        ncont.add(cost);
        ncont.add(new BButton(_msgs.get("m.buy_handle"), "buy_handle"));

        contents.add(ncont);
    }

    protected BContainer createHeader (String type)
    {
        BContainer row = new BContainer(GroupLayout.makeHStretch());
        row.add(new BLabel(_msgs.get("m." + type + "_header"),
                    "barber_char_header"), GroupLayout.FIXED);
        row.add(new BLabel(_msgs.get("m." + type + "_tip"), "barber_char_tip"));
        return row;
    }

    protected void configureLook (Look.Pose pose, Look look)
    {
        AvatarService asvc = (AvatarService)
            _ctx.getClient().requireService(AvatarService.class);
        asvc.selectLook(_ctx.getClient(), pose, look.name);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected StatusLabel _status;

    protected BTextField _handle;
}
