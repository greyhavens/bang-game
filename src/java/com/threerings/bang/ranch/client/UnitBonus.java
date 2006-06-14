//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;

import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;

import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.SubimageIcon;

import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.util.BasicContext;

/**
 * Displays bonus/penalty information for a unit.
 */
public class UnitBonus extends BContainer
{
    public UnitBonus (BasicContext ctx)
    {
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("ranch");
        _umsgs = ctx.getMessageManager().getBundle(BangCodes.UNITS_MSGS);
    }

    /**
     * Called to update the displayed information.
     */
    public void setUnitConfig (UnitConfig config, boolean addTip)
    {
        _addTip = addTip;
        removeAll();
        ArrayList<BContainer> bonusList = new ArrayList<BContainer>();
        for (UnitConfig.Mode mode : UnitConfig.Mode.values()) {
            int adj = config.damageAdjust[mode.ordinal()];
            if (config.damage + adj <= 0) {
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
            int adj = config.damageAdjust[
                UnitConfig.MODE_COUNT + make.ordinal()];
            if (config.damage + adj <= 0) {
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
            int adj = config.defenseAdjust[mode.ordinal()];
            if (adj > 0) {
                bonusList.add(makeBonusContainer(BonusIcons.DEFEND,
                            _modeIconMap.get(mode), BonusIcons.UP));
            } else if (adj < 0) {
                bonusList.add(makeBonusContainer(BonusIcons.DEFEND,
                            _modeIconMap.get(mode), BonusIcons.DOWN));
            }
        }
        for (UnitConfig.Make make : UnitConfig.Make.values()) {
            int adj = config.defenseAdjust[
                UnitConfig.MODE_COUNT + make.ordinal()];
            if (adj > 0) {
                bonusList.add(makeBonusContainer(BonusIcons.DEFEND,
                            _makeIconMap.get(make), BonusIcons.UP));
            } else if (adj < 0) {
                bonusList.add(makeBonusContainer(BonusIcons.DEFEND,
                            _makeIconMap.get(make), BonusIcons.DOWN));
            }
        }

        int size = bonusList.size();
        if (size == 0) {
            return;
        }
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
        setLayoutManager(layout);
        for (Iterator<BContainer> iter = bonusList.iterator();
                iter.hasNext(); ) {
            add(iter.next());
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
        if (_addTip) {
            label.setTooltipText(tip);
        }
        return label;
    }

    /**
     * Returns a BIcon of the bonus icon based on the UnitConfig.Mode.
     */
    public BIcon getBonusIcon (UnitConfig.Mode mode)
    {
        return getBonusIcon(_modeIconMap.get(mode));
    }

    /**
     * Returns a BIcon of the bonus icon based on the UnitConfig.Make.
     */
    public BIcon getBonusIcon (UnitConfig.Make make)
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

    protected BasicContext _ctx;

    protected MessageBundle _msgs;
    protected MessageBundle _umsgs;

    protected boolean _addTip;

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
