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
public class TankSprite extends MobileSprite
{
    @Override // documentation inherited
    public boolean isSelectable ()
    {
        return true;
    }

    @Override // documentation inherited
    public void paint (Graphics2D gfx)
    {
        super.paint(gfx);

        gfx.setColor(_piece.owner == 0 ? Color.white : Color.yellow);

        // first draw a circle
        gfx.fillOval(_ox, _oy, _bounds.width, _bounds.height);

        // then draw a square back to communicate our orientation
        switch (_piece.orientation) {
        case Piece.NORTH:
            gfx.fillRect(_ox, _oy + _bounds.height/2,
                         _bounds.width, _bounds.height/2);
            break;
        case Piece.EAST:
            gfx.fillRect(_ox, _oy, _bounds.width/2, _bounds.height);
            break;
        case Piece.SOUTH:
            gfx.fillRect(_ox, _oy, _bounds.width, _bounds.height/2);
            break;
        case Piece.WEST:
            gfx.fillRect(_ox + _bounds.width/2, _oy,
                         _bounds.width/2, _bounds.height);
            break;
        }

        // draw the turret
        Tank tank = (Tank)_piece;
        int dx = _bounds.width/2, dy = _bounds.height/2;
        switch (tank.turretOrient) {
        case Piece.NORTH: dy = 0; break;
        case Piece.SOUTH: dy = _bounds.height; break;
        case Piece.WEST: dx = 0; break;
        case Piece.EAST: dx = _bounds.height; break;
        }

        gfx.setColor(Color.black);
        gfx.drawLine(_ox + _bounds.width/2, _oy + _bounds.height/2,
                     _ox + dx - 1, _oy + dy - 1);

        if (_piece.hasPath()) {
            gfx.setColor(Color.blue);
            gfx.drawRect(_ox, _oy, _bounds.width-1, _bounds.height-1);
        } else if (_selected) {
            gfx.setColor(Color.green);
            gfx.drawRect(_ox, _oy, _bounds.width-1, _bounds.height-1);
        }
    }

    @Override // documentation inherited
    public int getRenderOrder ()
    {
        return 5;
    }
}
