//
// $Id$

package com.threerings.bang.client;

import java.awt.Font;

import com.jmex.bui.BButton;
import com.jmex.bui.BIcon;
import com.jmex.bui.BLabel;
import com.jmex.bui.BLookAndFeel;
import com.jmex.bui.BlankIcon;
import com.jmex.bui.ImageIcon;
import com.jmex.bui.text.AWTTextFactory;

import com.threerings.openal.ClipProvider;
import com.threerings.openal.WaveDataClipProvider;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Contains various utility routines and general purpose bits related to
 * our user interface.
 */
public class BangUI
{
    /** A font used to render counters in the game. */
    public static Font COUNTER_FONT = new Font("Helvetica", Font.BOLD, 48);

    /** A font used to render dialog titles. */
    public static Font DTITLE_FONT = new Font("Dialog", Font.BOLD, 16);

    /** A look and feel for big splash text. */
    public static BLookAndFeel marqueeLNF;

    /** A look and feel for dialog title text. */
    public static BLookAndFeel dtitleLNF;

    /** Used to load sounds from the classpath. */
    public static ClipProvider clipprov = new WaveDataClipProvider();

    /** An icon used to indicate a quantity of coins. */
    public static BIcon coinIcon;

    /** An icon used to indicate a quantity of scrip. */
    public static BIcon scripIcon;

    /**
     * Configures the UI singleton with a context reference.
     */
    public static void init (BangContext ctx)
    {
        _ctx = ctx;
        _umsgs = _ctx.getMessageManager().getBundle("units");

        marqueeLNF = new BangLookAndFeel();
        marqueeLNF.setTextFactory(new AWTTextFactory(COUNTER_FONT));

        dtitleLNF = new BangLookAndFeel();
        dtitleLNF.setTextFactory(new AWTTextFactory(DTITLE_FONT));

        scripIcon = new ImageIcon(ctx.loadImage("ui/scrip.png"));
        coinIcon = new ImageIcon(ctx.loadImage("ui/coins.png"));
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
     * Configures the supplied label to display the specified card.
     */
    public static void configCardLabel (BLabel label, CardItem card)
    {
        label.setOrientation(BLabel.VERTICAL);
        label.setHorizontalAlignment(BLabel.CENTER);
        String path = "cards/" + card.getType() + "/icon.png";
        label.setIcon(new ImageIcon(_ctx.loadImage(path)));
        String name = _ctx.xlate(BangCodes.CARDS_MSGS, "m." + card.getType());
        label.setText(name + ": " + card.getQuantity());
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
