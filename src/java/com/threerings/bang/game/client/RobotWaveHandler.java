//
// $Id$

package com.threerings.bang.game.client;

import com.jme.scene.Spatial;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.client.effect.RepairViz;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.effect.RobotWaveEffect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Handles the effects generated at the beginning and end of waves of robots.
 */
public class RobotWaveHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        super.execute();
        RobotWaveEffect reffect = (RobotWaveEffect)_effect;
        if (reffect.wave > 0) {
            displayStartMarquee(reffect.wave, reffect.difficulty);
        } else {
            displayEndMarquee(reffect.living, reffect.treeIds.length);
        }
        return true;
    }
    
    @Override // documentation inherited
    public void pieceAffected (Piece piece, String effect)
    {
        super.pieceAffected(piece, effect);
        
        // for the tree counted effect, hide the piece before firing off the
        // repair effect so that it is gone when the effect completes (the
        // sprite will be removed immediately afterwards)
        if (effect.equals(RobotWaveEffect.TREE_COUNTED)) {
            RepairViz rviz = new RepairViz();
            PieceSprite sprite = _view.getPieceSprite(piece);
            if (sprite != null) {
                sprite.setCullMode(Spatial.CULL_ALWAYS);
                sprite.getHighlight().setCullMode(Spatial.CULL_ALWAYS);
                queueEffect(sprite, piece, new RepairViz());
            }  
        }
    }
    
    /**
     * Displays the marquee indicating the start of the wave.
     */
    protected void displayStartMarquee (int wave, int difficulty)
    {
        String msg;
        if (wave < 10) {
            msg = "m.nth." + wave;
        } else {
            msg = MessageBundle.tcompose("m.nth.ten_plus", wave);
        }
        _view.fadeMarqueeInOut(MessageBundle.compose("m.wave_start", msg,
            MessageBundle.tcompose("m.stars", difficulty + 1)), 1f);
        notePender(2f);
    }
    
    /**
     * Displays the marquee indicating the end of the wave.
     */
    protected void displayEndMarquee (int living, int total)
    {
        _view.fadeMarqueeInOut(MessageBundle.tcompose(
            "m.wave_end", living, total), 1f);
        notePender(2f);
    }
}
