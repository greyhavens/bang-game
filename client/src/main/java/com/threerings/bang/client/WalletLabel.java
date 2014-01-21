//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

/**
 * Displays the quantity of money a player has on hand.
 */
public class WalletLabel extends MoneyLabel
    implements AttributeChangeListener
{
    public WalletLabel (BangContext ctx, boolean showHandle)
    {
        super(ctx);
        if (showHandle) {
            add(new BLabel(ctx.xlate(BangCodes.BANG_MSGS, "m.your_wallet"),
                           "wallet_name"), BorderLayout.NORTH);
        }
        _user = ctx.getUserObject();

        if (DeploymentConfig.usesOneTime()) {
            _coins.setTooltipText(ctx.xlate(BangCodes.BANG_MSGS, "m.onetime_wallet_tip"));
        }
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(PlayerObject.SCRIP)) {
            updateValues(true);
        } else if (event.getName().equals(PlayerObject.COINS)) {
            updateValues(true);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _user.addListener(this);
        updateValues(false);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _user.removeListener(this);
    }

    protected void updateValues (boolean animate)
    {
        int coins;
        switch (DeploymentConfig.getPaymentType()) {
        case COINS:
            coins = _user.coins;
            break;
        case ONETIME:
            coins = _user.holdsOneTime() ? 1 : 0;
            break;
        default:
            throw new RuntimeException("Unknown payment type!");
        }
        // TODO: animate and bling!
        setMoney(_user.scrip, coins, animate);
    }

    protected PlayerObject _user;
}
