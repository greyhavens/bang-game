//
// $Id$

package com.samskivert.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;

import static com.samskivert.bang.client.BangMetrics.*;

/**
 * Displays a building piece.
 */
public class BuildingSprite extends PieceSprite
{
    public BuildingSprite (int width, int height)
    {
        super(width*SQUARE-4, height*SQUARE-4);
    }

    // documentation inherited
    public void paint (Graphics2D gfx)
    {
        gfx.setColor(BUILDING_BROWN);
        gfx.fillRect(_bounds.x, _bounds.y, _bounds.width, _bounds.height);
        gfx.setColor(Color.black);
        gfx.drawRect(_bounds.x, _bounds.y, _bounds.width, _bounds.height);

        // TEMP: render our id so I can debug building jiggling
        int lx = _bounds.x+(_bounds.width-_idLabel.getSize().width)/2;
        int ly = _bounds.y+(_bounds.height-_idLabel.getSize().height)/2;
        _idLabel.render(gfx, lx, ly);
    }

    protected static final Color BUILDING_BROWN = new Color(0x79430E);
}
