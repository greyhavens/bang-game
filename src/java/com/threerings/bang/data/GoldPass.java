//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

/**
 * Reduces the gold cost of many items in a town to 0.
 */
public class GoldPass extends Item
{
    /** Creates a pass for the specified town. */
    public GoldPass (int ownerId, String townId)
    {
        super(ownerId);
        _townId = townId;
    }

    /** Blank construction for serialization. */
    public GoldPass ()
    {
    }

    /**
     * Returns the town which this pass is good for.
     */
    public String getTownId ()
    {
        return _townId;
    }

    @Override // documentation inherited
    public String getName()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS,
                MessageBundle.compose("m.gold_pass", "m." + _townId));
    }

    @Override // documentation inherited
    public String getTooltip (PlayerObject user)
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS,
                MessageBundle.compose("m.gold_pass_tip", "m." + _townId));
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "goods/gold/" + _townId + ".png";
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) && ((GoldPass)other)._townId.equals(_townId);
    }

    protected String _townId;
}
