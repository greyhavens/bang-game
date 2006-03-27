//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.HashMap;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.LineSegmentPath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.Sprite;
import com.threerings.media.image.Colorization;
import com.threerings.media.util.MathUtil;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.client.Config;
import com.threerings.bang.client.Model;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the rendering of a particular piece on the board.
 */
public class PieceSprite extends Sprite
{
    /** The types of shadow that can be cast by this piece. */
    public enum Shadow { NONE, STATIC, DYNAMIC };

    /**
     * Called by the editor to make pieces warp to their new locations for
     * rapid draggability.
     */
    public static void setEditorMode (boolean editorMode)
    {
        _editorMode = editorMode;
    }

    /**
     * Checks whether editor mode has been enabled.
     */
    public static boolean isEditorMode ()
    {
        return _editorMode;
    }
    
    /** Returns the piece associated with this sprite. */
    public Piece getPiece ()
    {
        return _piece;
    }

    /** Returns the id of the piece associated with this sprite. */
    public int getPieceId ()
    {
        return _piece.pieceId;
    }

    /**
     * Returns the help text identifier for this piece, or null if it has no
     * associated help.
     *
     * @param pidx our player's index for sprites that return different help
     * depending on whether they are ours or an opponent's.
     */
    public String getHelpIdent (int pidx)
    {
        return null;
    }

    /**
     * Called when we are first created and immediately before we are
     * added to the display.
     */
    public void init (BasicContext ctx, BoardView view, BangBoard board,
                      SoundGroup sounds, Piece piece, short tick)
    {
        _view = view;
        _piece = piece;
        _tick = tick;

        // create and set the material that we will use to change shadow values
        // (if appropriate)
        if (isShadowable()) {
            _mstate = ctx.getRenderer().createMaterialState();
            _mstate.setDiffuse(new ColorRGBA(ColorRGBA.white));
            _mstate.setAmbient(ColorRGBA.white);
            setRenderState(RenderUtil.createColorMaterialState(_mstate,
                false));
            updateRenderState();
        }

        // create our sprite geometry
        createGeometry(ctx);
        setAnimationSpeed(20);

        // create any sounds associated with this sprite
        createSounds(sounds);

        // position ourselves properly to start
        setLocation(board, _piece.x, _piece.y);
        setOrientation(piece.orientation);
    }

    /**
     * Allows the piece sprite to return a separate piece of geometry that will
     * be added to the scene along with it as a peer.
     */
    public Spatial getHighlight ()
    {
        return _hnode;
    }

    /** Indicates to this piece that it is selected by the user. May
     * someday trigger a special "selected" rendering mode, but presently
     * does nothing. */
    public void setSelected (boolean selected)
    {
        if (_selected != selected) {
            _selected = selected;
        }
    }

    /**
     * Configures this sprite's tile location.
     */
    public void setLocation (BangBoard board, int tx, int ty)
    {
        setLocation(tx, ty, computeElevation(board, tx, ty));
    }
    
    /**
     * Configures this sprite's tile location and elevation.
     */
    public void setLocation (int tx, int ty, int elevation)
    {
        toWorldCoords(_px = tx, _py = ty, elevation, _temp);
        if (!_temp.equals(localTranslation)) {
            setLocalTranslation(new Vector3f(_temp));
//             log.info("Moving to " + tx + ", " + ty + ", " + elevation +
//                      ": " + _temp);
            updateShadowValue();
            updateHighlight();
        }
    }

    /**
     * Configures this sprite's orientation.
     */
    public void setOrientation (int orientation)
    {
        // if moving, assume the path will do the right thing when we arrive
        if (!isMoving()) {
            Quaternion quat = new Quaternion();
            quat.fromAngleAxis(ROTATIONS[orientation], UP);
            setLocalRotation(quat);
        }
    }

    /**
     * If the unit is on the ground, sets its z position based on the height of
     * the terrain, and its pitch and roll based on the slope of the terrain.
     * The units's x coordinate, y coordinate, and heading are unaffected.
     */
    public void snapToTerrain ()
    {
        // flyers simply fly from point to point
        if (_piece.isFlyer()) {
            return;
        }

        // adjust position to terrain height
        Vector3f pos = getLocalTranslation();
        TerrainNode tnode = _view.getTerrainNode();
        pos.z = tnode.getHeightfieldHeight(pos.x, pos.y);
        setLocalTranslation(pos);

        // adjust rotation to terrain slope
        Quaternion rot = getLocalRotation();
        Vector3f normal = tnode.getHeightfieldNormal(pos.x, pos.y),
            up = rot.mult(Vector3f.UNIT_Z),
            cross = up.cross(normal);
        float angle = FastMath.asin(cross.length());
        if (angle == 0f) {
            return;
        }
        Quaternion mod = new Quaternion();
        mod.fromAngleAxis(angle, cross);
        setLocalRotation(mod.multLocal(rot));
    }

    /**
     * Indicates the type of shadow cast by this sprite. This is used to
     * determine which sprites cast precomputed shadows and which have shadow
     * geometry rendered in realtime.
     */
    public Shadow getShadowType ()
    {
        return Shadow.NONE;
    }

    /**
     * Checks whether this sprite should be darkened by static shadows.
     */
    public boolean isShadowable ()
    {
        return true;
    }

    /**
     * Returns true if this sprite can be hovered over with the mouse, which
     * will display contextual help. We provide this method instead of calling
     * <code>getHelpIdent() != null</code> because the latter may create a
     * string object and we don't want to create a bunch of garbage when hit
     * testing numerous sprites every time the mouse is moved.
     */
    public boolean isHoverable ()
    {
        return false;
    }

    /**
     * Determines how much of this piece lies in shadow and darkens it
     * accordingly.
     */
    public void updateShadowValue ()
    {
        if (!isShadowable()) {
            return;
        }

        float sheight = _view.getTerrainNode().getShadowHeight(
            localTranslation.x, localTranslation.y), shadowed;
        if (sheight >= localTranslation.z + TILE_SIZE) {
            shadowed = 1f;

        } else if (sheight > localTranslation.z) {
            shadowed = (sheight - localTranslation.z) / TILE_SIZE;

        } else {
            shadowed = 0f;
        }
        float diffuse = 1f - _view.getBoard().getShadowIntensity() * shadowed;
        _mstate.getDiffuse().set(diffuse, diffuse, diffuse, 1f);
        updateRenderState();
    }

    /**
     * Called when we receive an event indicating that our piece was updated in
     * some way.
     */
    public void updated (Piece piece, short tick)
    {
        _piece = (Piece)piece.clone();
        _tick = tick;
    }

    /**
     * Called when our piece is updated in such a way that we may have moved.
     * We compare our internal (to the sprite) last know position and
     * orientation to the orientation of our current piece, and effect any
     * necessary movement.
     *
     * @return true if this update resulted in the sprite being moved along a
     * path, in which case the view will wait for the movement to finish before
     * animating other bits.
     */
    public boolean updatePosition (BangBoard board)
    {
        // in the editor, just update everything; there could be something as
        // wacky as a change in elevation scale
        if (_editorMode) {
            moveSprite(board);
            setOrientation(_piece.orientation);
            return false;
        }
        
        // move ourselves to our new location if we have one
        if (_piece.x != _px || _piece.y != _py) {
//             log.info("Moving " + _piece.info() + " from +" +
//                      _piece.x + "+" + _piece.y + " to +" + _px + "+" + _py);
            moveSprite(board);
            if (!isMoving()) {
                log.warning("Moved but am not moving?! " + _piece.info());
            }
        }

        // if we started moving as a result, we need to be waited for
        return isMoving();
    }

    /**
     * Called when our piece is removed from the board state.
     */
    public void removed ()
    {
    }

    /**
     * Sprites should create and attach their scene geometry by overriding
     * this method.
     */
    protected void createGeometry (BasicContext ctx)
    {
        if (getShadowType() == Shadow.DYNAMIC) {
            // the dynamic shadow is a highlight with wider geometry
            float length = TILE_SIZE, // _view.getShadowLength(),
                rotation = 0f, // _view.getShadowRotation(),
                intensity = _view.getShadowIntensity();
            _shadow = _view.getTerrainNode().createHighlight(
                localTranslation.x, localTranslation.y, length, length);
            _shadow.setIsCollidable(false);
            _shadow.setRenderState(RenderUtil.createShadowTexture(
                                       ctx, length, rotation, intensity));
            _shadow.updateRenderState();
            attachHighlight(_shadow);
        }
    }

    /**
     * Attaches geometry to our highlight node, creating the highlight node if
     * necessary.
     */
    protected void attachHighlight (Spatial spatial)
    {
        if (_hnode == null) {
            _hnode = createHighlightNode();
        }
        _hnode.attachChild(spatial);
    }

    /**
     * Sprites can create and pre-load sounds they will need by overriding
     * this method.
     */
    protected void createSounds (SoundGroup sounds)
    {
    }

    /**
     * Creates a highlight node, which is used by some sprites to render onto
     * the terrain below themselves.
     */
    protected Node createHighlightNode ()
    {
        Node hnode = new Node("highlight");
        hnode.setLightCombineMode(LightState.OFF);
        hnode.setRenderState(RenderUtil.overlayZBuf);
        hnode.setRenderState(RenderUtil.blendAlpha);
        hnode.setRenderState(RenderUtil.backCull);
        hnode.updateRenderState();
        return hnode;
    }

    /**
     * Computes the elevation for this piece at the specified location.
     */
    protected int computeElevation (BangBoard board, int tx, int ty)
    {
        int width = _piece.getWidth(), height = _piece.getHeight();
        if (width == 1 && height == 1) {
            return board.getHeightfieldElevation(tx, ty);
        }

        int elevation = Integer.MIN_VALUE;
        for (int y = ty, ymax = ty + height; y < ymax; y++) {
            for (int x = tx, xmax = tx + width; x < xmax; x++) {
                elevation = Math.max(elevation,
                    board.getHeightfieldElevation(x, y));
            }
        }
        return elevation;
    }

    /**
     * Called when a sprite has been updated with a new location. The
     * default implementation simply relocates the sprite instantly but
     * derived classes will want to compute a path and animate the sprite
     * traveling between its old and new locations.
     */
    protected void moveSprite (BangBoard board)
    {
        setLocation(board, _piece.x, _piece.y);
    }

    /** Converts tile coordinates plus elevation into (3D) world
     * coordinates. */
    protected Vector3f toWorldCoords (int tx, int ty, int elev,
        Vector3f target)
    {
        target.x = tx * TILE_SIZE;
        target.y = ty * TILE_SIZE;
        target.z = elev * _view.getBoard().getElevationScale(TILE_SIZE);
        centerWorldCoords(target);
        return target;
    }

    /** Adjusts the coordinates to the center of the piece's footprint. */
    protected void centerWorldCoords (Vector3f coords)
    {
        coords.x += TILE_SIZE/2;
        coords.y += TILE_SIZE/2;
    }

    /**
     * Binds the given animation and updates the set of emissions.
     *
     * @param random the number used to select a texture
     * @param zations the colorizations to use for the texture, or
     * <code>null</code> for none
     */
    protected void bindAnimation (
        final BasicContext ctx, Model.Animation anim, int random,
        Colorization[] zations)
    {
        // stop all running emissions
        for (SpriteEmission emission : _emissions.values()) {
            if (emission.isRunning()) {
                emission.stop();
            }
        }

        // bind the new animation
        _view.addResolvingSprite(this);
        _binding = anim.bind(
            this, random, zations, new Model.Binding.Observer() {
            public void wasBound (
                Model.Animation anim, Model.Binding binding) {
                // now that the meshes are attached, configure the animation
                // speed and repeat type
                setAnimationSpeed(
                    Config.display.animationSpeed * anim.getSpeed());
                setAnimationRepeatType(anim.repeatType);

                // start emissions used in the animation, creating any uncreated
                for (int ii = 0; ii < anim.emitters.length; ii++) {
                    String name = anim.emitters[ii].name;
                    SpriteEmission emission = _emissions.get(name);
                    if (emission == null) {
                        _emissions.put(name, emission = SpriteEmission.create(
                                           name, anim.emitters[ii].props));
                        emission.init(ctx, _view, PieceSprite.this);
                    }
                    emission.start(anim, binding);
                }

                _view.clearResolvingSprite(PieceSprite.this);
            }
            public void wasSkipped (Model.Animation anim) {
                _view.clearResolvingSprite(PieceSprite.this);
            }
        });
    }

    /**
     * Updates the position of our highlights if we have them.
     */
    protected void updateHighlight ()
    {
        if (_shadow != null) {
            _shadow.setPosition(localTranslation.x, localTranslation.y);
            /*
            _loc.set(localTranslation.x, localTranslation.y,
                     localTranslation.z);
            _view.getShadowLocation(_loc, _result);
            if (_shadow.x != _result.x || _shadow.y != _result.y) {
                _shadow.setPosition(_result.x, _result.y);
            }
            */
        }
    }

    @Override // documentation inherited
    protected void setParent (Node parent)
    {
        super.setParent(parent);

        // clear our model binding and emissions when we're removed
        if (parent == null) {
            if (_binding != null) {
                _binding.detach();
                _binding = null;
            }
            for (SpriteEmission emission : _emissions.values()) {
                emission.cleanup();
            }
        }
    }

    protected BoardView _view;

    protected Piece _piece;
    protected int _px, _py;
    protected short _tick;

    protected boolean _selected;

    /** Most pieces have an underlying model, so we provide a reference. */
    protected Model _model;

    /** Sprites must bind animations from their model with this reference so
     * that we can properly clear the binding when the sprite is removed. */
    protected Model.Binding _binding;

    /** The material state used to manipulate shadow values. */
    protected MaterialState _mstate;

    /** The emissions activated by animations. */
    protected HashMap<String, SpriteEmission> _emissions =
        new HashMap<String, SpriteEmission>();

    /** A place to hang all highlight geometry. */
    protected Node _hnode;

    /** Our shadow if we have one. */
    protected TerrainNode.Highlight _shadow;

    /** Used when updating our shadow location. */
    protected Vector3f _loc = new Vector3f();

    /** Used when updating our shadow location. */
    protected Vector2f _result = new Vector2f();

    /** When activated, causes all pieces to warp instead of smoothly
     * follow a path. */
    protected static boolean _editorMode;

    /** Used for temporary calculations. */
    protected static Vector3f _temp = new Vector3f();

    protected static float[] ROTATIONS = {
        0, // NORTH
        FastMath.PI/2, // EAST
        FastMath.PI, // SOUTH
        3*FastMath.PI/2, // WEST
    };
}
