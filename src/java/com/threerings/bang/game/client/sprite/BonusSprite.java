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

import com.threerings.bang.game.data.BangBoard;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a bonus piece of some sort.
 */
public class BonusSprite extends MobileSprite
{
    public BonusSprite (String type)
    {
        super("bonuses", type);
        if (!isHidden()) {
            addController(new Spinner(this, FastMath.PI/2));
        }
    }

    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        // strip off the town prefix when reporting our bonus type
        return "bonus_" + _name.substring(_name.lastIndexOf("/")+1);
    }

    @Override // documentation inherited
    public Shadow getShadowType ()
    {
        return isHidden() ? Shadow.NONE : Shadow.DYNAMIC;
    }

    /**
     * Determines whether this bonus is hidden underground.
     */
    protected boolean isHidden ()
    {
        return _name.endsWith("trap") || _name.endsWith("spring");
    }
    
    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();

        // if the bonus emits particles while on the board, load those up
        // as well
        String effect = BonusConfig.getConfig(_name).particleEffect;
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
    protected void createDustManager ()
    {
        // no-op
    }
    
    @Override // documentation inherited
    protected void createSounds (SoundGroup sounds)
    {
        // preload any sound associated with activating this bonus
        String spath = "rsrc/bonuses/" + _name + "/activate.wav";
        if (SoundUtil.haveSound(spath)) {
            sounds.preloadClip(spath);
        }
    }
    
    @Override // documentation inherited
    protected void moveSprite (BangBoard board)
    {
        setLocation(board, _piece.x, _piece.y);
    }
}
