//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Point;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Geometry;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jmex.effects.ParticleManager;

import com.samskivert.util.ObserverList;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.Colorization;
import com.threerings.media.util.MathUtil;
import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;
import com.threerings.util.RandomUtil;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.Sprite;
import com.threerings.jme.sprite.SpriteObserver;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.Terrain;
import com.threerings.bang.game.data.piece.Piece;

import com.threerings.bang.client.Config;
import com.threerings.bang.client.Model;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.ColorMaterialState;
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
        // use the vector to the target on the XY plane to determine
        // the heading, then adjust to the terrain slope
        Vector3f dir = _tsprite.getLocalTranslation().subtract(
            localTranslation).normalizeLocal();
        localRotation.fromAngleNormalAxis(FastMath.atan2(dir.x, -dir.y),
            Vector3f.UNIT_Z);
        snapToTerrain();
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
        _moveSound.loop(false);

        // turn on the dust
        if (_dustmgr != null) {
            _dustmgr.setReleaseRate(32);
        }
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();

        // stop our movement sound
        _moveSound.stop();

        // deactivate the dust
        if (_dustmgr != null) {
            _dustmgr.setReleaseRate(0);
        }
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

                // if we were removed, don't bother updating the action
                if (getParent() == null) {
                    return;
                }
                
                // start the next action if we have one, otherwise rest
                if (_actions.size() > 0) {
                    startNextAction();
                } else {
                    setAction(getRestPose());
                }
            }
        }

        super.updateWorldData(time);
    }

    /**
     * Called whenever this sprite is moved to a new point along a path.
     */
    public void pathUpdate ()
    {
        snapToTerrain();
        updateHighlight();
        updateShadowValue();
    }

    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        super.createGeometry(ctx);
        _ctx = ctx;

        // the geometry of the highlight is shared between the elements
        _highlight = _view.getTerrainNode().createHighlight(
            localTranslation.x, localTranslation.y, TILE_SIZE, TILE_SIZE);

        // create the dust particle system
        createDustManager(ctx);
        
        // load our model
        _model = ctx.loadModel(_type, _name);
        _model.resolveActions();
        _wtypes = StringUtil.parseStringArray(
            _model.getProperties().getProperty("wreckage", ""));

        // start in our rest post
        setAction(getRestPose());
    }

    /**
     * Creates the dust particle manager, if this unit kicks up dust.
     */
    protected void createDustManager (BasicContext ctx)
    {
        // flyers don't kick up dust for now; eventually, we may want to add
        // prop wash effects
        if (_piece.isFlyer()) {
            return;
        }

        _dustmgr = new ParticleManager(NUM_DUST_PARTICLES);
        _dustmgr.setInitialVelocity(0.005f);
        _dustmgr.setEmissionDirection(Vector3f.UNIT_Z);
        _dustmgr.setEmissionMaximumAngle(FastMath.PI / 2);
        _dustmgr.setParticlesMinimumLifeTime(500f);
        _dustmgr.setRandomMod(0f);
        _dustmgr.setPrecision(FastMath.FLT_EPSILON);
        _dustmgr.setControlFlow(true);
        _dustmgr.setReleaseRate(0);
        _dustmgr.setReleaseVariance(0f);
        _dustmgr.setParticleSpinSpeed(0.05f);
        _dustmgr.setStartSize(TILE_SIZE / 5);
        _dustmgr.setEndSize(TILE_SIZE / 3);
        if (_dusttex == null) {
            _dusttex = RenderUtil.createTextureState(
                ctx, "textures/effects/dust.png");
        }
        _dustmgr.getParticles().setRenderState(_dusttex);
        _dustmgr.getParticles().setRenderState(RenderUtil.blendAlpha);
        _dustmgr.getParticles().setRenderState(RenderUtil.overlayZBuf);
        _dustmgr.getParticles().updateRenderState();
        _dustmgr.getParticles().addController(_dustmgr);

        // put them in the highlight node so that they are positioned relative
        // to the board
        attachHighlight(_dustmgr.getParticles());
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
     * Configures the current set of meshes being used by this sprite.
     */
    protected Model.Animation setAction (String action)
    {
        // remove the old meshes
        if (_binding != null) {
            _binding.detach();
        }
        // add the new meshes
        return bindAnimation(_ctx, _model, action, _texrando, _zations);
    }

    /**
     * Pulls the next action off of our queue and runs it.
     */
    protected void startNextAction ()
    {
        _action = _actions.remove(0);
        if (_action.equals(REMOVED)) {
            // have the unit sink into the ground and fade away
            _nextAction = REMOVAL_DURATION;

            setRenderState(RenderUtil.blendAlpha);
            final MaterialState mstate = new ColorMaterialState();
            final ColorRGBA color = new ColorRGBA(ColorRGBA.white);
            mstate.setAmbient(color);
            setRenderState(mstate);
            _shadow.setDefaultColor(color);

            Vector3f start = new Vector3f(localTranslation),
                finish = localRotation.mult(Vector3f.UNIT_Z);
            finish.multLocal(-TILE_SIZE).addLocal(localTranslation);
            move(new LinePath(this, start, finish, REMOVAL_DURATION) {
                public void update (float time) {
                    super.update(time);
                    color.a -= time / REMOVAL_DURATION;
                    mstate.getDiffuse().a = color.a;
                    mstate.getSpecular().a = color.a;
                }
            });

        } else {
            Model.Animation anim = setAction(_action);
            _nextAction = anim.getDuration() / Config.display.animationSpeed;
        }
    }

    /**
     * Returns the default pose for this model when it is simply resting
     * on the board.
     */
    protected String getRestPose ()
    {
        return _piece.isAlive() ? "standing" : "dead";
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
            float speed = Config.display.getMovementSpeed();
            if (!_piece.isAlive()) {
                speed /= 2;
            }
            return createPath(board, path, speed);

        } else {
            Vector3f start = toWorldCoords(
                _px, _py, computeElevation(board, _px, _py), new Vector3f());
            Vector3f end = toWorldCoords(
                _piece.x, _piece.y, computeElevation(board, _piece.x, _piece.y),
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
    protected void setCoord (
        BangBoard board, Vector3f[] coords, int idx, int nx, int ny)
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
        super.updateHighlight();

        if (_highlight.x != localTranslation.x ||
            _highlight.y != localTranslation.y) {
            _highlight.setPosition(localTranslation.x, localTranslation.y);
        }

        if (_dustmgr != null && isMoving()) {
            _dustmgr.getParticlesOrigin().set(localTranslation);
            int tx = (int)(localTranslation.x / TILE_SIZE),
                ty = (int)(localTranslation.y / TILE_SIZE);
            Terrain terrain = _view.getBoard().getPredominantTerrain(tx, ty);
            ColorRGBA color = RenderUtil.getGroundColor(terrain);
            _dustmgr.getStartColor().set(color.r, color.g, color.b,
                terrain.dustiness);
            _dustmgr.getEndColor().set(color.r, color.g, color.b, 0f);
        }
    }

    /** Computes the world space location of the named emitter. */
    protected Vector3f getEmitterTranslation (String name)
    {
        if (_binding == null) {
            return Vector3f.ZERO;
        }
        Geometry geom = _binding.getMarker(name);
        if (geom == null) {
            return Vector3f.ZERO;
        }
        return geom.getWorldBound().getCenter();
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

    protected BasicContext _ctx;

    protected String _type, _name;
    protected String[] _wtypes;
    protected TerrainNode.Highlight _highlight;
    protected ParticleManager _dustmgr;
    protected Sound _moveSound;

    protected String _action;
    protected float _nextAction;
    protected ArrayList<String> _actions = new ArrayList<String>();

    protected PieceSprite _tsprite;

    /** Ensures that we use the same random texture for every animation
     * displayed for this particular instance. */
    protected int _texrando = RandomUtil.getInt(Integer.MAX_VALUE);

    /** The colorizations to use for this sprite's textures. */
    protected Colorization[] _zations;
    
    protected static TextureState _dusttex;

    /** The number of dust particles. */
    protected static final int NUM_DUST_PARTICLES = 32;

    /** The number of seconds it takes dead pieces to fade out. */
    protected static final float REMOVAL_DURATION = 3f;
}
