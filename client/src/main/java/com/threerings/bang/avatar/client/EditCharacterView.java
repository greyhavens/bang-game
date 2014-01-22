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
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberObject;
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

        // create the UI for configuring our poses
        add(new BLabel(_msgs.get("m.poses_tip"), "barber_char_tip"), POSES_TIP);

        PlayerObject user = ctx.getUserObject();
        BContainer poses = new BContainer(new TableLayout(2, 5, 5));
        for (Look.Pose pose : Look.Pose.values()) {
            String pname = StringUtil.toUSLowerCase(pose.toString());
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
        add(poses, POSES_RECT);

        // create the UI for changing our handle
        add(new BLabel(_msgs.get("m.handle_tip"), "barber_char_tip"),
            HANDLE_TIP);

        BContainer ncont = GroupLayout.makeHBox(GroupLayout.LEFT);
        ncont.add(new BLabel(_msgs.get("m.handle")));
        ncont.add(_handle = new BTextField());
        _handle.setPreferredWidth(150);
        ncont.add(new Spacer(5, 5));
        ncont.add(new BLabel(_msgs.get("m.handle_cost"), "barber_char_cost"));
        MoneyLabel cost = new MoneyLabel(ctx);
        cost.setMoney(BarberCodes.HANDLE_CHANGE_SCRIP_COST,
            BarberCodes.HANDLE_CHANGE_COIN_COST, false);
        cost.setStyleClass("m.barber_char_cost");
        ncont.add(cost);
        ncont.add(new Spacer(5, 5));
        ncont.add(_buy = new BButton(_msgs.get("m.buy_handle"), "buy_handle"));
        _buy.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                changeHandle(new Handle(_handle.getText()));
            }
        });
        _buy.setEnabled(false);
        add(ncont, HANDLE_RECT);

        // configure our handle text field with standard validators
        _handle.setDocument(new CreateAvatarView.HandleDocument());
        _handle.addListener(
            new CreateAvatarView.HandleListener(_buy, _status, "", _msgs.get("m.invalid_handle")) {
            public void textChanged (TextEvent event) {
                super.textChanged(event);
                if (_handle.getText().equals(
                        _ctx.getUserObject().handle.toString())) {
                    _buy.setEnabled(false);
                }
            }
        });
        _handle.setText(_ctx.getUserObject().handle.toString());
    }

    /**
     * Called by the {@link BarberView} to give us a reference to our barber
     * object when needed.
     */
    public void setBarberObject (BarberObject barbobj)
    {
        _barbobj = barbobj;
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
        AvatarService asvc = _ctx.getClient().requireService(AvatarService.class);
        asvc.selectLook(pose, look.name);
    }

    protected void changeHandle (Handle handle)
    {
        _buy.setEnabled(false);

        BarberService.ConfirmListener cl = new BarberService.ConfirmListener() {
            public void requestProcessed () {
                _status.setText(_msgs.get("m.handle_changed"));
            }
            public void requestFailed (String reason) {
                _status.setText(_msgs.xlate(reason));
                _buy.setEnabled(true);
            }
        };
        _barbobj.service.changeHandle(handle, cl);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected StatusLabel _status;
    protected BarberObject _barbobj;

    protected BTextField _handle;
    protected BButton _buy;

    protected static final Point POSES_TIP = new Point(170, 450);
    protected static final Rectangle POSES_RECT =
        new Rectangle(100, 250, 500, 180);

    protected static final Point HANDLE_TIP = new Point(200, 220);
    protected static final Rectangle HANDLE_RECT =
        new Rectangle(100, 160, 500, 40);

    protected static final Point DISCARDS_TIP = new Point(250, 75);
    protected static final Rectangle DISCARDS_RECT =
        new Rectangle(100, 50, 200, 100);
}
