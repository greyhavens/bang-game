//
// $Id$

package com.threerings.bang.client;

import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;

import com.jme.image.Image;

import com.jmex.bui.BButton;
import com.jmex.bui.BLabel;
import com.jmex.bui.BStyleSheet;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.text.AWTTextFactory;
import com.jmex.bui.text.BTextFactory;

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

    /** The stylesheet used to configure our interface. */
    public static BStyleSheet stylesheet;

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
        _fonts.put("Dali", loadFont(ctx, "ui/fonts/dc.ttf"));
        _fonts.put("Tombstone", loadFont(ctx, "ui/fonts/tomb.ttf"));
        _fonts.put("Old Town", loadFont(ctx, "ui/fonts/oldtown.ttf"));

        COUNTER_FONT = _fonts.get("Dali").deriveFont(Font.BOLD, 48);

        // create our stylesheet
        BStyleSheet.ResourceProvider rprov = new BStyleSheet.ResourceProvider() {
            public BTextFactory createTextFactory (
                String family, String style, int size) {
                int nstyle = Font.PLAIN;
                if (style.equals(BStyleSheet.BOLD)) {
                    nstyle = Font.BOLD;
                } else if (style.equals(BStyleSheet.ITALIC)) {
                    nstyle = Font.ITALIC;
                } else if (style.equals(BStyleSheet.BOLD_ITALIC)) {
                    nstyle = Font.ITALIC|Font.BOLD;
                }
                Font font = _fonts.get(family);
                if (font == null) {
                    font = new Font(family, nstyle, size);
                } else {
                    font = font.deriveFont(nstyle, size);
                }
                return new AWTTextFactory(font, true);
            }
            public Image loadImage (String path) throws IOException {
                return _ctx.loadImage(path);
            }
        };
        try {
            InputStream is =
                ctx.getResourceManager().getResource("ui/style.bss");
            stylesheet =
                new BStyleSheet(new InputStreamReader(is, "UTF-8"), rprov);
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to load stylesheet", ioe);
        }

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
        label.setStyleClass("unit_label");
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
    public static void configCardLabel (BButton label, CardItem card)
    {
        label.setOrientation(BLabel.VERTICAL);
        label.setStyleClass("card_label");
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
        button.setStyleClass("unit_label");
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
    protected static HashMap<String,Font> _fonts = new HashMap<String,Font>();
}
