//
// $Id$

package com.threerings.bang.store.data;

import java.util.Comparator;

import com.jmex.bui.BImage;
import com.jmex.bui.icon.ImageIcon;

import com.samskivert.util.Comparators;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.media.image.Colorization;
import com.threerings.presents.dobj.DObject;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BasicContext;

/**
 * Represents a particular good that can be purchased from the general store.
 */
public abstract class Good extends SimpleStreamableObject
    implements DSet.Entry, Comparable<Good>
{
    /** Sorts goods by their scrip cost. */
    public static final Comparator<Good> BY_SCRIP_COST = new Comparator<Good>() {
        public int compare (Good g1, Good g2) {
            return Comparators.combine(Comparators.compare(g2._priority, g1._priority),
                                       Comparators.compare(g1.getScripCost(), g2.getScripCost()),
                                       g1.getType().compareTo(g2.getType()));
        }
    };

    /** A constructor only used during serialization. */
    public Good ()
    {
    }

    /**
     * Returns the string identifier that uniquely identifies this type of good.
     */
    public String getType ()
    {
        return _type;
    }

    /**
     * Returns the town id where this good can be purchased.
     */
    public String getTownId ()
    {
        return _townId;
    }

    /**
     * Creates the initial icon for this good.  By default, returns an icon containing the image
     * located at {@link #getIconPath}, but subclasses can override to customize the icon.
     *
     * @param entity the entity to use in determining which colors are available.
     * @param colorIds if the good is colorizable, this (three element) array will be populated
     * with the color ids of the random colors chosen for the icon.
     */
    public ImageIcon createIcon (BasicContext ctx, DObject entity, int[] colorIds)
    {
        return createIcon(ctx, null);
    }

    /**
     * Creates a customized icon for this good.  By default, returns an icon containing the image
     * located at {@link #getIconPath}, modified with the supplied colorizations.
     */
    public ImageIcon createIcon (BasicContext ctx, Colorization[] zations)
    {
        BImage img = (zations == null) ?
            ctx.loadImage(getIconPath()) :
            ctx.getImageCache().createColorizedBImage(getIconPath(), zations, true);
        return new ImageIcon(img);
    }

    /**
     * Returns the filename of the icon associated with this good. The default is based on the type
     * of the good, but this can be overridden by specialized goods.
     */
    public String getIconPath ()
    {
        return "goods/" + _type + ".png";
    }

    /**
     * Returns the names of the colorization classes used by this good, or <code>null</code> for
     * none.
     */
    public String[] getColorizationClasses (BasicContext ctx)
    {
        return null;
    }

    /**
     * Returns a fully qualified translatable string indicating the name of this good.
     */
    public String getName ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m." + _type);
    }

    /**
     * Returns a fully qualified translatable string used to convey additional information about
     * the good in question.
     */
    public abstract String getTip ();

    /**
     * Returns a fully qualified translatable string used to convey additional information about
     * the good in question for a tooltip window.
     */
    public String getToolTip ()
    {
        return getTip();
    }

    /**
     * Returns the cost of this good in scrip. This is in addition to the coin cost ({@link
     * #getCoinCost}).
     */
    public int getScripCost ()
    {
        return _scripCost;
    }

    /**
     * Returns the cost of this good in coins. This is in addition to the scrip cost ({@link
     * #getScripCost}).
     */
    public int getCoinCost ()
    {
        return _coinCost;
    }

    /**
     * Returns the cost of this good in coins. This is in addition to the scrip cost ({@link
     * #getScripCost}).
     */
    public int getCoinCost (PlayerObject user)
    {
        return (honorsGoldPass() && user.holdsGoldPass(_townId)) ? 0 :  _coinCost;
    }

    /**
     * Returns true if this item honors the Gold Pass (eliminates its gold cost when the user has
     * the necessary pass).
     */
    public boolean honorsGoldPass ()
    {
        return false;
    }

    /**
     * Indicates that this good is available to the specified user.
     */
    public abstract boolean isAvailable (PlayerObject user);

    /**
     * Returns true if this good is not yet available.
     */
    public boolean isPending (long timestamp)
    {
        return false;
    }

    /**
     * Returns true if this good is no longer available.
     */
    public boolean isExpired (long timestamp)
    {
        return false;
    }

    /**
     * Creates the {@link Item} sold by this good for the specified player.
     */
    public Item createItem (int playerId)
    {
        throw new RuntimeException("createItem() not supported by this Good");
    }

    /**
     * Returns true if this good would create the supplied item.
     */
    public boolean wouldCreateItem (Item item)
    {
        return false;
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return _type;
    }

    // from interface Comparable<Good>
    public int compareTo (Good other)
    {
        int diff = other._priority - _priority;
        return diff == 0 ? getType().compareTo(other.getType()) : diff;
    }

    /**
     * Returns a string qualifier to get the good.
     */
    public String getQualifier ()
    {
        return null;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return _type.hashCode();
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other.getClass().equals(getClass()) && _type.equals(((Good)other)._type);
    }

    /** Creates a good of the specified type. */
    protected Good (String type, String townId, int scripCost, int coinCost, int priority)
    {
        _type = type;
        _townId = townId;
        _scripCost = scripCost;
        _coinCost = coinCost;
        _priority = priority;
    }

    protected String _type;
    protected String _townId;
    protected int _scripCost;
    protected int _coinCost;
    protected int _priority;
}
