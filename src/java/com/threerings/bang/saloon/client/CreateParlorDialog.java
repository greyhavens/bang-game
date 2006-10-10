//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;

/**
 * Displays configuration parameters for creating a Back Parlor.
 */
public class CreateParlorDialog extends BDecoratedWindow
    implements ActionListener
{
    public CreateParlorDialog (BangContext ctx, SaloonObject salobj)
    {
        super(ctx.getStyleSheet(),
              ctx.xlate(SaloonCodes.SALOON_MSGS, "m.create_title"));
        setModal(true);

        _ctx = ctx;
        _salobj = salobj;
        _msgs = ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

        BContainer params = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                 GroupLayout.STRETCH));
        add(params);

        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(_pardsOnly = new BCheckBox(_msgs.get("m.pards_only")));
        _pardsOnly.setTooltipText(_msgs.get("m.pards_only_tip"));
        params.add(row);

        row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(_usePass = new BCheckBox(_msgs.get("m.use_password")));
        _usePass.setTooltipText(_msgs.get("m.use_password_tip"));
        row.add(_password = new BTextField(50));
        _password.setPreferredWidth(75);
        params.add(row);

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(new BButton(_msgs.get("m.create"), this, "create"));
        buttons.add(new BButton(_msgs.get("m.cancel"), this, "cancel"));
        add(buttons, GroupLayout.FIXED);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if ("create".equals(action)) {
            _salobj.service.createParlor(
                _ctx.getClient(), _pardsOnly.isSelected(),
                _usePass.isSelected() ? _password.getText() : null,
                new SaloonService.ResultListener() {
                    public void requestProcessed (Object result) {
                        // move immediately to our new parlor
                        _ctx.getLocationDirector().moveTo((Integer)result);
                    }
                    public void requestFailed (String cause) {
                        _ctx.getChatDirector().displayFeedback(
                            SaloonCodes.SALOON_MSGS,
                            MessageBundle.compose(
                                "m.create_parlor_failed", cause));
                    }
                });
            _ctx.getBangClient().clearPopup(this, true);

        } else if ("cancel".equals(action)) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    protected BangContext _ctx;
    protected SaloonObject _salobj;
    protected MessageBundle _msgs;

    protected BCheckBox _pardsOnly, _usePass;
    protected BTextField _password;
}
