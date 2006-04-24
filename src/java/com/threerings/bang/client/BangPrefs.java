//
// $Id$

package com.threerings.bang.client;

import java.util.logging.Level;

import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.Display;

import com.jme.system.PropertiesIO;

import com.samskivert.util.Config;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.game.data.TutorialCodes;

import static com.threerings.bang.Log.log;

/**
 * Contains client-side preferences.
 */
public class BangPrefs
{
    /** Contains our client-side preferences. */
    public static Config config = new Config("bang");

    /**
     * Returns the display mode in the supplied list that matches the one
     * configured as our preference, or null if there are no matches.
     */
    public static void configureDisplayMode (PropertiesIO props)
    {
        // first look up our "preferred" mode
        int width = config.getValue("display_width", 1024);
        int height = config.getValue("display_height", 768);
        int bpp = config.getValue("display_bpp", 16);
        int freq = config.getValue("display_freq", 60);

        // if that is a full screen mode, we need to find the closest matching
        // available screen mode
        if (isFullscreen()) {
            DisplayMode mode = getClosest(width, height, bpp, freq);
            if (mode == null) {
                mode = Display.getDisplayMode();
            }
            width = mode.getWidth();
            height = mode.getHeight();
            bpp = mode.getBitsPerPixel();
            freq = mode.getFrequency();

        } else {
            // otherwise we just need to sanitize the depth and frequency
            DisplayMode mode = Display.getDisplayMode();
            bpp = mode.getBitsPerPixel();
            freq = mode.getFrequency();
        }

        props.set("WIDTH", String.valueOf(width));
        props.set("HEIGHT", String.valueOf(height));
        props.set("DEPTH", String.valueOf(bpp));
        props.set("FREQ", String.valueOf(freq));
        props.set("FULLSCREEN", String.valueOf(isFullscreen()));
        props.set("RENDERER", "LWJGL");

        log.info("Display mode: " + props.getWidth() + "x" + props.getHeight() +
                 "x" + props.getDepth() + " " + props.getFreq() + "Hz " +
                 "(current: " + Display.getDisplayMode() + ").");
    }

    /**
     * Returns whether or not we prefer fullscreen mode.
     */
    public static boolean isFullscreen ()
    {
        return config.getValue("display_fullscreen", false);
    }

    /**
     * Stores our preferred display mode.
     */
    public static void updateDisplayMode (DisplayMode mode)
    {
        config.setValue("display_width", mode.getWidth());
        config.setValue("display_height", mode.getHeight());
        // see OptionsView for explanation for this hackery
        config.setValue("display_bpp", Math.max(mode.getBitsPerPixel(), 16));
        config.setValue("display_freq", mode.getFrequency());
    }

    /**
     * Updates our fullscreen preference.
     */
    public static void updateFullscreen (boolean fullscreen)
    {
        config.setValue("display_fullscreen", fullscreen);
    }

    /**
     * Returns the volume of the music, a value from zero to one hundred.
     */
    public static int getMusicVolume ()
    {
        return config.getValue("music_volume", 60);
    }

    /**
     * Updates the volume of the music, a value from zero to one hundred.
     */
    public static void updateMusicVolume (int volume)
    {
        config.setValue("music_volume", volume);
    }

    /**
     * Returns the volume of the sound effects, a value from zero to one
     * hundred.
     */
    public static int getEffectsVolume ()
    {
        return config.getValue("effects_volume", 80);
    }

    /**
     * Updates the volume of the sound effects, a value from zero to one
     * hundred.
     */
    public static void updateEffectsVolume (float volume)
    {
        config.setValue("effects_volume", volume);
    }

    /**
     * Used to prevent the tutorials from automatically showing up once a user
     * has dismissed them the first time or completed the first two.
     */
    public static boolean shouldShowTutorials (PlayerObject user)
    {
        return !config.getValue(user.username + ".declined_tuts", false) &&
            !(user.stats.containsValue(Stat.Type.TUTORIALS_COMPLETED,
                                       TutorialCodes.TUTORIALS[0]) &&
              user.stats.containsValue(Stat.Type.TUTORIALS_COMPLETED,
                                       TutorialCodes.TUTORIALS[1]));
    }

    /**
     * Called when the user has dismissed the tutorial dialog instead of
     * choosing a tutorial.
     */
    public static void setDeclinedTutorials (PlayerObject user)
    {
        config.setValue(user.username + ".declined_tuts", true);
    }

    /**
     * Returns the closest display mode to our specified default.
     */
    protected static DisplayMode getClosest (
        int width, int height, int depth, int freq)
    {
        DisplayMode c = null;
        try {
            DisplayMode[] modes = Display.getAvailableDisplayModes();
            for (int ii = 0; ii < modes.length; ii++) {
                DisplayMode m = modes[ii];
                // apparently LWJGL can't cope with >24 bpp
                if (m.getBitsPerPixel() > 24) {
                    continue;
                }
                if (c == null) {
                    c = m;
                } else if (closer(c.getWidth(), m.getWidth(), width)) {
                    c = m;
                } else if (closer(c.getHeight(), m.getHeight(), height)) {
                    c = m;
                } else if (closer(c.getBitsPerPixel(), m.getBitsPerPixel(),
                                  depth)) {
                    c = m;
                } else if (closer(c.getFrequency(), m.getFrequency(), freq)) {
                    c = m;
                }
            }
            return c;

        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to enumerate display modes.", e);
            return null;
        }
    }

    protected static boolean closer (int value, int ovalue, int tvalue)
    {
        return Math.abs(value-tvalue) > Math.abs(ovalue-tvalue);
    }
}
