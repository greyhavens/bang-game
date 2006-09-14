//
// $Id$

package com.threerings.bang.editor;

import java.awt.Color;

import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.samskivert.swing.VGroupLayout;
import com.samskivert.util.ListUtil;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PropConfig;
import com.threerings.bang.game.data.GameCodes;
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

        PieceCategory root = new PieceCategory("", "");
        
        addPiece(root, "viewpoint", new Viewpoint());
        addPiece(root, "markers/start", new Marker(Marker.START));
        //addPiece(root, "markers/bonus", new Marker(Marker.BONUS));
        addPiece(root, "markers/cattle", new Marker(Marker.CATTLE));
        addPiece(root, "markers/lode", new Marker(Marker.LODE));
        addPiece(root, "markers/totem", new Marker(Marker.TOTEM));
        addPiece(root, "markers/safe", new Marker(Marker.SAFE));
        addPiece(root, "markers/robots", new Marker(Marker.ROBOTS));
        addPiece(root, "markers/talisman", new Marker(Marker.TALISMAN));
        addPiece(root, "markers/fetish", new Marker(Marker.FETISH));
        addPiece(root, "markers/safe_alt", new Marker(Marker.SAFE_ALT));
        
        for (PropConfig config : PropConfig.getConfigs()) {
            Prop prop = Prop.getProp(config.type);
            addPiece(root, config.type, Prop.getProp(config.type));
            if (prop.isOwnerConfigurable()) {
                for (int ii = 0; ii < GameCodes.MAX_PLAYERS; ii++) {
                    prop = (Prop)prop.clone();
                    prop.owner = ii;
                    addPiece(root, config.type + ii, prop);
                }
            }
        }
        root.sortChildren();
        
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
        PieceCategory node = ((PieceCategory)path.getLastPathComponent());
        return (node instanceof NamedPiece) ? ((NamedPiece)node).piece : null;
    }
    
    /**
     * Adds a piece to the tree under the specified root.
     *
     * @param type the slash-delimited hierarchical type
     */
    protected void addPiece (PieceCategory root, String type, Piece piece)
    {
        String prefix = "";
        if (root.getParent() != null && !root.townCategory) {
            prefix = root.key + "_";
        }
        
        int idx = type.indexOf('/');
        if (idx == -1) {
            root.add(new NamedPiece(type, prefix + type, piece));
            return;
        }
        
        String cat = type.substring(0, idx);
        PieceCategory child = null;
        for (int ii = 0, nn = root.getChildCount(); ii < nn; ii++) {
            PieceCategory node = (PieceCategory)root.getChildAt(ii);
            if (node.name.equals(cat)) {
                child = node;
                break;
            }
        }
        if (child == null) {
            root.add(child = new PieceCategory(cat, prefix + cat));
        }
        addPiece(child, type.substring(idx + 1), piece);
    }
    
    /** Used to group pieces. */
    protected class PieceCategory extends DefaultMutableTreeNode
    {
        public String name, key;
        public boolean townCategory;

        public PieceCategory (String name, String key)
        {
            this.name = name;
            this.key = key;
            townCategory = ListUtil.contains(BangCodes.TOWN_IDS, key);
        }
        
        public String toString ()
        {
            String msg = "m.piece_" + key;
            return _ctx.getMessageManager().getBundle("editor").exists(msg) ?
                _ctx.xlate("editor", msg) : name;
        }
        
        /**
         * Recursively sorts the children of this node by their names.
         */
        public void sortChildren ()
        {
            if (children == null) {
                return;
            }
            if (getParent() != null) { // keep top level in original order
                @SuppressWarnings("unchecked") List<Object> list = children;
                Collections.sort(list, NAME_COMPARATOR);
            }
            for (Object child : children) {
                ((PieceCategory)child).sortChildren();
            }
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
            allowsChildren = false;
        }
    }
    
    protected BasicContext _ctx;
    protected JTree _tree;
    
    /** Compares objects by their string representations. */
    protected static final Comparator<Object> NAME_COMPARATOR =
        new Comparator<Object>() {
        public int compare (Object o1, Object o2) {
            return o1.toString().compareTo(o2.toString());
        }
    };
}
