//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Homestead;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Displays homesteads for the land grab scenario.
 */
public class HomesteadSprite extends ActiveSprite
    implements Targetable
{
    public HomesteadSprite ()
    {
        super("props", "frontier_town/special/homestead");
    }
    
    @Override // documentation inherited
    public Coloring getColoringType () {
        return Coloring.DYNAMIC;
    }
    
    @Override // documentation inherited
    public boolean isHoverable ()
    {
        return true;
    }
    
    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        String prefix = (pidx == _powner) ?
            "own" : (pidx == -1 ? "unclaimed" : "other");
        return prefix + "_frontier_town/special/homestead";
    }
    
    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        int oowner = _powner;
        super.updated(piece, tick);
        _target.updated(piece, tick);
        
        // build it up or tear it down
        if (oowner != piece.owner) {
            queueAction(oowner == -1 ? "unclaimed_build" : "claimed_dying");
        }
    }
    
    // from interface Targetable
    public void setTargeted (BangObject bangobj, TargetMode mode, Unit attacker)
    {
        _target.setTargeted(bangobj, mode, attacker);
    }

    // from interface Targetable
    public void setPendingShot (boolean pending)
    {
        _target.setPendingShot(pending);
    }

    // from interface Targetable
    public void setPossibleShot (boolean possible)
    {
        _target.setPossibleShot(possible);
    }

    // from interface Targetable
    public void configureAttacker ( int pidx, int delta)
    {
        _target.configureAttacker(pidx, delta);
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
        attachChild(_target = new PieceTarget(_piece, _ctx));
    }
    
    @Override // documentation inherited
    protected String[] getIdleAnimations ()
    {
        return new String[] { (_powner == -1 ? "un" : "") + "claimed_idle" };
    }
    
    protected PieceTarget _target;
}
