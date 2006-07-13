//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.List;

import com.jme.math.Vector3f;

import com.threerings.bang.game.client.WendigoHandler;

import com.threerings.bang.game.data.BangBoard;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the wendigo.
 */
public class WendigoSprite extends MobileSprite
{
    public WendigoSprite ()
    {
        super("extras", "indian_post/wendigo");
    }

    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        return "wendigo";
    }

    @Override // documentation inherited
    public void centerWorldCoords (Vector3f coords)
    {
        // Since the wendigo takes up a 2x2 tile space, center it to the far
        // corner of it's occupying tile
        coords.x += TILE_SIZE;
        coords.y += TILE_SIZE;
    }

    /**
     * Perform a move with an associate penderId for the WendigoHandler.
     */
    public void move (BangBoard board, List path, float speed,
            WendigoHandler handler)
    {
        _handler = handler;
        move(board, path, speed);
    }
    
    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();
        _handler.pathCompleted();
    }

    protected WendigoHandler _handler;
}
