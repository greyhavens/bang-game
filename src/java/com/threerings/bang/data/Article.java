//
// $Id$

package com.threerings.bang.data;

import com.samskivert.util.StringUtil;

import com.threerings.bang.client.ArticleIcon;
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
     * Creates a new article item with the specified slot and components.
     */
    public Article (int ownerId, String slot, String name, int[] components)
    {
        super(ownerId);
        _slot = slot;
        _name = name;
        _components = components;
    }

    /**
     * Returns the slot into which this article fits on an avatar.
     */
    public String getSlot ()
    {
        return _slot;
    }

    /**
     * Returns the name code for this article. This can be used to create a
     * translation string to obtain a human readable name.
     */
    public String getName ()
    {
        return _name;
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
        return new ArticleIcon();
    }

    @Override // documentation inherited
    protected void toString (StringBuffer buf)
    {
        super.toString(buf);
        buf.append(", slot=").append(_slot);
        buf.append(", name=").append(_name);
        buf.append(", components=");
        StringUtil.toString(buf, _components);
    }

    protected String _slot, _name;
    protected int[] _components;
}
