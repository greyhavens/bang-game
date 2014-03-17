//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.ServiceButton;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.ranch.data.RanchCodes;
import com.threerings.bang.ranch.data.RanchObject;

/**
 * Allows a Big Shot to be named and recruited.
 */
public class RecruitDialog extends BDecoratedWindow
    implements ActionListener
{
    public RecruitDialog (BangContext ctx, RanchView view, RanchObject ranchobj, UnitConfig config)
    {
        super(ctx.getStyleSheet(), null);
        setLayoutManager(new BorderLayout(5, 5));
        setModal(true);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(RanchCodes.RANCH_MSGS);
        _view = view;
        _ranchobj = ranchobj;
        _config = config;

        add(new UnitIcon(ctx, config), BorderLayout.WEST);
        BContainer cont = new BContainer(GroupLayout.makeVStretch());
        cont.add(new BLabel(_msgs.get("m.rd_intro"), "ranch_recruit"),
                 GroupLayout.FIXED);

        BContainer row = new BContainer(GroupLayout.makeHStretch());
        row.add(new BLabel(_msgs.get("m.rd_name")), GroupLayout.FIXED);
        row.add(_name = new BTextField(
                    config.pickRandomName(), BigShotItem.MAX_NAME_LENGTH));
        row.add(BangUI.createDiceButton(this, "random"), GroupLayout.FIXED);
        cont.add(row, GroupLayout.FIXED);

        cont.add(_status = new StatusLabel(ctx), GroupLayout.FIXED);
        cont.add(new Spacer(1, 1));

        row = new BContainer(GroupLayout.makeHStretch());
        MoneyLabel cost = new MoneyLabel(ctx);
        cost.setMoney(config.scripCost, config.getCoinCost(_ctx.getUserObject()), false);
        row.add(cost, GroupLayout.FIXED);
        row.add(new Spacer(1, 1));
        row.add(new BButton(_msgs.get("m.cancel")).
                addListener(_ctx.getBangClient().makePopupClearer(this, true)), GroupLayout.FIXED);
        _recruit = new ServiceButton(_ctx, _msgs.get("m.recruit"), RanchCodes.RANCH_MSGS, _status) {
            protected boolean callService () {
                _ranchobj.service.recruitBigShot(
                    _config.type, new Name(_name.getText()), createResultListener());
                return true;
            }
            protected boolean onSuccess (Object result) {
                BigShotItem unit = (BigShotItem)result;
                _view.unitRecruited(unit.getItemId());
                _ctx.getBangClient().clearPopup(RecruitDialog.this, true);
                return false;
            }
        };
        row.add(_recruit, GroupLayout.FIXED);
        _recruit.setEnabled(false);
        cont.add(row, GroupLayout.FIXED);

        add(cont, BorderLayout.CENTER);

        // set up a validator for the big shot name
        new EnablingValidator(_name, _recruit) {
            protected boolean checkEnabled (String text) {
                if (text.length() > BigShotItem.MAX_NAME_LENGTH) {
                    _status.setText(_msgs.get("m.name_to_long"));
                    return false;
                } else {
                    _status.setText("");
                    return super.checkEnabled(text);
                }
            }
        };
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("random".equals(event.getAction())) {
            _name.setText(_config.pickRandomName());

        } else if ("cancel".equals(event.getAction())) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();
        _name.requestFocus();
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected RanchView _view;
    protected RanchObject _ranchobj;
    protected UnitConfig _config;

    protected StatusLabel _status;
    protected BTextField _name;
    protected BButton _recruit;
}
