//
// $Id$

package com.threerings.bang.bounty.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;

import com.jme.util.export.binary.BinaryImporter;
import com.samskivert.util.Invoker;

import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.BoardRecord;

import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.server.BangManager;

import com.threerings.bang.bounty.client.OfficeService;
import com.threerings.bang.bounty.data.BoardInfo;
import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.bounty.data.OfficeCodes;
import com.threerings.bang.bounty.data.OfficeMarshaller;
import com.threerings.bang.bounty.data.OfficeObject;

import static com.threerings.bang.Log.log;

/**
 * Handles the server side of the Sheriff's Office.
 */
public class OfficeManager extends PlaceManager
    implements OfficeCodes, OfficeProvider
{
    /**
     * This would be part of {@link OfficeService} but we need to be able to call it at the end of
     * a bounty game at which point we are not in the Sheriff's Office.
     */
    public void playBountyGame (ClientObject caller, String bountyId, final String gameId,
                                final OfficeService.InvocationListener listener)
        throws InvocationException
    {
        final PlayerObject player = (PlayerObject)caller;

        final BountyConfig config = BountyConfig.getBounty(bountyId);
        if (config == null) {
            log.warning("Received request to start unknown bounty [from=" + player.who() +
                        ", bounty=" + bountyId + ", game=" + gameId + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure they haven't hacked their client
        if (!config.isAvailable(player)) {
            log.warning("Player requested to start unavailable bounty [who=" + player.who() +
                        ", bounty=" + bountyId + "].");
            throw new InvocationException(ACCESS_DENIED);
        }
        if (config.inOrder) {
            for (BountyConfig.GameInfo game : config.games) {
                if (game.ident.equals(gameId)) {
                    break;
                } else if (!player.stats.containsValue(Stat.Type.BOUNTY_GAMES_COMPLETED,
                                                       config.getStatKey(game.ident))) {
                    log.warning("Player tryied to play bounty game out of order " +
                                "[who=" + player.who() + ", bounty=" + bountyId +
                                ", game=" + gameId + "].");
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
                    String path = "bounties/" + ServerConfig.townId + "/" +
                        config.type.toString().toLowerCase() + "/" + key + ".game";
                    _gconfig = (BangConfig)BinaryImporter.getInstance().load(
                        BangServer.rsrcmgr.getResource(path));
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to load bounty game [key=" + key + "].", e);
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

    // from interface OfficeProvider
    public void testBountyGame (ClientObject caller, BangConfig config,
                                OfficeService.InvocationListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject)caller;
        if (!player.tokens.isSupport()) {
            throw new InvocationException(ACCESS_DENIED);
        }

        // create a fake bounty config
        BountyConfig bounty = new BountyConfig();
        bounty.reward = new BountyConfig.Reward();
        bounty.reward.scrip = 100;
        BountyConfig.GameInfo info = new BountyConfig.GameInfo();
        info.ident = "test";
        info.preGameQuote = "Do you want to play a game?";
        info.failedQuote = "All your base are belong to us.";
        info.completedQuote = "Only now in this dark hour do I see the folly of guns.";
        bounty.games.add(info);

        startBountyGame(player, bounty, "test", config);
    }

    protected void startBountyGame (PlayerObject player, BountyConfig bounty, String gameId,
                                    BangConfig gconfig)
        throws InvocationException
    {
        HashSet<String> names = new HashSet<String>();
        names.add(player.getVisibleName().toString());

        // configure our AIs and the player names array
        gconfig.type = BangConfig.Type.BOUNTY;
        gconfig.rated = false;
        gconfig.players = new Name[gconfig.teams.size()];
        gconfig.ais = new BangAI[gconfig.teams.size()];
        gconfig.players[0] = player.getVisibleName();
        for (int ii = 1; ii < gconfig.players.length; ii++) {
            BangAI ai = BangAI.createAI(1, 50, names);
            // the last AI is the outlaw
            if (ii == gconfig.players.length-1) {
                if (bounty.title == null) { // we're in a test game
                    bounty.title = ai.handle.toString();
                    bounty.outlawPrint = ai.avatar;
                } else {
                    ai.handle = new Handle(bounty.title);
                    ai.avatar = bounty.outlawPrint;
                }
            }
            gconfig.players[ii] = ai.handle;
            gconfig.ais[ii] = ai;
        }

        try {
            BangManager bangmgr = (BangManager)BangServer.plreg.createPlace(gconfig);
            bangmgr.setBountyConfig(bounty, gameId);
        } catch (InstantiationException ie) {
            log.log(Level.WARNING, "Error instantiating bounty game [for=" + player.who() +
                    ", bounty=" + bounty + ", gconfig=" + gconfig + "].", ie);
            throw new InvocationException(INTERNAL_ERROR);
        }
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new OfficeObject();
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
        _offobj = (OfficeObject)_plobj;
        _offobj.setService((OfficeMarshaller)BangServer.invmgr.registerDispatcher(
                               new OfficeDispatcher(this), false));

        // publish all known boards as board info records
        ArrayList<BoardInfo> infos = new ArrayList<BoardInfo>();
        for (int pp = 2; pp <= GameCodes.MAX_PLAYERS; pp++) {
            for (BoardRecord brec : BangServer.boardmgr.getBoards(pp)) {
                BoardInfo info = new BoardInfo();
                info.name = brec.name;
                info.players = brec.players;
                info.scenarios = brec.getScenarios();
                infos.add(info);
            }
        }
        _offobj.setBoards(new DSet<BoardInfo>(infos.iterator()));
    }

    protected OfficeObject _offobj;

    /** A cache of all loaded bounty configurations. */
    protected HashMap<String,BangConfig> _configs = new HashMap<String,BangConfig>();
}
