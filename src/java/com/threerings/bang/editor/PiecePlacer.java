//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;

import javax.swing.JPanel;

import com.jme.input.KeyInput;
import com.jme.math.Vector3f;

import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.KeyListener;
import com.jmex.bui.event.MouseEvent;

import com.threerings.jme.sprite.Sprite;
import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.sprite.PropSprite;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Prop;

import static com.threerings.bang.Log.*;

/**
 * Allows the user to place and manipulate pieces on the board.
 */
public class PiecePlacer extends EditorTool
    implements KeyListener, PieceCodes
{
    /** The name of this tool. */
    public static final String NAME = "piece_placer";
    
    public PiecePlacer (EditorContext ctx, EditorPanel panel)
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
                _fineDrag = _dragPiece instanceof Prop &&
                    (e.getModifiers() & MouseEvent.SHIFT_DOWN_MASK) != 0;
                if (_fineDrag) {
                    Prop p = (Prop)_dragPiece;
                    _panel.view.getGroundIntersect(e, false, _loc);
                    _dragOffset.setLocation(
                        (int)(_loc.x / PropSprite.FINE_POSITION_SCALE) -
                            (p.x * 256 + p.fx),
                        (int)(_loc.y / PropSprite.FINE_POSITION_SCALE) -
                            (p.y * 256 + p.fy));
                    
                } else {
                    _dragOffset.setLocation(tx-_dragPiece.x, ty-_dragPiece.y);
                }
            }
        
        // otherwise, create a piece and start dragging
        } else if (e.getButton() == MouseEvent.BUTTON1) {
            Piece piece = _chooser.getSelectedPiece();
            if (piece == null) {
                return;
            }
            
            _dragPiece = (Piece)piece.clone();
            _dragPiece.assignPieceId();
            _dragPiece.orientation = (short)DIRECTIONS[
                RandomUtil.getInt(DIRECTIONS.length)];
            _dragPiece.position(tx, ty);
            
            EditorController.postAction(
                _panel, EditorController.ADD_PIECE, _dragPiece);
            
            _panel.view.updateHoverState(e);
        }
    }

    @Override // documentation inherited
    public void mouseReleased (MouseEvent e)
    {
        _dragPiece = null;
    }
    
    @Override // documentation inherited
    public void mouseDragged (MouseEvent e)
    {
        if (_dragPiece != null && _fineDrag) {
            Piece p = (Piece)_dragPiece.clone();
            _panel.view.getGroundIntersect(e, false, _loc);
            ((Prop)p).positionFine(
                (int)(_loc.x / PropSprite.FINE_POSITION_SCALE) -
                    _dragOffset.x,
                (int)(_loc.y / PropSprite.FINE_POSITION_SCALE) -
                    _dragOffset.y);
            getBangObject().updatePieces(p);
        }
    }
    
    @Override // documentation inherited
    public void mouseWheeled (MouseEvent e)
    {
        // if we're over a piece, rotate it
        Piece piece = _panel.view.getHoverPiece();
        if (piece != null) {
            piece = (Piece)piece.clone();
            if ((e.getModifiers() & MouseEvent.SHIFT_DOWN_MASK) != 0 &&
                piece instanceof Prop) {
                ((Prop)piece).rotateFine(e.getDelta()*8);
                
            } else {
                piece.rotate(e.getDelta() > 0 ? Piece.CCW : Piece.CW);
            }
            getBangObject().updatePieces(piece);
        }
    }

    @Override // documentation inherited
    public void hoverTileChanged (int tx, int ty)
    {
        _hoverTile.setLocation(tx, ty);
        
        // if we are dragging a piece, move that feller around
        if (_dragPiece != null && !_fineDrag) {
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
    
    // documentation inherited from interface KeyListener
    public void keyPressed (KeyEvent e)
    {
        int code = e.getKeyCode(), rot;
        switch (code) {
             case KeyInput.KEY_LEFT: rot = Piece.CW; break;
             case KeyInput.KEY_RIGHT: rot = Piece.CCW; break;
             default: return;
        }
        Piece piece = _panel.view.getHoverPiece();
        if (piece == null) {
            return;
        }
        Piece p = (Piece)piece.clone();
        if ((e.getModifiers() & MouseEvent.SHIFT_DOWN_MASK) != 0 &&
            piece instanceof Prop) {
            ((Prop)piece).rotateFine(rot == Piece.CW ? -8 : +8);
                
        } else {
            piece.rotate(rot);
        }
        getBangObject().updatePieces(piece);
    }
    
    // documentation inherited from interface KeyListener
    public void keyReleased (KeyEvent e)
    {
        // nada
    }
    
    // documentation inherited
    protected JPanel createOptions ()
    {
        JPanel options = new JPanel(new BorderLayout());
        options.add(_chooser = new PieceChooser(_ctx), BorderLayout.CENTER);
        return options;
    }

    /** The piece chooser component. */
    protected PieceChooser _chooser;
    
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
    
    /** Whether or not we are performing a fine drag operation. */
    protected boolean _fineDrag;
    
    /** A temporary vector to reuse. */
    protected Vector3f _loc = new Vector3f();
}
