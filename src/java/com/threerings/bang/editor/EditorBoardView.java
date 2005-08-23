//
// $Id$

package com.threerings.bang.editor;

import java.awt.Cursor;
import java.awt.Point;

import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseListener;
import com.jmex.bui.event.MouseWheelListener;

import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.Terrain;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.editor.EditorContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the board when in editor mode.
 */
public class EditorBoardView extends BoardView
    implements MouseListener, MouseWheelListener
{
    public EditorBoardView (BangContext ctx, EditorPanel panel)
    {
        super(ctx);
        _panel = panel;
        addListener(this);

        // put piece sprites in editor mode
        PieceSprite.setEditorMode(true);
    }

    public void setTile (int tx, int ty, Terrain tile)
    {
        if (_bangobj.board.getBounds().contains(tx, ty) &&
            _bangobj.board.setTile(tx, ty, tile)) {
            // refresh this tile and the eight tiles around it as they all
            // may need refringing
            for (int yy = ty-1; yy <= ty+1; yy++) {
                for (int xx = tx-1; xx <= tx+1; xx++) {
                    refreshTile(xx, yy);
                }
            }
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseClicked (MouseEvent e)
    {
        // nothing doing, we handle this ourselves
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent e)
    {
        int tx = _mouse.x, ty = _mouse.y;

        // keep track of the press location in case we need it later
        _dragStart.setLocation(tx, ty);

        // if there's a piece under the mouse, start to drag or delete it
        _dragPiece = getHoverPiece();
        if (_dragPiece != null) {
            if (e.getButton() == MouseEvent.BUTTON2) {
                EditorController.postAction(
                    _panel, EditorController.REMOVE_PIECE, _dragPiece.getKey());
                _dragPiece = null;
            } else {
                _dragOffset.setLocation(tx-_dragPiece.x, ty-_dragPiece.y);
            }

        } else {
            // otherwise generate a PAINT_TERRAIN or CLEAR_TERRAIN
            handleMousePress(_dragButton = e.getButton(), tx, ty);
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (MouseEvent e)
    {
        _dragPiece = null;
        _dragButton = -1;
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

    // documentation inherited from interface MouseWheelListener
    public void mouseWheeled (MouseEvent e)
    {
        // if we're over a piece, rotate it
        Piece piece = getHoverPiece();
        if (piece != null) {
            piece = (Piece)piece.clone();
            if (piece.rotate(e.getDelta() > 0 ? Piece.CCW : Piece.CW)) {
                _bangobj.updatePieces(piece);
            }

        } else {
            // otherwise adjust the currently selected terrain type
            _panel.terrain.rollSelection(e.getDelta());
        }
    }

    @Override // documentation inherited
    protected void hoverTileChanged (int tx, int ty)
    {
        // if we are dragging a piece, move that feller around
        if (_dragPiece != null) {
            Piece p = (Piece)_dragPiece.clone();
            p.position(tx - _dragOffset.x, ty - _dragOffset.y);
            _bangobj.updatePieces(p);
        }

        // if we have a drag command and the mouse coordinates
        // changed, "paint" on the board
        if (_dragButton != -1) {
            handleMousePress(_dragButton, tx, ty);
        }
    }

    @Override // documentation inherited
    protected void hoverSpriteChanged (Sprite hover)
    {
        super.hoverSpriteChanged(hover);

        // update the cursor when we're hovering over a piece
        ((EditorContext)_ctx).getFrame().setCursor(
            hover == null ? Cursor.getDefaultCursor() :
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    protected void handleMousePress (int button, int tx, int ty)
    {
        if (button == MouseEvent.BUTTON2) {
            setTile(tx, ty, Terrain.NONE);
        } else {
            setTile(tx, ty, _panel.terrain.getSelectedTerrain());
        }
    }

    /** Returns the piece associated with the sprite under the mouse, if
     * there is one and if it is a piece sprite. Returns null otherwise. */
    protected Piece getHoverPiece ()
    {
        int pid = (_hover instanceof PieceSprite) ?
            ((PieceSprite)_hover).getPieceId() : -1;
        return (Piece)_bangobj.pieces.get(pid);
    }

    /** The panel that contains additional interface elements with which
     * we interact. */
    protected EditorPanel _panel;

    /** The point at which our last drag took place. */
    protected Point _dragStart = new Point();

    /** The piece we're dragging, if we clicked and dragged the mouse. */
    protected Piece _dragPiece;

    /** The offset into the piece from which we're dragging it. */
    protected Point _dragOffset = new Point();

    /** The button that started our drag or -1. */
    protected int _dragButton = -1;
}
