//
// $Id$

package com.threerings.bang.data;

import java.util.ArrayList;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.dobj.DSet;
import com.threerings.parlor.game.data.GameObject;

import com.threerings.bang.data.effect.Effect;
import com.threerings.bang.data.piece.Bonus;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.PieceSet;
import com.threerings.bang.util.PieceUtil;

import static com.threerings.bang.Log.log;

/**
 * Contains all distributed information for the game.
 */
public class BangObject extends GameObject
{
    /** Used to track statistics on each player. */
    public static class PlayerData
    {
        /** The number of still-alive pieces controlled by this player. */
        public int livePieces;

        /** The total power (un-damage) controlled by this player. */
        public int power;

        /** This player's power divided by the average power. */
        public double powerFactor;

        /** Clears our accumulator stats in preparation for a recompute. */
        public void clear () {
            livePieces = 0;
            power = 0;
        }

        /** Generates a string representation of this instance. */
        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /** Used to track statistics on the overall game. */
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

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>service</code> field. */
    public static final String SERVICE = "service";

    /** The field name of the <code>tick</code> field. */
    public static final String TICK = "tick";

    /** The field name of the <code>board</code> field. */
    public static final String BOARD = "board";

    /** The field name of the <code>pieces</code> field. */
    public static final String PIECES = "pieces";

    /** The field name of the <code>surprises</code> field. */
    public static final String SURPRISES = "surprises";

    /** The field name of the <code>effect</code> field. */
    public static final String EFFECT = "effect";

    /** The field name of the <code>points</code> field. */
    public static final String POINTS = "points";

    /** The field name of the <code>funds</code> field. */
    public static final String FUNDS = "funds";

    /** The field name of the <code>reserves</code> field. */
    public static final String RESERVES = "reserves";
    // AUTO-GENERATED: FIELDS END

    /** A {@link #state} constant indicating the pre-game buying phase. */
    public static final int PRE_ROUND = 4;

    /** A {@link #state} constant indicating the post-round phase. */
    public static final int POST_ROUND = 5;

    /** Contains statistics on the game, updated every time any change is
     * made to pertinent game state. */
    public transient GameData gstats = new GameData();

    /** Contains statistics on each player, updated every time any change
     * is made to pertinent game state. */
    public transient PlayerData[] pstats;

    /** The invocation service via which the client communicates with the
     * server. */
    public BangMarshaller service;

    /** The curent board tick count. */
    public short tick;

    /** Contains the representation of the game board. */
    public BangBoard board;

    /** Contains information on all pieces on the board. */
    public PieceDSet pieces;

    /** Contains information on all available surprises. */
    public DSet surprises = new DSet();

    /** A field we use to broadcast applied effects. */
    public Effect effect;

    /** Points earned by each player. */
    public int[] points;

    /** Cash earned by each player this round. */
    public int[] funds;

    /** Cash held by each player at the start of the round. */
    public int[] reserves;

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
    public boolean hasLivePieces (int pidx)
    {
        return countLivePieces(pidx) > 0;
    }

    /**
     * Returns the number of live pieces remaining for the specified
     * player.
     */
    public int countLivePieces (int pidx)
    {
        int pcount = 0;
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.owner == pidx && p.isAlive()) {
                pcount++;
            }
        }
        return pcount;
    }

    /**
     * Returns the number of dead pieces on the board.
     */
    public int countDeadPieces ()
    {
        int pcount = 0;
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.owner >= 0 && !p.isAlive()) {
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
     * Returns the average number of live pieces per player.
     */
    public int getAveragePieceCount ()
    {
        int[] pcount = getPieceCount();
        float tpieces = 0, tcount = 0;
        for (int ii = 0; ii < pcount.length; ii++) {
            if (pcount[ii] > 0) {
                tpieces += pcount[ii];
                tcount++;
            }
        }
        return (int)Math.round(tpieces / tcount);
    }

    /**
     * Returns the average number of live pieces among the specified set
     * of players.
     */
    public int getAveragePieceCount (ArrayIntSet players)
    {
        int[] pcount = getPieceCount();
        float tpieces = 0, tcount = 0;
        for (int ii = 0; ii < pcount.length; ii++) {
            if (pcount[ii] > 0 && players.contains(ii)) {
                tpieces += pcount[ii];
                tcount++;
            }
        }
        return (int)Math.round(tpieces / tcount);
    }

    /**
     * Returns the count of pieces per player.
     */
    public int[] getPieceCount ()
    {
        int[] pcount = new int[players.length];
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.isAlive() && p.owner >= 0) {
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
            tpower += pstats[players.get(ii)].power;
        }
        return tpower/players.size();
    }

    /**
     * Returns the average damage level of all live pieces owned by the
     * specified players.
     */
    public int getAveragePieceDamage (ArrayIntSet players)
    {
        int pcount = 0, tdamage = 0;
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.isAlive() && players.contains(p.owner)) {
                pcount++;
                tdamage += p.damage;
            }
        }
        return tdamage / pcount;
    }

    /**
     * Updates the {@link #gstats} and {@link #pstats} information.
     */
    public void updateStats ()
    {
        // don't do any computation on the client
        if (pstats == null) {
            return;
        }

        // first clear out the old stats
        gstats.clear();
        for (int ii = 0; ii < pstats.length; ii++) {
            pstats[ii].clear();
        }

        Piece[] pieces = getPieceArray();
        int pcount = players.length;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p instanceof Bonus) {
                gstats.bonuses++;
            } else if (p.isAlive() && p.owner >= 0) {
                pstats[p.owner].livePieces++;
                int pp = (100 - p.damage);
                pstats[p.owner].power += pp;
                gstats.totalPower += pp;
//                 if (p.ticksUntilMovable(prevTick) == 0) {
//                     nonactors[p.owner]++;
//                 }
            }
        }

        for (int ii = 0; ii < pstats.length; ii++) {
            if (pstats[ii].livePieces > 0) {
                gstats.livePlayers++;
            }
        }

        gstats.averagePower = (double)gstats.totalPower / gstats.livePlayers;
        for (int ii = 0; ii < pstats.length; ii++) {
            pstats[ii].powerFactor =
                (double)pstats[ii].power / gstats.averagePower;
        }

        log.info("Updated stats " + gstats + ": " +
                 StringUtil.toString(pstats));
    }

    // AUTO-GENERATED: METHODS START
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
     * <code>surprises</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToSurprises (DSet.Entry elem)
    {
        requestEntryAdd(SURPRISES, surprises, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>surprises</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromSurprises (Comparable key)
    {
        requestEntryRemove(SURPRISES, surprises, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>surprises</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateSurprises (DSet.Entry elem)
    {
        requestEntryUpdate(SURPRISES, surprises, elem);
    }

    /**
     * Requests that the <code>surprises</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setSurprises (DSet value)
    {
        requestAttributeChange(SURPRISES, value, this.surprises);
        this.surprises = (value == null) ? null : (DSet)value.clone();
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
            POINTS, index, new Integer(value), new Integer(ovalue));
        this.points[index] = value;
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
     * Requests that the <code>reserves</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setReserves (int[] value)
    {
        int[] ovalue = this.reserves;
        requestAttributeChange(
            RESERVES, value, ovalue);
        this.reserves = (value == null) ? null : (int[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>reserves</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setReservesAt (int value, int index)
    {
        int ovalue = this.reserves[index];
        requestElementUpdate(
            RESERVES, index, new Integer(value), new Integer(ovalue));
        this.reserves[index] = value;
    }
    // AUTO-GENERATED: METHODS END
}
