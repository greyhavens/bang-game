//
// $Id$

package com.threerings.bang.gang.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ListUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.gang.client.HideoutService;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutMarshaller;
import com.threerings.bang.gang.data.HideoutObject;

import static com.threerings.bang.Log.log;

/**
 * Provides hideout-related services.
 */
public class HideoutManager extends PlaceManager
    implements HideoutCodes, HideoutProvider
{
    // documentation inherited from interface HideoutProvider
    public void formGang (
        ClientObject caller, Name root, String suffix,
        HideoutService.ConfirmListener listener)
        throws InvocationException
    {
    }

    // documentation inherited from interface HideoutProvider
    public void leaveGang (
        ClientObject caller, HideoutService.ConfirmListener listener)
        throws InvocationException
    {
    }

    // documentation inherited from interface HideoutProvider
    public void addToCoffers (
        ClientObject caller, int scrip, int coins,
        HideoutService.ConfirmListener listener)
        throws InvocationException
    {
    }
    
    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new HideoutObject();
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // TODO: anything?
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _hobj = (HideoutObject)_plobj;
        _hobj.setService(
            (HideoutMarshaller)BangServer.invmgr.registerDispatcher(
                new HideoutDispatcher(this), false));
    }

    protected HideoutObject _hobj;
}
