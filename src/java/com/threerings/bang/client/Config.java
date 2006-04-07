//
// $Id$

package com.threerings.bang.client;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.prefs.Preferences;

import com.jmex.bui.BCheckBox;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.io.Streamable;

import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Contains all runtime tweakable parameters. These parameters are wired
 * up to an in-game editor to allow their modification at runtime for
 * development and tuning.
 */
public class Config
{
    /** Parameters relating to the display and user interface. */
    public static class Display implements Streamable
    {
        // Note: this class is not actually sent over the network, we just
        // implement streamable to prevent Proguard from renaming our fields
        // which we lookup by name.

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

        /** Whether or not to display render statistics. */
        public boolean displayStats = false;

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
     * Initializes our various configuration hooks.
     */
    public static void init (final BasicContext ctx)
    {
        // load our persistent preferences
        display.useDisplayLists = _prefs.getBoolean(
            "useDisplayLists", display.useDisplayLists);
        display.useVBOs = _prefs.getBoolean(
            "useVBOs", display.useVBOs);
        display.displayStats = _prefs.getBoolean(
            "displayStats", display.displayStats);

        // register our various hooks
        Hook hook = new Hook() {
            public void valueUpdated () {
                ctx.getApp().displayStatistics(display.displayStats);
            }
        };
        hook.valueUpdated();
        _hooks.put("displayStats", hook);
    }

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
                        // update the in memory value
                        field.setBoolean(config, checkbox.isSelected());
                        // update our persistent preference
                        _prefs.putBoolean(
                            field.getName(), checkbox.isSelected());
                        // run any associated hook
                        Hook hook = _hooks.get(field.getName());
                        if (hook != null) {
                            hook.valueUpdated();
                        }
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

    /** Used when we bind configuration values to user interface elements. */
    protected static interface Hook
    {
        public void valueUpdated ();
    }

    /** We use these to persist settings between invocations. */
    protected static Preferences _prefs =
        Preferences.userNodeForPackage(Config.class);

    /** Used to register hooks that are run when configuration values change. */
    protected static HashMap<String,Hook> _hooks = new HashMap<String,Hook>();
}
