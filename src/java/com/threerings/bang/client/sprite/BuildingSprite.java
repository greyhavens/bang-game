//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;

import com.jme.math.Vector3f;
import com.jme.scene.shape.Box;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a building piece.
 */
public class BuildingSprite extends PieceSprite
{
    public BuildingSprite (int width, int height)
    {
        // create some simple temporary geometry
        Box box = new Box("box", new Vector3f(0, 0, 0),
                          new Vector3f(TILE_SIZE*width,
                                       TILE_SIZE*height, TILE_SIZE));
        attachChild(box);
//         super(width*SQUARE, height*SQUARE);
    }

//     // documentation inherited
//     public void paint (Graphics2D gfx)
//     {
//         gfx.setColor(BUILDING_BROWN);
//         gfx.fillRect(_ox+1, _oy+1, _bounds.width-1, _bounds.height-1);

//         // TEMP: render our id so I can debug building jiggling
//         int lx = _ox+(_bounds.width-_idLabel.getSize().width)/2;
//         int ly = _oy+(_bounds.height-_idLabel.getSize().height)/2;
//         _idLabel.render(gfx, lx, ly);
//     }

    protected static final Color BUILDING_BROWN = new Color(0x79430E);
}
