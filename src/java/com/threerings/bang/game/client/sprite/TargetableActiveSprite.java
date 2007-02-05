//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.client.sprite.ActiveSprite;

/**
 * Displays homesteads for the land grab scenario.
 */
public class TargetableActiveSprite extends ActiveSprite
    implements Targetable
{
    public TargetableActiveSprite (String type, String name) 
    {
        super(type, name);
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

    protected PieceTarget _target;
}
