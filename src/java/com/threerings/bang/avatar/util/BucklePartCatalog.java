//
// $Id$

package com.threerings.bang.avatar.util;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.samskivert.util.RandomUtil;

/**
 * Contains metadata on each of our buckle parts.
 */
public class BucklePartCatalog
    implements Serializable
{
    /** The path (relative to the resource directory) at which the part
     * catalog should be loaded and stored. */
    public static final String CONFIG_PATH = "avatars/buckle_parts.dat";

    /** Contains all {@link Part} records for a particular class of parts. */
    public static class PartClass implements Serializable
    {
        /** The name of the class. */
        public String name;

        /** A mapping of names to the parts in this class. */
        public HashMap<String, Part> parts = new HashMap<String, Part>();

        /** A list of all the starter parts for this class. */
        public ArrayList<Part> starters = new ArrayList<Part>();

        /** Returns the name of the corresponding component class. */
        public String getComponentClass ()
        {
            return "buckle/" + name;
        }

        /** Used when parsing part definitions. */
        public void addPart (Part part)
        {
            part.pclass = this;
            parts.put(part.name, part);
            if (part.starter) {
                starters.add(part);
            }
        }

        /** Increase this value when object's serialized state is impacted by a
         * class change (modification of fields, inheritance). */
        private static final long serialVersionUID = 1;
    }

    /** Contains metadata on a particular part. */
    public static class Part implements Serializable
    {
        /** The name of this particular part. */
        public String name;

        /** The cost of the part in scrip. */
        public int scrip;

        /** The cost of the part in coins. */
        public int coins;

        /** The cost of the part in aces. */
        public int aces;

        /** The colorization classes used by this part. */
        public String[] colors;

        /** Whether or not this is a starter part. */
        public boolean starter;

        /** A reference to the part class. */
        public PartClass pclass;

        /** Increase this value when object's serialized state is impacted by a
         * class change (modification of fields, inheritance). */
        private static final long serialVersionUID = 1;
    }

    /**
     * Returns the collection of class names registered in the catalog.
     */
    public Collection<String> getClassNames ()
    {
        return _classes.keySet();
    }

    /**
     * Returns the collection of parts in the specified part class.
     * <code>null</code> is returned if no part class is registered with the
     * specified name.
     */
    public Collection<Part> getParts (String pclass)
    {
        PartClass pcrec = _classes.get(pclass);
        return (pcrec == null) ? new ArrayList<Part>() : pcrec.parts.values();
    }

    /**
     * Looks up and returns the specified part record, returning null if no
     * such part exists.
     */
    public Part getPart (String pclass, String part)
    {
        PartClass pcrec = _classes.get(pclass);
        return (pcrec == null) ? null : pcrec.parts.get(part);
    }

    /**
     * Returns a random starter part for the specified class.
     */
    public Part getStarter (String pclass)
    {
        PartClass pcrec = _classes.get(pclass);
        return (pcrec == null) ? null : RandomUtil.pickRandom(pcrec.starters);
    }

    /**
     * Adds a part class to this catalog instance.  Used in parsing.
     */
    public void addClass (PartClass pclass)
    {
        _classes.put(pclass.name, pclass);
    }

    /** A mapping of all registered part classes by name. */
    protected HashMap<String, PartClass> _classes = new HashMap<String, PartClass>();

    /** Increase this value when object's serialized state is impacted by a
     * class change (modification of fields, inheritance). */
    private static final long serialVersionUID = 1;
}
