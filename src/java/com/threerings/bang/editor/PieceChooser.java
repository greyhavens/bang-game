//
// $Id$

package com.threerings.bang.editor;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.samskivert.swing.VGroupLayout;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PropConfig;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.util.BasicContext;

/**
 * Allows the user to select a piece to place.
 */
public class PieceChooser extends JPanel
    implements BangCodes
{
    public PieceChooser (BasicContext ctx)
    {
        setLayout(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.STRETCH,
                                   5, VGroupLayout.TOP));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _ctx = ctx;

        add(new JLabel(_ctx.xlate("editor", "m.pieces")));

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        
        addPiece(root, "markers/start", new Marker(Marker.START));
        addPiece(root, "markers/bonus", new Marker(Marker.BONUS));
        addPiece(root, "markers/cattle", new Marker(Marker.CATTLE));
        addPiece(root, "markers/camera", new Marker(Marker.CAMERA));
        
        for (int i = 0; i < TOWN_IDS.length; i++) {
            DefaultMutableTreeNode town = new DefaultMutableTreeNode(
                _ctx.xlate("bang", "m." + TOWN_IDS[i]));
            root.add(town);
            PropConfig[] props = PropConfig.getTownProps(TOWN_IDS[i]);
            for (int j = 0; j < props.length; j++) {
                String type = props[j].type;
                addPiece(town, type, Prop.getProp(type));
            }
        }

        _tree = new JTree(root);
        _tree.setRootVisible(false);
        _tree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.SINGLE_TREE_SELECTION);
        add(_tree);
    }
    
    /**
     * Returns the piece selected by the user, or <code>null</code> for none.
     */
    public Piece getSelectedPiece ()
    {
        TreePath path = _tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode node =
            ((DefaultMutableTreeNode)path.getLastPathComponent());
        return (node.getUserObject() instanceof NamedPiece) ?
            ((NamedPiece)node.getUserObject()).piece : null;
    }
    
    /**
     * Adds a piece to the tree under the specified root.
     *
     * @param type the slash-delimited hierarchical type
     */
    protected void addPiece (DefaultMutableTreeNode root, String type,
        Piece piece)
    {
        String prefix = (root.getUserObject() instanceof PieceCategory) ?
            ((PieceCategory)root.getUserObject()).key + "_" : "";
        
        int idx = type.indexOf('/');
        if (idx == -1) {
            root.add(new DefaultMutableTreeNode(
                new NamedPiece(type, prefix + type, piece), false));
            return;
        }
        
        String cat = type.substring(0, idx);
        DefaultMutableTreeNode child = null;
        for (int i = 0, count = root.getChildCount(); i < count; i++) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode)root.getChildAt(i);
            if (node.getUserObject() instanceof PieceCategory &&
                ((PieceCategory)node.getUserObject()).name.equals(cat)) {
                child = node;
                break;
            }
        }
        if (child == null) {
            child = new DefaultMutableTreeNode(
                new PieceCategory(cat, prefix + cat));
            root.add(child);
        }
        addPiece(child, type.substring(idx + 1), piece);
    }
    
    /** Used to group pieces. */
    protected class PieceCategory
    {
        public String name, key;
        
        public PieceCategory (String name, String key)
        {
            this.name = name;
            this.key = key;
        }
        
        public String toString ()
        {
            return _ctx.xlate("editor", "m.piece_" + key);
        }
    }
    
    /** Combines a piece prototype with its translatable name. */
    protected class NamedPiece extends PieceCategory
    {
        public Piece piece;
        
        public NamedPiece (String name, String key, Piece piece)
        {
            super(name, key);
            this.piece = piece;
        }
    }
    
    protected BasicContext _ctx;
    protected JTree _tree;
}
