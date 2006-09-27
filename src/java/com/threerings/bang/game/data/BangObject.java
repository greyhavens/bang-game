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
import com.threerings.parlor.game.data.GameObject;
import com.threerings.util.StreamablePoint;

import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;

import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.util.PieceUtil;

import static com.threerings.bang.Log.log;

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

    /** Used to keep track of player occupant information even if they're not
     * in the room. */
    public static class PlayerInfo implements Streamable
    {
        /** The player's unique identifier. */
        public int playerId = -1;

        /** The player's avatar data. */
        public int[] avatar;
    }

    /** Used to keep track of where players were before the game. */
    public static class PriorLocation implements Streamable
    {
        /** Either "saloon", "ranch", "parlor" or "tutorial". */
        public String ident;

        /** If ident is "parlor" the room oid of the parlor otherwise zero. */
        public int placeOid;

        /** Default constructor used for unserialization. */
        public PriorLocation () {
        }

        /** Creates a prior location representing an actual location. */
        public PriorLocation (String ident, int placeOid) {
            this.ident = ident;
            this.placeOid = placeOid;
        }
    }

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>playerInfo</code> field. */
    public static final String PLAYER_INFO = "playerInfo";

    /** The field name of the <code>priorLocation</code> field. */
    public static final String PRIOR_LOCATION = "priorLocation";

    /** The field name of the <code>stats</code> field. */
    public static final String STATS = "stats";

    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>townId</code> field. */
    public static final String TOWN_ID = "townId";

    /** The field name of the <code>scenario</code> field. */
    public static final String SCENARIO = "scenario";

    /** The field name of the <code>boardName</code> field. */
    public static final String BOARD_NAME = "boardName";

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

    /** The field name of the <code>awards</code> field. */
    public static final String AWARDS = "awards";
    // AUTO-GENERATED: FIELDS END

    /** A {@link #state} constant used by the tutorial scenario. */
    public static final int PRE_TUTORIAL = 4;

    /** A {@link #state} constant indicating the pre-game selection phase. */
    public static final int SELECT_PHASE = 5;

    /** A {@link #state} constant indicating the pre-game buying phase. */
    public static final int BUYING_PHASE = 6;

    /** A {@link #state} constant indicating the post-round phase. */
    public static final int POST_ROUND = 7;

    /** A {@link #state} constant used by the practice scenario. */
    public static final int PRE_PRACTICE = 8;

    /** A {@link #playerStatus} constant used before the game starts. */
    public static final int PLAYER_PREPARING = 2;

    /** Contains the representation of the game board. */
    public transient BangBoard board;

    /** Contains statistics on the game, updated every time any change is
     * made to pertinent game state. */
    public transient GameData gdata = new GameData();

    /** Contains statistics on each player, updated every time any change
     * is made to pertinent game state. */
    public transient PlayerData[] pdata;

    /** Used to assign ids to pieces added during the game. */
    public transient int maxPieceId;

    /** Identifies an effect applied to the entire board. */
    public transient String boardEffect;

    /** A hindrance affecting all units (and applied to new units). */
    public transient Hindrance globalHindrance;

    /** Avatar fingerprints and other data for each of the players in the game.
     * We need these in case the player leaves early and so that we can provide
     * fake info for AIs. */
    public PlayerInfo[] playerInfo;

    /** Contains the prior location of the players in this game. Defaults to
     * the saloon. */
    public PriorLocation priorLocation;

    /** This value is set at the end of every round, to inform the players
     * of various interesting statistics. */
    public StatSet[] stats;

    /** The invocation service via which the client communicates with the
     * server. */
    public BangMarshaller service;

    /** The id of the town in which this game is being played. */
    public String townId;

    /** The metadata for the current scenario. */
    public ScenarioInfo scenario;

    /** The name of the current board. */
    public String boardName;

    /** The MD5 hash of the game board, to be compared against any cached
     * version of the board stored on the client. */
    public byte[] boardHash;

    /** A list of round-specific updates to be applied to the board after
     * downloading it or loading it from the cache. */
    public Piece[] boardUpdates;
    
    /** The starting positions for each player. */
    public StreamablePoint[] startPositions;

    /** The big shots selected for use by each player. */
    public Unit[] bigShots;

    /** The current board tick count. */
    public short tick;

    /** The tick after which the game will end. This may not be {@link
     * #duration} - 1 because some scenarios may opt to end the game early. */
    public short lastTick;

    /** The maximum number of ticks that will be allowed to elapse before the
     * game is ended. Some scenarios may choose to end the game early (see
     * {@link #lastTick}). */
    public short duration;

    /** Contains information on all pieces on the board. */
    public ModifiableDSet<Piece> pieces;

    /** Contains information on all available cards. */
    public ModifiableDSet<Card> cards = new ModifiableDSet<Card>();

    /** A field we use to broadcast applied effects. */
    public Effect effect;

    /** The currently executing action (only used in the tutorial). */
    public int actionId;

    /** Total points earned by each player. */
    public int[] points;

    /** Points earned per player per round, this is only broadcast to the
     * client at the end of the game. */
    public int[][] perRoundPoints;

    /** Used to report cash and badges awarded at the end of the game. */
    public Award[] awards;

    /** Returns the {@link #pieces} set as an array to allow for
     * simultaneous iteration and removal. */
    public Piece[] getPieceArray ()
    {
        return pieces.toArray(new Piece[pieces.size()]);
    }

    /**
     * Adds a piece directly to the piece set without broadcasting any
     * distributed object events. This is used by entities that are known
     * to run on both the client and server. The board's piece shadow is
     * also updated by this call.
     */
    public void addPieceDirect (Piece piece)
    {
        pieces.addDirect(piece);
        board.shadowPiece(piece);
    }

    /**
     * Removes a piece directly from the piece set without broadcasting
     * any distributed object events. This is used by entities that are
     * known to run on both the client and server. The board's piece
     * shadow is also updated by this call.
     */
    public void removePieceDirect (Piece piece)
    {
        pieces.removeDirect(piece);
        board.clearShadow(piece);
    }

    /**
     * Returns a list of pieces that overlap the specified piece given its
     * (hypothetical) current coordinates. If no pieces overlap, null will
     * be returned.
     */
    public ArrayList<Piece> getOverlappers (Piece piece)
    {
        return PieceUtil.getOverlappers(pieces, piece);
    }

    /**
     * Returns true if the specified player has live pieces, false if they
     * are totally knocked out.
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
     * Returns the number of live units remaining for the specified
     * player.
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
     * Returns the targetable piece at the specified coordinates or null if no
     * targetable piece exists at those coordinates.
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
        return (int)Math.round(tunits / tcount);
    }

    /**
     * Returns the average number of live units among the specified set
     * of players.
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
        return (int)Math.round(tunits / tcount);
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
     * Returns the average power of the specified set of players
     * (referenced by index).
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
     * Returns the average damage level of all live units owned by the
     * specified players.
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

        Piece[] pieces = getPieceArray();
        int pcount = players.length;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
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
            pdata[ii].powerFactor =
                (float)pdata[ii].power / gdata.averagePower;
        }

//         log.info("Updated stats " + gdata + ": " +
//                  StringUtil.toString(pdata));
    }

    /**
     * Grants the specified number of bonus points to the specified player.
     * Their total points will be updated by a call to {@link #grantPoints}.
     */
    public void grantBonusPoints (int pidx, int amount)
    {
        stats[pidx].incrementStat(Stat.Type.BONUS_POINTS, amount);
        grantPoints(pidx, amount);
    }

    /**
     * Grants the specified number of points to the specified player, updating
     * their {@link #points} and updating the appropriate earned points
     * statistic.
     */
    public void grantPoints (int pidx, int amount)
    {
        setPointsAt(points[pidx] + amount, pidx);
        perRoundPoints[roundId-1][pidx] += amount;
        stats[pidx].incrementStat(Stat.Type.POINTS_EARNED, amount);

        // keep our point factors up to date (on the server)
        if (pdata != null) {
            float tscore = IntListUtil.sum(points);
            pdata[pidx].pointFactor =
                (tscore == 0) ? 1f : (points[pidx] / tscore);
        }
    }

    /**
     * Returns an adjusted points array where players that have resigned from
     * the game are adjusted to zero.
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
     * Returns the current index into any round-related array. Because the
     * {@link #roundId} field is updated at slightly wonky times, this method
     * has to do some figuring out to return the proper index.
     */
    public int getRoundIndex ()
    {
        return roundId - (state == IN_PLAY || 
                          state == POST_ROUND || 
                          state == GAME_OVER ? 1 : 0);
    }

    /**
     * Returns a lazily computed mapping from encoded tile coordinates to
     * pieces of track on the board.
     */
    public HashIntMap<Track> getTracks ()
    {
        if (_trackBoardHash == null || 
                !Arrays.equals(_trackBoardHash, boardHash)) {
            _trackBoardHash = (byte[])boardHash.clone();
            _tracks = new HashIntMap<Track>();
            for (Piece piece : pieces) {
                if (piece instanceof Track) {
                    _tracks.put(piece.getCoord(), (Track)piece);
                }
            }
        }
        return _tracks;
    }

    @Override // documentation inherited
    public boolean isInPlay ()
    {
        return (state == BUYING_PHASE || state == SELECT_PHASE ||
                state == IN_PLAY || state == POST_ROUND);
    }

    /**
     * Returns true only while the game is in the actual moving and shooting
     * mode, not during round set up or after the round ends.
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

    @Override // documentation inherited
    protected boolean isActivePlayerStatus (int playerStatus)
    {
        return super.isActivePlayerStatus(playerStatus) ||
            (playerStatus == PLAYER_PREPARING);
    }

    // AUTO-GENERATED: METHODS START
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
        this.playerInfo = (value == null) ? null : (BangObject.PlayerInfo[])value.clone();
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
     * Requests that the <code>priorLocation</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPriorLocation (BangObject.PriorLocation value)
    {
        BangObject.PriorLocation ovalue = this.priorLocation;
        requestAttributeChange(
            PRIOR_LOCATION, value, ovalue);
        this.priorLocation = value;
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
        this.stats = (value == null) ? null : (StatSet[])value.clone();
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
     * Requests that the <code>boardName</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBoardName (String value)
    {
        String ovalue = this.boardName;
        requestAttributeChange(
            BOARD_NAME, value, ovalue);
        this.boardName = value;
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
        this.boardHash = (value == null) ? null : (byte[])value.clone();
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
        this.boardUpdates = (value == null) ? null : (Piece[])value.clone();
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
        this.startPositions = (value == null) ? null : (StreamablePoint[])value.clone();
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
        this.bigShots = (value == null) ? null : (Unit[])value.clone();
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
    public void removeFromPieces (Comparable key)
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
    public void setPieces (ModifiableDSet<com.threerings.bang.game.data.piece.Piece> value)
    {
        requestAttributeChange(PIECES, value, this.pieces);
        @SuppressWarnings("unchecked") ModifiableDSet<com.threerings.bang.game.data.piece.Piece> clone =
            (value == null) ? null : (ModifiableDSet<com.threerings.bang.game.data.piece.Piece>)value.clone();
        this.pieces = clone;
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
    public void removeFromCards (Comparable key)
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
    public void setCards (ModifiableDSet<com.threerings.bang.game.data.card.Card> value)
    {
        requestAttributeChange(CARDS, value, this.cards);
        @SuppressWarnings("unchecked") ModifiableDSet<com.threerings.bang.game.data.card.Card> clone =
            (value == null) ? null : (ModifiableDSet<com.threerings.bang.game.data.card.Card>)value.clone();
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
        this.points = (value == null) ? null : (int[])value.clone();
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
        this.perRoundPoints = (value == null) ? null : (int[][])value.clone();
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
        this.awards = (value == null) ? null : (Award[])value.clone();
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
}
