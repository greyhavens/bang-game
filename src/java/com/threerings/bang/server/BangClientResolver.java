//
// $Id$

package com.threerings.bang.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.RandomUtil;

import com.threerings.crowd.server.CrowdClientResolver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.gang.server.persist.GangInviteRecord;
import com.threerings.bang.gang.server.persist.GangMemberRecord;
import com.threerings.bang.server.persist.FolkRecord;
import com.threerings.bang.server.persist.PardnerRecord;
import com.threerings.bang.server.persist.PlayerRecord;

import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Rating;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;
import com.threerings.bang.data.TrainTicket;
import com.threerings.bang.util.BangUtil;

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

        // check for a stashed player record
        synchronized (_pstash) {
            player = _pstash.remove(username.toLowerCase());
        }

        // if we have nothing in the stash, we need to load them from the db
        if (player == null) {
            player = BangServer.playrepo.loadPlayer(username);
        }

        // if they're not in the db, it's their first time, how nice
        if (player == null) {
            // it's their first time, how nice
            player = new PlayerRecord(username);
            BangServer.playrepo.insertPlayer(player);
            BangServer.author.setAccountIsActive(username, true);
            BangServer.generalLog("first_timer " + username);
        }

        buser.playerId = player.playerId;
        if (player.handle != null) {
            buser.handle = new Handle(player.handle);
        }
        buser.isMale = player.isSet(PlayerRecord.IS_MALE_FLAG);
        buser.scrip = player.scrip;
        buser.coins = BangServer.coinmgr.getCoinRepository().getCoinCount(player.accountName);

        // load up this player's items
        ArrayList<Item> items = BangServer.itemrepo.loadItems(buser.playerId);
        buser.inventory = new DSet<Item>(items.iterator());

        // load up this player's persistent stats
        ArrayList<Stat> stats = BangServer.statrepo.loadStats(buser.playerId);
        buser.stats = new StatSet(stats.iterator());
        buser.stats.setContainer(buser);

        // if this player started playing before the end of beta and they've played 20 ranked
        // games, give them a free permanent ticket to ITP
        int itpidx = BangUtil.getTownIndex(BangCodes.INDIAN_POST);
        if (buser.playerId < BangCodes.BETA_PLAYER_CUTOFF &&
            buser.stats.getIntStat(Stat.Type.GAMES_PLAYED) >= FREE_ITP_GP_REQUIREMENT &&
            !buser.holdsTicket(BangCodes.INDIAN_POST)) {
            log.info("Granting free ITP ticket to beta player [who=" + username +
                     ", handle=" + buser.handle + ", pid=" + buser.playerId +
                     ", games=" + buser.stats.getIntStat(Stat.Type.GAMES_PLAYED) + "].");
            TrainTicket ticket = new TrainTicket(buser.playerId, itpidx);
            BangServer.itemrepo.insertItem(ticket);
            BangServer.playrepo.grantTownAccess(buser.playerId, ticket.getTownId());
            buser.addToInventory(ticket);

        // fix bug with ticket granting
        } else if (buser.holdsTicket(BangCodes.INDIAN_POST) &&
                   ((player.townId == null) || player.townId.equals(BangCodes.FRONTIER_TOWN))) {
            BangServer.playrepo.grantTownAccess(buser.playerId, BangCodes.INDIAN_POST);
        }

        // if we're giving out free access to ITP, give the user a temporary ITP ticket for this
        // session (if they don't already have one)
        if (RuntimeConfig.server.freeIndianPost &&
            !buser.holdsTicket(BangCodes.INDIAN_POST)) {
            buser.addToInventory(new TrainTicket(buser.playerId, itpidx));
        }

        // load up this player's ratings
        ArrayList<Rating> ratings = BangServer.ratingrepo.loadRatings(buser.playerId);
        buser.ratings = new DSet<Rating>(ratings.iterator());

        // load up this player's avatar looks
        buser.looks = new DSet<Look>(BangServer.lookrepo.loadLooks(player.playerId).iterator());

        // configure their chosen poses
        buser.poses = new String[Look.POSE_COUNT];
        buser.poses[Look.Pose.DEFAULT.ordinal()] = player.look;
        buser.poses[Look.Pose.VICTORY.ordinal()] = player.victoryLook;
        buser.poses[Look.Pose.WANTED_POSTER.ordinal()] = player.wantedLook;

        // initialize the set of notifications
        buser.notifications = new DSet<Notification>();

        // load up this player's pardners
        _precords = BangServer.playmgr.getPardnerRepository().getPardnerRecords(player.playerId);

        // load up this player's gang information
        _grecord = BangServer.gangmgr.getGangRepository().loadMember(player.playerId);
        if (_grecord == null) {
            _ginvites = BangServer.gangmgr.getGangRepository().getInviteRecords(player.playerId);
        } else {
            // make sure this gang's data is loaded from the database
            BangServer.gangmgr.resolveGang(_grecord.gangId);
        }

        // load this player's friends and foes
        ArrayList<FolkRecord> folks = BangServer.playrepo.loadOpinions(buser.playerId);
        ArrayIntSet friends = new ArrayIntSet(), foes = new ArrayIntSet();
        for (FolkRecord folk : folks) {
            (folk.opinion == FolkRecord.FRIEND ? friends : foes).add(folk.targetId);
        }
        // toIntArray() returns a sorted array
        buser.friends = friends.toIntArray();
        buser.foes = foes.toIntArray();
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

    @Override // documentation inherited
    protected void reportFailure (Exception cause)
    {
        super.reportFailure(cause);

        // let the gang manager know we're not going to finish our gang resolution
        PlayerObject buser = (PlayerObject)_clobj;
        if (buser.gangId > 0) {
            BangServer.gangmgr.releaseGang(buser.gangId);
        }
    }

    /** A temporary handle on this player's pardner records. */
    protected ArrayList<PardnerRecord> _precords;

    /** A temporary handle on this player's gang membership information (or null). */
    protected GangMemberRecord _grecord;

    /** A temporary list of this player's gang invitations (or null). */
    protected ArrayList<GangInviteRecord> _ginvites;

    /** Used to temporarily store player records during resolution. */
    protected static HashMap<String,PlayerRecord> _pstash = new HashMap<String,PlayerRecord>();

    /** The number of rated games a player has to have played to get a free ticket to ITP. */
    protected static final int FREE_ITP_GP_REQUIREMENT = 20;
}
