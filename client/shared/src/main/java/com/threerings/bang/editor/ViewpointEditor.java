//
// $Id$

package com.threerings.bang.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.badlogic.gdx.Input.Keys;

import com.jme.math.Vector3f;

import com.jmex.bui.event.KeyEvent;
import com.jmex.bui.event.MouseEvent;

import com.samskivert.swing.VGroupLayout;

import com.threerings.bang.game.client.sprite.ViewpointSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Viewpoint;

/**
 * Allows the user to paint heightfield vertices with different types of
 * terrain.
 */
public class ViewpointEditor extends EditorTool
{
    /** The name of this tool. */
    public static final String NAME = "viewpoint_editor";

    public ViewpointEditor (EditorContext ctx, EditorPanel panel)
    {
        super(ctx, panel);
    }

    // documentation inherited
    public String getName ()
    {
        return NAME;
    }

    @Override // documentation inherited
    public void activate ()
    {
        super.activate();
        _veopts.update();
    }

    @Override // documentation inherited
    public void deactivate ()
    {
        super.deactivate();
        setActiveViewpoint(null);
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent e)
    {
        _lastX = e.getX();
        _lastY = e.getY();
        _lastButton = e.getButton();
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseReleased (MouseEvent e)
    {
        _ctrl.maybeCommitPieceEdit();
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (MouseEvent e)
    {
        if (_vpsprite == null) {
            return;
        }
        int dx = _lastX - e.getX(), dy =  _lastY - e.getY();
        Piece piece = _vpsprite.getPiece();
        _ctrl.maybeStartPieceEdit(piece);
        Viewpoint vp = (Viewpoint)piece.clone();
        switch(_lastButton) {
            case MouseEvent.BUTTON1: // left changes heading and pitch
                vp.rotateFine(dx, -dy);
                break;

            case MouseEvent.BUTTON2: // right pans forward/back/left/right
                moveViewpoint(vp, dy, dx);
                break;
        }
        getBangObject().updatePieces(vp);

        _lastX = e.getX();
        _lastY = e.getY();
    }

    @Override // documentation inherited
    public void mouseWheeled (MouseEvent e)
    {
        if (_vpsprite == null) {
            return;
        }
        Piece piece = _vpsprite.getPiece();
        _ctrl.maybeStartPieceEdit(piece);
        Viewpoint vp = (Viewpoint)piece.clone();
        vp.elevation -= e.getDelta();
        getBangObject().updatePieces(vp);
        _ctrl.maybeCommitPieceEdit();
    }

    // documentation inherited from interface KeyListener
    public void keyPressed (KeyEvent e)
    {
        if (_vpsprite == null) {
            return;
        }
        Piece piece = _vpsprite.getPiece();
        _ctrl.maybeStartPieceEdit(piece);
        Viewpoint vp = (Viewpoint)piece.clone();
        switch (e.getKeyCode()) {
            case Keys.W: moveViewpoint(vp, 5, 0); break;
            case Keys.A: moveViewpoint(vp, 0, -5); break;
            case Keys.S: moveViewpoint(vp, -5, 0); break;
            case Keys.D: moveViewpoint(vp, 0, 5); break;
            case Keys.Q: vp.elevation += 1; break;
            case Keys.E: vp.elevation -= 1; break;
            default: return;
        }
        getBangObject().updatePieces(vp);
    }

    // documentation inherited from interface KeyListener
    public void keyReleased (KeyEvent e)
    {
        _ctrl.maybeCommitPieceEdit();
    }

    /**
     * Moves the given viewpoint piece forward/backward and left/right by the
     * given fine coordinates according to its orientation.
     */
    protected void moveViewpoint (Viewpoint vp, int forward, int right)
    {
        // forward components
        _dir.set(0f, -1f, 0f);
        _vpsprite.getLocalRotation().multLocal(_dir);
        _dir.z = 0f;
        _dir.normalizeLocal().multLocal(forward);
        float dx = _dir.x, dy = _dir.y;

        // plus right components
        _dir.set(-1f, 0f, 0f);
        _vpsprite.getLocalRotation().multLocal(_dir);
        _dir.z = 0f;
        _dir.normalizeLocal().multLocal(right);
        dx += _dir.x;
        dy += _dir.y;

        vp.translateFine((int)dx, (int)dy);
    }

    // documentation inherited
    protected JPanel createOptions ()
    {
        return (_veopts = new ViewpointEditorOptions());
    }

    /**
     * Locks the camera to the specified viewpoint (or reverts to the
     * standard editor camera if <code>null</code> is passed).
     */
    protected void setActiveViewpoint (Viewpoint vp)
    {
        if (_vpsprite != null) {
            _vpsprite.unbindCamera();
            _vpsprite = null;
        }
        _panel.recenter.setEnabled(vp == null);
        if (vp == null) {
            _panel.tools.cameraDolly.resume();
            return;
        }
        _panel.tools.cameraDolly.suspend();
        _vpsprite = (ViewpointSprite)_panel.view.getPieceSprite(vp);
        _vpsprite.bindCamera(_ctx.getCameraHandler().getCamera());
    }

    /** The options for this panel. */
    protected class ViewpointEditorOptions extends JPanel
        implements ActionListener, ListSelectionListener
    {
        public JList<Viewpoint> vlist;
        public DefaultListModel<Viewpoint> lmodel;
        public JPanel props;
        public JTextField name;

        public ViewpointEditorOptions ()
        {
            super(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.STRETCH, 5,
                VGroupLayout.TOP));
            setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

            JPanel lpanel = new JPanel(new VGroupLayout(VGroupLayout.NONE,
                VGroupLayout.STRETCH, 5, VGroupLayout.TOP));
            lpanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            lpanel.add(new JLabel(_ctx.xlate("editor", "m.viewpoints")));

            vlist = new JList<Viewpoint>(lmodel = new DefaultListModel<Viewpoint>());
            vlist.setBorder(BorderFactory.createEtchedBorder(
                EtchedBorder.LOWERED));
            vlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            vlist.addListSelectionListener(this);
            lpanel.add(vlist);
            add(lpanel);

            props = new JPanel(new VGroupLayout(VGroupLayout.NONE,
                VGroupLayout.TOP));
            props.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            props.setVisible(false);

            JPanel npanel = new JPanel();
            npanel.add(new JLabel(_ctx.xlate("editor", "m.name")));
            npanel.add(name = new JTextField(8));
            name.addActionListener(this);
            props.add(npanel);

            add(props);
        }

        public void update ()
        {
            BangObject bangobj =
                (BangObject)_ctx.getLocationDirector().getPlaceObject();

            // refresh the list of viewpoints on the board, preserving the
            // selected one
            Object selected = vlist.getSelectedValue();
            lmodel.clear();
            for (Piece piece : bangobj.pieces) {
                if (piece instanceof Viewpoint) {
                    lmodel.addElement((Viewpoint)piece);
                    if (piece.equals(selected)) {
                        vlist.setSelectedValue(selected, true);
                    }
                }
            }
        }

        public void actionPerformed (ActionEvent ae)
        {
            Viewpoint vp = vlist.getSelectedValue();
            vp.name = name.getText();
            lmodel.set(vlist.getSelectedIndex(), vp);
        }

        public void valueChanged (ListSelectionEvent lse)
        {
            Viewpoint vp = vlist.getSelectedValue();
            boolean enabled = (vp != null);
            props.setVisible(enabled);
            if (enabled) {
                name.setText(vp.name);
            }
            setActiveViewpoint(vp);
        }
    }

    /** The casted options panel. */
    protected ViewpointEditorOptions _veopts;

    /** The sprite for the currently selected viewpoint. */
    protected ViewpointSprite _vpsprite;

    /** The last mouse coordinates and button pressed. */
    protected int _lastX, _lastY, _lastButton;

    /** A temporary vector. */
    protected Vector3f _dir = new Vector3f();
}
