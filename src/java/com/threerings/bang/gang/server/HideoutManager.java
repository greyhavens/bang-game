//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.avatar.server.BarberManager;

import com.threerings.bang.gang.client.HideoutService;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
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
        ClientObject caller, Handle root, String suffix,
        final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're not already in a gang
        final PlayerObject user = (PlayerObject)caller;
        if (user.gangId > 0) {
            log.warning("Player tried to form a gang when already in one " +
                "[who=" + user.who() + ", gangId=" + user.gangId + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // make sure the suffix is in the approved set
        if (!NameFactory.getCreator().getGangSuffixes().contains(suffix)) {
            log.warning("Tried to form gang with invalid suffix [who=" +
                user.who() + ", suffix=" + suffix + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // validate the root using the same rules as the BarberManager
        BarberManager.validateHandle(user, root);
        
        // form the name and start up the financial action
        BangServer.gangmgr.formGang(
            user, new Handle(root + " " + suffix), listener);
    }

    // documentation inherited from interface HideoutProvider
    public void leaveGang (
        ClientObject caller, HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're in a gang
        PlayerObject user = (PlayerObject)caller;
        verifyInGang(user);
        
        // remove them
        BangServer.gangmgr.removeFromGang(
            user.gangId, user.playerId, user.handle, listener);
    }

    // documentation inherited from interface HideoutProvider
    public void addToCoffers (
        ClientObject caller, final int scrip, final int coins,
        final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're in a gang
        final PlayerObject user = (PlayerObject)caller;
        verifyInGang(user);
        
        // make sure the amounts are positive and that at least one is nonzero
        if (scrip < 0 || coins < 0 || scrip + coins == 0) {
            log.warning("Player tried to donate invalid amounts [who=" +
                user.who() + ", scrip=" + scrip + ", coins=" + coins + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // start up the financial action
        BangServer.gangmgr.addToCoffers(user, scrip, coins, listener);
    }
    
    // documentation inherited from interface HideoutProvider
    public void expelMember (
        ClientObject caller, Handle handle,
        HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they can change the user's status
        PlayerObject user = (PlayerObject)caller;
        GangMemberEntry entry = verifyCanChange(user, handle);
        
        // remove them
        BangServer.gangmgr.removeFromGang(
            user.gangId, entry.playerId, handle, listener);
    }
    
    // documentation inherited from interface HideoutProvider
    public void changeMemberRank (
        ClientObject caller, Handle handle, byte rank,
        HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they can change the user's status
        PlayerObject user = (PlayerObject)caller;
        GangMemberEntry entry = verifyCanChange(user, handle);
        
        // change it
        BangServer.gangmgr.changeMemberRank(
            user.gangId, entry.playerId, entry.handle, rank, listener);
    }
    
    /**
     * Verifies that the specified user can change the status (expel, change
     * rank, etc.) of the identified other member, throwing an exception and
     * logging a warning if not.
     *
     * @return the gang member's entry in the distributed object's member list,
     * if the check succeeded
     */
    protected GangMemberEntry verifyCanChange (
        PlayerObject user, Handle handle)
        throws InvocationException
    {
        // first things first
        verifyInGang(user);
        
        // make sure the gang object exists and contains the member
        GangObject gangobj =
            (GangObject)BangServer.omgr.getObject(user.gangOid);
        if (gangobj == null) {
            log.warning("Gang object is null [who=" + user.who() +
                ", gangId=" + user.gangId + ", gangOid = " +
                user.gangOid + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        GangMemberEntry entry = gangobj.members.get(handle);
        if (entry == null) {
            log.warning("Gang member does not exist [who=" + user.who() +
                ", gang=" + gangobj.which() + ", handle=" + handle + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // make sure they can change the status
        if (!entry.canChangeStatus(user)) {
            log.warning("Tried to change outranking member [who=" +
                user.who() + ", gang=" + gangobj.which() + ", member=" +
                entry + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // return the entry for the next step
        return entry;
    }
    
    /**
     * Verifies that the specified user is in a gang, throwing an exception
     * and logging a warning if not.
     */
    protected void verifyInGang (PlayerObject user)
        throws InvocationException
    {
        if (user.gangId <= 0) {
            log.warning("User not in gang [who=" + user.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
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
