//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.math.Quaternion;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;

import com.threerings.openal.SoundGroup;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a player start or bonus marker.
 */
public class MarkerSprite extends PieceSprite
    implements PieceCodes
{
    public MarkerSprite (int type)
    {
        if (type == Marker.SAFE) {
            return;
        }
        Sphere sphere = new Sphere("marker", new Vector3f(0, 0, TILE_SIZE/2),
            10, 10, TILE_SIZE/2);
        sphere.setSolidColor(COLORS[type]);
        sphere.setModelBound(new BoundingBox());
        sphere.updateModelBound();
        sphere.setLightCombineMode(LightState.OFF);
        attachChild(sphere);
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, BangBoard board, 
            SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        if (Marker.isMarker(piece, Marker.SAFE)) {
            if (_safestate[0] == null) {
                _safestate[0] = RenderUtil.createTextureState(
                        ctx, "textures/tile/safe1.png");
                Texture ntex = _safestate[0].getTexture();
                Vector3f up = new Vector3f(0f, 0f, 1f);
                // create the 4 rotations of the texture
                for (int ii = 1; ii < _safestate.length; ii++) {
                    Texture rtex = ntex.createSimpleClone();
                    Quaternion rot = new Quaternion();
                    rtex.setRotation(rot.fromAngleNormalAxis(
                                (float)(ii * Math.PI / 2), up));
                    _safestate[ii] = RenderUtil.createTextureState(
                            ctx, rtex);
                }
            }
            _tlight = view.getTerrainNode().createHighlight(
                    piece.x, piece.y, false, (byte)1);
            _tlight.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            _tlight.setRenderState(_safestate[piece.orientation]);
            _tlight.setRenderState(RenderUtil.blendAlpha);
            attachHighlight(_tlight);
        }
    }

    @Override // documenatation inherited
    public void setOrientation (int orientation)
    {
        if (Marker.isMarker(_piece, Marker.SAFE) && _tlight != null) {
            _tlight.setRenderState(_safestate[orientation]);
            _tlight.updateRenderState();
        }
    }

    protected static final ColorRGBA[] COLORS = {
        ColorRGBA.blue, // START
        ColorRGBA.green, // BONUS
        ColorRGBA.red, // CATTLE
        new ColorRGBA(1, 1, 0, 1), // LODE
        new ColorRGBA(0, 1, 1, 1), // TOTEM
        new ColorRGBA(1, 0, 1, 1), // SAFE
        new ColorRGBA(0.5f, 0.5f, 0.5f, 1), // ROBOTS
    };

    protected static TextureState[] _safestate = 
        new TextureState[DIRECTIONS.length];
}
