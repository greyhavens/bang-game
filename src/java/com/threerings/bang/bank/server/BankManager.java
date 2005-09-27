//
// $Id$

package com.threerings.bang.bank.server;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.bank.data.BankObject;

import static com.threerings.bang.Log.log;

/**
 * Handles the server-side operation of the Bank.
 */
public class BankManager extends PlaceManager
{
    @Override // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return BankObject.class;
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _bobj = (BankObject)_plobj;
//         _bobj.setService((BankMarshaller)BangServer.invmgr.registerDispatcher(
//                              new BankDispatcher(this), false));
    }

    protected BankObject _bobj;
}
