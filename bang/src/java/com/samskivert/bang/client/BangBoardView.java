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

        // if we have a pending path and are dragging with the right mouse
        // button down, pretend like we right clicked in this location
        if (_downButton == MouseEvent.BUTTON3 && _pendingPath != null) {
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

    // documentation inherited
    protected void paintInFront (Graphics2D gfx, Rectangle dirtyRect)
    {
        super.paintInFront(gfx, dirtyRect);

        // render our possible moves
        if (_moveSet.size() > 0) {
            renderSet(gfx, dirtyRect, _moveSet, Color.white);
        }

        // render any currently active path
        if (_pendingPath != null) {
            Rectangle r = new Rectangle(0, 0, SQUARE, SQUARE);
            for (int ii = 0, ll = _pendingPath.getLength(); ii < ll; ii++) {
                int px = _pendingPath.getX(ii), py = _pendingPath.getY(ii);
                r.x = px * SQUARE;
                r.y = py * SQUARE;
                if (dirtyRect.intersects(r)) {
                    gfx.setColor(Color.pink);
                    gfx.fillOval(r.x+2, r.y+2, r.width-4, r.height-4);
                }
            }
        }

        // render the path for the highlighted piece
        if (_highlightPath != null) {
            Rectangle r = new Rectangle(0, 0, SQUARE, SQUARE);
            int ll = _highlightPath.getLength();
            for (int ii = _highlight.pathPos; ii < ll; ii++) {
                int px = _highlightPath.getX(ii), py = _highlightPath.getY(ii);
                r.x = px * SQUARE;
                r.y = py * SQUARE;
                if (dirtyRect.intersects(r)) {
                    gfx.setColor(Color.red);
                    gfx.drawOval(r.x+2, r.y+2, r.width-4, r.height-4);
                }
            }
        }
    }

    @Override // documentation inherited
    protected boolean updateMouseTile (int mx, int my)
    {
        boolean changed = super.updateMouseTile(mx, my);

        // if we changed location...
        if (changed) {
            // clear any previous highlighted path
            if (_highlightPath != null) {
                dirtyPath(_highlightPath);
                _highlightPath = null;
            }

            // determine whether or not there's a piece under the mouse
            // and highlight its path if it has one
            Sprite s = _spritemgr.getHighestHitSprite(
                mx * SQUARE + SQUARE/2, my * SQUARE + SQUARE/2);
            if (s instanceof PieceSprite) {
                PieceSprite sprite = (PieceSprite)s;
                _highlight = (Piece)_bangobj.pieces.get(sprite.getPieceId());
                _highlightPath = _paths.get(_highlight.pieceId);
                if (_highlightPath != null) {
                    dirtyPath(_highlightPath);
                }
            }
        }

        return changed;
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
                // ...note the path in our cache...
                _paths.put(_pendingPath.pieceId, _pendingPath);
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
            // nada

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
            _paths.remove(piece.pieceId);
        }

        // clear the highlight if this piece was under the mouse
        if (_highlight != null && _highlight.pieceId == piece.pieceId) {
            if (_highlightPath != null) {
                dirtyPath(_highlightPath);
            }
            _highlightPath = null;
            _highlight = null;
        }

        // clear and reselect if this piece was the selection
        if (_selection != null && _selection.pieceId == piece.pieceId) {
            clearSelection();
            selectPiece(piece);
        }
    }

    protected Piece _selection, _highlight;
    protected PiecePath _pendingPath, _highlightPath;
    protected PointSet _moveSet = new PointSet();
    protected int _pidx;
    protected int _downButton = -1;

    /** Maps pieceId to path for pieces that have a path configured. */
    protected HashMap<Integer,PiecePath> _paths =
        new HashMap<Integer,PiecePath>();
}
