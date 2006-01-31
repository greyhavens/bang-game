//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

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
        super(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                   GroupLayout.STRETCH));

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("ranch");
        _umsgs = ctx.getMessageManager().getBundle(BangCodes.UNITS_MSGS);

        add(_uname = new BLabel("", "ranch_unit_name"));
        add(_uicon = new BLabel("", "ranch_unit_icon"));

        BContainer row = new BContainer(GroupLayout.makeHStretch());
        row.add(_umake = new BLabel("", "ranch_unit_info"));
        row.add(_umode = new BLabel("", "ranch_unit_info"));
        add(row);

        row = new BContainer(GroupLayout.makeHStretch());
        row.add(_umove = new BLabel("", "ranch_unit_info"));
        row.add(_ufire = new BLabel("", "ranch_unit_info"));
        add(row);

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

    /**
     * Configures the unit we're inspecting.
     */
    public void setUnit (int itemId, UnitConfig config)
    {
        _itemId = itemId;
        _config = config;

        _uname.setText(_umsgs.xlate(config.getName()));
        _uicon.setIcon(_ctx.loadModel("units", config.type).getIcon());

        _udescrip.setText(_umsgs.xlate(config.getName() + "_descrip"));

        String msg = "m." + config.make.toString().toLowerCase();
        _umake.setText(_umsgs.get("m.make", _umsgs.get(msg)));
        msg = "m." + config.mode.toString().toLowerCase();
        _umode.setText(_umsgs.get("m.mode", _umsgs.get(msg)));

        _umove.setText(_umsgs.get("m.move_range", "" + config.moveDistance));
        _ufire.setText(_umsgs.get("m.fire_range",
                                 config.getDisplayFireDistance()));

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
    public void iconSelected (SelectableIcon icon)
    {
        UnitIcon uicon = (UnitIcon)icon;
        setUnit(uicon.getItemId(), uicon.getUnit());
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
