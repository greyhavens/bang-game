//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

/**
 * A visualization for the Spirit Walk card.  Makes the unit semi-transparent
 * and tints it blue.
 */
public class NoncorporealViz extends InfluenceViz
{
    @Override // documentation inherited
    public void init (BasicContext ctx, PieceSprite target)
    {
        super.init(ctx, target);
        target.setRenderState(
            _mstate = ctx.getRenderer().createMaterialState());
        _mstate.getDiffuse().set(ColorRGBA.white);
        _mstate.setAmbient(_mstate.getDiffuse());
        target.setRenderState(RenderUtil.blendAlpha);
        target.updateRenderState();
        target.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        fadeMaterial(true);
    }
    
    @Override // documentation inherited
    public void destroy ()
    {
        fadeMaterial(false);
    }

    /**
     * Fades into or out of the noncorporeal color.
     */
    protected void fadeMaterial (final boolean in)
    {
        _target.addController(new Controller() {
            public void update (float time) {
                _elapsed = Math.min(_elapsed + time, FADE_DURATION);
                float alpha = _elapsed / FADE_DURATION;
                _mstate.getDiffuse().interpolate(ColorRGBA.white,
                    NONCORPOREAL_COLOR, in ? alpha : (1f - alpha));
                if (_elapsed >= FADE_DURATION) {
                    _target.removeController(this);
                    if (!in) {
                        _target.clearRenderState(RenderState.RS_MATERIAL);
                        _target.clearRenderState(RenderState.RS_ALPHA);
                        _target.updateRenderState();
                        _target.setRenderQueueMode(Renderer.QUEUE_INHERIT);
                    }
                }
            }
            protected float _elapsed;
        });
    }
    
    /** The material state used to control transparency and tint. */
    protected MaterialState _mstate;
    
    /** The color of noncorporeality. */
    protected static final ColorRGBA NONCORPOREAL_COLOR =
        new ColorRGBA(0f, 1f, 1f, 0.625f);
    
    /** The time it takes to fade into or out of the noncorporeal color. */
    protected static final float FADE_DURATION = 0.25f;
}
