//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;

import javax.swing.JPanel;

import com.jmex.bui.event.MouseEvent;

import com.threerings.crowd.util.CrowdContext;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.BasicContext;

/**
 * Allows the user to place and manipulate pieces on the board.
 */
public class PiecePlacer extends EditorTool
{
    /** The name of this tool. */
    public static final String NAME = "piece_placer";
    
    public PiecePlacer (BasicContext ctx, EditorPanel panel)
    {
        super(ctx, panel);
    }
    
    // documentation inherited
    public String getName ()
    {
        return NAME;
    }
    
    @Override // documentation inherited
    public void mousePressed (MouseEvent e)
    {
        int tx = _hoverTile.x, ty = _hoverTile.y;

        // keep track of the press location in case we need it later
        _dragStart.setLocation(tx, ty);

        // if there's a piece under the mouse, start to drag or delete it
        _dragPiece = _panel.view.getHoverPiece();
        if (_dragPiece != null) {
            if (e.getButton() == MouseEvent.BUTTON2) {
                EditorController.postAction(
                    _panel, EditorController.REMOVE_PIECE, _dragPiece.getKey());
                _dragPiece = null;
            } else {
                _dragOffset.setLocation(tx-_dragPiece.x, ty-_dragPiece.y);
            }
        }
    }

    @Override // documentation inherited
    public void mouseReleased (MouseEvent e)
    {
        _dragPiece = null;
    }
    
    @Override // documentation inherited
    public void mouseWheeled (MouseEvent e)
    {
        // if we're over a piece, rotate it
        Piece piece = _panel.view.getHoverPiece();
        if (piece != null) {
            piece = (Piece)piece.clone();
            if (piece.rotate(e.getDelta() > 0 ? Piece.CCW : Piece.CW)) {
                getBangObject().updatePieces(piece);
            }
        }
    }

    @Override // documentation inherited
    public void hoverTileChanged (int tx, int ty)
    {
        _hoverTile.setLocation(tx, ty);
        
        // if we are dragging a piece, move that feller around
        if (_dragPiece != null) {
            Piece p = (Piece)_dragPiece.clone();
            p.position(tx - _dragOffset.x, ty - _dragOffset.y);
            getBangObject().updatePieces(p);
        }
    }

    @Override // documentation inherited
    public void hoverSpriteChanged (Sprite hover)
    {
        // update the cursor when we're hovering over a piece
        ((EditorContext)_ctx).getFrame().setCursor(
            hover == null ? Cursor.getDefaultCursor() :
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    /** Returns a reference to the game object. */
    protected BangObject getBangObject ()
    {
        return (BangObject)((CrowdContext)
            _ctx).getLocationDirector().getPlaceObject();
    }
    
    // documentation inherited
    protected JPanel createOptions ()
    {
        JPanel options = new JPanel(new BorderLayout());
        options.add(new PieceCreator(_ctx), BorderLayout.CENTER);
        return options;
    }

    /** The location of the mouse pointer in tile coordinates. */
    protected Point _hoverTile = new Point(-1, -1);
        
    /** The point at which our last drag took place. */
    protected Point _dragStart = new Point();

    /** The piece we're dragging, if we clicked and dragged the mouse. */
    protected Piece _dragPiece;

    /** The offset into the piece from which we're dragging it. */
    protected Point _dragOffset = new Point();

    /** The button that started our drag or -1. */
    protected int _dragButton = -1;
}
