//
// $Id$

package com.threerings.bang.data;

import com.threerings.crowd.peer.data.CrowdNodeObject;

/**
 * Extends the Peer system's {@link CrowdNodeObject} with information needed by Bang! Howdy.
 */
public class BangNodeObject extends CrowdNodeObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>townId</code> field. */
    public static final String TOWN_ID = "townId";
    // AUTO-GENERATED: FIELDS END

    /** The town handled by this node. */
    public String townId;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>townId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTownId (String value)
    {
        String ovalue = this.townId;
        requestAttributeChange(
            TOWN_ID, value, ovalue);
        this.townId = value;
    }
    // AUTO-GENERATED: METHODS END
}
