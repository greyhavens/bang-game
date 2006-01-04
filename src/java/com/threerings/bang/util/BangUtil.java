//
// $Id$

package com.threerings.bang.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.zip.CRC32;

import com.samskivert.util.StringUtil;

import static com.threerings.bang.Log.log;

/**
 * Straight up random utility methods. We ain't like no body.
 */
public class BangUtil
{
    /**
     * Loads up a resource file and parses its contents as an array of
     * strings. If the file cannot be loaded, an error will be logged and
     * a zero length array will be returned.
     */
    public static String[] resourceToStrings (String path)
    {
        ArrayList<String> lines = new ArrayList<String>();
        try {
            InputStream in =
                BangUtil.class.getClassLoader().getResourceAsStream(path);
            if (in != null) {
                BufferedReader bin =
                    new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = bin.readLine()) != null) {
                    lines.add(line);
                }
            } else {
                log.warning("Missing resource [path=" + path + "].");
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to read resource file " +
                    "[path=" + path + "].", e);
        }

        return lines.toArray(new String[lines.size()]);
    }

    /**
     * Loads up a resource file and parses its contents into a {@link
     * Properties} instance. If the file cannot be loaded, an error will
     * be logged and an empty properties object will be returned.
     */
    public static Properties resourceToProperties (String path)
    {
        return resourceToProperties(path, new ArrayList<String>());
    }

    /** Helper function for {@link #resourceToProperties}. */
    protected static Properties resourceToProperties (
        String path, ArrayList<String> history)
    {
        Properties props = new Properties();
        if (history.contains(path)) {
            log.warning("Detected loop in properties inheritance " +
                        "[path=" + path +
                        ", history=" + StringUtil.toString(history) + "].");
            return props;
        }
        history.add(path);

        try {
            InputStream in =
                BangUtil.class.getClassLoader().getResourceAsStream(path);
            if (in != null) {
                props.load(in);
            } else {
                log.warning("Missing resource [path=" + path + "].");
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to read resource file " +
                    "[path=" + path + "].", e);
        }

        // if this properties file extends another file, load it up and
        // overlay it onto this file
        String ppath = props.getProperty("extends");
        if (!StringUtil.isBlank(ppath)) {
            Properties parent = resourceToProperties(ppath, history);
            Enumeration iter = parent.propertyNames();
            while (iter.hasMoreElements()) {
                String key = (String)iter.nextElement();
                if (props.getProperty(key) == null) {
                    props.setProperty(key, parent.getProperty(key));
                }
            }
        }

        return props;
    }

    /**
     * Extracts a property from a properties instance, logging a warning
     * if it is missing.
     */
    public static String requireProperty (
        String type, Properties props, String key)
    {
        String value = props.getProperty(key);
        if (value == null) {
            log.warning("Missing config [type=" + type +
                        ", key=" + key + "].");
            value = "";
        }
        return value;
    }

    /**
     * Extracts and converts an integer property from a properties
     * instance, logging a warning if it is missing or invalid.
     */
    public static int getIntProperty (
        String type, Properties props, String key, int defval)
    {
        String value = props.getProperty(key);
        try {
            return (value != null) ? Integer.parseInt(value) : defval;
        } catch (Exception e) {
            log.warning("Invalid config [type=" + type +
                        ", key=" + key + ", value=" + value + "]: " + e);
            return defval;
        }
    }

    /**
     * Extracts and converts a float property from a properties instance,
     * logging a warning if it is missing or invalid.
     */
    public static float getFloatProperty (
        String type, Properties props, String key, float defval)
    {
        String value = props.getProperty(key);
        try {
            return (value != null) ? Float.parseFloat(value) : defval;
        } catch (Exception e) {
            log.warning("Invalid config [type=" + type +
                        ", key=" + key + ", value=" + value + "]: " + e);
            return defval;
        }
    }

    /**
     * Computes and returns the CRC32 hash value for the supplied string.
     */
    public static int crc32 (String value)
    {
        _crc.reset();
        _crc.update(value.getBytes());
        return (int)_crc.getValue();
    }

    protected static CRC32 _crc = new CRC32();
}
