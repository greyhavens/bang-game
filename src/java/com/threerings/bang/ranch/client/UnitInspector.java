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
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.util.ReportingListener;
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
        setLayoutManager(GroupLayout.makeVStretch());

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("ranch");
        _umsgs = ctx.getMessageManager().getBundle(BangCodes.UNITS_MSGS);

        add(_uname = new BLabel("", "ranch_unit_name"), GroupLayout.FIXED);
        add(_uview = new UnitView(_ctx, false), GroupLayout.FIXED);

        TableLayout tlay = new TableLayout(4, 0, 5);
        tlay.setHorizontalAlignment(TableLayout.STRETCH);
        BContainer stats = new BContainer(tlay);
        stats.add(new BLabel(_msgs.get("m.make"), "table_label"));
        stats.add(_umake = new BLabel("", "table_data"));
        stats.add(new BLabel(_msgs.get("m.move"), "table_label"));
        stats.add(_umove = new BLabel("", "table_data"));

        stats.add(new BLabel(_msgs.get("m.mode"), "table_label"));
        stats.add(_umode = new BLabel("", "table_data"));
        stats.add(new BLabel(_msgs.get("m.shoot"), "table_label"));
        stats.add(_ufire = new BLabel("", "table_data"));
        add(stats, GroupLayout.FIXED);

        add(_ubonus = new UnitBonus(_ctx), GroupLayout.FIXED);
        _ubonus.setPreferredSize(new Dimension(258, 40));

        add(_udescrip = new BLabel("", "ranch_unit_info"));

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

        // we'll use this group to start practice scenarios
        _practice = new BContainer(new AbsoluteLayout());
        _practice.add(new BLabel(
                    _msgs.get("m.practice_desc"), "ranch_practice_info"),
                new Point(65, 48));
        btn = new BButton(_msgs.get("m.practice"), this, "practice");
        btn.setStyleClass("big_button");
        _practice.add(btn, new Point(82, 0));
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
        boolean showRecruit = false, 
                showCustomize = false, 
                showPractice = false;
        if (config.rank == UnitConfig.Rank.BIGSHOT) {
            if (_itemId == -1) {
                showRecruit = true;
                _cost.setMoney(config.scripCost, config.coinCost, false);
            } else {
                showPractice = true;
                showCustomize = true;
            }
        } else {
            showPractice = true;
        }
        if (showRecruit && !_recruit.isAdded()) {
            add(_recruit, GroupLayout.FIXED);
        } else if (!showRecruit && _recruit.isAdded()) {
            remove(_recruit);
        }
        if (showPractice && !_practice.isAdded()) {
            add(_practice, GroupLayout.FIXED);
        } else if (!showPractice && _practice.isAdded()) {
            remove(_practice);
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

        } else if ("practice".equals(event.getAction())) {
            if (_config != null) {
                PlayerService psvc = (PlayerService)
                    _ctx.getClient().requireService(PlayerService.class);
                ReportingListener rl = new ReportingListener(
                    _ctx, "ranch", "m.start_prac_failed");
                psvc.playPractice(_ctx.getClient(), _config.type, rl);
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

    protected BContainer _recruit, _customize, _practice;
    protected UnitBonus _ubonus;
    protected MoneyLabel _cost;
    protected UnitView _uview;
    protected BLabel _uname, _udescrip;
    protected BLabel _umake, _umode, _umove, _ufire;
}
