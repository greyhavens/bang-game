//
// $Id$

package com.threerings.bang.client;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import com.jmex.bui.BCheckBox;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import static com.threerings.bang.Log.log;

/**
 * Contains all runtime tweakable parameters. These parameters are wired
 * up to an in-game editor to allow their modification at runtime for
 * development and tuning.
 */
public class Config
{
    /** Parameters relating to the display and user interface. */
    public static class Display
    {
        /** Controls the overall speed of the game animations. */
        public float animationSpeed = 1f;

        /** The speed (in tiles per second) of unit movement. */
        public float movementSpeed = 4f;

        /** Whether or not move highlights float above pieces. */
        public boolean floatHighlights = true;

        /** Whether or not display lists are activated. */
        public boolean useDisplayLists = true;

        /** Whether or not VBOs are activated. */
        public boolean useVBOs = true;

        /** Returns the unit movement speed modulated by the total
         * animation speed. */
        public float getMovementSpeed ()
        {
            return movementSpeed * animationSpeed;
        }

        /** Updates and persists the {@link #useDisplayLists} setting. */
        public void setUseDisplayLists (boolean useDisplayLists)
        {
            this.useDisplayLists = useDisplayLists;
            _prefs.putBoolean("use_display_lists", useDisplayLists);
        }

        /** Updates and persists the {@link #useVBOs} setting. */
        public void setUseVBOs (boolean useVBOs)
        {
            this.useVBOs = useVBOs;
            _prefs.putBoolean("use_vbos", useVBOs);
        }
    }

    /** Contains display configuration parameters. */
    public static Display display = new Display();

    /**
     * Binds the supplied checkbox to the specified boolean configuration
     * parameter.
     */
    public static void bindComponent (
        final BCheckBox checkbox, final Object config, String fname)
    {
        try {
            final Field field = config.getClass().getField(fname);
            checkbox.setSelected(field.getBoolean(config));
            checkbox.addListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    try {
                        field.setBoolean(config, checkbox.isSelected());
                        _prefs.putBoolean(
                            field.getName(), checkbox.isSelected());
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Failed to update config " +
                                "[config=" + config.getClass().getName() +
                                ", field=" + field.getName() + "].", e);
                    }
                }
            });
        } catch (Exception e) {
            log.log(Level.WARNING, "Requested to bind component to invalid " +
                    "config field [config=" + config.getClass().getName() +
                    ", field=" + fname + "].", e);
        }
    }

    /** We use these to persist settings between invocations. */
    protected static Preferences _prefs =
        Preferences.userNodeForPackage(Config.class);

    static {
        // load our persistent preferences
        display.useDisplayLists = _prefs.getBoolean("useDisplayLists", true);
        display.useVBOs = _prefs.getBoolean("useVBOs", true);
    }
}
