//
// $Id$

package com.threerings.bang.server;

import java.io.File;
import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.Interator;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import org.apache.commons.io.IOUtils;

import com.threerings.io.Streamable;
import com.threerings.resource.ResourceManager;

import com.threerings.presents.annotation.AuthInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SpeakObject;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.server.ChatHistory;
import com.threerings.crowd.chat.server.SpeakUtil;
import com.threerings.parlor.server.ParlorSender;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.StreamableHashMap;

import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.server.persist.LookRepository;
import com.threerings.bang.data.PlayerMarshaller;
import com.threerings.bang.gang.server.persist.GangMemberRecord;
import com.threerings.bang.gang.server.persist.GangRepository;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.server.Match;
import com.threerings.bang.store.data.CardPackGood;
import com.threerings.bang.store.server.GoodsCatalog;
import com.threerings.bang.store.server.Provider;

import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.data.scenario.PracticeInfo;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.util.TutorialUtil;

import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.BangClientInfo;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangCredentials;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.EntryReplacedEvent;
import com.threerings.bang.data.GoldPass;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PardnerInvite;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.PosterInfo;
import com.threerings.bang.data.Rating;
import com.threerings.bang.data.Star;
import com.threerings.bang.data.StatType;
import com.threerings.bang.data.Warning;
import com.threerings.bang.util.BangUtil;

import com.threerings.bang.server.BangPeerManager;
import com.threerings.bang.server.persist.BangStatRepository;
import com.threerings.bang.server.persist.FolkRecord;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PardnerRecord;
import com.threerings.bang.server.persist.PardnerRepository;
import com.threerings.bang.server.persist.PlayerRecord;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.server.persist.PosterRecord;
import com.threerings.bang.server.persist.PosterRepository;
import com.threerings.bang.server.persist.RatingRepository;

import static com.threerings.bang.Log.log;

/**
 * Handles general player business, implements {@link PlayerProvider}.
 */
@Singleton
public class PlayerManager
    implements PlayerProvider, BangCodes
{
    /** Add the receivedChatListener to any SpeakObjects where you want to record messages
     * sent and received in the players stats. */
    public MessageListener receivedChatListener = new MessageListener() {
        public void messageReceived (MessageEvent event)
        {
            if (!event.getName().equals(ChatCodes.CHAT_NOTIFICATION)) {
                return;
            }
            ChatMessage msg = (ChatMessage)event.getArgs()[0];
            if (!(msg instanceof UserMessage)) {
                return;
            }
            Name speaker = ((UserMessage)msg).speaker;
            if (speaker != null) {
                PlayerObject user = (PlayerObject)BangServer.locator.lookupBody(speaker);
                if (user != null) {
                    user.stats.incrementStat(StatType.CHAT_SENT, 1);
                }
            }
            DObject speakObj = BangServer.omgr.getObject(event.getTargetOid());
            if (speakObj != null && speakObj instanceof SpeakObject) {
                ((SpeakObject)speakObj).applyToListeners(_messageCounter);
            }
        }
    };

    /**
     * Initializes the player manager, and registers its invocation service.
     */
    public void init ()
        throws PersistenceException
    {
        // register ourselves as the provider of the (bootstrap) PlayerService
        BangServer.invmgr.registerProvider(this, PlayerMarshaller.class, GLOBAL_GROUP);

        // register our remote player observer and poster cache observer
        _pardwatcher = new RemotePlayerWatcher<PardnerEntry>() {
            protected String getSetName () {
                return PlayerObject.PARDNERS;
            }
            protected void updateEntry (BangClientInfo info, int townIndex, PardnerEntry entry) {
                if (townIndex >= 0) {
                    entry.setOnline(townIndex);
                    entry.avatar = info.avatar;
                } else {
                    entry.status = PardnerEntry.OFFLINE;
                    entry.avatar = null;
                }
                entry.gameOid = 0;
            }
        };
        if (_peermgr.isRunning()) {
            _peermgr.addPlayerObserver(_pardwatcher);
            _peermgr.addStaleCacheObserver(POSTER_CACHE,
                new PeerManager.StaleCacheObserver() {
                    public void changedCacheData (Streamable data) {
                        _posterCache.remove(data);
                    }
                });
            // make sure we boot a local client if they login to a remote server
            _peermgr.addPlayerObserver(new BangPeerManager.RemotePlayerObserver() {
                public void remotePlayerLoggedOn (int townIndex, BangClientInfo info) {
                    PresentsSession pclient = BangServer.clmgr.getClient(info.username);
                    if (pclient != null) {
                        log.info("Booting user who logged onto remote server",
                                 "username", info.username, "townIndex", townIndex);
                        pclient.endSession();
                    }
                }
                public void remotePlayerLoggedOff (int townIndex, BangClientInfo info) { }
                public void remotePlayerChangedHandle (
                    int townIndex, Handle oldHandle, Handle newHandle) { }
            });
        }

        // register our download symlink cleaning interval; note that because this simply posts an
        // invoker unit, it is not routed through the dobjmgr
        new Interval(BangServer.invoker) {
            public void expired () {
                purgeDownloadLinks();
            }
        }.schedule(DOWNLOAD_PURGE_INTERVAL, true);

        // register our player purging interval if we're on frontier town
        if (BangCodes.FRONTIER_TOWN.equals(ServerConfig.townId)) {
            new Interval(Interval.RUN_DIRECT) {
                public void expired () {
                    purgeExpiredPlayers();
                }
            }.schedule(PLAYER_PURGE_INTERVAL, true);
        }

        // register our late night purging interval
        new Interval(BangServer.omgr) {
            public void expired () {
                clearLateNighters();
            }
        }.schedule(LATE_NIGHT_INTERVAL, true);
    }

    /**
     * Returns the pardner repository.
     */
    public PardnerRepository getPardnerRepository ()
    {
        return _pardrepo;
    }

    /**
     * Populates the identified player's set of pardners, performing any notifications and updates
     * that were being held until the player logged on.
     */
    public void initPardners (final PlayerObject player, List<PardnerRecord> records)
    {
        // TEMP: sanity check since I've seen duplicates
        HashSet<Handle> temp = new HashSet<Handle>();
        Iterator<PardnerRecord> iter = records.iterator();
        while (iter.hasNext()) {
            PardnerRecord record = iter.next();
            if (temp.contains(record.handle)) {
                log.warning("Player has duplicate pardner record", "pid", player.playerId,
                            "record", record);
                iter.remove();
            } else {
                temp.add(record.handle);
            }
        }
        // END TEMP

        // collect active players, send invitations
        List<PardnerEntry> pardners = new ArrayList<PardnerEntry>();
        for (PardnerRecord record : records) {
            if (record.isActive()) {
                pardners.add(getPardnerEntry(record.handle, record.lastSession));
            } else {
                sendPardnerInviteLocal(player, record.handle, record.message, record.lastSession);
            }
        }

        // set pardners and add an updater for this player
        player.setPardners(new DSet<PardnerEntry>(pardners.iterator()));
        if (player.getOnlinePardnerCount() > 0) {
            PardnerEntryUpdater updater = new PardnerEntryUpdater(player);
            _updaters.put(player.handle, updater);
            updater.updateEntries();
        }

        // finally add a mapper that will keep their pardner set mapped so that we can efficiently
        // handle remote pardner logon/logoff
        _pardwatcher.registerListener(new RemotePlayerWatcher.Container<PardnerEntry>() {
            public DObject getObject () {
                return player;
            }
            public Iterable<PardnerEntry> getEntries () {
                return player.pardners;
            }
            public PardnerEntry getEntry (Handle key) {
                return player.pardners.get(key);
            }
            public void updateEntry (PardnerEntry entry) {
                player.updatePardners(entry);
            }
            public void renameEntry (PardnerEntry oldEntry, Handle newHandle) {
                PardnerEntry newEntry = (PardnerEntry)oldEntry.clone();
                newEntry.handle = newHandle;
                BangServer.omgr.postEvent(new EntryReplacedEvent<PardnerEntry>(
                    player, PlayerObject.PARDNERS, oldEntry.handle, newEntry));
            }
            public String toString () {
                return player.who();
            }
        });
    }

    /**
     * Called from {@link BangSession#sessionWillStart} to redeem any rewards for which this
     * player is eligible.
     */
    public void redeemRewards (PlayerObject player, List<String> rewards)
    {
        for (String reward : rewards) {
            try {
                String[] data = reward.split(":");
                if (data[1].equalsIgnoreCase("coins")) {
                    int coins = Integer.parseInt(data[2]);
                    log.info("Granting coin reward", "account", player.username, "coins", coins);
                    // _coinmgr.grantRewardCoins(player, coins);

                }  else if (data[1].equalsIgnoreCase("billing") &&
                            data[2].equalsIgnoreCase("goldpass")) {
                    String townId = null;
                    for (String id : BangCodes.TOWN_IDS) {
                        if (id.equals(data[3])) {
                            townId = id;
                            break;
                        }
                    }
                    if (townId == null) {
                        throw new Exception("Invalid townId");
                    }

                    log.info("Granting Gold Pass reward", "account", player.username,
                             "townId", townId);
                    giveGoldPass(player, townId, false);

                }  else if (data[1].equalsIgnoreCase("billing") &&
                            data[2].equalsIgnoreCase("wrangler")) {
                    log.info("Granting Wrangler Pass reward", "account", player.username);
                    giveGoldPass(player, BangCodes.FRONTIER_TOWN, true);
                }

            } catch (Exception e) {
                // sorry kid, not much we can do for you
                log.warning("Failed to award reward to player", "who", player.who(),
                            "reward", reward, e);
            }
        }
    }

    // documentation inherited from interface PlayerProvider
    public void invitePardner (final PlayerObject inviter, final Handle handle, final String message,
                               final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure we're not anonymous (the client should prevent this)
        if (inviter.tokens.isAnonymous() || !inviter.hasCharacter()) {
            throw new InvocationException(INTERNAL_ERROR);

        // make sure it's not the player himself, that it's not already
        // a pardner, and that the player is under the limit
        } else if (inviter.handle.equals(handle)) {
            throw new InvocationException("e.pardner_self");

        } else if (inviter.pardners.containsKey(handle)) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.already_pardner", handle));

        } else if (inviter.pardners.size() >= MAX_PARDNERS) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.too_many_pardners", String.valueOf(MAX_PARDNERS)));
        }

        // store the invite in the db and send it
        BangServer.invoker.postUnit(new PersistingUnit("invitePardner", listener) {
            public void invokePersistent () throws PersistenceException {
                _error = _pardrepo.addPardners(inviter.playerId, handle, message);
            }
            public void handleSuccess () {
                if (_error != null) {
                    listener.requestFailed(_error);
                    return;
                }
                sendPardnerInvite(handle, inviter.handle, message);
                listener.requestProcessed();
            }
            public String getFailureMessage () {
                return "Failed to invite pardner [who=" + inviter.who() +
                    ", handle=" + handle + "]";
            }
            protected String _error;
        });
    }

    // documentation inherited from interface PlayerProvider
    public void respondToNotification (final PlayerObject player, final Comparable<?> key, int resp,
                                       final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure the notification exists
        Notification notification = player.notifications.get(key);
        if (notification == null) {
            log.warning("Missing notification for response", "who", player.who(), "key", key);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure the response is valid
        if (resp >= notification.getResponses().length) {
            log.warning("Received invalid response for notification", "who", player.who(),
                        "notification", notification, "response", resp);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // transfer control to the handler, removing the notification on success
        notification.handler.handleResponse(resp, new PlayerService.ConfirmListener() {
            public void requestProcessed () {
                // make sure the note is still there, as there are situations where notes will be
                // cleared en masse
                if (player.notifications.containsKey(key)) {
                    player.removeFromNotifications(key);
                }
                listener.requestProcessed();
            }
            public void requestFailed (String cause) {
                listener.requestFailed(cause);
            }
        });
    }

    // documentation inherited from interface PlayerProvider
    public void removePardner (final PlayerObject player, final Handle handle,
                               final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure the pardner entry is present
        PardnerEntry entry = player.pardners.get(handle);
        if (entry == null) {
            throw new InvocationException(INTERNAL_ERROR);
        }

        // remove from database and notify affected party on success
        BangServer.invoker.postUnit(new PersistingUnit("removePardner", listener) {
            public void invokePersistent () throws PersistenceException {
                _pardrepo.removePardners(player.playerId, handle);
            }
            public void handleSuccess () {
                player.removeFromPardners(handle);
                removePardner(handle, player.handle);
                listener.requestProcessed();
            }
            public String getFailureMessage () {
                return "Failed to remove pardner [who=" + player.who() +
                    ", pardner=" + handle + "]";
            }
        });
    }

    // documentation inherited from interface PlayerProvider
    public void playTutorial (PlayerObject player, String tutId,
                              PlayerService.InvocationListener il)
        throws InvocationException
    {
        // if we're not allowing new games, fail immediately
        if (!RuntimeConfig.server.allowNewGames) {
            throw new InvocationException(SaloonCodes.NEW_GAMES_DISABLED);
        }

        // make sure the tutorial is valid for this town
        int tutIdx = ListUtil.indexOf(TutorialCodes.NEW_TUTORIALS[ServerConfig.townIndex], tutId);
        if (!player.tokens.isAdmin() && // allow admin to play test tutorials
            tutIdx == -1) {
            log.warning("Player req'd invalid tutorial", "who", player.who(),
                        "town", player.townId, "tutid", tutId);
            throw new InvocationException(INTERNAL_ERROR);
        }

        BangConfig config = new BangConfig();
        config.rated = false;
        config.players = new Name[2];
        config.ais = new BangAI[2];
        config.init(2, 2);

        // if this is a "practice versus the computer" tutorial, start up a special two player game
        // in lieu of a proper tutorial
        if (tutId.startsWith(TutorialCodes.PRACTICE_PREFIX)) {
            String scenId = tutId.substring(TutorialCodes.PRACTICE_PREFIX.length());
            TutorialCodes.PracticeConfig pconfig = TutorialCodes.PRACTICE_CONFIGS.get(scenId);
            if (pconfig != null) {
                config.addRound(scenId, pconfig.board, null);
                BangConfig.Player pl = config.plist.get(0);
                pl.bigShot = pconfig.bigshot;
                if (pconfig.units != null) {
                    pl.units = pconfig.units;
                }
                pl.cards = pconfig.cards;
            } else {
                config.addRound(scenId, null, null);
            }
            config.duration = BangConfig.Duration.PRACTICE;

        } else {
            // otherwise load up the tutorial configuration and use that to
            // configure the tutorial game
            TutorialConfig tconfig = TutorialUtil.loadTutorial(_rsrcmgr, tutId);
            if ("error".equals(tconfig.board)) {
                throw new InvocationException(INTERNAL_ERROR);
            }
            config.addRound(tconfig.ident, tconfig.board, null);
            config.type = BangConfig.Type.TUTORIAL;
        }

        BangObject bangobj = playComputer(player, config, false);
        if (bangobj != null) {
            bangobj.actionId = 0;
        }
    }

    // documentation inherited from interface PlayerProvider
    public void playPractice (PlayerObject player, String unit, PlayerService.InvocationListener il)
        throws InvocationException
    {
        // if we're not allowing new games, fail immediately
        if (!RuntimeConfig.server.allowNewGames) {
            throw new InvocationException(SaloonCodes.NEW_GAMES_DISABLED);
        }

        // create a game configuration
        BangConfig config = new BangConfig();
        config.rated = false;
        config.type = BangConfig.Type.PRACTICE;
        config.players = new Name[2];
        config.ais = new BangAI[2];
        config.init(2, 2);
        config.addRound(unit, PracticeInfo.getBoardName(ServerConfig.townId), null);
        playComputer(player, config, false);
    }

    // documentation inherited from interface PlayerProvider
    public void playComputer (PlayerObject player, int players, String[] scenarios, String board,
                              boolean autoplay, PlayerService.InvocationListener listener)
        throws InvocationException
    {
        // if we're not allowing new games, fail immediately
        if (!RuntimeConfig.server.allowNewGames) {
            throw new InvocationException(SaloonCodes.NEW_GAMES_DISABLED);
        }

        // sanity check the parameters
        if (players < 2 || players > GameCodes.MAX_PLAYERS) {
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure the scenario types are valid for this town
        for (String scenId : scenarios) {
            ScenarioInfo info = ScenarioInfo.getScenarioInfo(scenId);
            if (info == null || info.getTownIndex() > ServerConfig.townIndex) {
                log.warning("Requested to play invalid scenario", "who", player.who(),
                            "scid", scenId);
                throw new InvocationException(INTERNAL_ERROR);
            }
        }

        // make sure non-admins aren't creating autoplay games
        if (autoplay && !player.tokens.isAdmin()) {
            log.warning("Non-admin requested autoplay game", "who", player.who(), "pl", players,
                        "scen", scenarios[0], "board", board);
            throw new InvocationException(INTERNAL_ERROR);
        }

        playComputer(player, players, scenarios, board, autoplay);
    }

    // from interface PlayerProvider
    public void playBountyGame (PlayerObject caller, String bountyId, String gameId,
                                PlayerService.InvocationListener listener)
        throws InvocationException
    {
        // pass the buck to the office manager
        BangServer.officemgr.playBountyGame(caller, bountyId, gameId, listener);
    }

    // from interface PlayerProvider
    public void getPosterInfo (PlayerObject caller, final Handle handle,
                               final PlayerService.ResultListener listener)
        throws InvocationException
    {
        // check the cache for previously generated posters
        SoftReference<PosterInfo> infoRef = _posterCache.get(handle);
        PosterInfo tmpInfo = null;
        if (infoRef != null) {
            tmpInfo = infoRef.get();
        }
        boolean cached = (tmpInfo != null);

        final PosterInfo info;
        if (cached) {
            info = tmpInfo;
        } else {
            info = new PosterInfo();
            info.handle = handle;
        }

        // if the player is online, populate the poster directly from there
        final PlayerObject posterPlayer = BangServer.locator.lookupPlayer(handle);
        if (posterPlayer != null) {
            Look look = posterPlayer.getLook(Look.Pose.WANTED_POSTER);
            if (look != null) {
                info.avatar = look.getAvatar(posterPlayer);
            }
            BangServer.gangmgr.populatePosterInfo(info, posterPlayer);

            info.rankGroups.clear();
            info.rankGroups.add(new PosterInfo.RankGroup(
                        0, buildRankings(posterPlayer.ratings.get(null))));
            for (int ii = 0; ii < SHOW_WEEKS; ii++) {
                java.sql.Date week = Rating.getWeek(ii);
                Map<String, Rating> map = posterPlayer.ratings.get(week);
                if (map != null && !map.isEmpty()) {
                    info.rankGroups.add(new PosterInfo.RankGroup(
                                week.getTime(), buildRankings(map)));
                }
            }
        }

        // if the poster came from the cache, we're already done
        if (cached) {
            listener.requestProcessed(info);
            return;
        }

        // otherwise, we need to hit some repositories
        BangServer.invoker.postUnit(new PersistingUnit("getPosterInfo", listener) {
            public void invokePersistent() throws PersistenceException {
                // first map handle to player id
                _player = _playrepo.loadByHandle(handle);
                if (_player == null) {
                    return;
                }

                // then fetch the poster record
                PosterRecord poster = _postrepo.loadPoster(_player.playerId);
                if (poster != null) {
                    info.statement = poster.statement;
                    info.badgeIds = new int[] {
                        poster.badge1, poster.badge2, poster.badge3, poster.badge4,
                    };

                } else {
                    info.statement = "";
                    info.badgeIds = new int[] { -1, -1, -1, -1 };
                }

                // for offline players, get look snapshot from repository
                if (posterPlayer == null) {
                    info.avatar = _lookrepo.loadSnapshot(_player.playerId);
                    info.rankGroups.add(new PosterInfo.RankGroup(
                                0, buildRankings(_raterepo.loadRatings(_player.playerId, null))));
                    for (int ii = 0; ii < SHOW_WEEKS; ii++) {
                        java.sql.Date week = Rating.getWeek(ii);
                        Map<String, Rating> map = _raterepo.loadRatings(_player.playerId, week);
                        if (map != null && !map.isEmpty()) {
                            info.rankGroups.add(new PosterInfo.RankGroup(
                                        week.getTime(), buildRankings(map)));
                        }
                    }
                    BangServer.gangmgr.populatePosterInfo(info, _player);
                }
            }

            public void handleSuccess() {
                if (_player == null) {
                    listener.requestFailed(
                        MessageBundle.tcompose(BangCodes.E_NO_SUCH_PLAYER, handle));
                } else {
                    // cache the result and return it
                    _posterCache.put(handle, new SoftReference<PosterInfo>(info));
                    listener.requestProcessed(info);
                }
            }

            public String getFailureMessage() {
                return "Failed to build wanted poster data [handle=" + handle + "]";
            }

            protected PlayerRecord _player;
        });
    }

    // from interface PlayerProvider
    public void updatePosterInfo (final PlayerObject user, int playerId, String statement,
                                  int[] badgeIds, final PlayerService.ConfirmListener cl)
        throws InvocationException
    {
        // create a poster record and populate it
        final PosterRecord poster = new PosterRecord(playerId);
        poster.statement = statement;
        poster.badge1 = badgeIds[0];
        poster.badge2 = badgeIds[1];
        poster.badge3 = badgeIds[2];
        poster.badge4 = badgeIds[3];

        // then store it in the database
        BangServer.invoker.postUnit(new PersistingUnit("updatePosterInfo", cl) {
            public void invokePersistent() throws PersistenceException {
                _postrepo.storePoster(poster);
            }
            public void handleSuccess() {
                cl.requestProcessed();
                clearPosterInfoCache(user.handle);
            }
            public String getFailureMessage() {
                return "Failed to store wanted poster record [poster = " + poster + "]";
            }
        });
    }

    /**
     * Clears the player's poster info from the cache.
     */
    public void clearPosterInfoCache (Handle handle)
    {
        _posterCache.remove(handle);
        if (_peermgr.isRunning()) {
            _peermgr.broadcastStaleCacheData(POSTER_CACHE, handle);
        }
    }

    // from interface PlayerProvider
    public void noteFolk (final PlayerObject user, final int folkId, int note,
                          PlayerService.ConfirmListener cl)
        throws InvocationException
    {
        int ixFoe = Arrays.binarySearch(user.foes, folkId);
        int ixFriend = Arrays.binarySearch(user.friends, folkId);

        final byte opinion;
        final int[] nfriends, nfoes;
        if (note == PlayerService.FOLK_IS_FRIEND && ixFriend < 0) {
            opinion = FolkRecord.FRIEND;
            nfriends = ArrayUtil.insert(user.friends, folkId, -1*(1+ixFriend));
            nfoes = (ixFoe >= 0) ? ArrayUtil.splice(user.foes, ixFoe, 1) : user.foes;

        } else if (note == PlayerService.FOLK_IS_FOE && ixFoe < 0) {
            opinion = FolkRecord.FOE;
            nfriends = (ixFriend >= 0) ? ArrayUtil.splice(user.friends, ixFriend, 1) : user.friends;
            nfoes = ArrayUtil.insert(user.foes, folkId, -1*(1+ixFoe));

        } else if (note == PlayerService.FOLK_NEUTRAL &&
            (ixFoe >= 0 || ixFriend >= 0)) {
            opinion = FolkRecord.NO_OPINION;
            nfriends = (ixFriend >= 0) ? ArrayUtil.splice(user.friends, ixFriend, 1) : user.friends;
            nfoes = (ixFoe >= 0) ? ArrayUtil.splice(user.foes, ixFoe, 1) : user.foes;

        } else {
            cl.requestProcessed(); // NOOP!
            return;
        }

        BangServer.invoker.postUnit(new PersistingUnit("noteFolk", cl) {
            public void invokePersistent() throws PersistenceException {
                _playrepo.registerOpinion(user.playerId, folkId, opinion);
            }
            public void handleSuccess() {
                user.startTransaction();
                try {
                    user.setFriends(nfriends);
                    user.setFoes(nfoes);
                } finally {
                    user.commitTransaction();
                }
                ((PlayerService.ConfirmListener)_listener).requestProcessed();
            }
            public String getFailureMessage() {
                return "Failed to register opinion [who=" + user.who() + ", folk=" + folkId + "]";
            }
        });
    }

    // from interface PlayerProvider
    public void prepSongForDownload (PlayerObject user, final String song,
                                     final PlayerService.ResultListener listener)
        throws InvocationException
    {
        // make sure the player owns this song
        if (!user.ownsSong(song)) {
            throw new InvocationException(ACCESS_DENIED);
        }

        // create a temporary symlink in the data/download directory for use in downloading
        BangServer.invoker.postUnit(new Invoker.Unit("createDownloadSymlink") {
            public boolean invoke () {
                _ident = createDownloadSymlink(song);
                return true;
            }
            public void handleResult () {
                if (_ident == null) {
                    listener.requestFailed(INTERNAL_ERROR);
                } else {
                    listener.requestProcessed(_ident);
                }
            }
            protected String _ident;
        });
    }

    // from interface PlayerProvider
    public void destroyItem (
        final PlayerObject user, final int itemId, final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure the player is holding the item
        final Item item = user.inventory.get(itemId);
        if (item == null) {
            log.warning("User requested to destroy invalid item", "who", user.who(),
                        "itemId", itemId);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // and that it's destroyable
        if (!item.isDestroyable(user)) {
            log.warning("User tried to destroy indestructable item", "who", user.who(),
                        "item", item);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // remove it from their inventory immediately, then from the database
        user.removeFromInventory(item.getKey());
        BangServer.invoker.postUnit(new PersistingUnit("destroyItem", listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _itemrepo.deleteItem(item, "trashed");
            }
            public void handleSuccess () {
                listener.requestProcessed();
            }
            public void handleFailure (Exception error) {
                super.handleFailure(error);
                user.addToInventory(item); // put it back
            }
            public String getFailureMessage () {
                return "Failed to destroy item [who=" + user.who() + ", item=" + item + "]";
            }
        });
    }

    // from interface PlayerProvider
    public void createAccount (final PlayerObject user, final String username, final String password,
                               final String email, final String affiliate, long birthdate,
                               final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        if (_creatingAccounts.contains(user.playerId)) {
            listener.requestFailed(E_IN_PROGRESS);
            return;
        }

        // make sure we're anonymous
        if (!user.tokens.isAnonymous()) {
            log.warning("Non-anonymous user tried to create account", "who", user.who());
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure we're old enough (the client should already verify this)
        final java.sql.Date bdate = new java.sql.Date(birthdate);
        Calendar cal = Calendar.getInstance();
        cal.roll(Calendar.YEAR, -BangCodes.COPPA_YEAR);
        if (bdate.after(cal.getTime())) {
            log.warning("Underage user tried to create account", "who", user.who());
            throw new InvocationException(INTERNAL_ERROR);
        }

        final String machIdent = ((BangCredentials)((BangSession)BangServer.clmgr.getClient(
                        user.username)).getCredentials()).ident;

        // prevent multiple requests from coming in
        _creatingAccounts.add(user.playerId);

        _authInvoker.postUnit(new Invoker.Unit("createAccount") {
            public boolean invoke () {
                try {
                    _errmsg = BangServer.author.createAccount(
                        username, password, email, affiliate, machIdent, bdate);
                    if (_errmsg == null) {
                        BangServer.author.setAccountIsActive(username, true);
                        _playrepo.clearAnonymous(user.playerId, username);
                        BangServer.generalLog("create_account " + user.playerId);
                    }
                } catch (PersistenceException pe) {
                    log.warning("Failed to create account for", "who", user.who(), pe);
                    _errmsg = INTERNAL_ERROR;
                }
                return true;
            }
            public void handleResult () {
                if (_errmsg != null) {
                    listener.requestFailed(_errmsg);
                    _creatingAccounts.remove(user.playerId);
                } else {
                    listener.requestProcessed();
                }
            }
            protected String _errmsg;
        });
    }

    // documentation inherited from PlayerProvider
    public void bootPlayer (PlayerObject user, Handle handle,
                            PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        if (!user.tokens.isSupport()) {
            log.warning("Attempting to boot player from non-support user", "who", user.who());
            throw new InvocationException(ACCESS_DENIED);
        }

        PlayerObject target = BangServer.locator.lookupPlayer(handle);
        if (target == null) {
            log.warning("Unable to find target to boot", "handle", handle);
            throw new InvocationException(INTERNAL_ERROR);
        }

        PresentsSession pclient = BangServer.clmgr.getClient(target.username);
        if (pclient == null) {
            log.warning("Unable to find client to boot", "target", target.who());
            throw new InvocationException(INTERNAL_ERROR);
        }

        pclient.endSession();
        listener.requestProcessed();
    }

    /**
     * Delivers the specified item to its owner if he is online (on any server).  The item is
     * assumed to be in the database already; this method merely requests an update of the
     * distributed object state.
     *
     * @param source a qualified translatable string describing the source of the item
     */
    public void deliverItem (Item item, String source)
    {
        int ownerId = item.getOwnerId();
        PlayerObject user = BangServer.locator.lookupPlayer(ownerId);
        if (user != null) {
            deliverItemLocal(user, item, source);
        } else if (_peermgr.isRunning()) {
            // try our peers
            _peermgr.forwardItem(item, source);
        }
    }

    /**
     * Delivers an item to a player on this server.
     */
    public void deliverItemLocal (PlayerObject user, Item item, String source)
    {
        user.addToInventory(item);
        SpeakUtil.sendInfo(user, BangCodes.BANG_MSGS,
            MessageBundle.compose("m.received_item", item.getName(), source));
    }

    /**
     * Sends a pardner invite to the specified player if he is online (on any server).
     */
    public void sendPardnerInvite (Handle invitee, Handle inviter, String message)
    {
        PlayerObject user = BangServer.locator.lookupPlayer(invitee);
        if (user != null) {
            sendPardnerInviteLocal(user, inviter, message, new Date());
        } else if (_peermgr.isRunning()) {
            _peermgr.forwardPardnerInvite(invitee, inviter, message);
        }
    }

    /**
     * Sends a pardner invite to the specified player from the named inviter (on this server only).
     */
    public void sendPardnerInviteLocal (
        final PlayerObject user, final Handle inviter, String message, final Date lastSession)
    {
        user.addToNotifications(
            new PardnerInvite(inviter, message, new PardnerInvite.ResponseHandler() {
            public void handleResponse (int resp, InvocationService.ConfirmListener listener) {
                handleInviteResponse(
                    user, inviter, lastSession, (resp == PardnerInvite.ACCEPT), listener);
            }
        }));
    }

    /**
     * Adds a pardner entry to the specified player if he is online (on any server).
     */
    public void respondToPardnerInvite (
        Handle inviter, Handle invitee, boolean accept, boolean full)
    {
        PlayerObject user = BangServer.locator.lookupPlayer(inviter);
        if (user != null) {
            respondToPardnerInviteLocal(user, invitee, accept, full);
        } else if (_peermgr.isRunning()) {
            _peermgr.forwardPardnerInviteResponse(inviter, invitee, accept, full);
        }
    }

    /**
     * Adds a pardner entry to the specified player (on this server only).
     */
    public void respondToPardnerInviteLocal (
        PlayerObject inviter, Handle invitee, boolean accept, boolean full)
    {
        if (accept) {
            inviter.addOrUpdatePardner(getPardnerEntry(invitee, new Date()));
            if (full) {
                clearPardnerInvites(inviter);
            }
        }
        String msg = accept ? "m.pardner_accepted" : "m.pardner_rejected";
        SpeakUtil.sendInfo(inviter, BANG_MSGS, MessageBundle.tcompose(msg, invitee));
    }

    /**
     * Removes a pardner entry from the specified player if he is online (on any server).
     */
    public void removePardner (Handle removee, Handle remover)
    {
        PlayerObject user = BangServer.locator.lookupPlayer(removee);
        if (user != null) {
            removePardnerLocal(user, remover);
        } else if (_peermgr.isRunning()) {
            _peermgr.forwardPardnerRemoval(removee, remover);
        }
    }

    /**
     * Removes a pardner entry from the specified player (on this server only).
     */
    public void removePardnerLocal (PlayerObject removee, Handle remover)
    {
        removee.removeFromPardners(remover);
        String msg = MessageBundle.tcompose("m.pardner_ended", remover);
        SpeakUtil.sendInfo(removee, BANG_MSGS, msg);
    }

    /**
     * Sends a warning message to the specified player.
     */
    public void sendWarningMessage (
        final PlayerObject user, boolean tempBan, String message)
    {
        user.addToNotifications(
            new Warning(tempBan ? Warning.TEMP_BAN : Warning.WARNING, message,
                new Warning.ResponseHandler() {
            public void handleResponse (int resp, InvocationService.ConfirmListener listener) {
                handleWarningResponse(user, listener);
            }
        }));
    }

    /**
     * Checks for any players that should be purged from the system.
     */
    public void purgeExpiredPlayers ()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -ANONYMOUS_EXPIRE_DAYS);
        final java.sql.Date anon = new java.sql.Date(cal.getTimeInMillis());
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_MONTH, -USER_EXPIRE_DAYS);
        final java.sql.Date user = new java.sql.Date(cal.getTimeInMillis());

        BangServer.invoker.postUnit(new Invoker.Unit("purgeExpiredPlayers") {
            public boolean invoke () {
                try {
                    _players = _playrepo.loadExpiredPlayers(anon, user);
                } catch (PersistenceException pe) {
                    log.warning("Failed to load player records to be expired", "pe", pe);
                    return false;
                }
                return true;
            }
            public void handleResult () {
                purgePlayers(_players.iterator());
            }
            public long getLongThreshold () {
                return 4000L; // this seems to take about ~3600ms every time...
            }
            protected List<PlayerRecord> _players;
        });
    }

    /**
     * Checks if a player has not already registered a late night game, and if not, increments
     * their stat.
     */
    public void setLateNight (PlayerObject user)
    {
        if (_lateNighters.containsKey(user.playerId)) {
            return;
        }
        user.stats.incrementStat(StatType.LATE_NIGHTS, 1);

        Calendar cal = Calendar.getInstance();
        _lateNighters.put(user.playerId, cal.get(Calendar.HOUR_OF_DAY));
    }

    /**
     * Clears our player Ids for late night games that occured 20 hours ago.
     */
    public void clearLateNighters ()
    {
        Calendar cal = Calendar.getInstance();
        int hour = (cal.get(Calendar.HOUR_OF_DAY) + 20) % 24;
        for (Interator inter = _lateNighters.values(); inter.hasNext(); ) {
            if (inter.nextInt() == hour) {
                inter.remove();
            }
        }
    }

    /**
     * Helper function for playing games. Assumes all parameters have been checked for validity.
     */
    protected void playComputer (
            PlayerObject player, int players, String[] scenarios, String board, boolean autoplay)
        throws InvocationException
    {
        // create a game configuration from that
        BangConfig config = new BangConfig();
        config.rated = false;
        config.players = new Name[players];
        config.ais = new BangAI[players];
        config.init(players, Match.TEAM_SIZES[players-2]);
        for (String scenId : scenarios) {
            config.addRound(scenId, board, null);
        }
        playComputer(player, config, autoplay);
    }

    /**
     * Helper function for playing games. Assumes all parameters have been checked for validity.
     */
    protected BangObject playComputer (PlayerObject player, BangConfig config, boolean autoplay)
        throws InvocationException
    {
        HashSet<String> names = new HashSet<String>();
        names.add(player.getVisibleName().toString());

        // configure our AIs and the player names array
        if (!autoplay) {
            config.players[0] = player.getVisibleName();
        }
        for (int ii = autoplay ? 0 : 1; ii < config.players.length; ii++) {
            int ailevel = (config.duration == BangConfig.Duration.PRACTICE ? 20 : 50);
            BangAI ai = BangAI.createAI(1, ailevel, names);
            config.players[ii] = ai.handle;
            config.ais[ii] = ai;
        }

        try {
            BangManager mgr = (BangManager)BangServer.plreg.createPlace(config);
            BangObject bangobj = (BangObject)mgr.getPlaceObject();

            // if this is an autoplay game, fake a game ready notification
            if (autoplay) {
                ParlorSender.gameIsReady(player, bangobj.getOid());
            }
            return bangobj;

        } catch (InstantiationException ie) {
            log.warning("Error instantiating game", "for", player.who(), "config", config, ie);
            throw new InvocationException(INTERNAL_ERROR);
        }
    }

    /**
     * Converts a players {@link Rating}s records into ranking levels for inclusion in their poster
     * info.
     */
    protected StreamableHashMap<String, Integer> buildRankings (Map<String, Rating> ratings)
    {
        StreamableHashMap<String, Integer> map = new StreamableHashMap<String,Integer>();
        for (Rating rating : ratings.values()) {
            map.put(rating.scenario, BangServer.ratingmgr.getRank(rating.scenario, rating.rating));
        }
        return map;
    }

    /**
     * Creates (if the pardner is offline) or retrieves (if the pardner is online) the up-to-date
     * {@link PardnerEntry} for the named pardner.  If the pardner is online and no {@link
     * PardnerEntryUpdater} exists for the pardner, one will be created, mapped, and used to keep
     * the {@link PardnerEntry} up-to-date.
     */
    protected PardnerEntry getPardnerEntry (Handle handle, Date lastSession)
    {
        // see if we've already got an updater for this player
        PardnerEntryUpdater updater = _updaters.get(handle);
        if (updater != null) {
            return updater.entry;
        }

        // check whether the player is online on this server
        PlayerObject player = BangServer.locator.lookupPlayer(handle);
        if (player != null) {
            _updaters.put(handle, updater = new PardnerEntryUpdater(player));
            return updater.entry;
        }

        // check whether the player is online on another server
        Tuple<BangClientInfo,Integer> remote = null;
        if (_peermgr.isRunning()) {
            remote = _peermgr.locateRemotePlayer(handle);
        }
        if (remote != null) {
            PardnerEntry entry = new PardnerEntry(handle);
            entry.setOnline(remote.right);
            entry.avatar = remote.left.avatar;
            return entry;
        }

        // otherwise they're offline
        return new PardnerEntry(handle, lastSession);
    }

    /**
     * Called by the {@link PardnerEntryUpdater} when its player logs off.
     */
    protected void clearPardnerEntryUpdater (Handle handle)
    {
        _updaters.remove(handle);
    }

    /**
     * Processes the response to a pardner invitation.
     */
    protected void handleInviteResponse (
        final PlayerObject user, final Handle inviter, final Date lastSession,
        final boolean accept, final InvocationService.ConfirmListener listener)
    {
        // update the database
        BangServer.invoker.postUnit(new PersistingUnit("handleInviteResponse", listener) {
            public void invokePersistent ()
                throws PersistenceException {
                if (accept) {
                    _error = _pardrepo.updatePardners(
                        user.playerId, inviter, _full = new boolean[2]);
                } else {
                    _pardrepo.removePardners(user.playerId, inviter);
                }
            }
            public void handleSuccess () {
                if (_error != null) {
                    listener.requestFailed(_error);
                } else {
                    handleInviteSuccess(user, inviter, lastSession, accept, _full, listener);
                }
            }
            public String getFailureMessage () {
                return "Failed to respond to pardner invite [who=" + user.who() +
                    ", inviter=" + inviter + ", accept=" + accept + "]";
            }
            protected boolean[] _full;
            protected String _error;
        });
    }

    /**
     * Handles the omgr portion of the invite processing, once the persistent part has successfully
     * completed.
     */
    protected void handleInviteSuccess (
        PlayerObject user, Handle inviter, Date lastSession, boolean accept,
        boolean[] full, InvocationService.ConfirmListener listener)
    {
        // if the inviter is online on any server, update and send a notification
        respondToPardnerInvite(inviter, user.handle, accept, full != null && full[1]);

        // update the invitee
        if (user.isActive()) {
            if (accept) {
                user.addOrUpdatePardner(getPardnerEntry(inviter, lastSession));
                if (full[0]) {
                    clearPardnerInvites(user);
                }
            }
            listener.requestProcessed();
        }
    }

    /**
     * Clears out all of the player's pardner invites.  Ideally, this would also clear out any
     * pardner invites that the player had sent, but that would be much more difficult.
     */
    protected void clearPardnerInvites (PlayerObject player)
    {
        List<Comparable<?>> keys = Lists.newArrayList();
        for (Notification notification : player.notifications) {
            if (notification instanceof PardnerInvite) {
                keys.add(notification.getKey());
            }
        }
        try {
            player.startTransaction();
            for (Comparable<?> key : keys) {
                player.removeFromNotifications(key);
            }
        } finally {
            player.commitTransaction();
        }
    }

    /**
     * Processes the response to a warning message.
     */
    protected void handleWarningResponse (
        final PlayerObject user, final InvocationService.ConfirmListener listener)
    {
        // update the database
        BangServer.invoker.postUnit(new PersistingUnit("handleWarningResponse", listener) {
            public void invokePersistent ()
                throws PersistenceException {
                _playrepo.clearWarning(user.playerId);
            }
            public void handleSuccess () {
                listener.requestProcessed();
            }
            public String getFailureMessage () {
                return "Failed to clear warning [who=" + user.who() + "]";
            }
        });
    }

    /**
     * Creates a (temporary) symlink allowing the download of the specified song.
     *
     * @return the random identifier assigned to the song for this download or null if we failed to
     * create the symlink.
     */
    protected String createDownloadSymlink (String song)
    {
        File source = new File(ServerConfig.serverRoot, "data" + File.separator + "soundtrack" +
                               File.separator + song + ".mp3");
        if (!source.exists()) {
            log.warning("Requested to create symlink for missing source", "song", song);
            return null;
        }

        // generate a random name for the to be downloaded media
        long rando = (long)(Math.random() * Long.MAX_VALUE);
        String ident = StringUtil.md5hex("" + (rando ^ System.currentTimeMillis()));
        File dest = new File(ServerConfig.serverRoot, "data" + File.separator + "downloads" +
                             File.separator + ident);

        // Java's file abstraction does not support symlinks, so we have to exec a process
        String stderr = "";
        try {
            Process proc = new ProcessBuilder(
                "ln", "-s", source.toString(), dest.toString()).start();
            stderr = IOUtils.toString(proc.getErrorStream());
            if (proc.waitFor() != 0) {
                log.warning("Failed to create download symlink", "song", song, "ident", ident,
                            "stderr", stderr);
                return null;
            }

            // create a timestamp file
            File stamp = new File(dest.getPath() + ".stamp");
            if (!stamp.createNewFile()) {
                log.warning("Failed to create timestamp file", "stamp", stamp);
            }

        } catch (Exception e) {
            log.warning("Failure running ln command.", e);
        }

        return ident;
    }

    /**
     * Called periodically to purge players that have not logged in for
     * a while.
     */
    protected void purgePlayers (final Iterator<PlayerRecord> players)
    {
        if (!players.hasNext()) {
            return;
        }

        InvocationService.ConfirmListener rl = new InvocationService.ConfirmListener() {
            public void requestProcessed () {
                doNext();
            }
            public void requestFailed (String cause) {
                log.warning(cause);
                doNext();
            }
            protected void doNext () {
                if (players.hasNext()) {
                    purgePlayer(players.next(), this);
                }
            }
        };

        purgePlayer(players.next(), rl);
    }

    /**
     * Purge a player from Bang.
     */
    protected void purgePlayer (
            final PlayerRecord user, final InvocationService.ConfirmListener listener)
    {
        BangServer.invoker.postUnit(new PersistingUnit("purgePlayer", listener) {
            public void invokePersistent () throws PersistenceException {
                _pardrepo.removeAllPardners(user.playerId);
                _playrepo.clearOpinions(user.playerId);
                _postrepo.deletePoster(user.playerId);
                _lookrepo.deleteAllLooks(user.playerId);
                _statrepo.deleteStats(user.playerId);
                _ratingrepo.deleteRatings(user.playerId);
                _itemrepo.deleteItems(user.playerId, "purging player");

                // find out if they're in a gang, and remove them from it, or remove any
                // pending gang invitations
                // TODO: make this play nice with gangs not currently loaded
                GangMemberRecord gmr = _gangrepo.loadMember(user.playerId);
                if (gmr == null) {
                    _gangrepo.deletePendingInvites(user.playerId);
                } else {
                    try {
                        BangServer.gangmgr.requireGangPeerProvider(gmr.gangId).removeFromGang(
                                null, null, new Handle(user.handle),
                                new InvocationService.ConfirmListener() {
                                    public void requestProcessed () { }
                                    public void requestFailed (String cause) { }
                                });
                    } catch (InvocationException ie) {
                        log.warning("Failure removing purged player from gang! Proceeding with " +
                                    "purge anyway", "ie", ie);
                    }
                }
                _playrepo.deletePlayer(user);
            }
            public void handleSuccess () {
                listener.requestProcessed();
            }
            public String getFailureMessage () {
                return "Failed to purge player! [user=" + user + "].";
            }
        });
    }

    /**
     * Called periodically (on the invoker thread) to purge download symlinks that are more than 5
     * minutes old.
     */
    protected void purgeDownloadLinks ()
    {
        File ddir = new File(ServerConfig.serverRoot, "data" + File.separator + "downloads");
        long now = System.currentTimeMillis();
        for (File file : ddir.listFiles()) {
            if (!file.getName().endsWith(".stamp")) {
                continue;
            }
            if (now - file.lastModified() > DOWNLOAD_PURGE_EXPIRE) {
                if (!file.delete()) {
                    log.warning("Failed to delete stamp file", "file", file.getPath());
                }
                file = new File(file.getPath().substring(0, file.getPath().length()-6));
                if (!file.delete()) {
                    log.warning("Failed to delete symlink file", "file", file.getPath());
                }
            }
        }
    }

    /**
     * Helper function that gives a player a gold pass.
     */
    protected void giveGoldPass (final PlayerObject user, final String townId, boolean addBonuses)
    {
        final List<Item> items = Lists.newArrayList();
        items.add(new GoldPass(user.playerId, townId));

        // if specified, we give a bunch of bonus items to the pass recipient
        if (addBonuses) {
            // two free bigshots
            items.add(new BigShotItem(user.playerId, "frontier_town/codger"));
            items.add(new BigShotItem(user.playerId, "indian_post/tricksterraven"));
            // two free copper stars
            items.add(new Star(user.playerId, BangUtil.getTownIndex(BangCodes.FRONTIER_TOWN),
                               Star.Difficulty.MEDIUM));
            items.add(new Star(user.playerId, BangUtil.getTownIndex(BangCodes.INDIAN_POST),
                               Star.Difficulty.MEDIUM));
        }

        // stick the new item in the database and in their inventory
        BangServer.invoker.postUnit(new RepositoryUnit("giveGoldPass") {
            public void invokePersist () throws Exception {
                // grand them their gold pass and bonus items
                for (Item item : items) {
                    _itemrepo.insertItem(item);
                }
                // grant them their bonus scrip
                _playrepo.grantScrip(user.playerId, GOLD_PASS_SCRIP_BONUS);
            }
            public void handleSuccess () {
                for (Item item : items) {
                    user.addToInventory(item);
                }
                user.setScrip(user.scrip + GOLD_PASS_SCRIP_BONUS);
                BangServer.itemLog("gold_pass " + user.playerId + " t:" + townId);
            }
            public void handleFailure (Exception err) {
                log.warning("Failed to grant gold pass and bonuses", "who", user.who(),
                            "items", items, err);
            }
        });

        // bonuses also include a 52 pack of cards which must be done more complexly
        if (addBonuses) {
            InvocationService.ConfirmListener cl = new InvocationService.ConfirmListener() {
                public void requestProcessed () {
                    // noop!
                }
                public void requestFailed (String cause) {
                    log.warning("Failed to grant gold pass cards", "who", user.who(),
                                "cause", cause);
                }
            };
            try {
                CardPackGood pack = new CardPackGood(52, BangCodes.FRONTIER_TOWN, 0, 0);
                Provider provider = _goods.getProvider(user, pack, null);
                provider.setListener(cl);
                _invoker.post(provider);
            } catch (InvocationException ie) {
                cl.requestFailed(ie.getMessage());
            }
        }
    }

    public static SpeakObject.ListenerOp _messageCounter = new SpeakObject.ListenerOp() {
        @Override public void apply (SpeakObject obj, int bodyOid) {
            DObject dobj = BangServer.omgr.getObject(bodyOid);
            if (dobj != null && dobj instanceof PlayerObject) {
                ((PlayerObject)dobj).stats.incrementStat(StatType.CHAT_RECEIVED, 1);
            }
        }
        @Override public void apply (SpeakObject obj, Name username) {
            PlayerObject user = (PlayerObject)BangServer.locator.lookupBody(username);
            if (user != null) {
                user.stats.incrementStat(StatType.CHAT_RECEIVED, 1);
            }
        }
    };

    /** Maps the names of users to updaters responsible for keeping their {@link PardnerEntry}s
     * up-to-date. */
    protected Map<Handle, PardnerEntryUpdater> _updaters = Maps.newHashMap();

    /** Keeps our {@link PardnerEntry}s up to date for remote players. */
    protected RemotePlayerWatcher<PardnerEntry> _pardwatcher;

    /** A light-weight cache of soft {@link PosterInfo} references. */
    protected Map<Handle, SoftReference<PosterInfo>> _posterCache = Maps.newHashMap();

    /** Keeps a record when players have played late night games. */
    protected IntIntMap _lateNighters = new IntIntMap();

    /** Keeps track of pending create account requests. */
    protected ArrayIntSet _creatingAccounts = new ArrayIntSet();

    // dependencies
    @Inject protected @AuthInvoker Invoker _authInvoker;
    @Inject protected BangInvoker _invoker;
    @Inject protected ResourceManager _rsrcmgr;
    @Inject protected GoodsCatalog _goods;
    @Inject protected ChatHistory _history;
    @Inject protected BangPeerManager _peermgr;
    @Inject protected PardnerRepository _pardrepo;
    @Inject protected PosterRepository _postrepo;
    @Inject protected LookRepository _lookrepo;
    @Inject protected RatingRepository _raterepo;
    @Inject protected PlayerRepository _playrepo;
    @Inject protected GangRepository _gangrepo;
    @Inject protected ItemRepository _itemrepo;
    @Inject protected BangStatRepository _statrepo;
    @Inject protected RatingRepository _ratingrepo;

    /** The name of our poster cache. */
    protected static final String POSTER_CACHE = "posterCache";

    /** The time after which song download symlinks are purged. */
    protected static final long DOWNLOAD_PURGE_EXPIRE = 5 * 60 * 1000L;

    /** The frequency with which download symlinks are purged. */
    protected static final long DOWNLOAD_PURGE_INTERVAL = 60 * 1000L;

    /** The frequency with which we search for players to purge. */
    protected static final long PLAYER_PURGE_INTERVAL = 60 * 1000L;

    /** The freqeuncy with which we clear late night players. */
    protected static final long LATE_NIGHT_INTERVAL = 60 * 1000L;

    /** The number of days after which to expire anonymous players. */
    protected static final int ANONYMOUS_EXPIRE_DAYS = 30;

    /** The number of days after which to expire regular players. */
    protected static final int USER_EXPIRE_DAYS = 180;

    /** The number of back weeks to show on the wanted poster. */
    protected static final int SHOW_WEEKS = 4;

    /** Bonus scrip granted to gold (and onetime) pass buyers. */
    protected static final int GOLD_PASS_SCRIP_BONUS = 5000;
}
