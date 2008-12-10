//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Point;

import java.util.Iterator;
import java.util.List;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.state.TextureState;
import com.jmex.effects.particles.ParticleFactory;
import com.jmex.effects.particles.ParticleMesh;

import com.threerings.media.util.MathUtil;
import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Path;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.Config;
import com.threerings.bang.util.RenderUtil;
import com.threerings.bang.util.SoundUtil;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.DuplicateEffect;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays some sort of mobile entity on the board (generally a unit or
 * some other piece that moves around and is interacted with).
 */
public class MobileSprite extends ActiveSprite
{
    /** Queued up when the sprite is teleported out of existence. */
    public static final String TELEPORTED_OUT = "__teleported_out__";

    /** Queued up when the sprite is teleported back into existence. */
    public static final String TELEPORTED_IN = "__teleported_in__";

    /** Normal movement. */
    public static final int MOVE_NORMAL = 0;

    /** Pushed movement. */
    public static final int MOVE_PUSH = 1;

    /** Walking movement. */
    public static final String MOVE_WALKING = "walking";

    /** Flying movement. */
    public static final String MOVE_FLYING = "flying";

    /**
     * Creates a mobile sprite with the specified model type and name.
     */
    public MobileSprite (String type, String name)
    {
        super(type, name);
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

    @Override // documentation inherited
    public Shadow getShadowType ()
    {
        return Shadow.DYNAMIC;
    }

    /**
     * Sets the move action of the sprite.
     */
    public void setMoveAction (int moveAction)
    {
        _moveAction = moveAction;
    }

    /**
     * Get the move action of the sprite.
     */
    public int getMoveAction ()
    {
        return _moveAction;
    }

    /**
     * Sets the move type of the sprite.
     */
    public void setMoveType (String moveType)
    {
        _moveType = moveType;
    }

    /**
     * Get the move type of the sprite.
     */
    public String getMoveType ()
    {
        return _moveType;
    }

    /**
     * Sets this sprite on a path defined by a list of {@link Point} objects.
     *
     * @param speed the speed at which to move, in tiles per second
     */
    public void move (BangBoard board, List<Point> path, float speed)
    {
        move(createPath(board, path, speed));
        Point pt = path.get(path.size() - 1);
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
        startMoveSound();

        // turn on the dust
        if (_dust != null) {
            _dust.setReleaseRate(32);
        }
    }

    @Override // documentation inherited
    public void cancelMove ()
    {
        super.cancelMove();

        moveEnded();
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();

        moveEnded();
    }

    @Override // documentation inherited
    public void setOrientation (int orientation)
    {
        super.setOrientation(orientation);
        snapToTerrain(false);
    }

    @Override // documentation inherited
    public void snapToTerrain (boolean moving)
    {
        super.snapToTerrain(moving);

        // flyers simply fly from point to point
        if (moving ? _piece.isFlyer() : _piece.isAirborne()) {
            return;
        }

        // if we're walking on a prop, possibly adjust our height
        Vector3f pos = getLocalTranslation();
        int tx = (int)(pos.x / TILE_SIZE);
        int ty = (int)(pos.y / TILE_SIZE);
        BangBoard board = _view.getBoard();
        int elev = board.getPieceElevation(tx, ty);
        if (elev > 0 && !board.isBridge(tx, ty)) {
            pos.z += elev * board.getElevationScale(TILE_SIZE);
            setLocalTranslation(pos);
        }
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
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);

        if (!isMoving()) {
            checkMoveSound("not moving");
        }
    }

    /**
     * Called to ensure that the move sound has stopped playing.
     */
    public void checkMoveSound (String msg)
    {
        if (_moveSound != null && _moveSound.isPlaying()) {
            log.warning("Move sound playing when " + msg, "piece", _piece);
            stopMoveSound();
        }
    }

    /**
     * Adds any procedural actions for this sprite.
     */
    protected void addProceduralActions ()
    {
        super.addProceduralActions();
        _procActions.put(AddPieceEffect.RESPAWNED, new ProceduralAction() {
            public float activate () {
                // fade the unit in and display the resurrection effect
                if (BangPrefs.isMediumDetail()) {
                    displayParticles("frontier_town/resurrection", false);
                }
                startRiseFade(-TILE_SIZE * 0.5f, true, RESPAWN_DURATION);
                return RESPAWN_DURATION;
            }
        });
        _procActions.put(DuplicateEffect.DUPLICATED, new ProceduralAction() {
            public float activate () {
                // fade the unit in (TODO: display dust cloud)
                startRiseFade(-TILE_SIZE * 0.5f, true, RESPAWN_DURATION);
                return RESPAWN_DURATION;
            }
        });
        _procActions.put(TELEPORTED_OUT, new ProceduralAction() {
            public float activate () {
                startRiseFade(TILE_SIZE * 0.5f, false, TELEPORT_DURATION);
                return TELEPORT_DURATION;
            }
        });
        _procActions.put(TELEPORTED_IN, new ProceduralAction() {
            public float activate () {
                startRiseFade(TILE_SIZE * 0.5f, true, TELEPORT_DURATION);
                return TELEPORT_DURATION;
            }
        });
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();

        // create the dust particle system
        createDustManager();
    }

    /**
     * Creates the dust particle manager, if this unit kicks up dust.
     */
    protected void createDustManager ()
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
        _dust.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        _dust.getParticleController().setControlFlow(true);
        _dust.setReleaseRate(0);
        _dust.getParticleController().setReleaseVariance(0f);
        _dust.setParticleSpinSpeed(0.05f);
        _dust.setStartSize(TILE_SIZE / 5);
        _dust.setEndSize(TILE_SIZE / 3);
        _view.addWindInfluence(_dust);
        _dust.setModelBound(new BoundingBox());
        _dust.setIsCollidable(false);
        if (_dusttex == null) {
            _dusttex = RenderUtil.createTextureState(
                _ctx, "textures/effects/dust.png");
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
        checkMoveSound("creating sounds");

        // load up our movement sounds
        String spath = "rsrc/" + getMoveSound();
        if (SoundUtil.haveSound(spath)) {
            _moveSound = sounds.getSound(spath);
        } else {
            log.info("No movement sound '" + spath + "'.");
        }

        // preload any associated sounds
        String spre = "rsrc/" + _type + "/" + _name;
        String[] preload = getPreloadSounds();
        int pcount = (preload == null) ? 0 : preload.length;
        for (int ii = 0; ii < pcount; ii++) {
            spath = spre + "/" + preload[ii] + ".ogg";
            if (SoundUtil.haveSound(spath)) {
                sounds.preloadClip(spath);
            }
        }
    }

    /**
     * Returns the path to the sound to play when this sprite moves. This is
     * assumed to be relative to rsrc/.
     */
    protected String getMoveSound ()
    {
        return _type + "/" + _name + "/move.ogg";
    }

    /**
     * Starts playing the move sound in a loop.
     */
    protected void startMoveSound ()
    {
        if (_moveSound != null) {
            _moveSound.loop(false);
        }
    }

    /**
     * Stops looping the move sound.
     */
    protected void stopMoveSound ()
    {
        if (_moveSound != null) {
            _moveSound.stop();
        }
    }

    /**
     * Called to stop associated move effects.
     */
    protected void moveEnded ()
    {
        // stop our movement sound
        stopMoveSound();

        // deactivate the dust
        if (_dust != null) {
            _dust.setReleaseRate(0);
        }
    }

    /**
     * Returns an array of sound identifiers that will be preloaded for this
     * mobile sprite. These are assumed to be relative to rsrc/type/name/.
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
            if ((_px != _piece.x || _py != _piece.y) && !_fastAnimation) {
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
     * Creates a path that will be used to move this piece from the
     * specified old position to the new one.
     */
    protected Path createPath (BangBoard board)
    {
        List<Point> path = null;
        if (board != null) {
            path = board.computePath(_px, _py, _piece, _moveAction != MOVE_PUSH);
        }

        if (path != null) {
            if (path.size() < 2) {
                log.warning("Created short path?", "piece", _piece,
                            "from", "(" + _px + ", " + _py + ")", "path", path);
                // fall through and create a line path

            } else {
                // if we're dead, take our final path in slow motion (this only
                // happens to flying units that are heading somewhere to
                // explode)
                float speed = Config.getMovementSpeed();
                if (!_piece.isAlive()) {
                    speed /= 2;
                }
                return createPath(board, path, speed);
            }
        }

        Vector3f start = toWorldCoords(
            _px, _py, _piece.computeElevation(board, _px, _py), new Vector3f());
        Vector3f end = toWorldCoords(
            _piece.x, _piece.y, _piece.computeElevation(board, _piece.x, _piece.y), new Vector3f());
        float duration = MathUtil.distance(_px, _py, _piece.x, _piece.y) * .003f;
        return new LinePath(this, start, end, duration);
    }

    /**
     * Creates a path from a list of {@link Point} objects.
     *
     * @param speed the speed at which to move, in tiles per second
     */
    protected Path createPath (BangBoard board, List<Point> path, float speed)
    {
        // create a world coordinate path from the tile
        // coordinates
        Vector3f[] coords = new Vector3f[path.size()];
        float[] durations = new float[path.size()-1];
        int ii = 0;
        for (Iterator<Point> iter = path.iterator(); iter.hasNext(); ii++) {
            Point p = iter.next();
            setCoord(board, coords, ii, p.x, p.y, (ii > 0 && iter.hasNext()));
            if (ii > 0) {
                durations[ii-1] = 1f / speed;
            }
        }
        String action = null;
        if (_moveAction == MOVE_PUSH) {
            action = "reacting";
        }
        return createPath(coords, durations, action);
    }

    protected Path createPath (Vector3f[] coords, float[] durations, String action)
    {
        return new MoveUnitPath(this, coords, durations, _moveType, action);
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

    protected ParticleMesh _dust;
    protected Sound _moveSound;
    protected PieceSprite _tsprite;

    protected int _moveAction = MOVE_NORMAL;
    protected String _moveType = MOVE_WALKING;

    protected static TextureState _dusttex;

    /** The number of dust particles. */
    protected static final int NUM_DUST_PARTICLES = 32;

    /** The number of seconds it takes new pieces to fade in. */
    protected static final float RESPAWN_DURATION = 1f;

    /** The number of seconds it takes pieces to teleport in or out. */
    protected static final float TELEPORT_DURATION = 0.5f;
}
