//
// $Id$

package com.threerings.bang.editor;

import java.awt.Color;

import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.QuickSort;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PropConfig;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.data.piece.Viewpoint;
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
        
        addPiece(root, "viewpoint", new Viewpoint());
        addPiece(root, "markers/start", new Marker(Marker.START));
        addPiece(root, "markers/bonus", new Marker(Marker.BONUS));
        addPiece(root, "markers/cattle", new Marker(Marker.CATTLE));
        addPiece(root, "markers/lode", new Marker(Marker.LODE));
        addPiece(root, "markers/totem", new Marker(Marker.TOTEM));
        addPiece(root, "markers/safe", new Marker(Marker.SAFE));
        
        ArrayList<PropConfig> configs =
            new ArrayList<PropConfig>(PropConfig.getConfigs());
        QuickSort.sort(configs, new Comparator<PropConfig>() {
            public int compare (PropConfig prop1, PropConfig prop2) {
                return prop1.type.compareTo(prop2.type);
            }
        });
        for (PropConfig config : configs) {
            addPiece(root, config.type, Prop.getProp(config.type));
        }

        _tree = new JTree(root);
        _tree.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
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
        String prefix = "";
        if (root.getUserObject() instanceof PieceCategory &&
            !((PieceCategory)root.getUserObject()).townCategory) {
            prefix = ((PieceCategory)root.getUserObject()).key + "_";
        }
        
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
        public boolean townCategory;

        public PieceCategory (String name, String key)
        {
            this.name = name;
            this.key = key;

            for (String townId : BangCodes.TOWN_IDS) {
                if (townId.equals(key)) {
                    townCategory = true;
                    break;
                }
            }
        }
        
        public String toString ()
        {
            String msg = "m.piece_" + key;
            return _ctx.getMessageManager().getBundle("editor").exists(msg) ?
                _ctx.xlate("editor", msg) : name;
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
