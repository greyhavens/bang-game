//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Color;

import com.jme.image.Texture;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.BillboardNode;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.TextureState;
import com.jme.util.geom.BufferUtils;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Claim;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a claim along with the count of nuggets remaining within it.
 */
public class ClaimSprite extends PropSprite
{
    public ClaimSprite (String type)
    {
        super(type);
    }

    @Override // documentation inherited
    public void updated (BangBoard board, Piece piece, short tick)
    {
        super.updated(board, piece, tick);

        // recompute and display our nugget count
        Claim claim = (Claim)piece;
        if (_piece.owner >= 0 && _dnuggets != claim.nuggets) {
            Vector2f[] tcoords = new Vector2f[4];
            Texture tex = RenderUtil.createTextTexture(
                BangUI.COUNTER_FONT, JPIECE_COLORS[_piece.owner],
                String.valueOf(claim.nuggets), tcoords);
            _counter.setTextureBuffer(BufferUtils.createFloatBuffer(tcoords));
            // resize our quad to accomodate the text
            _counter.resize(10 * tcoords[2].x, 10 * tcoords[2].y);
            _tstate.setTexture(tex);
            _counter.updateRenderState();
            _dnuggets = claim.nuggets;
            _counter.setCullMode(CULL_DYNAMIC);
        }
    }

    @Override // documentation inherited
    protected void createGeometry (BangContext ctx)
    {
        super.createGeometry(ctx);

        // create a billboard to display this mine's current nugget count
        _counter = new Quad("counter", 25, 25);
        _tstate = ctx.getRenderer().createTextureState();
        _tstate.setEnabled(true);
        _counter.setRenderState(_tstate);
        _counter.setRenderState(RenderUtil.blendAlpha);
        _counter.setRenderState(RenderUtil.overlayZBuf);
        _counter.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        BillboardNode bbn = new BillboardNode("cbillboard");
        bbn.attachChild(_counter);
        // TODO: account properly for the height of the claim model
        bbn.setLocalTranslation(new Vector3f(0, 0, 3*TILE_SIZE/2));
        attachChild(bbn);
        _counter.setCullMode(CULL_ALWAYS);
    }

    protected Quad _counter;
    protected TextureState _tstate;
    protected int _dnuggets = -1;
}
