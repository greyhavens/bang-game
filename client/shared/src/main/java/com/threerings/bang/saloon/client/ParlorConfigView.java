//
// $Id$

package com.threerings.bang.saloon.client;

import java.util.ArrayList;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;

/**
 * Displays configuration controls for a parlor.
 */
public class ParlorConfigView extends BDecoratedWindow
    implements AttributeChangeListener, ActionListener
{
    public ParlorConfigView (BangContext ctx, ParlorObject parobj)
    {
        super(ctx.getStyleSheet(), ctx.xlate(SaloonCodes.SALOON_MSGS, "m.settings"));

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);
        _parobj = parobj;
        _parobj.addListener(this);
        setModal(true);
        setLayer(BangUI.POPUP_MENU_LAYER);
        BContainer cont = new BContainer(GroupLayout.makeVert(
                    GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.EQUALIZE).setGap(10));

        BContainer bits = new BContainer(new TableLayout(2, 5, 10));
        BLabel label;
        bits.add(label = new BLabel(_msgs.get("m.parlor_type")));
        label.setTooltipText(_msgs.get("m.parlor_type_tip"));
        bits.add(_type = new BComboBox(getParlorTypes(ctx, false)));
        _type.setTooltipText(_msgs.get("m.parlor_type_tip"));
        _type.selectItem(0);
        _type.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                ParlorInfo.Type type = (ParlorInfo.Type)_type.getSelectedValue();
                _changePass.setEnabled(type == ParlorInfo.Type.PASSWORD);
            }
        });
        _type.addListener(_parconf);

        bits.add(new BLabel(_msgs.get("m.use_password")));
        bits.add(_changePass = new BButton(_msgs.get("m.change"), new ChangePasswordHelper(), ""));
        _changePass.setEnabled(false);
        cont.add(bits);

        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(_creator = new BCheckBox(_msgs.get("m.creator_only")));
        _creator.setTooltipText(_msgs.get("m.creator_only_tip"));
        _creator.addListener(_parconf);
        cont.add(row);
        add(cont);

        add(new Spacer(1, 10), GroupLayout.FIXED);

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        bcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);

        updateDisplay();
    }

    public void wasRemoved ()
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

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("dismiss".equals(event.getAction())) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    protected void updateDisplay ()
    {
        _type.selectValue(_parobj.info.type);
        _creator.setSelected(_parobj.onlyCreatorStart);

        boolean amCreator = _ctx.getUserObject().handle.equals(_parobj.info.creator);
        _type.setEnabled(amCreator);
        _changePass.setEnabled(amCreator && _parobj.info.type == ParlorInfo.Type.PASSWORD);
        _creator.setEnabled(amCreator);
    }

    protected void updateConfig ()
    {
        ParlorInfo info = new ParlorInfo();
        info.type = (ParlorInfo.Type)_type.getSelectedValue();
        _parobj.service.updateParlorConfig(info, _creator.isSelected());
    }

    protected static ArrayList<BComboBox.Item> getParlorTypes (BangContext ctx, boolean create)
    {
        ArrayList<BComboBox.Item> types = new ArrayList<BComboBox.Item>();
        for (ParlorInfo.Type type : ParlorInfo.Type.values()) {
            // only gang leaders can create recruiting parlors
            if (type == ParlorInfo.Type.RECRUITING &&
                    (!create || !ctx.getUserObject().canRecruit())) {
                continue;
            }
            String msg = "m.pt_" + StringUtil.toUSLowerCase(type.toString());
            types.add(new BComboBox.Item(type, ctx.xlate(SaloonCodes.SALOON_MSGS, msg)));
        }
        return types;
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
                _parobj.service.updateParlorPassword(((String)result).trim());
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

    protected BComboBox _type;
    protected BCheckBox _pards, _usePass, _creator;
    protected BButton _changePass;
}
