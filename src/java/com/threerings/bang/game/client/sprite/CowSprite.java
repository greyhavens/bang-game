//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.SharedMesh;
import com.jme.scene.state.TextureState;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a cow along with an indicator of who owns it.
 */
public class CowSprite extends MobileSprite
{
    public CowSprite ()
    {
        super("extras", "cow");
    }

    @Override // documentation inherited
    public void updated (BangBoard board, Piece piece, short tick)
    {
        super.updated(board, piece, tick);

        // update our colors in the event that our owner changes
        configureOwnerColors();
    }

    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        if (_owntex == null) {
            loadTextures(ctx);
        }

        super.createGeometry(ctx);
        
        // this is used to indicate who owns us
        _own = new SharedMesh("own", _highlight);
        _own.setRenderState(_owntex);
        _own.updateRenderState();
        _hnode.attachChild(_own);

        // configure our colors
        configureOwnerColors();
    }

    /** Sets up our colors according to our owning player. */
    protected void configureOwnerColors ()
    {
        if (_piece.owner < 0) {
            _own.setCullMode(CULL_ALWAYS);
        } else {
            _highlight.setDefaultColor(JPIECE_COLORS[_piece.owner]);
            _highlight.updateRenderState();
            _own.setCullMode(CULL_DYNAMIC);
        }
    }

    protected static void loadTextures (BasicContext ctx)
    {
        _owntex = RenderUtil.createTexture(
            ctx, ctx.loadImage("textures/ustatus/selected.png"));
    }

    protected SharedMesh _own;

    protected static TextureState _owntex;
}
