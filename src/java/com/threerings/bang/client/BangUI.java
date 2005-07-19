//
// $Id$

package com.threerings.bang.client;

import java.awt.Font;

import com.jme.bui.BButton;
import com.jme.bui.BLabel;
import com.jme.bui.BlankIcon;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Contains various utility routines and general purpose bits related to
 * our user interface.
 */
public class BangUI
{
    /** A font used to render counters in the game. */
    public static Font COUNTER_FONT = new Font("Helvetica", Font.BOLD, 24);

    /**
     * Configures the UI singleton with a context reference.
     */
    public static void init (BangContext ctx)
    {
        _ctx = ctx;
        _umsgs = _ctx.getMessageManager().getBundle("units");
    }

    /**
     * Creates a label with the icon for the specified unit and the unit's
     * name displayed below. If the supplied unit config is blank, an
     * "<empty>" label will be created.
     */
    public static BLabel createUnitLabel (UnitConfig config)
    {
        BLabel label = new BLabel("");
        configUnitLabel(label, config);
        return label;
    }

    /**
     * Configures the supplied label as a unit label. If the supplied unit
     * config is blank, an "<empty>" label will be configure.
     */
    public static void configUnitLabel (BLabel label, UnitConfig config)
    {
        label.setOrientation(BLabel.VERTICAL);
        label.setHorizontalAlignment(BLabel.CENTER);
        if (config == null) {
            label.setText(_ctx.xlate("units", "m.empty"));
            label.setIcon(new BlankIcon(Model.ICON_SIZE, Model.ICON_SIZE));
        } else {
            label.setText(_ctx.xlate("units", config.getName()));
            label.setIcon(_ctx.getModelCache().getModel(
                              "units", config.type).getIcon());
        }
    }

    /**
     * Creates a label with the icon for the specified unit and the unit's
     * name displayed below.
     */
    public static BButton createUnitButton (UnitConfig config)
    {
        BButton button = new BButton(_ctx.xlate("units", config.getName()));
        button.setIcon(_ctx.getModelCache().getModel(
                           "units", config.type).getIcon());
        button.setOrientation(BButton.VERTICAL);
        button.setHorizontalAlignment(BButton.CENTER);
        return button;
    }

    protected static BangContext _ctx;
    protected static MessageBundle _umsgs;
}
