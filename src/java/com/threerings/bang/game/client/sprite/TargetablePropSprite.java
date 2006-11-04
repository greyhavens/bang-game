//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Prop;

/**
 * A base class for targetable props.
 */
public class TargetablePropSprite extends PropSprite
    implements Targetable
{
    public TargetablePropSprite (String type)
    {
        super(type);
    }

    @Override // from PieceSprite
    public boolean isHoverable ()
    {
        return true;
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
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);

        _target.updated(piece, tick);
    }

    @Override // from PieceSprite
    protected void createGeometry ()
    {
        super.createGeometry();
        
        Prop prop = (Prop)_piece;
        if (prop.felev > 6) {
            _tlight = _view.getTerrainNode().createHighlight(
                    _piece.x, _piece.y, true, true, prop.computeElevation(
                        _view.getBoard(), _piece.x, _piece.y));
        } else {
            _tlight = _view.getTerrainNode().createHighlight(
                _piece.x, _piece.y, false, false);
        }
        attachHighlight(_status = new PieceStatus(_ctx, _tlight));
        updateStatus();
        attachChild(_target = new PieceTarget(_piece, _ctx));
    }

    protected PieceTarget _target;
}
