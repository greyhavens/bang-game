//
// $Id$

package com.samskivert.bang.client;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.threerings.media.sprite.Sprite;

import com.threerings.toybox.util.ToyBoxContext;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.PiecePath;
import com.samskivert.bang.data.piece.Piece;
import com.samskivert.bang.util.PointSet;

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

        // if we have a selection and are dragging with the right mouse
        // button down, pretend like we right clicked in this location
        if (_downButton == MouseEvent.BUTTON3 && _selection != null) {
            handleRightPress(e.getX(), e.getY());
        }
    }

    @Override // documentation inherited
    public void startGame (BangObject bangobj, int playerIdx)
    {
        super.startGame(bangobj, playerIdx);
        _pidx = playerIdx;
    }

    @Override // documentation inherited
    public void endGame ()
    {
        super.endGame();
        _moveSet.clear();
    }

    @Override // documentation inherited
    protected void paintBehind (Graphics2D gfx, Rectangle dirtyRect)
    {
        super.paintBehind(gfx, dirtyRect);

        // render all pending paths
        for (PiecePath path : _paths.values()) {
            Piece piece = (Piece)_bangobj.pieces.get(path.pieceId);
            // the piece might not yet know it has a path
            int pos = (piece.pathPos < 0) ? 0 : piece.pathPos;
            renderPath(gfx, dirtyRect, Color.gray, pos, path);
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
            renderPath(gfx, dirtyRect, Color.pink, 0, _pendingPath);
        }
    }

    protected void renderPath (Graphics2D gfx, Rectangle dirtyRect,
                               Color color, int pos, PiecePath path)
    {
        gfx.setColor(color);
        for (int ii = pos, ll = path.getLength(); ii < ll; ii++) {
            int px = path.getX(ii), py = path.getY(ii);
            _pr.x = px * SQUARE;
            _pr.y = py * SQUARE;
            if (dirtyRect.intersects(_pr)) {
                gfx.drawOval(_pr.x+2, _pr.y+2, _pr.width-4, _pr.height-4);
            }
        }
    }

    /** Handles a left mouse button click. */
    protected void handleLeftPress (int mx, int my)
    {
        int tx = mx / SQUARE, ty = my / SQUARE;

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

        // make sure this is a legal move
        if (!_moveSet.contains(tx, ty)) {
            if (_selection == null) {
                // potentially treat this like a left click so that we can
                // start a path by right clicking on a piece and dragging;
                // but only if we have no selection as otherwise we'll
                // auto-cancel the selection we started with the first
                // right click and drag
                handleLeftPress(mx, my);
            }

        } else if (_pendingPath != null) {
            // potentiall extend our existing path
            if (!_pendingPath.isTail(tx, ty)) {
                _pendingPath = _pendingPath.append(tx, ty);
                dirtyTile(tx, ty);
                updatePossibleMoves(_selection, tx, ty);
            }

        } else if (_selection != null) {
            // start a new path
            _pendingPath = new PiecePath(_selection.pieceId, tx, ty);
            dirtyPath(_pendingPath);
            updatePossibleMoves(_selection, tx, ty);
        }
    }

    protected void selectPiece (Piece piece)
    {
        boolean deselect = (piece == _selection);
        clearSelection();
        if (!deselect) {
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
    protected void pieceUpdated (Piece piece)
    {
        super.pieceUpdated(piece);

        // clear our cached path for this piece if it no longer has a path
        if (!piece.hasPath()) {
            dirtyPath(_paths.remove(piece.pieceId));
        }

        // clear and reselect if this piece was the selection and it moved
        if (_selection != null && _selection.pieceId == piece.pieceId &&
            (_selection.x != piece.x || _selection.y != piece.y)) {
            clearSelection();
            selectPiece(piece);
        }
    }

    protected Piece _selection;
    protected PiecePath _pendingPath;
    protected PointSet _moveSet = new PointSet();
    protected int _pidx;
    protected int _downButton = -1;

    /** Used by {@link #renderPath}. */
    protected Rectangle _pr = new Rectangle(0, 0, SQUARE, SQUARE);

    /** Maps pieceId to path for pieces that have a path configured. */
    protected HashMap<Integer,PiecePath> _paths =
        new HashMap<Integer,PiecePath>();
}
