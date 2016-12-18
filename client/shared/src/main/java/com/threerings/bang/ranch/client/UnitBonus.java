//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;

import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.SubimageIcon;

import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.ranch.data.RanchCodes;

/**
 * Displays bonus/penalty information for a unit.
 */
public class UnitBonus extends BContainer
{
    /** Used by {@link #setUnitConfig}. */
    public static enum Which { ATTACK, DEFEND, BOTH };

    public static enum BonusIcons {
        ATTACK, DEFEND,
        GROUND, AIR, RANGE,
        STEAM, HUMAN, SPIRIT,
        UP, DOWN, NA
    };

    public UnitBonus (BasicContext ctx, int gap)
    {
        this(ctx, gap, false);
    }

    public UnitBonus (BasicContext ctx, int gap, boolean smallMode)
    {
        _ctx = ctx;
        _gap = gap;
        _msgs = ctx.getMessageManager().getBundle(RanchCodes.RANCH_MSGS);
        _umsgs = ctx.getMessageManager().getBundle(BangCodes.UNITS_MSGS);
        _smallMode = smallMode;
    }

    /**
     * Called to update the displayed information.
     */
    public void setUnitConfig (UnitConfig config, boolean addTip, Which which)
    {
        _addTip = addTip;
        removeAll();
        ArrayList<BContainer> bonusList = new ArrayList<BContainer>();
        boolean text = (which == Which.BOTH);

        if (which != Which.DEFEND) {
            for (UnitConfig.Mode mode : UnitConfig.Mode.values()) {
                int adj = config.damageAdjust[mode.ordinal()];
                if (config.damage + adj <= 0) {
                    bonusList.add(makeBonusContainer(BonusIcons.ATTACK,
                              _modeIconMap.get(mode), BonusIcons.NA, text));
                } else if (adj > 0) {
                    bonusList.add(makeBonusContainer(BonusIcons.ATTACK,
                              _modeIconMap.get(mode), BonusIcons.UP, text));
                } else if (adj < 0) {
                    bonusList.add(makeBonusContainer(BonusIcons.ATTACK,
                              _modeIconMap.get(mode), BonusIcons.DOWN, text));
                }
            }
            for (UnitConfig.Make make : UnitConfig.Make.values()) {
                int adj = config.damageAdjust[
                    UnitConfig.MODE_COUNT + make.ordinal()];
                if (config.damage + adj <= 0) {
                    bonusList.add(makeBonusContainer(BonusIcons.ATTACK,
                              _makeIconMap.get(make), BonusIcons.NA, text));
                } else if (adj > 0) {
                    bonusList.add(makeBonusContainer(BonusIcons.ATTACK,
                              _makeIconMap.get(make), BonusIcons.UP, text));
                } else if (adj < 0) {
                    bonusList.add(makeBonusContainer(BonusIcons.ATTACK,
                              _makeIconMap.get(make), BonusIcons.DOWN, text));
                }
            }
        }

        if (which != Which.ATTACK) {
            for (UnitConfig.Mode mode : UnitConfig.Mode.values()) {
                int adj = config.defenseAdjust[mode.ordinal()];
                if (adj > 0) {
                    bonusList.add(makeBonusContainer(BonusIcons.DEFEND,
                              _modeIconMap.get(mode), BonusIcons.UP, text));
                } else if (adj < 0) {
                    bonusList.add(makeBonusContainer(BonusIcons.DEFEND,
                              _modeIconMap.get(mode), BonusIcons.DOWN, text));
                }
            }
            for (UnitConfig.Make make : UnitConfig.Make.values()) {
                int adj = config.defenseAdjust[
                    UnitConfig.MODE_COUNT + make.ordinal()];
                if (adj > 0) {
                    bonusList.add(makeBonusContainer(BonusIcons.DEFEND,
                              _makeIconMap.get(make), BonusIcons.UP, text));
                } else if (adj < 0) {
                    bonusList.add(makeBonusContainer(BonusIcons.DEFEND,
                              _makeIconMap.get(make), BonusIcons.DOWN, text));
                }
            }
        }

        int cols = bonusList.size();
        if (cols > MAX_COLS) {
            if (cols - MAX_COLS == 1) {
                cols = MAX_COLS - 1;
            } else {
                cols = MAX_COLS;
            }
        }

        if (cols == 0) {
            setLayoutManager(GroupLayout.makeHoriz(GroupLayout.CENTER));

            String none;
            switch (which) {
            case ATTACK: none = "m.no_attack_mods"; break;
            case DEFEND: none = "m.no_defend_mods"; break;
            default: none = "m.no_mods"; break;
            }
            add(bonusIconLabel(BonusIcons.NA, _msgs.get(none)));

        } else {
            if (text) {
                GroupLayout layout = GroupLayout.makeVert(GroupLayout.CENTER);
                layout.setOffAxisJustification(GroupLayout.LEFT);
                setLayoutManager(layout);
            } else {
                TableLayout layout = new TableLayout(cols, 3, _gap);
                layout.setVerticalAlignment(TableLayout.CENTER);
                setLayoutManager(layout);
            }
            for (BContainer cont : bonusList) {
                add(cont);
            }
        }
    }

    /**
     * Creates and returns a container that has the three bonus icons with
     * a formated tool tip text.
     */
    protected BContainer makeBonusContainer (
        BonusIcons method, BonusIcons type, BonusIcons effect, boolean text)
    {
        GroupLayout layout = GroupLayout.makeHoriz(GroupLayout.CENTER);
        layout.setGap(1);
        BContainer bonus = new BContainer(layout);

        String firstPart = null;
        String sMethod = StringUtil.toUSLowerCase(method.toString());
        if (BonusIcons.NA.equals(effect)) {
            firstPart = _msgs.get("m.na." + sMethod);
        } else {
            firstPart = _msgs.xlate(MessageBundle.compose("m.versus",
                    "m." + sMethod, "m." + StringUtil.toUSLowerCase(effect.toString())));
        }
        String tip = _msgs.xlate(MessageBundle.tcompose("m.units", firstPart,
                        _umsgs.get("m." + StringUtil.toUSLowerCase(type.toString()))));
        if (text) {
            bonus.add(bonusIconLabel(method, tip));
        }
        bonus.add(bonusIconLabel(type, tip));
        bonus.add(bonusIconLabel(effect, tip));
        if (text) {
            bonus.add(new Spacer(4, 0));
            bonus.add(new BLabel(tip, "tooltip_label"));
        }

        return bonus;
    }

    /**
     * Convenience function for creating a bonus icon label with the tooltip.
     */
    protected BLabel bonusIconLabel (BonusIcons icon, String tip)
    {
        BLabel label = new BLabel("", "table_data");
        label.setIcon(getBonusIcon(icon, _ctx, _smallMode));
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
        return getBonusIcon(mode, _ctx, _smallMode);
    }

    public static BIcon getBonusIcon (
            UnitConfig.Mode mode, BasicContext ctx, boolean small)
    {
        return getBonusIcon(_modeIconMap.get(mode), ctx, small);
    }

    /**
     * Returns a BIcon of the bonus icon based on the UnitConfig.Make.
     */
    public BIcon getBonusIcon (UnitConfig.Make make)
    {
        return getBonusIcon(make, _ctx, _smallMode);
    }

    public static BIcon getBonusIcon (
            UnitConfig.Make make, BasicContext ctx, boolean small)
    {
        return getBonusIcon(_makeIconMap.get(make), ctx, small);
    }

    /**
     * Returns a BIcon of the bonus icon for the specific index.
     */
    public static BIcon getBonusIcon (BonusIcons bi, BasicContext ctx)
    {
        return getBonusIcon(bi, ctx, false);
    }

    public static BIcon getBonusIcon (
            BonusIcons bi, BasicContext ctx, boolean small)
    {
        int idx = bi.ordinal();
        int isize = (small ? 1 : 0);
        if (_bonusIcons[isize][idx] != null) {
            return _bonusIcons[isize][idx];
        }

        String path = "ui/ranch/unit_icons";
        if (small) {
            path += "_small";
        }
        BImage icons = ctx.getImageCache().getBImage(path + ".png");
        int size = icons.getHeight();
        _bonusIcons[isize][idx] = new SubimageIcon(
            icons, idx * ICON_WIDTH[isize], 0, ICON_WIDTH[isize], size);
        return _bonusIcons[isize][idx];
    }

    protected BasicContext _ctx;
    protected int _gap;
    protected MessageBundle _msgs, _umsgs;
    protected boolean _addTip, _smallMode;
    protected static BIcon[][] _bonusIcons = new BIcon[][] {
        new BIcon[BonusIcons.values().length],
        new BIcon[BonusIcons.values().length]
    };

    protected static final int[] ICON_WIDTH = { 19, 12 };
    protected static final int MAX_COLS = 3;

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
