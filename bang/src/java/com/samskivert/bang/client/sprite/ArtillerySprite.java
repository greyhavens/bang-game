//
// $Id$

package com.samskivert.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;

import com.samskivert.bang.data.piece.Artillery;
import com.samskivert.bang.data.piece.Piece;

import static com.samskivert.bang.Log.log;
import static com.samskivert.bang.client.BangMetrics.*;

/**
 * Displays a artillery piece.
 */
public class ArtillerySprite extends MobileSprite
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

        // draw a circle
        Color color = _piece.owner == 0 ? Color.white : Color.yellow;
        if (_piece.energy == 0) {
            color = Color.gray;
        }
        gfx.setColor(color);
        gfx.fillRoundRect(_ox, _oy, width, height, width/2, height/2);

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
