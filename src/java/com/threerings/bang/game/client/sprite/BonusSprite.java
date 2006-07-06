//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

import com.threerings.openal.SoundGroup;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.util.SoundUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a bonus piece of some sort.
 */
public class BonusSprite extends PieceSprite
{
    public BonusSprite (String type)
    {
        _type = type;
        if (!_type.endsWith("mine") && !_type.endsWith("spring")) {
            addController(new Spinner(this, FastMath.PI/2));
        }
    }

    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        // strip off the town prefix when reporting our bonus type
        return "bonus_" + _type.substring(_type.lastIndexOf("/")+1);
    }

    @Override // documentation inherited
    public Shadow getShadowType ()
    {
        return Shadow.DYNAMIC;
    }

    @Override // documentation inherited
    public boolean isHoverable ()
    {
        return true;
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();

        // load up the model for this bonus
        loadModel("bonuses", _type);
        
        // if the bonus emits particles while on the board, load those up
        // as well
        String effect = BonusConfig.getConfig(_type).particleEffect;
        if (effect != null) {
            _ctx.loadEffect(effect, new ResultAttacher<Spatial>(this) {
                public void requestCompleted (Spatial result) {
                    super.requestCompleted(result);
                    result.getLocalTranslation().set(0, 0,
                        _piece.getHeight() * 0.5f * TILE_SIZE);
                }
            });
        }
    }

    @Override // documentation inherited
    protected void createSounds (SoundGroup sounds)
    {
        super.createSounds(sounds);

        // preload any sound associated with activating this bonus
        String spath = "rsrc/bonuses/" + _type + "/activate.wav";
        if (SoundUtil.haveSound(spath)) {
            sounds.preloadClip(spath);
        }
    }

    protected String _type;
}
