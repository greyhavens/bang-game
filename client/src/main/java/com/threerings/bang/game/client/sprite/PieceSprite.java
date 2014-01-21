//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.ArrayList;
import java.util.List;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jmex.effects.particles.ParticleMesh;

import com.threerings.util.MessageBundle;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.Sprite;
import com.threerings.media.image.Colorization;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.client.Config;
import com.threerings.bang.client.util.ParticleCache;
import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.client.effect.ParticlePool;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.GameCodes;
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

    /** The type of owner colorization used by this piece: none, static
     * colorization determined at the time of creation, or dynamic colorization
     * that reflects the current owner. */
    public enum Coloring { NONE, STATIC, DYNAMIC };
    
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
     * Returns the tooltip text to display when hovering over this sprite or
     * null if it has no tooltip. This will be translated via the {@link
     * GameCodes#GAME_MSGS} bundle and should be qualified if needed.
     */
    public String getTooltip (int pidx)
    {
        String ident = getHelpIdent(pidx);
        if (ident == null) {
            return null;
        }
        ident = "m.help_" + ident;
        return MessageBundle.compose("m.help_tip", ident + "_title", ident);
    }

    /**
     * Returns the help text identifier for this piece, or null if it has no
     * associated help.
     *
     * @param pidx our player's index for sprites that return different help
     * depending on whether they are ours or an opponent's.
     */
    protected String getHelpIdent (int pidx)
    {
        return null;
    }

    /**
     * Returns the help text title identifier for this piece, or null if it has
     * no associated help.
     *
     * @param pidx our player's index for sprites that return different help
     * depending on whether they are ours or an opponent's.
     */
    public String getHelpTitleIdent (int pidx)
    {
        String hident = getHelpIdent(pidx);
        return (hident == null) ? null : (hident + "_title");
    }

    /**
     * Called when we are first created and immediately before we are
     * added to the display.
     */
    public void init (BasicContext ctx, BoardView view, BangBoard board,
                      SoundGroup sounds, Piece piece, short tick)
    {
        _ctx = ctx;
        _view = view;
        _piece = piece;
        _tick = tick;

        // create and set the material that we will use to change shadow values
        // (if appropriate)
        if (isShadowable()) {
            _mstate = ctx.getRenderer().createMaterialState();
            _mstate.getDiffuse().set(ColorRGBA.white);
            _mstate.getAmbient().set(ColorRGBA.white);
            setRenderState(_mstate);
        }

        // don't create collision trees when not necessary
        if (!view.isHoverable(this) && !view.hasTooltip(this)) {
            setIsCollidable(false);
        }
        
        // position ourselves properly to start
        setLocation(board, _piece.x, _piece.y);
        setOrientation(piece.orientation);
        
        // create our sprite geometry
        createGeometry();

        // create any sounds associated with this sprite
        if (sounds != null) {
            createSounds(sounds);
        }
    }

    /**
     * Allows the piece sprite to return a separate piece of geometry that will
     * be added to the scene along with it as a peer.
     */
    public Spatial getHighlight ()
    {
        return _hnode;
    }

    /**
     * Returns a reference to the shadow highlight (or <code>null</code> if
     * this piece is not of type {@link Shadow#DYNAMIC}).
     */
    public TerrainNode.Highlight getShadow ()
    {
        return _shadow;
    }
    
    /** Indicates to this piece that it is selected by the user. May
     * someday trigger a special "selected" rendering mode, but presently
     * does nothing. */
    public void setSelected (boolean selected)
    {
        if (_selected != selected) {
            _selected = selected;
            updateStatus();
        }
    }

    /**
     * Returns the selected status of this sprite.
     */
    public boolean isSelected ()
    {
        return _selected;
    }

    /**
     * Indicates that the mouse is hovering over this piece.
     */
    public void setHovered (boolean hovered)
    {
        if (_hovered != hovered) {
            _hovered = hovered;
            updateStatus();
        }
    }

    /**
     * Configures this sprite's tile location.
     */
    public void setLocation (BangBoard board, int tx, int ty)
    {
        setLocation(tx, ty, _piece.computeElevation(board, tx, ty));
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
    public void snapToTerrain (boolean moving)
    {
        // flyers simply fly from point to point
        if (moving ? _piece.isFlyer() : _piece.isAirborne()) {
            return;
        }

        // adjust position to terrain height
        Vector3f pos = getLocalTranslation();
        Quaternion rot = getLocalRotation();
        Vector3f normal = null;
        int tx = (int)(pos.x / TILE_SIZE);
        int ty = (int)(pos.y / TILE_SIZE);
        BangBoard board = _view.getBoard();
        boolean bridge = board.isBridge(tx, ty);
        if (bridge) {
            pos.z = board.getElevation(tx, ty) * 
                board.getElevationScale(TILE_SIZE);
            normal = UP;
        } else {
            TerrainNode tnode = _view.getTerrainNode();
            pos.z = tnode.getHeightfieldHeight(pos.x, pos.y);
            normal = tnode.getHeightfieldNormal(pos.x, pos.y);
        }
        setLocalTranslation(pos);

        // adjust rotation to terrain slope
        Vector3f up = rot.mult(Vector3f.UNIT_Z),
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
     * Returns true if this sprite can be hovered over with the mouse, usually
     * in preparation for a selection of some kind.
     */
    public boolean isHoverable ()
    {
        return false;
    }
    
    /**
     * Returns true if this sprite has a tooltip for display.  We provide this
     * method instead of calling <code>getHelpIdent() != null</code> because
     * the latter may create a string object and we don't want to create a
     * bunch of garbage when hit testing numerous sprites every time the mouse
     * is moved.
     */
    public boolean hasTooltip ()
    {
        // as a general rule, everything hoverable has a tooltip
        return isHoverable();
    }
    
    /**
     * Indicates the type of owner recolorization applied to this sprite: none,
     * static colorization determined at the time of creation, or dynamic
     * recoloring that reflects the current owner.
     */
    public Coloring getColoringType ()
    {
        return Coloring.NONE;
    }
    
    /**
     * Returns the colorizations applied to the sprite.
     */
    public Colorization[] getColorizations ()
    {
        return _zations;
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
        float diffuse = 1f - _view.getShadowIntensity() * shadowed;
        _mstate.getDiffuse().set(diffuse, diffuse, diffuse,
            _mstate.getDiffuse().a);
    }

    /**
     * Called when we receive an event indicating that our piece was updated in
     * some way.
     */
    public void updated (Piece piece, short tick)
    {
        _piece = (Piece)piece.clone();
        _tick = (short)Math.max(_tick, tick);
        
        // update colorizations for dynamically colored pieces when their
        // owners change
        if (_powner != piece.owner && getColoringType() == Coloring.DYNAMIC &&
            _type != null && _name != null) {
            updateColorizations();
            loadModel(_type, _name);
            _powner = piece.owner;
        }

        updateStatus();
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
            updateTileHighlight();
            updateStatus();
            return false;
        }
        
        // move ourselves to our new location if we have one
        if (_piece.x != _px || _piece.y != _py) {
//             log.info("Moving " + _piece + " from +" +
//                      _piece.x + "+" + _piece.y + " to +" + _px + "+" + _py);
            moveSprite(board);
            if (!isMoving() && animatedMove()) {
                log.warning("Moved but am not moving?! " + _piece);
            }
        }

        // if we started moving as a result, we need to be waited for
        return isMoving();
    }

    /**
     * Set the sprite to fast animation mode.
     */
    public void fastAnimations (boolean fast)
    {
        _fastAnimation = fast;
    }

    /**
     * Called when a damage indicator is added.  Returns the offset for
     * the indicator.
     */
    public int damageAttach ()
    {
        return _damOn++;
    }

    /**
     * Called when a damage indicator is removed.
     */
    public void damageDetach ()
    {
        _damOff++;
        if (_damOn == _damOff) {
            _damOn = _damOff = 0;
        }
    }

    /**
     * Returns a reference to the model node.
     */
    public Model getModelNode ()
    {
        return _model;
    }
    
    /**
     * Returns the model controllers attached to the model.
     */
    public List<?> getModelControllers ()
    {
        return (_model == null ? null : _model.getControllers());
    }

    /**
     * Returns the height of the model.
     */
    public float getHeight ()
    {
        return (_piece.getHeight() * TILE_SIZE);
    }

    /**
     * Fires off a transient particle effect at this sprite's location.
     *
     * @param center if true, place the effect at the sprite's vertical center
     */
    public void displayParticles (String name, final boolean center)
    {
        final Quaternion rot = new Quaternion(localRotation);
        final Vector3f trans = new Vector3f(localTranslation);
        ParticlePool.getParticles(name,
            new ResultAttacher<Spatial>(_view.getPieceNode()) {
            public void requestCompleted (Spatial result) {
                super.requestCompleted(result);
                rot.mult(ParticleCache.Z_UP_ROTATION,
                    result.getLocalRotation());
                if (center) {
                    rot.multLocal(result.getLocalTranslation().set(
                        0f, 0f, _piece.getHeight() *
                            TILE_SIZE * 0.5f)).addLocal(trans);
                } else {
                    result.getLocalTranslation().set(trans);
                }
            }      
        });
    }
    
    /**
     * Fires off a dust ring at this sprite's base.
     */
    public void displayDustRing ()
    {
        ParticleMesh ring = ParticlePool.getDustRing();
        TerrainConfig terrain = TerrainConfig.getConfig(
            _view.getBoard().getPredominantTerrain(_piece.x, _piece.y));
        ColorRGBA color = terrain.dustColor;
        ring.getStartColor().set(color);
        ring.getEndColor().set(color.r, color.g, color.b, 0f);
        
        ring.setLocalTranslation(getLocalTranslation());
        ring.setLocalRotation(getLocalRotation());
        
        _view.getPieceNode().attachChild(ring);
        ring.updateRenderState();
        ring.updateGeometricState(0f, false);
        ring.forceRespawn();
    }

    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);

        if (_status != null) {
            // of up and forward, use the one with the greater x/y
            Vector3f dir = _ctx.getCameraHandler().getCamera().getDirection(),
                up = _ctx.getCameraHandler().getCamera().getUp(),
                vec = (dir.x * dir.x + dir.y * dir.y > 
                        up.x * up.x + up.y * up.y) ? dir : up;
            _angle = -FastMath.atan2(-vec.x, vec.y);
            _camrot.fromAngleAxis(_angle, Vector3f.UNIT_Z);
            _camrot.mult(HALF_UNIT, _camtrans);
            _camtrans.set(0.5f - _camtrans.x, 0.5f - _camtrans.y, 0f);

            // rotate our unit status with the camera
            _status.rotateWithCamera(_camrot, _camtrans);
        }
    }

    /**
     * Called when the terrain has been updated so that the highlight can
     * update its vertices.
     */
    public void updateTileHighlightVertices ()
    {
        if (_tlight != null) {
            _tlight.updateVertices();
        }
    }

    /**
     * Sprites should create and attach their scene geometry by overriding
     * this method.
     */
    protected void createGeometry ()
    {
        if (getColoringType() != Coloring.NONE) {
            _powner = _piece.owner;
            updateColorizations();
        }
        if (getShadowType() == Shadow.DYNAMIC) {
            // the dynamic shadow is a highlight with wider geometry
            float length = TILE_SIZE, // _view.getShadowLength(),
                rotation = 0f, // _view.getShadowRotation(),
                intensity = _view.getDynamicShadowIntensity();
            _shadow = _view.getTerrainNode().createHighlight(
                localTranslation.x, localTranslation.y, length, length);
            _shadow.setIsCollidable(false);
            _shadow.setRenderState(RenderUtil.createShadowTexture(
                _ctx, length, rotation, intensity));
            _shadow.updateRenderState();
            attachHighlight(_shadow);
        }
    }

    /**
     * Sets the sprite's colorizations to correspond to its piece's owner.
     */
    protected void updateColorizations ()
    {
        _zations = new Colorization[] {
            _ctx.getAvatarLogic().getColorPository().getColorization("unit",
                colorLookup[_piece.owner + 1] + 1 ) };
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
     * Updates the position of the highlight geometry that covers or floats
     * over the nearest tile.
     */
    protected void updateTileHighlight ()
    {
        if (_tlight == null) {
            return;
        }
        if (_tlight.getTileX() != _px || _tlight.getTileY() != _py) {
            _tlight.setPosition(_px, _py);
            if (_status != null) {
                _status.updateTranslations(_tlight);
            }
        }
    }

    /**
     * Updates the visibility and location of the status display.
     */
    protected void updateStatus ()
    {
        if (_status != null) {
            _status.update(_piece, _selected || _hovered);
            _status.setCullMode(CULL_DYNAMIC);
        }
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
     * Called when a sprite has been updated with a new location. The
     * default implementation simply relocates the sprite instantly but
     * derived classes will want to compute a path and animate the sprite
     * traveling between its old and new locations.
     */
    protected void moveSprite (BangBoard board)
    {
        setLocation(board, _piece.x, _piece.y);
    }

    /**
     * Returns true if this sprite should have animated movement.
     */
    protected boolean animatedMove ()
    {
        return !_fastAnimation;
    }

    /** Converts tile coordinates plus elevation into (3D) world
     * coordinates. */
    protected Vector3f toWorldCoords (int tx, int ty, int elev, Vector3f target)
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
     * Loads the identified model and attaches it to this sprite.
     */
    protected void loadModel (String type, String name)
    {
        loadModel(type, name, null);
    }
    
    /**
     * Loads the identified model and attaches it to this sprite.
     */
    protected void loadModel (String type, String name, String variant)
    {
        _type = type;
        _name = name;
        _variant = variant;
        
        // if we're not displaying units, don't load any models
        if (!Config.displayUnits) {
            return;
        }
        _view.addResolving(this);
        _ctx.getModelCache().getModel(type, name, variant, _zations,
            new ResultAttacher<Model>(this) {
            public void requestCompleted (Model model) {
                super.requestCompleted(model);
                _view.clearResolving(PieceSprite.this);
                modelLoaded(model);
            }
            public void requestFailed (Exception cause) {
                _view.clearResolving(PieceSprite.this);
            }
        });
    }
    
    /**
     * Called when our model has been loaded.
     */
    protected void modelLoaded (Model model)
    {
        // if we already have a model, remove it
        if (_model != null) {
            detachChild(_model);
        }
        _model = model;
        
        // give any sprite emissions references to the view and sprite
        for (Object ctrl : model.getControllers()) {
            if (ctrl instanceof SpriteEmission) {
                ((SpriteEmission)ctrl).setSpriteRefs(_ctx, _view, this);
            }
        }
    }
    
    /**
     * Updates the position of our highlights if we have them.
     */
    protected void updateHighlight ()
    {
        if (_shadow != null) {
            _shadow.setPosition(localTranslation.x, localTranslation.y);
        }
        updateTileHighlight();
    }

    protected BasicContext _ctx;
    protected BoardView _view;

    protected Piece _piece;
    protected int _px, _py, _powner;
    protected short _tick;

    protected boolean _selected;
    protected boolean _hovered;

    /** Most pieces have an underlying model, so we provide a reference. */
    protected Model _model;

    /** The type, name, and variant of the loaded model. */
    protected String _type, _name, _variant;

    /** The number of damage indicators being shown. */
    protected int _damOn, _damOff;
    
    /** Colorizations to apply to the model. */
    protected Colorization[] _zations;
    
    /** The material state used to manipulate shadow values. */
    protected MaterialState _mstate;

    /** The emissions associated with the model. */
    protected List<SpriteEmission> _emissions = new ArrayList<SpriteEmission>();

    /** A place to hang all highlight geometry. */
    protected Node _hnode;

    /** Our shadow if we have one. */
    protected TerrainNode.Highlight _shadow;

    /** Used when updating our shadow location. */
    protected Vector3f _loc = new Vector3f();

    /** Used when updating our shadow location. */
    protected Vector2f _result = new Vector2f();

    /** Our status. */
    protected PieceStatus _status;
    protected TerrainNode.Highlight _tlight;

    protected float _angle;
    protected Quaternion _camrot = new Quaternion();
    protected Vector3f _camtrans = new Vector3f();

    /** Perform fast versions of animations. */
    protected boolean _fastAnimation;

    /** When activated, causes all pieces to warp instead of smoothly
     * follow a path. */
    protected static boolean _editorMode;

    /** Used for temporary calculations. */
    protected static Vector3f _temp = new Vector3f();

    protected static final Vector3f HALF_UNIT = new Vector3f(0.5f, 0.5f, 0f);

    protected static float[] ROTATIONS = {
        0, // NORTH
        FastMath.PI/2, // EAST
        FastMath.PI, // SOUTH
        3*FastMath.PI/2, // WEST
    };
}
