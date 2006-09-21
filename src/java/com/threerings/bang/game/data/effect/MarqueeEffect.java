//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;

/**
 * An effect that displays a message on a marquee over the board view.
 */
public class MarqueeEffect extends Effect
{
    /** The translatable message to display on the marquee. */
    public String message;
    
    public MarqueeEffect (String message)
    {
        this.message = message;
    }
    
    public MarqueeEffect ()
    {
    }
    
    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return NO_PIECES;   
    }
    
    @Override // documentation inherited
    public Rectangle getBounds (BangObject bangobj)
    {
        // wait for and hold everything while the marquee is shown
        return bangobj.board.getPlayableArea();
    }
    
    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // no-op
    }

    // documentation inherited    
    public boolean apply (BangObject bangobj, Observer observer)
    {
        affectBoard(bangobj, message, false, observer);
        return true;
    }
}
