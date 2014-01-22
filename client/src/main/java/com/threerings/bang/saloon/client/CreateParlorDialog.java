//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.ServiceButton;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;

/**
 * Displays configuration parameters for creating a Back Parlor.
 */
public class CreateParlorDialog extends BDecoratedWindow
{
    public CreateParlorDialog (BangContext ctx, SaloonObject salobj)
    {
        super(ctx.getStyleSheet(), ctx.xlate(SaloonCodes.SALOON_MSGS, "m.create_title"));
        setModal(true);

        _ctx = ctx;
        _salobj = salobj;
        _msgs = ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

        BContainer params = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        add(params);

        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        BLabel label;
        row.add(label = new BLabel(_msgs.get("m.parlor_type")));
        label.setTooltipText(_msgs.get("m.parlor_type_tip"));
        row.add(_type = new BComboBox(ParlorConfigView.getParlorTypes(ctx, true)));
        _type.selectItem(0);
        _type.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                ParlorInfo.Type type = (ParlorInfo.Type)_type.getSelectedValue();
                _password.setEnabled(type == ParlorInfo.Type.PASSWORD);
                _matched.setSelected(type == ParlorInfo.Type.SOCIAL);
            }
        });
        _type.setTooltipText(_msgs.get("m.parlor_type_tip"));
        params.add(row);

        row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(label = new BLabel(_msgs.get("m.use_password")));
        label.setTooltipText(_msgs.get("m.parlor_type_tip"));
        row.add(_password = new BTextField(50));
        _password.setPreferredWidth(75);
        _password.setEnabled(false);
        params.add(row);

        row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(label = new BLabel(_msgs.get("m.use_matched")));
        label.setTooltipText(_msgs.get("m.parlor_matched_tip"));
        row.add(_matched = new BCheckBox(null));
        _matched.setSelected(true);
        params.add(row);

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(new ServiceButton(_ctx, _msgs.get("m.create"),
                                      SaloonCodes.SALOON_MSGS, "m.create_parlor_failed") {
            protected boolean callService () {
                ParlorInfo.Type type = (ParlorInfo.Type)_type.getSelectedValue();
                String passwd = type == ParlorInfo.Type.PASSWORD ? _password.getText() : null;
                _salobj.service.createParlor(
                    type, passwd, _matched.isSelected(), createResultListener());
                return true;
            }
            protected boolean onSuccess (Object result) {
                // move immediately to our new parlor
                _ctx.getLocationDirector().moveTo((Integer)result);
                return false;
            }
        });
        buttons.add(new BButton(_msgs.get("m.cancel")).
                    addListener(_ctx.getBangClient().makePopupClearer(this, true)));
        add(buttons, GroupLayout.FIXED);
    }

    protected BangContext _ctx;
    protected SaloonObject _salobj;
    protected MessageBundle _msgs;

    protected BComboBox _type;
    protected BTextField _password;
    protected BCheckBox _matched;
}
