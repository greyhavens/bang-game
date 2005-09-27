//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BLabel;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

/**
 * Displays the quantity of money a player has on hand.
 */
public class WalletLabel extends MoneyLabel
    implements AttributeChangeListener
{
    public WalletLabel (BangContext ctx)
    {
        super(ctx);
        _user = ctx.getUserObject();
        updateValues(false);
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
    protected void createLabels (BangContext ctx)
    {
        add(new BLabel(ctx.xlate(BangCodes.BANG_MSGS, "m.cash_on_hand")));
        super.createLabels(ctx);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _user.addListener(this);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _user.removeListener(this);
    }

    protected void updateValues (boolean animate)
    {
        // TODO: animate and bling!
        setMoney(_user.scrip, _user.coins, animate);
    }

    protected PlayerObject _user;
}
