//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.Interval;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;

/**
 * Displays configuration controls for a parlor.
 */
public class ParlorConfigView extends BContainer
    implements AttributeChangeListener
{
    public ParlorConfigView (BangContext ctx)
    {
        super(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.CENTER,
                                   GroupLayout.EQUALIZE));
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(_pards = new BCheckBox(_msgs.get("m.pards_only")));
        _pards.setTooltipText(_msgs.get("m.pards_only_tip"));
        _pards.addListener(_parconf);
        add(row);

        row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(_usePass = new BCheckBox(_msgs.get("m.req_password")));
        _usePass.setTooltipText(_msgs.get("m.use_password_tip"));
        _usePass.addListener(_parconf);
        row.add(_changePass = new BButton(
                    _msgs.get("m.change"), new ChangePasswordHelper(), ""));
        add(row);

        row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(_creator = new BCheckBox(_msgs.get("m.creator_only")));
        _creator.setTooltipText(_msgs.get("m.creator_only_tip"));
        _creator.addListener(_parconf);
        add(row);
    }

    public void willEnterPlace (ParlorObject parobj)
    {
        _parobj = parobj;
        _parobj.addListener(this);
        updateDisplay();
    }

    public void didLeavePlace ()
    {
        if (_parobj != null) {
            _parobj.removeListener(this);
            _parobj = null;
        }
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (ParlorObject.INFO.equals(event.getName()) ||
            ParlorObject.ONLY_CREATOR_START.equals(event.getName())) {
            updateDisplay();
        }
    }

    protected void updateDisplay ()
    {
        _pards.setSelected(_parobj.info.pardnersOnly);
        _usePass.setSelected(_parobj.info.passwordProtected);
        _creator.setSelected(_parobj.onlyCreatorStart);

        boolean amCreator =
            _ctx.getUserObject().handle.equals(_parobj.info.creator);
        _pards.setEnabled(amCreator);
        _usePass.setEnabled(amCreator);
        _changePass.setEnabled(amCreator);
        _creator.setEnabled(amCreator);
    }

    protected void updateConfig ()
    {
        ParlorInfo info = new ParlorInfo();
        info.pardnersOnly = _pards.isSelected();
        info.passwordProtected = _usePass.isSelected();
        _parobj.service.updateParlorConfig(
            _ctx.getClient(), info, _creator.isSelected());
    }

    protected class ChangePasswordHelper
        implements ActionListener, OptionDialog.ResponseReceiver
    {
        public void actionPerformed (ActionEvent event) {
            OptionDialog.showStringDialog(
                _ctx, SaloonCodes.SALOON_MSGS, "m.enter_new_pass",
                new String[] { "m.change", "m.cancel" }, 150, "", this);
        }

        public void resultPosted (int button, Object result) {
            if (button == 0) {
                _parobj.service.updateParlorPassword(
                    _ctx.getClient(), ((String)result).trim());
            }
        }
    }

    protected ActionListener _parconf = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            updateConfig();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected ParlorObject _parobj;

    protected BCheckBox _pards, _usePass, _creator;
    protected BButton _changePass;
}
