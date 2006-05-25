//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;

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
        _uname.setPreferredSize(new Dimension(258, 30));

        add(_uview = new UnitView(_ctx, false));

        add(new Spacer(10, 3));

        BContainer stats = new BContainer(new TableLayout(4, 0, 5));
        stats.add(new BLabel(_msgs.get("m.make"), "table_label"));
        stats.add(_umake = new BLabel("", "table_data"));
        stats.add(new BLabel(_msgs.get("m.move"), "table_label"));
        stats.add(_umove = new BLabel("", "table_data"));

        stats.add(new Spacer(10, 1));
        stats.add(new Spacer(100, 1));
        stats.add(new Spacer(10, 1));
        stats.add(new Spacer(10, 1));

        stats.add(new BLabel(_msgs.get("m.mode"), "table_label"));
        stats.add(_umode = new BLabel("", "table_data"));
        stats.add(new BLabel(_msgs.get("m.shoot"), "table_label"));
        stats.add(_ufire = new BLabel("", "table_data"));
        add(stats);

        add(new Spacer(10, 3));

        add(_ubonus = new UnitBonus(_ctx));
        _ubonus.setPreferredSize(new Dimension(258, 51));

        add(new Spacer(10, 6));

        add(_udescrip = new BLabel("", "ranch_unit_info"), GroupLayout.FIXED);
        _udescrip.setPreferredSize(new Dimension(1, 53));

        add(new Spacer(10, 3));

        // we'll use this group when recruiting
        _recruit = new BContainer(new AbsoluteLayout());
        BContainer row = new BContainer(
                GroupLayout.makeHoriz(GroupLayout.LEFT));
        row.add(new BLabel(_msgs.get("m.cost"), "ranch_unit_cost"));
        row.add(_cost = new MoneyLabel(ctx));
        _recruit.add(row, new Point(65, 53));
        BButton btn = new BButton(_msgs.get("m.recruit"), this, "recruit");
        btn.setStyleClass("big_button");
        _recruit.add(btn, new Point(105, 0));
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
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        // don't worry about deselection
        if (!selected) {
            return;
        }

        UnitIcon uicon = (UnitIcon)icon;
        UnitConfig config = uicon.getUnit();

        _itemId = uicon.getItemId();
        _config = config;

        // set up the myriad labels
        _uname.setText(uicon.getText());
        _udescrip.setText(_umsgs.xlate(config.getName() + "_descrip"));
        _umake.setText(_umsgs.get("m." + config.make.toString().toLowerCase()));
        _umake.setIcon(_ubonus.getBonusIcon(config.make));
        _umode.setText(_umsgs.get("m." + config.mode.toString().toLowerCase()));
        _umode.setIcon(_ubonus.getBonusIcon(config.mode));
        _umove.setText("" + config.moveDistance);
        _ufire.setText(config.getDisplayFireDistance());

        // set up the bonus display
        _ubonus.setUnitConfig(config, true);

        // configure the fancy 3D unit display
        _uview.setUnit(config);

        // Big Shots have some additional user interface bits
        boolean showRecruit = false, showCustomize = false;
        if (config.rank == UnitConfig.Rank.BIGSHOT) {
            if (_itemId == -1) {
                showRecruit = true;
                _cost.setMoney(config.scripCost, config.coinCost, false);
            } else {
                showCustomize = true;
            }
        }
        if (showRecruit && !_recruit.isAdded()) {
            add(_recruit);
        } else if (!showRecruit && _recruit.isAdded()) {
            remove(_recruit);
        }
        // TODO: handle customize
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("recruit".equals(event.getAction())) {
            if (_config != null && _itemId == -1 &&
                _config.rank == UnitConfig.Rank.BIGSHOT) {
                _ctx.getBangClient().displayPopup(
                    new RecruitDialog(_ctx, (RanchView)getParent(), _ranchobj,
                                      _config), true, 400);
            }

        } else if ("customize".equals(event.getAction())) {
            // setText("Not yet implemented. Sorry.");
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs, _umsgs;
    protected RanchObject _ranchobj;

    protected int _itemId = -1;
    protected UnitConfig _config;

    protected BContainer _recruit, _customize;
    protected UnitBonus _ubonus;
    protected MoneyLabel _cost;
    protected UnitView _uview;
    protected BLabel _uname, _udescrip;
    protected BLabel _umake, _umode, _umove, _ufire;
}
