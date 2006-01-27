//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.TriMesh;
import com.jme.scene.shape.Pyramid;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.LightState;

import com.threerings.bang.game.data.piece.Marker;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a player start or bonus marker.
 */
public class MarkerSprite extends PieceSprite
{
    public MarkerSprite (int type)
    {
        TriMesh mesh;
        if (type == Marker.CAMERA) {
            mesh = new Pyramid("marker", TILE_SIZE, TILE_SIZE);
            mesh.getLocalTranslation().set(0f, 0f, TILE_SIZE/2);
            
        } else {
            mesh = new Sphere("marker", new Vector3f(0, 0, TILE_SIZE/2),
                10, 10, TILE_SIZE/2);
        }
        mesh.setSolidColor(COLORS[type]);
        mesh.setModelBound(new BoundingBox());
        mesh.updateModelBound();
        mesh.setLightCombineMode(LightState.OFF);
        attachChild(mesh);
    }

    protected static final ColorRGBA[] COLORS = {
        ColorRGBA.blue, // START
        ColorRGBA.green, // BONUS
        ColorRGBA.red, // CATTLE
        ColorRGBA.gray, // CAMERA
    };
}
