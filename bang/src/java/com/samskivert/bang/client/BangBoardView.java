//
// $Id$

package com.samskivert.bang.client;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.threerings.media.sprite.Sprite;

import com.threerings.toybox.util.ToyBoxContext;

import com.samskivert.bang.client.sprite.PieceSprite;
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
        int tx = e.getX() / SQUARE, ty = e.getY() / SQUARE;

        // button 3 (right button) creates or extends a path
        if (e.getButton() == MouseEvent.BUTTON3) {
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

        } else if (e.getButton() == MouseEvent.BUTTON1) {
            // check for a selectable piece under the mouse
            PieceSprite sprite = null;
            Sprite s = _spritemgr.getHighestHitSprite(e.getX(), e.getY());
            if (s instanceof PieceSprite) {
                sprite = (PieceSprite)s;
                if (!sprite.isSelectable()) {
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
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (MouseEvent e)
    {
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
        mouseMoved(e);
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
    }

    protected void selectPiece (Piece piece)
    {
        clearSelection();
        _selection = piece;
        getPieceSprite(_selection).setSelected(true);
        updatePossibleMoves(_selection, _selection.x[0], _selection.y[0]);
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

    protected Piece _selection;
    protected PiecePath _pendingPath;
    protected PointSet _moveSet = new PointSet();
}
