//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.client.sprite.SafeMarkerSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ToggleSwitchEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.SafeMarker;
import com.threerings.bang.util.BangContext;

import com.threerings.openal.SoundGroup;

/**
 * Handles updating highlights during a switch.
 */
public class ToggleSwitchEffectHandler extends EffectHandler
{
    @Override // documentation inherited
    public void init (
        BangContext ctx, BangObject bangobj, int pidx, BangBoardView view,
        BangView bview, SoundGroup sounds, Effect effect)
    {
        super.init(ctx, bangobj, pidx, view, bview, sounds, effect);
        _tse = (ToggleSwitchEffect)_effect;
    }

    @Override // documentation inherited
    public void pieceAffected (Piece piece, String effect)
    {
        if (piece.pieceId == _tse.switchId && _tse.state != null) {
            for (Piece p : _bangobj.pieces) {
                if (p instanceof SafeMarker) {
                    SafeMarkerSprite sprite = 
                        (SafeMarkerSprite)_view.getPieceSprite(p);
                    sprite.updated(p, _tick);
                }
            }
        }
        super.pieceAffected(piece, effect);
    }

    /** Reference to our casted effect. */
    protected ToggleSwitchEffect _tse;
}
