//
// $Id$

package com.samskivert.bang.data;

import java.util.ArrayList;
import java.util.Iterator;

import com.threerings.presents.dobj.DSet;
import com.threerings.parlor.game.data.GameObject;

import com.samskivert.bang.data.effect.Effect;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.util.PieceSet;
import com.samskivert.bang.util.PieceUtil;

import static com.samskivert.bang.Log.log;

/**
 * Contains all distributed information for the game.
 */
public class BangObject extends GameObject
{
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

    /** The field name of the <code>funds</code> field. */
    public static final String FUNDS = "funds";

    /** The field name of the <code>reserves</code> field. */
    public static final String RESERVES = "reserves";
    // AUTO-GENERATED: FIELDS END

    /** A {@link #state} constant indicating the pre-game buying phase. */
    public static final int PRE_GAME = 4;

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
    public DSet surprises;

    /** A field we use to broadcast applied effects. */
    public Effect effect;

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
        for (Iterator iter = pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.owner == pidx && p.isAlive()) {
                return true;
            }
        }
        return false;
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
