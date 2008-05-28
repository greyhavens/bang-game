//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.scene.Spatial;
import com.threerings.jme.model.Model;
import com.threerings.bang.util.RenderUtil;
import com.threerings.bang.game.client.sprite.BreakableSprite;

/**
 * Sprite for Fireworks
 */
public class FireworksSprite extends BreakableSprite
{
    public FireworksSprite (String type, String name)
    {
        super(type, name);
    }

    @Override // documentation inherited
    protected void modelLoaded (Model model)
    {
        super.modelLoaded(model);
        String bframe = _model.getProperties().getProperty(
            "ballistic_shot_frame");
        if (bframe != null) {
            _rocketDelay = (float)Integer.parseInt(bframe) /
                _model.getAnimation("shooting").frameRate;
        }
        String bsource = _model.getProperties().getProperty(
            "ballistic_shot_source");
        _rocketSource = (bsource == null ?
            null : _model.getDescendant(bsource));
    }

    /**
     * Returns the delay to use before releasing the shot sprite from the
     * unit sprite (in order to match the shooting animation) if this is
     * a ballistic unit.
     */
    public float getRocketDelay ()
    {
        return _rocketDelay;
    }

    /**
     * Returns the source node whose location should be used as the starting
     * point of ballistic shots, or <code>null</code> if a source was not
     * configured or is outside of the view frustum.
     */
    public Spatial getRocketSource ()
    {
        return (_rocketSource != null &&
            !RenderUtil.isOutsideFrustum(_rocketSource)) ?
                _rocketSource : null;
    }

    protected String getSmokeEffect()
    {
        return "boom_town/fireworks/sparks";
    }

    protected float _rocketDelay;
    protected Spatial _rocketSource;
}