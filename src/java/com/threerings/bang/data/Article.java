//
// $Id$

package com.threerings.bang.data;

import com.threerings.bang.client.ItemIcon;

/**
 * Represents an article of clothing or an accessory.
 */
public class Article extends Item
{
    /** A blank constructor used during unserialization. */
    public Article ()
    {
    }

    /**
     * Creates a new article item with the specified component id and
     * colorizations.
     */
    public Article (int ownerId, int componentId, int zations)
    {
        super(ownerId);
        _componentId = componentId;
        _zations = zations;
    }

    /**
     * Returns the component id of this article.
     */
    public int getComponentId ()
    {
        return _componentId;
    }

    /**
     * Returns the colorizations for this article.
     */
    public int getColorizations ()
    {
        return _zations;
    }

    @Override // documentation inherited
    public ItemIcon createIcon ()
    {
        return null;
    }

    protected int _componentId, _zations;
}
