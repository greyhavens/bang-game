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
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.client.util.EscapeListener;
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
    public RecruitDialog (BangContext ctx, RanchView view,
                          RanchObject ranchobj, UnitConfig config)
    {
        super(ctx.getStyleSheet(), null);
        setModal(true);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(RanchCodes.RANCH_MSGS);
        _view = view;
        _ranchobj = ranchobj;
        _config = config;

        addListener(new EscapeListener() {
            public void escapePressed() {
                _ctx.getBangClient().clearPopup();
            }
        });

        add(new UnitIcon(ctx, -1, config), BorderLayout.WEST);
        BContainer cont = new BContainer(GroupLayout.makeVStretch());
        cont.add(new BLabel(_msgs.get("m.rd_intro")), GroupLayout.FIXED);

        BContainer row = new BContainer(GroupLayout.makeHStretch());
        row.add(new BLabel(_msgs.get("m.rd_name")), GroupLayout.FIXED);
        row.add(_name = new BTextField());
        _name.addListener(new TextListener() {
            public void textChanged (TextEvent event) {
                if (_name.getText().length() > BigShotItem.MAX_NAME_LENGTH) {
                    _status.setText(_msgs.get("m.name_to_long"));
                    _recruit.setEnabled(false);
                } else {
                    _status.setText("");
                    _recruit.setEnabled(_name.getText().length() > 0);
                }
            }
        });
        cont.add(row, GroupLayout.FIXED);

        cont.add(_status = new StatusLabel(ctx), GroupLayout.FIXED);
        _status.setStyleClass("shop_status");

        cont.add(new Spacer(1, 1));

        row = new BContainer(GroupLayout.makeHStretch());
        MoneyLabel cost = new MoneyLabel(ctx);
        cost.setMoney(config.scripCost, config.coinCost, false);
        row.add(cost, GroupLayout.FIXED);
        row.add(new Spacer(1, 1));
        row.add(new BButton(_msgs.get("m.cancel"), this, "cancel"),
                GroupLayout.FIXED);
        row.add(_recruit = new BButton(_msgs.get("m.recruit"), this, "recruit"),
                GroupLayout.FIXED);
        _recruit.setEnabled(false);
        cont.add(row, GroupLayout.FIXED);

        add(cont, BorderLayout.CENTER);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("cancel".equals(event.getAction())) {
            _ctx.getBangClient().clearPopup();

        } else if ("recruit".equals(event.getAction())) {
            RanchService.ResultListener rl = new RanchService.ResultListener() {
                public void requestProcessed (Object result) {
                    BigShotItem unit = (BigShotItem)result;
                    _view.unitRecruited(unit.getItemId());
                    _ctx.getBangClient().clearPopup();
                }
                public void requestFailed (String cause) {
                    _status.setStatus(_msgs.xlate(cause), true);
                }
            };
            _ranchobj.service.recruitBigShot(
                _ctx.getClient(), _config.type, new Name(_name.getText()), rl);
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
