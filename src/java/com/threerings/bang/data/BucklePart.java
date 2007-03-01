//
// $Id$

package com.threerings.bang.data;

import java.util.Arrays;

import com.jmex.bui.icon.ImageIcon;

import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

import com.threerings.bang.avatar.data.AvatarCodes;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Represents part of a gang buckle.
 */
public class BucklePart extends Item
{
    /** A blank constructor used during unserialization. */
    public BucklePart ()
    {
    }

    /**
     * Creates a new buckle part item with the specified components.
     */
    public BucklePart (int gangId, String pclass, String name, int[] components)
    {
        super(gangId);
        _class = pclass;
        _name = name;
        _components = components;

        // buckles always belong to gangs
        setGangOwned(true);
    }

    /**
     * Returns the name of the part's class.
     */
    public String getPartClass ()
    {
        return _class;
    }

    /**
     * Returns the name of the part.
     */
    public String getPartName ()
    {
        return _name;
    }

    /**
     * Returns the component ids (and associated colorizations) for the various
     * buckle components that should be used when depicting this part.
     */
    public int[] getComponents ()
    {
        return _components;
    }

    /**
     * Returns the part's configured x position.
     */
    public short getX ()
    {
        return _x;
    }

    /**
     * Returns the part's configured y position.
     */
    public short getY ()
    {
        return _y;
    }

    /**
     * Sets the part's position.
     */
    public void setPosition (short x, short y)
    {
        _x = x;
        _y = y;
    }

    @Override // documentation inherited
    public String getName ()
    {
        return MessageBundle.qualify(AvatarCodes.BUCKLE_MSGS, "m." + _name);
    }

    @Override // documentation inherited
    public String getTooltip (PlayerObject user)
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.buckle_part_tip");
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return null;
    }

    @Override // documentation inherited
    public ImageIcon createIcon (BasicContext ctx, String iconPath)
    {
        return null;
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        BucklePart opart;
        return super.isEquivalent(other) &&
            (opart = (BucklePart)other)._name.equals(_name) &&
            Arrays.equals(opart._components, _components);
    }

    @Override // documentation inherited
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", class=").append(_class);
        buf.append(", name=").append(_name);
        buf.append(", components=");
        StringUtil.toString(buf, _components);
        buf.append(", x=").append(_x);
        buf.append(", y=").append(_y);
    }

    protected String _class, _name;
    protected int[] _components;
    protected short _x, _y;
}
