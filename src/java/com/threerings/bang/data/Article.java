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
     * Creates a new article item with the specified avatar components.
     */
    public Article (int ownerId, int[] components)
    {
        super(ownerId);
        _components = components;
    }

    /**
     * Returns the component ids (and associated colorizations) for the various
     * avatar components that should be "applied" when wearing this article.
     */
    public int[] getComponents ()
    {
        return _components;
    }

    @Override // documentation inherited
    public ItemIcon createIcon ()
    {
        return null;
    }

    protected int[] _components;
}
