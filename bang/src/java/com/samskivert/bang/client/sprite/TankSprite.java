//
// $Id$

package com.samskivert.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;

import com.samskivert.bang.data.piece.Tank;
import com.samskivert.bang.data.piece.Piece;

import static com.samskivert.bang.client.BangMetrics.*;

/**
 * Displays a tank piece.
 */
public class TankSprite extends PieceSprite
{
    @Override // documentation inherited
    public boolean isSelectable ()
    {
        return true;
    }

    @Override // documentation inherited
    public void paint (Graphics2D gfx)
    {
        gfx.setColor(_piece.owner == 0 ? Color.white : Color.yellow);
        gfx.fill(_bounds);

        int dx = SQUARE/2, dy = SQUARE/2;
        switch (_piece.orientation) {
        case Piece.NORTH: dy = 2; break;
        case Piece.SOUTH: dy = SQUARE-4; break;
        case Piece.WEST: dx = 2; break;
        case Piece.EAST: dx = SQUARE-4; break;
        }

        gfx.setColor(Color.black);
        gfx.drawLine(_bounds.x + SQUARE/2, _bounds.y + SQUARE/2,
                     _bounds.x + dx, _bounds.y + dy);

        if (_piece.hasPath) {
            gfx.setColor(Color.blue);
            gfx.drawRect(_bounds.x, _bounds.y,
                         _bounds.width-1, _bounds.height-1);
        } else if (_selected) {
            gfx.setColor(Color.green);
            gfx.drawRect(_bounds.x, _bounds.y,
                         _bounds.width-1, _bounds.height-1);
        }

        paintEnergy(gfx);
    }

    @Override // documentation inherited
    public int getRenderOrder ()
    {
        return 5;
    }
}
