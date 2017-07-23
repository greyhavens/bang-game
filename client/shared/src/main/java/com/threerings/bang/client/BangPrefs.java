//
// $Id$

package com.threerings.bang.client;

import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.Display;

import com.jme.system.JmeException;
import com.jme.system.PropertiesIO;

import com.samskivert.util.PrefsConfig;
import com.samskivert.util.StringUtil;

import com.threerings.crowd.chat.client.CurseFilter;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;

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
    public static PrefsConfig config = new PrefsConfig("bang");

    /**
     * Returns true if no logon information is set.
     */
    public static boolean firstTimeUser ()
    {
        return (StringUtil.isBlank(BangPrefs.config.getValue("username", "")) &&
                StringUtil.isBlank(BangPrefs.config.getValue("anonymous", "")));
    }

    /**
     * Configures props with a display setting.
     *
     * @throws JmeException if a valid displaymode cannot be found
     */
    public static void configureDisplayMode (PropertiesIO props, boolean safeMode)
        throws JmeException
    {
        // first look up our "preferred" mode
        int width = safeMode ? BangUI.MIN_WIDTH :
                config.getValue("display_width", BangUI.MIN_WIDTH);
        int height = safeMode ? BangUI.MIN_HEIGHT :
                config.getValue("display_height", BangUI.MIN_HEIGHT);
        int bpp = safeMode ? 16 : config.getValue("display_bpp", 16);
        int freq = safeMode ? 60 : config.getValue("display_freq", 60);
        boolean fullscreenSet = isFullscreenSet();
        boolean fullscreen = safeMode ? true : isFullscreen();

        if (!fullscreen) {
            DisplayMode mode = Display.getDisplayMode();
            // if the display mode is too small, we'll try to go fullscreen (if this is their
            // first time then force fullscreen when the display mode is the minimum)
            if (mode.getWidth() < BangUI.MIN_WIDTH || mode.getHeight() < BangUI.MIN_HEIGHT ||
                    mode.getBitsPerPixel() < BangUI.MIN_BPP || (!fullscreenSet &&
                        (mode.getWidth() <= BangUI.MIN_WIDTH ||
                         mode.getHeight() <= BangUI.MIN_HEIGHT))) {
                fullscreen = true;
                updateFullscreen(true);

            // otherwise we just need to sanitize the depth and frequency
            } else {
                bpp = mode.getBitsPerPixel();
                freq = mode.getFrequency();
            }
        }

        // if this is a full screen mode, we need to find the closest matching
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
        }
        if (width < BangUI.MIN_WIDTH || height < BangUI.MIN_HEIGHT) {
            throw new JmeException("Cannot find display mode which meats the minimum dimensions [" +
                    "found (" + width + "x" + height + ") requires (" + BangUI.MIN_WIDTH +
                    "x" + BangUI.MIN_HEIGHT + ")]");
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

    public static int getDisplayWidth () {
        return config.getValue("display_width", BangUI.MIN_WIDTH);
    }
    public static int getDisplayHeight () {
        return config.getValue("display_height", BangUI.MIN_HEIGHT);
    }
    public static int getDisplayBPP () {
        return config.getValue("display_bpp", 32);
    }
    public static int getDisplayFreq () {
        return config.getValue("display_freq", 60);
    }

    /**
     * Returns whether or not we prefer fullscreen mode.
     */
    public static boolean isFullscreen ()
    {
        return config.getValue("display_fullscreen", false);
    }

    /**
     * Returns whether there is a preference for fullscreen mode.
     */
    public static boolean isFullscreenSet ()
    {
        return config.getValue("display_fullscreen", (String)null) != null;
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
     * Returns true if a detail level is set.
     */
    public static boolean isDetailSet ()
    {
        return config.getValue("detail_level", (String)null) != null;
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
     * Checks whether the application should recommend changes to the graphical detail level based
     * on performance history.
     */
    public static boolean shouldSuggestDetail ()
    {
        return config.getValue("suggest_detail", true);
    }

    /**
     * Sets whether the application should recommend changes to the detail level.
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
     * Returns the volume of the sound effects, a value from zero to one hundred.
     */
    public static int getEffectsVolume ()
    {
        return config.getValue("effects_volume", 100);
    }

    /**
     * Updates the volume of the sound effects, a value from zero to one hundred.
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
     * Used to prevent the Where To view from automatically showing up once a user has requested it
     * not be auto-shown. This is tracked per-town, so the a player will be shown the view again
     * the first time they visit a new town.
     */
    public static boolean shouldShowWhereTo (PlayerObject user)
    {
        return !config.getValue(user.username + ".no_where." + user.townId, false);
    }

    /**
     * Called when the user has requested not to show the Where To view.
     */
    public static void setNoWhereTo (PlayerObject user)
    {
        config.setValue(user.username + ".no_where." + user.townId, true);
    }

    /**
     * Used to prevent the Tutorial Intro view from automatically showing up once a user has
     * requested it not be auto-shown. This is tracked per-town, so the a player will be shown the
     * view again the first time they visit a new town.
     */
    public static boolean shouldShowTutIntro (PlayerObject user)
    {
        return !config.getValue(user.username + ".tut_intro." + user.townId, false);
    }

    /**
     * Called when the user has requested not to show the Tutorial Intro View.
     */
    public static void setNoTutIntro (PlayerObject user)
    {
        config.setValue(user.username + ".tut_intro." + user.townId, true);
    }

    /**
     * Used to prevent a shop popup from showing upon entering said shop.
     */
    public static boolean shouldShowShopPopup (PlayerObject user, String shop)
    {
        return !config.getValue(user.username + ".shop_popup." + shop, false);
    }

    /**
     * Called when the user has reqeusted not to show the shop popup.
     */
    public static void setNoShopPopup (PlayerObject user, String shop)
    {
        config.setValue(user.username + ".shop_popup." + shop, true);
    }

    /**
     * Check if we should popup our free ticket details window.
     */
    public static boolean shouldShowPassDetail (PlayerObject user, String townId)
    {
        return config.getValue(user.username + ".free_ticket." + townId, true);
    }

    /**
     * Called when the user doesn't want to be reminded of their free ticket.
     */
    public static void setNoRemind (PlayerObject user, String townId)
    {
        config.setValue(user.username + ".free_ticket." + townId, false);
    }

    /**
     * Check if we should show the leaving game early warnings.
     */
    public static boolean shouldShowQuitterWarning (PlayerObject user)
    {
        return !config.getValue(user.username + ".quitter_warning", false);
    }

    /**
     * Called when the user doesn't want to be warned about leaving early.
     */
    public static void setNoQuitterWarning (PlayerObject user)
    {
        config.setValue(user.username + ".quitter_warning", true);
    }

    /**
     * Returns the id of the last town to which the specified user logged on. If the user has never
     * logged on, the default town (Frontier Town) will be returned.
     */
    public static String getLastTownId (String username)
    {
        // avoid funny business if the user types their name in a strange case
        username = username.toLowerCase();
        return config.getValue(username + ".town_id", BangCodes.FRONTIER_TOWN);
    }

    /**
     * Stores the id of the town to which the specified user has connected so we can go directly to
     * that town next time.
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
    protected static DisplayMode getClosest (int width, int height, int depth, int freq)
    {
        DisplayMode c = null;
        try {
            DisplayMode[] modes = Display.getAvailableDisplayModes();
            for (int ii = 0; ii < modes.length; ii++) {
                DisplayMode m = modes[ii];
                // apparently LWJGL can't cope with >24 bpp
                if (m.getBitsPerPixel() > 24 || m.getWidth() < BangUI.MIN_WIDTH ||
                        m.getHeight() < BangUI.MIN_HEIGHT) {
                    continue;
                }
                if (c == null) {
                    c = m;
                } else if (closer(c.getWidth(), m.getWidth(), width)) {
                    c = m;
                } else if (closer(c.getHeight(), m.getHeight(), height)) {
                    c = m;
                } else if (closer(c.getBitsPerPixel(), m.getBitsPerPixel(), depth)) {
                    c = m;
                } else if (closer(c.getFrequency(), m.getFrequency(), freq)) {
                    c = m;
                }
            }
            return c;

        } catch (Exception e) {
            log.warning("Unable to enumerate display modes.", e);
            return null;
        }
    }

    protected static boolean closer (int value, int ovalue, int tvalue)
    {
        return Math.abs(value-tvalue) > Math.abs(ovalue-tvalue);
    }
}
