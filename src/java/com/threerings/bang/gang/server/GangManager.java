//
// $Id$

package com.threerings.bang.gang.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.server.BangServer;

import com.threerings.bang.gang.client.GangService;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.server.persist.GangRepository;

import com.threerings.bang.data.Handle;

/**
 * Handles gang-related functionality.
 */
public class GangManager
    implements GangProvider, GangCodes
{
    /**
     * Initializes the gang manager and registers its invocation service.
     */
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        _gangrepo = new GangRepository(conprov);

        // register ourselves as the provider of the (bootstrap) GangService
        BangServer.invmgr.registerDispatcher(new GangDispatcher(this), true);
    }

    // documentation inherited from GangProvider
    public void inviteMember (ClientObject caller,
        Handle handle, String message, GangService.ConfirmListener listener)
        throws InvocationException
    {
    }
    
    /** The persistent store for gang data. */
    protected GangRepository _gangrepo;
}
