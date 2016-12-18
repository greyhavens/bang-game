//
// $Id$

package com.threerings.bang.gang.server;

import java.util.Calendar;

import com.google.inject.Inject;
import com.samskivert.io.PersistenceException;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.server.persist.ItemRepository;

import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.RentalGood;
import com.threerings.bang.gang.server.persist.GangFinancialAction;
import com.threerings.bang.gang.server.persist.GangRepository;
import com.threerings.bang.gang.util.GangUtil;

/**
 * Used to renew a gang rented item.
 */
public class ItemRenewal extends GangFinancialAction
{
    public ItemRenewal (GangObject gang, Handle handle, Item item, RentalGood good)
    {
        super(gang, false, good.getRentalScripCost(gang), good.getRentalCoinCost(gang), 0);
        _handle = handle;
        _item = item;
        _good = good;
    }

    /**
     * Configures the listener.
     */
    public void setListener (InvocationService.ConfirmListener listener)
    {
        _listener = listener;
    }

    @Override // documentation inherited
    protected String getCoinDescrip ()
    {
        return MessageBundle.compose("m.good_renewal", _good.getName());
    }

    @Override // documentation inherited
    protected String persistentAction ()
        throws PersistenceException
    {
        // update the item
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(_item.getExpires());
        cal.add(Calendar.DAY_OF_YEAR, 30);
        _item.setExpires(cal.getTimeInMillis());
        _itemrepo.updateItem(_item);

        // insert a history entry
        _entryId = _gangrepo.insertHistoryEntry(_gang.gangId,
                MessageBundle.compose("m.renewal_entry", MessageBundle.taint(_handle),
                    _good.getName(), GangUtil.getMoneyDesc(_scripCost, _coinCost, _aceCost)));
        _gangmgr.incLeaderLevel(_gang, _handle);
        return null;
    }

    @Override // documentation inherited
    protected void rollbackPersistentAction ()
        throws PersistenceException
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(_item.getExpires());
        cal.add(Calendar.DAY_OF_YEAR, -30);
        _item.setExpires(cal.getTimeInMillis());
        _itemrepo.updateItem(_item);

        super.rollbackPersistentAction();
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        _gang.updateMembers(_gang.members.get(_handle));
        _gang.updateInventory(_item);
        _listener.requestProcessed();
        super.actionCompleted();
    }

    @Override // documentation inherited
    protected void actionFailed (String cause)
    {
        _listener.requestFailed(cause);
    }

    protected String getGoodType ()
    {
        return _good.getClass().getSimpleName();
    }

    protected Item _item;
    protected RentalGood _good;
    protected Handle _handle;
    protected int _entryId;
    protected InvocationService.ConfirmListener _listener;

    // dependencies
    @Inject GangManager _gangmgr;
    @Inject GangRepository _gangrepo;
    @Inject ItemRepository _itemrepo;
}
