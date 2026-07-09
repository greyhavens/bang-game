//
// $Id$

package com.threerings.bang.gang.data;

import com.jmex.bui.icon.ImageIcon;

import com.threerings.media.image.Colorization;

import com.threerings.presents.dobj.DObject;

import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.Good;

/**
 * A good that may be rented in the gang store.
 */
public class RentalGood extends GangGood
{
    public RentalGood ()
    {
    }

    public RentalGood (Good good)
    {
        _good = good;
    }

    @Override // documentation inherited
    public String getType ()
    {
        return _good.getType();
    }

    public Good getGood ()
    {
        return _good;
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return _good.getTownId();
    }

    @Override // documentation inherited
    public ImageIcon createIcon (BasicContext ctx, DObject entity, int[] colorIds)
    {
        return _good.createIcon(ctx, entity, colorIds);
    }

    @Override // documentation inherited
    public ImageIcon createIcon (BasicContext ctx, Colorization[] zations)
    {
        return _good.createIcon(ctx, zations);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return _good.getIconPath();
    }

    @Override // documentation inherited
    public String[] getColorizationClasses (BasicContext ctx)
    {
        return _good.getColorizationClasses(ctx);
    }

    @Override // documentation inherited
    public String getName ()
    {
        return _good.getName();
    }

    // documentation inherited
    public String getTip ()
    {
        return _good.getTip();
    }

    @Override // documentation inherited
    public String getToolTip ()
    {
        return _good.getToolTip();
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return _good.getScripCost();
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return _good.getCoinCost();
    }

    @Override // documentation inherited
    public int getCoinCost (PlayerObject user)
    {
        return _good.getCoinCost();
    }

    /**
     * Returns the 30 day scrip rental price for this good for a gang.
     */
    public int getRentalScripCost (GangObject gangobj)
    {
        return Math.round(getScripCost() * (_good instanceof ArticleGood ?
                        gangobj.articleRentMultiplier : gangobj.rentMultiplier));
    }

    /**
     * Returns the 30 day scrip rental price for this good for a gang.
     */
    public int getRentalCoinCost (GangObject gangobj)
    {
        return Math.round(getCoinCost() * (_good instanceof ArticleGood ?
                        gangobj.articleRentMultiplier : gangobj.rentMultiplier));
    }

    // documentation inhertied
    public boolean isAvailable (PlayerObject user)
    {
        return false;
    }

    // documentation inherited
    public boolean isAvailable (GangObject gang)
    {
        return _good.getQualifier() == null;
    }

    @Override // documentation inherited
    public Item createItem (int playerId)
    {
        return _good.createItem(playerId);
    }

    @Override // documentation inherited
    public boolean wouldCreateItem (Item item)
    {
        return _good.wouldCreateItem(item);
    }

    @Override // documentation inherited
    public Comparable<?> getKey ()
    {
        return _good.getKey();
    }

    @Override // documentation inherited
    public int compareTo (Good other)
    {
        return _good.compareTo(other);
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return _good.hashCode();
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return _good.equals(other);
    }

    protected Good _good;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_type", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_townId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_scripCost", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_coinCost", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_priority", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangGood.class, "_aceCost", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.RentalGood.class, "_good", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_type", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_townId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_scripCost", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_coinCost", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_priority", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangGood.class, "_aceCost", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.RentalGood.class, "_good", this, out);
    }
}
