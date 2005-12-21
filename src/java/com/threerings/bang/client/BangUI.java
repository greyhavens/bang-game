//
// $Id$

package com.threerings.bang.client;

import java.awt.Font;
import java.util.logging.Level;

import com.jmex.bui.BButton;
import com.jmex.bui.BLabel;
import com.jmex.bui.BLookAndFeel;
import com.jmex.bui.background.BBackground;
import com.jmex.bui.background.TiledBackground;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.text.AWTTextFactory;

import com.threerings.openal.ClipProvider;
import com.threerings.openal.WaveDataClipProvider;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Contains various utility routines and general purpose bits related to
 * our user interface.
 */
public class BangUI
{
    /** A font used to render counters in the game. */
    public static Font COUNTER_FONT;

    /** The default look and feel. */
    public static BLookAndFeel defaultLNF;

    /** A look and feel for big splash text. */
    public static BLookAndFeel marqueeLNF;

    /** A look and feel for dialog title text. */
    public static BLookAndFeel dtitleLNF;

    /** A look and feel for player status text. */
    public static BLookAndFeel pstatusLNF;

    /** Used to load sounds from the classpath. */
    public static ClipProvider clipprov = new WaveDataClipProvider();

    /** An icon used to indicate a quantity of coins. */
    public static BIcon coinIcon;

    /** An icon used to indicate a quantity of scrip. */
    public static BIcon scripIcon;

    /** A left arrow icon. */
    public static BIcon leftArrow;

    /** A right arrow icon. */
    public static BIcon rightArrow;

    /**
     * Configures the UI singleton with a context reference.
     */
    public static void init (BasicContext ctx)
    {
        _ctx = ctx;
        _umsgs = _ctx.getMessageManager().getBundle("units");

        // load up our fonts
        Font dc = loadFont(ctx, "ui/fonts/dc.ttf");
        Font oldtown = loadFont(ctx, "ui/fonts/oldtown.ttf");
        Font tomb = loadFont(ctx, "ui/fonts/tomb.ttf");
        COUNTER_FONT = dc.deriveFont(Font.BOLD, 48);

        defaultLNF = new BangLookAndFeel();
        defaultLNF.setTextFactory(
            new AWTTextFactory(dc.deriveFont(Font.PLAIN, 16), true));

        marqueeLNF = new BangLookAndFeel();
        marqueeLNF.setTextFactory(new AWTTextFactory(COUNTER_FONT, true));

        dtitleLNF = new BangLookAndFeel();
        dtitleLNF.setTextFactory(
            new AWTTextFactory(oldtown.deriveFont(Font.PLAIN, 30), true));

        pstatusLNF = new BangLookAndFeel() {
            public BBackground createButtonBack (int state) {
                String path;
                int dx = 0, dy = 0;
                switch (state) {
                case BButton.DISABLED:
                case BButton.DOWN:
                    dx = 1;
                    dy = -1;
                    path = "rsrc/ui/pstatus/card_down.png";
                    break;
                default:
                case BButton.OVER:
                case BButton.UP: path = "rsrc/ui/pstatus/card_up.png"; break;
                }
                return new TiledBackground(
                    getResource(path), 1+dx, 1+dy, 1-dx, 1-dy);
            }
        };
        pstatusLNF.setTextFactory(
            new AWTTextFactory(new Font("Helvetica", Font.BOLD, 11), false));

        scripIcon = new ImageIcon(ctx.loadImage("ui/scrip.png"));
        coinIcon = new ImageIcon(ctx.loadImage("ui/coins.png"));

        leftArrow = new ImageIcon(ctx.loadImage("ui/left_arrow.png"));
        rightArrow = new ImageIcon(ctx.loadImage("ui/right_arrow.png"));
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
            label.setIcon(_ctx.loadModel("units", config.type).getIcon());
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
        button.setIcon(_ctx.loadModel("units", config.type).getIcon());
        button.setOrientation(BButton.VERTICAL);
        button.setHorizontalAlignment(BButton.CENTER);
        return button;
    }

    protected static Font loadFont (BasicContext ctx, String path)
    {
        Font font = null;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT,
                                   ctx.getResourceManager().getResource(path));
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load font '" + path + "'.", e);
            font = new Font("Dialog", Font.PLAIN, 16);
        }
        return font;
    }

    protected static BasicContext _ctx;
    protected static MessageBundle _umsgs;
}
