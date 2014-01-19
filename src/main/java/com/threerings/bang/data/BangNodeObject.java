//
// $Id$

package com.threerings.bang.data;

import com.threerings.crowd.peer.data.CrowdNodeObject;

import com.threerings.util.StreamableTuple;

/**
 * Extends the Peer system's {@link CrowdNodeObject} with information needed by Bang! Howdy.
 */
public class BangNodeObject extends CrowdNodeObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>townId</code> field. */
    public static final String TOWN_ID = "townId";

    /** The field name of the <code>bangPeerService</code> field. */
    public static final String BANG_PEER_SERVICE = "bangPeerService";

    /** The field name of the <code>activatedGang</code> field. */
    public static final String ACTIVATED_GANG = "activatedGang";

    /** The field name of the <code>removedGang</code> field. */
    public static final String REMOVED_GANG = "removedGang";

    /** The field name of the <code>changedHandle</code> field. */
    public static final String CHANGED_HANDLE = "changedHandle";
    // AUTO-GENERATED: FIELDS END

    /** The town handled by this node. */
    public String townId;

    /** The peer service for Bang-specific requests. */
    public BangPeerMarshaller bangPeerService;

    /** Used to broadcast the addition of a new gang or the reactivation of an existing gang. */
    public Handle activatedGang;

    /** Used to broadcast the removal of a gang. */
    public Handle removedGang;

    /** Used to broadcast a player's changing his handle (the left element contains the old handle
     * and the right contains the new). */
    public StreamableTuple<Handle, Handle> changedHandle;

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

    /**
     * Requests that the <code>bangPeerService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBangPeerService (BangPeerMarshaller value)
    {
        BangPeerMarshaller ovalue = this.bangPeerService;
        requestAttributeChange(
            BANG_PEER_SERVICE, value, ovalue);
        this.bangPeerService = value;
    }

    /**
     * Requests that the <code>activatedGang</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setActivatedGang (Handle value)
    {
        Handle ovalue = this.activatedGang;
        requestAttributeChange(
            ACTIVATED_GANG, value, ovalue);
        this.activatedGang = value;
    }

    /**
     * Requests that the <code>removedGang</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRemovedGang (Handle value)
    {
        Handle ovalue = this.removedGang;
        requestAttributeChange(
            REMOVED_GANG, value, ovalue);
        this.removedGang = value;
    }

    /**
     * Requests that the <code>changedHandle</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setChangedHandle (StreamableTuple<Handle, Handle> value)
    {
        StreamableTuple<Handle, Handle> ovalue = this.changedHandle;
        requestAttributeChange(
            CHANGED_HANDLE, value, ovalue);
        this.changedHandle = value;
    }
    // AUTO-GENERATED: METHODS END
}
