//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.intersection.PickData;
import com.jme.intersection.TrianglePickResults;
import com.jme.light.DirectionalLight;
import com.jme.light.PointLight;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.util.geom.BufferUtils;

import com.jme.scene.Controller;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Skybox;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.lod.AreaClodMesh;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;

import com.jmex.bui.BComponent;
import com.jmex.bui.BLabel;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.text.BText;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseMotionListener;
import com.jmex.bui.layout.BorderLayout;

import com.jmex.sound.openAL.objects.MusicStream;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.ObserverList;
import com.threerings.util.MessageBundle;

import com.threerings.jme.effect.FadeInOutEffect;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;
import com.threerings.jme.util.LinearTimeFunction;
import com.threerings.jme.util.TimeFunction;
import com.threerings.openal.SoundGroup;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.Model;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.PropSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.Terrain;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Provides a shared base for displaying the board that is extended to
 * create the actual game view as well as the level editor.
 */
public class BoardView extends BComponent
    implements MouseMotionListener
{
    /**
     * Used to queue up board actions that must execute sequentially before
     * subsequent actions can start (pieces moving, pieces firing at one
     * another, etc.).
     */
    public abstract static class BoardAction
    {
        /** Configured to the time at which this action was started. */
        public long start;

        /** The board action must fill in this array with the ids of all pieces
         * affected by the action. This will be used to ensure that a piece is
         * only involved in one board action at a time. */
        public int[] pieceIds;

        /** The board action must fill in this array with the ids of any pieces
         * that are not affected but that we need to wait for before executing
         * this action. */
        public int[] waiterIds;

        /** Returns true if this action can be executed, false if it operates
         * on a piece that is currently involved in another action. */
        public boolean canExecute (ArrayIntSet penders)
        {
            for (int ii = 0; ii < pieceIds.length; ii++) {
                if (penders.contains(pieceIds[ii])) {
                    return false;
                }
            }
            for (int ii = 0; ii < waiterIds.length; ii++) {
                if (penders.contains(waiterIds[ii])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Executes this board action.
         *
         * @return true if we should wait for a call to {@link
         * #actionCompleted}. Prior to executing the next action, false if not.
         */
        public abstract boolean execute ();

        /** Returns a string representation of this action. */
        public String toString () {
            String cname = getClass().getName();
            return cname.substring(cname.lastIndexOf("$")+1) + ":" + hashCode();
        }
    }

    public BoardView (BasicContext ctx)
    {
        setStyleClass("board_view");
        _ctx = ctx;

        // create our top-level node
        _node = new Node("board_view");

        // let there be lights
        _lstate = _ctx.getRenderer().createLightState();
        _lights = new DirectionalLight[BangBoard.NUM_LIGHTS];
        for (int i = 0; i < _lights.length; i++) {
            _lstate.attach(_lights[i] = new DirectionalLight());
            _lights[i].setEnabled(true);
        }
        _node.setRenderState(_lstate);
        _node.setLightCombineMode(LightState.REPLACE);

        // default states
        MaterialState mstate = ctx.getRenderer().createMaterialState();
        mstate.setDiffuse(ColorRGBA.white);
        mstate.setAmbient(ColorRGBA.white);
        _node.setRenderState(RenderUtil.createColorMaterialState(mstate,
            false));
        _node.setRenderState(RenderUtil.lequalZBuf);
        _node.setRenderState(RenderUtil.opaqueAlpha);
        _node.setTextureCombineMode(TextureState.REPLACE);
        _node.updateRenderState();

        // create the sky
        _node.attachChild(_snode = new SkyNode(ctx));

        // we'll hang the board geometry off this node
        Node bnode = new Node("board");
        _node.attachChild(bnode);
        bnode.attachChild(_tnode = createTerrainNode(ctx));
        bnode.attachChild(_wnode = new WaterNode(ctx));

        // the children of this node will display highlighted tiles
        bnode.attachChild(_hnode = new Node("highlights"));
        _hnode.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);

        // we'll hang all of our pieces off this node
        _node.attachChild(_pnode = new Node("pieces"));

        // create our highlight alpha state
        _hastate = ctx.getDisplay().getRenderer().createAlphaState();
        _hastate.setBlendEnabled(true);
        _hastate.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        _hastate.setDstFunction(AlphaState.DB_ONE);
        _hastate.setEnabled(true);

        // this is used to target tiles when deploying a card
        _tgtstate = RenderUtil.createTextureState(
            ctx, "textures/ustatus/crosshairs.png");

        // this is used to indicate where you can move
        _movstate = RenderUtil.createTextureState(
            ctx, "textures/ustatus/movement.png");

        // create a sound group that we'll use for all in-game sounds
        _sounds = ctx.getSoundManager().createGroup(
            BangUI.clipprov, GAME_SOURCE_COUNT);

        // create a paused fade in effect, we'll do our real fading in once
        // everything is loaded up and we're ready to show the board
        createPausedFadeIn();
    }

    /**
     * Returns our top-level geometry node.
     */
    public Node getNode ()
    {
        return _node;
    }

    /**
     * Sets up the board view with all the necessary bits. This is called
     * by the controller when we enter an already started game or the game
     * in which we're involved gets started.
     */
    public void prepareForRound (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // add the listener that will react to pertinent events
        _bangobj = bangobj;
        _bangobj.addListener(_blistener);

        // freshen up
        refreshBoard();
    }

    /**
     * Called by the editor when the entire board has changed.
     */
    public void refreshBoard ()
    {
        // remove any old sprites
        removePieceSprites();

        // start afresh
        _board = _bangobj.board;
        _board.shadowPieces(_bangobj.pieces.iterator());
        _bbounds = new Rectangle(0, 0, _board.getWidth(), _board.getHeight());

        // create a marquee with our board name if we've got one
        if (_bangobj.boardName != null) {
            createMarquee(_bangobj.boardName);
        }

        // refresh the lights
        refreshLights();

        // create the board geometry
        _snode.createBoardSky(_board);
        _tnode.createBoardTerrain(_board);
        _wnode.createBoardWater(_board);

        // display the tile grid if appropriate
        if (shouldShowGrid()) {
            if (_grid != null && _grid.getParent() != null) {
                _grid.removeFromParent();
            }
            _grid = null;
            toggleGrid(false);
        }

        // create sprites for all of the pieces
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            // create sprites for all of the pieces on the board
            createPieceSprite((Piece)iter.next(), _bangobj.tick);
        }
        _pnode.updateGeometricState(0, true);

        // create a loading marquee to report loading progress
        if (_toLoad > 0) {
            updateLoadingMarquee();
        }

        // fade the board in when the sprites are all resolved
        addResolutionObserver(new ResolutionObserver() {
            public void mediaResolved () {
                if (_loading != null) {
                    _ctx.getInterface().detachChild(_loading);
                    _loading = null;
                    _loaded = _toLoad = 0;
                }
                if (_fadein != null) {
                    _fadein.setPaused(false);
                }
            }
        });
        
        // load and start the opening music stream
        String mpath = getOpeningMusicPath();
        if (mpath != null) {
            try {
                _mstream = new MusicStream(
                    _ctx.getResourceManager().getResourceFile(mpath).toString(),
                    false);
                _mstream.play();
            } catch (Throwable t) {
                log.log(Level.WARNING, "Failed to start music " +
                        "[path=" + mpath + "].", t);
            }
        }
    }
    
    /**
     * Shows or hides the tile grid.
     */
    public void toggleGrid (boolean persistent)
    {
        if (_grid == null || _grid.getParent() == null) {
            updateGrid();
            _node.attachChild(_grid);
        } else {
            _grid.removeFromParent();
        }
        if (persistent) {
            BangPrefs.config.setValue("show_grid", _grid.getParent() != null);
        }
    }

    /**
     * Informs the board that this sprite is currently resolving.
     */
    public void addResolvingSprite (PieceSprite resolver)
    {
        _resolvingSprites++;
        _toLoad++;
    }

    /**
     * Informs the board that this sprite is finished resolving.
     */
    public void clearResolvingSprite (PieceSprite resolved)
    {
        _resolvingSprites--;
        _loaded++;

        // update our loading marquee
        if (_loading != null) {
            updateLoadingMarquee();
        }

        // if we're done resolving, notify any resolution observers
        if (_resolvingSprites == 0 && _resolutionObs.size() > 0) {
            _resolutionObs.apply(new ObserverList.ObserverOp() {
                public boolean apply (Object observer) {
                    ((ResolutionObserver)observer).mediaResolved();
                    return false; // clear the observer
                }
            });
        }
    }

    /**
     * Returns the length of shadows cast by tile-sized objects, which depends
     * on the elevation of the primary light.
     */
    public float getShadowLength ()
    {
        float elevation = _board.getLightElevation(0);
        if (elevation == FastMath.HALF_PI || elevation <= 0f) {
            return TILE_SIZE;

        } else {
            return Math.min(TILE_SIZE + TILE_SIZE / FastMath.tan(elevation),
                MAX_SHADOW_LENGTH);
        }
    }

    /**
     * Returns the rotation required to rotate shadows in the direction of the
     * primary light.
     */
    public float getShadowRotation ()
    {
        return _board.getLightAzimuth(0);
    }

    /**
     * Returns the intensity of shadows on the board, which depends on the
     * total light and the board's configured shadow intensity.
     *
     * @return 0.0 for no shadows, 1.0 for completely black shadows
     */
    public float getShadowIntensity ()
    {
        float total = 0f;
        float[] hsb = new float[3];
        for (int i = 0; i < BangBoard.NUM_LIGHTS; i++) {
            Color c = new Color(_board.getLightAmbientColor(i));
            Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
            total += hsb[2];
        }
        return 0.7f * _board.getShadowIntensity() * Math.max(0f, 1f - total);
    }

    /**
     * Given the location of the center of a shadow caster and the azimuth and
     * elevation of the primary light source, computes the location of the
     * shadow on the ground.
     *
     * @return true if the shadow should be shown, false if the light is below
     * the horizon and the shadow should be hidden
     */
    public boolean getShadowLocation (Vector3f center, Vector2f result)
    {
        float elevation = _board.getLightElevation(0);
        if (elevation <= 0f) {
            return false;
        }
        float height = center.z - _tnode.getHeightfieldHeight(
            center.x, center.y);
        float distance = Math.min(height / FastMath.tan(elevation),
            MAX_SHADOW_DISTANCE * height);
        float azimuth = _board.getLightAzimuth(0);
        result.set(center.x - distance * FastMath.cos(azimuth),
            center.y - distance * FastMath.sin(azimuth));
        return true;
    }

    /**
     * Creates a brief flash of light at the specified location with the
     * given color and duration.
     */
    public void createLightFlash (Vector3f location, final ColorRGBA color,
        final float duration)
    {
        final PointLight light = new PointLight();
        light.setLocation(location);
        light.setAttenuate(true);
        light.setConstant(1f);
        light.setQuadratic(0.01f);
        light.setEnabled(true);
        _lstate.attach(light);
        _node.updateRenderState();

        _node.addController(new Controller() {
            public void update (float time) {
                if ((_elapsed += time) < duration) {
                    float alpha = _elapsed / duration;
                    light.getDiffuse().interpolate(color, ColorRGBA.black,
                        alpha);
                    light.getAmbient().interpolate(color, ColorRGBA.black,
                        0.5f + alpha * 0.5f);

                } else {
                    _node.removeController(this);
                    _lstate.detach(light);
                    _node.updateRenderState();
                }
            }
            float _elapsed;
        });
    }

    /**
     * Called by the controller when the round has ended.
     */
    public void endRound ()
    {
        // remove our event listener
        _bangobj.removeListener(_blistener);
    }

    /**
     * Returns a reference to the board being viewed.
     */
    public BangBoard getBoard ()
    {
        return _board;
    }

    /**
     * Returns the node containing the sky.
     */
    public SkyNode getSkyNode ()
    {
        return _snode;
    }

    /**
     * Returns the node containing the terrain.
     */
    public TerrainNode getTerrainNode ()
    {
        return _tnode;
    }

    /**
     * Returns the node to which our pieces and piece effects are
     * attached.
     */
    public Node getPieceNode ()
    {
        return _pnode;
    }

    /**
     * Adds a sprite to the active view.
     */
    public void addSprite (Sprite sprite)
    {
        _pnode.attachChild(sprite);
        sprite.updateRenderState();
        sprite.updateGeometricState(0.0f, true);
        if (sprite instanceof PieceSprite) {
            Spatial highlight = ((PieceSprite)sprite).getHighlight();
            if (highlight != null) {
                _pnode.attachChild(highlight);
                _plights.put(highlight, sprite);
            }
        }
    }

    /**
     * Removes a sprite from the active view.
     */
    public void removeSprite (Sprite sprite)
    {
        _pnode.detachChild(sprite);
        if (sprite instanceof PieceSprite) {
            Spatial highlight = ((PieceSprite)sprite).getHighlight();
            if (highlight != null) {
                _pnode.detachChild(highlight);
                _plights.remove(highlight);
            }
        }
    }

    /**
     * Returns the sprite associated with this spatial or null if the
     * spatial is not part of a sprite.
     */
    public Sprite getSprite (Spatial spatial)
    {
        if (spatial instanceof Sprite) {
            return (Sprite)spatial;

        } else if (_plights.containsKey(spatial)) {
            return _plights.get(spatial);

        } else if (spatial.getParent() != null) {
            return getSprite(spatial.getParent());
        } else {
            return null;
        }
    }

    /**
     * Returns the piece sprite associated with the supplied piece.
     */
    public PieceSprite getPieceSprite (Piece piece)
    {
        return _pieces.get(piece.pieceId);
    }

    /**
     * Returns true if the specified sprite is part of the active view.
     */
    public boolean isManaged (PieceSprite sprite)
    {
        return _pnode.hasChild(sprite);
    }

    /**
     * Computes the maximum height of any props occupying the given
     * tile coordinates, or <code>-Float.MAX_VALUE</code> if there
     * are no props there.
     */
    public float getPropHeight (int tx, int ty)
    {
        float mheight = -Float.MAX_VALUE;
        for (Iterator<PieceSprite> it = _pieces.values().iterator();
                it.hasNext(); ) {
            PieceSprite ps = it.next();
            if (!(ps instanceof PropSprite) ||
                !ps.getPiece().intersects(tx, ty)) {
                continue;
            }
            Object bound = ps.getWorldBound();
            if (!(bound instanceof BoundingBox)) {
                continue;
            }
            BoundingBox bbox = (BoundingBox)bound;
            mheight = Math.max(mheight, bbox.getCenter().z + bbox.zExtent);
        }
        return mheight;
    }

    /**
     * Requests that the specified action be executed. If there are other
     * actions executing and queued for execution, this action will be executed
     * after they complete.
     */
    public void executeAction (BoardAction action)
    {
        if (ACTION_DEBUG) {
            log.info("Queueing: " + action);
        }

        // if we can execute this action immediately, do so
        if (action.canExecute(_punits)) {
            processAction(action);
        } else {
            _pactions.add(action);
        }

        // we always have to add this actions pieces to the pending set
        _punits.add(action.pieceIds);

        // scan the running actions and issue a warning for long runners
        long now = System.currentTimeMillis(), since;
        for (int ii = 0, ll = _ractions.size(); ii < ll; ii++) {
            BoardAction running = _ractions.get(ii);
            if (running.start > 0L && (since = now - running.start) > 5000L) {
                log.warning("Board action stuck on the queue? " +
                            "[action=" + running +
                            ", since=" + since + "ms].");
                running.start = 0L; // avoid repeat warnings
            }
        }
    }

    /**
     * This <em>must</em> be called by any action that returns true from {@link
     * BoardAction#execute} to inform the board that that action is completed
     * and that subsequent actions may be processed.
     */
    public void actionCompleted (BoardAction action)
    {
        if (!_ractions.remove(action)) {
            log.warning("Action re-completed! [action=" + action + "].");
            Thread.dumpStack();
            return;
        }

        if (ACTION_DEBUG) {
            log.info("Completed " + action);
        }
        noteExecuting(action.pieceIds, -1);
        processActions();
    }

    /**
     * Called when a piece is updated in the game object, though sometimes not
     * immediately but after various effects have been visualized.
     */
    public void queuePieceCreate (Piece piece, short tick)
    {
        executeAction(new PieceCreatedAction(piece, tick));
    }

    /**
     * Called when a piece is updated in the game object, though sometimes not
     * immediately but after various effects have been visualized.
     */
    public void queuePieceUpdate (Piece opiece, Piece npiece)
    {
        executeAction(new PieceUpdatedAction(opiece, npiece, _bangobj.tick));
    }

    /**
     * Called when a piece is removed from the game object.
     */
    public void queuePieceRemoval (Piece piece)
    {
        executeAction(new PieceRemovedAction(piece, _bangobj.tick));
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseMoved (MouseEvent e)
    {
        updateHoverState(e);
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (MouseEvent e)
    {
        updateHoverState(e);
    }

    /**
     * Updates the hover state based on the supplied mouse event.
     */
    public void updateHoverState (MouseEvent e)
    {
        Vector3f ground = getGroundIntersect(e, false, null);
        int mx = (int)Math.floor(ground.x / TILE_SIZE);
        int my = (int)Math.floor(ground.y / TILE_SIZE);

        if (mx != _mouse.x || my != _mouse.y) {
            _mouse.x = mx;
            _mouse.y = my;
            hoverTileChanged(_mouse.x, _mouse.y);
        }

        // now update the rest based on our saved coordinates
        updateHoverState();
    }

    /**
     * Updates the hover state based on the last stored mouse coordinates.
     */
    public void updateHoverState ()
    {
        // if we don't know where the mouse is, we can't update
        if (_worldMouse == null) {
            return;
        }

        // update the sprite over which the mouse is hovering
        updateHoverSprite();

        // if we have highlight tiles, determine which of those the mouse
        // is over
        if (_hnode.getQuantity() > 0) {
            updateHighlightHover();
        }
    }

    /**
     * Given a mouse event, returns the point at which a ray cast from the
     * eye through the mouse pointer intersects the ground.
     *
     * @param planar whether or not to intersect with the ground plane as
     * opposed to the terrain
     * @param result a vector to hold the result, or <code>null</code> to
     * create a new vector
     * @return a reference to the result
     */
    public Vector3f getGroundIntersect (MouseEvent e, boolean planar,
        Vector3f result)
    {
        if (result == null) {
            result = new Vector3f();
        }

        // compute the vector from camera location to mouse cursor
        Camera camera = _ctx.getCameraHandler().getCamera();
        Vector2f screenPos = new Vector2f(e.getX(), e.getY());
        _worldMouse = _ctx.getDisplay().getWorldCoordinates(screenPos, 0);
        Vector3f camloc = camera.getLocation();
        _worldMouse.subtractLocal(camloc).normalizeLocal();

        // see if the ray intersects with the terrain
        if (!planar && _tnode.calculatePick(new Ray(camloc, _worldMouse),
                result)) {
            result.z += 0.1f;
            return result;
        }

        // otherwise, intersect with ground plane
        float dist = (-_groundNormal.dot(camloc) + _tnode.getHeightfieldValue(
            -1, -1)) / _groundNormal.dot(_worldMouse);
        result.scaleAdd(dist, _worldMouse, camloc);
        result.z += 0.1f;

        return result;
    }

    /**
     * Retrieves the pick intersection closest to the origin and places it in
     * the given destination vector.
     *
     * @return true if a triangle intersection was found and placed in the
     * result vector, false otherwise
     */
    protected boolean getPickIntersection (Vector3f result)
    {
        float nearest = Float.MAX_VALUE;
        Vector3f[] verts = new Vector3f[3];
        Vector3f loc = new Vector3f();
        for (int i = 0, size = _pick.getNumber(); i < size; i++) {
            PickData pdata = _pick.getPickData(i);
            if (notReallyAHit(pdata)) {
                continue;
            }
            Object mesh = pdata.getTargetMesh();
            for (Object oidx : pdata.getTargetTris()) {
                int idx = ((Integer)oidx).intValue();
                if (mesh instanceof SharedMesh) {
                    ((SharedMesh)mesh).getTarget().getTriangle(idx, verts);
                } else {
                    ((TriMesh)mesh).getTriangle(idx, verts);
                }
                pdata.getRay().intersectWhere(verts[0], verts[1], verts[2],
                    loc);
                float d2 = pdata.getRay().getOrigin().distanceSquared(loc);
                if (d2 < nearest) {
                    nearest = d2;
                    result.set(loc);
                }
            }
        }
        return nearest < Float.MAX_VALUE;
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // add our geometry into the scene graph
        _ctx.getGeometry().attachChild(_node);

        // add our own blackness that we'll fade in when we're ready
        _ctx.getInterface().attachChild(_fadein);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // clear our sprites so that piece sprites can clean up after
        // themselves
        removePieceSprites();

        // remove our geometry from the scene graph
        _ctx.getGeometry().detachChild(_node);

        // clear any marquee we have up
        clearMarquee(0f);

        // let the child nodes know that they need to clean up any textures
        // they've created
        _tnode.cleanup();
        _wnode.cleanup();
        
        // get rid of the music stream
        if (_mstream != null) {
            _mstream.stop();
            _mstream.close();
            _mstream = null;
        }
    }

    /**
     * Creates and returns the terrain node for this board view, giving
     * subclasses a change to customize the object.
     */
    protected TerrainNode createTerrainNode (BasicContext ctx)
    {
        return new TerrainNode(ctx, this);
    }

    /**
     * Returns whether or not we should show the grid by default.
     */
    protected boolean shouldShowGrid ()
    {
        return BangPrefs.config.getValue("show_grid", true);
    }

    /**
     * Updates the tile grid over the entire board.
     */
    protected void updateGrid ()
    {
        if (_grid == null) {
            _grid = new GridNode(_board, _tnode);
        }
        _grid.updateVertices();
    }

    /**
     * Adds a sprite resolution observer which will be notified when all
     * pending sprites are fully resolved. If there are no pending sprites, the
     * observer will be notified immediately (before this call returns).
     */
    protected void addResolutionObserver (ResolutionObserver obs)
    {
        if (_resolvingSprites == 0) {
            obs.mediaResolved();
        } else {
            _resolutionObs.add(obs);
        }
    }

    /**
     * Returns the resource path for the board's opening music, or
     * <code>null</code> for none.
     */
    protected String getOpeningMusicPath ()
    {
        return null;
    }
    
    /**
     * Creates a fade-in effect and pauses it, we will unpause once the board
     * is fully resolved.
     */
    protected void createPausedFadeIn ()
    {
        _fadein = new FadeInOutEffect(
            ColorRGBA.black, 1f, 0f, getFadeInTime(), false) {
            protected void fadeComplete () {
                super.fadeComplete();
                fadeInComplete();
            }
        };
        _fadein.setPaused(true);
    }

    /**
     * Returns our fade-in time in seconds.
     */
    protected float getFadeInTime ()
    {
        return 2f;
    }

    /**
     * This is called when our initial fade-in of the board is complete.
     */
    protected void fadeInComplete ()
    {
        clearMarquee(1f);
        _fadein = null;
    }

    /**
     * Updates the directional lights according to the board's light
     * parameters.
     */
    protected void refreshLights ()
    {
        for (int i = 0; i < _lights.length; i++) {
            refreshLight(i);
        }
    }

    /**
     * Updates a directional light according to the board's light parameters.
     */
    protected void refreshLight (int idx)
    {
        int dcolor = _board.getLightDiffuseColor(idx),
            acolor = _board.getLightAmbientColor(idx);
        if (dcolor == 0 && acolor == 0) {
            _lights[idx].setEnabled(false);
            return;
        }
        _lights[idx].setEnabled(true);

        float cose = FastMath.cos(_board.getLightElevation(idx));
        _lights[idx].setDirection(new Vector3f(
            -cose * FastMath.cos(_board.getLightAzimuth(idx)),
            -cose * FastMath.sin(_board.getLightAzimuth(idx)),
            -FastMath.sin(_board.getLightElevation(idx))));

        float[] drgb = new Color(dcolor).getRGBColorComponents(null),
            argb = new Color(acolor).getRGBColorComponents(null);
        _lights[idx].setDiffuse(new ColorRGBA(drgb[0], drgb[1], drgb[2], 1f));
        _lights[idx].setAmbient(new ColorRGBA(argb[0], argb[1], argb[2], 1f));
    }

    /**
     * Processes all ready actions in the action queue.
     */
    protected void processActions ()
    {
        // clear out _punits and repopulate it with what's in eunits
        _punits.clear();
        _punits.add(_eunits.getValues());

        Iterator<BoardAction> iter = _pactions.iterator();
        while (iter.hasNext()) {
            BoardAction action = iter.next();
            if (action.canExecute(_punits)) {
                iter.remove();
                // this only queues up the action for processing, so we need
                // not worry that the action will complete immediately and
                // result in a recursive call to processActions()
                processAction(action);
            }
            _punits.add(action.pieceIds);
        }
    }

    /**
     * Processes the next action from the board action queue.
     */
    protected void processAction (final BoardAction action)
    {
        if (ACTION_DEBUG) {
            log.info("Posting: " + action);
        }

        // mark the pieces involved in this action as executing
        noteExecuting(action.pieceIds, 1);

        // throw a runnable on the queue that will execute this action
        _ractions.add(action);
        _ctx.getApp().postRunnable(new Runnable() {
            public void run () {
                try {
                    if (ACTION_DEBUG) {
                        log.info("Running: " + action);
                    }

                    action.start = System.currentTimeMillis();
                    if (action.execute()) {
                        if (ACTION_DEBUG) {
                            log.info("Waiting: " + action);
                        }
                        // the action requires us to wait until it completes
                        return;
                    }

                    if (ACTION_DEBUG) {
                        log.info("Completed: " + action);
                    }

                } catch (Throwable t) {
                    log.log(Level.WARNING, "Board action choked: " + action, t);
                }

                // note that this action is completed
                actionCompleted(action);
            }
        });
    }

    /**
     * Called when a piece update is actually being effected. Updates are first
     * queued via {@link #queuePieceUpdate} and then executed one at a time as
     * board actions (see {@link #executeAction}), waiting for animations and
     * effects to complete in between.
     */
    protected void pieceUpdated (Piece opiece, Piece npiece, short tick)
    {
        PieceSprite sprite;
        if ((sprite = getPieceSprite(npiece)) == null) {
            log.info("Not updating, missing piece sprite [opiece=" + opiece +
                ", npiece=" + npiece + ", tick=" + tick + "].");
            return;
        }

        // update the sprite with its new piece
        sprite.updated(npiece, tick);

        // if the piece has moved (which should only happen in the editor,
        // never in game), then go ahead and move it
        if (PieceSprite.isEditorMode()) {
            // it should finish immediately because we're in the editor
            if (sprite.updatePosition(_bangobj.board)) {
                log.warning("Piece moved along path after update " +
                    "[piece=" + npiece + "]!");
            }
        }
    }

    /**
     * Called when a piece removal is actually being effected. Removals are
     * first queued via {@link #queuePieceRemoval} and then executed after
     * other previously queued board actions are completed.
     */
    protected void pieceRemoved (Piece piece, short tick)
    {
        // TODO: queue up a fade out action like we do when a piece dies?
        removePieceSprite(piece.pieceId, "pieceRemoved(" + piece.info() + ")");
    }

    /**
     * Creates a big text marquee in the middle of the screen.
     */
    protected void createMarquee (String text)
    {
        clearMarquee(0);

        // create the marquee, center it and display it
        _marquee = RenderUtil.createTextQuad(_ctx, BangUI.MARQUEE_FONT, text);
        _marquee.setLocalTranslation(
            new Vector3f(_ctx.getRenderer().getWidth()/2f,
                         _ctx.getRenderer().getHeight()/2f, 0));
        _marquee.setRenderQueueMode(Renderer.QUEUE_ORTHO);
        _marquee.setZOrder(-2);
        _ctx.getInterface().attachChild(_marquee);
    }

    /**
     * Clears our marquee display.
     */
    protected void clearMarquee (float fadeTime)
    {
        if (_marquee != null) {
            clearMarquee(_marquee, fadeTime);
            _marquee = null;
        }
    }

    /**
     * Clears out a marquee quad.
     */
    protected void clearMarquee (Quad mquad, float fadeTime)
    {
        _ctx.getInterface().detachChild(mquad);
        if (fadeTime > 0f) {
            TimeFunction tf = new LinearTimeFunction(1f, 0f, fadeTime);
            _ctx.getInterface().attachChild(
                new FadeInOutEffect(mquad, ColorRGBA.white, tf, true));
        }
    }

    /**
     * Updates the loading progress display.
     */
    protected void updateLoadingMarquee ()
    {
        if (_loading != null) {
            _ctx.getInterface().detachChild(_loading);
        }

        // Model.getLoader().getQueueSize()
        int pct = _loaded * 100 / _toLoad;
        String pctstr = _ctx.xlate(
            GameCodes.GAME_MSGS, MessageBundle.tcompose(
                "m.loading_pct", String.valueOf(pct)));
        _loading = RenderUtil.createTextQuad(_ctx, BangUI.LOADING_FONT, pctstr);
        _loading.setLocalTranslation(
            new Vector3f(_ctx.getRenderer().getWidth()-100, 25, 0));
        _loading.setRenderQueueMode(Renderer.QUEUE_ORTHO);
        _loading.setZOrder(-2);
        _ctx.getInterface().attachChild(_loading);
    }

    /**
     * Creates the piece sprite for the supplied piece. The newly created
     * sprite will automatically be initialized with the supplied piece and
     * added to the board view.
     */
    protected void createPieceSprite (Piece piece, short tick)
    {
        log.fine("Creating sprite for " + piece + ".");
        PieceSprite sprite = piece.createSprite();
        sprite.init(_ctx, this, _bangobj.board, _sounds, piece, tick);
        _pieces.put((int)piece.pieceId, sprite);
        addSprite(sprite);
    }

    /**
     * Removes the sprite associated with the specified piece.
     */
    protected PieceSprite removePieceSprite (int pieceId, String why)
    {
        PieceSprite sprite = _pieces.remove(pieceId);
        if (sprite != null) {
            log.fine("Removing sprite [id=" + pieceId + ", why=" + why + "].");
            removeSprite(sprite);

        } else {
            log.warning("No sprite for removed piece [id=" + pieceId +
                        ", why=" + why + "].");
        }
        return sprite;
    }

    /**
     * Removes all of the piece sprites.
     */
    protected void removePieceSprites ()
    {
        for (PieceSprite sprite : _pieces.values()) {
            removeSprite(sprite);
        }
    }

    /**
     * Updates the sprite that the mouse is "hovering" over (the one nearest to
     * the camera that is hit by the ray projecting from the camera to the
     * ground plane at the current mouse coordinates, if present, or any
     * hoverable sprite occupying the hover tile).
     */
    protected void updateHoverSprite ()
    {
        Vector3f camloc = _ctx.getCameraHandler().getCamera().getLocation();
        _pick.clear();
        _pnode.findPick(new Ray(camloc, _worldMouse), _pick);
        float dist = Float.MAX_VALUE;
        Sprite hit = null;
        for (int ii = 0; ii < _pick.getNumber(); ii++) {
            PickData pdata = _pick.getPickData(ii);
            if (notReallyAHit(pdata)) {
                continue;
            }
            Sprite s = getSprite(pdata.getTargetMesh());
            if (!isHoverable(s)) {
                continue;
            }
            float sdist = camloc.distanceSquared(s.getWorldTranslation());
            if (sdist < dist) {
                hit = s;
                dist = sdist;
            }
        }
        if (hit == null) {
            for (Iterator<PieceSprite> it = _pieces.values().iterator();
                    it.hasNext(); ) {
                PieceSprite ps = it.next();
                if (ps.getPiece().intersects(_mouse.x, _mouse.y) &&
                        isHoverable(ps)) {
                    hit = ps;
                    break;
                }
            }
        }
        if (hit != _hover) {
            hoverSpriteChanged(hit);
        }
    }

    /** Helper function for {@link #updateHoverSprite}. */
    protected boolean isHoverable (Sprite sprite)
    {
        return (sprite != null);
    }

    /**
     * Determine which of the highlight tiles the mouse is hovering over
     * and records that tile's coordinates in {@link #_high}.
     */
    protected void updateHighlightHover ()
    {
        Vector3f camloc = _ctx.getCameraHandler().getCamera().getLocation();
        _pick.clear();
        _hnode.findPick(new Ray(camloc, _worldMouse), _pick);
        for (int ii = 0; ii < _pick.getNumber(); ii++) {
            PickData pdata = _pick.getPickData(ii);
            if (notReallyAHit(pdata)) {
                continue;
            }
            Geometry tmesh = pdata.getTargetMesh();
            if (tmesh instanceof TerrainNode.Highlight) {
                TerrainNode.Highlight highlight = (TerrainNode.Highlight)tmesh;
                _high.x = highlight.getTileX();
                _high.y = highlight.getTileY();
                return;
            }
        }
        if (_high.x != -1 || _high.y != -1) {
//             log.info("Clearing highlight.");
            _high.x = -1;
            _high.y = -1;
        }
    }

    /**
     * This is called when the mouse is moved to hover over a different
     * board tile.
     */
    protected void hoverTileChanged (int tx, int ty)
    {
    }

    /**
     * This is called when the mouse is moved to hover over a different
     * sprite (or none at all).
     */
    protected void hoverSpriteChanged (Sprite hover)
    {
        _hover = hover;
    }

    /** Creates geometry to highlight the supplied set of tiles. */
    protected void highlightTiles (PointSet set, boolean forFlyer)
    {
        for (int ii = 0, ll = set.size(); ii < ll; ii++) {
            int tx = set.getX(ii), ty = set.getY(ii);
            TerrainNode.Highlight highlight =
                _tnode.createHighlight(tx, ty, false);
            highlight.setDefaultColor(MOVEMENT_HIGHLIGHT_COLOR);
            highlight.setRenderState(_movstate);
            if (_bangobj.board.isUnderProp(tx, ty)) {
                highlight.setRenderState(RenderUtil.alwaysZBuf);
            }
            highlight.updateRenderState();
            _hnode.attachChild(highlight);
        }
    }

    /** Creates geometry to "target" the supplied set of tiles. */
    protected void targetTiles (PointSet set)
    {
        for (int ii = 0, ll = set.size(); ii < ll; ii++) {
            TerrainNode.Highlight highlight = _tnode.createHighlight(
                set.getX(ii), set.getY(ii), true);
            highlight.setRenderState(_tgtstate);
            highlight.updateRenderState();
            _hnode.attachChild(highlight);
        }
    }

    /** Clears out all highlighted tiles. */
    protected void clearHighlights ()
    {
        _hnode.detachAllChildren();
        _hnode.updateRenderState();
        _hnode.updateGeometricState(0f, true);
    }

    /** JME peskily returns bogus hits when we do triangle level picking. */
    protected boolean notReallyAHit (PickData pdata)
    {
        ArrayList tris = pdata.getTargetTris();
        Object mesh = pdata.getTargetMesh();
        return (tris == null || tris.size() == 0 || !(mesh instanceof TriMesh));
    }

    /** Used to increment and decrement executing status for pieces. */
    protected void noteExecuting (int[] pieceIds, int delta)
    {
        for (int ii = 0; ii < pieceIds.length; ii++) {
            _eunits.increment(pieceIds[ii], delta);
        }
    }

    /** Used to queue up piece createion so that the piece shows up on the
     * board in the right sequence with all other board actions. */
    protected class PieceCreatedAction extends BoardAction
    {
        public Piece piece;
        public short tick;

        public PieceCreatedAction (Piece piece, short tick) {
            this.piece = piece;
            this.tick = tick;
            this.pieceIds = new int[] { piece.pieceId };
            this.waiterIds = new int[0];
        }

        public boolean execute () {
            createPieceSprite(piece, tick);
            return false;
        }
    }

    /** Used to queue up piece updates so that each can execute, trigger
     * animations and run to completion before the next one is run. */
    protected class PieceUpdatedAction extends BoardAction
    {
        public Piece opiece, npiece;
        public short tick;

        public PieceUpdatedAction (Piece opiece, Piece npiece, short tick) {
            this.opiece = opiece;
            this.npiece = npiece;
            this.tick = tick;
            this.pieceIds = new int[] { opiece.pieceId };
            this.waiterIds = new int[0];
        }

        public boolean execute () {
            pieceUpdated(opiece, npiece, tick);
            return false;
        }

        public String toString () {
            String strval = super.toString();
            if (opiece != null) {
                strval += ":" + opiece.info();
            }
            if (npiece != null) {
                strval += "->" + npiece.info();
            }
            return strval;
        }
    }

    /** Used to queue up piece removals so that each can execute, trigger
     * animations and run to completion before the next one is run. */
    protected class PieceRemovedAction extends BoardAction
    {
        public Piece piece;
        public short tick;

        public PieceRemovedAction (Piece piece, short tick) {
            this.piece = piece;
            this.tick = tick;
            this.pieceIds = new int[] { piece.pieceId };
            this.waiterIds = new int[0];
        }

        public boolean execute () {
            pieceRemoved(piece, tick);
            return false;
        }

        public String toString () {
            return super.toString() + ":" + piece.info();
        }
    }

    /** Listens for various different events and does the right thing. */
    protected class BoardEventListener
        implements SetListener
    {
        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(BangObject.PIECES)) {
                queuePieceCreate((Piece)event.getEntry(), _bangobj.tick);
            }
        }

        public void entryUpdated (EntryUpdatedEvent event) {
            if (event.getName().equals(BangObject.PIECES)) {
                queuePieceUpdate((Piece)event.getOldEntry(),
                                 (Piece)event.getEntry());
            }
        }

        public void entryRemoved (EntryRemovedEvent event) {
            if (event.getName().equals(BangObject.PIECES)) {
                queuePieceRemoval((Piece)event.getOldEntry());
            }
        }
    };

    /** Used to delay actions until we're done resolving our sprites. */
    protected interface ResolutionObserver
    {
        public void mediaResolved ();
    };

    protected BasicContext _ctx;
    protected BangObject _bangobj;
    protected BangBoard _board;
    protected Rectangle _bbounds;
    protected BoardEventListener _blistener = new BoardEventListener();

    protected Quad _marquee, _loading;
    protected int _toLoad, _loaded;

    protected Node _node, _pnode, _hnode;
    protected LightState _lstate;
    protected DirectionalLight[] _lights;
    protected SkyNode _snode;
    protected TerrainNode _tnode;
    protected WaterNode _wnode;
    protected Vector3f _worldMouse;
    protected Ray _pray = new Ray();
    protected TrianglePickResults _pick = new TrianglePickResults();
    protected Sprite _hover;

    protected ArrayList<BoardAction> _ractions = new ArrayList<BoardAction>();
    protected ArrayList<BoardAction> _pactions = new ArrayList<BoardAction>();
    protected IntIntMap _eunits = new IntIntMap();
    protected ArrayIntSet _punits = new ArrayIntSet();

    /** Used to texture a quad that "targets" a tile. */
    protected TextureState _tgtstate;

    /** Used to texture movement highlights. */
    protected TextureState _movstate;

    // TODO: rename _hastate
    protected AlphaState _hastate;

    /** The current tile coordinates of the mouse. */
    protected Point _mouse = new Point(-1, -1);

    /** The tile coordinates of the highlight tile that the mouse is
     * hovering over or (-1, -1). */
    protected Point _high = new Point(-1, -1);

    /** The grid indicating where the tile boundaries lie. */
    protected GridNode _grid;

    /** Used to load all in-game sounds. */
    protected SoundGroup _sounds;

    /** The music stream for the board's soundtrack. */
    protected MusicStream _mstream;
    
    protected HashMap<Integer,PieceSprite> _pieces =
        new HashMap<Integer,PieceSprite>();

    /** Used to track sprites that are loading their animations. */
    protected int _resolvingSprites;

    /** Used to keep track of observers that want to know when our sprites are
     * resolved and we're ready to roll. */
    protected ObserverList _resolutionObs =
        new ObserverList(ObserverList.SAFE_IN_ORDER_NOTIFY);

    /** Used to fade ourselves in at the start of the game. */
    protected FadeInOutEffect _fadein;

    protected HashMap<Spatial,Sprite> _plights =
        new HashMap<Spatial,Sprite>();

    /** Used when intersecting the ground. */
    protected static final Vector3f _groundNormal = new Vector3f(0, 0, 1);

    protected static final ColorRGBA DARK =
        new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f);
    protected static final ColorRGBA LIGHT =
        new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f);
    protected static final ColorRGBA BROWN =
        new ColorRGBA(204/255f, 153/255f, 51/255f, 1.0f);

    /** The color of the movement highlights. */
    protected static final ColorRGBA MOVEMENT_HIGHLIGHT_COLOR =
        new ColorRGBA(1f, 1f, 0.5f, 0.5f);

    /** The number of simultaneous sound "sources" available to the game. */
    protected static final int GAME_SOURCE_COUNT = 10;

    /** The longest we'll let the shadows get. */
    protected static final float MAX_SHADOW_LENGTH = TILE_SIZE * 3;

    /** The furthest we'll let the shadows get (as a multiple of height). */
    protected static final float MAX_SHADOW_DISTANCE =
        MAX_SHADOW_LENGTH / (TILE_SIZE * 2);

    /** Used when debugging board actions. */
    protected static final boolean ACTION_DEBUG = false;
}
