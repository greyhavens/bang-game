//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.image.Texture;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.SharedMesh;

import com.jmex.bui.background.BBackground;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Influence;

import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A helper class to manage the composition of our unit status display.
 */
public class UnitStatus extends PieceStatus
{
    /**
     * Creates a unit status helper with the supplied unit sprite highlight
     * node. The status will be textured onto the highlight node (using a
     * {@link SharedMesh}) and will be textured onto a set of quads which will
     * be used to display our iconic unit status (which we make available as a
     * {@link BBackground}.
     */
    public UnitStatus (BasicContext ctx, TerrainNode.Highlight highlight)
    {
        super(ctx, highlight);

    }

    /**
     * Recomposites if necessary our status texture and updates the texture
     * state.
     */
    public void update (Piece piece, int ticksToMove,
                    UnitSprite.AdvanceOrder pendo, boolean selected, int pidx)
    {
        super.update(piece, selected);

        if (_ticksToMove != ticksToMove) {
            _ticksToMove = ticksToMove;

            // update our tick texture
            int tickidx = Math.max(0, 4-ticksToMove);
            Texture ttex = _ticktexs[tickidx];
            getTextureState(_info[2]).setTexture(ttex.createSimpleClone());
            getTextureState(_icon[2]).setTexture(ttex.createSimpleClone());

            // update our outline texture
            Texture otex = (_ticksToMove > 0) ? _outtex : _routtex;
            getTextureState(_info[0]).setTexture(otex.createSimpleClone());
            getTextureState(_icon[0]).setTexture(otex.createSimpleClone());
        }

        if (_pendo == null || _pendo != pendo) {
            _pendo = pendo;

            Texture otex;
            switch (_pendo) {
            case MOVE: otex = _movetex; break;
            case MOVE_SHOOT: otex = _shoottex; break;
            default: otex = null; break;
            }
            if (otex == null) {
                _info[3].setCullMode(CULL_ALWAYS);
                _icon[3].setCullMode(CULL_ALWAYS);
            } else {
                _info[3].setCullMode(CULL_DYNAMIC);
                _icon[3].setCullMode(CULL_DYNAMIC);
                getTextureState(_info[3]).setTexture(otex.createSimpleClone());
                getTextureState(_icon[3]).setTexture(otex.createSimpleClone());
            }
        }

        Unit unit = (Unit)piece;
        if (unit.getMainInfluence() == null || unit.getMainInfluence() != _influence) {
            _influence = unit.getMainInfluence();
            if (_influence == null ||
                    (_influence.hidden() && unit.owner != pidx)) {
                _info[4].setCullMode(CULL_ALWAYS);
            } else {
                Texture tex = _ctx.getTextureCache().getTexture(
                    "influences/icons/" + _influence.getName() + ".png");
                tex.setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
                _info[4].setCullMode(CULL_DYNAMIC);
                getTextureState(_info[4]).setTexture(tex.createSimpleClone());
            }
        }
        _icon[4].setCullMode(CULL_ALWAYS);
    }

    @Override // documentatio inherited
    public void rotateWithCamera (Quaternion camrot, Vector3f camtrans)
    {
        super.rotateWithCamera(camrot, camtrans);

        if (_info[4].getCullMode() != CULL_ALWAYS) {
            Texture tex = getTextureState(_info[4]).getTexture(0);
            if (tex != null) {
                Vector3f trans = new Vector3f(2f, 2f, 2f);
                camrot.mult(trans, trans);
                trans.set(-0.8f - trans.x, -0.2f - trans.y, 0f);
                tex.setScale(new Vector3f(4f, 4f, 4f));
                tex.setTranslation(trans);
            }
        }
    }

    @Override // documentation inherited
    protected void loadTextures ()
    {
        super.loadTextures();

        if (_ticktexs == null) {
            // load up our various static textures
            _ticktexs = new Texture[5];
            for (int ii = 0; ii < _ticktexs.length; ii++) {
                _ticktexs[ii] = prepare("tick_counter_" + ii + ".png");
            }
            _movetex = prepare("move_order.png");
            _shoottex = prepare("shoot_order.png");
            _outtex = prepare("tick_outline.png");
            _routtex = prepare("tick_ready_outline.png");
        }
    }

    @Override // documentation inherited
    protected int numLayers ()
    {
        return 5;
    }

    @Override // documentation inherited
    protected int recolorLayers ()
    {
        return 4;
    }

    protected int _ticksToMove = -1;
    protected Influence _influence;
    protected UnitSprite.AdvanceOrder _pendo;

    protected static Texture[] _ticktexs;
    protected static Texture _outtex, _routtex, _movetex, _shoottex;

    protected static final float MOD_OFFSET = 3f * TILE_SIZE / 8f;
    protected static final Vector3f[] MOD_COORDS = {
        new Vector3f( MOD_OFFSET, -MOD_OFFSET, 0f),
        new Vector3f(-MOD_OFFSET, -MOD_OFFSET, 0f)
    };
}
