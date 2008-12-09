//
// $Id$

package com.threerings.bang.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.CRC32;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.bang.data.BangCodes;

import static com.threerings.bang.Log.log;

/**
 * Straight up random utility methods. We ain't like no body.
 */
public class BangUtil
{
    /**
     * Returns a file instance given the supplied path (starting with
     * <code>rsrc/</code>) to an unpacked resource file. The path should
     * contain / as a file separator and that will be converted to the OS's
     * file separator prior to lookup.
     */
    public static File getResourceFile (String path)
    {
        path = path.replace("/", File.separator);
        String appdir = System.getProperty("appdir");
        if (!StringUtil.isBlank(appdir)) {
            path = appdir + File.separator + path;
        }
        return new File(path);
    }

    /**
     * Loads up a resource file and parses its contents as an array of
     * strings. If the file cannot be loaded, an error will be logged and
     * a zero length array will be returned.
     *
     * <p><em>Note:</em> the resource file is first located via the classpath
     * and if that fails, we look for a resource file.
     */
    public static String[] resourceToStrings (String path)
    {
        ArrayList<String> lines = new ArrayList<String>();
        BufferedReader bin = null;
        InputStream in = getResourceAsStream(path);
        try {
            if (in != null) {
                bin = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = bin.readLine()) != null) {
                    lines.add(line);
                }
            } else {
                log.warning("Missing resource", "path", path);
            }

        } catch (Exception e) {
            log.warning("Failed to read resource file", "path", path, e);

        } finally {
            StreamUtil.close(in);
        }

        return lines.toArray(new String[lines.size()]);
    }

    /**
     * Loads up a resource file and parses its contents into a {@link
     * Properties} instance. If the file cannot be loaded, an error will
     * be logged and an empty properties object will be returned.
     *
     * <p><em>Note:</em> the resource file is first located via the classpath
     * and if that fails, we look for a resource file.
     */
    public static Properties resourceToProperties (String path)
    {
        return resourceToProperties(path, new ArrayList<String>());
    }

    /**
     * Loads up per-town resource files and parses their contents as an array
     * of strings.
     *
     * <p><em>Note:</em> the resource files are assumed to be unpacked into the
     * application installation directory.
     *
     * @param path the path to the resource files, containing the string
     * <code>TOWN</code> where the town id should be substituted.
     */
    public static String[] townResourceToStrings (String path)
    {
        ArrayList<String> lines = new ArrayList<String>();
        for (String townId : BangCodes.TOWN_IDS) {
            String tpath = path.replace("TOWN", townId);
            InputStream in = getResourceAsStream(tpath);
            if (in == null) {
                continue;
            }

            try {
                BufferedReader bin =
                    new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = bin.readLine()) != null) {
                    lines.add(line);
                }
                bin.close();

            } catch (Exception e) {
                log.warning("Failed to read resource file", "path", path, e);
            }
        }

        return lines.toArray(new String[lines.size()]);
    }

    /** Helper function for {@link #resourceToProperties}. */
    protected static Properties resourceToProperties (
        String path, ArrayList<String> history)
    {
        Properties props = new Properties();
        if (history.contains(path)) {
            log.warning("Detected loop in properties inheritance", "path", path,
                        "history", StringUtil.toString(history));
            return props;
        }
        history.add(path);

        InputStream in = getResourceAsStream(path);
        try {
            if (in != null) {
                props.load(in);
                in.close();
            } else {
                log.warning("Missing resource", "path", path);
                Thread.dumpStack();
            }

        } catch (Exception e) {
            log.warning("Failed to read resource file", "path", path, e);

        } finally {
            StreamUtil.close(in);
        }

        // if this properties file extends another file, load it up and
        // overlay it onto this file
        String ppath = props.getProperty("extends");
        if (!StringUtil.isBlank(ppath)) {
            Properties parent = resourceToProperties(ppath, history);
            Enumeration<?> iter = parent.propertyNames();
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
     * Locates a resource, either via the classpath or from a resource file and
     * returns an input stream to its contents. Returns null if the resource
     * could not be found.
     */
    protected static InputStream getResourceAsStream (String path)
    {
        try {
            InputStream in =
                BangUtil.class.getClassLoader().getResourceAsStream(path);
            if (in != null) {
                return in;
            }
            File file = getResourceFile(path);
            if (file.exists()) {
                return new FileInputStream(file);
            }
        } catch (Exception e) {
            log.warning("Failed to look up resource", "path", path, e);
        }
        return null;
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
            log.warning("Missing config", "type", type, "key", key);
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
            log.warning("Invalid config", "type", type, "key", key, "value", value, "error", e);
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
            log.warning("Invalid config", "type", type, "key", key, "value", value, "error", e);
            return defval;
        }
    }

    /**
     * Extracts and converts a boolean property from a properties instance.
     */
    public static boolean getBooleanProperty (
        String type, Properties props, String key, boolean defval)
    {
        String value = props.getProperty(key);
        return (value != null) ? Boolean.parseBoolean(value) : defval;
    }

    /**
     * Extracts and converts an enum property from a properties instance.
     */
    public static <T extends Enum<T>> T getEnumProperty (
        String type, Properties props, String key, T defval)
    {
        String value = props.getProperty(key);
        T evalue = null;
        if (value != null) {
            @SuppressWarnings("unchecked") Class<T> etype = (Class<T>)defval.getClass();
            try {
                evalue = Enum.valueOf(etype, StringUtil.toUSUpperCase(value));
            } catch (Exception e) {
                log.warning("Invalid enum config", "type", type, "key", key, "value", value);
            }
        }
        return (evalue != null) ? evalue : defval;
    }

    /**
     * Returns the index of the specified town or -1 if the supplied town id is
     * invalid.
     */
    public static int getTownIndex (String townId)
    {
        return ListUtil.indexOf(BangCodes.TOWN_IDS, townId);
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
