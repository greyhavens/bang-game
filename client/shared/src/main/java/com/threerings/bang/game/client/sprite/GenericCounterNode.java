//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.image.Texture;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.BillboardNode;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.util.geom.BufferUtils;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.piece.CounterInterface;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a counter prop along with the count value.
 */
public class GenericCounterNode extends Node
{
    /**
     * Creates the geometry
     */
    public void createGeometry (CounterInterface counter, BasicContext ctx)
    {
        // create a billboard to display this mine's current nugget count
        _ctx = ctx;
        _quad = new Quad("counter", 25, 25);
        _tstate = _ctx.getRenderer().createTextureState();
        _tstate.setEnabled(true);
        _quad.setRenderState(_tstate);
        updateCount(counter);
        _quad.setRenderState(RenderUtil.blendAlpha);
        _quad.setRenderState(RenderUtil.overlayZBuf);
        _quad.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _quad.setLightCombineMode(LightState.OFF);
        BillboardNode bbn = new BillboardNode("cbillboard");
        bbn.attachChild(_quad);
        bbn.setLocalTranslation(new Vector3f(
                    0, 0, (int)((1.0 + 0.5) * TILE_SIZE)));
        attachChild(bbn);
        _quad.setCullMode(CULL_ALWAYS);
    }

    /**
     * Updates the count hovering over the sprite.
     */
    public void updateCount (CounterInterface counter)
    {
        // recompute and display our nugget count
        if (_dcount != counter.getCount()) {
            if (_tstate.getNumberOfSetTextures() > 0) {
                _tstate.deleteAll();
            }
            Vector2f[] tcoords = new Vector2f[4];
            Texture tex = RenderUtil.createTextTexture(
                _ctx, BangUI.COUNTER_FONT, ColorRGBA.gray,
                ColorRGBA.darkGray, String.valueOf(counter.getCount()),
                tcoords, null);
            _quad.setTextureBuffer(
                0, BufferUtils.createFloatBuffer(tcoords));
            // resize our quad to accomodate the text
            float qrat = TILE_SIZE * 0.8f / tcoords[2].y;
            _quad.resize(qrat * tcoords[2].x, qrat * tcoords[2].y);
            _tstate.setTexture(tex);
            _quad.updateRenderState();
            _dcount = counter.getCount();
            _quad.setCullMode(CULL_DYNAMIC);
        }
    }

    protected BasicContext _ctx;
    protected Quad _quad;
    protected TextureState _tstate;
    protected int _dcount = -1;
}
