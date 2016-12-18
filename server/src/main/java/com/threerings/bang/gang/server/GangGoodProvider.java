//
// $Id$

package com.threerings.bang.gang.server;

import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;

import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.server.persist.GangFinancialAction;
import com.threerings.bang.gang.util.GangUtil;

/**
 * Creates and delivers a GangGood purchased by a gang.
 */
public abstract class GangGoodProvider extends GangFinancialAction
{
    /**
     * Creates and initializes the goods provider with the receiving gang and the good to be
     * created and delivered.
     */
    protected GangGoodProvider (
        GangObject gang, Handle handle, boolean admin, GangGood good, Object[] args)
    {
        super(gang, admin, good.getScripCost(gang), good.getCoinCost(gang), good.getAceCost(gang));
        _handle = handle;
        _good = good;
        _args = args;
    }

    /**
     * Configures this provider with its listener.
     */
    protected void setListener (InvocationService.ConfirmListener listener)
    {
        _listener = listener;
    }

    @Override // documentation inherited
    protected String getCoinDescrip ()
    {
        return MessageBundle.compose("m.good_purchase", _good.getName());
    }

    @Override // documentation inherited
    protected String persistentAction ()
        throws PersistenceException
    {
        // insert a history entry
        final String msg = MessageBundle.compose(
            getHistoryLogKey(), MessageBundle.taint(_handle), _good.getName(),
            GangUtil.getMoneyDesc(_scripCost, _coinCost, _aceCost));
        _entryId = _gangrepo.insertHistoryEntry(_gang.gangId, msg);
        _gangmgr.incLeaderLevel(_gang, _handle);
        return null;
    }

    @Override // documentation inherited
    protected void rollbackPersistentAction ()
        throws PersistenceException
    {
        if (_entryId > 0) {
            _gangrepo.deleteHistoryEntry(_entryId);
        }
    }

    @Override // documentation inherited
    protected void actionCompleted ()
    {
        _gang.updateMembers(_gang.members.get(_handle));
        _listener.requestProcessed();
        super.actionCompleted();
    }

    @Override // documentation inherited
    protected void actionFailed (String cause)
    {
        _listener.requestFailed(cause);
    }

    protected String getHistoryLogKey ()
    {
        return "m.purchase_entry";
    }

    protected String getGoodType ()
    {
        return _good.getClass().getSimpleName();
    }

    protected Handle _handle;
    protected GangGood _good;
    protected Object[] _args;
    protected InvocationService.ConfirmListener _listener;

    protected int _entryId;

    // dependencies
    @Inject protected GangManager _gangmgr;
}
