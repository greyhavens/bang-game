//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;

/**
 * Displays the amount of money in a gang's coffers.
 */
public class CofferLabel extends MoneyLabel
    implements AttributeChangeListener
{
    public CofferLabel (BangContext ctx, GangObject gangobj)
    {
        super(ctx, false);
        _gangobj = gangobj;

        setStyleClass("gang_coffers");

        add(_aces = new BLabel(BangUI.acesIcon), BorderLayout.EAST);
        _aces.setIconTextGap(5);
        _aces.setStyleClass("gang_coffers");
        _aces.setTooltipText(ctx.xlate(HideoutCodes.HIDEOUT_MSGS, "m.ace_tip"));
    }

    /**
     * Configures the quantity of scrip, coins, and aces displayed by this label.
     */
    public void setMoney (int scrip, int coins, int aces, boolean animate)
    {
        setMoney(scrip, coins, animate);
        _aces.setText(String.valueOf(aces));
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
    protected BLabel _aces;
}
