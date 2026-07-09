//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.dobj.DObject;

/**
 * Contains information about the town (currently just the population).
 */
public class TownObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>population</code> field. */
    public static final String POPULATION = "population";
    // AUTO-GENERATED: FIELDS END

    /** The population of the town. */
    public int population;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>population</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPopulation (int value)
    {
        int ovalue = this.population;
        requestAttributeChange(
            POPULATION, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.population = value;
    }
    // AUTO-GENERATED: METHODS END

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.presents.dobj.DObject.class, "_oid", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.TownObject.class, "population", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.presents.dobj.DObject.class, "_oid", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.TownObject.class, "population", this, out);
    }
}
