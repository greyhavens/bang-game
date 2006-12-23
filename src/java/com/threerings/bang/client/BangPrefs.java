//
// $Id$

package com.threerings.bang.client;

import java.util.logging.Level;

import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.Display;

import com.jme.system.PropertiesIO;

import com.samskivert.util.Config;

import com.threerings.crowd.chat.client.CurseFilter;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * Contains client-side preferences.
 */
public class BangPrefs
{
    /** Graphical detail levels. */
    public enum DetailLevel {
        LOW, MEDIUM, HIGH
    };

    /** Contains our client-side preferences. */
    public static Config config = new Config("bang");

    /**
     * Returns the display mode in the supplied list that matches the one
     * configured as our preference, or null if there are no matches.
     */
    public static void configureDisplayMode (
            PropertiesIO props, boolean safeMode)
    {
        // first look up our "preferred" mode
        int width = safeMode ? 1024 : config.getValue("display_width", 1024);
        int height = safeMode ? 768 : config.getValue("display_height", 768);
        int bpp = safeMode ? 16 : config.getValue("display_bpp", 16);
        int freq = safeMode ? 60 : config.getValue("display_freq", 60);
        boolean fullscreen = safeMode ? true : isFullscreen();

        // if that is a full screen mode, we need to find the closest matching
        // available screen mode
        if (fullscreen) {
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
        props.set("FULLSCREEN", String.valueOf(fullscreen));
        props.set("RENDERER", "LWJGL");

        log.info("Display " + (safeMode ? "in safe mode: " : "mode: ") + 
                 props.getWidth() + "x" + props.getHeight() +
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
     * Returns the desired level of graphical detail.
     */
    public static DetailLevel getDetailLevel ()
    {
        return Enum.valueOf(DetailLevel.class,
            config.getValue("detail_level", "HIGH"));
    }

    /**
     * Determines whether the level of detail is at least medium.
     */
    public static boolean isMediumDetail ()
    {
        return getDetailLevel().compareTo(DetailLevel.MEDIUM) >= 0;
    }

    /**
     * Determines whether the level of detail is high.
     */
    public static boolean isHighDetail ()
    {
        return getDetailLevel() == DetailLevel.HIGH;
    }

    /**
     * Updates the desired level of graphical detail.
     */
    public static void updateDetailLevel (DetailLevel level)
    {
        config.setValue("detail_level", level.name());
    }

    /**
     * Checks whether the application should recommend changes to the graphical
     * detail level based on performance history.
     */
    public static boolean shouldSuggestDetail ()
    {
        return config.getValue("suggest_detail", true);
    }

    /**
     * Sets whether the application should recommend changes to the detail
     * level.
     */
    public static void setSuggestDetail (boolean suggest)
    {
        config.setValue("suggest_detail", suggest);
    }

    /**
     * Returns the volume of the music, a value from zero to one hundred.
     */
    public static int getMusicVolume ()
    {
        return config.getValue("music_volume", 50);
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
        return config.getValue("effects_volume", 100);
    }

    /**
     * Updates the volume of the sound effects, a value from zero to one
     * hundred.
     */
    public static void updateEffectsVolume (int volume)
    {
        config.setValue("effects_volume", volume);
    }

    /**
     * Returns the current chat filter mode.
     */
    public static CurseFilter.Mode getChatFilterMode ()
    {
        String dmode = CurseFilter.Mode.VERNACULAR.toString();
        return CurseFilter.Mode.valueOf(config.getValue("filter_mode", dmode));
    }

    /**
     * Configures the current chat filter mode.
     */
    public static void setChatFilterMode (CurseFilter.Mode mode)
    {
        config.setValue("filter_mode", mode.toString());
    }

    /**
     * Returns whether the chat mogrifier is enabled.
     */
    public static boolean getChatMogrifierEnabled ()
    {
        return config.getValue("mogrifier_enabled", true);
    }

    /**
     * Configures whether the chat mogrifier is enabled.
     */
    public static void setChatMogrifierEnabled (boolean enabled)
    {
        config.setValue("mogrifier_enabled", enabled);
    }

    /**
     * Returns the card palette size preference, true for small.
     */
    public static boolean getCardPaletteSize ()
    {
        return config.getValue("card_palette_size", false);
    }

    /**
     * updates the card palette size preference.
     */
    public static void updateCardPaletteSize (boolean small)
    {
        config.setValue("card_palette_size", small);
    }

    /**
     * Returns the unit status detail preference, true for showing.
     */
    public static boolean getUnitStatusDetails ()
    {
        return config.getValue("unit_status_details", true);
    }

    /**
     * Updates the unit stats detail preference.
     */
    public static void updateUnitStatusDetails (boolean details)
    {
        config.setValue("unit_status_details", details);
    }

    /**
     * Used to prevent the tutorials from automatically showing up once a user
     * has dismissed them the first time or completed the first two. This is
     * tracked per-town, so the a player will be shown the tutorial view again
     * the first time they visit a new town.
     */
    public static boolean shouldShowTutorials (PlayerObject user)
    {
        int townIdx = BangUtil.getTownIndex(user.townId);
        return !config.getValue(
            user.username + ".declined_tuts." + user.townId, false) &&
            !(user.stats.containsValue(Stat.Type.TUTORIALS_COMPLETED,
                                       TutorialCodes.TUTORIALS[townIdx][0]) &&
              user.stats.containsValue(Stat.Type.TUTORIALS_COMPLETED,
                                       TutorialCodes.TUTORIALS[townIdx][1]));
    }

    /**
     * Called when the user has dismissed the tutorial dialog instead of
     * choosing a tutorial. This marks the player as having declined the
     * tutorials for the town they are currently in.
     */
    public static void setDeclinedTutorials (PlayerObject user)
    {
        config.setValue(user.username + ".declined_tuts." + user.townId, true);
    }

    /**
     * Returns the id of the last town to which the specified user logged
     * on. If the user has never logged on, the default town (Frontier Town)
     * will be returned.
     */
    public static String getLastTownId (String username)
    {
        // avoid funny business if the user types their name in a strange case
        username = username.toLowerCase();
        return config.getValue(username + ".town_id", BangCodes.FRONTIER_TOWN);
    }

    /**
     * Stores the id of the town to which the specified user has connected so
     * we can go directly to that town next time.
     */
    public static void setLastTownId (String username, String townId)
    {
        // avoid funny business if the user types their name in a strange case
        username = username.toLowerCase();
        config.setValue(username + ".town_id", townId);
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
