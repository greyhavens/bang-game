//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.threerings.jme.model.Model;

import com.threerings.bang.client.util.ResultAttacher;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TreeBed;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays trees for the forest guardians scenario.
 */
public class TreeBedSprite extends PropSprite
{
    public TreeBedSprite ()
    {
        super("indian_post/special/tree_bed");
    }
    
    @Override // from PieceSprite
    public boolean isHoverable ()
    {
        return true;
    }
    
    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        return _config.type;
    }
    
    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        
        // change models based on the tree's growth phase
        updateTreeModel();
    }
    
    @Override // from PieceSprite
    protected void createGeometry ()
    {
        super.createGeometry();
        
        // load the tree model
        updateTreeModel();
        
        _tlight = _view.getTerrainNode().createHighlight(
            _piece.x, _piece.y, false, false);
        attachHighlight(_status = new PieceStatus(_ctx, _tlight));
        updateStatus();
    }
    
    protected void updateTreeModel ()
    {
        String model = "indian_post/special/trees/" +
            (_piece.isAlive() ? "" : "stump_") +
            TREE_MODELS[((TreeBed)_piece).growth];
        if (model.equals(_tmodel)) {
            return;
        }
        _tmodel = model;
        _view.addResolving(this);
        _ctx.loadModel("props", model, new ResultAttacher<Model>(this) {
            public void requestCompleted (Model model) {
                super.requestCompleted(model);
                _view.clearResolving(TreeBedSprite.this);
                if (_tree != null) {
                    detachChild(_tree);
                }
                _tree = model;
            }
            public void requestFailed (Exception cause) {
                _view.clearResolving(TreeBedSprite.this);
            }
        });
    }
    
    /** The tree that we switch around. */
    protected Model _tree;
    
    /** The current tree model. */
    protected String _tmodel;
    
    /** The tree models corresponding to each growth stage. */
    protected static final String[] TREE_MODELS = {
        "sprout", "sapling", "mature", "elder" };
}
