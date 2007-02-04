//
// $Id$

package com.threerings.bang.gang.server;

import java.util.ArrayList;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
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
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.gang.data.TopRankedGangList;

import static com.threerings.bang.Log.log;

/**
 * Provides hideout-related services.
 */
public class HideoutManager extends MatchHostManager
    implements GangCodes, HideoutCodes, HideoutProvider
{
    // documentation inherited from interface HideoutProvider
    public void formGang (ClientObject caller, Handle root, String suffix,
                          final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're not already in a gang
        final PlayerObject user = requireShopEnabled(caller);
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
        
        // make sure the name isn't already in use
        Handle name = new Handle(root + " " + suffix);
        if (_hobj.gangs.containsKey(name)) {
            throw new InvocationException("m.duplicate_gang_name");
        }
        
        // form the name and start up the financial action
        BangServer.gangmgr.formGang(user, name,
            new ResultListener<GangEntry>() {
                public void requestCompleted (GangEntry result) {
                    _hobj.addToGangs(result);
                    listener.requestProcessed();
                }
                public void requestFailed (Exception cause) {
                    String msg;
                    if (cause instanceof InvocationException) {
                        msg = ((InvocationException)cause).getMessage();
                    } else {
                        log.warning("Failed to create new gang [who=" + user.who() + ", error=" +
                            cause + "].");
                        msg = INTERNAL_ERROR;
                    }
                    listener.requestFailed(msg);
                }
            });
    }

    // documentation inherited from interface HideoutProvider
    public void leaveGang (ClientObject caller, final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're in a gang
        final PlayerObject user = requireShopEnabled(caller);
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
    public void setStatement (ClientObject caller, String statement, String url,
                              final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're the leader of a gang
        PlayerObject user = requireShopEnabled(caller);
        verifyIsLeader(user);
        
        // make sure the entries are valid
        if (statement == null || statement.length() > MAX_STATEMENT_LENGTH) {
            log.warning("Invalid statement [who=" + user.who() + ", statement=" +
                statement + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        if (url == null || url.length() > MAX_URL_LENGTH) {
            log.warning("Invalid URL [who=" + user.who() + ", url=" + url + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // pass it off to the gang manager
        BangServer.gangmgr.setStatement(user.gangId, statement, url, listener);
    }
    
    // documentation inherited from interface HideoutProvider
    public void addToCoffers (ClientObject caller, final int scrip, final int coins,
                              final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're in a gang
        PlayerObject user = requireShopEnabled(caller);
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
    public void expelMember (ClientObject caller, final Handle handle,
                             final HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they can change the user's status
        final PlayerObject user = requireShopEnabled(caller);
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
    public void changeMemberRank (ClientObject caller, Handle handle, byte rank,
                                  HideoutService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they can change the user's status
        PlayerObject user = requireShopEnabled(caller);
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
    public void getHistoryEntries (ClientObject caller, int offset,
                                   HideoutService.ResultListener listener)
        throws InvocationException
    {
        // make sure they're in a gang
        PlayerObject user = requireShopEnabled(caller);
        verifyInGang(user);
        
        // make sure the offset is valid
        if (offset < 0) {
            log.warning("Invalid history entry offset [who=" + user.who() + ", offset=" + offset +
                "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // fetch the entries from the database.  we ask for one more than we display on a page in
        // order to find out if there are any on the previous page
        BangServer.gangmgr.getHistoryEntries(
            user.gangId, offset, HISTORY_PAGE_ENTRIES + 1, listener);
    }
    
    // documentation inherited from interface HideoutProvider
    public void getOutfitQuote (ClientObject caller, OutfitArticle[] outfit,
                                HideoutService.ResultListener listener)
        throws InvocationException
    {
        // make sure they're the leader of a gang
        PlayerObject user = requireShopEnabled(caller);
        verifyIsLeader(user);
        
        // pass it on to the gang manager
        BangServer.gangmgr.getOutfitQuote(user, outfit, listener);
    }
    
    // documentation inherited from interface HideoutProvider
    public void buyOutfits (ClientObject caller, OutfitArticle[] outfit,
                            HideoutService.ResultListener listener)
        throws InvocationException
    {
        // make sure they're the leader of a gang
        PlayerObject user = requireShopEnabled(caller);
        verifyIsLeader(user);
        
        // pass it on to the gang manager
        BangServer.gangmgr.buyOutfits(user, outfit, listener);
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
     * Verifies that the specified user is a leader of a gang, throwing an
     * exception and logging a warning if not.
     */
    protected void verifyIsLeader (PlayerObject user)
        throws InvocationException
    {
        // first things first
        verifyInGang(user); 
        if (user.gangRank != LEADER_RANK) {
            log.warning("User not gang leader [who=" + user.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
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

    @Override // from ShopManager
    protected String getIdent ()
    {
        return "hideout";
    }
    
    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new HideoutObject();
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _hobj = (HideoutObject)_plobj;
        _hobj.setService((HideoutMarshaller)
                         BangServer.invmgr.registerDispatcher(new HideoutDispatcher(this), false));
    
        // load up the gangs for the directory
        BangServer.gangmgr.loadGangs(new ResultListener<ArrayList<GangEntry>>() {
            public void requestCompleted (ArrayList<GangEntry> result) {
                _hobj.setGangs(new DSet<GangEntry>(result.iterator()));
            }
            public void requestFailed (Exception cause) {
                log.warning("Failed to load gang list [error=" + cause + "].");
            }
        });
        
        // start up our top-ranked list refresher interval
        _rankval = new Interval(BangServer.omgr) {
            public void expired () {
                refreshTopRanked();
            }
        };
        _rankval.schedule(1000L, RANK_REFRESH_INTERVAL);
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        super.didShutdown();

        // clear out our invocation service
        if (_hobj != null) {
            BangServer.invmgr.clearDispatcher(_hobj.service);
            _hobj = null;
        }

        // stop our top-ranked list refresher
        if (_rankval != null) {
            _rankval.cancel();
            _rankval = null;
        }
    }
    
    protected void refreshTopRanked ()
    {
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _lists = new ArrayList<TopRankedGangList>();
                    _lists.add(BangServer.gangrepo.loadTopRankedByNotoriety(TOP_RANKED_LIST_SIZE));
                    return true;

                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to load top-ranked gangs.", pe);
                    return false;
                }
            }

            public void handleResult () {
                // make sure we weren't shutdown while we were off invoking
                if (!_hobj.isActive()) {
                    return;
                }
                _hobj.startTransaction();
                try {
                    for (TopRankedGangList list : _lists) {
                        if (_hobj.topRanked.containsKey(list.criterion)) {
                            _hobj.updateTopRanked(list);
                        } else {
                            _hobj.addToTopRanked(list);
                        }
                    }
                } finally {
                    _hobj.commitTransaction();
                }
            }

            protected ArrayList<TopRankedGangList> _lists;
        });
    }
    
    protected HideoutObject _hobj;
    protected Interval _rankval;
    
    /** The frequency with which we update the top-ranked gang lists. */
    protected static final long RANK_REFRESH_INTERVAL = 60 * 60 * 1000L;

    /** The size of the top-ranked gang lists. */
    protected static final int TOP_RANKED_LIST_SIZE = 10;
}
