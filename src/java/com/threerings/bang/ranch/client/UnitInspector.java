//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.SubimageIcon;
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

        add(_ubonus = new BContainer());
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
        _umake.setIcon(getBonusIcon(config.make));
        _umode.setText(_umsgs.get("m." + config.mode.toString().toLowerCase()));
        _umode.setIcon(getBonusIcon(config.mode));
        _umove.setText("" + config.moveDistance);
        _ufire.setText(config.getDisplayFireDistance());

        // set up the bonus display
        configureBonusDisplay();

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

    /**
     * Configures the display of bonuses and penalties.
     */
    protected void configureBonusDisplay ()
    {
        _ubonus.removeAll();
        ArrayList<BContainer> bonusList = new ArrayList<BContainer>();
        for (UnitConfig.Mode mode : UnitConfig.Mode.values()) {
            int adj = _config.damageAdjust[mode.ordinal()];
            if (_config.damage + adj <= 0) {
                bonusList.add(makeBonusContainer(BonusIcons.ATTACK, 
                            _modeIconMap.get(mode), BonusIcons.NA));
            } else if (adj > 0) {
                bonusList.add(makeBonusContainer(BonusIcons.ATTACK, 
                            _modeIconMap.get(mode), BonusIcons.UP));
            } else if (adj < 0) {
                bonusList.add(makeBonusContainer(BonusIcons.ATTACK,
                            _modeIconMap.get(mode), BonusIcons.DOWN));
            }
        }
        for (UnitConfig.Make make : UnitConfig.Make.values()) {
            int adj = _config.damageAdjust[
                UnitConfig.MODE_COUNT + make.ordinal()];
            if (_config.damage + adj <= 0) {
                bonusList.add(makeBonusContainer(BonusIcons.ATTACK, 
                            _makeIconMap.get(make), BonusIcons.NA));
            } else if (adj > 0) {
                bonusList.add(makeBonusContainer(BonusIcons.ATTACK, 
                            _makeIconMap.get(make), BonusIcons.UP));
            } else if (adj < 0) {
                bonusList.add(makeBonusContainer(BonusIcons.ATTACK,
                            _makeIconMap.get(make), BonusIcons.DOWN));
            }
        }
        for (UnitConfig.Mode mode : UnitConfig.Mode.values()) {
            int adj = _config.defenseAdjust[mode.ordinal()];
            if (adj > 0) {
                bonusList.add(makeBonusContainer(BonusIcons.DEFEND, 
                            _modeIconMap.get(mode), BonusIcons.UP));
            } else if (adj < 0) {
                bonusList.add(makeBonusContainer(BonusIcons.DEFEND,
                            _modeIconMap.get(mode), BonusIcons.DOWN));
            }
        }
        for (UnitConfig.Make make : UnitConfig.Make.values()) {
            int adj = _config.defenseAdjust[
                UnitConfig.MODE_COUNT + make.ordinal()];
            if (_config.damage + adj <= 0) {
                bonusList.add(makeBonusContainer(BonusIcons.DEFEND, 
                            _makeIconMap.get(make), BonusIcons.NA));
            } else if (adj > 0) {
                bonusList.add(makeBonusContainer(BonusIcons.DEFEND, 
                            _makeIconMap.get(make), BonusIcons.UP));
            } else if (adj < 0) {
                bonusList.add(makeBonusContainer(BonusIcons.DEFEND,
                            _makeIconMap.get(make), BonusIcons.DOWN));
            }
        }

        int size = bonusList.size();
        if (size > _maxBonusPerRow) {
            if (size - _maxBonusPerRow == 1) {
                size = _maxBonusPerRow - 1;
            } else {
                size = _maxBonusPerRow;
            }
        }
        
        TableLayout layout = new TableLayout(size, 3, 24);
        layout.setHorizontalAlignment(TableLayout.CENTER);
        layout.setVerticalAlignment(TableLayout.CENTER);
        _ubonus.setLayoutManager(layout);
        for (Iterator<BContainer> iter = bonusList.iterator(); 
                iter.hasNext(); ) {
            _ubonus.add(iter.next());
        }
    }

    /**
     * Creates and returns a container that has the three bonus icons with
     * a formated tool tip text.
     */
    protected BContainer makeBonusContainer (
            BonusIcons method, BonusIcons type, BonusIcons effect)
    {
        GroupLayout layout = GroupLayout.makeHoriz(GroupLayout.CENTER);
        layout.setGap(0);
        BContainer bonus = new BContainer(layout);
        
        String firstPart = null;
        String sMethod = method.toString().toLowerCase();
        if (BonusIcons.NA.equals(effect)) {
            firstPart = _msgs.get("m.na." + sMethod);
        } else {
            firstPart = _msgs.xlate(MessageBundle.compose("m.versus", 
                    "m." + sMethod, "m." + effect.toString().toLowerCase()));
        }
        String tip = _msgs.xlate(MessageBundle.tcompose("m.units", firstPart, 
                        _umsgs.get("m." + type.toString().toLowerCase())));
        bonus.add(bonusIconLabel(method, tip));
        bonus.add(bonusIconLabel(type, tip));
        bonus.add(bonusIconLabel(effect, tip));
        
        return bonus;
    }

    /**
     * Convenience function for creating a bonus icon label with the tooltip.
     */
    protected BLabel bonusIconLabel (BonusIcons icon, String tip)
    {
        BLabel label = new BLabel("", "table_data");
        label.setIcon(getBonusIcon(icon));
        label.setTooltipText(tip);
        return label;
    }

    /**
     * Returns a BIcon of the bonus icon based on the UnitConfig.Mode.
     */
    protected BIcon getBonusIcon (UnitConfig.Mode mode)
    {
        return getBonusIcon(_modeIconMap.get(mode));
    }

    /**
     * Returns a BIcon of the bonus icon based on the UnitConfig.Make.
     */
    protected BIcon getBonusIcon (UnitConfig.Make make)
    {
        return getBonusIcon(_makeIconMap.get(make));
    }

    /**
     * Returns a BIcon of the bonus icon for the specific index.
     */
    protected BIcon getBonusIcon (BonusIcons bi)
    {
        int idx = bi.ordinal();
        if (_bonusIcons[idx] != null) {
            return _bonusIcons[idx];
        }

        BImage icons = _ctx.getImageCache().getBImage(
                "ui/ranch/unit_icons.png");
        int size = icons.getHeight();
        _bonusIcons[idx] = new SubimageIcon(icons, idx * size, 0, size, size);
        return _bonusIcons[idx];
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs, _umsgs;
    protected RanchObject _ranchobj;

    protected int _itemId = -1;
    protected UnitConfig _config;

    protected BContainer _recruit, _customize, _ubonus;
    protected MoneyLabel _cost;
    protected UnitView _uview;
    protected BLabel _uname, _udescrip;
    protected BLabel _umake, _umode, _umove, _ufire;

    protected enum BonusIcons {
        ATTACK, DEFEND,
        GROUND, AIR, RANGE,
        STEAM, HUMAN, SPIRIT,
        UP, DOWN, NA
    };
    protected BIcon[] _bonusIcons = 
        new BIcon[EnumSet.allOf(BonusIcons.class).size()];

    protected static final int _maxBonusPerRow = 3;
    protected static final HashMap<UnitConfig.Mode, BonusIcons> _modeIconMap = 
        new HashMap<UnitConfig.Mode, BonusIcons>();
    protected static final HashMap<UnitConfig.Make, BonusIcons> _makeIconMap = 
        new HashMap<UnitConfig.Make, BonusIcons>();
    static {
        _modeIconMap.put(UnitConfig.Mode.GROUND, BonusIcons.GROUND);
        _modeIconMap.put(UnitConfig.Mode.AIR, BonusIcons.AIR);
        _modeIconMap.put(UnitConfig.Mode.RANGE, BonusIcons.RANGE);
        
        _makeIconMap.put(UnitConfig.Make.HUMAN, BonusIcons.HUMAN);
        _makeIconMap.put(UnitConfig.Make.STEAM, BonusIcons.STEAM);
        _makeIconMap.put(UnitConfig.Make.SPIRIT, BonusIcons.SPIRIT);
    };
}
