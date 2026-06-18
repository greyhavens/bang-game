//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.ParticleUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * An influence visualization consisting of a particle system.
 */
public class ParticleInfluenceViz extends InfluenceViz
{
    public ParticleInfluenceViz (String effect)
    {
        _effect = effect;
    }
    
    @Override // documentation inherited
    public void init (BasicContext ctx, PieceSprite target)
    {
        super.init(ctx, target);
        _ctx.loadParticles(_effect, new ResultAttacher<Spatial>(target) {
            public void requestCompleted (Spatial result) {
                super.requestCompleted(result);
                _particles = result;
                // jME3: Spatial.getLocalTranslation() returns the internal vector but mutating it
                // in place does NOT flag the transform refresh, so the particle system stayed at the
                // sprite origin (on the ground, under the unit). setLocalTranslation applies the
                // transform and dirties the spatial, floating it above the unit as the fork intended.
                _particles.setLocalTranslation(new Vector3f(0, 0,
                    _target.getPiece().getHeight() * 0.5f * TILE_SIZE));
            }
        });
    }
    
    @Override // documentation inherited
    public void destroy ()
    {
        if (_particles != null) {
            ParticleUtil.stopAndRemove(_particles);
        }
    }
    
    /** The name of the particle effect to use. */
    protected String _effect;
    
    /** The particle system node. */
    protected Spatial _particles;
}
