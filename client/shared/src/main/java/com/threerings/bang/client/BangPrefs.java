//
// $Id$

package com.threerings.bang.client;

import com.jme3.system.AppSettings;

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

    /** Dev convenience: audio is audible by default; pass <code>-Dmute=true</code> to start the
     * client MUTED (music and effects volumes report 0) so repeated test launches don't blast
     * sound. Does not touch the persisted volume prefs. */
    public static final boolean SILENT = Boolean.getBoolean("mute");

    /**
     * Returns true if no logon information is set.
     */
    public static boolean firstTimeUser ()
    {
        return (StringUtil.isBlank(BangPrefs.config.getValue("username", "")) &&
                StringUtil.isBlank(BangPrefs.config.getValue("anonymous", "")));
    }

    /**
     * Configures the supplied jME3 {@link AppSettings} with the preferred display settings
     * (width/height/bpp/frequency/fullscreen) read from our preferences.
     *
     * <p>jME3 cutover: the fork wrote into a {@code com.jme.system.PropertiesIO} and queried the
     * LWJGL2 {@code org.lwjgl.opengl.Display} to discover/sanitize the current mode (throwing the
     * fork {@code JmeException}). Mode <em>enumeration/validation</em> against the live display is
     * a host concern: at launch the GLFW context/window does not exist yet, so there is nothing to
     * query — we only seed the AppSettings from stored preferences (clamped to the minimum playable
     * size). The live monitor-mode resolution (a sane fullscreen target, the nearest-mode picker)
     * happens at runtime against the LWJGL3 context in {@link OptionsView} / {@link
     * com.threerings.jme.JmeApp#getPrimaryMonitorMode}, where the context is up.
     */
    public static void configureDisplayMode (AppSettings settings, boolean safeMode)
    {
        int width = safeMode ? BangUI.MIN_WIDTH :
                config.getValue("display_width", BangUI.MIN_WIDTH);
        int height = safeMode ? BangUI.MIN_HEIGHT :
                config.getValue("display_height", BangUI.MIN_HEIGHT);
        int bpp = safeMode ? 16 : config.getValue("display_bpp", 16);
        int freq = safeMode ? 60 : config.getValue("display_freq", 60);
        // safe mode recovers to the minimum windowed size (fullscreen off) so a bad stored
        // fullscreen/resolution can't lock the user out.
        boolean fullscreen = safeMode ? false : isFullscreen();

        int w = Math.max(width, BangUI.MIN_WIDTH);
        int h = Math.max(height, BangUI.MIN_HEIGHT);
        settings.setResolution(w, h);
        settings.setWindowSize(w, h);
        settings.setBitsPerPixel(bpp);
        settings.setFrequency(freq);
        settings.setFullscreen(fullscreen);

        log.info("Display " + (safeMode ? "in safe mode: " : "mode: ") +
                 settings.getWidth() + "x" + settings.getHeight() +
                 "x" + bpp + " " + freq + "Hz" + (fullscreen ? " fullscreen" : "") + ".");
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
     *
     * <p>jME3 cutover: was {@code updateDisplayMode(org.lwjgl.opengl.DisplayMode)}; retyped to
     * host-neutral primitives so the LWJGL2 {@code DisplayMode} type does not leak into the
     * preferences layer. The Phase-3 host (LWJGL3/GLFW) supplies these values.
     */
    public static void updateDisplayMode (int width, int height, int bpp, int freq)
    {
        config.setValue("display_width", width);
        config.setValue("display_height", height);
        // see OptionsView for explanation for this hackery
        config.setValue("display_bpp", Math.max(bpp, 16));
        config.setValue("display_freq", freq);
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
        // dev convenience: launch with -Dmute=true to start muted (does not persist)
        return SILENT ? 0 : config.getValue("music_volume", 50);
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
        // dev convenience: launch with -Dmute=true to start muted (does not persist)
        return SILENT ? 0 : config.getValue("effects_volume", 100);
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

    // jME3 cutover: the fork's getClosest()/closer() picked the nearest available LWJGL2
    // DisplayMode from Display.getAvailableDisplayModes(). Mode enumeration is a host concern: the
    // LWJGL3/GLFW context exposes the primary-monitor native mode via JmeApp.getPrimaryMonitorMode
    // (a fork-free jME3 Displays read), and OptionsView offers a curated windowed-resolution list
    // clamped to MIN_WIDTH/MIN_HEIGHT for the resolution chooser.
}
