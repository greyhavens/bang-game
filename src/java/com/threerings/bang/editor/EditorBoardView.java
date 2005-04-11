//
// $Id$

package com.threerings.bang.editor;

import java.awt.Point;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import com.threerings.media.sprite.Sprite;
import com.threerings.toybox.util.ToyBoxContext;

import com.threerings.bang.client.BoardView;
import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.data.Terrain;
import com.threerings.bang.data.piece.Piece;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the board when in editor mode.
 */
public class EditorBoardView extends BoardView
    implements MouseListener, MouseMotionListener, MouseWheelListener
{
    public EditorBoardView (ToyBoxContext ctx)
    {
        super(ctx);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    public void setTile (int tx, int ty, Terrain tile)
    {
        _bangobj.board.setTile(tx, ty, tile);
        invalidateTile(tx, ty);
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

        // keep track of the press location in case we need it later
        _dragStart.setLocation(tx, ty);

        // if there's a piece under the mouse, generate a ROTATE_PIECE
        _dragPiece = getHitPiece(e.getX(), e.getY());
        if (_dragPiece != null) {
            _dragOffset.setLocation(tx-_dragPiece.x, ty-_dragPiece.y);
            return;
        }

        // otherwise generate a PAINT_TERRAIN or CLEAR_TERRAIN
        _dragCommand = (e.getButton() == MouseEvent.BUTTON3) ?
            EditorController.CLEAR_TERRAIN : EditorController.PAINT_TERRAIN;
        EditorController.postAction(this, _dragCommand, new Point(tx, ty));
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (MouseEvent e)
    {
        _dragPiece = null;
        _dragCommand = null;
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
        int mx = e.getX() / SQUARE, my = e.getY() / SQUARE;
        if (updateMouseTile(mx, my)) {
            // if we are dragging a piece, move that feller around
            if (_dragPiece != null) {
                _dragPiece.position(mx - _dragOffset.x, my - _dragOffset.y);
                _bangobj.updatePieces(_dragPiece);
            }

            // if we have a drag command and the mouse coordinates
            // changed, fire off another instance of the same command
            if (_dragCommand != null) {
                EditorController.postAction(
                    this, _dragCommand, new Point(mx, my));
            }
        }
    }

    // documentation inherited from interface MouseWheelListener
    public void mouseWheelMoved (MouseWheelEvent event)
    {
        // if we're over a piece, rotate it
        Piece piece = getHitPiece(event.getX(), event.getY());
        if (piece != null) {
            String cmd = (event.getWheelRotation() > 0) ?
                EditorController.ROTATE_PIECE_CW :
                EditorController.ROTATE_PIECE_CCW;
            EditorController.postAction(this, cmd, piece);

        } else {
            // otherwise adjust the currently selected terrain type
            EditorController.postAction(
                this, EditorController.ROLL_TERRAIN_SELECTION,
                event.getWheelRotation());
        }
    }

    /** Returns the piece associated with the sprite at the specified
     * screen coordinates or null if no piece sprite contains that
     * pixel. */
    protected Piece getHitPiece (int mx, int my)
    {
        Sprite s = _spritemgr.getHighestHitSprite(mx, my);
        if (s != null) {
            if (s instanceof PieceSprite) {
                int pieceId = ((PieceSprite)s).getPieceId();
                return (Piece)_bangobj.pieces.get(pieceId);
            }
        }
        return null;
    }

    /** The point at which our last drag took place. */
    protected Point _dragStart = new Point();

    /** The piece we're dragging, if we clicked and dragged the mouse. */
    protected Piece _dragPiece;

    /** The offset into the piece from which we're dragging it. */
    protected Point _dragOffset = new Point();

    /** The command we generate if we're dragging the mouse. */
    protected String _dragCommand;
}
