//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;

import com.threerings.jme.model.Model;

import com.threerings.bang.client.util.ResultAttacher;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TreeBed;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays trees for the forest guardians scenario.
 */
public class TreeBedSprite extends MobileSprite
{
    public TreeBedSprite ()
    {
        super("props", "indian_post/special/tree_bed");
    }
    
    @Override // documentation inherited
    public Shadow getShadowType ()
    {
        return Shadow.NONE;
    }
    
    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        return "indian_post/special/tree_bed";
    }
    
    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        TreeBed tree = (TreeBed)piece;
        
        // grow to the next stage
        while (_growth < tree.growth) {
            queueAction("grow_stage" + (++_growth));
        }
        if (_growth != tree.growth) {
            _growth = tree.growth;
            _nextIdle = FastMath.FLT_EPSILON;
        }
    }
    
    @Override // documentation inherited
    protected void addProceduralActions ()
    {
        super.addProceduralActions();
        _procActions.put("reacting", new ProceduralAction() {
            public float activate () {
                // TODO: either an animation or a particle effect
                return FastMath.FLT_EPSILON;
            }
        });
    }
    
    @Override // from PieceSprite
    protected void createGeometry ()
    {
        super.createGeometry();
        
        _tlight = _view.getTerrainNode().createHighlight(
            _piece.x, _piece.y, false, false);
        attachHighlight(_status = new PieceStatus(_ctx, _tlight));
        updateStatus();
    }
   
    @Override // documentation inherited
    protected String[] getIdleAnimations ()
    {
        return _dead ? null : new String[] { "idle_stage" + _growth };
    }
    
    @Override // documentation inherited
    protected String getDeadModel ()
    {
        return _name + "/stump" + _growth;
    }
    
    /** The currently depicted growth stage. */
    protected byte _growth;
}
