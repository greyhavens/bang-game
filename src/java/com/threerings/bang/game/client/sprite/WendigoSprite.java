//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Point;
import java.util.List;

import com.jme.math.Vector3f;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jme.scene.Controller;
import com.jme.scene.state.MaterialState;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.WendigoHandler;

import com.threerings.bang.game.data.BangBoard;

import com.threerings.bang.game.data.piece.Piece;

import com.threerings.openal.SoundGroup;

import static com.threerings.bang.client.BangMetrics.*;
import com.jme.scene.Node;

/**
 * Displays the wendigo.
 */
public class WendigoSprite extends MobileSprite
{
    public WendigoSprite ()
    {
        super("extras", "indian_post/wendigo");
    }

    public void init (BasicContext ctx, BoardView view, BangBoard board,
            SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        setRenderState(RenderUtil.blendAlpha);
        setRenderState(RenderUtil.overlayZBuf);
        setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        updateRenderState();
        fade(true);
    }

    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        return "wendigo";
    }

    @Override // documentation inherited
    public void centerWorldCoords (Vector3f coords)
    {
        // Since the wendigo takes up a 2x2 tile space, center it to the far
        // corner of it's occupying tile
        coords.x += TILE_SIZE;
        coords.y += TILE_SIZE;
    }

    /**
     * Perform a move with an associate penderId for the WendigoHandler.
     */
    public void move (BangBoard board, List<Point> path, float speed,
            WendigoHandler handler, int penderId)
    {
        _handler = handler;
        _penderId = penderId;
        move(board, path, speed);
    }
    
    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();
        _handler.pathCompleted(_penderId);
        fade(false);
    }

    /**
     * Fades in/out the wendigo.
     */
    protected void fade (final boolean in)
    {
        final MaterialState mstate = _ctx.getRenderer().createMaterialState();
        mstate.getAmbient().set(ColorRGBA.white);
        mstate.getDiffuse().set(ColorRGBA.white);
        mstate.getDiffuse().a = (in ? 0f : 0.5f);
        setRenderState(mstate);
        final float duration = (in ? 2f : 0.5f);
        _view.getNode().addController(new Controller() {
            public void update (float time) {
                _elapsed = Math.min(_elapsed + time, duration);
                float alpha = _elapsed / duration;
                if (!in) {
                    alpha = 1f - alpha;
                }
                mstate.getDiffuse().a = 0.5f * alpha;
                setRenderState(mstate);
                updateRenderState();
                if (_elapsed >= duration) {
                    _view.getNode().removeController(this);
                    if (!in) {
                        _view.removeSprite(WendigoSprite.this);
                    }
                }
            }
            protected float _elapsed;
        });
    }

    protected WendigoHandler _handler;

    protected int _penderId;
}
