//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Point;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Geometry;
import com.jme.scene.Spatial;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jmex.effects.particles.ParticleFactory;
import com.jmex.effects.particles.ParticleMesh;

import com.samskivert.util.ObserverList;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.Colorization;
import com.threerings.media.util.MathUtil;
import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.Sprite;
import com.threerings.jme.sprite.SpriteObserver;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.Config;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;
import com.threerings.bang.util.SoundUtil;

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
     * should switch to its dead model when all other actions are complete. */
    public static final String DEAD = "__dead__";
    
    /** A fake action that is queued up to indicate that this sprite
     * should be removed when all other actions are completed. */
    public static final String REMOVED = "__removed__";

    /** Normal movement. */
    public static final int MOVE_NORMAL = 0;

    /** Pushed movement. */
    public static final int MOVE_PUSH = 1;
    
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
        return _model != null && _model.hasAnimation(action);
    }

    /**
     * Returns the underlying animation for the specified action.
     */
    public Model.Animation getAction (String action)
    {
        return (_model == null) ? null : _model.getAnimation(action);
    }

    /**
     * Returns an array containing the types of wreckage to be thrown from this
     * sprite when it blows up.
     */
    public String[] getWreckageTypes ()
    {
        return _wtypes;
    }
    
    /**
     * Called to inform us that we will be shooting the specified target
     * sprite when we finish our path.
     */
    public void willShoot (Piece target, PieceSprite tsprite)
    {
        _tsprite = tsprite;
    }

    /**
     * Returns a reference to the last targeted sprite.
     */
    public PieceSprite getTargetSprite ()
    {
        return _tsprite;
    }

    /**
     * Turns the sprite towards its current target.
     */
    public void faceTarget ()
    {
        if (_tsprite != null) {
            // use the vector to the target on the XY plane to determine the
            // heading, then adjust to the terrain slope
            Vector3f dir = _tsprite.getLocalTranslation().subtract(
                localTranslation).normalizeLocal();
            localRotation.fromAngleNormalAxis(
                FastMath.atan2(dir.x, -dir.y), Vector3f.UNIT_Z);
            snapToTerrain(false);
        }
    }

    /**
     * Runs the specified action animation.
     */
    public void queueAction (String action)
    {
        // log.info("Queueing action " + action + " on " + _piece.info() + ".");
        _actions.add(action);
        if (_action == null && !isMoving()) {
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
    public boolean isHoverable ()
    {
        return true;
    }

    @Override // documentation inherited
    public Shadow getShadowType ()
    {
        return Shadow.DYNAMIC;
    }

    /**
     * Sets the move type of the sprite.
     */
    public void setMoveType (int moveType)
    {
        _moveType = moveType;
    }

    /**
     * Get the move type of the sprite.
     */
    public int getMoveType ()
    {
        return _moveType;
    }

    /**
     * Sets this sprite on a path defined by a list of {@link Point} objects.
     *
     * @param speed the speed at which to move, in tiles per second
     */
    public void move (BangBoard board, List path, float speed)
    {
        move(createPath(board, path, speed));
        Point pt = (Point)path.get(path.size() - 1);
        _px = pt.x;
        _py = pt.y;
    }

    @Override // documentation inherited
    public void move (Path path)
    {
        super.move(path);

        // only activate sound/dust for unit paths
        if (!(path instanceof MoveUnitPath)) {
            return;
        }

        // start the movement sound
        if (_moveSound != null) {
            _moveSound.loop(false);
        }

        // turn on the dust
        if (_dust != null) {
            _dust.setReleaseRate(32);
        }
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();

        // stop our movement sound
        if (_moveSound != null) {
            _moveSound.stop();
        }

        // deactivate the dust
        if (_dust != null) {
            _dust.setReleaseRate(0);
        }
    }
    
    @Override // documentation inherited
    public void setOrientation (int orientation)
    {
        super.setOrientation(orientation);
        snapToTerrain(false);
    }

    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        // wait until we're done moving to do any actions or idle animations
        if (isMoving()) {
            super.updateWorldData(time);
            return;
        }
        
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

                // if we were removed, don't bother updating the action
                if (getParent() == null) {
                    return;
                }
                
                // start the next action if we have one, otherwise idle
                startNext();
            }
            
        } else if (_nextIdle > 0) {
            _nextIdle -= time;
            if (_nextIdle <= 0) {
                startNextIdle(false);
            }
        }

        super.updateWorldData(time);
    }

    /**
     * Called whenever this sprite is moved to a new point along a path.
     */
    public void pathUpdate ()
    {
        snapToTerrain(true);
        updateHighlight();
        updateShadowValue();
    }

    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        super.createGeometry(ctx);
        _ctx = ctx;

        // create the dust particle system
        createDustManager(ctx);
        
        // load our model
        loadModel(ctx, _type, _name, _zations);
    }

    @Override // documentation inherited
    protected void modelLoaded (BasicContext ctx, Model model)
    {
        super.modelLoaded(ctx, model);
        _wtypes = StringUtil.parseStringArray(
            _model.getProperties().getProperty("wreckage", ""));
        _ianims = StringUtil.parseStringArray(
            _model.getProperties().getProperty("idle", ""));
        startNextIdle(true);
    }
    
    /**
     * Creates the dust particle manager, if this unit kicks up dust.
     */
    protected void createDustManager (BasicContext ctx)
    {
        // flyers don't kick up dust for now; eventually, we may want to add
        // prop wash effects.  only show dust on high detail setting
        if (_piece.isFlyer() || !BangPrefs.isHighDetail()) {
            return;
        }

        _dust = ParticleFactory.buildParticles("dust", NUM_DUST_PARTICLES);
        _dust.setInitialVelocity(0.005f);
        _dust.setEmissionDirection(Vector3f.UNIT_Z);
        _dust.setMaximumAngle(FastMath.PI / 2);
        _dust.setMinimumLifeTime(500f);
        _dust.setMaximumLifeTime(1500f);
        _dust.setRandomMod(0f);
        _dust.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        _dust.getParticleController().setControlFlow(true);
        _dust.setReleaseRate(0);
        _dust.getParticleController().setReleaseVariance(0f);
        _dust.setParticleSpinSpeed(0.05f);
        _dust.setStartSize(TILE_SIZE / 5);
        _dust.setEndSize(TILE_SIZE / 3);
        _dust.setModelBound(new BoundingBox());
        _dust.setIsCollidable(false);
        if (_dusttex == null) {
            _dusttex = RenderUtil.createTextureState(
                ctx, "textures/effects/dust.png");
            _dusttex.getTexture().setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
        }
        _dust.setRenderState(_dusttex);
        _dust.setRenderState(RenderUtil.blendAlpha);
        _dust.setRenderState(RenderUtil.overlayZBuf);
        _dust.updateRenderState();

        // put them in the highlight node so that they are positioned relative
        // to the board
        attachHighlight(_dust);
    }

    @Override // documentation inherited
    protected void createSounds (SoundGroup sounds)
    {
        super.createSounds(sounds);

        // load up our movement sounds
        String spre = "rsrc/" + _type + "/" + _name;
        String spath = spre + "/move.wav";
        if (SoundUtil.haveSound(spath)) {
            _moveSound = sounds.getSound(spath);
        } else {
            log.info("No movement sound '" + spath + "'.");
        }

        // preload any associated sounds
        String[] preload = getPreloadSounds();
        int pcount = (preload == null) ? 0 : preload.length;
        for (int ii = 0; ii < pcount; ii++) {
            spath = spre + "/" + preload[ii] + ".wav";
            if (SoundUtil.haveSound(spath)) {
                sounds.preloadClip(spath);
            }
        }
    }

    /**
     * Returns an array of sound identifiers that will be preloaded for this
     * mobile sprite.
     */
    protected String[] getPreloadSounds ()
    {
        return null;
    }

    @Override // documentation inherited
    protected void moveSprite (BangBoard board)
    {
        // no animating when we're in the editor
        if (_editorMode) {
            super.moveSprite(board);
            return;
        }

        // TODO: append an additional path if we're currently moving?
        if (!isMoving()) {
            Path path = null;
            // only create a path if we're moving along the ground, if this is
            // solely an elevation move (which happens at the start of the
            // game), we just blip to our new location
            if (_px != _piece.x || _py != _piece.y) {
                path = createPath(board);
            }
            if (path != null) {
                move(path);
                _px = _piece.x;
                _py = _piece.y;
                
            } else {
                setLocation(board, _piece.x, _piece.y);
            }
        }
    }

    /**
     * Activates one of the model animations.
     *
     * @return the duration of the animation (for looping animations,
     * the duration of one cycle), or -1 if the action was not found
     */
    protected float setAction (String action)
    {
        return (_model == null || _dead) ?
            -1f : _model.startAnimation(action);
    }
    
    /**
     * Starts the next action or idle animation.
     */
    protected void startNext ()
    {
        if (_actions.size() > 0) {
            startNextAction();
        } else {
            startNextIdle(true);
        }
    }

    /**
     * Pulls the next action off of our queue and runs it.
     */
    protected void startNextAction ()
    {
        _action = _actions.remove(0);
        if (_action.equals(DEAD)) {
            loadModel(_ctx, _type, _name + "/dead", _zations);
            _dead = true;
            _action = null;
            
        } else if (_action.equals(REMOVED)) {
            // have the unit sink into the ground and fade away
            _nextAction = REMOVAL_DURATION;

            setRenderState(RenderUtil.blendAlpha);
            final MaterialState mstate =
                _ctx.getRenderer().createMaterialState();
            final ColorRGBA color = new ColorRGBA(ColorRGBA.white);
            mstate.setAmbient(color);
            mstate.setDiffuse(color);
            setRenderState(mstate);
            _shadow.setDefaultColor(color);

            Vector3f start = new Vector3f(localTranslation),
                finish = localRotation.mult(Vector3f.UNIT_Z);
            finish.multLocal(-TILE_SIZE).addLocal(localTranslation);
            move(new LinePath(this, start, finish, REMOVAL_DURATION) {
                public void update (float time) {
                    super.update(time);
                    color.a -= time / REMOVAL_DURATION;
                }
            });
            
        } else {
            // cope when we have units disabled for debugging purposes
            if ((_nextAction = setAction(_action)) < 0f) {
                _nextAction = 0.5f;
            }
        }
    }

    /**
     * Starts the next idle animation (if any) and schedules the one after
     * that.
     *
     * @param offset if true, start at a random offset into the animation to
     * make sure idle animations aren't synchronized between units
     */
    protected void startNextIdle (boolean offset)
    {
        // if there's one idle animation, assume that it loops; if there
        // are many, cycle randomly between them
        float duration = -1f;
        if (_ianims == null || _ianims.length == 0) {
            if (_model != null) {
                _model.stopAnimation();
            }
            _idle = null;
            _nextIdle = Float.MAX_VALUE;
            
        } else if (_ianims.length == 1) {
            duration = setAction(_idle = _ianims[0]);
            _nextIdle = Float.MAX_VALUE;
            
        } else if (_ianims.length > 1) {
            _idle = (String)RandomUtil.pickRandom(_ianims, _idle);
            duration = _nextIdle = setAction(_idle);
        }
        if (duration > 0f && offset) {
            float time = RandomUtil.getFloat(duration);
            _nextIdle -= time;
            _model.fastForwardAnimation(time);
        }
    }
    
    /**
     * Creates a path that will be used to move this piece from the
     * specified old position to the new one.
     */
    protected Path createPath (BangBoard board)
    {
        List path = null;
        if (board != null) {
            path = board.computePath(_px, _py, _piece);
        }

        if (path != null) {
            if (path.size() < 2) {
                log.warning("Created short path? [piece=" + _piece.info() +
                            ", path=" + StringUtil.toString(path) + "].");
                return null;
            }
            // if we're dead, take our final path in slow motion (this only
            // happens to flying units that are heading somewhere to explode)
            float speed = Config.getMovementSpeed();
            if (!_piece.isAlive()) {
                speed /= 2;
            }
            return createPath(board, path, speed);

        } else {
            Vector3f start = toWorldCoords(
                _px, _py, _piece.computeElevation(board, _px, _py),
                new Vector3f());
            Vector3f end = toWorldCoords(_piece.x, _piece.y,
                _piece.computeElevation(board, _piece.x, _piece.y),
                new Vector3f());
            float duration = (float)MathUtil.distance(
                _px, _py, _piece.x, _piece.y) * .003f;
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
            setCoord(board, coords, ii, p.x, p.y, (ii > 0 && iter.hasNext()));
            if (ii > 0) {
                durations[ii-1] = 1f / speed;
            }
        }
        String action = null;
        if (_moveType == MOVE_PUSH) {
            action = "reacting";
        }
        return new MoveUnitPath(this, coords, durations, action);
    }

    /**
     * Sets the coordinate in the given array at the specified index.
     */
    protected void setCoord (BangBoard board, Vector3f[] coords, int idx, 
                             int nx, int ny, boolean moving)
    {
        coords[idx] = new Vector3f();
        toWorldCoords(nx, ny, _piece.computeElevation(board, nx, ny, moving),
            coords[idx]);
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
        super.updateHighlight();
        
        if (_dust != null && isMoving()) {
            _dust.getOriginOffset().set(localTranslation);
            ColorRGBA start = _dust.getStartColor();
            _view.getDustColor(localTranslation, start);
            _dust.getEndColor().set(start.r, start.g, start.b, 0f);
        }
    }

    /** Used to dispatch {@link ActionObserver#actionCompleted}. */
    protected static class CompletedOp
        implements ObserverList.ObserverOp<SpriteObserver>
    {
        public CompletedOp (Sprite sprite, String action) {
            _sprite = sprite;
            _action = action;
        }

        public boolean apply (SpriteObserver observer) {
            if (observer instanceof ActionObserver) {
                ((ActionObserver)observer).actionCompleted(_sprite, _action);
            }
            return true;
        }

        protected Sprite _sprite;
        protected String _action;
    }

    protected BasicContext _ctx;

    protected String _type, _name;
    protected String[] _wtypes;
    protected String[] _ianims;
    
    protected ParticleMesh _dust;
    protected Sound _moveSound;

    protected String _action, _idle;
    protected float _nextAction, _nextIdle;
    protected ArrayList<String> _actions = new ArrayList<String>();

    protected PieceSprite _tsprite;

    protected int _moveType = MOVE_NORMAL;

    /** The colorizations to use for this sprite's textures. */
    protected Colorization[] _zations;
    
    /** Whether or not we have switched to the dead model. */
    protected boolean _dead;
        
    protected static TextureState _dusttex;

    /** The number of dust particles. */
    protected static final int NUM_DUST_PARTICLES = 32;

    /** The number of seconds it takes dead pieces to fade out. */
    protected static final float REMOVAL_DURATION = 3f;
}
