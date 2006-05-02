//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

import com.threerings.openal.SoundGroup;

import com.threerings.bang.util.BasicContext;
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
        addController(new Spinner(this, FastMath.PI/2));
    }

    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        return "bonus_" + _type;
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
    protected void createGeometry (BasicContext ctx)
    {
        super.createGeometry(ctx);

        // load up the model for this bonus
        loadModel(ctx, "bonuses", _type);
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
