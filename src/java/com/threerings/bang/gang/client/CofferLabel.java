//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangObject;

/**
 * Displays the amount of money in a gang's coffers.
 */
public class CofferLabel extends GangMoneyLabel
    implements AttributeChangeListener
{
    public CofferLabel (BangContext ctx, GangObject gangobj)
    {
        super(ctx, false);
        _gangobj = gangobj;

        setStyleClass("gang_coffers");
    }

    // documentation inherited from AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(GangObject.SCRIP) || name.equals(GangObject.COINS) ||
            name.equals(GangObject.ACES)) {
            setMoney(_gangobj.scrip, _gangobj.coins, _gangobj.aces, true);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        setMoney(_gangobj.scrip, _gangobj.coins, _gangobj.aces, false);
        _gangobj.addListener(this);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _gangobj.removeListener(this);
    }

    protected GangObject _gangobj;
}
