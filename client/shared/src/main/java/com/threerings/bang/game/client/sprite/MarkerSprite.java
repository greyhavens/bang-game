//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;

import com.samskivert.util.HashIntMap;

import com.threerings.openal.SoundGroup;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;

import static com.threerings.bang.client.BangMetrics.*;

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
            Sphere mesh = new Sphere(10, 10, TILE_SIZE/2);
            _sphere = new Geometry("marker", mesh);
            _sphere.setLocalTranslation(0, 0, TILE_SIZE/2);
            _sphereColor = (ColorRGBA)SPRITES[type*2+1];
            attachChild(_sphere);
        } else if (!_modelType.equals("highlight") &&
                !_modelType.equals("terrain")) {
            _type = _modelType;
            _name = (String)SPRITES[type*2+1];
        }
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, BangBoard board,
            SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        if (!_modelType.equals("terrain")) {
            return;
        }
        int type = ((Marker)_piece).getType();
        Material tmat = _markerMaterials.get(type);
        if (tmat == null) {
            tmat = RenderUtil.createTextureMaterial(ctx, (String)SPRITES[type*2+1]);
            RenderUtil.applyBlendAlpha(tmat);
            RenderUtil.applyOverlayZBuf(tmat);
            _markerMaterials.put(type, tmat);
        }
        _tlight = view.getTerrainNode().createHighlight(
                piece.x, piece.y, false, (byte)1);
        _tlight.setTextures(tmat, tmat);
        attachHighlight(_tlight);
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();

        // colour the sphere marker now that the context (asset manager) is available
        if (_sphere != null) {
            Material mat = new Material(_ctx.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA(_sphereColor));
            _sphere.setMaterial(mat);
        }

        // load our specialized model if we have one
        if (_type != null) {
            loadModel(_type, _name);
        }
    }

    protected static final ColorRGBA[] COLORS = {
        ColorRGBA.Blue, // START
        ColorRGBA.Green, // BONUS
        ColorRGBA.Red, // CATTLE
        new ColorRGBA(1, 1, 0, 1), // LODE
        new ColorRGBA(0, 1, 1, 1), // TOTEM
        new ColorRGBA(1, 0, 1, 1), // SAFE
        new ColorRGBA(0.5f, 0.5f, 0.5f, 1), // ROBOTS
        new ColorRGBA(0.2f, 0.7f, 0.4f, 1), // TALISMAN
        new ColorRGBA(0.2f, 0.7f, 0.4f, 1), // TALISMAN
    };

    protected static final Object[] SPRITES = {
        "sphere", ColorRGBA.Blue,   // START
        "sphere", ColorRGBA.Green,  // BONUS
        "extras", "frontier_town/cow", // CATTLE
        "bonuses", "frontier_town/nugget", // LODE
        "bonuses", "indian_post/totem_crown", // TOTEM
        "highlight", "textures/tile/safe_square", // SAFE
        "units", "indian_post/logging_robot", // ROBOTS
        "bonuses", "indian_post/talisman", // TALISMAN
        "bonuses", "indian_post/fetish_turtle", // FETISH
        "highlight", "textures/tile/safe_circle", //SAFE_ALT
        "terrain", "textures/tile/impass.png", // IMPASS
    };

    protected String _modelType;

    /** The sphere marker geometry and its colour (for the "sphere" marker types). */
    protected Geometry _sphere;
    protected ColorRGBA _sphereColor;

    protected static HashIntMap<Material> _markerMaterials =
        new HashIntMap<Material>();
}
