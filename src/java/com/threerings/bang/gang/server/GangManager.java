//
// $Id$

package com.threerings.bang.gang.server;

import java.util.ArrayList;
import java.util.Date;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.jdbc.RepositoryUnit;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Collections;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.server.SpeakDispatcher;
import com.threerings.crowd.chat.server.SpeakProvider;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.PosterInfo;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;
import com.threerings.bang.server.persist.PlayerRecord;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.ArticleCatalog;

import com.threerings.bang.saloon.server.SaloonManager;

import com.threerings.bang.gang.client.GangService;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangEntry;
import com.threerings.bang.gang.data.GangInvite;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HistoryEntry;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.gang.server.persist.GangFinancialAction;
import com.threerings.bang.gang.server.persist.GangInviteRecord;
import com.threerings.bang.gang.server.persist.GangMemberRecord;
import com.threerings.bang.gang.server.persist.GangOutfitRecord;
import com.threerings.bang.gang.server.persist.GangRecord;
import com.threerings.bang.gang.server.persist.GangRepository;

import static com.threerings.bang.Log.*;

/**
 * Handles gang-related functionality.
 */
public class GangManager
    implements GangProvider, SpeakProvider.SpeakerValidator, GangCodes
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

    /**
     * Returns the gang repository.
     */
    public GangRepository getGangRepository ()
    {
        return _gangrepo;
    }

    // documentation inherited from GangProvider
    public void inviteMember (ClientObject caller, final Handle handle, final String message,
                              final GangService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure they're in a gang and can recruit
        final PlayerObject player = (PlayerObject)caller;
        if (!player.canRecruit()) {
            log.warning("Player not qualified to recruit gang members [who=" +
                player.who() + ", gangId=" + player.gangId + ", rank=" +
                player.gangRank + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure it's not the player himself, that it's not already
        // a member, and that the player is under the limit
        if (player.handle.equals(handle)) {
            throw new InvocationException("e.invite_self");
        }
        final GangObject gangobj = getGangObject(player.gangId);
        if (gangobj.members.containsKey(handle)) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.already_member_this", handle));
        } else if (gangobj.members.size() >= MAX_MEMBERS) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.too_many_members", String.valueOf(MAX_MEMBERS)));
        }

        // store the invitation in the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _error = _gangrepo.insertInvite(player.playerId, player.gangId, handle, message);
                if (_error == null) {
                    _gangrepo.insertHistoryEntry(player.gangId,
                        MessageBundle.tcompose("m.invited_entry", player.handle, handle));
                }
            }
            public void handleSuccess () {
                if (_error != null) {
                    listener.requestFailed(_error);
                    return;
                }
                PlayerObject invitee = BangServer.lookupPlayer(handle);
                if (invitee != null) {
                    sendGangInvite(invitee, player.handle, player.gangId, gangobj.name, message);
                }
                listener.requestProcessed();
            }
            protected String _error;
        });
    }

    /**
     * Ensures that the gang data for the identified gang is loaded from the database. This must
     * only be called from the invoker thread. This must be followed by a call to {@link
     * #initPlayer} or {@link #releaseGang} on the dobjmgr thread.
     */
    public void resolveGang (int gangId)
        throws PersistenceException
    {
        BangServer.refuseDObjThread(); // safety first

        // first, determine if the gang is already loaded; note that we rely on the fact that only
        // the invoker thread will every add anything to _gangs; that allows us to leave _gangs
        // unlocked while we load the gang record
        GangHandler gang = _gangs.get(gangId);
        if (gang == null) {
            _gangs.put(gangId, gang = new GangHandler(_gangrepo.loadGang(gangId, true)));
        } else {
            gang.online++;
        }
    }

    /**
     * Registers a just-logged-on player with their Gang, if appropriate, dispatches any pending
     * Gang invitations otherwise. Called during logon after a player has resolved all of their
     * persistent data.
     */
    public void initPlayer (PlayerObject player, GangMemberRecord mrec,
                            ArrayList<GangInviteRecord> invites)
    {
        BangServer.requireDObjThread(); // safety first

        // if they're a gang member, wire them up
        if (mrec != null) {
            GangHandler gang = _gangs.get(mrec.gangId);
            GangObject gangobj = gang.getGangObject();

            try {
                player.startTransaction();
                player.setGangId(mrec.gangId);
                player.setGangRank(mrec.rank);
                player.setJoinedGang(mrec.joined.getTime());
                player.setGangNotoriety(mrec.notoriety);
                player.setGangOid(gangobj.getOid());
            } finally {
                player.commitTransaction();
            }
            
            // if they are the most senior leader, listen for avatar changes
            if (player.playerId == gangobj.getSeniorLeader().playerId) {
                new AvatarUpdater().add(player, gangobj);
            }       
            return;
        }

        // otherwise dispatch any pending gang invitations
        if (invites != null) {
            for (GangInviteRecord record : invites) {
                sendGangInvite(player, record.inviter, record.gangId, record.name, record.message);
            }
        }
    }

    /**
     * Releases the reference created earlier by {@link #resolveGang}. This should be called when
     * the player logs off or if an error prevents a call to {@link #initPlayer}.
     * This must be called on the dobjmgr thread.
     */
    public void releaseGang (int gangId)
    {
        BangServer.requireDObjThread(); // safety first
        GangHandler gang = _gangs.get(gangId);
        gang.online--;
        maybeShutdownGang(gangId);
    }

    /**
     * Populates the gang-related poster fields for an online player.
     */
    public void populatePosterInfo (PosterInfo info, PlayerObject player)
    {
        if (player.gangId <= 0) {
            return;
        }
        GangObject gangobj = getGangObject(player.gangId);
        if (gangobj == null) {
            log.warning("Missing gang object for player [who=" + player.who() +
                ", id=" + player.gangId + "].");
            return;
        }
        info.gang = gangobj.name;
        info.rank = getPosterRank(player.gangRank);
    }

    /**
     * Populates the gang-related poster fields for an offline player (from the invoker thread).
     */
    public void populatePosterInfo (PosterInfo info, PlayerRecord player)
        throws PersistenceException
    {
        BangServer.refuseDObjThread(); // safety first
        GangMemberRecord mrec = _gangrepo.loadMember(player.playerId);
        if (mrec != null) {
            info.gang = _gangrepo.loadGang(mrec.gangId, false).getName();
            info.rank = getPosterRank(mrec.rank);
        }
    }

    /**
     * Loads the list of gangs asynchronously on the invoker thread.
     */
    public void loadGangs (final ResultListener<ArrayList<GangEntry>> listener)
    {
        BangServer.invoker.postUnit(new RepositoryListenerUnit<ArrayList<GangEntry>>(listener) {
            public ArrayList<GangEntry> invokePersistResult ()
                throws PersistenceException {
                return _gangrepo.loadGangs();
            }
        });
    }
    
    /**
     * Grants notoriety points to a gang member and his gang.
     */
    public void grantNotoriety (
        final int gangId, final int playerId, final Handle handle, final int points)
    {
        final GangObject gang = getGangObject(gangId);
        final PlayerObject player = BangServer.lookupPlayer(playerId);
        BangServer.invoker.postUnit(new RepositoryUnit() {
            public void invokePersist ()
                throws PersistenceException {
                _gangrepo.addNotoriety(gangId, playerId, points);
            }
            public void handleSuccess () {
                if (gang != null && gang.isActive()) {
                    GangMemberEntry member = gang.members.get(handle);
                    gang.startTransaction();
                    try {
                        gang.setNotoriety(gang.notoriety + points);
                        if (member != null) {
                            member.notoriety += points;
                            gang.updateMembers(member);
                        } 
                    } finally {
                        gang.commitTransaction();
                    }
                }
                if (player != null && player.isActive()) {
                    player.setGangNotoriety(player.gangNotoriety + points);
                }
            }
            public void handleFailure (Exception cause) {
                log.warning("Failed to grant notoriety [gangId=" + gangId + ", playerId=" +
                    playerId + ", points=" + points + ", error=" + cause + "].");
            }
        });
    }
    
    /**
     * Processes a request to form a gang.  It is assumed that the player does not already belong
     * to a gang and that the provided name is valid.
     *
     * @param listener if the request succeeds, this listener will receive the new
     * {@link GangEntry} for the gang
     */
    public void formGang (final PlayerObject user, final Handle name,
                          final ResultListener<GangEntry> listener)
        throws InvocationException
    {
        Look look = user.getLook(Look.Pose.WANTED_POSTER);
        final int[] avatar = (look == null) ? null : look.getAvatar(user);
        new FinancialAction(user, FORM_GANG_SCRIP_COST, FORM_GANG_COIN_COST) {
            protected int getCoinType () {
                return CoinTransaction.GANG_CREATION;
            }
            protected String getCoinDescrip () {
                return MessageBundle.tcompose("m.gang_creation", name);
            }

            protected String persistentAction ()
                throws PersistenceException {
                _gangrepo.insertGang(_grec);
                _gangrepo.insertMember(
                    _mrec = new GangMemberRecord(user.playerId, _grec.gangId, LEADER_RANK));
                _gangrepo.insertHistoryEntry(
                    _grec.gangId, MessageBundle.tcompose("m.founded_entry", user.handle));
                // we must create and insert our gang handler on the invoker thread
                _grec.members.add(new GangMemberEntry(
                    user.handle, user.playerId, LEADER_RANK, _mrec.joined, 0));
                _grec.avatar = avatar;
                _gangs.put(_grec.gangId, new GangHandler(_grec));
                return null;
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                // deleting the gang also deletes its history
                _gangrepo.deleteGang(_grec.gangId);
                _gangrepo.deleteMember(user.playerId);
            }

            protected void actionCompleted () {
                log.info("Formed new gang [who=" + user.who() + ", name=" + name +
                         ", gangId=" + _grec.gangId + "].");
                if (!user.isActive()) {
                    releaseGang(_grec.gangId);  // he bailed; no point in continuing
                } else {
                    initPlayer(user, _mrec, null);
                    listener.requestCompleted(new GangEntry(name));
                }
            }
            protected void actionFailed (String cause) {
                listener.requestFailed(new InvocationException(cause));
            }

            protected GangRecord _grec = new GangRecord(name.toString());
            protected GangMemberRecord _mrec;
        }.start();
    }

    /**
     * Processes a request to change the gang's statement.
     */
    public void setStatement (
        final int gangId, final String statement, final String url,
        final InvocationService.ConfirmListener listener)
    {
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _gangrepo.updateStatement(gangId, statement, url);
            }
            public void handleSuccess () {
                log.info("Changed gang statement [gangId=" + gangId + ", statement=" + statement +
                         ", url=" + url + "].");
                GangObject gangobj = getGangObject(gangId);
                if (gangobj != null) {
                    gangobj.startTransaction();
                    try {
                        gangobj.setStatement(statement);
                        gangobj.setUrl(url);
                    } finally {
                        gangobj.commitTransaction();
                    }
                }
                listener.requestProcessed();
            }
        });
    }
    
    /**
     * Processes a request to add to the gang's coffers.  It is assumed that the player belongs to
     * a gang and that the amounts are valid.
     */
    public void addToCoffers (final PlayerObject user, final int scrip, final int coins,
                              final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final GangObject gang = getGangObject(user.gangId);
        new FinancialAction(user, scrip, coins) { {
                // because we're transferring rather than spending, we need to reserve coins even
                // for admins
                _coinCost = coins;
            }
            protected String persistentAction () {
                try {
                    _gangrepo.grantScrip(user.gangId, scrip);
                    _entryId = _gangrepo.insertHistoryEntry(user.gangId,
                        MessageBundle.compose(
                            "m.donation_entry",
                            MessageBundle.taint(user.handle),
                            getMoneyDesc(scrip, coins)));
                    return null;
                } catch (PersistenceException e) {
                    return INTERNAL_ERROR;
                }
            }
            protected boolean spendCoins (int reservationId)
                throws PersistenceException {
                return BangServer.coinmgr.getCoinRepository().transferCoins(
                    reservationId, gang.getCoinAccount(), CoinTransaction.GANG_DONATION,
                    "m.gang_donation", "m.gang_donation");
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                _gangrepo.spendScrip(user.gangId, scrip);
                if (_entryId > 0) {
                    _gangrepo.deleteHistoryEntry(_entryId);
                }
            }

            protected void actionCompleted () {
                log.info("Added to gang coffers [who=" + user.who() + ", gangId=" + user.gangId +
                         ", scrip=" + scrip + ", coins=" + coins + "].");
                GangObject gangobj = getGangObject(user.gangId);
                if (gangobj == null) {
                    return; // adder bailed
                }
                try {
                    gangobj.startTransaction();
                    gangobj.setScrip(gangobj.scrip + scrip);
                    gangobj.setCoins(gangobj.coins + coins);
                } finally {
                    gangobj.commitTransaction();
                }
                listener.requestProcessed();
            }
            protected void actionFailed (String cause) {
                listener.requestFailed(INTERNAL_ERROR);
            }
            
            protected int _entryId;
        }.start();
    }

    /**
     * Processes a request to remove a member from a gang.  The entry provided is assumed to
     * describe a member of the specified gang.  At least one member of the gang must be online.
     * When the last member is removed, the gang itself is deleted.
     *
     * @param remover the leader responsible for kicking the member out, or <code>null</code> if
     * the member left voluntarily
     * @param listener a listener that will be notified when the request completes with the name of
     * a gang that was deleted, if any
     */
    public void removeFromGang (
        final int gangId, final int playerId, final Handle handle,
        final Handle remover, final ResultListener<Handle> listener)
        throws InvocationException
    {
        final GangObject gangobj = getGangObject(gangId);
        if (gangobj == null) {
            log.warning("Missing gang object for removal [gangId=" + gangId +
                        ", playerId=" + playerId + ", handle=" + handle + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // determine whether to delete the gang
        boolean delete = (gangobj.members.size() == 1);

        // gangs cannot be left without a leader; if we are removing the last leader, we must
        // promote the most senior non-leader.  otherwise, if the most senior leader leaves,
        // we must note the next most senior leader in order to update their avatar
        GangMemberEntry removal = gangobj.members.get(handle);
        int seniorLeaderId = -1;
        if (removal.rank == LEADER_RANK && remover == null && !delete) {
            boolean leaderless = true;
            GangMemberEntry msenior = null, lsenior = null;
            for (GangMemberEntry member : gangobj.members) {
                if (member == removal) {
                    continue;
                } else if (member.rank == LEADER_RANK &&
                    (lsenior == null || member.joined < lsenior.joined)) {
                    lsenior = member;
                } else if (msenior == null || member.joined < msenior.joined) {
                    msenior = member;
                }
            }
            if (lsenior == null) {
                final int seniorId = lsenior.playerId;
                changeMemberRank(
                    gangId, msenior.playerId, msenior.handle, null, msenior.rank, LEADER_RANK,
                    new InvocationService.ConfirmListener() {
                        public void requestProcessed () {
                            continueRemovingFromGang(
                                gangId, gangobj.name, playerId, handle, null, seniorId, false,
                                listener);
                        }
                        public void requestFailed (String cause) {
                            listener.requestFailed(new InvocationException(cause));
                        }
                    });
                return;
                
            } else if (playerId == gangobj.getSeniorLeader().playerId) {
                seniorLeaderId = lsenior.playerId;
            }
        }

        // continue the process
        continueRemovingFromGang(
            gangId, gangobj.name, playerId, handle, remover, seniorLeaderId, delete, listener);
    }

    /**
     * Processes a request to change a member's rank.  The entry provided is assumed to describe a
     * member of the specified gang.
     *
     * @param changer the leader responsible for changing the member's rank, or <code>null</code>
     * if the rank was changed by default
     */
    public void changeMemberRank (
        final int gangId, final int playerId, final Handle handle, final Handle changer,
        final byte orank, final byte nrank, final InvocationService.ConfirmListener listener)
    {
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _gangrepo.updateRank(playerId, nrank);
                _gangrepo.insertHistoryEntry(gangId, (changer == null) ?
                    MessageBundle.tcompose("m.auto_promotion_entry", handle) :
                    MessageBundle.compose(
                        (orank < nrank) ? "m.promotion_entry" : "m.demotion_entry",
                        MessageBundle.taint(changer),
                        MessageBundle.taint(handle),
                        MessageBundle.qualify(GANG_MSGS, XLATE_RANKS[nrank])));
            }
            public void handleSuccess () {
                log.info("Changed member rank [gangId=" + gangId + ", playerId=" + playerId +
                         ", handle=" + handle + ", rank=" + nrank + "].");
                PlayerObject plobj = BangServer.lookupPlayer(handle);
                if (plobj != null) {
                    plobj.setGangRank(nrank);
                }
                GangObject gangobj = getGangObject(gangId);
                if (gangobj != null) {
                    GangMemberEntry entry = gangobj.members.get(handle);
                    entry.rank = nrank;
                    gangobj.updateMembers(entry);
                }
                listener.requestProcessed();
            }
        });
    }

    /**
     * Fetches a batch of history entries for the specified gang at the requested offset.
     */
    public void getHistoryEntries (final int gangId, final int offset, final int count,
                                   final InvocationService.ResultListener listener)
    {
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                _entries = _gangrepo.loadHistoryEntries(gangId, offset, count);
            }
            public void handleSuccess () {
                listener.requestProcessed(_entries.toArray(new HistoryEntry[_entries.size()]));
            }
            protected ArrayList<HistoryEntry> _entries;
        });
    }

    /**
     * Gets a quote for the specified outfit.  The result listener will receive an array containing
     * the cost in scrip and coins to buy the outfit for every member who doesn't already own it.
     */
    public void getOutfitQuote (PlayerObject user, OutfitArticle[] outfit,
                                InvocationService.ResultListener listener)
        throws InvocationException
    {
        processOutfit(user, outfit, listener, false);
    }
    
    /**
     * Buys the specified outfit for the gang.  The listener will receive an array containing the 
     * number of members who received articles and the total number of articles purchased.
     */
    public void buyOutfits (PlayerObject user, OutfitArticle[] outfit,
                            InvocationService.ResultListener listener)
        throws InvocationException
    {
        processOutfit(user, outfit, listener, true);
    }
    
    // documentation inherited from interface SpeakProvider.SpeakerValidator
    public boolean isValidSpeaker (DObject speakObj, ClientObject speaker, byte mode)
    {
        GangObject gangobj = (GangObject)speakObj;
        PlayerObject user = (PlayerObject)speaker;
        return gangobj.members.containsKey(user.handle);
    }

    /**
     * Returns a translatable string describing the identified amounts (at least one of which must
     * be nonzero).
     */
    protected String getMoneyDesc (int scrip, int coins)
    {
        String sdesc = null, cdesc = null;
        if (scrip > 0) {
            sdesc = MessageBundle.tcompose("m.scrip", String.valueOf(scrip));
            if (coins == 0) {
                return sdesc;
            }
        }
        if (coins > 0) {
            cdesc = MessageBundle.tcompose("m.coins", coins);
            if (scrip == 0) {
                return cdesc;
            }
        }
        return MessageBundle.compose("m.times_2", cdesc, sdesc);
    }

    /**
     * Determines how much it would cost to buy the specified gang outfit and either reports the
     * amount as a price quote or goes through with the purchase.
     */
    protected void processOutfit (
        final PlayerObject user, final OutfitArticle[] outfit,
        final InvocationService.ResultListener listener, final boolean buy)
        throws InvocationException
    {
        // validate the outfit and get the corresponding articles
        // TODO: make sure gang has access to requested articles and colors
        final ArticleCatalog.Article[] catarts = new ArticleCatalog.Article[outfit.length];
        final Article[] articles = new Article[outfit.length];
        for (int ii = 0; ii < outfit.length; ii++) {
            OutfitArticle oart = outfit[ii];
            catarts[ii] = BangServer.alogic.getArticleCatalog().getArticle(oart.article);
            if (catarts[ii] == null) {
                log.warning("Invalid article requested for outfit [who=" + user.who() +
                    ", article=" + oart.article + "].");
                throw new InvocationException(INTERNAL_ERROR);
            }
            articles[ii] = BangServer.alogic.createArticle(-1, catarts[ii], oart.zations);
            if (articles[ii] == null) {
                // an error will have already been logged
                throw new InvocationException(INTERNAL_ERROR);
            }
        }
        
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                // save the outfit as the gang's current
                _gangrepo.updateOutfit(user.gangId, outfit);
                
                // find out who needs the articles and how much it will cost
                ArrayIntSet maleIds = new ArrayIntSet(), femaleIds = new ArrayIntSet();
                _gangrepo.loadMemberIds(user.gangId, maleIds, femaleIds);
                for (int ii = 0; ii < articles.length; ii++) {
                    Article article = articles[ii];
                    ArrayIntSet memberIds = (article.getArticleName().indexOf("female") == -1) ?
                        maleIds : femaleIds;
                    ArrayIntSet ownerIds = BangServer.itemrepo.getItemOwners(memberIds, article);
                    int count = memberIds.size() - ownerIds.size();
                    _cost[0] += (catarts[ii].scrip * count);
                    _cost[1] += (catarts[ii].coins * count);
                    if (buy && count > 0) {
                        _receiverIds[ii] = (ArrayIntSet)memberIds.clone();
                        _receiverIds[ii].removeAll(ownerIds);
                    }
                }
            }
            public void handleSuccess () {
                // update the gang's configured outfit
                GangObject gangobj = getGangObject(user.gangId);
                if (gangobj != null) {
                    gangobj.setOutfit(outfit);
                }
                
                if (!buy) {
                    // if we're not buying, just report the price quote
                    listener.requestProcessed(_cost);
                } else if (_cost[0] == 0 && _cost[1] == 0) {
                    listener.requestProcessed(new int[] { 0, 0 });
                } else if (user.isActive()) {
                    // only proceed if the buyer is still online
                    try {
                        buyOutfits(user, _cost[0], _cost[1], _receiverIds, articles, listener);
                    } catch (InvocationException e) {
                        listener.requestFailed(e.getMessage());
                    }
                }
            }
            protected int[] _cost = new int[2];
            protected ArrayIntSet[] _receiverIds = new ArrayIntSet[articles.length];
        });
    }
    
    /**
     * Having determined which gang members need parts of the outfit and computed the cost,
     * uses fund from the gang's coffers to buy the outfits.
     */
    protected void buyOutfits (
        final PlayerObject user, final int scripCost, final int coinCost,
        final ArrayIntSet[] userIds, final Article[] articles,
        final InvocationService.ResultListener listener)
        throws InvocationException
    {
        final GangObject gang = getGangObject(user.gangId);
        if (gang == null) {
            log.warning("Missing gang object for outfit purchase [who=" + user.who() +
                ", gangId=" + user.gangId + "].");
            listener.requestFailed(INTERNAL_ERROR);
        }
        new GangFinancialAction(user, gang, scripCost, coinCost) {
            protected int getCoinType () {
                return CoinTransaction.GANG_OUTFIT_PURCHASE;
            }
            protected String getCoinDescrip () {
                return "m.gang_outfits";
            }

            protected String persistentAction ()
                throws PersistenceException {
                ArrayIntSet memberIds = new ArrayIntSet();
                for (int ii = 0; ii < userIds.length; ii++) {
                    if (userIds[ii] == null) {
                        continue;
                    }
                    memberIds.addAll(userIds[ii]);
                    BangServer.itemrepo.insertItems(articles[ii], userIds[ii], _items);
                }
                _memberCount = memberIds.size();
                _entryId = _gangrepo.insertHistoryEntry(user.gangId,
                        MessageBundle.compose(
                            "m.outfit_entry",
                            MessageBundle.taint(user.handle),
                            getMoneyDesc(scripCost, coinCost),
                            MessageBundle.tcompose("m.member_count", _memberCount),
                            MessageBundle.tcompose("m.articles", _items.size())));
                return null;
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                if (!_items.isEmpty()) {
                    ArrayIntSet itemIds = new ArrayIntSet();
                    for (Item item : _items) {
                        itemIds.add(item.getItemId());
                    }
                    BangServer.itemrepo.deleteItems(itemIds, "rollback");
                }
                if (_entryId > 0) {
                    _gangrepo.deleteHistoryEntry(_entryId);
                }
            }

            protected void actionCompleted () {
                for (Item item : _items) {
                    PlayerObject owner = BangServer.lookupPlayer(item.getOwnerId());
                    if (owner != null) {
                        owner.addToInventory(item);
                    }
                }
                listener.requestProcessed(new int[] { _memberCount, _items.size() });
            }
            protected void actionFailed (String cause) {
                listener.requestFailed(cause);
            }
            
            protected ArrayList<Item> _items = new ArrayList<Item>();
            protected int _memberCount;
            protected int _entryId;
        }.start();
    }
    
    /**
     * Sends an invitation to join a gang.
     */
    protected void sendGangInvite (
        final PlayerObject user, final Handle inviter, final int gangId,
        final Handle name, String message)
    {
        user.addToNotifications(new GangInvite(inviter, name, message,
            new GangInvite.ResponseHandler() {
            public void handleResponse (int resp, InvocationService.ConfirmListener listener) {
                handleInviteResponse(
                    user, inviter, gangId, name, (resp == GangInvite.ACCEPT), listener);
            }
        }));
    }

    /**
     * Processes the response to a gang invitation.
     */
    protected void handleInviteResponse (
        final PlayerObject user, final Handle inviter, final int gangId, final Handle name,
        final boolean accept, final InvocationService.ConfirmListener listener)
    {
        // update the database
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                _error = _gangrepo.deleteInvite(gangId, user.playerId, accept);
                if (_error == null && accept) {
                    _mrec = new GangMemberRecord(user.playerId, gangId, MEMBER_RANK);
                    _gangrepo.insertMember(_mrec);
                    String hmsg = MessageBundle.tcompose("m.joined_entry", user.handle);
                    _gangrepo.insertHistoryEntry(gangId, hmsg);
                    // if this player is accepting an invitation, the gang to which they are
                    // accepting may not yet be resolved; we can also rely on the fact that if
                    // resolveGang() returns, we'll end up in handleInviteSuccess() where we will
                    // call initPlayer() or releaseGang() as appropriate
                    resolveGang(gangId);
                }
            }
            public void handleSuccess () {
                if (_error != null) {
                    listener.requestFailed(_error);
                } else {
                    handleInviteSuccess(user, inviter, gangId, name, accept, _mrec, listener);
                }
            }
            public String getFailureMessage () {
                return "Failed to respond to gang invite [who=" + user.who() +
                    ", gangId=" + gangId + ", accept=" + accept + "]";
            }
            protected GangMemberRecord _mrec;
            protected String _error;
        });
    }

    /**
     * Handles the omgr portion of the invite processing, once the persistent part has successfully
     * completed.
     */
    protected void handleInviteSuccess (
        PlayerObject user, Handle inviter, int gangId, Handle name, boolean accept,
        GangMemberRecord mrec, InvocationService.ConfirmListener listener)
    {
        PlayerObject invobj = BangServer.lookupPlayer(inviter);
        if (invobj != null) {
            SpeakProvider.sendInfo(invobj, GANG_MSGS,
                MessageBundle.tcompose(
                    accept ? "m.member_accepted" : "m.member_rejected", user.handle, name));
        }
        if (!accept) { // nothing to do
            listener.requestProcessed();
            return;
        }

        // add the member entry
        GangObject gangobj = getGangObject(gangId);
        if (gangobj != null && !gangobj.members.containsKey(user.handle)) {
            gangobj.addToMembers(new GangMemberEntry(
                user.handle, user.playerId, MEMBER_RANK, mrec.joined, 0));
        }
        
        // init the player or release the reference
        if (!user.isActive()) {
            releaseGang(gangId);
            return;
        }
        ArrayList<Comparable> keys = new ArrayList<Comparable>();
        for (Notification notif : user.notifications) {
            // we want to remove all gang invites *except* for the one we're answering, because
            // that one will be removed by the caller
            if (notif instanceof GangInvite && !((GangInvite)notif).gang.equals(name)) {
                keys.add(notif.getKey());
            }
        }
        if (!keys.isEmpty()) {
            try {
                user.startTransaction();
                for (Comparable key : keys) {
                    user.removeFromNotifications(key);
                }
            } finally {
                user.commitTransaction();
            }
        }
        initPlayer(user, mrec, null);
        listener.requestProcessed();
    }

    /**
     * Continues the process of removing a member from a gang, perhaps after having promoted a
     * player to avoid leaving a gang leaderless.
     *
     * @param seniorLeaderId if not -1, the player id of the gang's new senior leader
     */
    protected void continueRemovingFromGang (
        final int gangId, final Handle gangName, final int playerId, final Handle handle,
        final Handle remover, final int seniorLeaderId, final boolean delete,
        final ResultListener<Handle> listener)
    {
        BangServer.invoker.postUnit(new RepositoryListenerUnit<Handle>(listener) {
            public Handle invokePersistResult ()
                throws PersistenceException {
                _gangrepo.deleteMember(playerId);
                if (delete) {
                    _gangrepo.deleteGang(gangId);
                } else {
                    if (seniorLeaderId > 0) {
                        _avatar = BangServer.lookrepo.loadSnapshot(seniorLeaderId);
                    }
                    _gangrepo.insertHistoryEntry(gangId, (remover == null) ?
                        MessageBundle.tcompose("m.left_entry", handle) :
                        MessageBundle.tcompose("m.expelled_entry", remover, handle));
                }
                return (delete ? gangName : null);
            }
            public void handleSuccess () {
                log.info("Removed member from gang [gangId=" + gangId + ", playerId=" + playerId +
                         ", handle=" + handle + ", delete=" + delete + "].");
                // remove the member entry and perhaps start updating the new senior leader
                GangObject gangobj = getGangObject(gangId);
                if (gangobj != null) {
                    gangobj.removeFromMembers(handle);
                    PlayerObject senior = BangServer.lookupPlayer(seniorLeaderId);
                    if (senior != null) {
                        new AvatarUpdater().add(senior, gangobj);
                    } else if (_avatar != null) {
                        gangobj.setAvatar(_avatar);
                    }
                }
                
                // update gang fields if the user didn't log in after being removed
                PlayerObject plobj = BangServer.lookupPlayer(handle);
                if (plobj != null && plobj.gangId > 0) {
                    try {
                        plobj.startTransaction();
                        plobj.setGangId(0);
                        plobj.setGangOid(0);
                    } finally {
                        plobj.commitTransaction();
                    }
                    releaseGang(gangId);
                }
                super.handleSuccess();
            }
            protected int[] _avatar;
        });
    }

    /**
     * Retrieves the identified gang object, initializing it if necessary. Returns null if the gang
     * in question is not currently resolved.
     */
    protected GangObject getGangObject (int gangId)
    {
        BangServer.requireDObjThread(); // safety first
        GangHandler gang = _gangs.get(gangId);
        return (gang == null) ? null : gang.getGangObject();
    }

    /**
     * Unmaps and destroys the given gang object if there are no more members online or in the
     * process of resolution.
     */
    protected void maybeShutdownGang (int gangId)
    {
        GangHandler gang = _gangs.get(gangId);
        if (gang == null || !gang.canBeShutdown()) {
            return;
        }
        _gangs.remove(gang.getGangObject().gangId);
        gang.shutdown();
    }

    /**
     * Converts an actual rank to a rank appropriate for display on a poster.
     */
    protected static byte getPosterRank (byte rank)
    {
        return (rank == RECRUITER_RANK) ? MEMBER_RANK : rank;
    }

    /** Handles information for a particular Gang on the server. */
    protected class GangHandler
    {
        /** The number of members currently online. This will be read and modified on multiple
         * threads. */
        public volatile int online = 1;

        public GangHandler (GangRecord record)
            throws PersistenceException {
            // create and popupate (but do not yet register) our gang distributed object
            _gangobj = new GangObject();
            _gangobj.gangId = record.gangId;
            _gangobj.name = record.getName();
            _gangobj.founded = record.founded.getTime();
            _gangobj.statement = record.statement;
            _gangobj.url = record.url;
            _gangobj.avatar = record.avatar;
            _gangobj.scrip = record.scrip;
            _gangobj.coins = BangServer.coinmgr.getCoinRepository().getCoinCount(
                _gangobj.getCoinAccount());
            _gangobj.notoriety = record.notoriety;
            _gangobj.outfit = record.outfit;
            _gangobj.members = new DSet<GangMemberEntry>(record.members.iterator());
            
            _rankval = new Interval(BangServer.omgr) {
                public void expired () {
                    SaloonManager.refreshTopRanked(getGangObject(),
                        "GANG_MEMBERS", "RATINGS.PLAYER_ID = GANG_MEMBERS.PLAYER_ID and " +
                        "GANG_MEMBERS.GANG_ID = " + _gangobj.gangId, TOP_RANKED_LIST_SIZE);
                }
            };
            _rankval.schedule(1000L, RANK_REFRESH_INTERVAL);
        }

        public GangObject getGangObject () {
            if (_gangobj.getOid() == 0) {
                _gangobj.speakService =
                    (SpeakMarshaller)BangServer.invmgr.registerDispatcher(
                        new SpeakDispatcher(new SpeakProvider(_gangobj, GangManager.this)), false);
                BangServer.omgr.registerObject(_gangobj);
                log.info("Initialized gang object " + this + ".");
            }
            return _gangobj;
        }

        /**
         * Determines whether this Gang can be shutdown (i.e., its distributed object is not active
         * and there are no Gang members online or in the process of resolution).
         */
        public boolean canBeShutdown () {
            return (_gangobj.isActive() && online == 0);
        }

        /**
         * Destroys our Gang object and clears out our other registrations.
         */
        public void shutdown () {
            BangServer.invmgr.clearDispatcher(_gangobj.speakService);
            _rankval.cancel();
            _gangobj.destroy();
            log.info("Gang shutdown " + this + ".");
            _gangobj = null;
        }

        @Override // from Object
        public String toString () {
            return _gangobj.name + " (" + _gangobj.gangId + ")";
        }

        /** Our Gang distributed object. */
        protected GangObject _gangobj;

        /** The interval that refreshes the list of top-ranked members. */
        protected Interval _rankval;
    }

    /** Listens for changes to the avatar of the most senior leader, updating the gang object to
     * match. */
    protected class AvatarUpdater extends SetAdapter
        implements AttributeChangeListener, ElementUpdateListener
    {
        public void add (PlayerObject player, GangObject gang)
        {
            _player = player;
            _gang = gang;
            _player.addListener(this);
            updateAvatar();
        }
        
        @Override // documentation inherited
        public void entryUpdated (EntryUpdatedEvent event)
        {
            if (event.getName().equals(PlayerObject.LOOKS) &&
                event.getEntry() == _player.getLook(Look.Pose.WANTED_POSTER)) {
                updateAvatar();
            }
        }
        
        // documentation inherited from interface AttributeChangeListener
        public void attributeChanged (AttributeChangedEvent event)
        {
            // stop listening if they leave the gang
            if (event.getName().equals(PlayerObject.GANG_ID) && event.getIntValue() <= 0) {
                _player.removeListener(this);
            }
        }
        
        // documentation inherited from interface ElementUpdateListener
        public void elementUpdated (ElementUpdatedEvent event)
        {
            if (event.getName().equals(PlayerObject.POSES) &&
                event.getIndex() == Look.Pose.WANTED_POSTER.ordinal()) {
                updateAvatar();
            }
        }
        
        protected void updateAvatar ()
        {
            Look look = _player.getLook(Look.Pose.WANTED_POSTER);
            _gang.setAvatar(look == null ? null : look.getAvatar(_player));
        }
        
        protected PlayerObject _player;
        protected GangObject _gang;
    }
    
    /** The persistent store for gang data. */
    protected GangRepository _gangrepo;

    /** Maps gang ids to currently loaded gang objects. */
    protected IntMap<GangHandler> _gangs =
        Collections.synchronizedIntMap(new HashIntMap<GangHandler>());

    /** The frequency with which we update the top-ranked member lists. */
    protected static final long RANK_REFRESH_INTERVAL = 60 * 60 * 1000L;

    /** The size of the top-ranked member lists. */
    protected static final int TOP_RANKED_LIST_SIZE = 10;
}
