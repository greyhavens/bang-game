//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.image.Texture;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.BillboardNode;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.util.geom.BufferUtils;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;

import com.threerings.openal.SoundGroup;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a counter prop along with the count value.
 */
public class CounterSprite extends PropSprite
{
    public CounterSprite (String type)
    {
        super(type);
    }

    @Override // documentation inherited
    public boolean hasTooltip ()
    {
        return true;
    }

    @Override // documentation inherited
    public Coloring getColoringType ()
    {
        return Coloring.DYNAMIC;
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, BangBoard board,
                      SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        updateCount(piece);
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        updateCount(piece);
    }

    /**
     * Updates the count hovering over the sprite.
     */
    protected void updateCount (Piece piece)
    {
        // recompute and display our nugget count
        Counter counter = (Counter)piece;
        if (_piece.owner >= 0 && _dcount != counter.count) {
            if (_tstate.getNumberOfSetTextures() > 0) {
                _tstate.deleteAll();
            }
            Vector2f[] tcoords = new Vector2f[4];
            Texture tex = RenderUtil.createTextTexture(
                _ctx, BangUI.COUNTER_FONT, getJPieceColor(_piece.owner),
                getDarkerPieceColor(_piece.owner), String.valueOf(counter.count),
                tcoords, null);
            _counter.setTextureBuffer(
                0, BufferUtils.createFloatBuffer(tcoords));
            // resize our quad to accomodate the text
            float qrat = TILE_SIZE * 0.8f / tcoords[2].y;
            _counter.resize(qrat * tcoords[2].x, qrat * tcoords[2].y);
            _tstate.setTexture(tex);
            _counter.updateRenderState();
            _dcount = counter.count;
            _counter.setCullMode(CULL_DYNAMIC);
        }
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();

        // create a billboard to display this mine's current nugget count
        _counter = new Quad("counter", 25, 25);
        _tstate = _ctx.getRenderer().createTextureState();
        _tstate.setEnabled(true);
        _counter.setRenderState(_tstate);
        _counter.setRenderState(RenderUtil.blendAlpha);
        _counter.setRenderState(RenderUtil.overlayZBuf);
        _counter.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _counter.setLightCombineMode(LightState.OFF);
        BillboardNode bbn = new BillboardNode("cbillboard");
        bbn.attachChild(_counter);
        bbn.setLocalTranslation(new Vector3f(
                    0, 0, (_config.height + 0.5f) * TILE_SIZE));
        attachChild(bbn);
        _counter.setCullMode(CULL_ALWAYS);
    }

    @Override // documentation inherited
    protected String getHelpIdent (int pidx)
    {
        return (pidx == _piece.owner ? "own_" : "other_") + _config.type;
    }

    protected Quad _counter;
    protected TextureState _tstate;
    protected int _dcount = -1;
}
