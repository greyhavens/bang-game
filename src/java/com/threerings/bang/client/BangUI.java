//
// $Id$

package com.threerings.bang.client;

import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;

import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS;
import javax.swing.text.html.StyleSheet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Level;

import org.lwjgl.openal.AL10;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.WaveData;

import com.jme.image.Image;

import com.jmex.bui.BButton;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BStyleSheet;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.text.AWTTextFactory;
import com.jmex.bui.text.BTextFactory;

import com.jmex.sound.openAL.objects.util.OggInputStream;

import com.threerings.openal.Clip;
import com.threerings.openal.ClipProvider;
import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.util.ImageCache;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.SoundUtil;

import static com.threerings.bang.Log.log;

/**
 * Contains various utility routines and general purpose bits related to
 * our user interface.
 */
public class BangUI
{
    /** Enumerates all possible UI feedback sounds. */
    public static enum FeedbackSound {
        BUTTON_PRESS,
        CHAT_RECEIVE,
        CHAT_SEND,
        CLICK,
        INVALID_ACTION,
        ITEM_PURCHASE,
        ITEM_SELECTED,
        KEY_TYPED,
        TAB_SELECTED,
        WINDOW_DISMISS,
        WINDOW_OPEN,
    };

    /** The default max length of any text field. */
    public static final int TEXT_FIELD_MAX_LENGTH = 300;

    /** A font used to render counters in the game. */
    public static Font COUNTER_FONT;

    /** A font used to render the marquee over the board. */
    public static Font MARQUEE_FONT;

    /** A font used to render the loading status over the board. */
    public static Font LOADING_FONT;

    /** A font used to render the damage indicator in the game. */
    public static Font DAMAGE_FONT;

    /** The stylesheet used to configure our interface. */
    public static BStyleSheet stylesheet;

    /** The stylesheet used to render HTML in the game. */
    public static StyleSheet css;

    /** Used to load sounds from the classpath. */
    public static ClipProvider clipprov;

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

        // configure our tooltip settings
        _ctx.getRootNode().setTooltipPreferredWidth(300);
        _ctx.getRootNode().setTooltipTimeout(1f);

        // load up our fonts
        _fonts.put("Tombstone", loadFont(ctx, "ui/fonts/tomb.ttf"));

        // we need to stretch Dom Casual out a bit
        Font dom = loadFont(ctx, "ui/fonts/domcasual.ttf");
        dom = dom.deriveFont(
            Font.PLAIN, AffineTransform.getScaleInstance(1.2, 1));
        _fonts.put("Dom Casual", dom);

        // we want a normal and wide version of Old Town
        Font town = loadFont(ctx, "ui/fonts/oldtown.ttf");
        _fonts.put("Old Town", town);
        town = town.deriveFont(
            Font.PLAIN, AffineTransform.getScaleInstance(1.4, 1));
        _fonts.put("Old Town Wide", town);

        COUNTER_FONT = _fonts.get("Old Town").deriveFont(Font.BOLD, 72);
        MARQUEE_FONT = _fonts.get("Old Town").deriveFont(Font.PLAIN, 96);
        LOADING_FONT = _fonts.get("Dom Casual").deriveFont(Font.PLAIN, 24);
        DAMAGE_FONT = _fonts.get("Dom Casual").deriveFont(Font.PLAIN, 36);

        // load up our HTML stylesheet
        css = new BangStyleSheet();
        try {
            InputStream is =
                _ctx.getResourceManager().getResource("ui/html_style.css");
            css.loadRules(new InputStreamReader(is), null);
        } catch (Throwable t) {
            log.log(Level.WARNING, "Failed to load HTML style sheet.", t);
        }

        // create our BUI stylesheet
        reloadStylesheet();

        scripIcon = new ImageIcon(ctx.loadImage("ui/icons/scrip.png"));
        coinIcon = new ImageIcon(ctx.loadImage("ui/icons/coins.png"));

        leftArrow = new ImageIcon(ctx.loadImage("ui/icons/left_arrow.png"));
        rightArrow = new ImageIcon(ctx.loadImage("ui/icons/right_arrow.png"));

        // create our sound clip provider
        clipprov = new ClipProvider() {
            public Clip loadClip (String path) throws IOException {
                if (path.startsWith("rsrc/")) {
                    path = path.substring(5);
                }
                File file = _ctx.getResourceManager().getResourceFile(path);
                if (!file.exists()) {
                    throw new IOException(
                        "Missing sound resource '" + path + "'.");
                }
                if (path.endsWith(".ogg")) {
                    return loadOggClip(file);
                }
                
                // TODO: preconvert all of our sounds to a standard format and
                // then mmap() the sound files and stuff them directly into the
                // clip; WaveData does all sorts of expensive conversion
                Clip clip = new Clip();
                WaveData wfile = null;
                Exception cause = null;
                try {
                    wfile = WaveData.create(
                        AudioSystem.getAudioInputStream(file));
                } catch (Exception e) {
                    cause = e;
                }
                if (wfile == null) {
                    String err = "Error loading sound resource '" + path + "'.";
                    throw (IOException)new IOException(err).initCause(cause);
                }

                clip.format = wfile.format;
                clip.frequency = wfile.samplerate;
                clip.data = wfile.data;
                return clip;
            }
        };

        // create the sound group for our UI sounds
        _sgroup = _ctx.getSoundManager().createGroup(
            BangUI.clipprov, UI_SOURCE_COUNT);
    }

    /**
     * Configures our application icons (shown in the corner of the window and
     * in the task bar, etc.).
     */
    public static void configIcons ()
    {
        ByteBuffer[] icons = new ByteBuffer[ICON_SIZES.length];
        for (int ii = 0; ii < ICON_SIZES.length; ii++) {
            String path = "rsrc/ui/icons/" + ICON_SIZES[ii] + "_icon.png";
            try {
                // getdown has already unpacked our resources, so we can load
                // these images straight from the filesystem (this method gets
                // called before the resource manager and image cache are set
                // up, otherwise we'd use them)
                BufferedImage bicon =
                    ImageIO.read(BangUtil.getResourceFile(path));
                Image icon = ImageCache.createImage(bicon, false);
                icons[ii] = icon.getData();
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to load icon " +
                        "[path=" + path + "].", e);
                return;
            }
        }
        try {
            Display.setIcon(icons);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to set icons.", e);
        }
    }

    /**
     * Cleans up prior to the game exiting.
     */
    public static void shutdown ()
    {
        _sgroup.dispose();
    }

    /**
     * Plays the specified feedback sound.
     */
    public static void play (FeedbackSound ident)
    {
        Sound sound = _sounds.get(ident);
        if (sound == null) {
            sound = _sgroup.getSound(
                "sounds/feedback/" + ident.toString().toLowerCase() + ".ogg");
            _sounds.put(ident, sound);
        }
        if (sound != null) {
            sound.play(true);
        }
    }

    /**
     * Plays the entry sound for the specified shop.
     */
    public static void playShopEntry (String townId, String shoppe)
    {
        String tpath = "menu/" + townId + "/" + shoppe + ".ogg";
        if (!SoundUtil.haveSound(tpath)) {
            tpath = "menu/" + shoppe + ".ogg";
        }
        Sound sound = _sgroup.getSound(tpath);
        if (sound != null) {
            sound.play(true);
        }
    }

    /**
     * Reloads our interface stylesheet. This is used when testing.
     */
    public static void reloadStylesheet ()
    {
        BStyleSheet.ResourceProvider rp = new BStyleSheet.ResourceProvider() {
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
            public BImage loadImage (String path) throws IOException {
                return _ctx.getImageCache().getBImage(path);
            }
        };
        try {
            InputStream is =
                _ctx.getResourceManager().getResource("ui/style.bss");
            stylesheet =
                new BStyleSheet(new InputStreamReader(is, "UTF-8"), rp);
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to load stylesheet", ioe);
        }
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
        if (!label.getStyleClass().equals("unit_label")) {
            label.setStyleClass("unit_label");
        }
        if (config == null) {
            label.setText(_ctx.xlate("units", "m.empty"));
            label.setIcon(null);
        } else {
            label.setText(_ctx.xlate("units", config.getName()));
            label.setIcon(getUnitIcon(config));
        }
    }

    /**
     * Returns the icon that represents the specified unit.
     */
    public static BIcon getUnitIcon (UnitConfig config)
    {
        return new ImageIcon(
            _ctx.loadImage("units/" + config.type + "/icon.png"));
    }

    /**
     * Creates a label with the icon for the specified unit and the unit's
     * name displayed below.
     */
    public static BButton createUnitButton (UnitConfig config)
    {
        BButton button = new BButton(_ctx.xlate("units", config.getName()));
        button.setIcon(getUnitIcon(config));
        button.setOrientation(BButton.VERTICAL);
        button.setStyleClass("unit_label");
        return button;
    }

    /**
     * Creates a label with the specified label and style class, looking up the
     * label and <code>label_tip</code> in the supplied message bundle.
     */
    public static BLabel createLabel (
        MessageBundle msgs, String text, String style)
    {
        BLabel label = new BLabel(msgs.get(text), style);
        String tipkey = text + "_tip";
        if (msgs.exists(tipkey)) {
            label.setTooltipText(msgs.get(tipkey));
        }
        return label;
    }

    protected static Font loadFont (BasicContext ctx, String path)
    {
        Font font = null;
        int type = path.endsWith(".pfb") ? Font.TYPE1_FONT : Font.TRUETYPE_FONT;
        try {
            font = Font.createFont(
                type, ctx.getResourceManager().getResourceFile(path));
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load font '" + path + "'.", e);
            font = new Font("Dialog", Font.PLAIN, 16);
        }
        return font;
    }

    protected static Clip loadOggClip (File file)
        throws IOException
    {
        OggInputStream istream = new OggInputStream(new FileInputStream(file));
        Clip clip = new Clip();
        clip.format = (istream.getFormat() == OggInputStream.FORMAT_MONO16) ?
            AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
        clip.frequency = istream.getRate();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = istream.read(buffer, 0, buffer.length)) > 0) {
            out.write(buffer, 0, read);
        }
        byte[] bytes = out.toByteArray();
        clip.data = ByteBuffer.allocateDirect(bytes.length);
        clip.data.put(bytes);
        clip.data.rewind();
        return clip;
    }
    
    /** We use this to provide custom fonts in our HTML views. */
    protected static class BangStyleSheet extends StyleSheet
    {
        public Font getFont (AttributeSet attrs) {
            // Java's style sheet parser annoyingly looks up whatever is
            // supplied for font-family and if it doesn't map to an internal
            // Java font; it discards it. Thanks! So we do this hackery with
            // the font-variant which it passes through unmolested.
            String variant = (String)
                attrs.getAttribute(CSS.Attribute.FONT_VARIANT);
            if (variant != null) {
                Font base = _fonts.get(variant);
                if (base != null) {
                    int style = Font.PLAIN;
                    if (StyleConstants.isBold(attrs)) {
                        style |= Font.BOLD;
                    }
                    if (StyleConstants.isItalic(attrs)) {
                        style |= Font.ITALIC;
                    }
                    int size;
                    try {
                        size = StyleConstants.getFontSize(attrs);
                        if (StyleConstants.isSuperscript(attrs) ||
                            StyleConstants.isSubscript(attrs)) {
                            size -= 2;
                        }
                    } catch (Throwable t) {
                        log.log(Level.WARNING, "StyleConstants choked " +
                                "looking up size [font=" + variant +
                                ", attrs=" + attrs + "].", t);
                        size = 9;
                    }
                    return base.deriveFont(style, size);
                }
            }
            return super.getFont(attrs);
        }
    }

    protected static BasicContext _ctx;
    protected static MessageBundle _umsgs;
    protected static HashMap<String,Font> _fonts = new HashMap<String,Font>();

    protected static SoundGroup _sgroup;
    protected static HashMap<FeedbackSound,Sound> _sounds =
        new HashMap<FeedbackSound,Sound>();

    /** The number of simultaneous UI sounds allowed. */
    protected static final int UI_SOURCE_COUNT = 2;

    /** Sizes of icons we need. */
    protected static final int[] ICON_SIZES = {16, 32, 128};
}
