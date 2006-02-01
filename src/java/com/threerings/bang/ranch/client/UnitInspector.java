//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.ranch.data.RanchObject;

/**
 * Displays information on a unit. Also allows for Big Shots customization and
 * purchase as appropriate.
 */
public class UnitInspector extends BContainer
    implements IconPalette.Inspector, ActionListener
{
    public UnitInspector (BangContext ctx)
    {
        GroupLayout glay = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        glay.setGap(0);
        setLayoutManager(glay);

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("ranch");
        _umsgs = ctx.getMessageManager().getBundle(BangCodes.UNITS_MSGS);

        add(_uname = new BLabel("", "ranch_unit_name"));
        _uname.setPreferredSize(new Dimension(258, 40));
        add(_uicon = new BLabel("", "ranch_unit_icon"));
        _uicon.setPreferredSize(new Dimension(258, 314));

        add(new Spacer(10, 10));

        BContainer row = new BContainer(GroupLayout.makeHStretch());
        row.add(new BLabel(_msgs.get("m.make"), "ranch_unit_label"));
        row.add(_umake = new BLabel("", "ranch_unit_data"));
        row.add(new BLabel(_msgs.get("m.move"), "ranch_unit_label"));
        row.add(_umove = new BLabel("", "ranch_unit_data"));
        add(row);

        row = new BContainer(GroupLayout.makeHStretch());
        row.add(new BLabel(_msgs.get("m.mode"), "ranch_unit_label"));
        row.add(_umode = new BLabel("", "ranch_unit_data"));
        row.add(new BLabel(_msgs.get("m.shoot"), "ranch_unit_label"));
        row.add(_ufire = new BLabel("", "ranch_unit_data"));
        add(row);

        add(new Spacer(10, 15));

        add(_udescrip = new BLabel("", "ranch_unit_info"), GroupLayout.FIXED);
    }

    /**
     * Called by our containing view once it gets ahold of the ranch
     * distributed object.
     */
    public void init (RanchObject ranchobj)
    {
        _ranchobj = ranchobj;
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconSelected (SelectableIcon icon)
    {
        UnitIcon uicon = (UnitIcon)icon;
        UnitConfig config = uicon.getUnit();

        _itemId = uicon.getItemId();
        _config = config;

        _uname.setText(uicon.getText());
        _uicon.setIcon(_ctx.loadModel("units", config.type).getIcon());

        _udescrip.setText(_umsgs.xlate(config.getName() + "_descrip"));

        _umake.setText(_umsgs.get("m." + config.make.toString().toLowerCase()));
        _umode.setText(_umsgs.get("m." + config.mode.toString().toLowerCase()));

        _umove.setText("" + config.moveDistance);
        _ufire.setText(config.getDisplayFireDistance());

//         // Big Shots have some additional user interface bits
//         boolean showRecruit = false, showCustomize = false;
//         if (config.rank == UnitConfig.Rank.BIGSHOT) {
//             if (itemId == -1) {
//                 showRecruit = true;
//                 _cost.setMoney(config.scripCost, config.coinCost, false);
//             } else {
//                 showCustomize = true;
//             }
//         }
//         setVisible(_details, _recruit, showRecruit);
//         setVisible(_details, _customize, showCustomize);
    }

    // documentation inherited from interface IconPalette.Inspector
    public void selectionCleared ()
    {
        // nada
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("recruit".equals(event.getAction())) {
            if (_config != null && _itemId == -1 &&
                _config.rank == UnitConfig.Rank.BIGSHOT) {
                recruit(_config);
            }

        } else if ("customize".equals(event.getAction())) {
            // setText("Not yet implemented. Sorry.");
        }
    }

    protected void recruit (UnitConfig config)
    {
        RanchService.ResultListener rl = new RanchService.ResultListener() {
            public void requestProcessed (Object result) {
                BigShotItem unit = (BigShotItem)result;
                RanchView parent = (RanchView)getParent().getParent();
                parent.unitRecruited(unit.getItemId());
                // setText(_msgs.get("m.recruited_bigshot"));
            }
            public void requestFailed (String cause) {
                // setText(_msgs.xlate(cause));
            }
        };
        _ranchobj.service.recruitBigShot(_ctx.getClient(), config.type, rl);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs, _umsgs;
    protected RanchObject _ranchobj;

    protected int _itemId = -1;
    protected UnitConfig _config;

    protected BContainer _recruit, _customize;
    protected MoneyLabel _cost;
    protected BLabel _uicon, _uname, _udescrip;
    protected BLabel _umake, _umode, _umove, _ufire;
}
