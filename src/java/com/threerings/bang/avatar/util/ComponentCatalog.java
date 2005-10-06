//
// $Id$

package com.threerings.bang.avatar.util;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static com.threerings.bang.Log.log;

/**
 * Contains metadata on each of our avatar components.
 */
public class ComponentCatalog
    implements Serializable
{
    /** The path (relative to the resource directory) at which the component
     * catalog should be loaded and stored. */
    public static final String CONFIG_PATH = "avatar/components.dat";

    /** Contains all {@link Component} records for a particular class of avatar
     * components. */
    public static class ComponentClass implements Serializable
    {
        /** The name of the class to which these constraints apply. */
        public String name;

        /** A mapping of names to the components in this class. */
        public HashMap<String,Component> components =
            new HashMap<String,Component>();

        /** Used when parsing constraints definitions. */
        public void addComponent (Component comp)
        {
            components.put(comp.name, comp);
        }

        /** Increase this value when object's serialized state is impacted by a
         * class change (modification of fields, inheritance). */
        private static final long serialVersionUID = 1;
    }

    /** Contains metadata on a particular avatar component. */
    public static class Component implements Serializable
    {
        /** The name of this particular attire component. */
        public String name;

        /** The cost associated with this component. This is interpreted
         * differently depending on whether this is a "body" component or just
         * clothing or an accessory. */
        public int cost;
    }

    /**
     * Returns an array containing the set of component class names registered
     * in this constraints object.
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
     * Returns an iterator over the set of component names in the specified
     * component class. <code>null</code> is returned if no component class is
     * registered with the specified name.
     */
    public Iterator<String> enumerateComponentNames (String cclass)
    {
        ComponentClass ccrec = getComponentClass(cclass);
        return (ccrec == null) ? null : ccrec.components.keySet().iterator();
    }

    /**
     * Get the cost of the specified component.
     */
    public int getCost (String cclass, String component)
    {
        Component comp = getComponent(cclass, component);
        return (comp == null) ? 0 : comp.cost;
    }

    /**
     * Adds a parsed component class to this constraints instance. This is used
     * when parsing component constraint definitions.
     */
    public void addClass (ComponentClass cclass)
    {
        _classes.put(cclass.name, cclass);
    }

    /**
     * Looks up the requested class record and logs a warning if it
     * doesn't exist.
     */
    protected ComponentClass getComponentClass (String cclass)
    {
        ComponentClass ccrec = _classes.get(cclass);
        if (ccrec == null) {
            log.warning("No such component class [class=" + cclass + "].");
            Thread.dumpStack();
        }
        return ccrec;
    }

    /**
     * Looks up the requested component record and logs a warning if it
     * doesn't exist.
     */
    protected Component getComponent (String cclass, String component)
    {
        ComponentClass ccrec = getComponentClass(cclass);
        Component comp = null;
        if (ccrec != null) {
            comp = ccrec.components.get(component);
            if (comp == null) {
                log.warning("No such component in class [class=" + cclass +
                            ", component=" + component + "].");
            }
        }
        return comp;
    }

    /** The mapping from class name to class. */
    protected HashMap<String,ComponentClass> _classes =
        new HashMap<String,ComponentClass>();

    /** Increase this value when object's serialized state is impacted by a
     * class change (modification of fields, inheritance). */
    private static final long serialVersionUID = 1;
}
