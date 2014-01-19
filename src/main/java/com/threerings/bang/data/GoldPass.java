//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.util.DeploymentConfig;

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
        String msg = DeploymentConfig.usesCoins() ?
            MessageBundle.compose("m.gold_pass", "m." + _townId) : "m.onetime_pass";
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getTooltip (PlayerObject user)
    {
        String msg = DeploymentConfig.usesCoins() ?
            MessageBundle.compose("m.gold_pass_tip", "m." + _townId) : "m.onetime_pass_tip";
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, msg);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return DeploymentConfig.usesCoins() ? ("goods/gold/" + _townId + ".png") :
            "goods/onetime.png";
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) && ((GoldPass)other)._townId.equals(_townId);
    }

    protected String _townId;
}
