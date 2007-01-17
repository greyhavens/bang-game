//
// $Id$

package com.threerings.bang.gang.server;

import java.util.ArrayList;

import com.samskivert.util.ResultListener;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.avatar.server.BarberManager;

import com.threerings.bang.saloon.server.MatchHostManager;

import com.threerings.bang.gang.client.HideoutService;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutMarshaller;
import com.threerings.bang.gang.data.HideoutObject;

import static com.threerings.bang.Log.log;

/**
 * Provides hideout-related services.
 */
public class HideoutManager extends MatchHostManager
    implements GangCodes, HideoutCodes, HideoutProvider
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
            user, new Handle(root + " " + suffix), new ResultListener<GangEntry>() {
                public void requestCompleted (GangEntry result) {
                    _hobj.addToGangs(result);
                    listener.requestProcessed();
                }
                public void requestFailed (Exception cause) {
                    log.warning("Failed to create new gang [who=" + user.who() + ", error=" +
                        cause + "].");
                    listener.requestFailed(INTERNAL_ERROR);
                }
            });
    }

    // documentation inherited from interface HideoutProvider
    public void leaveGang (ClientObject caller, final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're in a gang
        final PlayerObject user = (PlayerObject)caller;
        verifyInGang(user);
        
        // remove them
        BangServer.gangmgr.removeFromGang(
            user.gangId, user.playerId, user.handle, null, new ResultListener<Handle>() {
                public void requestCompleted (Handle deletion) {
                    if (deletion != null) {
                        _hobj.removeFromGangs(deletion);
                    }
                    listener.requestProcessed();
                }
                public void requestFailed (Exception cause) {
                    log.warning("Failed to leave gang [who=" + user.who() + ", error=" +
                        cause + "].");
                    listener.requestFailed(INTERNAL_ERROR);
                }
            });
    }

    // documentation inherited from interface HideoutProvider
    public void addToCoffers (
        ClientObject caller, final int scrip, final int coins,
        final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're in a gang
        PlayerObject user = (PlayerObject)caller;
        verifyInGang(user);
        
        // make sure they can donate; we return a user-friendly message if not, even though they
        // shouldn't see the option, because their clock may not match up with ours
        if (!user.canDonate()) {
            throw new InvocationException("e.too_soon");
        }
        
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
        ClientObject caller, final Handle handle,
        final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they can change the user's status
        final PlayerObject user = (PlayerObject)caller;
        GangMemberEntry entry = verifyCanChange(user, handle);
        
        // remove them
        BangServer.gangmgr.removeFromGang(
            user.gangId, entry.playerId, handle, user.handle, new ResultListener<Handle>() {
                public void requestCompleted (Handle deletion) {
                    listener.requestProcessed();
                }
                public void requestFailed (Exception cause) {
                    log.warning("Failed to expel member from gang [who=" + user.who() +
                        ", target=" + handle + ", error=" + cause + "].");
                    listener.requestFailed(INTERNAL_ERROR);
                }
            });
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
        
        // make sure it's a valid rank
        if (rank < 0 || rank >= RANK_COUNT || rank == entry.rank) {
            log.warning("Tried to change member to invalid rank [who=" + user.who() +
                ", entry=" + entry + ", rank=" + rank + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // change it
        BangServer.gangmgr.changeMemberRank(
            user.gangId, entry.playerId, entry.handle, user.handle, entry.rank, rank, listener);
    }
    
    // documentation inherited from interface HideoutProvider
    public void getHistoryEntries (
        ClientObject caller, int offset, HideoutService.ResultListener listener)
        throws InvocationException
    {
        // make sure they're in a gang
        PlayerObject user = (PlayerObject)caller;
        verifyInGang(user);
        
        // make sure the offset is valid
        if (offset < 0) {
            log.warning("Invalid history entry offset [who=" + user.who() + ", offset=" + offset +
                "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // fetch the entries from the database
        BangServer.gangmgr.getHistoryEntries(user.gangId, offset, HISTORY_PAGE_ENTRIES, listener);
    }
    
    /**
     * Verifies that the specified user can change the status (expel, change
     * rank, etc.) of the identified other member, throwing an exception and
     * logging a warning if not.
     *
     * @return the gang member's entry in the distributed object's member list,
     * if the check succeeded
     */
    protected GangMemberEntry verifyCanChange (PlayerObject user, Handle handle)
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
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _hobj = (HideoutObject)_plobj;
        _hobj.setService(
            (HideoutMarshaller)BangServer.invmgr.registerDispatcher(
                new HideoutDispatcher(this), false));
    
        // load up the gangs for the directory
        BangServer.gangmgr.loadGangs(new ResultListener<ArrayList<GangEntry>>() {
            public void requestCompleted (ArrayList<GangEntry> result) {
                _hobj.setGangs(new DSet<GangEntry>(result.iterator()));
            }
            public void requestFailed (Exception cause) {
                log.warning("Failed to load gang list [error=" + cause + "].");
            }
        });
    }

    protected HideoutObject _hobj;
}
