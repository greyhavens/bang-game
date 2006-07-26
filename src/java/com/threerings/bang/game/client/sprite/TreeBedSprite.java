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
        
        // position the tree according to the bed's growth phase
        updateTreeHeight();
    }
    
    @Override // from PieceSprite
    protected void createGeometry ()
    {
        super.createGeometry();
        
        // load th e
        _view.addResolving(this);
        _ctx.loadModel("props", "indian_post/natural/tree_pine_green_tall",
            new ResultAttacher<Model>(this) {
            public void requestCompleted (Model model) {
                super.requestCompleted(model);
                _view.clearResolving(TreeBedSprite.this);
                _tree = model;     
                updateTreeHeight();          
            }
            public void requestFailed (Exception cause) {
                _view.clearResolving(TreeBedSprite.this);
            }
        });
        
        _tlight = _view.getTerrainNode().createHighlight(
            _piece.x, _piece.y, false, false);
        attachHighlight(_status = new PieceStatus(_ctx, _tlight));
        updateStatus();
    }
    
    protected void updateTreeHeight ()
    {
        float height = 3.95f * TILE_SIZE,
            depth = height - ((TreeBed)_piece).growth * height /
                TreeBed.FULLY_GROWN;
        _tree.getLocalTranslation().set(0f, 0f, -depth);
    }
    
    /** The tree that we raise up and down. */
    protected Model _tree;
}
