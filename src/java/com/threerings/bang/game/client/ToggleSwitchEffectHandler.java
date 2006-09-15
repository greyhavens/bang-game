//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.client.sprite.MarkerSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ToggleSwitchEffect;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.ToggleSwitch;
import com.threerings.bang.util.BangContext;

import com.threerings.openal.SoundGroup;

import static com.threerings.bang.Log.log;

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
            boolean square = _tse.state == ToggleSwitch.State.SQUARE;
            for (Marker m : _bview.getMarkers()) {
                MarkerSprite sprite = (MarkerSprite)_view.getPieceSprite(m);
                if (Marker.isMarker(m, Marker.SAFE)) {
                    sprite.setOnOff(m.orientation, square);
                } else if (Marker.isMarker(m, Marker.SAFE_ALT)) {
                    sprite.setOnOff(m.orientation, !square);
                }
            }
        }
        super.pieceAffected(piece, effect);
    }

    /** Reference to our casted effect. */
    protected ToggleSwitchEffect _tse;
}
