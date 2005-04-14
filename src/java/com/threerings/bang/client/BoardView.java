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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.swing.Label;
import com.samskivert.swing.util.SwingUtil;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.media.VirtualMediaPanel;
import com.threerings.media.sprite.LabelSprite;

import com.threerings.toybox.data.ToyBoxGameConfig;
import com.threerings.toybox.util.ToyBoxContext;

import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.client.sprite.UnitSprite;
import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.Terrain;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.PointSet;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Provides a shared base for displaying the board that is extended to
 * create the actual game view as well as the level editor.
 */
public class BoardView extends VirtualMediaPanel
    implements KeyListener
{
    public BoardView (ToyBoxContext ctx)
    {
        super(ctx.getFrameManager());
        _ctx = ctx;
    }

    /**
     * Sets up the board view with all the necessary bits. This is called
     * by the controller when we enter an already started game or the game
     * in which we're involved gets started.
     */
    public void startGame (BangObject bangobj, ToyBoxGameConfig cfg, int pidx)
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

        // set our background color according to our affiliation
        if (pidx >= 0) {
            _bgcolor = UnitSprite.DARKER_COLORS[pidx];
        }
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
        dirtyScreenRect(new Rectangle(0, 0, getWidth(), getHeight()));

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
        Label label = new Label(text, Color.white, getFont().deriveFont(40f));
        label.setAlignment(Label.CENTER);
        label.setStyle(Label.OUTLINE);
        label.setAlternateColor(Color.black);
        label.setTargetWidth(300);
        label.layout(this);

        _marquee = new LabelSprite(label);
        _marquee.setRenderOrder(100);
        _marquee.setLocation(
            _vbounds.x+(_vbounds.width-label.getSize().width)/2,
            _vbounds.y+(_vbounds.height-label.getSize().height)/2);
        addSprite(_marquee);
    }

    // documentation inherited from interface KeyListener
    public void keyTyped (KeyEvent e)
    {
        // nothing doing
    }

    // documentation inherited from interface KeyListener
    public void keyPressed (KeyEvent e)
    {
//         if (e.getKeyCode() == KeyEvent.VK_ALT &&
//             _bangobj != null && _bangobj.isInPlay()) {
//             showAttackSet();
//         }
    }

    // documentation inherited from interface KeyListener
    public void keyReleased (KeyEvent e)
    {
//         if (e.getKeyCode() == KeyEvent.VK_ALT) {
//             clearAttackSet();
//         }
    }

    // documentation inherited
    public void addNotify ()
    {
        super.addNotify();
        _ctx.getKeyDispatcher().addGlobalKeyListener(this);
    }

    // documentation inherited
    public void doLayout ()
    {
        super.doLayout();

        if (_board != null) {
            centerBoard();
        }
    }

    // documentation inherited
    public void removeNotify ()
    {
        super.removeNotify();
        _ctx.getKeyDispatcher().removeGlobalKeyListener(this);

        if (_marquee != null) {
            removeSprite(_marquee);
            _marquee = null;
        }
    }

    // documentation inherited
    protected void paintBehind (Graphics2D gfx, Rectangle dirtyRect)
    {
        super.paintBehind(gfx, dirtyRect);

        // wait until we have some sort of board
        if (_board == null) {
            return;
        }

        // start with a black background
        gfx.setColor(_bgcolor);
        gfx.fill(dirtyRect);

        _pr.setLocation(0, 0);
        for (int yy = 0, hh = _board.getHeight(); yy < hh; yy++) {
            _pr.x = 0;
            for (int xx = 0, ww = _board.getWidth(); xx < ww; xx++) {
                if (dirtyRect.intersects(_pr)) {
                    Color color = getColor(_board.getTile(xx, yy));
                    gfx.setColor(color);
                    gfx.fill(_pr);
                    gfx.setColor(Color.black);
                    gfx.draw(_pr);
                    if (xx == _mouse.x && yy == _mouse.y) {
                        paintMouseTile(gfx, xx, yy);
                    }
                }
                _pr.x += SQUARE;
            }
            _pr.y += SQUARE;
        }
    }

    /** Renders a marker on the tile under the mouse. {@link #_pr} will
     * already be configured with that tiles coordinates. */
    protected void paintMouseTile (Graphics2D gfx, int mx, int my)
    {
        gfx.setColor(Color.white);
        gfx.drawOval(_pr.x+2, _pr.y+2, _pr.width-4, _pr.height-4);
    }

    // documentation inherited
    protected void paintInFront (Graphics2D gfx, Rectangle dirtyRect)
    {
        super.paintInFront(gfx, dirtyRect);

        // render any attack set
        if (_attackSet != null) {
            renderSet(gfx, dirtyRect, _attackSet, _crosshairRenderer);
        }
    }

    /** Relocates our virtual view to center the board iff it is smaller
     * than the viewport. */
    protected void centerBoard ()
    {
        int width = _board.getWidth() * SQUARE,
            height = _board.getHeight() * SQUARE;
        int nx = _vbounds.x, ny = _vbounds.y;
        if (width < _vbounds.width) {
            nx = (width - _vbounds.width) / 2;
        }
        if (height < _vbounds.height) {
            ny = (height - _vbounds.height) / 2;
        }
        setViewLocation(nx, ny);
    }

    /** Highlights a set of tiles in the specified color. */
    protected void renderSet (Graphics2D gfx, Rectangle dirtyRect,
                              PointSet set, SetRenderer renderer)
    {
        Composite comp = renderer.getComposite(), ocomp = null;
        if (comp != null) {
            ocomp = gfx.getComposite();
            gfx.setComposite(comp);
        }
        Stroke stroke = renderer.getStroke(), ostroke = null;
        if (stroke != null) {
            ostroke = gfx.getStroke();
            gfx.setStroke(stroke);
        }

        _pr.setLocation(0, 0);
        for (int ii = 0, ll = set.size(); ii < ll; ii++) {
            _pr.x = set.getX(ii) * SQUARE;
            _pr.y = set.getY(ii) * SQUARE;
            if (dirtyRect.intersects(_pr)) {
                renderer.render(gfx, _pr);
            }
        }

        if (stroke != null) {
            gfx.setStroke(ostroke);
        }
        if (comp != null) {
            gfx.setComposite(ocomp);
        }
    }

    /** Invalidates a tile, causing it to be redrawn on the next tick. */
    protected void invalidateTile (int xx, int yy)
    {
        _remgr.invalidateRegion(xx * SQUARE, yy * SQUARE, SQUARE, SQUARE);
    }

    /**
     * Updates the tile over which we know the mouse to be.
     *
     * @return true if the specified new mouse coordinates are different
     * from the previously known coordinates, false if they are the same.
     */
    protected boolean updateMouseTile (int mx, int my)
    {
        if (mx != _mouse.x || my != _mouse.y) {
            invalidateTile(_mouse.x, _mouse.y);
            _mouse.x = mx;
            _mouse.y = my;
            invalidateTile(_mouse.x, _mouse.y);
            return true;
        }
        return false;
    }

    protected Color getColor (Terrain tile)
    {
        Color color = null;
        switch (tile) {
        case DIRT: color = Color.orange.darker(); break;
        case ROAD: color = Color.gray; break;
        case TALL_GRASS: color = Color.green; break;
        case WATER: color = Color.blue; break;
        case LEAF_BRIDGE: color = Color.lightGray; break;
        default: color = Color.black; break;
        }
        return color;
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
            sprite.removed();
        } else {
            log.warning("No sprite for removed piece [id=" + pieceId + "].");
        }
    }

    protected void clearAttackSet ()
    {
        if (_attackSet != null) {
            dirtySet(_attackSet);
            _attackSet = null;
        }
    }

    protected void dirtySet (PointSet set)
    {
        for (int ii = 0, ll = set.size(); ii < ll; ii++) {
            dirtyTile(set.getX(ii), set.getY(ii));
        }
    }

    protected void dirtyTile (int tx, int ty)
    {
        _remgr.invalidateRegion(tx*SQUARE, ty*SQUARE, SQUARE, SQUARE);
    }

    /** Called when a piece is updated in the game object. */
    protected void pieceUpdated (Piece opiece, Piece npiece)
    {
        if (npiece != null) {
            getPieceSprite(npiece).updated(
                _bangobj.board, npiece, _bangobj.tick);
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

    /** Used to render the contents of a point set. */
    protected static interface SetRenderer
    {
        public Composite getComposite ();
        public Stroke getStroke ();
        public void render (Graphics2D gfx, Rectangle tile);
    }

    protected ToyBoxContext _ctx;

    protected BangObject _bangobj;
    protected BangBoard _board;
    protected Rectangle _bbounds;
    protected BoardEventListener _blistener = new BoardEventListener();

    protected PointSet _attackSet;
    protected Color _bgcolor;

    /** The current tile coordinates of the mouse. */
    protected Point _mouse = new Point(-1, -1);

    /** Displayed over top of the board. */
    protected LabelSprite _marquee;

    protected HashMap<Integer,PieceSprite> _pieces =
        new HashMap<Integer,PieceSprite>();

    /** Used during rendering. */
    protected Rectangle _pr = new Rectangle(0, 0, SQUARE, SQUARE);

    /** Renders a cross-hair over the specified coordinate. */
    protected static SetRenderer _crosshairRenderer = new SetRenderer() {
        public Composite getComposite () {
            return null;
        }
        public Stroke getStroke () {
            return null; // _thick;
        }
        public void render (Graphics2D gfx, Rectangle tile) {
            gfx.setColor(Color.white);
            gfx.drawOval(tile.x+2, tile.y+2, tile.width-4, tile.height-4);
            gfx.drawOval(tile.x+10, tile.y+10, tile.width-20, tile.height-20);
            int cx = tile.x+tile.width/2, cy = tile.y+tile.height/2;
            gfx.drawLine(cx, tile.y+5, cx, tile.y+15);
            gfx.drawLine(cx, tile.y+tile.height-5, cx, tile.y+tile.height-15);
            gfx.drawLine(tile.x+5, cy, tile.x+15, cy);
            gfx.drawLine(tile.x+tile.width-5, cy, tile.x+tile.width-15, cy);
        }
        protected Stroke _thick = new BasicStroke(2);
    };

    /** Renders a white rounded rectangle over the specified coordinate. */
    protected static SetRenderer _whiteRenderer = new SetRenderer() {
        public Composite getComposite () {
            return SET_ALPHA;
        }
        public Stroke getStroke () {
            return null;
        }
        public void render (Graphics2D gfx, Rectangle tile) {
            gfx.setColor(Color.white);
            gfx.fillRoundRect(tile.x+2, tile.y+2,
                              tile.width-4, tile.height-4, 8, 8);
        }
    };

    /** The alpha level used to render a set of pieces. */
    protected static final Composite SET_ALPHA =
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f);
}
