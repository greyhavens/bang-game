//
// $Id$

package com.threerings.bang.lobby.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableLobbyObject;
import com.threerings.parlor.data.TableMarshaller;

/**
 * Contains the distributed information for a Bang! lobby.
 */
public class LobbyObject extends PlaceObject
    implements TableLobbyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>tableSet</code> field. */
    public static final String TABLE_SET = "tableSet";

    /** The field name of the <code>townId</code> field. */
    public static final String TOWN_ID = "townId";

    /** The field name of the <code>scenarios</code> field. */
    public static final String SCENARIOS = "scenarios";

    /** The field name of the <code>tableService</code> field. */
    public static final String TABLE_SERVICE = "tableService";
    // AUTO-GENERATED: FIELDS END

    /** A set containing all of the tables being managed by this lobby.
     * This may be empty if we're not using tables to match-make. */
    public DSet<Table> tableSet = new DSet<Table>();

    /** The town in which this lobby resides. */
    public String townId;

    /** Valid scenarios that may be match-made from this lobby. */
    public String[] scenarios;

    /** Used to interact with our table manager. */
    public TableMarshaller tableService;

    // from interface TableLobbyObject
    public DSet<Table> getTables ()
    {
        return tableSet;
    }

    // from interface TableLobbyObject
    public TableMarshaller getTableService ()
    {
        return tableService;
    }

    // from interface TableLobbyObject
    public void addToTables (Table table)
    {
        addToTableSet(table);
    }

    // from interface TableLobbyObject
    public void updateTables (Table table)
    {
        updateTableSet(table);
    }

    // from interface TableLobbyObject
    public void removeFromTables (Comparable<?> key)
    {
        removeFromTableSet(key);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>tableSet</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToTableSet (Table elem)
    {
        requestEntryAdd(TABLE_SET, tableSet, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>tableSet</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromTableSet (Comparable<?> key)
    {
        requestEntryRemove(TABLE_SET, tableSet, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>tableSet</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateTableSet (Table elem)
    {
        requestEntryUpdate(TABLE_SET, tableSet, elem);
    }

    /**
     * Requests that the <code>tableSet</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setTableSet (DSet<Table> value)
    {
        requestAttributeChange(TABLE_SET, value, this.tableSet);
        DSet<Table> clone = (value == null) ? null : value.clone();
        this.tableSet = clone;
    }

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
     * Requests that the <code>scenarios</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setScenarios (String[] value)
    {
        String[] ovalue = this.scenarios;
        requestAttributeChange(
            SCENARIOS, value, ovalue);
        this.scenarios = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>scenarios</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setScenariosAt (String value, int index)
    {
        String ovalue = this.scenarios[index];
        requestElementUpdate(
            SCENARIOS, index, value, ovalue);
        this.scenarios[index] = value;
    }

    /**
     * Requests that the <code>tableService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTableService (TableMarshaller value)
    {
        TableMarshaller ovalue = this.tableService;
        requestAttributeChange(
            TABLE_SERVICE, value, ovalue);
        this.tableService = value;
    }
    // AUTO-GENERATED: METHODS END
}
