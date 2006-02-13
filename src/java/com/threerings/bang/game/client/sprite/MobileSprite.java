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
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jmex.effects.ParticleManager;

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
import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.Terrain;
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
    public void init (BasicContext ctx, BoardView view, BangBoard board,
                      SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        updateHighlight();
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
        move(createPath(board, path, speed));
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
                    setAnimationActive(false);
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

        // contains highlights draped over terrain
        _hnode = new Node("highlight");
        _hnode.setLightCombineMode(LightState.OFF);
        _hnode.setRenderState(RenderUtil.overlayZBuf);
        _hnode.setRenderState(RenderUtil.blendAlpha);
        _hnode.setRenderState(RenderUtil.backCull);
        _hnode.updateRenderState();

        // the geometry of the highlight is shared between the elements
        _highlight = _view.getTerrainNode().createHighlight(localTranslation.x,
            localTranslation.y, TILE_SIZE, TILE_SIZE);

        // the shadow is an additional highlight with wider geometry
        createShadow(ctx);

        // create the dust particle system
        createDustManager(ctx);
        
        // load our model
        _model = ctx.loadModel(_type, _name);
        _model.resolveActions();

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
        _hnode.attachChild(_dustmgr.getParticles());
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
    protected void moveSprite (BangBoard board)
    {
        // no animating when we're in the editor
        if (_editorMode) {
            super.moveSprite(board);
            return;
        }

        // TODO: append an additional path if we're currently moving
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
            } else {
                int elev = computeElevation(board, _piece.x, _piece.y);
                setLocation(_piece.x, _piece.y, elev);
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
        Model.Animation anim = _model.getAnimation(action);
        bindAnimation(_ctx, anim, _texrando);
        return anim;
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
            final MaterialState mstate =
                _ctx.getRenderer().createMaterialState();
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
                    updateRenderState();
                }
            });

        } else if (_action.equals("dying") && !hasAction("dying")) {
            // burst into pieces of wreckage
            
            
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
            return createPath(board, path, Config.display.getMovementSpeed());

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
    protected Node _hnode;
    protected TerrainNode.Highlight _highlight;
    protected TerrainNode.Highlight _shadow;
    protected ParticleManager _dustmgr;
    protected Sound _moveSound;

    protected String _action;
    protected float _nextAction;
    protected ArrayList<String> _actions = new ArrayList<String>();

    protected Vector3f _loc = new Vector3f();
    protected Vector2f _result = new Vector2f();

    protected PieceSprite _tsprite;

    /** Ensures that we use the same random texture for every animation
     * displayed for this particular instance. */
    protected int _texrando = RandomUtil.getInt(Integer.MAX_VALUE);

    protected static TextureState _shadtex, _dusttex;
    protected static float _slength, _srotation, _sintensity;

    /** The size of the shadow texture. */
    protected static final int SHADOW_TEXTURE_SIZE = 128;

    /** The number of dust particles. */
    protected static final int NUM_DUST_PARTICLES = 32;

    /** The number of seconds it takes dead pieces to fade out. */
    protected static final float REMOVAL_DURATION = 3f;
}
