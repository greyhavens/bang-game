//
// $Id$

package com.threerings.bang.game.data;

import java.util.ArrayList;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;
import com.threerings.parlor.game.data.GameObject;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.Stat;
import com.threerings.bang.data.StatSet;

import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
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
        public double averagePower;

        /** The number of unclaimed bonuses on the board. */
        public int bonuses;

        /** Clears our accumulator stats in preparation for a recompute. */
        public void clear () {
            livePlayers = 0;
            totalPower = 0;
            bonuses = 0;
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
        public double powerFactor;

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

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>stats</code> field. */
    public static final String STATS = "stats";

    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>townId</code> field. */
    public static final String TOWN_ID = "townId";

    /** The field name of the <code>scenarioId</code> field. */
    public static final String SCENARIO_ID = "scenarioId";

    /** The field name of the <code>boardName</code> field. */
    public static final String BOARD_NAME = "boardName";

    /** The field name of the <code>board</code> field. */
    public static final String BOARD = "board";

    /** The field name of the <code>bigShots</code> field. */
    public static final String BIG_SHOTS = "bigShots";

    /** The field name of the <code>tick</code> field. */
    public static final String TICK = "tick";

    /** The field name of the <code>pieces</code> field. */
    public static final String PIECES = "pieces";

    /** The field name of the <code>cards</code> field. */
    public static final String CARDS = "cards";

    /** The field name of the <code>effect</code> field. */
    public static final String EFFECT = "effect";

    /** The field name of the <code>funds</code> field. */
    public static final String FUNDS = "funds";

    /** The field name of the <code>badges</code> field. */
    public static final String BADGES = "badges";

    /** The field name of the <code>badgeCounts</code> field. */
    public static final String BADGE_COUNTS = "badgeCounts";
    // AUTO-GENERATED: FIELDS END

    /** A {@link #state} constant indicating the pre-game selection phase. */
    public static final int SELECT_PHASE = 4;

    /** A {@link #state} constant indicating the pre-game buying phase. */
    public static final int BUYING_PHASE = 5;

    /** A {@link #state} constant indicating the post-round phase. */
    public static final int POST_ROUND = 6;

    /** Contains statistics on the game, updated every time any change is
     * made to pertinent game state. */
    public transient GameData gdata = new GameData();

    /** Contains statistics on each player, updated every time any change
     * is made to pertinent game state. */
    public transient PlayerData[] pdata;

    /** This value is set at the end of every round, to inform the players
     * of various interesting statistics. */
    public StatSet[] stats;

    /** The invocation service via which the client communicates with the
     * server. */
    public BangMarshaller service;

    /** The id of the town in which this game is being played. */
    public String townId;

    /** The identifier for the current scenario. */
    public String scenarioId;

    /** The name of the current board. */
    public String boardName;

    /** Contains the representation of the game board. */
    public BangBoard board;

    /** The big shots selected for use by each player. */
    public Unit[] bigShots;

    /** The curent board tick count. */
    public short tick;

    /** Contains information on all pieces on the board. */
    public PieceDSet pieces;

    /** Contains information on all available cards. */
    public DSet cards = new DSet();

    /** A field we use to broadcast applied effects. */
    public Effect effect;

    /** Total cash earned by each player. */
    public int[] funds;

    /** Used to report badges awarded at the end of the game. */
    public Badge[] badges;

    /** Indicates how many badges were earned by each player. This is used
     * to decode {@link #badges}. */
    public int[] badgeCounts;

    /** Returns the {@link #pieces} set as an array to allow for
     * simultaneous iteration and removal. */
    public Piece[] getPieceArray ()
    {
        return (Piece[])pieces.toArray(new Piece[pieces.size()]);
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
        board.updateShadow(null, piece);
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
        board.updateShadow(piece, null);
    }

    /**
     * Returns a list of pieces that overlap the specified piece given its
     * (hypothetical) current coordinates. If no pieces overlap, null will
     * be returned.
     */
    public ArrayList<Piece> getOverlappers (Piece piece)
    {
        return PieceUtil.getOverlappers(pieces.iterator(), piece);
    }

    /**
     * Returns true if the specified player has live pieces, false if they
     * are totally knocked out.
     */
    public boolean hasLiveUnits (int pidx)
    {
        return countLiveUnits(pidx) > 0;
    }

    /**
     * Returns the number of live units remaining for the specified
     * player.
     */
    public int countLiveUnits (int pidx)
    {
        int pcount = 0;
        if (pieces != null) {
            for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
                Piece p = (Piece)iter.next();
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
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.owner >= 0 && p instanceof Unit && !p.isAlive()) {
                pcount++;
            }
        }
        return pcount;
    }

    /**
     * Returns the player piece at the specified coordinates or null if no
     * owned piece exists at those coordinates.
     */
    public Piece getPlayerPiece (int tx, int ty)
    {
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.owner >= 0 && p.x == tx && p.y == ty) {
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
        for (int ii = 0; ii < pcount.length; ii++) {
            if (pcount[ii] > 0) {
                tunits += pcount[ii];
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
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p instanceof Unit && p.isAlive() && p.owner >= 0) {
                pcount[p.owner]++;
            }
        }
        return pcount;
    }

    /**
     * Returns the average power of the specified set of players
     * (referenced by index).
     */
    public double getAveragePower (ArrayIntSet players)
    {
        double tpower = 0;
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
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
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
            if (p instanceof Bonus) {
                gdata.bonuses++;
            } else if (p.isAlive() && p.owner >= 0) {
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

        gdata.averagePower = (double)gdata.totalPower / gdata.livePlayers;
        for (int ii = 0; ii < pdata.length; ii++) {
            pdata[ii].powerFactor =
                (double)pdata[ii].power / gdata.averagePower;
        }

//         log.info("Updated stats " + gdata + ": " +
//                  StringUtil.toString(pdata));
    }

    /**
     * Grants the specified amount of cash to the specified player,
     * updating their {@link #funds} and updating the appropriate earned
     * cash statistic.
     */
    public void grantCash (int pidx, int amount)
    {
        setFundsAt(funds[pidx] + amount, pidx);
        stats[pidx].incrementStat(Stat.Type.CASH_EARNED, amount);
    }

    // AUTO-GENERATED: METHODS START
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
     * Requests that the <code>scenarioId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setScenarioId (String value)
    {
        String ovalue = this.scenarioId;
        requestAttributeChange(
            SCENARIO_ID, value, ovalue);
        this.scenarioId = value;
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
     * Requests that the <code>board</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBoard (BangBoard value)
    {
        BangBoard ovalue = this.board;
        requestAttributeChange(
            BOARD, value, ovalue);
        this.board = value;
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
            TICK, new Short(value), new Short(ovalue));
        this.tick = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>pieces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPieces (DSet.Entry elem)
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
    public void updatePieces (DSet.Entry elem)
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
    public void setPieces (PieceDSet value)
    {
        requestAttributeChange(PIECES, value, this.pieces);
        this.pieces = (value == null) ? null : (PieceDSet)value.clone();
    }

    /**
     * Requests that the specified entry be added to the
     * <code>cards</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToCards (DSet.Entry elem)
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
    public void updateCards (DSet.Entry elem)
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
    public void setCards (DSet value)
    {
        requestAttributeChange(CARDS, value, this.cards);
        this.cards = (value == null) ? null : (DSet)value.clone();
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
     * Requests that the <code>funds</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFunds (int[] value)
    {
        int[] ovalue = this.funds;
        requestAttributeChange(
            FUNDS, value, ovalue);
        this.funds = (value == null) ? null : (int[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>funds</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setFundsAt (int value, int index)
    {
        int ovalue = this.funds[index];
        requestElementUpdate(
            FUNDS, index, new Integer(value), new Integer(ovalue));
        this.funds[index] = value;
    }

    /**
     * Requests that the <code>badges</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBadges (Badge[] value)
    {
        Badge[] ovalue = this.badges;
        requestAttributeChange(
            BADGES, value, ovalue);
        this.badges = (value == null) ? null : (Badge[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>badges</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setBadgesAt (Badge value, int index)
    {
        Badge ovalue = this.badges[index];
        requestElementUpdate(
            BADGES, index, value, ovalue);
        this.badges[index] = value;
    }

    /**
     * Requests that the <code>badgeCounts</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBadgeCounts (int[] value)
    {
        int[] ovalue = this.badgeCounts;
        requestAttributeChange(
            BADGE_COUNTS, value, ovalue);
        this.badgeCounts = (value == null) ? null : (int[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>badgeCounts</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setBadgeCountsAt (int value, int index)
    {
        int ovalue = this.badgeCounts[index];
        requestElementUpdate(
            BADGE_COUNTS, index, new Integer(value), new Integer(ovalue));
        this.badgeCounts[index] = value;
    }
    // AUTO-GENERATED: METHODS END
}
