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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.TransitionRepository;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ListUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import org.apache.commons.io.IOUtils;

import com.threerings.io.Streamable;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.server.PeerManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.parlor.server.ParlorSender;

import com.threerings.underwire.server.persist.EventRecord;
import com.threerings.underwire.server.persist.UnderwireRepository;
import com.threerings.underwire.web.data.Event;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.StreamableHashMap;

import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.server.persist.LookRepository;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.ranch.data.RanchCodes;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.saloon.server.Match;

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
import com.threerings.bang.data.Article;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BangClientInfo;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangCredentials;
import com.threerings.bang.data.BangTokenRing;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.Notification;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PardnerInvite;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.PosterInfo;
import com.threerings.bang.data.Rating;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.server.persist.FolkRecord;
import com.threerings.bang.server.persist.PardnerRecord;
import com.threerings.bang.server.persist.PardnerRepository;
import com.threerings.bang.server.persist.PlayerRecord;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.server.persist.PosterRecord;
import com.threerings.bang.server.persist.PosterRepository;
import com.threerings.bang.server.persist.RatingRepository.RankLevels;
import com.threerings.bang.server.persist.RatingRepository;
import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Handles general player business, implements {@link PlayerProvider}.
 */
public class PlayerManager
    implements PlayerProvider, BangCodes
{
    /**
     * Initializes the player manager, and registers its invocation service.
     */
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        // we're the repository kings!
        _pardrepo = new PardnerRepository(conprov);
        _postrepo = new PosterRepository(conprov);
        _playrepo = new PlayerRepository(conprov);
        _raterepo = new RatingRepository(conprov);
        _lookrepo = new LookRepository(conprov);
        _underepo = new UnderwireRepository(conprov);

        // register ourselves as the provider of the (bootstrap) PlayerService
        BangServer.invmgr.registerDispatcher(new PlayerDispatcher(this), GLOBAL_GROUP);

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
        if (BangServer.peermgr != null) {
            BangServer.peermgr.addPlayerObserver(_pardwatcher);
            BangServer.peermgr.addStaleCacheObserver(POSTER_CACHE,
                new PeerManager.StaleCacheObserver() {
                    public void changedCacheData (Streamable data) {
                        _posterCache.remove((Handle)data);
                    }
                });
        }

        // register our download symlink cleaning interval; note that because this simply posts an
        // invoker unit, it is not routed through the dobjmgr
        new Interval() {
            public void expired () {
                BangServer.invoker.postUnit(new Invoker.Unit("purgeDownloadLinks") {
                    public boolean invoke () {
                        purgeDownloadLinks();
                        return false;
                    }
                });
            }
        }.schedule(DOWNLOAD_PURGE_INTERVAL, true);
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
    public void initPardners (final PlayerObject player, ArrayList<PardnerRecord> records)
    {
        // TEMP: sanity check since I've seen duplicates
        HashSet<Handle> temp = new HashSet<Handle>();
        Iterator<PardnerRecord> iter = records.iterator();
        while (iter.hasNext()) {
            PardnerRecord record = iter.next();
            if (temp.contains(record.handle)) {
                log.warning("Player has duplicate pardner record [pid=" + player.playerId +
                            ", record=" + record + "].");
                iter.remove();
            } else {
                temp.add(record.handle);
            }
        }
        // END TEMP

        // collect active players, send invitations
        ArrayList<PardnerEntry> pardners = new ArrayList<PardnerEntry>();
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
            public String toString () {
                return player.who();
            }
        });
    }

    /**
     * Called from {@link BangClientResolver#finishResolution} to redeem any rewards for which this
     * player is eligible.
     */
    public void redeemRewards (PlayerObject player, ArrayList<String> rewards)
    {
        for (String reward : rewards) {
            try {
                String[] data = reward.split(":");
                if (data[1].equalsIgnoreCase("coins")) {
                    int coins = Integer.parseInt(data[2]);
                    log.info("Granting coin reward [account=" + player.username +
                             ", coins=" + coins + "].");
                    BangServer.coinmgr.grantRewardCoins(player, coins);
                }

            } catch (Exception e) {
                // sorry kid, not much we can do for you
                log.log(Level.WARNING, "Failed to award reward to player [who=" + player.who() +
                        ", reward=" + reward + "].", e);
            }
        }
    }

    // documentation inherited from interface PlayerProvider
    public void pickFirstBigShot (ClientObject caller, String type, Name name,
                                  final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject)caller;

        // sanity check: make sure they don't already have a big shot
        if (user.hasBigShot()) {
            log.warning("Player requested free big shot but already has one " +
                        "[who=" + user.who() + ", inventory=" + user.inventory +
                        ", type=" + type + "].");
            throw new InvocationException(RanchCodes.INTERNAL_ERROR);
        }

        // sanity check: make sure the big shot is valid
        UnitConfig config = UnitConfig.getConfig(type, false);
        if (config == null ||
            ListUtil.indexOf(RanchCodes.STARTER_BIGSHOTS, config.type) == -1) {
            log.warning("Player requested invalid free big shot " +
                        "[who=" + user.who() + ", type=" + type + "].");
            throw new InvocationException(RanchCodes.INTERNAL_ERROR);
        }

        // create the BigShot item and stuff it on into their inventory
        final BigShotItem bsitem = new BigShotItem(user.playerId, config.type);
        bsitem.setGivenName(name);

        // stick the new item in the database and in their inventory
        BangServer.invoker.postUnit(new PersistingUnit("pickFirstBigShot", listener) {
            public void invokePersistent () throws PersistenceException {
                BangServer.itemrepo.insertItem(bsitem);
            }
            public void handleSuccess () {
                user.addToInventory(bsitem);
                listener.requestProcessed();
            }
            public String getFailureMessage () {
                return "Failed to add first big shot to repository " +
                    "[who=" + user.who() + ", item=" + bsitem + "]";
            }
        });
    }

    // documentation inherited from interface PlayerProvider
    public void invitePardner (ClientObject caller, final Handle handle,
        final String message, final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it's not the player himself, that it's not already
        // a pardner, and that the player is under the limit
        final PlayerObject inviter = (PlayerObject)caller;
        if (inviter.handle.equals(handle)) {
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
    public void respondToNotification (
        ClientObject caller, final Comparable key, int resp,
        final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure the notification exists
        final PlayerObject player = (PlayerObject)caller;
        Notification notification = player.notifications.get(key);
        if (notification == null) {
            log.warning("Missing notification for response [who=" +
                player.who() + ", key=" + key + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure the response is valid
        if (resp >= notification.getResponses().length) {
            log.warning("Received invalid response for notification [who=" + player.who() +
                ", notification=" + notification + ", response=" + resp + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // transfer control to the handler, removing the notification on success
        notification.handler.handleResponse(resp, new PlayerService.ConfirmListener() {
            public void requestProcessed () {
                player.removeFromNotifications(key);
                listener.requestProcessed();
            }
            public void requestFailed (String cause) {
                listener.requestFailed(cause);
            }
        });
    }

    // documentation inherited from interface PlayerProvider
    public void removePardner (ClientObject caller, final Handle handle,
                               final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure the pardner entry is present
        final PlayerObject player = (PlayerObject)caller;
        PardnerEntry entry = (PardnerEntry)player.pardners.get(handle);
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
                PlayerObject pardobj = BangServer.lookupPlayer(handle);
                if (pardobj != null) {
                    pardobj.removeFromPardners(player.handle);
                    String msg = MessageBundle.tcompose("m.pardner_ended", player.handle);
                    SpeakProvider.sendInfo(pardobj, BANG_MSGS, msg);
                }
                listener.requestProcessed();
            }
            public String getFailureMessage () {
                return "Failed to remove pardner [who=" + player.who() +
                    ", pardner=" + handle + "]";
            }
        });
    }

    // documentation inherited from interface PlayerProvider
    public void playTutorial (ClientObject caller, String tutId,
                              PlayerService.InvocationListener il)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject)caller;

        // if we're not allowing new games, fail immediately
        if (!RuntimeConfig.server.allowNewGames) {
            throw new InvocationException(SaloonCodes.NEW_GAMES_DISABLED);
        }

        // make sure the tutorial is valid for this town
        int tutIdx = ListUtil.indexOf(TutorialCodes.TUTORIALS[ServerConfig.townIndex], tutId);
        if (!player.tokens.isAdmin() && // allow admin to play test tutorials
            tutIdx == -1) {
            log.warning("Player req'd invalid tutorial [who=" + player.who() +
                        ", town=" + player.townId + ", tutid=" + tutId + "].");
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
            config.addRound(scenId, null, null);
            config.duration = BangConfig.Duration.PRACTICE;

        } else {
            // otherwise load up the tutorial configuration and use that to
            // configure the tutorial game
            TutorialConfig tconfig = TutorialUtil.loadTutorial(BangServer.rsrcmgr, tutId);
            config.addRound(tconfig.ident, tconfig.board, null);
            config.type = BangConfig.Type.TUTORIAL;
        }

        playComputer(player, config, false);
    }

    // documentation inherited from interface PlayerProvider
    public void playPractice (ClientObject caller, String unit, PlayerService.InvocationListener il)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject)caller;

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
    public void playComputer (ClientObject caller, int players, String[] scenarios, String board,
                              boolean autoplay, PlayerService.InvocationListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject)caller;

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
                log.warning("Requested to play invalid scenario " +
                            "[who=" + player.who() + ", scid=" + scenId + "].");
                throw new InvocationException(INTERNAL_ERROR);
            }
        }

        // make sure non-admins aren't creating autoplay games
        if (autoplay && !player.tokens.isAdmin()) {
            log.warning("Non-admin requested autoplay game " +
                        "[who=" + player.who() + ", pl=" + players +
                        ", scen=" + scenarios[0] + ", board=" + board + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        playComputer(player, players, scenarios, board, autoplay);
    }

    // from interface PlayerProvider
    public void playBountyGame (ClientObject caller, String bountyId, String gameId,
                                PlayerService.InvocationListener listener)
        throws InvocationException
    {
        // pass the buck to the office manager
        BangServer.officemgr.playBountyGame(caller, bountyId, gameId, listener);
    }

    // from interface PlayerProvider
    public void getPosterInfo (ClientObject caller, final Handle handle,
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
        final PlayerObject posterPlayer = BangServer.lookupPlayer(handle);
        if (posterPlayer != null) {
            Look look = posterPlayer.getLook(Look.Pose.WANTED_POSTER);
            if (look != null) {
                info.avatar = look.getAvatar(posterPlayer);
            }
            BangServer.gangmgr.populatePosterInfo(info, posterPlayer);
            info.rankings = buildRankings(posterPlayer.ratings);
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
                    info.rankings = buildRankings(_raterepo.loadRatings(_player.playerId));
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
    public void updatePosterInfo (ClientObject caller, int playerId, String statement,
                                  int[] badgeIds, final PlayerService.ConfirmListener cl)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject)caller;

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
                _posterCache.remove(user.handle);
            }
            public void handleSuccess() {
                cl.requestProcessed();
                if (BangServer.peermgr != null) {
                    BangServer.peermgr.broadcastStaleCacheData(POSTER_CACHE, user.handle);
                }
            }
            public String getFailureMessage() {
                return "Failed to store wanted poster record [poster = " + poster + "]";
            }
        });
    }

    // from interface PlayerProvider
    public void noteFolk (ClientObject caller, final int folkId, int note,
                          PlayerService.ConfirmListener cl)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject) caller;
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
    public void registerComplaint (ClientObject caller, final Handle target, String reason,
                                   PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject)caller;

        // populate the event with what we can
        final EventRecord event = new EventRecord();
        event.source = user.username.toString();
        event.status = Event.OPEN;
        event.subject = reason;

        // format and provide the complainer's chat history
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss:SSS");
        StringBuilder chatHistory = new StringBuilder();
        for (ChatMessage msg : SpeakProvider.getChatHistory(user.handle)) {
            UserMessage umsg = (UserMessage)msg;
            chatHistory.append(df.format(new Date(umsg.timestamp))).append(' ');
            chatHistory.append(StringUtil.pad(ChatCodes.XLATE_MODES[umsg.mode], 10)).append(' ');
            chatHistory.append(umsg.speaker).append(": ").append(umsg.message).append('\n');
        }
        event.chatHistory = chatHistory.toString();

        // if the target is online, get their username from their player object
        PlayerObject tuser = BangServer.lookupPlayer(target);
        if (tuser != null) {
            event.target = tuser.username.toString();
        }

        // now finish the job on the invoker thread
        BangServer.invoker.postUnit(new PersistingUnit("registerComplaint", listener) {
            public void invokePersistent() throws PersistenceException {
                // if the target is unset, look that up
                if (event.target == null) {
                    PlayerRecord tplayer = _playrepo.loadByHandle(target);
                    if (tplayer == null) {
                        log.warning("Unable to locate target of complaint [event=" + event +
                                    ", target=" + target + "].");
                    } else {
                        event.target = tplayer.accountName;
                    }
                }
                // insert the event into the support repository
                _underepo.insertEvent(event);
            }
            public void handleSuccess() {
                ((PlayerService.ConfirmListener)_listener).requestProcessed();
            }
            public String getFailureMessage() {
                return "Failed to record complaint [event=" + event + "].";
            }
        });
    }

    // from interface PlayerProvider
    public void prepSongForDownload (ClientObject caller, final String song,
                                     final PlayerService.ResultListener listener)
        throws InvocationException
    {
        // make sure the player owns this song
        PlayerObject user = (PlayerObject)caller;
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
        ClientObject caller, final int itemId, final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure the player is holding the item
        final PlayerObject user = (PlayerObject)caller;
        final Item item = user.inventory.get(itemId);
        if (item == null) {
            log.warning("User requested to destroy invalid item [who=" + user.who() + ", itemId=" +
                itemId + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // and that it's destroyable
        if (!item.isDestroyable(user)) {
            log.warning("User tried to destroy indestructable item [who=" + user.who() +
                ", item=" + item + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // remove it from their inventory immediately, then from the database
        user.removeFromInventory(item.getKey());
        BangServer.invoker.postUnit(new PersistingUnit("destroyItem", listener) {
            public void invokePersistent ()
                throws PersistenceException {
                BangServer.itemrepo.deleteItem(item, "trashed");
            }
            public void handleSuccess () {
                listener.requestProcessed();
            }
            public void handleFailure (PersistenceException error) {
                super.handleFailure(error);
                user.addToInventory(item); // put it back
            }
            public String getFailureMessage () {
                return "Failed to destroy item [who=" + user.who() + ", item=" + item + "]";
            }
        });
    }

    // from interface PlayerProvider
    public void createAccount (
            ClientObject caller, final String username, final String password, final String email,
            final String affiliate, long birthdate, final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject)caller;

        // make sure we're anonymous
        if (!user.tokens.isAnonymous()) {
            log.warning("Non-anonymous user tried to create account [who=" + user.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure we're old enough (the client should already verify this)
        final java.sql.Date bdate = new java.sql.Date(birthdate);
        Calendar cal = Calendar.getInstance();
        cal.roll(Calendar.YEAR, -BangCodes.COPPA_YEAR);
        if (bdate.after(cal.getTime())) {
            log.warning("Underage user tried to create account [who=" + user.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        final String machIdent = ((BangCredentials)((BangClient)BangServer.clmgr.getClient(
                        user.username)).getCredentials()).ident;

        // clear their anonymous status immediately
        user.tokens.setToken(BangTokenRing.ANONYMOUS, false);
        user.setTokens(user.tokens);

        BangServer.authInvoker.postUnit(new Invoker.Unit("createAccount") {
            public boolean invoke () {
                try {
                    _errmsg = BangServer.author.createAccount(
                        username, password, email, affiliate, machIdent, bdate);
                    if (_errmsg == null) {
                        BangServer.author.setAccountIsActive(username, true);
                        _playrepo.clearAnonymous(user.playerId);
                    }
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING,
                        "Failed to create account for [who=" + user.who() + "]", pe);
                    _errmsg = INTERNAL_ERROR;
                }
                return true;
            }
            public void handleResult () {
                if (_errmsg != null) {
                    listener.requestFailed(_errmsg);
                    user.tokens.setToken(BangTokenRing.ANONYMOUS, true);
                    user.setTokens(user.tokens);
                } else {
                    listener.requestProcessed();
                }
            }
            protected String _errmsg;
        });
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
        PlayerObject user = BangServer.lookupPlayer(ownerId);
        if (user != null) {
            deliverItemLocal(user, item, source);
        } else if (BangServer.peermgr != null) {
            // try our peers
            BangServer.peermgr.forwardItem(item, source);
        }
    }

    /**
     * Delivers an item to a player on this server.
     */
    public void deliverItemLocal (PlayerObject user, Item item, String source)
    {
        user.addToInventory(item);
        SpeakProvider.sendInfo(user, BangCodes.BANG_MSGS,
            MessageBundle.compose("m.received_item", item.getName(), source));
    }

    /**
     * Sends a pardner invite to the specified player if he is online (on any server).
     */
    public void sendPardnerInvite (Handle invitee, Handle inviter, String message)
    {
        PlayerObject user = BangServer.lookupPlayer(invitee);
        if (user != null) {
            sendPardnerInviteLocal(user, inviter, message, new Date());
        } else if (BangServer.peermgr != null) {
            BangServer.peermgr.forwardPardnerInvite(invitee, inviter, message);
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
    protected void playComputer (PlayerObject player, BangConfig config, boolean autoplay)
        throws InvocationException
    {
        HashSet<String> names = new HashSet<String>();
        names.add(player.getVisibleName().toString());

        // configure our AIs and the player names array
        if (!autoplay) {
            config.players[0] = player.getVisibleName();
        }
        for (int ii = autoplay ? 0 : 1; ii < config.players.length; ii++) {
            BangAI ai = BangAI.createAI(1, 50, names);
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

        } catch (InstantiationException ie) {
            log.log(Level.WARNING, "Error instantiating game " +
                    "[for=" + player.who() + ", config=" + config + "].", ie);
            throw new InvocationException(INTERNAL_ERROR);
        }
    }

    /**
     * Converts a players {@link Rating}s records into ranking levels for inclusion in their poster
     * info.
     */
    protected StreamableHashMap<String, Integer> buildRankings (
        Iterable<Rating> ratings)
    {
        StreamableHashMap<String, Integer> map = new StreamableHashMap<String,Integer>();
        for (Rating rating : ratings) {
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
        PlayerObject player = BangServer.lookupPlayer(handle);
        if (player != null) {
            _updaters.put(handle, updater = new PardnerEntryUpdater(player));
            return updater.entry;
        }

        // check whether the player is online on another server
        Tuple<BangClientInfo,Integer> remote = null;
        if (BangServer.peermgr != null) {
            remote = BangServer.peermgr.locateRemotePlayer(handle);
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
        // if the inviter is online, update and send a notification
        PlayerObject invobj = BangServer.lookupPlayer(inviter);
        if (invobj != null) {
            if (accept) {
                invobj.addOrUpdatePardner(getPardnerEntry(user.handle, null));
                if (full[1]) {
                    clearPardnerInvites(invobj);
                }
            }
            String msg = accept ? "m.pardner_accepted" : "m.pardner_rejected";
            SpeakProvider.sendInfo(invobj, BANG_MSGS, MessageBundle.tcompose(msg, user.handle));
        }

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
        ArrayList<Comparable> keys = new ArrayList<Comparable>();
        for (Notification notification : player.notifications) {
            if (notification instanceof PardnerInvite) {
                keys.add(notification.getKey());
            }
        }
        try {
            player.startTransaction();
            for (Comparable key : keys) {
                player.removeFromNotifications(key);
            }
        } finally {
            player.commitTransaction();
        }
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
            log.warning("Requested to create symlink for missing source [song=" + song + "].");
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
                log.warning("Failed to create download symlink [song=" + song +
                            ", ident=" + ident + ", stderr=" + stderr + "].");
                return null;
            }

            // create a timestamp file
            File stamp = new File(dest.getPath() + ".stamp");
            if (!stamp.createNewFile()) {
                log.warning("Failed to create timestamp file [stamp=" + stamp + "].");
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Failure running ln command.", e);
        }

        return ident;
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
                    log.warning("Failed to delete stamp file [file=" + file.getPath() + "].");
                }
                file = new File(file.getPath().substring(0, file.getPath().length()-6));
                if (!file.delete()) {
                    log.warning("Failed to delete symlink file [file=" + file.getPath() + "].");
                }
            }
        }
    }

    /** Provides access to the pardner database. */
    protected PardnerRepository _pardrepo;

    /** Provides access to the poster database. */
    protected PosterRepository _postrepo;

    /** Provides access to the look database. */
    protected LookRepository _lookrepo;

    /** Provides access to the rating database. */
    protected RatingRepository _raterepo;

    /** Provides access to the player database. */
    protected PlayerRepository _playrepo;

    /** Used to file complaint reports. */
    protected UnderwireRepository _underepo;

    /** Maps the names of users to updaters responsible for keeping their {@link PardnerEntry}s
     * up-to-date. */
    protected HashMap<Handle, PardnerEntryUpdater> _updaters =
        new HashMap<Handle, PardnerEntryUpdater>();

    /** Keeps our {@link PardnerEntry}s up to date for remote players. */
    protected RemotePlayerWatcher<PardnerEntry> _pardwatcher;

    /** A light-weight cache of soft {@link PosterInfo} references. */
    protected Map<Handle, SoftReference<PosterInfo>> _posterCache =
        new HashMap<Handle, SoftReference<PosterInfo>>();

    /** The name of our poster cache. */
    protected static final String POSTER_CACHE = "posterCache";

    /** The time after which song download symlinks are purged. */
    protected static final long DOWNLOAD_PURGE_EXPIRE = 5 * 60 * 1000L;

    /** The frequency with which download symlinks are purged. */
    protected static final long DOWNLOAD_PURGE_INTERVAL = 60 * 1000L;
}
