//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Point;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.TextureState;

import com.samskivert.util.ObserverList;
import com.samskivert.util.StringUtil;

import com.threerings.media.util.MathUtil;
import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;
import com.threerings.util.RandomUtil;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.Sprite;
import com.threerings.jme.sprite.SpriteObserver;

import com.threerings.bang.client.Config;
import com.threerings.bang.client.Model;
import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays some sort of mobile entity on the board (generally a unit or
 * some other piece that moves around and is interacted with).
 */
public class MobileSprite extends PieceSprite
{
    /** Used to notify observers of completed animation actions. */
    public interface ActionObserver extends SpriteObserver
    {
        /** Called when an action has been completed by this sprite. */
        public void actionCompleted (Sprite sprite, String action);
    }

    /** A fake action that is queued up to indicate that this sprite
     * should be removed when all other actions are completed. */
    public static final String REMOVED = "__removed__";

    /**
     * Creates a mobile sprite with the specified model type and name.
     */
    public MobileSprite (String type, String name)
    {
        _type = type;
        _name = name;
    }

    /**
     * Returns true if this sprite supports the specified action
     * animation.
     */
    public boolean hasAction (String action)
    {
        return _model.hasAnimation(action);
    }

    /**
     * Returns the underlying animation for the specified action.
     */
    public Model.Animation getAction (String action)
    {
        return _model.getAnimation(action);
    }

    /**
     * Called to inform us that we will be shooting the specified target
     * sprite when we finish our path.
     */
    public void willShoot (Piece target, PieceSprite tsprite)
    {
    }

    /**
     * Runs the specified action animation.
     */
    public void queueAction (String action)
    {
        log.info("Queueing action " + action + " on " + _piece.info() + ".");
        _actions.add(action);
        if (_action == null) {
            startNextAction();
        }
    }

    /**
     * Returns true if this sprite is currently displaying an action
     * animation or moving along a path.
     */
    public boolean isAnimating ()
    {
        // this might be called between clearing _action and starting our
        // next action, so check both _action and _actions.size()
        return isMoving() || (_action != null) || (_actions.size() > 0);
    }
    
    @Override // documentation inherited
    public Spatial getHighlight ()
    {
        return _hnode;
    }
    
    /**
     * Sets this sprite on a path defined by a list of {@link Point} objects.
     *
     * @param speed the speed at which to move, in tiles per second
     */
    public void move (BangBoard board, List path, float speed)
    {
        _moveSound.loop(false);
        move(createPath(board, path, speed));
    }
    
    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();

        // stop our movement sound
        _moveSound.stop();

        // reorient properly
        reorient();
    }
    
    @Override // documentation inherited
    public void setOrientation (int orientation)
    {
        super.setOrientation(orientation);
        snapToTerrain();
    }
    
    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        // expire any actions first before we update our children
        if (_nextAction > 0) {
            _nextAction -= time;
            if (_nextAction <= 0) {
                String action = _action;
                _nextAction = 0;
                _action = null;

                // report that we completed this action
                if (_observers != null) {
                    _observers.apply(new CompletedOp(this, action));
                }

                // start the next action if we have one, otherwise rest
                if (_actions.size() > 0) {
                    startNextAction();
                } else {
                    setAction(getRestPose());
                    setAnimationActive(false);
                }
            }
        }

        super.updateWorldData(time);
    }
    
    @Override // documentation inherited
    public void snapToTerrain ()
    {
        super.snapToTerrain();
        updateHighlight();
    }
    
    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        super.createGeometry(ctx);

        // contains highlights draped over terrain
        _hnode = new Node("highlight");
        
        // the geometry of the highlight is shared between the elements
        _highlight = _view.getTerrainNode().createHighlight(localTranslation.x,
            localTranslation.y, TILE_SIZE, TILE_SIZE);
        
        // the shadow is an additional highlight with wider geometry
        createShadow(ctx);
        
        // load our model
        _model = ctx.loadModel(_type, _name);

        // start in our rest post
        setAction(getRestPose());
    }
    
    /**
     * Creates and attaches the shadow for this sprite.
     */
    protected void createShadow (BasicContext ctx)
    {
        float length = _view.getShadowLength(),
            rotation = _view.getShadowRotation(),
            intensity = _view.getShadowIntensity();
        _shadow = _view.getTerrainNode().createHighlight(localTranslation.x,
            localTranslation.y, length, length);
        _shadow.setIsCollidable(false);
        if (_shadtex == null || _slength != length || _srotation != rotation ||
                _sintensity != intensity) {
            createShadowTexture(ctx, length, rotation, intensity);
        }
        _shadow.setRenderState(_shadtex);
        _shadow.updateRenderState();
        _hnode.attachChild(_shadow);
    }
    
    /**
     * Creates the shadow texture for the current light parameters.
     */
    protected void createShadowTexture (BasicContext ctx, float length,
        float rotation, float intensity)
    {
        _slength = length;
        _srotation = rotation;
        _sintensity = intensity;
        
        float yscale = length / TILE_SIZE;
        int size = SHADOW_TEXTURE_SIZE, hsize = size / 2;
        ByteBuffer pbuf = ByteBuffer.allocateDirect(size * size * 4);
        byte[] pixel = new byte[] { 0, 0, 0, 0 };
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float xd = (float)(x - hsize) / hsize,
                    yd = yscale * (y - hsize) / hsize,
                    d = FastMath.sqrt(xd*xd + yd*yd),
                    val = d < 0.25f ? intensity : intensity *
                        Math.max(0f, 1.333f - 1.333f*d);
                pixel[3] = (byte)(val * 255);
                pbuf.put(pixel);
            }
        }
        pbuf.rewind();
        
        // we must rotate the shadow into place and translate to recenter
        Texture stex = new Texture();
        stex.setImage(new Image(Image.RGBA8888, size, size, pbuf));
        Quaternion rot = new Quaternion();
        rot.fromAngleNormalAxis(-rotation, Vector3f.UNIT_Z);
        stex.setRotation(rot);
        Vector3f trans = new Vector3f(0.5f, 0.5f, 0f);
        rot.multLocal(trans);
        stex.setTranslation(new Vector3f(0.5f - trans.x, 0.5f - trans.y, 0f));
        
        _shadtex = ctx.getRenderer().createTextureState();
        _shadtex.setTexture(stex);
    }
    
    @Override // documentation inherited
    protected void createSounds (SoundGroup sounds)
    {
        super.createSounds(sounds);

        // load up our movement sounds
        _moveSound = sounds.getSound(
            "rsrc/" + _type + "/" + _name + "/move.wav");
    }

    @Override // documentation inherited
    protected void moveSprite (BangBoard board, Piece opiece, Piece npiece)
    {
        // no animating when we're in the editor
        if (_editorMode) {
            super.moveSprite(board, opiece, npiece);
            return;
        }

        // TODO: append an additional path if we're currently moving
        if (!isMoving()) {
            Path path = null;
            // only create a path if we're moving along the ground, if this is
            // solely an elevation move (which happens at the start of the
            // game), we just blip to our new location
            if (opiece.x != npiece.x || opiece.y != npiece.y) {
                path = createPath(board, opiece, npiece);
            }
            if (path != null) {
                // start looping our movement sound
                _moveSound.loop(false);
                move(path);
            } else {
                int elev = computeElevation(board, npiece.x, npiece.y);
                setLocation(npiece.x, npiece.y, elev);
            }
        }
    }
    
    /**
     * Configures the current set of meshes being used by this sprite.
     */
    protected Model.Animation setAction (String action)
    {
        // remove the old meshes
        if (_meshes != null) {
            for (int ii = 0; ii < _meshes.length; ii++) {
                detachChild(_meshes[ii]);
            }
        }

        // add the new meshes
        Model.Animation anim = _model.getAnimation(action);
        _meshes = anim.getMeshes(_texrando);
        for (int ii = 0; ii < _meshes.length; ii++) {
            attachChild(_meshes[ii]);
            _meshes[ii].updateRenderState();
            _meshes[ii].updateGeometricState(0, true);
        }
        setAnimationSpeed(Config.display.animationSpeed * anim.getSpeed());
        return anim;
    }

    /**
     * Pulls the next action off of our queue and runs it.
     */
    protected void startNextAction ()
    {
        _action = _actions.remove(0);
        if (_action.equals(REMOVED)) {
            // expire our fake action on the next frame and keep using our
            // previous action
            _nextAction = 0.001f;
        } else {
            Model.Animation anim = setAction(_action);
            _nextAction = anim.getDuration() / Config.display.animationSpeed;
            setAnimationActive(true);
        }
    }

    /**
     * Returns the default pose for this model when it is simply resting
     * on the board.
     */
    protected String getRestPose ()
    {
        return "standing";
    }

    /**
     * Creates a path that will be used to move this piece from the
     * specified old position to the new one.
     */
    protected Path createPath (BangBoard board, Piece opiece, Piece npiece)
    {
        List path = null;
        if (board != null) {
            path = board.computePath(opiece, npiece.x, npiece.y);
        }

        if (path != null) {
            if (path.size() < 2) {
                log.warning("Created short path? [opiece=" + opiece.info() +
                            ", npiece=" + npiece.info() +
                            ", path=" + StringUtil.toString(path) + "].");
                return null;
            }
            return createPath(board, path, Config.display.getMovementSpeed());

        } else {
            Vector3f start = toWorldCoords(opiece.x, opiece.y,
                computeElevation(board, opiece.x, opiece.y), new Vector3f());
            Vector3f end = toWorldCoords(npiece.x, npiece.y,
                computeElevation(board, npiece.x, npiece.y), new Vector3f());
            float duration = (float)MathUtil.distance(
                opiece.x, opiece.y, npiece.x, npiece.y) * .003f;
            return new LinePath(this, start, end, duration);
        }
    }
    
    /**
     * Creates a path from a list of {@link Point} objects.
     *
     * @param speed the speed at which to move, in tiles per second
     */
    protected Path createPath (BangBoard board, List path, float speed)
    {
        // create a world coordinate path from the tile
        // coordinates
        Vector3f[] coords = new Vector3f[path.size()];
        float[] durations = new float[path.size()-1];
        int ii = 0;
        for (Iterator iter = path.iterator(); iter.hasNext(); ii++) {
            Point p = (Point)iter.next();
            setCoord(board, coords, ii, p.x, p.y);
            if (ii > 0) {
                durations[ii-1] = 1f / speed;
            }
        }
        return new MoveUnitPath(this, coords, durations);
    }
    
    /**
     * Sets the coordinate in the given array at the specified index.
     */
    protected void setCoord (BangBoard board, Vector3f[] coords, int idx,
        int nx, int ny)
    {
        coords[idx] = new Vector3f();
        toWorldCoords(nx, ny, computeElevation(board, nx, ny), coords[idx]);
    }
    
    /**
     * Sets the orientation to the one stored in the piece.
     */
    protected void reorient ()
    {
        setOrientation(_piece.orientation);
    }
    
    /**
     * Updates the position of the highlight.
     */
    protected void updateHighlight ()
    {
        if (_highlight.x != localTranslation.x ||
            _highlight.y != localTranslation.y) {
            _highlight.setPosition(localTranslation.x, localTranslation.y);
        }
        
        _loc.set(localTranslation.x, localTranslation.y,
            localTranslation.z + TILE_SIZE/2);
        _view.getShadowLocation(_loc, _result);
        if (_shadow.x != _result.x || _shadow.y != _result.y) {
            _shadow.setPosition(_result.x, _result.y);
        }
    }
    
    /** Used to dispatch {@link ActionObserver#actionCompleted}. */
    protected static class CompletedOp implements ObserverList.ObserverOp
    {
        public CompletedOp (Sprite sprite, String action) {
            _sprite = sprite;
            _action = action;
        }

        public boolean apply (Object observer) {
            if (observer instanceof ActionObserver) {
                ((ActionObserver)observer).actionCompleted(_sprite, _action);
            }
            return true;
        }

        protected Sprite _sprite;
        protected String _action;
    }

    protected String _type, _name;
    protected Model _model;
    protected Node _hnode;
    protected TerrainNode.Highlight _highlight;
    protected TerrainNode.Highlight _shadow;
    protected Node[] _meshes;
    protected Sound _moveSound;

    protected String _action;
    protected float _nextAction;
    protected ArrayList<String> _actions = new ArrayList<String>();

    protected Vector3f _loc = new Vector3f();
    protected Vector2f _result = new Vector2f();
    
    /** Ensures that we use the same random texture for every animation
     * displayed for this particular instance. */
    protected int _texrando = RandomUtil.getInt(Integer.MAX_VALUE);

    protected static TextureState _shadtex;
    protected static float _slength, _srotation, _sintensity;
    
    /** The size of the shadow texture. */
    protected static final int SHADOW_TEXTURE_SIZE = 128;
}
