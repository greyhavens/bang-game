//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.util.ReportingListener;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.ranch.data.RanchCodes;
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
        setLayoutManager(GroupLayout.makeHStretch());

        add(_left = new BContainer(GroupLayout.makeVStretch()));
        add(_right = new BContainer(GroupLayout.makeVStretch()));
        ((GroupLayout)_right.getLayoutManager()).setOffAxisJustification(
            GroupLayout.CENTER);

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(RanchCodes.RANCH_MSGS);
        _umsgs = ctx.getMessageManager().getBundle(BangCodes.UNITS_MSGS);

        // first create the right column
        _right.add(new Spacer(10, 35), GroupLayout.FIXED);
        _right.add(_udetails = new BLabel("", "ranch_unit_details") {
            protected Dimension computePreferredSize (int whint, int hhint) {
                Dimension d = super.computePreferredSize(whint, hhint);
                // fix our height to the height of the avatar view next door
                d.height = 281;
                return d;
            }
        }, GroupLayout.FIXED);
        ImageIcon div = new ImageIcon(ctx.loadImage("ui/ranch/divider.png"));
        _right.add(new BLabel(div, "center_label"), GroupLayout.FIXED);
        _right.add(_customize = new BContainer());

        // then the left column
        _left.add(_uname = new BLabel("", "ranch_unit_name"),
            GroupLayout.FIXED);
        _left.add(_uview = new UnitView(_ctx, false), GroupLayout.FIXED);

        TableLayout tlay = new TableLayout(4, 5, 5);
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
        _left.add(stats, GroupLayout.FIXED);

        tlay = new TableLayout(2, 5, 5);
        tlay.setVerticalAlignment(TableLayout.CENTER);
        BContainer bonuses = new BContainer(tlay);
        GroupLayout glay = GroupLayout.makeHStretch();
        glay.setJustification(GroupLayout.RIGHT);
        BContainer labels = new BContainer(glay);
        labels.add(new BLabel(UnitBonus.getBonusIcon(
                        UnitBonus.BonusIcons.ATTACK, _ctx)), GroupLayout.FIXED);
        labels.add(new BLabel(_msgs.get("m.t_attack"), "table_label"));
        bonuses.add(labels);
        bonuses.add(_abonus = new UnitBonus(_ctx, 10));
        labels = new BContainer(glay);
        labels.add(new BLabel(UnitBonus.getBonusIcon(
                        UnitBonus.BonusIcons.DEFEND, _ctx)), GroupLayout.FIXED);
        labels.add(new BLabel(_msgs.get("m.t_defend"), "table_label"));
        bonuses.add(labels);
        bonuses.add(_dbonus = new UnitBonus(_ctx, 10));
        _left.add(bonuses);

        // we'll use this group when recruiting
        _recruit = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
        _recruit.add(new BLabel(_msgs.get("m.cost"), "table_label"));
        _recruit.add(_cost = new MoneyLabel(ctx));
        BButton btn = new BButton(_msgs.get("m.recruit"), this, "recruit");
        btn.setStyleClass("big_button");
        _recruit.add(btn);

        // we'll use this group to start practice scenarios
        _practice = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
        _practice.add(new BLabel(_msgs.get("m.practice_desc"),
                          "ranch_practice_info"));
        btn = new BButton(_msgs.get("m.practice"), this, "practice");
        btn.setStyleClass("big_button");
        _practice.add(btn);
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
        String details = _umsgs.xlate(config.getName() + "_details") + "\n\n" +
            _msgs.get("m.special_power") + "\n" +
            _umsgs.xlate(config.getName() + "_power");
        _udetails.setText(details);
        _umake.setText(_umsgs.get("m." + StringUtil.toUSLowerCase(config.make.toString())));
        _umake.setIcon(_abonus.getBonusIcon(config.make));
        _umode.setText(_umsgs.get("m." + StringUtil.toUSLowerCase(config.mode.toString())));
        _umode.setIcon(_abonus.getBonusIcon(config.mode));
        _umove.setText("" + config.moveDistance);
        _ufire.setText(config.getDisplayFireDistance());

        // set up the bonus display
        _abonus.setUnitConfig(config, true, UnitBonus.Which.ATTACK);
        _dbonus.setUnitConfig(config, true, UnitBonus.Which.DEFEND);

        // configure the fancy 3D unit display
        _uview.setUnit(config);

        // Big Shots have some additional user interface bits
        boolean showRecruit = false, showPractice = false;
        if (config.rank == UnitConfig.Rank.BIGSHOT) {
            if (_itemId == -1) {
                showRecruit = true;
                _cost.setMoney(config.scripCost, config.getCoinCost(_ctx.getUserObject()), false);
            } else {
                showPractice = true;
            }
        } else {
            showPractice = true;
        }
        if (showRecruit && !_recruit.isAdded()) {
            _left.add(_recruit, GroupLayout.FIXED);
        } else if (!showRecruit && _recruit.isAdded()) {
            _left.remove(_recruit);
        }
        if (showPractice && !_practice.isAdded()) {
            _left.add(_practice, GroupLayout.FIXED);
        } else if (!showPractice && _practice.isAdded()) {
            _left.remove(_practice);
        }
        // TODO: handle customize
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("recruit".equals(event.getAction())) {
            if (_config != null && _itemId == -1 && _config.rank == UnitConfig.Rank.BIGSHOT) {
                _ctx.getBangClient().displayPopup(
                    new RecruitDialog(_ctx, (RanchView)getParent(), _ranchobj, _config), true, 500);
            }

        } else if ("practice".equals(event.getAction())) {
            if (_config != null) {
                PlayerService psvc = _ctx.getClient().requireService(PlayerService.class);
                ReportingListener rl = new ReportingListener(_ctx, "ranch", "m.start_prac_failed");
                psvc.playPractice(_config.type, rl);
                _practice.setEnabled(false); // prevent double clicky
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

    protected BContainer _left, _right;
    protected BContainer _recruit, _customize, _practice;
    protected UnitBonus _abonus, _dbonus;
    protected MoneyLabel _cost;
    protected UnitView _uview;
    protected BLabel _uname, _udetails;
    protected BLabel _umake, _umode, _umove, _ufire;
}
