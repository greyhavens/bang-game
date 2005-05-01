//
// $Id$

package com.threerings.bang.client;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.Rectangle;

import java.util.HashMap;
import java.util.Iterator;

import com.jme.bounding.BoundingBox;
import com.jme.bui.BComponent;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Box;

import com.threerings.jme.sprite.Sprite;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.client.sprite.UnitSprite;
import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.BangConfig;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.Terrain;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.PointSet;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Provides a shared base for displaying the board that is extended to
 * create the actual game view as well as the level editor.
 */
public class BoardView extends BComponent
{
    public BoardView (BangContext ctx)
    {
        _ctx = ctx;

        // configure ourselves as the default mouse target
        _ctx.getInputDispatcher().setDefaultMouseTarget(this);

        // we don't actually want to render in orthographic mode
        setRenderQueueMode(Renderer.QUEUE_INHERIT);

        // we'll hang the board geometry off this node
        attachChild(_bnode = new Node("board"));
        for (int yy = 0; yy < 16; yy++) {
            for (int xx = 0; xx < 16; xx++) {
                int bx = xx * TILE_SIZE, by = yy * TILE_SIZE;
                Vector3f min = new Vector3f(bx, by, -1);
                Vector3f max = new Vector3f(bx+TILE_SIZE, by+TILE_SIZE, 0);
                Box t = new Box("Box", min, max);
                t.setModelBound(new BoundingBox());
                t.updateModelBound();
                t.setSolidColor((xx + yy) % 2 == 0 ? DARK : LIGHT);
                _bnode.attachChild(t);
            }
        }
        _bnode.updateRenderState();
        _bnode.updateGeometricState(0f, true);

        // we'll hang all of our sprites off this node
        attachChild(_snode = new Node("sprites"));
        _snode.updateRenderState();
        _snode.updateGeometricState(0f, true);
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

        // freshen up
        refreshBoard();
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

        // create sprites for all of the pieces
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            // this will trigger the creation, initialization and whatnot
            pieceUpdated(null, (Piece)iter.next());
        }

        // if the board is smaller than we are, center it in our view
        centerBoard();
    }

    /**
     * Called by the controller when the round has ended.
     */
    public void endRound ()
    {
        // remove our event listener
        _bangobj.removeListener(_blistener);

        createMarquee("Round over!");
    }

    /**
     * Called by the controller when our game has ended.
     */
    public void endGame ()
    {
        // remove our event listener
        _bangobj.removeListener(_blistener);

        // create a giant game over label and render it
        StringBuffer winners = new StringBuffer();
        for (int ii = 0; ii < _bangobj.winners.length; ii++) {
            if (_bangobj.winners[ii]) {
                if (winners.length() > 0) {
                    winners.append(", ");
                }
                winners.append(_bangobj.players[ii]);
            }
        }
        String wtext = "Game Over!\n";
        if (winners.length() > 1) {
            wtext += "Winner: " + winners;
        } else if (winners.length() > 0) {
            wtext += "Winner: " + winners;
        } else {
            wtext += "No winner!";
        }
        createMarquee(wtext);
    }

    /**
     * Creates a big animation that scrolls up the middle of the board.
     */
    protected void createMarquee (String text)
    {
//         Label label = new Label(text, Color.white, getFont().deriveFont(40f));
//         label.setAlignment(Label.CENTER);
//         label.setStyle(Label.OUTLINE);
//         label.setAlternateColor(Color.black);
//         label.setTargetWidth(300);
//         label.layout(this);

//         _marquee = new LabelSprite(label);
//         _marquee.setRenderOrder(100);
//         _marquee.setLocation(
//             _vbounds.x+(_vbounds.width-label.getSize().width)/2,
//             _vbounds.y+(_vbounds.height-label.getSize().height)/2);
//         addSprite(_marquee);
    }

    /**
     * Adds a sprite to the active view.
     */
    public void addSprite (Sprite sprite)
    {
        _snode.attachChild(sprite);
        sprite.updateRenderState();
        sprite.updateGeometricState(0.0f, true);
    }

    /**
     * Removes a sprite from the active view.
     */
    public void removeSprite (Sprite sprite)
    {
        _snode.detachChild(sprite);
    }

    /**
     * Returns true if the specified sprite is part of the active view.
     */
    public boolean isManaged (PieceSprite sprite)
    {
        return _snode.hasChild(sprite);
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

//         if (_marquee != null) {
//             removeSprite(_marquee);
//             _marquee = null;
//         }
    }

    /** Relocates our virtual view to center the board iff it is smaller
     * than the viewport. */
    protected void centerBoard ()
    {
//         int width = _board.getWidth() * SQUARE,
//             height = _board.getHeight() * SQUARE;
//         int nx = _vbounds.x, ny = _vbounds.y;
//         if (width < _vbounds.width) {
//             nx = (width - _vbounds.width) / 2;
//         }
//         if (height < _vbounds.height) {
//             ny = (height - _vbounds.height) / 2;
//         }
//         setViewLocation(nx, ny);
    }

//     protected Color getColor (Terrain tile)
//     {
//         Color color = null;
//         switch (tile) {
//         case DIRT: color = Color.orange.darker(); break;
//         case ROAD: color = Color.gray; break;
//         case TALL_GRASS: color = Color.green; break;
//         case WATER: color = Color.blue; break;
//         case LEAF_BRIDGE: color = Color.lightGray; break;
//         default: color = Color.black; break;
//         }
//         return color;
//     }

    /**
     * This should be called when the mouse moves to update our notion of
     * which tile the mouse is hovering over.
     */
    protected boolean updateMouseTile (int mx, int my)
    {
        if (mx != _mouse.x || my != _mouse.y) {
            _mouse.x = mx;
            _mouse.y = my;
            return true;
        }
        return false;
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
            sprite.init(_ctx, piece, _bangobj.tick);
            _pieces.put((int)piece.pieceId, sprite);
            addSprite(sprite);
        }
        return sprite;
    }

    /**
     * Removes the sprite associated with the specified piece.
     */
    protected void removePieceSprite (int pieceId)
    {
        PieceSprite sprite = _pieces.remove(pieceId);
        if (sprite != null) {
            removeSprite(sprite);
        } else {
            log.warning("No sprite for removed piece [id=" + pieceId + "].");
        }
    }

    protected void clearAttackSet ()
    {
        if (_attackSet != null) {
            removeSet(_attackSet);
            _attackSet = null;
        }
    }

    protected void removeSet (PointSet set)
    {
        for (int ii = 0, ll = set.size(); ii < ll; ii++) {
//             dirtyTile(set.getX(ii), set.getY(ii));
        }
    }

    /** Called when a piece is updated in the game object. */
    protected void pieceUpdated (Piece opiece, Piece npiece)
    {
        if (npiece != null) {
            getPieceSprite(npiece).updated(
                _bangobj.board, npiece, _bangobj.tick);
        }
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
                Piece piece = (Piece)event.getEntry();
                getPieceSprite(piece);
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
                removePieceSprite((Integer)event.getKey());
                pieceUpdated((Piece)event.getOldEntry(), null);
            }
        }
    };

//     /** Used to render the contents of a point set. */
//     protected static interface SetRenderer
//     {
//         public Composite getComposite ();
//         public Stroke getStroke ();
//         public void render (Graphics2D gfx, Rectangle tile);
//     }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected BangBoard _board;
    protected Rectangle _bbounds;
    protected BoardEventListener _blistener = new BoardEventListener();

    protected Node _snode, _bnode;

    protected PointSet _attackSet;

    /** The current tile coordinates of the mouse. */
    protected Point _mouse = new Point(-1, -1);

//     /** Displayed over top of the board. */
//     protected LabelSprite _marquee;

    protected HashMap<Integer,PieceSprite> _pieces =
        new HashMap<Integer,PieceSprite>();

//     /** Used during rendering. */
//     protected Rectangle _pr = new Rectangle(0, 0, SQUARE, SQUARE);

//     /** Renders a cross-hair over the specified coordinate. */
//     protected static SetRenderer _crosshairRenderer = new SetRenderer() {
//         public Composite getComposite () {
//             return null;
//         }
//         public Stroke getStroke () {
//             return null; // _thick;
//         }
//         public void render (Graphics2D gfx, Rectangle tile) {
//             gfx.setColor(Color.white);
//             gfx.drawOval(tile.x+2, tile.y+2, tile.width-4, tile.height-4);
//             gfx.drawOval(tile.x+10, tile.y+10, tile.width-20, tile.height-20);
//             int cx = tile.x+tile.width/2, cy = tile.y+tile.height/2;
//             gfx.drawLine(cx, tile.y+5, cx, tile.y+15);
//             gfx.drawLine(cx, tile.y+tile.height-5, cx, tile.y+tile.height-15);
//             gfx.drawLine(tile.x+5, cy, tile.x+15, cy);
//             gfx.drawLine(tile.x+tile.width-5, cy, tile.x+tile.width-15, cy);
//         }
//         protected Stroke _thick = new BasicStroke(2);
//     };

//     /** Renders a white rounded rectangle over the specified coordinate. */
//     protected static SetRenderer _whiteRenderer = new SetRenderer() {
//         public Composite getComposite () {
//             return SET_ALPHA;
//         }
//         public Stroke getStroke () {
//             return null;
//         }
//         public void render (Graphics2D gfx, Rectangle tile) {
//             gfx.setColor(Color.white);
//             gfx.fillRoundRect(tile.x+2, tile.y+2,
//                               tile.width-4, tile.height-4, 8, 8);
//         }
//     };

//     /** The alpha level used to render a set of pieces. */
//     protected static final Composite SET_ALPHA =
//         AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f);

    protected static final ColorRGBA DARK =
        new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f);
    protected static final ColorRGBA LIGHT =
        new ColorRGBA(0.9f, 0.9f, 0.9f, 1.0f);
}
