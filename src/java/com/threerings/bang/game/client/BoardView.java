//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.HashMap;
import java.util.Iterator;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.intersection.TrianglePickResults;
import com.jme.light.DirectionalLight;
import com.jme.light.SimpleLightNode;
import com.jme.math.FastMath;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.lod.AreaClodMesh;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.util.geom.BufferUtils;

import com.jmex.bui.BComponent;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseMotionListener;
import com.jmex.bui.event.MouseWheelListener;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.jme.input.GodViewHandler;
import com.threerings.jme.sprite.Sprite;
import com.threerings.openal.SoundGroup;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
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
    implements MouseMotionListener, MouseWheelListener
{
    public BoardView (BasicContext ctx)
    {
        _ctx = ctx;

        // create our top-level node
        _node = new Node("board_view");

        // create a sky box
        _node.attachChild(new SkyNode(ctx));
        
        // create some fake ground
        _node.attachChild(_gnode = new Node("ground"));
        refreshGround();

        // we'll hang the board geometry off this node
        Node bnode = new Node("board");
        _node.attachChild(bnode);
        bnode.attachChild(_tnode = new TerrainNode(ctx));
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
        _tgtstate = RenderUtil.createTexture(
            ctx, ctx.loadImage("textures/ustatus/crosshairs.png"));

        // this is used to indicate where you can move
        _movstate = RenderUtil.createTexture(
            ctx, ctx.loadImage("textures/ustatus/movement.png"));
            
        // create a sound group that we'll use for all in-game sounds
        _sounds = ctx.getSoundManager().createGroup(
            BangUI.clipprov, GAME_SOURCE_COUNT);
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
    public void startGame (BangObject bangobj, BangConfig cfg, int pidx)
    {
        // clear out old piece sprites from a previous game
        for (Iterator<PieceSprite> iter = _pieces.values().iterator();
             iter.hasNext(); ) {
            removeSprite(iter.next());
            iter.remove();
        }

        // add the listener that will react to pertinent events
        _bangobj = bangobj;
        _bangobj.addListener(_blistener);

        // reset the camera for this board
        CameraHandler ch = (CameraHandler)_ctx.getInputHandler();
        if (ch != null) {
            ch.setBoardDimens(bangobj, pidx);
        }

        // freshen up
        refreshBoard();

        // clear any previous round's marquee
        clearMarquee();
    }

    /**
     * Called by the editor when the entire board has changed.
     */
    public void refreshBoard ()
    {
        // remove any old sprites
        for (PieceSprite sprite : _pieces.values()) {
            removeSprite(sprite);
        }
        _pieces.clear();

        // start afresh
        _board = _bangobj.board;
        _board.shadowPieces(_bangobj.pieces.iterator());
        _bbounds = new Rectangle(0, 0, _board.getWidth(), _board.getHeight());

        // refresh the ground as well
        refreshGround();
        
        // create the board geometry
        _tnode.createBoardTerrain(_board);
        _wnode.createBoardWater(_board);
        
        // create sprites for all of the pieces
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            // this will trigger the creation, initialization and whatnot
            pieceUpdated(null, (Piece)iter.next());
        }
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
     * Called by the controller when our game has ended.
     */
    public void endGame ()
    {
        // remove our event listener
        _bangobj.removeListener(_blistener);
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
    }

    /**
     * Removes a sprite from the active view.
     */
    public void removeSprite (Sprite sprite)
    {
        _pnode.detachChild(sprite);
    }

    /**
     * Returns true if the specified sprite is part of the active view.
     */
    public boolean isManaged (PieceSprite sprite)
    {
        return _pnode.hasChild(sprite);
    }

    /**
     * Called when a piece is updated in the game object, though sometimes
     * not immediately but after various effects have been visualized.
     */
    public void pieceUpdated (Piece opiece, Piece npiece)
    {
        if (npiece != null) {
            getPieceSprite(npiece).updated(
                _bangobj.board, npiece, _bangobj.tick);
        }
    }        

    // documentation inherited from interface MouseMotionListener
    public void mouseMoved (MouseEvent e)
    {
        Vector3f ground = getGroundIntersect(e, null); 

        int mx = (int)Math.floor(ground.x / TILE_SIZE);
        int my = (int)Math.floor(ground.y / TILE_SIZE);
        ground.x = (float)mx *TILE_SIZE + TILE_SIZE/2;
        ground.y = (float)my * TILE_SIZE + TILE_SIZE/2;
//         _cursor.setLocalTranslation(ground);

        // update the sprite over which the mouse is hovering
        Sprite hover = updateHoverSprite();
        if (hover != _hover) {
            hoverSpriteChanged(hover);
        }

        // if we have highlight tiles, determine which of those the mouse
        // is over
        if (_hnode.getQuantity() > 0) {
            updateHighlightHover();
        }

        if (mx != _mouse.x || my != _mouse.y) {
            _mouse.x = mx;
            _mouse.y = my;
            hoverTileChanged(_mouse.x, _mouse.y);
        }
    }

    /**
     * Given a mouse event, returns the point at which a ray cast from the
     * eye through the mouse pointer intersects the ground plane.
     *
     * @param result a vector to hold the result, or <code>null</code> to
     * create a new vector
     * @return a reference to the result
     */
    public Vector3f getGroundIntersect (MouseEvent e, Vector3f result)
    {
        if (result == null) {
            result = new Vector3f();
        }
        
        // determine which tile the mouse is over
        Vector2f screenPos = new Vector2f(e.getX(), e.getY());
        _worldMouse = _ctx.getDisplay().getWorldCoordinates(screenPos, 0);
        _worldMouse.subtractLocal(_ctx.getCamera().getLocation());

        // determine which tile the mouse is over
        float dist = -1f * _groundNormal.dot(_ctx.getCamera().getLocation()) /
            _groundNormal.dot(_worldMouse);
        _ctx.getCamera().getLocation().add(
            _worldMouse.mult(dist), result);
        result.z = 0.1f;
        
        return result;
    }
     
    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (MouseEvent e)
    {
        // first update our mousely business
        mouseMoved(e);
    }

    // documentation inherited from interface MouseWheelListener
    public void mouseWheeled (MouseEvent e)
    {
        GodViewHandler ih = (GodViewHandler)_ctx.getInputHandler();
        if ((e.getModifiers() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
            float zoom = ih.getZoomLevel();
            if (e.getDelta() > 0) {
                zoom = Math.max(0f, zoom - 0.1f);
            } else {
                zoom = Math.min(1f, zoom + 0.1f);
            }
            ih.setZoomLevel(zoom);

        } else if (!ih.isRotating()) {
            ih.rotateCamera((e.getDelta() > 0) ?
                            -FastMath.PI/2 : FastMath.PI/2);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // add our geometry into the scene graph
        _ctx.getGeometry().attachChild(_node);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // remove our geometry from the scene graph
        _ctx.getGeometry().detachChild(_node);

        // clear any marquee we have up
        clearMarquee();
    }
    
    /**
     * Creates the geometry that defines the ground around and behind the
     * board.
     */
    protected void refreshGround ()
    {
        _gnode.detachAllChildren();

        if (_board == null) {
            _gnode.attachChild(createGroundSection(-GROUND_SIZE/2,
                -GROUND_SIZE/2, GROUND_SIZE, GROUND_SIZE));
            
        } else {
            int tbheight = (GROUND_SIZE - _board.getHeight())/2,
                lrwidth = (GROUND_SIZE - _board.getWidth())/2,
                lrheight = _board.getHeight();
                
            _gnode.attachChild(createGroundSection(-lrwidth, +lrheight,
                GROUND_SIZE, tbheight)); // top
            _gnode.attachChild(createGroundSection(-lrwidth, -tbheight,
                GROUND_SIZE, tbheight)); // bottom
            _gnode.attachChild(createGroundSection(-lrwidth, 0, lrwidth,
                lrheight)); // left
            _gnode.attachChild(createGroundSection(_board.getWidth(), 0,
                lrwidth, lrheight)); // right
        }
        
        _gnode.setLightCombineMode(LightState.OFF);
        _gnode.updateRenderState();
    }

    /**
     * Creates a section of the ground around the board with the specified tile
     * coordinate dimensions.
     */
    protected Quad createGroundSection (int x, int y, int width, int height)
    {
        float w = width*TILE_SIZE, h = height*TILE_SIZE;
        Quad section = new Quad("section", w, h);
        section.setLocalTranslation(new Vector3f(x*TILE_SIZE + w/2,
            y*TILE_SIZE + h/2, 0f));
            
        // tile the texture over the quad
        Texture texture = RenderUtil.getGroundTexture(
            Terrain.OUTER).getTexture().createSimpleClone();
        texture.setScale(new Vector3f(width, height, 1));
        TextureState tstate =
            _ctx.getDisplay().getRenderer().createTextureState();
        tstate.setTexture(texture);
        section.setRenderState(tstate);
        
        return section;
    }
    
    /**
     * Creates a big text marquee in the middle of the screen.
     */
    protected void createMarquee (String text)
    {
        if (_marquee != null) {
            clearMarquee();
        }
        _marquee = new BDecoratedWindow(BangUI.marqueeLNF, null);
        _marquee.add(new BLabel(text), BorderLayout.CENTER);
        _ctx.getRootNode().addWindow(_marquee);
        _marquee.pack();
        _marquee.center();
    }

    /**
     * Clears our marquee display.
     */
    protected void clearMarquee ()
    {
        if (_marquee != null) {
            _ctx.getRootNode().removeWindow(_marquee);
            _marquee = null;
        }
    }

    /**
     * Returns (creating if necessary) the piece sprite associated with
     * the supplied piece. A newly created sprite will automatically be
     * initialized with the supplied piece and added to the board view.
     */
    protected PieceSprite getPieceSprite (Piece piece)
    {
        PieceSprite sprite = _pieces.get(piece.pieceId);
        if (sprite == null) {
            sprite = piece.createSprite();
            sprite.init(_ctx, this, _sounds, piece, _bangobj.tick);
            log.fine("Creating sprite for " + piece + ".");
            _pieces.put((int)piece.pieceId, sprite);
            addSprite(sprite);
        }
        return sprite;
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
     * Returns the sprite that the mouse is "hovering" over (the one
     * nearest to the camera that is hit by the ray projecting from the
     * camera to the ground plane at the current mouse coordinates). This
     * method also recomputes the "highlight tile" over which the mouse is
     * hovering as those can differ from the "hover" tile due to their
     * elevation.
     */
    protected Sprite updateHoverSprite ()
    {
        Vector3f camloc = _ctx.getCamera().getLocation();
        _pick.clear();
        _pnode.findPick(new Ray(camloc, _worldMouse), _pick);
        float dist = Float.MAX_VALUE;
        Sprite hit = null;
        for (int ii = 0; ii < _pick.getNumber(); ii++) {
            Sprite s = getSprite(_pick.getPickData(ii).getTargetMesh());
            if (!isHoverable(s)) {
                continue;
            }
            float sdist = camloc.distanceSquared(s.getWorldTranslation());
            if (sdist < dist) {
                hit = s;
                dist = sdist;
            }
        }
        return hit;
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
        Vector3f camloc = _ctx.getCamera().getLocation();
        _pick.clear();
        _hnode.findPick(new Ray(camloc, _worldMouse), _pick);
        for (int ii = 0; ii < _pick.getNumber(); ii++) {
            Geometry tmesh = _pick.getPickData(ii).getTargetMesh();
            if (tmesh instanceof TerrainNode.Highlight) {
                TerrainNode.Highlight highlight = (TerrainNode.Highlight)tmesh;
                _high.x = highlight.x;
                _high.y = highlight.y;
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
        if (_hover instanceof UnitSprite) {
            ((UnitSprite)_hover).setHovered(false);
        }
        _hover = hover;
        if (_hover instanceof UnitSprite) {
            ((UnitSprite)_hover).setHovered(true);
        }
    }

    /** Creates geometry to highlight the supplied set of tiles. */
    protected void highlightTiles (PointSet set, boolean forFlyer)
    {
        for (int ii = 0, ll = set.size(); ii < ll; ii++) {
            TerrainNode.Highlight highlight = _tnode.createHighlight(
                set.getX(ii), set.getY(ii), true);
            highlight.setRenderState(_movstate);
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

    /**
     * Returns the sprite associated with this spatial or null if the
     * spatial is not part of a sprite.
     */
    protected Sprite getSprite (Spatial spatial)
    {
        if (spatial instanceof Sprite) {
            return (Sprite)spatial;
        } else if (spatial.getParent() != null) {
            return getSprite(spatial.getParent());
        } else {
            return null;
        }
    }

    /** Listens for various different events and does the right thing. */
    protected class BoardEventListener
        implements SetListener
    {
        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(BangObject.PIECES)) {
                pieceUpdated(null, (Piece)event.getEntry());
            }
        }

        public void entryUpdated (EntryUpdatedEvent event) {
            if (event.getName().equals(BangObject.PIECES)) {
                pieceUpdated((Piece)event.getOldEntry(),
                             (Piece)event.getEntry());
            }
        }

        public void entryRemoved (EntryRemovedEvent event) {
            if (event.getName().equals(BangObject.PIECES)) {
                removePieceSprite((Integer)event.getKey(), "pieceRemoved");
                pieceUpdated((Piece)event.getOldEntry(), null);
            }
        }
    };

    protected BasicContext _ctx;
    protected BangObject _bangobj;
    protected BangBoard _board;
    protected Rectangle _bbounds;
    protected BoardEventListener _blistener = new BoardEventListener();

    protected BDecoratedWindow _marquee;

    protected HashMap _fmasks = new HashMap();

    protected Node _node, _pnode, _hnode, _gnode;
    protected TerrainNode _tnode;
    protected WaterNode _wnode;
    protected Vector3f _worldMouse;
    protected TrianglePickResults _pick = new TrianglePickResults();
    protected Sprite _hover;
    
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

    /** Used to load all in-game sounds. */
    protected SoundGroup _sounds;

    protected HashMap<Integer,PieceSprite> _pieces =
        new HashMap<Integer,PieceSprite>();

    /** Used when intersecting the ground. */
    protected static final Vector3f _groundNormal = new Vector3f(0, 0, 1);

    protected static final ColorRGBA DARK =
        new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f);
    protected static final ColorRGBA LIGHT =
        new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f);
    protected static final ColorRGBA BROWN =
        new ColorRGBA(204/255f, 153/255f, 51/255f, 1.0f);

    /** The number of simultaneous sound "sources" available to the game. */
    protected static final int GAME_SOURCE_COUNT = 10;
    
    /** The size of the surrounding ground, in tiles. */
    protected static final int GROUND_SIZE = 100;
}
