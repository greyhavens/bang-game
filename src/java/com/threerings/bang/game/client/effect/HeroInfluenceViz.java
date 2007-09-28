//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.util.geom.BufferUtils;

import com.jme.image.Texture;

import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.BillboardNode;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;

import com.threerings.bang.client.BangUI;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.sprite.PieceSprite;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * An influence visualization that shows a floating number.
 */
public class HeroInfluenceViz extends InfluenceViz
{
    public HeroInfluenceViz (int level)
    {
        _level = level;
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, PieceSprite target)
    {
        super.init(ctx, target);
        // create the geometry we'll add the count to
        _count = new Quad("count", 25, 25);
        _tstate = ctx.getRenderer().createTextureState();
        _tstate.setEnabled(true);
        _count.setRenderState(_tstate);
        _count.setRenderState(RenderUtil.blendAlpha);
        _count.setRenderState(RenderUtil.overlayZBuf);
        _count.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _count.setLightCombineMode(LightState.OFF);

        _owner = target.getPiece().owner;
        setLevel(_level);

        // attach the geometry to the sprite
        _billboard = new BillboardNode("hero_influence");
        _billboard.attachChild(_count);
        _billboard.setLocalTranslation(new Vector3f(0, 0, target.getHeight() + 0.5f * TILE_SIZE));
        target.attachChild(_billboard);
        float scale = 1f + _level * 0.05f;
        _target.setLocalScale(new Vector3f(scale, scale, scale));
    }

    /**
     * Update the level number.
     */
    public void setLevel (int level)
    {
        if (_tstate.getNumberOfSetTextures() > 0) {
            _tstate.deleteAll();
        }
        _level = level;
        // generate the texture that will have the current level count
        Vector2f[] tcoords = new Vector2f[4];
        Texture tex = RenderUtil.createTextTexture(
                _ctx, BangUI.COUNTER_FONT, getJPieceColor(_owner), getDarkerPieceColor(_owner),
                String.valueOf(_level), tcoords, null);
        _count.setTextureBuffer(0, BufferUtils.createFloatBuffer(tcoords));
        float qrat = TILE_SIZE * 0.5f / tcoords[2].y;
        _count.resize(qrat * tcoords[2].x, qrat * tcoords[2].y);
        _tstate.setTexture(tex);
        _count.updateRenderState();
        _count.setCullMode(Spatial.CULL_DYNAMIC);
        float scale = 1f + _level * 0.05f;
        _target.setLocalScale(new Vector3f(scale, scale, scale));
    }

    @Override // documentation inherited
    public void destroy ()
    {
        if (_target != null) {
            _target.detachChild(_billboard);
        }
    }

    protected int _level, _owner;
    protected BillboardNode _billboard;
    protected Quad _count;
    protected TextureState _tstate;
}
