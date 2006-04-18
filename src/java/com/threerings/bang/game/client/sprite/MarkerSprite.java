//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.LightState;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a player start or bonus marker.
 */
public class MarkerSprite extends PieceSprite
{
    public MarkerSprite (int type)
    {
        Sphere sphere = new Sphere("marker", new Vector3f(0, 0, TILE_SIZE/2),
            10, 10, TILE_SIZE/2);
        sphere.setSolidColor(COLORS[type]);
        sphere.setModelBound(new BoundingBox());
        sphere.updateModelBound();
        sphere.setLightCombineMode(LightState.OFF);
        attachChild(sphere);
    }

    protected static final ColorRGBA[] COLORS = {
        ColorRGBA.blue, // START
        ColorRGBA.green, // BONUS
        ColorRGBA.red, // CATTLE
        new ColorRGBA(1, 1, 0, 1), // LODE
    };
}
