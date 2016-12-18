//
// $Id$

package com.threerings.bang.avatar.util;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Contains metadata on each of our avatar aspects.
 */
public class AspectCatalog
    implements Serializable
{
    /** The path (relative to the resource directory) at which the aspect
     * catalog should be loaded and stored. */
    public static final String CONFIG_PATH = "avatars/aspects.dat";

    /** Contains all {@link Aspect} records for a particular class of avatar
     * aspects. */
    public static class AspectClass implements Serializable
    {
        /** The name of the class to which these constraints apply. */
        public String name;

        /** A mapping of names to the aspects in this class. */
        public HashMap<String,Aspect> aspects = new HashMap<String,Aspect>();

        /** Used when parsing constraints definitions. */
        public void addAspect (Aspect comp)
        {
            aspects.put(comp.name, comp);
        }

        @Override public String toString () {
            return name;
        }

        /** Increase this value when object's serialized state is impacted by a
         * class change (modification of fields, inheritance). */
        private static final long serialVersionUID = 1;
    }

    /** Contains metadata on a particular avatar aspect. */
    public static class Aspect implements Serializable
    {
        /** The name of this particular attire aspect. */
        public String name;

        /** The town in which this aspect is available. */
        public String townId;

        /** The scrip cost associated with this aspect. */
        public int scrip;

        /** The coin cost associated with this aspect. */
        public int coins;

        @Override public String toString () {
            return name;
        }

        /** Increase this value when object's serialized state is impacted by a
         * class change (modification of fields, inheritance). */
        private static final long serialVersionUID = 1;
    }

    /**
     * Returns an array containing all matching aspect class names registered
     * in this catalog.
     */
    public String[] enumerateClassNames (String prefix)
    {
        ArrayList<String> names = new ArrayList<String>();
        for (String name : _classes.keySet()) {
            if (name.startsWith(prefix)) {
                names.add(name);
            }
        }
        return names.toArray(new String[names.size()]);
    }

    /**
     * Returns the collection of aspects in the specified aspect class.
     * <code>null</code> is returned if no aspect class is registered with the
     * specified name.
     */
    public Collection<Aspect> getAspects (String aclass)
    {
        AspectClass ccrec = _classes.get(aclass);
        return (ccrec == null) ?
            new ArrayList<Aspect>() : ccrec.aspects.values();
    }

    /**
     * Looks up and returns the specified aspect record, returning null if no
     * such aspect exists.
     */
    public Aspect getAspect (String aclass, String aspect)
    {
        AspectClass ccrec = _classes.get(aclass);
        return (ccrec == null) ? null : ccrec.aspects.get(aspect);
    }

    /**
     * Adds a parsed aspect class to this constraints instance. This is used
     * when parsing aspect constraint definitions.
     */
    public void addClass (AspectClass aclass)
    {
        _classes.put(aclass.name, aclass);
    }

    /** A mapping of all registered aspect classes by name. */
    protected HashMap<String,AspectClass> _classes =
        new HashMap<String,AspectClass>();

    /** Increase this value when object's serialized state is impacted by a
     * class change (modification of fields, inheritance). */
    private static final long serialVersionUID = 1;
}
