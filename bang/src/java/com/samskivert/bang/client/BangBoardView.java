//
// $Id$

package com.samskivert.bang.client;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.threerings.media.sprite.PathObserver;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.util.AStarPathUtil;
import com.threerings.media.util.LinePath;
import com.threerings.media.util.MathUtil;
import com.threerings.media.util.Path;

import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;

import com.threerings.toybox.util.ToyBoxContext;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.client.sprite.ShotSprite;
import com.samskivert.util.StringUtil;
import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.PiecePath;
import com.samskivert.bang.data.Shot;
import com.samskivert.bang.data.piece.BigPiece;
import com.samskivert.bang.data.piece.Chopper;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.util.PointSet;
import com.samskivert.bang.util.VisibilityState;

import static com.samskivert.bang.Log.log;
import static com.samskivert.bang.client.BangMetrics.*;

/**
 * Displays the main game board.
 */
public class BangBoardView extends BoardView
    implements MouseListener, MouseMotionListener
{
    public BangBoardView (ToyBoxContext ctx)
    {
        super(ctx);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    // documentation inherited from interface MouseListener
    public void mouseClicked (MouseEvent e)
    {
        // nothing doing, we handle this ourselves
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent e)
    {
        switch (_downButton = e.getButton()) {
        case MouseEvent.BUTTON3:
            // button 3 (right button) creates or extends a path
            handleRightPress(e.getX(), e.getY());
            break;

        case MouseEvent.BUTTON1:
            handleLeftPress(e.getX(), e.getY());
            break;
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (MouseEvent e)
    {
        _downButton = -1;

        if (_attackSet != null) {
            clearAttackSet();
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseEntered (MouseEvent e)
    {
        // nada
    }

    // documentation inherited from interface MouseListener
    public void mouseExited (MouseEvent e)
    {
        // nada
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseMoved (MouseEvent e)
    {
        int mx = e.getX() / SQUARE, my = e.getY() / SQUARE;
        updateMouseTile(mx, my);
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (MouseEvent e)
    {
        // first update our mousely business
        mouseMoved(e);

//         // if we have a selection and are dragging with the right mouse
//         // button down, pretend like we right clicked in this location
//         if (_downButton == MouseEvent.BUTTON3 && _selection != null) {
//             handleRightPress(e.getX(), e.getY());
//         }
    }

    @Override // documentation inherited
    public void startGame (BangObject bangobj, int playerIdx)
    {
        super.startGame(bangobj, playerIdx);

        _pidx = playerIdx;
        _bangobj.addListener(_ticklist);

        _vstate = new VisibilityState(_bbounds.width, _bbounds.height);
        _tstate = new byte[_bbounds.width*_bbounds.height];

        // set up the starting visibility
        adjustBoardVisibility();
        adjustEnemyVisibility();
    }

    @Override // documentation inherited
    public void endGame ()
    {
        if (_bangobj != null) {
            _bangobj.removeListener(_ticklist);
        }

        super.endGame();
        clearSelection();

        // allow everything to be visible
        _vstate.reveal();
        adjustEnemyVisibility();
        dirtyScreenRect(new Rectangle(0, 0, getWidth(), getHeight()));
    }

    @Override // documentation inherited
    protected void paintBehind (Graphics2D gfx, Rectangle dirtyRect)
    {
        super.paintBehind(gfx, dirtyRect);

        // render all pending paths
        for (PiecePath path : _paths.values()) {
            Piece piece = (Piece)_bangobj.pieces.get(path.pieceId);
            renderPath(gfx, dirtyRect, Color.gray, piece, path);
        }
    }

    @Override // documentation inherited
    protected void paintMouseTile (Graphics2D gfx, int mx, int my)
    {
        // only highlight the mouse coordinates while we're in play
        if (_bangobj != null && _bangobj.isInPlay()) {
            super.paintMouseTile(gfx, mx, my);
        }
    }

    @Override // documentation inherited
    protected void paintInFront (Graphics2D gfx, Rectangle dirtyRect)
    {
        super.paintInFront(gfx, dirtyRect);

        // render our possible moves
        if (_moveSet.size() > 0) {
            renderSet(gfx, dirtyRect, _moveSet, Color.white);
        }

        // render any currently active path
        if (_pendingPath != null) {
            renderPath(gfx, dirtyRect, Color.pink, _selection, _pendingPath);
        }

        // render the necessary tiles as dimmed if it is not "visible"
        if (_board != null) {
            Composite ocomp = gfx.getComposite();
            gfx.setComposite(SET_ALPHA);
            gfx.setColor(Color.black);
            _pr.setLocation(0, 0);
            for (int yy = 0, hh = _board.getHeight(); yy < hh; yy++) {
                _pr.x = 0;
                int xoff = yy * _board.getWidth();
                for (int xx = 0, ww = _board.getWidth(); xx < ww; xx++) {
                    if (!_vstate.getVisible(xoff+xx) &&
                        dirtyRect.intersects(_pr)) {
                        gfx.fill(_pr);
                    }
                    _pr.x += SQUARE;
                }
                _pr.y += SQUARE;
            }
            gfx.setComposite(ocomp);
        }
    }

    @Override // documentation inherited
    protected boolean updateMouseTile (int mx, int my)
    {
        if (super.updateMouseTile(mx, my)) {
            // if we have a selected piece, path-find a new path to this
            // location and display it as the pending path
            if (_selection != null) {
                if (_pendingPath != null) {
                    dirtyPath(_pendingPath);
                    _pendingPath = null;
                }
                List path = AStarPathUtil.getPath(
                    _tpred, _selection.getStepper(), _selection,
                    _bbounds.width+_bbounds.height, _selection.x, _selection.y,
                    mx, my, true);
                if (path.size() > 1) {
                    // the first coordinate is the piece's current coordinate
                    path.remove(0);
                    _pendingPath = new PiecePath(_selection.pieceId, path);
                    dirtyPath(_pendingPath);
                }
            }
            return true;
        }
        return false;
    }

    protected void renderPath (Graphics2D gfx, Rectangle dirtyRect,
                               Color color, Piece piece, PiecePath path)
    {
        // the piece might not yet know it has a path
        int pos = (piece.pathPos < 0) ? 0 : piece.pathPos;
        gfx.setColor(color);

        int sx = piece.x * SQUARE + SQUARE/2,
            sy = piece.y * SQUARE + SQUARE/2;
        for (int ii = pos, ll = path.getLength(); ii < ll; ii++) {
            int px = path.getX(ii) * SQUARE + SQUARE/2,
                py = path.getY(ii) * SQUARE + SQUARE/2;
            if (dirtyRect.contains(sx, sy) || dirtyRect.contains(px, py)) {
                gfx.drawLine(sx, sy, px, py);
            }
            sx = px;
            sy = py;
        }
    }

    /** Handles a left mouse button click. */
    protected void handleLeftPress (int mx, int my)
    {
        int tx = mx / SQUARE, ty = my / SQUARE;

        // nothing doing if the game is not in play
        if (_bangobj == null || !_bangobj.isInPlay()) {
            return;
        }

        // check for a selectable piece under the mouse
        PieceSprite sprite = null;
        Sprite s = _spritemgr.getHighestHitSprite(mx, my);
        if (s instanceof PieceSprite) {
            sprite = (PieceSprite)s;
            Piece piece = (Piece)_bangobj.pieces.get(sprite.getPieceId());
            if (_pidx == -1 || piece.owner != _pidx ||
                !sprite.isSelectable()) {
                sprite = null;
            }
        }

        if (_pendingPath != null) {
            // if their final click is a legal move...
            boolean tail = _pendingPath.isTail(tx, ty);
            if (_moveSet.contains(tx, ty) || tail) {
                // ...add the final node to the path...
                if (!tail) {
                    _pendingPath = _pendingPath.append(tx, ty);
                }
                // ...erase any old path...
                dirtyPath(_paths.remove(_pendingPath.pieceId));
                // ...note the new path in our cache...
                _paths.put(_pendingPath.pieceId, _pendingPath);
                // ...override our local piece which may think it's
                // part-way down some path but is now starting a new one...
                Piece piece = (Piece)_bangobj.pieces.get(_pendingPath.pieceId);
                // the next server update will formalize this change
                piece.pathPos = 0;
                // ...and ship it off for processing
                BangController.postAction(
                    this, BangController.SET_PATH, _pendingPath);
                clearSelection();

            } else if (sprite != null &&
                       sprite.getPieceId() == _selection.pieceId) {
                // if they clicked in an illegal position, allow a
                // click on the original selected piece to reset the
                // path, other clicks we will ignore
                selectPiece(_selection);
            }

        } else if (sprite != null) {
            Piece piece = (Piece)_bangobj.pieces.get(sprite.getPieceId());
            if (piece != null) {
                selectPiece(piece);
            } else {
                log.warning("PieceSprite with no piece!? " +
                            "[sprite=" + sprite +
                            ", pieceId=" + sprite.getPieceId() + "].");
            }

        } else if (_selection != null) {
            if (_moveSet.contains(tx, ty)) {
                // create a one move path and send that off
                BangController.postAction(
                    this, BangController.SET_PATH,
                    new PiecePath(_selection.pieceId, tx, ty));
                // and clear the selection to debounce double clicking, etc.
                clearSelection();
            }
        }
    }

    /** Handles a right mouse button click. */
    protected void handleRightPress (int mx, int my)
    {
        int tx = mx / SQUARE, ty = my / SQUARE;

        // nothing doing if the game is not in play
        if (_bangobj == null || !_bangobj.isInPlay()) {
            return;
        }

        // if there is a piece under the cursor, show their possible shots
        PieceSprite sprite = null;
        Sprite s = _spritemgr.getHighestHitSprite(mx, my);
        if (s instanceof PieceSprite) {
            sprite = (PieceSprite)s;
            Piece piece = (Piece)_bangobj.pieces.get(sprite.getPieceId());
            if (sprite.isSelectable() && piece.isAlive()) {
                _attackSet = new PointSet();
                piece.enumerateAttacks(_attackSet);
                piece.enumerateAttention(_attentionSet);
                _remgr.invalidateRegion(_vbounds);
            }
        }
        
//         // make sure this is a legal move
//         if (!_moveSet.contains(tx, ty)) {
//             if (_selection == null) {
//                 // potentially treat this like a left click so that we can
//                 // start a path by right clicking on a piece and dragging;
//                 // but only if we have no selection as otherwise we'll
//                 // auto-cancel the selection we started with the first
//                 // right click and drag
//                 handleLeftPress(mx, my);
//             }

//         } else if (_pendingPath != null) {
//             // potentiall extend our existing path
//             if (!_pendingPath.isTail(tx, ty)) {
//                 _pendingPath = _pendingPath.append(tx, ty);
//                 dirtyTile(tx, ty);
//                 updatePossibleMoves(_selection, tx, ty);
//             }

//         } else if (_selection != null) {
//             // start a new path
//             _pendingPath = new PiecePath(_selection.pieceId, tx, ty);
//             dirtyPath(_pendingPath);
//             updatePossibleMoves(_selection, tx, ty);
//         }
    }

    protected void selectPiece (Piece piece)
    {
        boolean deselect = (piece == _selection);
        clearSelection();
        if (!deselect && piece.isAlive()) {
            _selection = piece;
            getPieceSprite(_selection).setSelected(true);
            updatePossibleMoves(_selection, _selection.x, _selection.y);
        }
    }

    protected void clearSelection ()
    {
        if (_pendingPath != null) {
            dirtyPath(_pendingPath);
            _pendingPath = null;
        }
        if (_selection != null) {
            getPieceSprite(_selection).setSelected(false);
            _selection = null;
            dirtySet(_moveSet);
            _moveSet.clear();
        }
    }

    protected void updatePossibleMoves (Piece piece, int x, int y)
    {
        dirtySet(_moveSet);
        _moveSet.clear();
        piece.enumerateLegalMoves(x, y, _moveSet);
        dirtySet(_moveSet);
    }

    @Override // documentation inherited
    protected void pieceUpdated (Piece opiece, Piece npiece)
    {
        super.pieceUpdated(opiece, npiece);

        // clear our cached path for this piece if it no longer has a path
        if (!npiece.hasPath()) {
            dirtyPath(_paths.remove(npiece.pieceId));
        }

        // clear and reselect if this piece was the selection and it moved
        if (_selection != null && _selection.pieceId == npiece.pieceId &&
            (_selection.x != npiece.x || _selection.y != npiece.y)) {
            clearSelection();
            selectPiece(npiece);
        }
    }

    /**
     * Called after all updates associated with a tick have come in.
     */
    protected void tickFinished (Object[] args)
    {
        // adjust the board visibility
        adjustBoardVisibility();

        // finally adjust the visibility of enemy pieces
        adjustEnemyVisibility();

        // recompute the board traversability
        Arrays.fill(_tstate, (byte)0);
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece instanceof BigPiece) {
                Rectangle pbounds = ((BigPiece)piece).getBounds();
                for (int yy = pbounds.y, ly = yy + pbounds.height;
                     yy < ly; yy++) {
                    for (int xx = pbounds.x, lx = xx + pbounds.width;
                         xx < lx; xx++) {
                        _tstate[_bbounds.width*yy+xx] = 1;
                    }
                }
            } else {
                _tstate[_bbounds.width*piece.y+piece.x] = 2;
            }
        }

        // create shot handlers for all fired shots
        for (int ii = 0; ii < args.length; ii++) {
            new ShotHandler((Shot)args[ii]);
        }
    }

    /** Adjusts the visibility settings for the tiles of the board. */
    protected void adjustBoardVisibility ()
    {
        // if we're out of the game, just reveal everything
        if (!_bangobj.hasLivePieces(_pidx)) {
            _vstate.reveal();
            return;
        }

        // swap our visibility state to the fresh set
        _vstate.swap();

        // update the board visibility based on our piece's new position
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece.owner == -1 || (_pidx != -1 && _pidx != piece.owner)) {
                continue; // skip non-player pieces in this pass
            }

            int dist = piece.getSightDistance(), dist2 = dist * dist;
            Rectangle rect = new Rectangle(
                piece.x - dist, piece.y - dist, 2*dist+1, 2*dist+1);
            rect = rect.intersection(
                new Rectangle(0, 0, _board.getWidth(), _board.getHeight()));
            for (int yy = rect.y, ly = yy + rect.height; yy < ly; yy++) {
                for (int xx = rect.x, lx = xx + rect.width; xx < lx; xx++) {
                    int tdist = MathUtil.distanceSq(xx, yy, piece.x, piece.y);
                    if (tdist < dist2) {
                        _vstate.setVisible(xx, yy);
                    }
                }
            }
        }

        // now dirty any tiles whose visibility changed
        for (int yy = 0, ly = _board.getHeight(); yy < ly; yy++) {
            for (int xx = 0, lx = _board.getHeight(); xx < lx; xx++) {
                if (_vstate.visibilityChanged(xx, yy)) {
                    dirtyTile(xx, yy);
                }
            }
        }
    }

    /** Makes enemy pieces visible or invisible based on _vstate. */
    protected void adjustEnemyVisibility ()
    {
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece.owner == -1 || (_pidx == -1 || _pidx == piece.owner)) {
                continue; // skip unowned and player pieces in this pass
            }

            PieceSprite sprite = _pieces.get(piece.pieceId);
            if (sprite != null) {
                boolean viz = _vstate.getVisible(piece.x, piece.y);
                if (viz && !isManaged(sprite)) {
                    sprite.updated(piece);
                    addSprite(sprite);
                } else if (!viz && isManaged(sprite)) {
                    removeSprite(sprite);
                }
            }
        }
    }

    /** Waits for all sprites involved in a shot to stop moving and then
     * animates the fired shot. */
    protected class ShotHandler
        implements PathObserver
    {
        public ShotHandler (Shot shot) {
            _shot = shot;
            _shooter = (Piece)_bangobj.pieces.get(shot.shooterId);
            if (_shooter == null) {
                log.warning("Missing shooter? [shot=" + shot + "].");
                // abandon ship, we're screwed
                return;
            }

            // figure out which sprites we need to wait for
            considerPiece(_shooter);
            for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
                Piece p = (Piece)iter.next();
                if (p == _shooter || !_shot.affects(p.x, p.y)) {
                    continue;
                }
                considerPiece(p);
            }

            // if no one was managed, it's a shot fired from an invisible
            // piece at invisible pieces, ignore it
            if (_managed == 0) {
                log.info("Tree feel in the woods, no one was around.");

            } else if (_sprites == 0) {
                // if we're not waiting for any sprites to finish moving,
                // fire the shot immediately
                fireShot();
            }
        }

        public void pathCompleted (Sprite sprite, Path path, long when) {
            sprite.removeSpriteObserver(this);
            if (--_sprites == 0) {
                fireShot();
            }
        }

        public void pathCancelled (Sprite sprite, Path path) {
            sprite.removeSpriteObserver(this);
            if (--_sprites == 0) {
                fireShot();
            }
        }

        protected void considerPiece (Piece piece) {
            PieceSprite sprite = null;
            if (piece != null) {
                sprite = _pieces.get(piece.pieceId);
            }
            if (sprite == null) {
                return;
            }
            if (isManaged(sprite)) {
                _managed++;
                if (sprite.isMoving()) {
                    sprite.addSpriteObserver(this);
                    _sprites++;
                }
            }
        }

        protected void fireShot ()
        {
            ShotSprite shot = new ShotSprite();
            shot.addSpriteObserver(_remover);
            int sx = _shooter.x * SQUARE + SQUARE/2;
            int sy = _shooter.y * SQUARE + SQUARE/2;
            int tx = _shot.x * SQUARE + SQUARE/2;
            int ty = _shot.y * SQUARE + SQUARE/2;
            int duration = (int)MathUtil.distance(sx, sy, tx, ty) * 2;
            shot.setLocation(sx, sy);
            addSprite(shot);
            shot.move(new LinePath(sx, sy, tx, ty, duration));
        }

        protected Shot _shot;
        protected Piece _shooter;
        protected int _sprites, _managed;
    }

    /** Used to remove shot sprites when they reach their target. */
    protected PathObserver _remover = new PathObserver() {
        public void pathCompleted (Sprite sprite, Path path, long when) {
            removeSprite(sprite);
        }
        public void pathCancelled (Sprite sprite, Path path) {
            removeSprite(sprite);
        }
    };

    /** Listens for the "end of tick" indicator. */
    protected MessageListener _ticklist = new MessageListener() {
        public void messageReceived (MessageEvent event) {
            if (event.getName().equals("ticked")) {
                tickFinished(event.getArgs());
            }
        }
    };

    /** Used when path finding. */
    protected AStarPathUtil.TraversalPred _tpred =
        new AStarPathUtil.TraversalPred() {
        public boolean canTraverse (Object traverser, int x, int y) {
            if (!_bbounds.contains(x, y)) {
                return false;
            }
            int max = 0;
            if (traverser instanceof Chopper) {
                max = 1;
            }
            return (_tstate[y*_bbounds.width+x] <= max);
        }
    };

    protected Piece _selection;
    protected PiecePath _pendingPath;
    protected PointSet _moveSet = new PointSet();
    protected int _pidx;
    protected int _downButton = -1;

    /** Tracks coordinate visibility. */
    protected VisibilityState _vstate;

    /** Tracks coordinate traversability. */
    protected byte[] _tstate;

    /** Maps pieceId to path for pieces that have a path configured. */
    protected HashMap<Integer,PiecePath> _paths =
        new HashMap<Integer,PiecePath>();
}
