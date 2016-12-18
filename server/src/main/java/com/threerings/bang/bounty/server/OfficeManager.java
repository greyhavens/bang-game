//
// $Id$

package com.threerings.bang.bounty.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.jme.util.export.binary.BinaryImporter;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.Invoker;

import com.threerings.resource.ResourceManager;
import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.GuestHandle;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Star;
import com.threerings.bang.data.StatType;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.BoardManager;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.ShopManager;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.server.BangManager;
import com.threerings.bang.game.util.BoardFile;

import com.threerings.bang.bounty.client.OfficeService;
import com.threerings.bang.bounty.data.BoardInfo;
import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.bounty.data.OfficeCodes;
import com.threerings.bang.bounty.data.OfficeMarshaller;
import com.threerings.bang.bounty.data.OfficeObject;
import com.threerings.bang.bounty.data.RecentCompleters;
import com.threerings.bang.bounty.server.persist.BountyRepository;
import com.threerings.bang.bounty.server.persist.RecentCompletersRecord;

import static com.threerings.bang.Log.log;

/**
 * Handles the server side of the Sheriff's Office.
 */
@Singleton
public class OfficeManager extends ShopManager
    implements OfficeCodes, OfficeProvider
{
    /**
     * This would be part of {@link OfficeService} but we need to be able to call it at the end of
     * a bounty game at which point we are not in the Sheriff's Office.
     */
    public void playBountyGame (PlayerObject caller, String bountyId, final String gameId,
                                final OfficeService.InvocationListener listener)
        throws InvocationException
    {
        final PlayerObject player = requireShopEnabled(caller);

        final BountyConfig config = BountyConfig.getBounty(bountyId);
        if (config == null) {
            log.warning("Received request to start unknown bounty", "from", player.who(),
                        "bounty", bountyId, "game", gameId);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // if we're a onetime deployment, disallow anything other than the easy bounties for
        // non-onetime-holders
        if (DeploymentConfig.usesOneTime() && !player.holdsOneTime() &&
            (config.difficulty != Star.Difficulty.EASY || config.type != BountyConfig.Type.TOWN)) {
            throw new InvocationException(BangCodes.E_LACK_ONETIME);
        }
            
        // make sure they haven't hacked their client
        if (!config.isAvailable(player)) {
            log.warning("Player requested to start unavailable bounty", "who", player.who(),
                        "bounty", bountyId);
            throw new InvocationException(ACCESS_DENIED);
        }
        if (config.inOrder && !player.tokens.isSupport()) {
            for (BountyConfig.GameInfo game : config.games) {
                if (game.ident.equals(gameId)) {
                    break;
                } else if (!player.stats.containsValue(StatType.BOUNTY_GAMES_COMPLETED,
                                                       config.getStatKey(game.ident))) {
                    log.warning("Player tried to play bounty game out of order",
                                "who", player.who(), "bounty", bountyId, "game", gameId);
                    throw new InvocationException(ACCESS_DENIED);
                }
            }
        }

        // check whether we've already cached the game config in question
        final String key = bountyId + "/" + gameId;
        BangConfig gconfig = _configs.get(key);
        if (gconfig != null) {
            startBountyGame(player, config, gameId, gconfig);
            return;
        }

        // load up the game configuration from disk (on the invoker thread)
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    String path = config.getGamePath(gameId);
                    _gconfig = (BangConfig)BinaryImporter.getInstance().load(
                        _rsrcmgr.getResource(path));
                } catch (Exception e) {
                    log.warning("Failed to load bounty game", "key", key, e);
                }
                return true;
            }

            public void handleResult () {
                String err = INTERNAL_ERROR;
                if (_gconfig != null) {
                    _configs.put(key, _gconfig);
                    try {
                        startBountyGame(player, config, gameId, _gconfig);
                        return;
                    } catch (InvocationException ie) {
                        err = ie.getMessage();
                        // fall through and fail
                    }
                }
                listener.requestFailed(err);
            }

            protected BangConfig _gconfig;
        });
    }

    /**
     * Called by the BangManager when a bounty game is completed that completes a whole bounty for
     * a player.
     */
    public void noteCompletedBounty (String bountyId, Handle player)
    {
        // anonymous players don't get put on the list
        if (player instanceof GuestHandle) {
            return;
        }

        // add their name to the appropriate completers list
        RecentCompleters comp = _offobj.completers.get(bountyId);
        if (comp == null) {
            comp = new RecentCompleters();
            comp.bountyId = bountyId;
            comp.addCompleter(player.toString());
            _offobj.addToCompleters(comp);
        } else {
            comp.addCompleter(player.toString());
            _offobj.updateCompleters(comp);
        }

        // record it to the database
        final RecentCompletersRecord record = new RecentCompletersRecord(ServerConfig.townId, comp);
        BangServer.invoker.postUnit(new Invoker.Unit("updateRecentCompleters") {
            public boolean invoke () {
                try {
                    _bountyrepo.storeCompleters(record);
                } catch (Exception e) {
                    log.warning("Failed to store recent completers " + record + ".", e);
                }
                return false;
            };
        });
    }

    // from interface OfficeProvider
    public void testBountyGame (PlayerObject caller, BangConfig config,
                                OfficeService.InvocationListener listener)
        throws InvocationException
    {
        PlayerObject player = requireShopEnabled(caller);
        if (!player.tokens.isSupport()) {
            throw new InvocationException(ACCESS_DENIED);
        }

        // create a fake bounty config
        BountyConfig bounty = new BountyConfig();
        bounty.title = "Bounty Game Test";
        bounty.ident = "test";
        bounty.reward = new BountyConfig.Reward();
        bounty.reward.scrip = 100;
        BountyConfig.GameInfo info = new BountyConfig.GameInfo();
        info.ident = "test";
        info.preGameQuote = new BountyConfig.Quote();
        info.preGameQuote.text = "Do you want to play a game?";
        info.failedQuote = new BountyConfig.Quote();
        info.failedQuote.text = "All your base are belong to us.";
        info.completedQuote = new BountyConfig.Quote();
        info.completedQuote.text = "Only now in this dark hour do I see the folly of guns.";
        bounty.games.add(info);

        startBountyGame(player, bounty, "test", config);
    }

    protected void startBountyGame (PlayerObject user, BountyConfig bounty, String gameId,
                                    BangConfig gconfig)
        throws InvocationException
    {
        HashSet<String> names = new HashSet<String>();
        names.add(user.getVisibleName().toString());

        // configure our AIs and the player names array
        gconfig.type = BangConfig.Type.BOUNTY;
        gconfig.rated = false;
        gconfig.players = new Name[gconfig.plist.size()];
        gconfig.ais = new BangAI[gconfig.plist.size()];
        gconfig.players[0] = user.getVisibleName();
        for (int ii = 1; ii < gconfig.players.length; ii++) {
            BangConfig.Player player = gconfig.plist.get(ii);
            BangAI ai = BangAI.createAI(1, player.skill, names);
            gconfig.ais[ii] = bounty.getOpponent(gameId, gconfig.players.length, ii, ai);
            gconfig.players[ii] = ai.handle;
        }

        try {
            BangManager bangmgr = (BangManager)BangServer.plreg.createPlace(gconfig);
            bangmgr.setBountyConfig(bounty, gameId);
        } catch (InstantiationException ie) {
            log.warning("Error instantiating bounty game", "for", user.who(), "bounty", bounty,
                        "gconfig", gconfig, ie);
            throw new InvocationException(INTERNAL_ERROR);
        }
    }

    @Override // from ShopManager
    protected String getIdent ()
    {
        return "office";
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new OfficeObject();
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _offobj = (OfficeObject)_plobj;
        _offobj.setService(BangServer.invmgr.registerProvider(this, OfficeMarshaller.class));

        // publish all known boards as board info records
        ArrayList<BoardInfo> infos = new ArrayList<BoardInfo>();
        for (int pp = 2; pp <= GameCodes.MAX_PLAYERS; pp++) {
            for (BoardFile brec : _boardmgr.getBoards(pp)) {
                BoardInfo info = new BoardInfo();
                info.name = brec.name;
                info.players = brec.players;
                info.scenarios = brec.scenarios;
                infos.add(info);
            }
        }
        _offobj.setBoards(new DSet<BoardInfo>(infos.iterator()));

        // load our recent completers information from the database
        BangServer.invoker.postUnit(new RepositoryUnit("loadRecentCompleters") {
            public void invokePersist () throws Exception {
                for (RecentCompletersRecord record :
                         _bountyrepo.loadCompleters(ServerConfig.townId)) {
                    _comps.add(record.toRecentCompleters());
                }
            }
            public void handleSuccess () {
                _offobj.setCompleters(new DSet<RecentCompleters>(_comps));
            }
            public void handleFailure (Exception cause) {
                log.warning("Failed to load recent completers.", cause);
                _offobj.setCompleters(new DSet<RecentCompleters>());
            }
            protected ArrayList<RecentCompleters> _comps = new ArrayList<RecentCompleters>();
        });
    }

    protected OfficeObject _offobj;

    /** A cache of all loaded bounty configurations. */
    protected HashMap<String,BangConfig> _configs = new HashMap<String,BangConfig>();

    // dependencies
    @Inject protected ResourceManager _rsrcmgr;
    @Inject protected BoardManager _boardmgr;
    @Inject protected BountyRepository _bountyrepo;
}
