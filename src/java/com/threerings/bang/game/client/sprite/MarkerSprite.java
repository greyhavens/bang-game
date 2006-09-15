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
import com.samskivert.util.HashIntMap;

/**
 * Displays a player start or bonus marker.
 */
public class MarkerSprite extends PieceSprite
    implements PieceCodes
{
    public MarkerSprite (int type)
    {
        _modelType = (String)SPRITES[type*2];
        if (_modelType.equals("sphere")) {
            Sphere sphere = new Sphere("marker", 
                    new Vector3f(0, 0, TILE_SIZE/2), 10, 10, TILE_SIZE/2);
            sphere.setSolidColor((ColorRGBA)SPRITES[type*2+1]);
            sphere.setModelBound(new BoundingBox());
            sphere.updateModelBound();
            sphere.setLightCombineMode(LightState.OFF);
            attachChild(sphere);
        } else if (!_modelType.equals("highlight")) {
            _type = _modelType;
            _name = (String)SPRITES[type*2+1];
        }
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, BangBoard board, 
            SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        if (_modelType.equals("highlight")) {
            int type = ((Marker)_piece).getType();
            TextureState[] texstates = _highlightStates.get(type);
            if (texstates == null) {
                texstates = new TextureState[DIRECTIONS.length*2];
                texstates[0] = RenderUtil.createTextureState(
                        ctx, (String)SPRITES[type*2+1] + "_on.png");
                texstates[DIRECTIONS.length] = RenderUtil.createTextureState(
                        ctx, (String)SPRITES[type*2+1] + "_off.png");
                Texture ntex = texstates[0].getTexture();
                Texture ntexoff = texstates[DIRECTIONS.length].getTexture();
                Vector3f up = new Vector3f(0f, 0f, 1f);
                // create the 4 rotations of the texture
                for (int ii = 1; ii < DIRECTIONS.length; ii++) {
                    Texture rtex = ntex.createSimpleClone();
                    Texture rtexoff = ntexoff.createSimpleClone();
                    Quaternion rot = new Quaternion();
                    rtex.setRotation(rot.fromAngleNormalAxis(
                                (float)(ii * Math.PI / 2), up));
                    rtexoff.setRotation(rot.fromAngleNormalAxis(
                                (float)(ii * Math.PI / 2), up));
                    texstates[ii] = RenderUtil.createTextureState(
                            ctx, rtex);
                    texstates[DIRECTIONS.length + ii] = 
                        RenderUtil.createTextureState(ctx, rtexoff);
                }
                _highlightStates.put(type, texstates);
            }
            _tlight = view.getTerrainNode().createHighlight(
                    piece.x, piece.y, false, (byte)1);
            _tlight.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            _tlight.setRenderState(RenderUtil.blendAlpha);
            setOrientation(piece.orientation);
            attachHighlight(_tlight);
        }
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();

        // load our specialized model if we have one
        if (_type != null) {
            loadModel(_type, _name);
            setLightCombineMode(LightState.OFF);
        }
    }

    @Override // documenatation inherited
    public void setOrientation (int orientation)
    {
        setOnOff(orientation, ((Marker)_piece).getType() != Marker.SAFE_ALT);
    }

    /**
     * Set the on/off texture for a highlight marker.
     */
    public void setOnOff (int orientation, boolean on)
    {
        if (_modelType.equals("highlight") && _tlight != null) {
            if (!on) {
                orientation += DIRECTIONS.length;
            }
            _tlight.setRenderState(_highlightStates.get(
                        ((Marker)_piece).getType())[orientation]);
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
        new ColorRGBA(0.2f, 0.7f, 0.4f, 1), // TALISMAN
        new ColorRGBA(0.2f, 0.7f, 0.4f, 1), // TALISMAN
    };

    protected static final Object[] SPRITES = {
        "sphere", ColorRGBA.blue,   // START
        "sphere", ColorRGBA.green,  // BONUS
        "extras", "frontier_town/cow", // CATTLE
        "bonuses", "frontier_town/nugget", // LODE
        "bonuses", "indian_post/totem_crown", // TOTEM
        "highlight", "textures/tile/wendigo_safe_square", // SAFE
        "units", "indian_post/logging_robot", // ROBOTS
        "bonuses", "indian_post/talisman", // TALISMAN
        "bonuses", "indian_post/fetish_turtle", // FETISH
        "highlight", "textures/tile/wendigo_safe_circle", //SAFE_ALT
    };

    protected String _modelType;

    protected static HashIntMap<TextureState[]> _highlightStates = 
        new HashIntMap<TextureState[]>();
}
