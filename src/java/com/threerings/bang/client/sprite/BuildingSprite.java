//
// $Id$

package com.threerings.bang.client.sprite;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
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
        box.setSolidColor(BUILDING_BROWN);
        attachChild(box);
    }

    protected static final ColorRGBA BUILDING_BROWN =
        new ColorRGBA(0.47f, 0.26f,  0.05f, 0f);
}
