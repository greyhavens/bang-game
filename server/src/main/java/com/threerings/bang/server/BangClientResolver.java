//
// $Id$

package com.threerings.bang.server;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;

import com.threerings.crowd.server.CrowdClientResolver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;

import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.server.persist.GangInviteRecord;
import com.threerings.bang.gang.server.persist.GangMemberRecord;
import com.threerings.bang.gang.server.persist.GangRepository;

import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.server.persist.LookRepository;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.saloon.data.TopRankedList;
import com.threerings.bang.saloon.data.TopRankObject;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangCredentials;
import com.threerings.bang.data.BangTokenRing;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.FreeTicket;
import com.threerings.bang.data.GuestHandle;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Rating;
import com.threerings.bang.data.StatType;
import com.threerings.bang.data.TrainTicket;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.server.persist.BangStatRepository;
import com.threerings.bang.server.persist.FolkRecord;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PardnerRecord;
import com.threerings.bang.server.persist.PlayerRecord;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.server.persist.RatingRepository;

import static com.threerings.bang.Log.log;

/**
 * Customizes the client resolver to use our {@link PlayerObject}.
 */
public class BangClientResolver extends CrowdClientResolver
{
    /**
     * This is called earlier in the authentication process where we have to load an account's
     * player record, so we stash it here to avoid loading it again when the time comes to resolve
     * their data.
     */
    public static void stashPlayer (PlayerRecord player)
    {
        synchronized (_pstash) {
            _pstash.put(player.accountName.toLowerCase(), player);
        }
    }

    /**
     * This is called earlier in the authentication process where we find out the user's age
     * so we stash it here to avoid loading it again when the time comes to resolve their data.
     */
    public static void stashPlayerOver13 (String accountName)
    {
        synchronized (_astash) {
            _astash.add(accountName.toLowerCase());
        }
    }

    // documentation inherited
    public ClientObject createClientObject ()
    {
        return new PlayerObject();
    }

    // documentation inherited
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);
        PlayerObject buser = (PlayerObject)clobj;
        String username = buser.username.toString();
        PlayerRecord player;

        log.info("Resolving " + username + "...");

        // check for a stashed player record
        synchronized (_pstash) {
            player = _pstash.remove(username.toLowerCase());
        }

        // if we have nothing in the stash, we need to load them from the db
        if (player == null) {
            player = _playrepo.loadPlayer(username);
        }

        // if they're not in the db, it's their first time, how nice
        if (player == null) {
            BangSession client = (BangSession)BangServer.clmgr.getClient(buser.username);
            boolean anonymous = ((BangCredentials)client.getCredentials()).anonymous;
            player = new PlayerRecord(username, anonymous);
            _playrepo.insertPlayer(player);
            if (!anonymous) {
                BangServer.author.setAccountIsActive(username, true);
                synchronized (_astash) {
                    if (_astash.remove(username.toLowerCase())) {
                        player.isOver13 = true;
                    }
                }
            }
            BangServer.generalLog(
                    "first_timer " + player.playerId + (anonymous ? " anon" : " account"));
        }

        buser.playerId = player.playerId;
        if (player.handle != null) {
            buser.handle = new Handle(player.handle);
        } else {
            buser.handle = new GuestHandle("!!" + username);
        }
        buser.isMale = player.isSet(PlayerRecord.IS_MALE_FLAG);
        buser.tokens.setToken(BangTokenRing.ANONYMOUS, player.isSet(PlayerRecord.IS_ANONYMOUS));
        buser.tokens.setToken(BangTokenRing.OVER_13, player.isOver13);
        buser.tokens.setToken(BangTokenRing.DEMO, player.isSet(PlayerRecord.IS_DEMO_ACCOUNT));
        buser.scrip = player.scrip;
        // buser.coins = _coinmgr.getCoinRepository().getCoinCount(player.accountName);

        // load up this player's gang information
        _grecord = _gangrepo.loadMember(player.playerId);
        if (_grecord == null) {
            _ginvites = _gangrepo.getInviteRecords(player.playerId);
        }
        int gangId = (_grecord == null ? 0 : _grecord.gangId);

        // load up this player's items
        List<Item> items = _itemrepo.loadItems(buser.playerId);
        long now = System.currentTimeMillis();
        // check for expired items
        ArrayIntSet removals = new ArrayIntSet();
        for (Iterator<Item> iter = items.iterator(); iter.hasNext(); ) {
            Item item = iter.next();
            int gid = item.getGangId();
            if (item.isExpired(now) || (gid != 0 && gid != gangId)) {
                removals.add(item.getItemId());
                iter.remove();
                _itemrepo.deleteItem(item, "Item expired");
            }
        }

        // if they have no bigshots, give them the starter bigshot (first one's free kid)
        if (!Iterables.filter(items, BigShotItem.class).iterator().hasNext()) {
            BigShotItem bsitem = new BigShotItem(buser.playerId, FREE_BIGSHOT_TYPE);
            _itemrepo.insertItem(bsitem);
            items.add(bsitem);
        }

        // finally place their items into their inventory DSet
        buser.inventory = new DSet<Item>(items.iterator());

        // load up this player's persistent stats
        List<Stat> stats = _statrepo.loadStats(buser.playerId);
        buser.stats = new StatSet(stats.iterator());
        buser.stats.setContainer(buser);

        // clear a players granted access when tickets expire
        if (!buser.holdsTicket(player.townId)) {
            String townAccess = BangCodes.FRONTIER_TOWN;
            for (int ii = 1; ii < BangCodes.TOWN_IDS.length; ii++) {
                if (buser.holdsTicket(BangCodes.TOWN_IDS[ii])) {
                    townAccess = BangCodes.TOWN_IDS[ii];
                }
            }
            _playrepo.grantTownAccess(buser.playerId, townAccess);
        }

        // if they have an expired free ticket, remove it
        boolean noFreeTicket = true;
        for (Item item : items) {
            if (!(item instanceof FreeTicket)) {
                continue;
            }
            noFreeTicket = false;
            FreeTicket ticket = (FreeTicket)item;
            if (ticket.isExpired(System.currentTimeMillis()) ||
                    buser.holdsTicket(ticket.getTownId())) {
                _itemrepo.deleteItem(item, "Free Ticket Expired");
                buser.removeFromInventory(item.getKey());
            }
        }

        // give out a free ticket if a player qualified but never successfully made it to ITP
        if (DeploymentConfig.usesCoins() && noFreeTicket && player.nextTown == null &&
            buser.stats.containsValue(StatType.FREE_TICKETS, BangCodes.INDIAN_POST) &&
            !buser.stats.containsValue(StatType.ACTIVATED_TICKETS, BangCodes.INDIAN_POST)) {
            FreeTicket ticket = FreeTicket.checkQualifies(
                buser, BangUtil.getTownIndex(BangCodes.INDIAN_POST));
            if (ticket != null) {
                _itemrepo.insertItem(ticket);
                buser.addToInventory(ticket);
            }
        }

        // if we're giving out free access to ITP, give the user a temporary ITP ticket for this
        // session (if they don't already have one)
        int itpidx = BangUtil.getTownIndex(BangCodes.INDIAN_POST);
        boolean holdsITPTicket = buser.holdsTicket(BangCodes.INDIAN_POST);
        if (RuntimeConfig.server.freeIndianPost && !buser.tokens.isAnonymous() && !holdsITPTicket &&
                !buser.holdsFreeTicket(BangCodes.INDIAN_POST)) {
            buser.addToInventory(new TrainTicket(buser.playerId, itpidx));
        }

        // load up this player's ratings
        buser.ratings = new HashMap<Date, HashMap<String, Rating>>();
        buser.ratings.put(null, _ratingrepo.loadRatings(buser.playerId, null));
        for (int ii = 0; ii < PlayerManager.SHOW_WEEKS; ii++) {
            Date week = Rating.getWeek(ii);
            buser.ratings.put(week, _ratingrepo.loadRatings(buser.playerId, week));
        }

        // load up this player's avatar looks and modify any looks that have now expired articles
        List<Look> looks = _lookrepo.loadLooks(player.playerId);
        List<Look> modified = AvatarLogic.stripLooks(removals, buser.inventory, looks);
        for (Look look : modified) {
            _lookrepo.updateLook(buser.playerId, look);
        }
        buser.looks = new DSet<Look>(looks);

        // configure their chosen poses
        buser.poses = new String[Look.POSE_COUNT];
        buser.poses[Look.Pose.DEFAULT.ordinal()] = player.look;
        buser.poses[Look.Pose.VICTORY.ordinal()] = player.victoryLook;
        buser.poses[Look.Pose.WANTED_POSTER.ordinal()] = player.wantedLook;

        // initialize the set of notifications
        buser.notifications = new DSet<Notification>();

        // send any warning messages
        if (player.warning != null) {
            BangServer.playmgr.sendWarningMessage(buser, player.banExpires != null, player.warning);
            // clear out stale temp ban information
            if (player.banExpires != null) {
                _playrepo.clearTempBan(player.playerId);
            }
        }

        // load up this player's pardners
        _precords = BangServer.playmgr.getPardnerRepository().getPardnerRecords(player.playerId);

        // load this player's friends and foes
        List<FolkRecord> folks = _playrepo.loadOpinions(buser.playerId);
        ArrayIntSet friends = new ArrayIntSet(), foes = new ArrayIntSet();
        for (FolkRecord folk : folks) {
            (folk.opinion == FolkRecord.FRIEND ? friends : foes).add(folk.targetId);
        }
        // toIntArray() returns a sorted array
        buser.friends = friends.toIntArray();
        buser.foes = foes.toIntArray();

        // see if they were in the top 10 last week
        TopRankObject rankobj = (TopRankObject)BangServer.saloonmgr.getPlaceObject();
        for (TopRankedList trl : rankobj.getTopRanked()) {
            if (trl.period != TopRankedList.LAST_WEEK) {
                continue;
            }
            for (int ii = 0; ii < trl.playerIds.length; ii++) {
                if (buser.playerId == trl.playerIds[ii]) {
                    buser.stats.addToSetStat(StatType.WEEKLY_TOP10, trl.criterion);
                    if (ii == 0) {
                        buser.stats.addToSetStat(StatType.WEEKLY_WINNER, trl.criterion);
                    }
                    break;
                }
            }
        }
    }

    @Override // documentation inherited
    public void handleResult ()
    {
        // if something went wrong or the player isn't in a gang, we can go right to the final
        // processing stage
        if (_grecord == null || _failure != null) {
            super.handleResult();
            return;
        }
        // otherwise, we must resolve the gang through the gang manager
        BangServer.gangmgr.resolveGang(_grecord.gangId,
            new ResultListener<GangObject>() {
                public void requestCompleted (GangObject result) {
                    updateRentedItems((PlayerObject)_clobj, result);
                }
                public void requestFailed (Exception cause) {
                    // let them log in as if they didn't belong to a gang
                    _grecord = null;
                    BangClientResolver.super.handleResult();
                }
            });
    }

    /**
     * Checks for expired rented items.
     */
    public void updateRentedItems (final PlayerObject buser, GangObject gangobj)
    {
        final ArrayIntSet removed = new ArrayIntSet();
        final List<Item> updated = new ArrayList<Item>();
        final List<Item> added = new ArrayList<Item>();
        for (Item item : buser.inventorySnapshot()) {
            if (item.getGangId() == 0) {
                continue;
            }
            if (item.getGangId() != gangobj.gangId) {
                removed.add(item.getItemId());
                buser.removeFromInventory(item.getKey());
                continue;
            }
            Item gitem = null;
            for (Item gi : gangobj.inventory) {
                if (gi.isEquivalent(item)) {
                    gitem = gi;
                    break;
                }
            }
            if (gitem == null || gitem.isExpired(System.currentTimeMillis())) {
                removed.add(item.getItemId());
                buser.removeFromInventory(item.getKey());
                continue;
            }
            if (item.getExpires() != gitem.getExpires()) {
                item.setExpires(gitem.getExpires());
                updated.add(item);
            }
        }
        for (Item item : gangobj.inventorySnapshot()) {
            if (item.canBeOwned(buser) && !buser.holdsEquivalentItem(item)) {
                Item citem = (Item)item.clone();
                citem.setGangId(citem.getOwnerId());
                citem.setOwnerId(buser.playerId);
                citem.setGangOwned(false);
                citem.setItemId(0);
                added.add(citem);
            }
        }

        final List<Look> modified = AvatarLogic.stripLooks(removed, buser.inventory, buser.looks);
        for (Look look : modified) {
            buser.updateLooks(look);
        }
        if (!removed.isEmpty() || !updated.isEmpty() || !added.isEmpty()) {
            BangServer.invoker.postUnit(new Invoker.Unit("updateRentedItems") {
                public boolean invoke () {
                    try {
                        if (!removed.isEmpty()) {
                            _itemrepo.deleteItems(removed, "Gang items expired");
                        }
                        for (Item item : updated) {
                            _itemrepo.updateItem(item);
                        }
                        for (Look look : modified) {
                            _lookrepo.updateLook(buser.playerId, look);
                        }
                        for (Item item : added) {
                            _itemrepo.insertItem(item);
                        }
                    } catch (PersistenceException pe) {
                        log.warning("Failed to update player gang items.", "user", buser.who(), pe);
                    }
                    return true;
                }
                public void handleResult() {
                    for (Item item : added) {
                        if (item.getItemId() > 0) {
                            buser.addToInventory(item);
                        }
                    }
                    BangClientResolver.super.handleResult();
                }
            });
        } else {
            super.handleResult();
        }
    }

    @Override // documentation inherited
    protected void finishResolution (ClientObject clobj)
    {
        super.finishResolution(clobj);
        PlayerObject buser = (PlayerObject)clobj;

        // initialize our pardner information
        BangServer.playmgr.initPardners(buser, _precords);

        // initialize our gang information
        BangServer.gangmgr.initPlayer(buser, _grecord, _ginvites);
    }

    /** A temporary handle on this player's pardner records. */
    protected List<PardnerRecord> _precords;

    /** A temporary handle on this player's gang membership information (or null). */
    protected GangMemberRecord _grecord;

    /** A temporary list of this player's gang invitations (or null). */
    protected List<GangInviteRecord> _ginvites;

    // dependencies
    @Inject protected PlayerRepository _playrepo;
    @Inject protected GangRepository _gangrepo;
    @Inject protected ItemRepository _itemrepo;
    @Inject protected BangStatRepository _statrepo;
    @Inject protected RatingRepository _ratingrepo;
    @Inject protected LookRepository _lookrepo;

    /** Used to temporarily store player records during resolution. */
    protected static Map<String,PlayerRecord> _pstash = new HashMap<String,PlayerRecord>();

    /** Used to temporarily store player age during resolution. */
    protected static Set<String> _astash = new HashSet<String>();

    /** The type of Big Shot given out free to new players. */
    protected static final String FREE_BIGSHOT_TYPE = "frontier_town/cavalry";
}
