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
    // AUTO-GENERATED: FIELDS END

    /** The invocation service via which the client communicates with the
     * server. */
    public BangMarshaller service;

    /** The curent board tick count. */
    public short tick;

    /** Contains the representation of the game board. */
    public BangBoard board;

    /** Contains information on all pieces on the board. */
    public DSet pieces;

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

    /**
     * Applies a single shot.
     */
    public void applyShot (Shot shot)
    {
        Piece piece = (Piece)pieces.get(shot.targetId);
        if (piece != null) {
//             log.info("Applying " + shot.damage + " to " + piece + ".");
            piece.damage = Math.min(100, piece.damage + shot.damage);
        }
    }

    /**
     * Applies the supplied board effects.
     */
    public void applyEffects (Effect[] effects, ArrayList<Piece> additions,
                              PieceSet removals)
    {
        for (int ii = 0; ii < effects.length; ii++) {
            Effect effect = effects[ii];
            effect.apply(this, additions, removals);
        }
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
    public void setPieces (DSet value)
    {
        requestAttributeChange(PIECES, value, this.pieces);
        this.pieces = (value == null) ? null : (DSet)value.clone();
    }
    // AUTO-GENERATED: METHODS END
}
