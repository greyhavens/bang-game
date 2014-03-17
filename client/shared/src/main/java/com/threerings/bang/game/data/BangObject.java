//
// $Id$

package com.threerings.bang.game.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;
import com.threerings.util.StreamablePoint;

import com.threerings.parlor.game.data.GameObject;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.StatType;

import com.threerings.bang.bounty.data.BountyConfig;

import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.util.PieceUtil;

/**
 * Contains all distributed information for the game.
 */
public class BangObject extends GameObject
{
    /** Used to track runtime metrics for the overall game. */
    public static class GameData
    {
        /** The number of live players remaining in the game. */
        public int livePlayers;

        /** The total power of all players on the board. */
        public int totalPower;

        /** The average power of the live players. */
        public float averagePower;

        /** Clears our accumulator stats in preparation for a recompute. */
        public void clear () {
            livePlayers = 0;
            totalPower = 0;
        }

        /** Generates a string representation of this instance. */
        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /** Used to track runtime metrics for each player. */
    public static class PlayerData
    {
        /** The number of still-alive units controlled by this player. */
        public int liveUnits;

        /** The total power (un-damage) controlled by this player. */
        public int power;

        /** This player's power divided by the average power. */
        public float powerFactor;

        /** This player's points divided by the average points. */
        public float pointFactor;

        /** Clears our accumulator stats in preparation for a recompute. */
        public void clear () {
            liveUnits = 0;
            power = 0;
        }

        /** Generates a string representation of this instance. */
        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /** Used to keep track of player occupant information even if they're not in the room. */
    public static class PlayerInfo implements Streamable
    {
        /** The player's unique identifier. */
        public int playerId = -1;

        /** The player's normal avatar pose. */
        public AvatarInfo avatar;

        /** The player's victory pose. */
        public AvatarInfo victory;

        /** The player's buckle. */
        public BuckleInfo buckle;

        /** The player's gang name. */
        public Handle gang;

        /** The readyness state of the player. */
        public int readyState;
    }

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>roundId</code> field. */
    public static final String ROUND_ID = "roundId";

    /** The field name of the <code>playerInfo</code> field. */
    public static final String PLAYER_INFO = "playerInfo";

    /** The field name of the <code>teams</code> field. */
    public static final String TEAMS = "teams";

    /** The field name of the <code>stats</code> field. */
    public static final String STATS = "stats";

    /** The field name of the <code>critStats</code> field. */
    public static final String CRIT_STATS = "critStats";

    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>townId</code> field. */
    public static final String TOWN_ID = "townId";

    /** The field name of the <code>scenario</code> field. */
    public static final String SCENARIO = "scenario";

    /** The field name of the <code>marquee</code> field. */
    public static final String MARQUEE = "marquee";

    /** The field name of the <code>bounty</code> field. */
    public static final String BOUNTY = "bounty";

    /** The field name of the <code>bountyGameId</code> field. */
    public static final String BOUNTY_GAME_ID = "bountyGameId";

    /** The field name of the <code>boardHash</code> field. */
    public static final String BOARD_HASH = "boardHash";

    /** The field name of the <code>boardUpdates</code> field. */
    public static final String BOARD_UPDATES = "boardUpdates";

    /** The field name of the <code>startPositions</code> field. */
    public static final String START_POSITIONS = "startPositions";

    /** The field name of the <code>bigShots</code> field. */
    public static final String BIG_SHOTS = "bigShots";

    /** The field name of the <code>tick</code> field. */
    public static final String TICK = "tick";

    /** The field name of the <code>lastTick</code> field. */
    public static final String LAST_TICK = "lastTick";

    /** The field name of the <code>duration</code> field. */
    public static final String DURATION = "duration";

    /** The field name of the <code>pieces</code> field. */
    public static final String PIECES = "pieces";

    /** The field name of the <code>debugPieces</code> field. */
    public static final String DEBUG_PIECES = "debugPieces";

    /** The field name of the <code>cards</code> field. */
    public static final String CARDS = "cards";

    /** The field name of the <code>effect</code> field. */
    public static final String EFFECT = "effect";

    /** The field name of the <code>actionId</code> field. */
    public static final String ACTION_ID = "actionId";

    /** The field name of the <code>points</code> field. */
    public static final String POINTS = "points";

    /** The field name of the <code>perRoundPoints</code> field. */
    public static final String PER_ROUND_POINTS = "perRoundPoints";

    /** The field name of the <code>perRoundRanks</code> field. */
    public static final String PER_ROUND_RANKS = "perRoundRanks";

    /** The field name of the <code>awards</code> field. */
    public static final String AWARDS = "awards";
    // AUTO-GENERATED: FIELDS END

    /** A {@link #state} constant indicating the pre-game selection phase. */
    public static final int SELECT_PHASE = 4;

    /** A {@link #state} constant used in tutorial, practice or bounty modes. */
    public static final int SKIP_SELECT_PHASE = 5;

    /** A {@link #state} constant indicating the post-round phase. */
    public static final int POST_ROUND = 6;

    /** An offset for {@link #perRoundRanks} for coop ranks. */
    public static final short COOP_RANK = 100;

    /** Contains the representation of the game board. */
    public transient BangBoard board;

    /** Contains the non-interactive props on the game board within games (but not in the editor or
     * the town view). */
    public transient Prop[] props = new Prop[0];

    /** Contains statistics on the game, updated every time any change is made to game state. */
    public transient GameData gdata = new GameData();

    /** Contains statistics on each player, updated every time any change is made to game state. */
    public transient PlayerData[] pdata;

    /** Used to assign ids to pieces added during the game. */
    public transient int maxPieceId;

    /** Identifies an effect applied to the entire board. */
    public transient String boardEffect;

    /** A hindrance affecting all units (and applied to new units). */
    public transient Hindrance globalHindrance;

    /** The minimum card and bonus weight that is allowed in this game or 100 to disable cards and
     * bonuses completely. */
    public transient int minCardBonusWeight;

    /** The id of the current round (counting from zero) or -1 if we haven't started yet. */
    public int roundId = -1;

    /** Avatar fingerprints and other data for each of the players in the game.  We need these in
     * case the player leaves early and so that we can provide fake info for AIs. */
    public PlayerInfo[] playerInfo;

    /** The team assignments for all players in the game. */
    public int[] teams;

    /** This value is set at the end of every round, to inform the players of various interesting
     * statistics. */
    public StatSet[] stats;

    /** A stat set that contains only "watched" stats relating to criteria for a bounty game. */
    public StatSet critStats;

    /** The invocation service via which the client communicates with the server. */
    public BangMarshaller service;

    /** The id of the town in which this game is being played. */
    public String townId;

    /** The metadata for the current scenario. */
    public ScenarioInfo scenario;

    /** The (untranslated) marquee to display before the start of the round. */
    public String marquee;

    /** The bounty being played or null. */
    public BountyConfig bounty;

    /** The id of the bounty game being played or null. */
    public String bountyGameId;

    /** The MD5 hash of the game board, to be compared against any cached version of the board
     * stored on the client. */
    public byte[] boardHash;

    /** A list of round-specific updates to be applied to the board after downloading it or loading
     * it from the cache. */
    public Piece[] boardUpdates;

    /** The starting positions for each player. */
    public StreamablePoint[] startPositions;

    /** The big shots selected for use by each player. */
    public Unit[] bigShots;

    /** The current board tick count. */
    public short tick;

    /** The tick after which the game will end. This may not be {@link #duration} - 1 because some
     * scenarios may opt to end the game early. */
    public short lastTick;

    /** The maximum number of ticks that will be allowed to elapse before the game is ended. Some
     * scenarios may choose to end the game early (see {@link #lastTick}). */
    public short duration;

    /** Contains information on all pieces on the board. */
    public ModifiableDSet<Piece> pieces;

    /** For debugging purposes. */
    public ModifiableDSet<Piece> debugPieces;

    /** Contains information on all available cards. */
    public ModifiableDSet<Card> cards = new ModifiableDSet<Card>();

    /** A field we use to broadcast applied effects. */
    public Effect effect;

    /** The currently executing action (only used in the tutorial). */
    public int actionId = -1;

    /** Total points earned by each player. */
    public int[] points;

    /** Points earned per player per round, this is broadcast to the client at end of the game. */
    public int[][] perRoundPoints;

    /** Rank for each player in each round, either individual or the team rank for a coop game. */
    public short[][] perRoundRanks;

    /** Used to report cash and badges awarded at the end of the game. */
    public Award[] awards;

    /** Returns an iterator over all the {@link #props} and all the {@link #pieces}. */
    public Iterator<Piece> getPropPieceIterator ()
    {
        return new Iterator<Piece>() {
            public boolean hasNext () {
                return (_idx < props.length || _it.hasNext());
            }
            public Piece next () {
                return (_idx < props.length) ? props[_idx++] : _it.next();
            }
            public void remove () {
                throw new UnsupportedOperationException("Prop/piece " +
                    "iterator does not support remove method.");
            }
            protected int _idx = 0;
            protected Iterator<Piece> _it = pieces.iterator();
        };
    }

    /** Returns {@link #pieces} as an array list to allow for simultaneous iteration and removal. */
    public ArrayList<Piece> getPieceArray ()
    {
        return pieces.toArrayList();
    }

    /**
     * Adds a piece directly to the piece set without broadcasting any distributed object events.
     * This is used by entities that are known to run on both the client and server. The board's
     * piece shadow is also updated by this call.
     */
    public void addPieceDirect (Piece piece)
    {
        pieces.addDirect(piece);
        board.shadowPiece(piece);
    }

    /**
     * Removes a piece directly from the piece set without broadcasting any distributed object
     * events. This is used by entities that are known to run on both the client and server. The
     * board's piece shadow is also updated by this call.
     */
    public void removePieceDirect (Piece piece)
    {
        pieces.removeDirect(piece);
        board.clearShadow(piece);
    }

    /**
     * Returns a list of pieces that overlap the specified piece given its (hypothetical) current
     * coordinates. If no pieces overlap, null will be returned.
     */
    public ArrayList<Piece> getOverlappers (Piece piece)
    {
        return PieceUtil.getOverlappers(pieces, piece);
    }

    /**
     * Returns true if the specified player has live pieces, false if they are totally knocked out.
     */
    public boolean hasLiveUnits (int pidx)
    {
        if (pieces != null) {
            for (Piece p : pieces) {
                if (p.owner == pidx && p instanceof Unit && p.isAlive()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the number of live units remaining for the specified player.
     */
    public int countLiveUnits (int pidx)
    {
        int pcount = 0;
        if (pieces != null) {
            for (Piece p : pieces) {
                if (p.owner == pidx && p instanceof Unit && p.isAlive()) {
                    pcount++;
                }
            }
        }
        return pcount;
    }

    /**
     * Returns the number of dead units on the board.
     */
    public int countDeadUnits ()
    {
        int pcount = 0;
        for (Piece p : pieces) {
            if (p.owner >= 0 && p instanceof Unit && !p.isAlive()) {
                pcount++;
            }
        }
        return pcount;
    }

    /**
     * Returns the number of playable cards owned by the specified player.
     */
    public int countPlayerCards (int pidx)
    {
        int ccount = 0;
        for (Card card : cards) {
            if (card.owner == pidx) {
                ccount++;
            }
        }
        return ccount;
    }

    /**
     * Returns the targetable piece at the specified coordinates or null if no targetable piece
     * exists at those coordinates.
     */
    public Piece getTarget (int tx, int ty)
    {
        for (Piece p : pieces) {
            if (p.x == tx && p.y == ty && p.isTargetable()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Returns the first available targetable piece in a direction from the specified coordinates
     * and elevation, or a dummy piece with id -1 and coordinates at the last tile reachable by the
     * projectile in its search for a target in the specified direction.
     */
    public Piece getFirstAvailableTarget (int x, int y, int dir)
    {
        int startElev = board.getHeightfieldElevation(x, y);

        do {
            // check that we can cross into the next tile
            int nx = x + Piece.DX[dir], ny = y + Piece.DY[dir];
            if (!board.canCrossSide(x, y, nx, ny)) {
                break;
            }

            // move to this tile and continue
            x = nx;
            y = ny;

            // make sure we haven't moved off of the board and that ensure that terrain does not
            // block our path
            if (!board.getPlayableArea().contains(x, y) ||
                board.getHeightfieldElevation(x, y) > startElev) {
                break;
            }

            // look for a targetable piece at this tile
            for (Piece p : pieces) {
                if (p.x == x && p.y == y) {
                    if (p.isTargetable()) {
                        return p;
                    } else {
                        break;
                    }
                }
            }

        // stop if a non-penetrable prop is in the way
        } while (board.isPenetrable(x, y));

        Unit dummy = new Unit();
        dummy.pieceId = -1;
        dummy.x = (short)x;
        dummy.y = (short)y;
        return dummy;
    }

    /**
     * Returns the average number of live units per player.
     */
    public int getAverageUnitCount ()
    {
        int[] pcount = getUnitCount();
        float tunits = 0, tcount = 0;
        for (int ucount : pcount) {
            if (ucount > 0) {
                tunits += ucount;
                tcount++;
            }
        }
        return Math.round(tunits / tcount);
    }

    /**
     * Returns the average number of live units among the specified set of players.
     */
    public int getAverageUnitCount (ArrayIntSet players)
    {
        int[] pcount = getUnitCount();
        float tunits = 0, tcount = 0;
        for (int ii = 0; ii < pcount.length; ii++) {
            if (pcount[ii] > 0 && players.contains(ii)) {
                tunits += pcount[ii];
                tcount++;
            }
        }
        return Math.round(tunits / tcount);
    }

    /**
     * Returns the count of units per player.
     */
    public int[] getUnitCount ()
    {
        int[] pcount = new int[players.length];
        for (Piece p : pieces) {
            if (p instanceof Unit && p.isAlive() && p.owner >= 0) {
                pcount[p.owner]++;
            }
        }
        return pcount;
    }

    /**
     * Returns the total count of living units.
     */
    public int getTotalUnitCount ()
    {
        int count = 0;
        for (Piece p : pieces) {
            if (p instanceof Unit && p.isAlive()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the average power of the specified set of players (referenced by index).
     */
    public float getAveragePower (ArrayIntSet players)
    {
        float tpower = 0;
        for (int ii = 0; ii < players.size(); ii++) {
            tpower += pdata[players.get(ii)].power;
        }
        return tpower/players.size();
    }

    /**
     * Returns the average damage level of all live units owned by the specified players.
     */
    public int getAverageUnitDamage (ArrayIntSet players)
    {
        int pcount = 0, tdamage = 0;
        for (Piece p : pieces) {
            if (p instanceof Unit && p.isAlive() && players.contains(p.owner)) {
                pcount++;
                tdamage += p.damage;
            }
        }
        return tdamage / pcount;
    }

    /**
     * Returns the smallest point difference between specified players scores and the max score or
     * 0 if no players can reach.
     */
    public int getPointsDiff (ArrayIntSet players)
    {
        int max = 0, ppoints = 0;
        for (int ii = 0; ii < points.length; ii++) {
            max = Math.max(max, points[ii]);
            if (players.contains(ii)) {
                ppoints = Math.max(ppoints, points[ii]);
            }
        }
        return (ppoints == 0 ? 0 : max - ppoints);
    }

    /**
     * Returns the team for the player with the specified index. If the pidx is -1 the team is
     * reported as -1 so it is safe to pass {@link Piece#owner} to this method.
     */
    public int getTeam (int pidx)
    {
        return (pidx >= 0) ? teams[pidx] : -1;
    }

    /**
     * Returns true if this is a non-bounty team game.
     */
    public boolean isTeamGame ()
    {
        if (bounty != null) {
            return false;
        }
        int numTeams = teams.length;
        for (int ii = 0; ii < teams.length; ii++) {
            for (int jj = ii + 1; jj < teams.length; jj++) {
                if (teams[ii] == teams[jj]) {
                    numTeams--;
                    break;
                }
            }
        }
        return (numTeams > 1 && numTeams < teams.length);
    }

    /**
     * Returns the number of active teams.
     */
    public int getActiveTeamCount ()
    {
        boolean[] activeTeam = new boolean[players.length];
        int count = 0;
        for (int ii = 0; ii < players.length; ii++) {
            if (isActivePlayer(ii)) {
                if (!activeTeam[teams[ii]]) {
                    activeTeam[teams[ii]] = true;
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns an array with points calculated by teams.
     */
    public int[] getTeamPoints (int[] points)
    {
        int[] tpoints = points.clone();
        for (int ii = 0; ii < points.length; ii++) {
            for (int jj = 0; jj < teams.length; jj++) {
                if (ii != jj && teams[ii] == teams[jj]) {
                    tpoints[ii] += points[jj];
                }
            }
        }
        return tpoints;
    }

    /**
     * Updates the {@link #gdata} and {@link #pdata} information.
     */
    public void updateData ()
    {
        // don't do any computation on the client
        if (pdata == null) {
            return;
        }

        // first clear out the old stats
        gdata.clear();
        for (int ii = 0; ii < pdata.length; ii++) {
            pdata[ii].clear();
        }

        for (Piece p : pieces) {
            if (p.isAlive() && p.owner >= 0) {
                pdata[p.owner].liveUnits++;
                int pp = (100 - p.damage);
                pdata[p.owner].power += pp;
                gdata.totalPower += pp;
//                 if (p.ticksUntilMovable(prevTick) == 0) {
//                     nonactors[p.owner]++;
//                 }
            }
        }

        for (int ii = 0; ii < pdata.length; ii++) {
            if (pdata[ii].liveUnits > 0) {
                gdata.livePlayers++;
            }
        }

        gdata.averagePower = (float)gdata.totalPower / gdata.livePlayers;
        for (int ii = 0; ii < pdata.length; ii++) {
            pdata[ii].powerFactor = pdata[ii].power / gdata.averagePower;
        }

//         log.info("Updated stats " + gdata + ": " + StringUtil.toString(pdata));
    }

    /**
     * Grants the specified number of bonus points to the specified player.  Their total points
     * will be updated by a call to {@link #grantPoints}.
     */
    public void grantBonusPoints (int pidx, int amount)
    {
        stats[pidx].incrementStat(StatType.BONUS_POINTS, amount);
        grantPoints(pidx, amount);
    }

    /**
     * Grants the specified number of points to the specified player, updating their {@link
     * #points} and updating the appropriate earned points statistic.
     */
    public void grantPoints (int pidx, int amount)
    {
        setPointsAt(points[pidx] + amount, pidx);
        perRoundPoints[roundId][pidx] += amount;
        stats[pidx].incrementStat(StatType.POINTS_EARNED, amount);

        // keep our point factors up to date (on the server)
        if (pdata != null) {
            float tscore = IntListUtil.sum(points);
            pdata[pidx].pointFactor = (tscore == 0) ? 1f : (points[pidx] / tscore);
        }
    }

    /**
     * Returns an adjusted points array where players that have resigned from the game are adjusted
     * to zero.
     */
    public int[] getFilteredPoints ()
    {
        int[] apoints = points.clone();
        for (int ii = 0; ii < apoints.length; ii++) {
            if (!isActivePlayer(ii)) {
                apoints[ii] = 0;
            }
        }
        return apoints;
    }

    /**
     * Returns an adjusted round points array where players that have resigned from the game are
     * adjusted to zero.
     */
    public int [] getFilteredRoundPoints (int ridx)
    {
        int [] apoints = perRoundPoints[ridx].clone();
        for (int ii = 0; ii < apoints.length; ii++) {
            if (!isActivePlayer(ii)) {
                apoints[ii] = Math.min(apoints[ii], 0);
            }
        }
        return apoints;
    }

    /**
     * Returns a lazily computed mapping from encoded tile coordinates to pieces of track on the
     * board.
     */
    public HashIntMap<Track> getTracks ()
    {
        if (_trackBoardHash == null || !Arrays.equals(_trackBoardHash, boardHash)) {
            // boardHash is null when testing uploaded boards
            if (boardHash != null) {
                _trackBoardHash = boardHash.clone();
            }
            _tracks = new HashIntMap<Track>();
            for (Piece piece : pieces) {
                if (piece instanceof Track) {
                    _tracks.put(piece.getCoord(), (Track)piece);
                }
            }
        }
        return _tracks;
    }

    /**
     * Returns a lazily computed mapping from encoded tile coordinates to teleporters on the board.
     */
    public HashIntMap<Teleporter> getTeleporters ()
    {
        if (_teleporterBoardHash == null || !Arrays.equals(_teleporterBoardHash, boardHash)) {
            // boardHash is null when testing uploaded boards
            if (boardHash != null) {
                _teleporterBoardHash = boardHash.clone();
            }
            _teleporters = new HashIntMap<Teleporter>();
            for (Piece piece : pieces) {
                if (piece instanceof Teleporter) {
                    _teleporters.put(piece.getCoord(), (Teleporter)piece);
                }
            }
        }
        return _teleporters;
    }

    @Override // documentation inherited
    public boolean isInPlay ()
    {
        return (state == SELECT_PHASE || state == IN_PLAY || state == POST_ROUND);
    }

    /**
     * Returns true only while the game is in the actual moving and shooting mode, not during round
     * set up or after the round ends.
     */
    public boolean isInteractivePlay ()
    {
        return (state == IN_PLAY && tick >= 0);
    }

    /**
     * Helper function for {@link #toString}.
     */
    public String piecesToString ()
    {
        return (pieces == null) ? "null" : String.valueOf(pieces.size());
    }

    /**
     * Helper for ticking.
     */
    public void tick (short tick)
    {
        if (GameCodes.SYNC_DEBUG && pieces != null) {
            ModifiableDSet<Piece> clone = pieces.clone();
            setDebugPieces(clone);
        }
        setTick(tick);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>roundId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRoundId (int value)
    {
        int ovalue = this.roundId;
        requestAttributeChange(
            ROUND_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.roundId = value;
    }

    /**
     * Requests that the <code>playerInfo</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPlayerInfo (BangObject.PlayerInfo[] value)
    {
        BangObject.PlayerInfo[] ovalue = this.playerInfo;
        requestAttributeChange(
            PLAYER_INFO, value, ovalue);
        this.playerInfo = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>playerInfo</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setPlayerInfoAt (BangObject.PlayerInfo value, int index)
    {
        BangObject.PlayerInfo ovalue = this.playerInfo[index];
        requestElementUpdate(
            PLAYER_INFO, index, value, ovalue);
        this.playerInfo[index] = value;
    }

    /**
     * Requests that the <code>teams</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTeams (int[] value)
    {
        int[] ovalue = this.teams;
        requestAttributeChange(
            TEAMS, value, ovalue);
        this.teams = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>teams</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setTeamsAt (int value, int index)
    {
        int ovalue = this.teams[index];
        requestElementUpdate(
            TEAMS, index, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.teams[index] = value;
    }

    /**
     * Requests that the <code>stats</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setStats (StatSet[] value)
    {
        StatSet[] ovalue = this.stats;
        requestAttributeChange(
            STATS, value, ovalue);
        this.stats = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>stats</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setStatsAt (StatSet value, int index)
    {
        StatSet ovalue = this.stats[index];
        requestElementUpdate(
            STATS, index, value, ovalue);
        this.stats[index] = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>critStats</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToCritStats (Stat elem)
    {
        requestEntryAdd(CRIT_STATS, critStats, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>critStats</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromCritStats (Comparable<?> key)
    {
        requestEntryRemove(CRIT_STATS, critStats, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>critStats</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateCritStats (Stat elem)
    {
        requestEntryUpdate(CRIT_STATS, critStats, elem);
    }

    /**
     * Requests that the <code>critStats</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setCritStats (StatSet value)
    {
        requestAttributeChange(CRIT_STATS, value, this.critStats);
        StatSet clone = (value == null) ? null : (StatSet)value.clone();
        this.critStats = clone;
    }

    /**
     * Requests that the <code>service</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setService (BangMarshaller value)
    {
        BangMarshaller ovalue = this.service;
        requestAttributeChange(
            SERVICE, value, ovalue);
        this.service = value;
    }

    /**
     * Requests that the <code>townId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTownId (String value)
    {
        String ovalue = this.townId;
        requestAttributeChange(
            TOWN_ID, value, ovalue);
        this.townId = value;
    }

    /**
     * Requests that the <code>scenario</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setScenario (ScenarioInfo value)
    {
        ScenarioInfo ovalue = this.scenario;
        requestAttributeChange(
            SCENARIO, value, ovalue);
        this.scenario = value;
    }

    /**
     * Requests that the <code>marquee</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMarquee (String value)
    {
        String ovalue = this.marquee;
        requestAttributeChange(
            MARQUEE, value, ovalue);
        this.marquee = value;
    }

    /**
     * Requests that the <code>bounty</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBounty (BountyConfig value)
    {
        BountyConfig ovalue = this.bounty;
        requestAttributeChange(
            BOUNTY, value, ovalue);
        this.bounty = value;
    }

    /**
     * Requests that the <code>bountyGameId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBountyGameId (String value)
    {
        String ovalue = this.bountyGameId;
        requestAttributeChange(
            BOUNTY_GAME_ID, value, ovalue);
        this.bountyGameId = value;
    }

    /**
     * Requests that the <code>boardHash</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBoardHash (byte[] value)
    {
        byte[] ovalue = this.boardHash;
        requestAttributeChange(
            BOARD_HASH, value, ovalue);
        this.boardHash = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>boardHash</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setBoardHashAt (byte value, int index)
    {
        byte ovalue = this.boardHash[index];
        requestElementUpdate(
            BOARD_HASH, index, Byte.valueOf(value), Byte.valueOf(ovalue));
        this.boardHash[index] = value;
    }

    /**
     * Requests that the <code>boardUpdates</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBoardUpdates (Piece[] value)
    {
        Piece[] ovalue = this.boardUpdates;
        requestAttributeChange(
            BOARD_UPDATES, value, ovalue);
        this.boardUpdates = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>boardUpdates</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setBoardUpdatesAt (Piece value, int index)
    {
        Piece ovalue = this.boardUpdates[index];
        requestElementUpdate(
            BOARD_UPDATES, index, value, ovalue);
        this.boardUpdates[index] = value;
    }

    /**
     * Requests that the <code>startPositions</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setStartPositions (StreamablePoint[] value)
    {
        StreamablePoint[] ovalue = this.startPositions;
        requestAttributeChange(
            START_POSITIONS, value, ovalue);
        this.startPositions = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>startPositions</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setStartPositionsAt (StreamablePoint value, int index)
    {
        StreamablePoint ovalue = this.startPositions[index];
        requestElementUpdate(
            START_POSITIONS, index, value, ovalue);
        this.startPositions[index] = value;
    }

    /**
     * Requests that the <code>bigShots</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBigShots (Unit[] value)
    {
        Unit[] ovalue = this.bigShots;
        requestAttributeChange(
            BIG_SHOTS, value, ovalue);
        this.bigShots = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>bigShots</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setBigShotsAt (Unit value, int index)
    {
        Unit ovalue = this.bigShots[index];
        requestElementUpdate(
            BIG_SHOTS, index, value, ovalue);
        this.bigShots[index] = value;
    }

    /**
     * Requests that the <code>tick</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTick (short value)
    {
        short ovalue = this.tick;
        requestAttributeChange(
            TICK, Short.valueOf(value), Short.valueOf(ovalue));
        this.tick = value;
    }

    /**
     * Requests that the <code>lastTick</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLastTick (short value)
    {
        short ovalue = this.lastTick;
        requestAttributeChange(
            LAST_TICK, Short.valueOf(value), Short.valueOf(ovalue));
        this.lastTick = value;
    }

    /**
     * Requests that the <code>duration</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setDuration (short value)
    {
        short ovalue = this.duration;
        requestAttributeChange(
            DURATION, Short.valueOf(value), Short.valueOf(ovalue));
        this.duration = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>pieces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPieces (Piece elem)
    {
        requestEntryAdd(PIECES, pieces, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>pieces</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPieces (Comparable<?> key)
    {
        requestEntryRemove(PIECES, pieces, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>pieces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePieces (Piece elem)
    {
        requestEntryUpdate(PIECES, pieces, elem);
    }

    /**
     * Requests that the <code>pieces</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPieces (ModifiableDSet<Piece> value)
    {
        requestAttributeChange(PIECES, value, this.pieces);
        ModifiableDSet<Piece> clone = (value == null) ? null : value.clone();
        this.pieces = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>debugPieces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToDebugPieces (Piece elem)
    {
        requestEntryAdd(DEBUG_PIECES, debugPieces, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>debugPieces</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromDebugPieces (Comparable<?> key)
    {
        requestEntryRemove(DEBUG_PIECES, debugPieces, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>debugPieces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateDebugPieces (Piece elem)
    {
        requestEntryUpdate(DEBUG_PIECES, debugPieces, elem);
    }

    /**
     * Requests that the <code>debugPieces</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setDebugPieces (ModifiableDSet<Piece> value)
    {
        requestAttributeChange(DEBUG_PIECES, value, this.debugPieces);
        ModifiableDSet<Piece> clone = (value == null) ? null : value.clone();
        this.debugPieces = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>cards</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToCards (Card elem)
    {
        requestEntryAdd(CARDS, cards, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>cards</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromCards (Comparable<?> key)
    {
        requestEntryRemove(CARDS, cards, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>cards</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateCards (Card elem)
    {
        requestEntryUpdate(CARDS, cards, elem);
    }

    /**
     * Requests that the <code>cards</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setCards (ModifiableDSet<Card> value)
    {
        requestAttributeChange(CARDS, value, this.cards);
        ModifiableDSet<Card> clone = (value == null) ? null : value.clone();
        this.cards = clone;
    }

    /**
     * Requests that the <code>effect</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setEffect (Effect value)
    {
        Effect ovalue = this.effect;
        requestAttributeChange(
            EFFECT, value, ovalue);
        this.effect = value;
    }

    /**
     * Requests that the <code>actionId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setActionId (int value)
    {
        int ovalue = this.actionId;
        requestAttributeChange(
            ACTION_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.actionId = value;
    }

    /**
     * Requests that the <code>points</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPoints (int[] value)
    {
        int[] ovalue = this.points;
        requestAttributeChange(
            POINTS, value, ovalue);
        this.points = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>points</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setPointsAt (int value, int index)
    {
        int ovalue = this.points[index];
        requestElementUpdate(
            POINTS, index, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.points[index] = value;
    }

    /**
     * Requests that the <code>perRoundPoints</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPerRoundPoints (int[][] value)
    {
        int[][] ovalue = this.perRoundPoints;
        requestAttributeChange(
            PER_ROUND_POINTS, value, ovalue);
        this.perRoundPoints = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>perRoundPoints</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setPerRoundPointsAt (int[] value, int index)
    {
        int[] ovalue = this.perRoundPoints[index];
        requestElementUpdate(
            PER_ROUND_POINTS, index, value, ovalue);
        this.perRoundPoints[index] = value;
    }

    /**
     * Requests that the <code>perRoundRanks</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPerRoundRanks (short[][] value)
    {
        short[][] ovalue = this.perRoundRanks;
        requestAttributeChange(
            PER_ROUND_RANKS, value, ovalue);
        this.perRoundRanks = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>perRoundRanks</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setPerRoundRanksAt (short[] value, int index)
    {
        short[] ovalue = this.perRoundRanks[index];
        requestElementUpdate(
            PER_ROUND_RANKS, index, value, ovalue);
        this.perRoundRanks[index] = value;
    }

    /**
     * Requests that the <code>awards</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAwards (Award[] value)
    {
        Award[] ovalue = this.awards;
        requestAttributeChange(
            AWARDS, value, ovalue);
        this.awards = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>awards</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setAwardsAt (Award value, int index)
    {
        Award ovalue = this.awards[index];
        requestElementUpdate(
            AWARDS, index, value, ovalue);
        this.awards[index] = value;
    }
    // AUTO-GENERATED: METHODS END

    /** Maps encoded tile coordinates to pieces of track on the board. */
    protected transient HashIntMap<Track> _tracks;
    protected transient byte[] _trackBoardHash;

    /** Maps encoded tile coordinates to teleporters on the board. */
    protected transient HashIntMap<Teleporter> _teleporters;
    protected transient byte[] _teleporterBoardHash;
}
