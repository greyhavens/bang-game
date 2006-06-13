//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;

import javax.swing.JPanel;

import com.jme.input.KeyInput;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;

import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.MouseEvent;

import com.samskivert.util.RandomUtil;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.game.client.sprite.PropSprite;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.Prop;

import static com.threerings.bang.Log.*;

/**
 * Allows the user to place and manipulate pieces on the board.
 */
public class PiecePlacer extends EditorTool
    implements PieceCodes
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
        _dragStart.setLocation(e.getX(), e.getY());

        // if there's a piece under the mouse, start to drag or delete it
        _dragPiece = _panel.view.getHoverPiece();
        if (_dragPiece != null) {
            if (e.getButton() == MouseEvent.BUTTON2) {
                _ctrl.removePiece(_dragPiece);
                _dragPiece = null;
                
            } else {
                _dragType = NORMAL_DRAG;
                if (_dragPiece instanceof Prop &&
                    (e.getModifiers() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        _dragType = FINE_DRAG;
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        _dragType = ELEVATION_DRAG;
                    }
                }
                if (_dragType == FINE_DRAG) {
                    Prop p = (Prop)_dragPiece;
                    _panel.view.getGroundIntersect(e, false, _loc);
                    _dragOffset.setLocation(
                        (int)(_loc.x / PropSprite.FINE_POSITION_SCALE) -
                            (p.x * 256 + p.fx),
                        (int)(_loc.y / PropSprite.FINE_POSITION_SCALE) -
                            (p.y * 256 + p.fy));
                    
                } else if (_dragType == NORMAL_DRAG) {
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
            _dragPiece.orientation = (short)DIRECTIONS[
                RandomUtil.getInt(DIRECTIONS.length)];
            _dragPiece.position(tx, ty);

            _ctrl.addPiece(_dragPiece);
            
            _panel.view.updateHoverState(e);
        }
    }

    @Override // documentation inherited
    public void mouseReleased (MouseEvent e)
    {
        _dragPiece = null;
        _ctrl.maybeCommitPieceEdit();
    }
    
    @Override // documentation inherited
    public void mouseDragged (MouseEvent e)
    {
        if (_dragPiece == null || _dragType == NORMAL_DRAG) {
            return;
        }
        _ctrl.maybeStartPieceEdit(_dragPiece);
        Prop p = (Prop)_dragPiece.clone();
        if (_dragType == FINE_DRAG) {
            _panel.view.getGroundIntersect(e, false, _loc);
            p.positionFine(
                (int)(_loc.x / PropSprite.FINE_POSITION_SCALE) -
                    _dragOffset.x,
                (int)(_loc.y / PropSprite.FINE_POSITION_SCALE) -
                    _dragOffset.y);
            
        } else { // _dragType == ELEVATION_DRAG
            p.elevate(e.getY() - _dragStart.y);
            _dragStart.setLocation(e.getX(), e.getY());
        }
        getBangObject().updatePieces(_dragPiece = p);
    }
    
    @Override // documentation inherited
    public void mouseWheeled (MouseEvent e)
    {
        // if we're over a piece, rotate it
        Piece piece = _panel.view.getHoverPiece();
        if (piece != null) {
            _ctrl.maybeStartPieceEdit(piece);
            piece = (Piece)piece.clone();
            boolean fine = (e.getModifiers() &
                MouseEvent.SHIFT_DOWN_MASK) != 0 && piece instanceof Prop;
            if (fine) {
                ((Prop)piece).rotateFine(e.getDelta()*8);
            } else {
                piece.rotate(e.getDelta() > 0 ? Piece.CCW : Piece.CW);
            }
            getBangObject().updatePieces(piece);
            if (!fine) { // for fine rotation, commit when shift released
                _ctrl.maybeCommitPieceEdit();
            }
        }
    }

    @Override // documentation inherited
    public void hoverTileChanged (int tx, int ty)
    {
        _hoverTile.setLocation(tx, ty);
        
        // if we are dragging a piece, move that feller around
        if (_dragPiece != null && _dragType == NORMAL_DRAG) {
            _ctrl.maybeStartPieceEdit(_dragPiece);
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
    
    @Override // documentation inherited
    public void keyPressed (KeyEvent e)
    {
        int code = e.getKeyCode(), lr = -1, ud = 0;
        byte ws = 0, ad = 0;
        switch (code) {
             case KeyInput.KEY_LEFT: lr = -1; break;
             case KeyInput.KEY_RIGHT: lr = +1; break;
             case KeyInput.KEY_UP: ud = +1; break;
             case KeyInput.KEY_DOWN: ud = -1; break;
             case KeyInput.KEY_W: ws = +1; break;
             case KeyInput.KEY_A: ad = -1; break;
             case KeyInput.KEY_S: ws = -1; break;
             case KeyInput.KEY_D: ad = +1; break;
             default: return;
        }
        Piece piece = _panel.view.getHoverPiece();
        if (piece == null) {
            return;
        }
        _ctrl.maybeStartPieceEdit(piece);
        piece = (Piece)piece.clone();
        if ((e.getModifiers() & MouseEvent.SHIFT_DOWN_MASK) != 0 &&
            piece instanceof Prop) {
            if (ud != 0) {
                ((Prop)piece).elevate(ud*2);
            } else if (ws != 0) {
                ((Prop)piece).pitch += ws;
            } else if (ad != 0) {
                ((Prop)piece).roll += ad;
            } else {
                ((Prop)piece).rotateFine(lr*8);
            }
            
        } else if (lr != -1) {
            piece.rotate(lr < 0 ? Piece.CW : Piece.CCW);
        } else if (piece instanceof Prop) {
            Prop p = (Prop)piece;
            Vector3f left = _ctx.getCameraHandler().getCamera().getLeft(),
                fwd = _ctx.getCameraHandler().getCamera().getDirection();
            fwd = new Vector3f(fwd.x, fwd.y, 0f);
            if (fwd.length() < FastMath.FLT_EPSILON) {
                fwd = _ctx.getCameraHandler().getCamera().getUp();
                fwd = new Vector3f(fwd.x, fwd.y, 0f);
            }
            fwd.normalizeLocal();
            p.positionFine(
                (int)(p.x*256 + p.fx + 128 + left.x*ad*-8 + fwd.x*ws*8),
                (int)(p.y*256 + p.fy + 128 + left.y*ad*-8 + fwd.y*ws*8)
            );
        }
        getBangObject().updatePieces(piece);
    }
    
    @Override // documentation inherited
    public void keyReleased (KeyEvent e)
    {
        _ctrl.maybeCommitPieceEdit();
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
        
    /** The screen coordinates where our drag started. */
    protected Point _dragStart = new Point();

    /** The piece we're dragging, if we clicked and dragged the mouse. */
    protected Piece _dragPiece;

    /** The offset into the piece from which we're dragging it. */
    protected Point _dragOffset = new Point();

    /** The button that started our drag or -1. */
    protected int _dragButton = -1;
    
    /** The type of drag operation we're performing. */
    protected int _dragType;
    
    /** A temporary vector to reuse. */
    protected Vector3f _loc = new Vector3f();
    
    /** The normal drag type: dragging pieces with tile coordinates. */
    protected static final int NORMAL_DRAG = 0;
    
    /** The fine drag type: dragging pieces with fine coordinates. */
    protected static final int FINE_DRAG = 1;
    
    /** The elevation drag type: dragging pieces vertically. */
    protected static final int ELEVATION_DRAG = 2;
}
