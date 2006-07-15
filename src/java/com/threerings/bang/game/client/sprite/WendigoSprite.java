//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.List;

import com.jme.math.Vector3f;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jme.scene.state.MaterialState;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.WendigoHandler;

import com.threerings.bang.game.data.BangBoard;

import com.threerings.bang.game.data.piece.Piece;

import com.threerings.openal.SoundGroup;

import static com.threerings.bang.client.BangMetrics.*;

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
        MaterialState mstate = _ctx.getRenderer().createMaterialState();
        mstate.getAmbient().set(ColorRGBA.white);
        mstate.getDiffuse().set(ColorRGBA.white);
        mstate.getDiffuse().a = .5f;
        setRenderState(mstate);
        setRenderState(RenderUtil.blendAlpha);
        setRenderState(RenderUtil.overlayZBuf);
        setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        updateRenderState();
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
    public void move (BangBoard board, List path, float speed,
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
        _view.removeSprite(this);
    }

    protected WendigoHandler _handler;

    protected int _penderId;
}
