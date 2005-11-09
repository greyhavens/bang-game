//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;

/**
 * A train car or engine.
 */
public class Train extends Piece
{
    /** The id of the piece to which this is attached, or 0 for the engine. */
    public int attachId;
    
    /** The last occupied position of the piece. */
    public transient int lastX, lastY;
    
    @Override // documentation inherited
    public boolean tick (short tick, BangBoard board, Piece[] pieces)
    {
        // store the last occupied position
        lastX = x;
        lastY = y;
            
        if (attachId == 0) {
            // engines move according to the tracks in front of them
            ArrayList<Track> tracks = new ArrayList<Track>();
            for (int i = 0; i < pieces.length; i++) {
                if (pieces[i] instanceof Track) {
                    Track track = (Track)pieces[i];
                    if (getDistance(track) == 1 && !isBehind(track)) {
                        tracks.add(track);
                    }
                }
            }
            
            // if there's nowhere to go, disappear; otherwise, move to a random
            // piece of track
            if (tracks.size() == 0) {
                damage = 100;
                
            } else {
                Track track = (Track)RandomUtil.pickRandom(tracks);
                board.updateShadow(this, null);
                position(track.x, track.y);
                board.updateShadow(null, this);
            }
            
        } else {
            // cars move according to the cars in front of them
            Train attached = null;
            for (int i = 0; i < pieces.length; i++) {
                if (pieces[i].pieceId == attachId) {
                    attached = (Train)pieces[i];
                    break;
                }
            }
            
            // if the car in front is gone, destroy this one; otherwise, move
            // to the last position of the car in front
            if (attached == null) {
                damage = 100;
            
            } else {
                board.updateShadow(this, null);
                if (attached.lastActed == tick) {
                    position(attached.lastX, attached.lastY);
                
                } else {
                    position(attached.x, attached.y);
                }
                board.updateShadow(null, this);
            }
        }
        
        lastActed = tick;
        return true;
    }
    
    /**
     * Determines whether the specified piece is behind this one.
     */
    public boolean isBehind (Piece piece)
    {
        return piece.x == (x + REV_X_MAP[orientation]) &&
            piece.y == (y + REV_Y_MAP[orientation]);
    }
    
    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return true;
    }
    
    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return !lapper.isFlyer();
    }
    
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new MobileSprite("extras", attachId == 0 ? "bison" : "cow");
    }
}
