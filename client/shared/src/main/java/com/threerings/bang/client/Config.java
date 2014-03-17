//
// $Id$

package com.threerings.bang.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.prefs.Preferences;

import com.threerings.bang.client.util.PerfMonitor;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Contains all runtime tweakable parameters. These parameters are wired
 * up to an in-game editor to allow their modification at runtime for
 * development and tuning.
 */
public class Config
{
    /** Used when we bind configuration values to user interface elements. */
    public static interface Hook
    {
        public void valueUpdated ();
    }

    /** Controls the overall speed of the game animations. */
    public static float animationSpeed = 1f;

    /** The speed (in tiles per second) of unit movement. */
    public static float movementSpeed = 4f;

    /** Whether or not move highlights float above pieces. */
    public static boolean floatHighlights = true;

    /** Whether or not move highlights will flatten when over 
     * non-traversable terrain. */
    public static boolean flattenHighlights = true;

    /** Whether or not display lists are activated. */
    public static boolean useDisplayLists = true;

    /** Whether or not VBOs are activated. */
    public static boolean useVBOs = true;

    /** Whether or not to display render statistics. */
    public static boolean displayStats = false;

    /** Whether or not to display sky geometry. */
    public static boolean displaySky = true;

    /** Whether or not to display terrain geometry. */
    public static boolean displayTerrain = true;

    /** Whether or not to display water geometry. */
    public static boolean displayWater = true;

    /** Whether or not to display model geometry. */
    public static boolean displayModels = true;

    /** Whether or not to display unit geometry. */
    public static boolean displayUnits = true;

    /** Returns the unit movement speed modulated by the total
     * animation speed. */
    public static float getMovementSpeed ()
    {
        return movementSpeed * animationSpeed;
    }

    /**
     * Initializes our various configuration hooks.
     */
    public static void init (final BasicContext ctx)
    {
        // load our persistent preferences
        Field[] fields = Config.class.getDeclaredFields();
        for (int ii = 0; ii < fields.length; ii++) {
            Field field = fields[ii];
            if ((field.getModifiers() & Modifier.STATIC) == 0 ||
                (field.getModifiers() & Modifier.PUBLIC) == 0) {
                continue;
            }

            Class<?> ftype = field.getType();
            try {
                if (ftype.equals(Boolean.TYPE)) {
                    boolean val = field.getBoolean(null);
                    val = _prefs.getBoolean(field.getName(), val);
                    field.setBoolean(null, val);
                } else if (ftype.equals(Float.TYPE)) {
                    float val = field.getFloat(null);
                    field.setFloat(null, _prefs.getFloat(field.getName(), val));
                } else if (ftype.equals(Integer.TYPE)) {
                    int val = field.getInt(null);
                    field.setInt(null, _prefs.getInt(field.getName(), val));
                } else {
                    log.warning("Unhandled config field type", "field", field.getName(),
                                "type", ftype);
                }

            } catch (Exception e) {
                log.warning("Failed to configure field from prefs", "field", field.getName(),
                            "type", ftype, e);
            }
        }

        // register some internal hooks
        registerHook("displayStats", new Hook() {
            public void valueUpdated () {
                // ctx.getApp().displayStatistics(displayStats);
                PerfMonitor.setReportToChat(displayStats);
            }
        });
    }

    /**
     * Registers a hook to be called when the specified runtime configuration
     * field is updated. The hook will be triggered during this call to ensure
     * it is run with the currently configured value.
     */
    public static void registerHook (String field, Hook hook)
    {
        // register the hook
        ArrayList<Hook> hooks = _hooks.get(field);
        if (hooks == null) {
            _hooks.put(field, hooks = new ArrayList<Hook>());
        }
        hooks.add(hook);

        // trigger it to ensure everything is properly set to start
        hook.valueUpdated();
    }

    /**
     * Called by the {@link ConfigEditorView} to update our configuration
     * values and run their associated hooks.
     */
    protected static void updateValue (Field field, Object value)
    {
        String name = field.getName();

        // update the in memory value
        try {
            field.set(null, value);
        } catch (Exception e) {
            log.warning("Failed to updated config field", "field", name, "value", value, e);
            return;
        }

        // update our persistent preference
        if (value instanceof Boolean) {
            _prefs.putBoolean(name, (Boolean)value);
        } else if (value instanceof Integer) {
            _prefs.putInt(name, (Integer)value);
        } else if (value instanceof Float) {
            _prefs.putFloat(name, (Float)value);
        } else {
            log.warning("Requested to update config field with value of unhandled type",
                        "field", name, "type", value.getClass().getName(), "value", value);
        }

        // run any associated hooks
        ArrayList<Hook> hooks = _hooks.get(name);
        if (hooks != null) {
            for (Hook hook : hooks) {
                hook.valueUpdated();
            }
        }
    }

    /** We use these to persist settings between invocations. */
    protected static Preferences _prefs =
        Preferences.userNodeForPackage(Config.class);

    /** Used to register hooks that are run when configuration values change. */
    protected static HashMap<String,ArrayList<Hook>> _hooks =
        new HashMap<String,ArrayList<Hook>>();
}
