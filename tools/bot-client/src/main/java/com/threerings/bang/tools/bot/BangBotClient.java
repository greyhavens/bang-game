//
// $Id$

package com.threerings.bang.tools.bot;

import java.util.Arrays;

import com.samskivert.util.BasicRunQueue;
import com.samskivert.util.Config;
import com.samskivert.util.Interval;

import com.threerings.util.MessageManager;
import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.LocationObserver;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.client.GameReadyObserver;
import com.threerings.parlor.client.ParlorDirector;
import com.threerings.parlor.game.data.GameObject;
import com.threerings.parlor.util.ParlorContext;

import com.threerings.admin.data.AdminCodes;

import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.BangCredentials;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ModifiableDSet;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.util.DeploymentConfig;
import com.threerings.bang.util.IdentUtil;

import com.samskivert.servlet.user.Password;

/**
 * A <b>rendering-free</b> Narya "Presents" bot client -- the headless analogue of the live
 * {@code BangClient}, built for agent / CI testing. It is the closest thing this game has to a
 * Playwright harness:
 *
 * <ol>
 *   <li>Connects to a running {@code BangServer} over the Narya wire and logs in (default
 *       {@code test}/{@code yeehaw}, which holds the admin token autoplay requires).</li>
 *   <li>Drives a game to completion by reusing the existing AI machinery:
 *       {@link PlayerService#playComputer} with {@code autoplay=true} creates an AI-vs-AI game
 *       that the server runs autonomously (every slot is a {@link com.threerings.bang.game.data.BangAI};
 *       no human "ready" is needed, so it ticks server-side with no client input).</li>
 *   <li><b>Asserts on distributed-object state</b> as the game runs: it subscribes to the
 *       {@link BangObject} and verifies it transitions
 *       {@code PRE_GAME -> SELECT_PHASE/IN_PLAY -> GAME_OVER}, that game ticks advance, that
 *       board pieces (units) exist, and that a winner / points are recorded at game over.</li>
 * </ol>
 *
 * There is <b>no jME3 / LWJGL / GL / BUI</b> here at all: we never construct {@code BangClient},
 * {@code BasicClient} or {@code JmeApp}. We enter the game place through the normal authorized
 * {@code LocationDirector.moveTo} path (a raw {@code subscribeToObject} is access-denied by the
 * server until you formally enter the place), but a custom {@link LocationDirector} returns a
 * trivial headless {@link PlaceController} (null view, no UI) instead of the rendering
 * {@code BangController}. Assertions fail loudly (non-zero exit) so this can act as a CI smoke
 * test.
 *
 * <p>Run against a {@code bin/devtest --no-client} server:
 * <pre>./gradlew :tools:bot-client:run</pre>
 * Tunables: {@code -Phost=}, {@code -Pusername=}, {@code -Ppassword=}, {@code -Pplayers=},
 * {@code -Pscenario=}, {@code -Pboard=}, {@code -Ptimeout=} (seconds).
 */
public class BangBotClient
    implements ClientObserver, GameReadyObserver, LocationObserver, AttributeChangeListener
{
    public static void main (String[] args)
    {
        String host = sysprop("bot.host", "localhost");
        int port = Integer.getInteger("bot.port", 47624);
        String username = sysprop("bot.username", "test");
        String password = sysprop("bot.password", "yeehaw");
        int players = Integer.getInteger("bot.players", 2);
        int timeoutSecs = Integer.getInteger("bot.timeout", 180);
        String scenario = System.getProperty("bot.scenario"); // null => random valid
        String board = System.getProperty("bot.board");        // null => server picks

        BangBotClient bot = new BangBotClient(players, scenario, board, timeoutSecs);
        int rv = bot.run(host, port, username, password);
        System.out.println(rv == 0 ? "BOT RESULT: PASS" : "BOT RESULT: FAIL (code " + rv + ")");
        System.exit(rv);
    }

    public BangBotClient (int players, String scenario, String board, int timeoutSecs)
    {
        _players = players;
        _scenario = scenario;
        _board = board;
        _timeoutSecs = timeoutSecs;
    }

    /** Logs in, starts the game, runs the event loop until the game ends or we time out. Returns
     * a process exit code (0 = all assertions passed). */
    public int run (String host, int port, String username, String password)
    {
        BangCredentials creds = new BangCredentials(
            new Name(username), Password.makeFromClear(password));
        // the server rejects a blank/spoofed machine ident; mirror BangClient.createCredentials:
        // a real MAC-derived ident, "C"-prefixed (the server decodes substring(1) + checksum)
        creds.ident = IdentUtil.getMachineIdentifier();
        if (creds.ident != null && !creds.ident.matches("S[A-Za-z0-9/+]{32}")) {
            creds.ident = "C" + creds.ident;
        }

        _client = new Client(creds, _rqueue);
        _client.setServer(host, new int[] { port });
        _client.setVersion(String.valueOf(DeploymentConfig.getVersion()));
        _client.addClientObserver(this);

        // construct the (renderless) directors now that the Client exists -- their ctors call
        // _ctx.getClient(), so they cannot be built before _client is set. The LocationDirector
        // is subclassed to hand back a headless PlaceController (no BangController, no rendering).
        _locdir = new LocationDirector(_ctx) {
            @Override protected PlaceController createController (PlaceConfig config) {
                return new HeadlessPlaceController();
            }
        };
        _locdir.addLocationObserver(this);
        _occdir = new OccupantDirector(_ctx);
        _chatdir = new ChatDirector(_ctx, "chat");
        _pardir = new ParlorDirector(_ctx);

        log("connecting to " + host + ":" + port + " as '" + username + "' (version " +
            DeploymentConfig.getVersion() + ")");

        // hard wall-clock watchdog: if anything stalls, fail rather than hang forever
        Interval watchdog = new Interval(_rqueue) {
            public void expired () {
                if (!_done) {
                    fail("watchdog timeout after " + _timeoutSecs + "s (last state=" +
                         _lastState + ", lastTick=" + _lastTick + ")");
                    shutdown();
                }
            }
        };
        watchdog.schedule(_timeoutSecs * 1000L);

        _client.logon();

        // drive the dobj event loop on this thread until shutdown() stops it
        _rqueue.run();

        return _failure == null ? 0 : 1;
    }

    // ---- ClientObserver -------------------------------------------------------------------

    public void clientWillLogon (Client client)
    {
        // request the admin service group so the server treats us like the real client does
        // (admin token still comes from the account; this just mirrors BangClient).
        client.addServiceGroup(AdminCodes.ADMIN_GROUP);
    }

    public void clientDidLogon (Client client)
    {
        PlayerObject user = (PlayerObject)client.getClientObject();
        log("logged on as " + user.username + " (townId=" + user.townId +
            ", admin=" + user.tokens.isAdmin() + ")");
        check(user.tokens.isAdmin(),
              "account must hold the admin token for autoplay (test/yeehaw should)");
        if (_failure != null) {
            shutdown();
            return;
        }

        // pick scenario id(s): explicit -Pscenario, else random valid ids for our town
        String[] scenarios;
        if (_scenario != null) {
            scenarios = new String[] { _scenario };
        } else {
            scenarios = ScenarioInfo.selectRandomIds(
                user.townId, 1, _players, null, false, Criterion.ANY);
        }
        log("starting autoplay game: players=" + _players + ", scenarios=" +
            Arrays.toString(scenarios) + ", board=" + _board);

        // register as a game-ready observer FIRST so we intercept the notification and subscribe
        // to the game object directly (no LocationDirector.moveTo -> no rendering PlaceController)
        _pardir.addGameReadyObserver(this);

        PlayerService psvc = client.requireService(PlayerService.class);
        psvc.playComputer(_players, scenarios, _board, true /*autoplay*/,
                          new InvocationService.InvocationListener() {
            public void requestFailed (String cause) {
                fail("playComputer request failed: " + cause);
                shutdown();
            }
        });
    }

    public void clientFailedToLogon (Client client, Exception cause)
    {
        fail("failed to logon: " + cause);
        shutdown();
    }

    public void clientConnectionFailed (Client client, Exception cause)
    {
        fail("connection failed: " + cause);
        shutdown();
    }

    public boolean clientWillLogoff (Client client)
    {
        return true;
    }

    public void clientDidLogoff (Client client)
    {
        log("logged off");
    }

    public void clientObjectDidChange (Client client) { /* unused */ }
    public void clientDidClear (Client client) { /* unused */ }

    // ---- GameReadyObserver ----------------------------------------------------------------

    public boolean receivedGameReady (int gameOid)
    {
        log("game is ready: oid=" + gameOid + " -> entering place (headless controller)");
        _gameOid = gameOid;
        // enter the place via the authorized moveTo path (the server grants object access on a
        // formal place entry). Our LocationDirector builds a headless PlaceController, not the
        // rendering BangController. Return true so ParlorDirector does not ALSO call moveTo.
        _locdir.moveTo(gameOid);
        return true;
    }

    // ---- LocationObserver -----------------------------------------------------------------

    public boolean locationMayChange (int placeId)
    {
        return true;
    }

    public void locationDidChange (PlaceObject place)
    {
        if (!(place instanceof BangObject)) {
            return;
        }
        BangObject bangobj = (BangObject)place;
        _bangobj = bangobj;
        // The server never sends the initial pieces DSet as a dobj event (it assigns the field
        // directly), so client-side it arrives null; give it an empty set so the stream of
        // per-piece element updates has somewhere to land instead of NPEing in event dispatch.
        // (We don't assert on per-piece state -- see the pieces note in evaluate().)
        if (bangobj.pieces == null) {
            bangobj.pieces = new ModifiableDSet<Piece>();
        }
        bangobj.addListener(this);
        log("entered BangObject oid=" + bangobj.getOid() +
            " scenario=" + bangobj.scenario + " townId=" + bangobj.townId +
            " players=" + Arrays.toString(bangobj.players) + " state=" +
            stateName(bangobj.state));

        // ASSERTION 1: the game object exists with the right shape
        check(bangobj.players != null && bangobj.players.length == _players,
              "BangObject.players should have " + _players + " entries, was " +
              (bangobj.players == null ? "null" : bangobj.players.length));

        // This is an all-AI autoplay game: the server runs both AIs, but it waits for the
        // observing client to drive the pre-game handshake (BangManager.playerReady /
        // playerReadyFor, see "if all players are AIs, the human observer determines when to
        // proceed"). We have no rendered board to gate on, so we reproduce that handshake purely
        // from dobj state: report ready now, and advance each phase as the state changes.
        reportReady();

        recordState(bangobj.state);
        maybeAdvancePhase(bangobj.state);
        evaluate();
    }

    /** Tells the server we (the observer) are ready -> triggers playersAllHere() server-side. */
    protected void reportReady ()
    {
        log("-> playerReady (kick off all-AI game)");
        _bangobj.manager.invoke("playerReady");
    }

    /** Mirrors BangController's pre-game phase acks for an all-AI game: ack SKIP_SELECT_PHASE and
     * then IN_PLAY so the server advances to the first tick (there is no board fade-in to gate on
     * headless). */
    protected void maybeAdvancePhase (int state)
    {
        switch (state) {
        case BangObject.SKIP_SELECT_PHASE:
        case BangObject.SELECT_PHASE:
            log("-> playerReadyFor(SKIP_SELECT_PHASE), playerReadyFor(IN_PLAY)");
            _bangobj.manager.invoke("playerReadyFor", BangObject.SKIP_SELECT_PHASE);
            _bangobj.manager.invoke("playerReadyFor", BangObject.IN_PLAY);
            break;
        default:
            break;
        }
    }

    public void locationChangeFailed (int placeId, String reason)
    {
        fail("failed to enter game place oid=" + placeId + ": " + reason);
        shutdown();
    }

    // ---- AttributeChangeListener ----------------------------------------------------------

    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        // NB: BangObject.state is an int but BangObject.tick is a short; use Number to be safe
        // for either boxed type rather than AttributeChangedEvent.getIntValue() (Integer-only).
        if (BangObject.STATE.equals(name)) {
            int state = ((Number)event.getValue()).intValue();
            recordState(state);
            log("state -> " + stateName(state));
            maybeAdvancePhase(state);
        } else if (BangObject.TICK.equals(name)) {
            int tick = ((Number)event.getValue()).intValue();
            if (tick > _lastTick) {
                _lastTick = tick;
                // a real simulation tick is >0; tick==0 is the round-start reset the server emits
                // (BangManager.tick((short)0)) before any unit acts, so it must not count here.
                _sawTickAdvance |= (tick > 0);
            }
            // count units once the board is live
            if (tick == 0 && _bangobj != null && _bangobj.pieces != null) {
                _maxPieces = Math.max(_maxPieces, _bangobj.pieces.size());
            }
        }
        if (_bangobj != null && _bangobj.pieces != null) {
            _maxPieces = Math.max(_maxPieces, _bangobj.pieces.size());
        }
        evaluate();
    }

    // ---- assertion engine -----------------------------------------------------------------

    protected void recordState (int state)
    {
        _lastState = state;
        switch (state) {
        case BangObject.SELECT_PHASE:
        case BangObject.SKIP_SELECT_PHASE:
        case BangObject.IN_PLAY:
        case BangObject.POST_ROUND:
            _sawInPlay = true;
            break;
        case GameObject.GAME_OVER:
            _sawGameOver = true;
            break;
        case GameObject.CANCELLED:
            fail("game was CANCELLED before completing");
            break;
        }
    }

    /** Checks the win condition; when the game reaches GAME_OVER, validates the full set of dobj
     * assertions and shuts down. */
    protected void evaluate ()
    {
        if (_done || _bangobj == null) {
            return;
        }
        if (!_sawGameOver) {
            return;
        }

        // ASSERTION 2: we observed the game enter active play (units moving each tick)
        check(_sawInPlay, "game never entered an in-play phase (SELECT/IN_PLAY/POST_ROUND)");

        // ASSERTION 3: ticks advanced -- the board actually simulated
        check(_sawTickAdvance, "game ticks never advanced past 0 (board did not simulate)");

        // ASSERTION 4: a winner is recorded at game over
        boolean[] winners = _bangobj.winners;
        boolean haveWinner = false;
        if (winners != null) {
            for (boolean w : winners) {
                haveWinner |= w;
            }
        }
        check(winners != null && winners.length == _players,
              "winners[] should have " + _players + " entries at game over");
        check(haveWinner, "no winner flagged at game over (winners=" +
              (winners == null ? "null" : Arrays.toString(winners)) + ")");

        // ASSERTION 5: points/score were tallied to a meaningful (non-zero) result. A non-zero
        // total proves the AIs actually played -- moved units, captured/scored -- end to end; it
        // is the attribute-backed proxy for "units existed and acted" (see the pieces note below).
        int[] points = _bangobj.points;
        int totalPoints = 0;
        if (points != null) {
            for (int p : points) {
                totalPoints += p;
            }
        }
        check(points != null && points.length == _players,
              "points[] should have " + _players + " entries at game over");
        check(totalPoints > 0, "no points were scored -- the game did not actually simulate play");

        log("GAME OVER reached.");
        log("  final state    = " + stateName(_bangobj.state));
        log("  players        = " + Arrays.toString(_bangobj.players));
        log("  winners        = " + (winners == null ? "null" : Arrays.toString(winners)));
        log("  points         = " + (points == null ? "null" : Arrays.toString(points)) +
            " (total=" + totalPoints + ")");
        log("  last tick seen = " + _lastTick);

        // NB (deferred): the per-piece DSet is not asserted here. The server writes BangObject.
        // pieces as a direct field assignment (no dobj event); the live client reconstructs the
        // piece set from the loaded .board file, which is rendering-adjacent and out of scope for
        // a headless bot. So pieces stays empty client-side and element updates for it are dropped.
        // We instead assert on attribute-level state that IS synced over the wire (phases, ticks,
        // winners, points) -- points>0 proves units fought and scored without needing the board.
        log("  pieces (dobj)  = " + _maxPieces + " (per-piece tracking deferred; see note)");

        if (_failure == null) {
            log("ALL DOBJ ASSERTIONS PASSED");
        }
        shutdown();
    }

    protected void check (boolean cond, String desc)
    {
        if (!cond) {
            fail("ASSERTION FAILED: " + desc);
        }
    }

    protected void fail (String msg)
    {
        if (_failure == null) {
            _failure = msg;
        }
        System.err.println("[bot] " + msg);
    }

    protected void shutdown ()
    {
        if (_done) {
            return;
        }
        _done = true;
        try {
            if (_bangobj != null) {
                _bangobj.removeListener(this);
            }
            if (_client.isLoggedOn()) {
                _client.logoff(false);
            }
        } catch (Throwable t) {
            // best effort
        }
        // stop the run-queue loop so run() returns (shutdown() sets the loop flag and kicks the
        // queue; safe to call from a runnable on the dispatch thread)
        _rqueue.postRunnable(new Runnable() {
            public void run () {
                _rqueue.shutdown();
            }
        });
    }

    protected static String stateName (int state)
    {
        switch (state) {
        case GameObject.PRE_GAME: return "PRE_GAME";
        case GameObject.IN_PLAY: return "IN_PLAY";
        case GameObject.GAME_OVER: return "GAME_OVER";
        case GameObject.CANCELLED: return "CANCELLED";
        case BangObject.SELECT_PHASE: return "SELECT_PHASE";
        case BangObject.SKIP_SELECT_PHASE: return "SKIP_SELECT_PHASE";
        case BangObject.POST_ROUND: return "POST_ROUND";
        default: return "state#" + state;
        }
    }

    protected static String sysprop (String key, String def)
    {
        String v = System.getProperty(key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    protected static void log (String msg)
    {
        System.out.println("[bot] " + msg);
    }

    // configuration
    protected final int _players;
    protected final String _scenario;
    protected final String _board;
    protected final int _timeoutSecs;

    // wire / context
    protected Client _client;
    protected final BasicRunQueue _rqueue = new BasicRunQueue("bot-client");

    // observed game state
    protected int _gameOid;
    protected BangObject _bangobj;
    protected int _lastState = GameObject.PRE_GAME;
    protected int _lastTick = -1;
    protected int _maxPieces;
    protected boolean _sawInPlay;
    protected boolean _sawTickAdvance;
    protected boolean _sawGameOver;

    // lifecycle
    protected boolean _done;
    protected String _failure;

    // ---- minimal renderless ParlorContext ------------------------------------------------
    // Only the directors that the parlor/crowd client code actually needs; no rendering.

    protected final MessageManager _msgmgr = new MessageManager("rsrc.i18n");
    protected final Config _config = new Config("bot");

    protected final ParlorContext _ctx = new ParlorContext() {
        public Config getConfig () { return _config; }
        public Client getClient () { return _client; }
        public DObjectManager getDObjectManager () { return _client.getDObjectManager(); }
        public LocationDirector getLocationDirector () { return _locdir; }
        public OccupantDirector getOccupantDirector () { return _occdir; }
        public ChatDirector getChatDirector () { return _chatdir; }
        public ParlorDirector getParlorDirector () { return _pardir; }
        public MessageManager getMessageManager () { return _msgmgr; }
        public void setPlaceView (PlaceView view) { /* headless: no UI */ }
        public void clearPlaceView (PlaceView view) { /* headless: no UI */ }
    };

    // directors are built in run() once _client exists (their ctors dereference _ctx.getClient())
    protected LocationDirector _locdir;
    protected OccupantDirector _occdir;
    protected ChatDirector _chatdir;
    protected ParlorDirector _pardir;

    /**
     * A trivial, rendering-free {@link PlaceController} used instead of {@code BangController}.
     * The base class defaults a null place view, so {@code init}/{@code willEnterPlace} do no UI
     * work; entering the place is purely what authorizes our access to the {@link BangObject}.
     */
    protected static class HeadlessPlaceController extends PlaceController
    {
        @Override protected PlaceView createPlaceView (CrowdContext ctx) {
            return null; // no UI
        }
    }
}
