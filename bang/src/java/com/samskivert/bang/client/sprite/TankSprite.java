//
// $Id$

package com.samskivert.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;

import com.samskivert.bang.data.piece.Tank;
import com.samskivert.bang.data.piece.Piece;

import static com.samskivert.bang.Log.log;
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

        int width = _bounds.width - _oxoff;
        int height = _bounds.height - _oyoff;

        // first draw a circle
        gfx.setColor(_piece.owner == 0 ? Color.white : Color.yellow);
        gfx.fillOval(_ox, _oy, width-1, height-1);

        // then draw a square back to communicate our orientation
        switch (_piece.orientation) {
        case Piece.NORTH:
            gfx.fillRect(_ox, _oy + height/2, width, height/2);
            break;
        case Piece.EAST:
            gfx.fillRect(_ox, _oy, width/2, height);
            break;
        case Piece.SOUTH:
            gfx.fillRect(_ox, _oy, width, height/2);
            break;
        case Piece.WEST:
            gfx.fillRect(_ox + width/2, _oy, width/2, height);
            break;
        }

        // draw the turret
        Tank tank = (Tank)_piece;
        int dx = width/2, dy = height/2;
        switch (tank.turretOrient) {
        case Piece.NORTH: dy = 0; break;
        case Piece.SOUTH: dy = height-1; break;
        case Piece.WEST: dx = 0; break;
        case Piece.EAST: dx = width-1; break;
        }

        gfx.setColor(Color.black);
        gfx.drawLine(_ox + width/2, _oy + height/2, _ox + dx, _oy + dy);

        if (_piece.hasPath()) {
            gfx.setColor(Color.blue);
            gfx.drawRect(_ox, _oy, width-1, height-1);
        } else if (_selected) {
            gfx.setColor(Color.green);
            gfx.drawRect(_ox, _oy, width-1, height-1);
        }
    }

    @Override // documentation inherited
    public int getRenderOrder ()
    {
        return 5;
    }
}
