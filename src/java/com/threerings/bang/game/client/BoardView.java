//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jme.intersection.PickData;
import com.jme.intersection.TrianglePickResults;
import com.jme.light.DirectionalLight;
import com.jme.light.PointLight;
import com.jme.math.FastMath;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.FogState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;

import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BRootNode;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseMotionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Insets;

import com.jmex.effects.particles.ParticleGeometry;
import com.jmex.effects.particles.SimpleParticleInfluenceFactory;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.jme.effect.FadeInOutEffect;
import com.threerings.jme.sprite.Sprite;
import com.threerings.jme.util.LinearTimeFunction;
import com.threerings.jme.util.SpatialVisitor;
import com.threerings.jme.util.TimeFunction;
import com.threerings.openal.SoundGroup;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.Config;
import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.game.client.effect.ParticlePool;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
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
        /** The id of this action. */
        public int actionId;

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

        /** The board action must fill in this array with the ids of any pieces
         * that need special move handling. */
        public int[] moveIds;

        /** The board action must specify the boundaries affected by the
         * action.  This will be used to ensure that a piece does not enter
         * the affected area if not participating in the action. */
        public Rectangle[] bounds;

        /** Returns true if this action can be executed, false if it operates
         * on a piece that is currently involved in another action. */
        public boolean canExecute (ArrayIntSet penders,
                HashSet<Rectangle> boundset, LinkedList<Integer> syncQueue)
        {
            if (!syncQueue.isEmpty() && actionId > syncQueue.getFirst()) {
                return false;
            }
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
            if (bounds != null) {
                for (Rectangle r : boundset) {
                    for (Rectangle b : bounds) {
                        if (r.intersects(b)) {
                            return false;
                        }
                    }
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
            return cname.substring(cname.lastIndexOf("$")+1) +
                " w" + StringUtil.toString(waiterIds) +
                " p" + StringUtil.toString(pieceIds) + ":" + hashCode();
        }

        /** Whether this action has been cleared by the BoardView. */
        protected boolean _cleared = false;
    }

    public BoardView (BasicContext ctx, boolean editorMode)
    {
        setStyleClass("board_view");
        setTooltipRelativeToMouse(true);
        _ctx = ctx;
        _editorMode = editorMode;

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
        _node.setNormalsMode(Spatial.NM_GL_NORMALIZE_PROVIDED);

        // default states
        MaterialState mstate = _ctx.getRenderer().createMaterialState();
        mstate.getDiffuse().set(ColorRGBA.white);
        mstate.getAmbient().set(ColorRGBA.white);
        _node.setRenderState(mstate);
        _node.setRenderState(RenderUtil.lequalZBuf);
        _node.setRenderState(RenderUtil.opaqueAlpha);
        _node.setTextureCombineMode(TextureState.REPLACE);
        _node.setCullMode(Spatial.CULL_DYNAMIC);
        _node.updateRenderState();

        // create the sky
        if (shouldShowSky() && Config.displaySky) {
            _node.attachChild(_snode = new SkyNode(_ctx));
        }

        // we'll hang the board geometry off this node
        Node bnode = new Node("board");
        _node.attachChild(bnode);
        _tnode = new TerrainNode(ctx, this, editorMode);
        if (Config.displayTerrain) {
            bnode.attachChild(_tnode);
        }
        _wnode = new WaterNode(ctx, _lights[0], editorMode);
        if (Config.displayWater) {
            bnode.attachChild(_wnode);
        }

        // create the shared wind influence
        _wind = new SimpleParticleInfluenceFactory.BasicWind(
            0f, new Vector3f(), true, false);

        // the children of this node will have special tile textures
        bnode.attachChild(_texnode = new Node("texturehighlights"));
        _texnode.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);

        // the children of this node will display highlighted tiles
        // with colors that may throb on and off
        bnode.attachChild(_hnode = new Node("highlights") {
            public void updateWorldData (float time) {
                super.updateWorldData(time);
                if (getQuantity() > 0) {
                    updateThrobbingColors();
                }
            }
        });
        _hnode.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);

        // we'll hang all of our pieces off this node
        _pnode = createPieceNode();
        if (Config.displayModels) {
            _node.attachChild(_pnode);
        }

        // create our highlight alpha state
        _hastate = ctx.getDisplay().getRenderer().createAlphaState();
        _hastate.setBlendEnabled(true);
        _hastate.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        _hastate.setDstFunction(AlphaState.DB_ONE);
        _hastate.setEnabled(true);

        // this is used to target tiles when deploying a card
        _tgtstate = RenderUtil.createTextureState(
            ctx, "textures/ustatus/crosshairs_card.png");

        // this is used to indicate where you can move
        _movstate = RenderUtil.createTextureState(
            ctx, "textures/ustatus/movement.png");

        // this is used to indicate you hovering over a valid move
        _movhovstate = RenderUtil.createTextureState(
            ctx, "textures/ustatus/movement_hover.png");

        // this is used to indicate a movement goal
        _goalstate = RenderUtil.createTextureState(
            ctx, "textures/ustatus/movement_goal.png");

        // this is used to indicate you hovering over a movement goal
        _goalhovstate = RenderUtil.createTextureState(
            ctx, "textures/ustatus/movement_goal_hover.png");

        // create a sound group that we'll use for all in-game sounds
        _sounds = ctx.getSoundManager().createGroup(
            BangUI.clipprov, GAME_SOURCE_COUNT);

        // create a paused fade in effect, we'll do our real fading in once
        // everything is loaded up and we're ready to show the board
        if (!editorMode) {
            createPausedFadeIn();
        }
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
        if (_fadein != null) {
            // add our own blackness that we'll fade in when we're ready; we don't
            // attach the main scene graph until we're ready to fade in to avoid
            // consuming CPU while we're loading and decoding models
            _ctx.getInterface().attachChild(_fadein);
        }

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

        // reset the sound effects
        if (_sounds != null) {
            _sounds.reclaimAll();
        }

        // remove any possible pending or executing board actions
        clearBoardActions();

        // start afresh
        _board = _bangobj.board;
        _board.init(_bangobj.teams, _bangobj.getPropPieceIterator());
        _bbounds = new Rectangle(0, 0, _board.getWidth(), _board.getHeight());

        // create a marquee if we've been configured to do so
        if (_bangobj.marquee != null) {
            createMarquee(_ctx.xlate(GameCodes.GAME_MSGS, _bangobj.marquee));
        }

        // refresh the lights and fog and wind and such
        refreshLights();
        refreshFog();
        refreshBackgroundColor();
        refreshWindInfluence();

        // create the board geometry
        if (_snode != null) {
            _snode.createBoardSky(_board);
        }
        _tnode.createBoardTerrain(_board);
        _wnode.createBoardWater(_board);

        // display the tile grid if appropriate
        if (shouldShowGrid()) {
            if (_grid != null) {
                _grid.cleanup();
                _grid.removeFromParent();
            }
            _grid = null;
            toggleGrid(false);
        }

        // create sprites for all of the board pieces
        for (Iterator<Piece> it = _bangobj.getPropPieceIterator();
                it.hasNext(); ) {
            Piece piece = it.next();
            if (shouldShowStarter(piece)) {
                createPieceSprite(piece, _bangobj.tick);
            }
        }
        _pnode.updateGeometricState(0, true);

        // create a loading marquee to report loading progress
        if (_toLoad > 0) {
            updateLoadingMarquee();
        }

        // fade the board in when the sprites are all resolved
        addResolutionObserver(new ResolutionObserver() {
            public void mediaResolved () {
                // now that our terrain has finished loading, make sure all
                // the highlights are updated vertices
                for (PieceSprite ps : _pieces.values()) {
                    ps.updateTileHighlightVertices();
                }

                clearLoadingMarquee();

                // now that we're ready to fade in, go ahead and add our
                // geometry into the scene graph
                _ctx.getGeometry().attachChild(_node);

                if (_fadein != null) {
                    _fadein.setPaused(false);
                }
            }
        });
    }

    /**
     * Shows or hides the tile grid.
     */
    public void toggleGrid (boolean persistent)
    {
        if (_board == null) { // no freaky outy
            return;
        }
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
     * Informs the board that a board element is currently resolving.
     */
    public void addResolving (Object resolver)
    {
        _resolving++;
        _toLoad++;
    }

    /**
     * Informs the board that a board element is finished resolving.
     */
    public void clearResolving (Object resolved)
    {
        _resolving--;
        _loaded++;

        // update our loading marquee
        if (_loading != null) {
            updateLoadingMarquee();
        }

        // if we're done resolving, notify any resolution observers
        if (_resolving == 0 && _resolutionObs.size() > 0) {
            // flatten the list to an array and clear it to avoid funny
            // business if the resolution observer does something that triggers
            // a sprite to resolve and clear itself
            ResolutionObserver[] olist = _resolutionObs.toArray(
                new ResolutionObserver[_resolutionObs.size()]);
            _resolutionObs.clear();
            for (int ii = 0; ii < olist.length; ii++) {
                olist[ii].mediaResolved();
            }
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
     * Returns the intensity of the shadows on the board.
     */
    public float getShadowIntensity ()
    {
        return _board.getShadowIntensity();
    }

    /**
     * Returns the intensity of the dynamic shadows on the board, which depends
     * on the total light and the board's configured shadow intensity.
     *
     * @return 0.0 for no shadows, 1.0 for completely black shadows
     */
    public float getDynamicShadowIntensity ()
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
     * Gets the color of the dust at the specified location, which will depend
     * on the terrain type, the ground normal, and the lighting parameters.
     */
    public void getDustColor (Vector3f location, ColorRGBA result)
    {
        // get the normal at the supplied location and use it to determine the
        // total light falling onto the terrain
        Vector3f normal = _tnode.getHeightfieldNormal(location.x, location.y);
        result.set(0f, 0f, 0f, 1f);
        for (int ii = 0; ii < _lights.length; ii++) {
            result.addLocal(_lights[ii].getAmbient());
            _color.set(_lights[ii].getDiffuse());
            result.addLocal(_color.multLocal(
                Math.max(0, -normal.dot(_lights[ii].getDirection()))));
        }

        // get the color of the predominant terrain and multiply it by the
        // light
        int tx = (int)(location.x / TILE_SIZE),
            ty = (int)(location.y / TILE_SIZE);
        TerrainConfig terrain = TerrainConfig.getConfig(
            _board.getPredominantTerrain(tx, ty));
        ColorRGBA dcolor = terrain.dustColor;
        result.set(result.r * dcolor.r, result.g * dcolor.g,
            result.b * dcolor.b, dcolor.a);
    }

    /**
     * Adds the board's wind influence to all particle systems under the
     * given node.
     */
    public void addWindInfluence (Spatial spatial)
    {
        new SpatialVisitor<ParticleGeometry>(ParticleGeometry.class) {
            protected void visit (ParticleGeometry geom) {
                geom.addInfluence(_wind);
            }
        }.traverse(spatial);
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
                highlight.updateRenderState();
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
    public Sprite getPieceSprite (Spatial spatial)
    {
        if (spatial instanceof PieceSprite) {
            return (Sprite)spatial;

        } else if (_plights.containsKey(spatial)) {
            return _plights.get(spatial);

        } else if (spatial.getParent() != null) {
            return getPieceSprite(spatial.getParent());
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
     * Requests that the specified action be executed. If there are other
     * actions executing and queued for execution, this action will be executed
     * after they complete.
     */
    public void executeAction (BoardAction action)
    {
        action.actionId = ++_maxActionId;
        // if we can execute this action immediately, do so
        if (action.canExecute(_punits, _pbounds, _syncQueue)) {
            processAction(action);
        } else {
            if (ACTION_DEBUG) {
                log.info("Queueing p" + _punits + ": " + action);
            }
            _pactions.add(action);
        }

        // we always have to add this action's pieces to the pending set
        notePending(action);

        // scan the running actions and issue a warning for long runners
        long now = System.currentTimeMillis(), since;
        for (int ii = 0, ll = _ractions.size(); ii < ll; ii++) {
            BoardAction running = _ractions.get(ii);
            if (running.start > 0L && (since = now - running.start) > 8000L) {
                log.warning("Board action stuck on the queue?", "action", running,
                            "since", since + "ms");
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
        // we don't care about cleared actions
        if (action._cleared) {
            return;
        }

        if (!_ractions.remove(action)) {
            log.warning("Action re-completed!", "action", action);
            Thread.dumpStack();
            return;
        }

        if (ACTION_DEBUG) {
            log.info("Completed: " + action);
        }
        noteExecuting(action, -1);
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
        for (SyncAction sa : _syncActions) {
            sa.removePiece(piece);
        }
        executeAction(new PieceRemovedAction(piece, _bangobj.tick));
    }

    /**
     * Called to sync all the board actions for a tick.
     */
    public void queueSyncAction ()
    {
        SyncAction sa = new SyncAction(_bangobj.tick);
        _syncActions.add(sa);
        executeAction(sa);
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

        // update the sprites over which the mouse is hovering
        updateHoverSprites();

        // if we have highlight tiles, determine which of those the mouse
        // is over
        if (_hnode.getQuantity() > 0) {
            updateHighlightHover();
        }
    }

    /**
     * Determines whether the given sprite can be hovered over.
     */
    public boolean isHoverable (Sprite sprite)
    {
        return (sprite != null);
    }

    /**
     * Determines whether the given sprite has a tooltip to display.
     */
    public boolean hasTooltip (Sprite sprite)
    {
        return false;
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

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

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

        // restore the black background color
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.black);

        // let the child nodes know that they need to clean up any textures
        // they've created
        if (_snode != null) {
            _snode.cleanup();
        }
        _tnode.cleanup();
        _wnode.cleanup();
        if (_grid != null) {
            _grid.cleanup();
        }

        // clean up our sound handler
        if (_sounds != null) {
            _sounds.dispose();
            _sounds = null;
        }

        // if we have a lingering fade-in or marquee, clear them
        if (_fadein != null) {
            _ctx.getInterface().detachChild(_fadein);
            _fadein = null;
        }
        if (_mroot != null) {
            _ctx.getInterface().detachChild(_mroot);
        }

        // clear out the particle pool
        ParticlePool.clear();

        // clear the render queue of any lingering references
        _ctx.getRenderer().clearQueue();
    }

    /**
     * Creates the piece node.
     */
    protected Node createPieceNode ()
    {
        return new Node("pieces");
    }

    /**
     * Checks whether or not we should show the sky node (i.e., whether the
     * player will ever see the sky).
     */
    protected boolean shouldShowSky ()
    {
        return true;
    }

    /**
     * Returns whether or not we should show the grid by default.
     */
    protected boolean shouldShowGrid ()
    {
        return BangPrefs.config.getValue("show_grid", true);
    }

    /**
     * Called for each piece on the board at refresh time, giving boards a
     * chance to omit optional pieces for improved performance.
     */
    protected boolean shouldShowStarter (Piece piece)
    {
        // some markers don't like being seen
        if (!PieceSprite.isEditorMode() && piece instanceof Marker &&
                !((Marker)piece).addSprite()) {
            return false;
        }
        // for medium detail, omit half of the props that are outside of the
        // board or passable; for low detail, omit all of them
        if (!(piece instanceof Prop) ||
            BangPrefs.isHighDetail()) {
            return true;
        }
        Prop prop = (Prop)piece;
        if (!prop.isOmissible(_board)) {
            return true;
        }
        return BangPrefs.isMediumDetail() ?
            ((piece.pieceId & 0x01) == 0) : false;
    }

    /**
     * Updates the tile grid over the entire board.
     */
    protected void updateGrid ()
    {
        if (_grid == null) {
            _grid = new GridNode(_ctx, _board, _tnode, _editorMode);
        } else {
            _grid.updateVertices();
        }
        ColorRGBA color = RenderUtil.createColorRGBA(
            _board.getGridColor());
        _grid.getBatch(0).getDefaultColor().set(
            color.r, color.g, color.b, 0.4f);
    }

    /**
     * Adds a sprite resolution observer which will be notified when all
     * pending sprites are fully resolved. If there are no pending sprites, the
     * observer will be notified immediately (before this call returns).
     */
    protected void addResolutionObserver (ResolutionObserver obs)
    {
        if (_resolving == 0) {
            obs.mediaResolved();
        } else {
            _resolutionObs.add(obs);
        }
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

        getDirectionVector(
            _board.getLightAzimuth(idx), _board.getLightElevation(idx),
            _lights[idx].getDirection());

        float[] drgb = new Color(dcolor).getRGBColorComponents(null),
            argb = new Color(acolor).getRGBColorComponents(null);
        _lights[idx].getDiffuse().set(drgb[0], drgb[1], drgb[2], 1f);
        _lights[idx].getSpecular().set(drgb[0], drgb[1], drgb[2], 1f);
        _lights[idx].getAmbient().set(argb[0], argb[1], argb[2], 1f);
    }

    /**
     * Refreshes the fog state according to the board's fog parameters.
     */
    protected void refreshFog ()
    {
        float density = _board.getFogDensity();
        if (density == 0f) {
            _node.clearRenderState(RenderState.RS_FOG);
            _node.updateRenderState();
            return;
        }
        FogState fstate = (FogState)_node.getRenderState(RenderState.RS_FOG);
        if (fstate == null) {
            fstate = _ctx.getRenderer().createFogState();
            fstate.setDensityFunction(FogState.DF_EXP);
            _node.setRenderState(fstate);
            _node.updateRenderState();
        }
        fstate.setColor(RenderUtil.createColorRGBA(_board.getFogColor()));
        fstate.setDensity(density);
    }

    /**
     * Refreshes the background color based on the horizon color and fog
     * parameters.
     */
    protected void refreshBackgroundColor ()
    {
        int bg = (_board.getFogDensity() > 0f) ?
            _board.getFogColor() : _board.getSkyHorizonColor();
        _ctx.getRenderer().setBackgroundColor(RenderUtil.createColorRGBA(bg));
    }

    /**
     * Refreshes the shared wind influence according to the board's wind
     * parameters.
     */
    protected void refreshWindInfluence ()
    {
        float wdir = _board.getWindDirection();
        _wind.getWindDirection().set(
            -FastMath.cos(wdir), -FastMath.sin(wdir), 0f);
        _wind.setStrength(_board.getWindSpeed() * 0.0005f);
    }

    /**
     * Returns true if there are no pending or executing actions.
     */
    protected boolean noActions ()
    {
        if (!_pactions.isEmpty()) {
            return false;
        }
        for (int ex : _eunits.getValues()) {
            if (ex > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Processes all ready actions in the action queue.
     */
    protected void processActions ()
    {
        // clear out _punits and repopulate it with what's in eunits
        _punits.clear();
        int[] executers = _eunits.getKeys();
        for (int ii = 0; ii < executers.length; ii++) {
            if (_eunits.get(executers[ii]) > 0) {
                _punits.add(executers[ii]);
            }
        }
        // clear out _pbounds and repopulate it with what's in _ebounds
        _pbounds.clear();
        for (Rectangle r : _ebounds) {
            _pbounds.add(r);
        }

        _syncQueue.clear();

        Iterator<BoardAction> iter = _pactions.iterator();
        while (iter.hasNext()) {
            BoardAction action = iter.next();
            if (action.canExecute(_punits, _pbounds, _syncQueue)) {
                iter.remove();
                // this only queues up the action for processing, so we need
                // not worry that the action will complete immediately and
                // result in a recursive call to processActions()
                processAction(action);
            }
            notePending(action);
        }
    }

    /**
     * Processes the next action from the board action queue.
     */
    protected void processAction (final BoardAction action)
    {
        if (ACTION_DEBUG) {
            log.info("Posting p" + _punits + ": " + action);
        }

        // mark the pieces involved in this action as executing
        noteExecuting(action, 1);

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

                } catch (Throwable t) {
                    log.warning("Board action choked: " + action, t);
                }

                // note that this action is completed
                actionCompleted(action);
            }
        });
    }

    /**
     * Removes all pending and executing board actions from the queue.
     */
    protected void clearBoardActions ()
    {
        for (BoardAction action : _ractions) {
            action._cleared = true;
        }
        _ractions.clear();
        _pactions.clear();
        _eunits.clear();
        _ebounds.clear();
        _pbounds.clear();
        _punits.clear();
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
            log.info("Not updating, missing piece sprite", "opiece", opiece, "npiece", npiece,
                     "tick", tick);
            return;
        }

        // update the sprite with its new piece
        sprite.updated(npiece, tick);

        // if the piece has moved (which should only happen in the editor,
        // never in game), then go ahead and move it
        if (PieceSprite.isEditorMode()) {
            // it should finish immediately because we're in the editor
            if (sprite.updatePosition(_bangobj.board)) {
                log.warning("Piece moved along path after update!", "piece", npiece);
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
        log.info("Piece removed from DSet " + piece + ".");
        // TODO: queue up a fade out action like we do when a piece dies?
        removePieceSprite(piece.pieceId, "pieceRemoved(" + piece + ")");
    }

    /**
     * Creates a big text marquee in the middle of the screen.
     */
    protected void createMarquee (String text)
    {
        clearMarquee(0);

        // create the marquee, center it and display it
        addMarquee(_marquee = createMarqueeLabel(text),
            _ctx.getRenderer().getWidth()/2, _ctx.getRenderer().getHeight()/2);
    }

    /**
     * Generates the marquee label.
     */
    protected BLabel createMarqueeLabel (String text)
    {
        return new BLabel(text, "marquee");
    }

    /**
     * Clears our marquee display.
     */
    protected void clearMarquee (float fadeTime)
    {
        if (_marquee == null) {
            return;
        }
        if (fadeTime > 0f) {
            new ComponentFader(_marquee, new LinearTimeFunction(1f, 0f, fadeTime)) {
                public void fadeComplete () {
                    removeMarquee(_marquee);
                }
            }.start();
        } else {
            removeMarquee(_marquee);
        }
        _marquee = null;
    }

    /**
     * Updates the loading progress display.
     */
    protected void updateLoadingMarquee ()
    {
        // avoid recreating our marquee if not necessary
        int pct = _loaded * 100 / _toLoad;
        if (pct == _curpct) {
            return;
        }
        _curpct = pct;

        String pctstr = _ctx.xlate(
            GameCodes.GAME_MSGS, MessageBundle.tcompose(
                "m.loading_pct", String.valueOf(pct)));
        if (_loading == null) {
            addMarquee(_loading = new BLabel(pctstr, "loading_marquee"),
                _ctx.getRenderer().getWidth()/2, 100);
        } else {
            _loading.setText(pctstr);
        }
    }

    /**
     * Clears and turns off the loading progress display.
     */
    protected void clearLoadingMarquee ()
    {
        if (_loading != null) {
            removeMarquee(_loading);
            _loading = null;
        }
        _loaded = _toLoad = 0;
        _curpct = -1;
    }

    /**
     * Adds a marquee component centered about the given coordinates.
     */
    protected void addMarquee (BComponent marquee, int x, int y)
    {
        if (_mroot == null) {
            // create a bare bones root node above the normal one
            _ctx.getInterface().attachChild(_mroot = new BRootNode() {
                public long getTickStamp () {
                    return System.currentTimeMillis();
                }
                public void rootInvalidated (BComponent root) {
                    root.validate();
                }
            });
            _mroot.setZOrder(-2);

            // the layout centers its components about their positions
            AbsoluteLayout layout = new AbsoluteLayout() {
                public void layoutContainer (BContainer target) {
                    Insets insets = target.getInsets();
                    for (int ii = 0, cc = target.getComponentCount(); ii < cc; ii++) {
                        BComponent comp = target.getComponent(ii);
                        if (!comp.isVisible()) {
                            continue;
                        }
                        com.jmex.bui.util.Point p = (com.jmex.bui.util.Point)_spots.get(comp);
                        Dimension d = comp.getPreferredSize(-1, -1);
                        comp.setBounds(
                            insets.left + p.x - d.width / 2,
                            insets.bottom + p.y - d.height / 2,
                            d.width, d.height);
                    }
                }
            };
            _mwindow = new BWindow(_ctx.getStyleSheet(), layout) {
                public boolean isOverlay () {
                    return true;
                }
                public BComponent getHitComponent (int mx, int my) {
                    return null;
                }
            };
            _mroot.addWindow(_mwindow);
            _mwindow.setBounds(0, 0, _ctx.getRenderer().getWidth(),
                _ctx.getRenderer().getHeight());
        }
        _mwindow.add(marquee, new com.jmex.bui.util.Point(x, y));
    }

    /**
     * Removes a marquee component.
     */
    protected void removeMarquee (BComponent marquee)
    {
        _mwindow.remove(marquee);
        if (_mwindow.getComponentCount() == 0) {
            _mroot.removeWindow(_mwindow);
            _ctx.getInterface().detachChild(_mroot);
            _mwindow = null;
            _mroot = null;
        }
    }

    /**
     * Creates the piece sprite for the supplied piece. The newly created
     * sprite will automatically be initialized with the supplied piece and
     * added to the board view.
     */
    protected void createPieceSprite (Piece piece, short tick)
    {
        log.debug("Creating sprite for " + piece + ".");
        PieceSprite sprite = piece.createSprite();
        sprite.init(_ctx, this, _bangobj.board, _sounds, piece, tick);
        _pieces.put(piece.pieceId, sprite);
        addSprite(sprite);
    }

    /**
     * Removes the sprite associated with the specified piece.
     */
    protected PieceSprite removePieceSprite (int pieceId, String why)
    {
        PieceSprite sprite = _pieces.remove(pieceId);
        if (sprite != null) {
            log.debug("Removing sprite", "id", pieceId, "why", why);
            removeSprite(sprite);

        } else {
            log.warning("No sprite for removed piece", "id", pieceId, "why", why);
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
        _pieces.clear();
    }

    /**
     * Updates the sprites that the mouse is "hovering" over (the ones nearest
     * to the camera that are hit by the ray projecting from the camera to the
     * ground plane at the current mouse coordinates).  Instead of or in
     * addition to the primary hover sprite, there may be a sprite that has a
     * tooltip but is not specifically marked as hoverable.
     */
    protected void updateHoverSprites ()
    {
        Vector3f camloc = _ctx.getCameraHandler().getCamera().getLocation();
        _pick.clear();
        _pnode.findPick(new Ray(camloc, _worldMouse), _pick);
        float dist = Float.MAX_VALUE, tdist = Float.MAX_VALUE;
        Sprite hit = null, thit = null;
        for (int ii = 0; ii < _pick.getNumber(); ii++) {
            PickData pdata = _pick.getPickData(ii);
            if (notReallyAHit(pdata)) {
                continue;
            }
            Sprite s = getPieceSprite(pdata.getTargetMesh().getParentGeom());
            boolean hoverable = isHoverable(s), tooltip = hasTooltip(s);
            if (!hoverable && !tooltip) {
                continue;
            }
            float sdist = camloc.distanceSquared(s.getWorldTranslation());
            if (sdist < dist && hoverable) {
                hit = s;
                dist = sdist;
            }
            if (sdist < tdist && tooltip) {
                thit = s;
                tdist = sdist;
            }
        }

        // if nothing intersected the ray, look for a piece that intersects
        // the mouse's coordinates on the terrain
        if (hit == null || thit == null) {
            for (PieceSprite sprite : _pieces.values()) {
                if (sprite.getParent() != null &&
                    sprite.getPiece().intersects(_mouse.x, _mouse.y)) {
                    if (hit == null && isHoverable(sprite)) {
                        hit = sprite;
                    }
                    if (thit == null && hasTooltip(sprite)) {
                        thit = sprite;
                    }
                    if (hit != null && thit != null) {
                        break;
                    }
                }
            }
        }

        if (hit != _hover || thit != _thover) {
            hoverSpritesChanged(hit, thit);
        }
    }

    /**
     * Determine which of the highlight tiles the mouse is hovering over
     * and records that tile's coordinates in {@link #_high}.  It also
     * changes the color of the highlight.
     */
    protected void updateHighlightHover ()
    {
        TerrainNode.Highlight hover = null;

        // if hovering over a piece that has a highlight underneath it,
        // use that highlight
        if (_hover != null && _hover instanceof PieceSprite) {
            Piece piece = ((PieceSprite)_hover).getPiece();
            hover = _htiles.get(piece.getCoord());
        }

        // try picking against the highlight tile geometry (this is only
        // for floating tiles)
        if (hover == null) {
            Vector3f camloc =
                _ctx.getCameraHandler().getCamera().getLocation();
            _pick.clear();
            _hnode.findPick(new Ray(camloc, _worldMouse), _pick);
            float dist = Float.MAX_VALUE;

            for (int ii = 0; ii < _pick.getNumber(); ii++) {
                PickData pdata = _pick.getPickData(ii);
                if (notReallyAHit(pdata)) {
                    continue;
                }
                Spatial hit = pdata.getTargetMesh().getParentGeom();
                if (!(hit instanceof TerrainNode.Highlight)) {
                    continue;
                }
                TerrainNode.Highlight highlight = (TerrainNode.Highlight)hit;
                if (!highlight.hoverable) {
                    continue;
                }
                float hdist = FastMath.sqr(camloc.x - highlight.x) +
                    FastMath.sqr(camloc.y - highlight.y);
                if (hdist < dist) {
                    hover = highlight;
                    dist = hdist;
                }
            }
        }

        // look for a draped highlight at the terrain mouse coordinates
        if (hover == null) {
            hover = _htiles.get(Piece.coord(_mouse.x, _mouse.y));
        }

        // note the switch, if there is one
        if (hover != _highlightHover) {
            hoverHighlightChanged(hover);
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
     * This is called when the mouse is moved to hover over different
     * sprites (or none at all).
     *
     * @param hover the sprite over which the mouse is hovering
     * @param thover the tooltip-enabled sprite over which the mouse
     * is hovering
     */
    protected void hoverSpritesChanged (Sprite hover, Sprite thover)
    {
        _hover = hover;
        _thover = thover;
    }

    /**
     * This is called when the mouse is moved to hover over a different
     * highlight tile (or none at all).
     */
    protected void hoverHighlightChanged (TerrainNode.Highlight hover)
    {
        if (_highlightHover != null) {
            _highlightHover.setHover(false);
        }
        _highlightHover = hover;
        if (_highlightHover != null) {
            _highlightHover.setHover(true);
            _high.setLocation(_highlightHover.getTileX(),
                _highlightHover.getTileY());
        } else {
            _high.setLocation(-1, -1);
        }
    }

    /** Creates geometry to highlight the supplied set of movement tiles. */
    protected void highlightMovementTiles (
        PointSet set, PointSet goals, ColorRGBA highlightColor)
    {
        highlightTiles(set, goals, highlightColor, _movstate, true, true);
    }

    /** Creates geometry to highlight the supplied set of tiles. */
    protected void highlightTiles (
        PointSet set, ColorRGBA highlightColor, TextureState tstate,
        boolean flatten)
    {
        highlightTiles(set, null, highlightColor, tstate, flatten, false);
    }

    /** Creates geometry to highlight the supplied set of tiles. */
    protected void highlightTiles (
        PointSet set, PointSet goals, ColorRGBA highlightColor,
        TextureState tstate, boolean flatten, boolean hoverable)
    {
        ColorRGBA hoverHighlightColor =
            highlightColor.add(HOVER_HIGHLIGHT_COLOR);

        for (int ii = 0, ll = set.size(); ii < ll; ii++) {
            int tx = set.getX(ii), ty = set.getY(ii);
            TerrainNode.Highlight highlight =
                attachHighlight(tx, ty, flatten, hoverable);
            if (goals != null && goals.contains(tx, ty)) {
                highlight.setColors(getThrobbingColor(highlightColor),
                    hoverHighlightColor);
                highlight.setTextures(_goalstate, _goalhovstate);
            } else {
                highlight.setColors(highlightColor, hoverHighlightColor);
                highlight.setTextures(tstate, _movhovstate);
            }
            if (hoverable) {
                _htiles.put(Piece.coord(tx, ty), highlight);
            }
        }
        if (hoverable) {
            updateHighlightHover();
        }
    }

    /**
     * Gets a version of the specified color that will pulse on and off over
     * time.
     */
    protected ColorRGBA getThrobbingColor (final ColorRGBA color)
    {
        ColorRGBA throbber = _throbbers.get(color);
        if (throbber == null) {
            _throbbers.put(color, throbber = new ColorRGBA(color));
        }
        return throbber;
    }

    /**
     * Updates the throbbing colors used in the highlights.
     */
    protected void updateThrobbingColors ()
    {
        float v = FastMath.sin((System.currentTimeMillis() % THROB_PERIOD) *
            FastMath.TWO_PI / THROB_PERIOD) * 0.5f + 0.5f;
        for (Map.Entry<ColorRGBA, ColorRGBA> me : _throbbers.entrySet()) {
            me.getValue().a = FastMath.LERP(v, me.getKey().a, 1f);
        }
    }

    /**
     * Creates geometry to "target" the supplied set of tiles.
     *
     * @param valid If true then render normally, if false, make them
     * semi-transparent and red.
     */
    protected void targetTiles (PointSet set, boolean valid)
    {
        for (int ii = 0, ll = set.size(); ii < ll; ii++) {
            TerrainNode.Highlight highlight = attachHighlight(
                set.getX(ii), set.getY(ii), true, false);
            highlight.setRenderState(_tgtstate);
            highlight.updateRenderState();
            highlight.setDefaultColor(valid ? ColorRGBA.white : INVALID_TARGET_HIGHLIGHT_COLOR);
        }
    }

    /**
     * Attaches and returns a terrain highlight from the pool, creating a new
     * one if necessary.
     */
    protected TerrainNode.Highlight attachHighlight (
        int tx, int ty, boolean flatten, boolean hoverable)
    {
        for (TerrainNode.Highlight highlight : _highlights) {
            if (highlight.getParent() == null) {
                highlight.overPieces = flatten;
                highlight.flatten = flatten;
                highlight.hover = false;
                highlight.hoverable = hoverable;
                highlight.setPosition(tx, ty);
                _hnode.attachChild(highlight);
                return highlight;
            }
        }
        TerrainNode.Highlight highlight =
            _tnode.createHighlight(tx, ty, flatten, flatten);
        highlight.hoverable = hoverable;
        _hnode.attachChild(highlight);
        _highlights.add(highlight);
        return highlight;
    }

    /** Clears out all highlighted tiles. */
    protected void clearHighlights ()
    {
        hoverHighlightChanged(null);
        _hnode.detachAllChildren();
        _htiles.clear();
        _throbbers.clear();
    }

    /** JME peskily returns bogus hits when we do triangle level picking. */
    protected boolean notReallyAHit (PickData pdata)
    {
        List<?> tris = pdata.getTargetTris();
        Object mesh = pdata.getTargetMesh().getParentGeom();
        return (tris == null || tris.size() == 0 || !(mesh instanceof TriMesh));
    }

    protected void notePending (BoardAction action)
    {
        for (int ii = 0; ii < action.pieceIds.length; ii++) {
            if (action.pieceIds[ii] > 0) {
                _punits.add(action.pieceIds[ii]);
            }
        }
        if (action.bounds != null) {
            for (Rectangle b : action.bounds) {
                _pbounds.add(b);
            }
        }
        if (action instanceof SyncAction) {
            _syncQueue.addLast(action.actionId);
        }
    }

    /** Used to increment and decrement executing status for pieces. */
    protected void noteExecuting (BoardAction action, int delta)
    {
        for (int ii = 0; ii < action.pieceIds.length; ii++) {
            if (action.pieceIds[ii] > 0) {
                _eunits.increment(action.pieceIds[ii], delta);
            }
        }
        if (action.bounds != null) {
            for (Rectangle b : action.bounds) {
                for (int ii = 0; ii < Math.abs(delta); ii++) {
                    if (delta > 0) {
                        _ebounds.add(b);
                    } else {
                        _ebounds.remove(b);
                    }
                }
            }
        }
    }

    /**
     * Computes light azimuth and elevation values to a direction vector.
     */
    protected static Vector3f getDirectionVector (
        float azimuth, float elevation, Vector3f result)
    {
        if (result == null) {
            result = new Vector3f();
        }
        float cose = FastMath.cos(elevation);
        result.set(-cose * FastMath.cos(azimuth),
            -cose * FastMath.sin(azimuth),
            -FastMath.sin(elevation));
        return result;
    }

    /**
     * Returns the azimuth of the given direction vector.
     */
    protected static float getAzimuth (Vector3f direction)
    {
        return FastMath.atan2(-direction.y, -direction.x);
    }

    /**
     * Returns the elevation of the given direction vector.
     */
    protected static float getElevation (Vector3f direction)
    {
        return FastMath.asin(-direction.z);
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
            this.moveIds = new int[0];
            this.bounds = new Rectangle[] {
                new Rectangle(piece.x, piece.y, 1, 1) };
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
            this.moveIds = new int[0];
        }

        public boolean execute () {
            pieceUpdated(opiece, npiece, tick);
            return false;
        }

        public String toString () {
            String strval = super.toString();
            if (opiece != null) {
                strval += ":" + opiece;
            }
            if (npiece != null) {
                strval += "->" + npiece;
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
            this.moveIds = new int[0];
            this.bounds = new Rectangle[] {
                new Rectangle(piece.x, piece.y, 1, 1) };
        }

        public boolean execute () {
            pieceRemoved(piece, tick);
            return false;
        }

        public String toString () {
            return super.toString() + ":" + piece;
        }
    }

    /** Used to sync all the board actions on a tick and compare the server
     * state to the client state. */
    protected class SyncAction extends BoardAction
    {
        public short tick;

        public SyncAction (short tick) {
            this.pieceIds = new int[0];
            this.waiterIds = new int[0];
            this.moveIds = new int[0];
            this.bounds = null;
            this.tick = tick;

            for (Piece p : _bangobj.debugPieces) {
                _spieces.add((Piece)p.clone());
            }
        }

        public void removePiece (Piece piece)
        {
            for (Iterator<Piece> iter = _spieces.iterator(); iter.hasNext(); ) {
                if (iter.next().pieceId == piece.pieceId) {
                    iter.remove();
                    break;
                }
            }
        }

        @Override // documentation inherited
        public boolean canExecute (ArrayIntSet penders,
                HashSet<Rectangle> boundset, LinkedList<Integer> syncQueue)
        {
            return (penders.isEmpty() && boundset.isEmpty() &&
                    syncQueue.isEmpty());
        }

        public boolean execute () {
            for (Piece p : _bangobj.pieces) {
                _cpieces.add((Piece)p.clone());
            }
            pieceComparator pc = new pieceComparator();
            Collections.sort(_cpieces, pc);
            Collections.sort(_spieces, pc);
            boolean errors = false;
            Iterator<Piece> citer = _cpieces.iterator(),
                    siter = _spieces.iterator();
            Piece cpiece = null, spiece = null;
            if (citer.hasNext()) {
                cpiece = citer.next();
            }
            if (siter.hasNext()) {
                spiece = siter.next();
            }
            while (cpiece != null || spiece != null) {
                int comp = pc.compare(cpiece, spiece);
                if (comp == 0) {
                    if (citer.hasNext()) {
                        cpiece = citer.next();
                    } else {
                        cpiece = null;
                    }
                    if (siter.hasNext()) {
                        spiece = siter.next();
                    } else {
                        spiece = null;
                    }
                } else if (comp < 0) {
                    log.warning("Client has extra piece", "piece", cpiece);
                    errors = true;
                    if (citer.hasNext()) {
                        cpiece = citer.next();
                    } else {
                        cpiece = null;
                    }
                } else {
                    log.warning("Server has extra piece", "piece", spiece);
                    errors = true;
                    if (siter.hasNext()) {
                        spiece = siter.next();
                    } else {
                        spiece = null;
                    }
                }
            }
            if (!errors) {
                log.info("Sync on tick " + tick + " passed.");
            } else {
                System.exit(0);
            }
            return false;
        }

        protected ArrayList<Piece> _cpieces = new ArrayList<Piece>(),
                                   _spieces = new ArrayList<Piece>();
    }

    protected static class pieceComparator
        implements Comparator<Piece>
    {
        public int compare (Piece p1, Piece p2)
        {
            if (p1 == null && p2 == null) {
                return 0;
            } else if (p1 == null) {
                return 1;
            } else if (p2 == null) {
                return -1;
            }
            return p1.pieceId - p2.pieceId;
        }
    }

    /** Listens for various different events and does the right thing. */
    protected class BoardEventListener
        implements SetListener<Piece>, AttributeChangeListener
    {
        public void entryAdded (EntryAddedEvent<Piece> event) {
            if (event.getName().equals(BangObject.PIECES)) {
                queuePieceCreate(event.getEntry(), _bangobj.tick);
            }
        }

        public void entryUpdated (EntryUpdatedEvent<Piece> event) {
            if (event.getName().equals(BangObject.PIECES)) {
                queuePieceUpdate(event.getOldEntry(), event.getEntry());
            }
        }

        public void entryRemoved (EntryRemovedEvent<Piece> event) {
            if (event.getName().equals(BangObject.PIECES)) {
                queuePieceRemoval(event.getOldEntry());
            }
        }

        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(BangObject.DEBUG_PIECES)) {
                queueSyncAction();
            }
        }
    };

    /** Used to delay actions until we're done resolving our sprites. */
    protected interface ResolutionObserver
    {
        public void mediaResolved ();
    };

    /** Fades a component in or out by manipulating its alpha value. */
    protected class ComponentFader extends Controller
    {
        public ComponentFader (BComponent target, TimeFunction tfunc) {
            _target = target;
            _tfunc = tfunc;
        }

        public void start () {
            _ctx.getRootNode().addController(this);
        }

        public void update (float time) {
            _target.setAlpha(_tfunc.getValue(time));
            if (_tfunc.isComplete()) {
                _ctx.getRootNode().removeController(this);
                fadeComplete();
            }
        }

        public void fadeComplete () {
            // nothing by default
        }

        protected BComponent _target;
        protected TimeFunction _tfunc;
    }

    protected BasicContext _ctx;
    protected boolean _editorMode;
    protected BangObject _bangobj;
    protected BangBoard _board;
    protected Rectangle _bbounds;
    protected BoardEventListener _blistener = new BoardEventListener();

    protected BRootNode _mroot;
    protected BWindow _mwindow;
    protected BLabel _marquee, _loading;
    protected int _toLoad, _loaded, _curpct = -1;

    protected Node _node, _pnode, _hnode, _texnode;
    protected LightState _lstate;
    protected DirectionalLight[] _lights;
    protected SkyNode _snode;
    protected TerrainNode _tnode;
    protected WaterNode _wnode;
    protected SimpleParticleInfluenceFactory.BasicWind _wind;
    protected Vector3f _worldMouse;
    protected Ray _pray = new Ray();
    protected TrianglePickResults _pick = new TrianglePickResults();
    protected Sprite _hover, _thover;

    protected ArrayList<BoardAction> _ractions = new ArrayList<BoardAction>();
    protected ArrayList<BoardAction> _pactions = new ArrayList<BoardAction>();
    protected IntIntMap _eunits = new IntIntMap();
    protected ArrayIntSet _punits = new ArrayIntSet();
    protected ArrayList<Rectangle> _ebounds = new ArrayList<Rectangle>();
    protected HashSet<Rectangle> _pbounds = new HashSet<Rectangle>();

    /** Used to texture a quad that "targets" a tile. */
    protected TextureState _tgtstate;

    /** Used to texture movement highlights. */
    protected TextureState _movstate, _movhovstate, _goalstate, _goalhovstate;

    // TODO: rename _hastate
    protected AlphaState _hastate;

    /** The current tile coordinates of the mouse. */
    protected Point _mouse = new Point(-1, -1);

    /** The pool of terrain highlights created for repeated use. */
    protected ArrayList<TerrainNode.Highlight> _highlights =
        new ArrayList<TerrainNode.Highlight>();

    /** Maps encoded tile coordinates to the hoverable highlights
     * occupying those tiles. */
    protected HashIntMap<TerrainNode.Highlight> _htiles =
        new HashIntMap<TerrainNode.Highlight>();

    /** The tile coordinates of the highlight tile that the mouse is
     * hovering over or (-1, -1). */
    protected Point _high = new Point(-1, -1);

    /** The highlight currently being hovered over. */
    protected TerrainNode.Highlight _highlightHover;

    /** The grid indicating where the tile boundaries lie. */
    protected GridNode _grid;

    /** Used to load all in-game sounds. */
    protected SoundGroup _sounds;

    protected HashMap<Integer,PieceSprite> _pieces =
        new HashMap<Integer,PieceSprite>();

    /** Used to track board elements in the process of resolution. */
    protected int _resolving;

    /** Used to keep track of observers that want to know when our sprites are
     * resolved and we're ready to roll. */
    protected ArrayList<ResolutionObserver> _resolutionObs =
        new ArrayList<ResolutionObserver>();

    /** Used to fade ourselves in at the start of the game. */
    protected FadeInOutEffect _fadein;

    protected HashMap<Spatial,Sprite> _plights =
        new HashMap<Spatial,Sprite>();

    /** Maps normal colors to their intensity-throbbing equivalents. */
    protected HashMap<ColorRGBA, ColorRGBA> _throbbers =
        new HashMap<ColorRGBA, ColorRGBA>();

    /** Temporary result variables. */
    protected ColorRGBA _color = new ColorRGBA();

    /** The max id action. */
    protected int _maxActionId = 0;

    protected LinkedList<Integer> _syncQueue = new LinkedList<Integer>();
    protected ArrayList<SyncAction> _syncActions = new ArrayList<SyncAction>();

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

    /** The color of the movement highlights when the mouse is hovering. */
    protected static final ColorRGBA HOVER_HIGHLIGHT_COLOR =
        new ColorRGBA(0f, 0f, 0f, 0.5f);

    /** The color of the target highlights when the target is invalid. */
    protected static final ColorRGBA INVALID_TARGET_HIGHLIGHT_COLOR =
        new ColorRGBA(1f, 0f, 0f, 0.5f);

    /** The time in milliseconds that it takes to complete one throb cycle. */
    protected static final long THROB_PERIOD = 1000L;

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
