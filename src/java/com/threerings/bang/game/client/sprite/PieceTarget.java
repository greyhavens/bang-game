//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.EnumSet;

import com.jme.util.geom.BufferUtils;

import com.jme.image.Texture;

import com.jme.math.Vector2f;
import com.jme.math.Vector3f;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jme.scene.BillboardNode;
import com.jme.scene.Node;

import com.jme.scene.shape.Quad;

import com.jme.scene.state.TextureState;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.effect.NuggetEffect;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.client.BangMetrics.*;

import static com.threerings.bang.Log.log;


/**
 * A target display for Pieces that are Targetable.
 */
public class PieceTarget extends Node
    implements Targetable
{
    public PieceTarget (Piece piece, BasicContext ctx)
    {
        super("piece_target");
        _piece = piece;
        createGeometry(ctx);
    }

    // documentation inherited from Targetable
    public void setTargeted (TargetMode mode, Unit attacker)
    {
        boolean addModifiers = false;
        if (_pendingTick == -1) {
            _tgtquad.setDefaultColor(ColorRGBA.white);
            switch (mode) {
            case NONE:
                _tgtquad.setCullMode(CULL_ALWAYS);
                break;
            case SURE_SHOT:
                displayTextureQuad(_tgtquad, _crosstst[0]);
                addModifiers = true;
                break;
            case MAYBE:
                displayTextureQuad(_tgtquad, _crosstst[1]);
                addModifiers = true;
                break;
            }
        }
        if (!addModifiers) {
            for (int ii = 0; ii < _modquad.length; ii++) {
                _modquad[ii].setCullMode(CULL_ALWAYS);
            }
            return;
        }
        int diff = attacker.computeDamageDiff(_piece);
        if (diff > 0) {
            displayTextureQuad(_modquad[ModIcon.ARROW_UP.idx()],
                    _modtst[ModIcon.ARROW_UP.ordinal()]);
        } else if (diff < 0) {
            displayTextureQuad(_modquad[ModIcon.ARROW_DOWN.idx()],
                    _modtst[ModIcon.ARROW_DOWN.ordinal()]);
        }
        if (_piece instanceof Unit) {
            Unit unit = (Unit)_piece;
            if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                displayTextureQuad(_modquad[ModIcon.STAR.idx()],
                        _modtst[ModIcon.STAR.ordinal()]);
            }
            if (NuggetEffect.NUGGET_BONUS.equals(unit.holding)) {
                displayTextureQuad(_modquad[ModIcon.NUGGET.idx()],
                        _modtst[ModIcon.NUGGET.ordinal()]);
            }
        }
    }

    // documentation inherited from Targetable
    public void setPendingShot (boolean pending)
    {
        if (pending) {
            if (_pendingTick == -1) {
                _tgtquad.setDefaultColor(ColorRGBA.red);
            }
            _pendingTick = _tick;
        } else {
            _pendingTick = -1;
        }
        _tgtquad.setCullMode(pending ? CULL_DYNAMIC : CULL_ALWAYS);
    }

    // documentation inherited from Targetable
    public void configureAttacker (int pidx, int delta)
    {
        // sanity check
        if (_attackers == 0 && delta < 0) {
            log.warning("Requested to decrement attackers but we have none! " +                        "[sprite=" + this + ", pidx=" + pidx +
                        ", delta=" + delta + "].");
            Thread.dumpStack();
            return;
        }

        _attackers += delta;

        if (_attackers > 0) {
            displayTextureQuad(_ptquad, _crosstst[Math.min(_attackers, 3)+1],
                    JPIECE_COLORS[pidx + 1]);
        } else {
            _ptquad.setCullMode(CULL_ALWAYS);
        }
    }

    /**
     * Called to update the target.
     */
    public void updated (Piece piece, short tick)
    {
        _tick = tick;
        _piece = (Piece)piece.clone();
        int ticks = piece.ticksUntilMovable(tick);

        // clear our pending shot once we've been ticked (or if we die)
        if (!piece.isAlive() || (_pendingTick != -1 && tick > _pendingTick)) {
            setPendingShot(false);
        }
    }

    /**
     * Create the geometry.
     */
    protected void createGeometry (BasicContext ctx)
    {
        loadTextures(ctx);

        // we'll use this to keep a few things rotated toward the camera
        BillboardNode bbn = new BillboardNode("billboard");
        bbn.setLocalTranslation(new Vector3f(0, 0, TILE_SIZE/3));
        attachChild(bbn);

        // this icon is displayed when we're highlighted as a potential target
        _tgtquad = RenderUtil.createIcon(_crosstst[0]);
        _tgtquad.setLocalTranslation(new Vector3f(0, 0, 0));
        _tgtquad.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _tgtquad.setRenderState(RenderUtil.alwaysZBuf);
        _tgtquad.updateRenderState();
        bbn.attachChild(_tgtquad);
        _tgtquad.setCullMode(CULL_ALWAYS);

        // these icons are displayed when there are modifiers for a
        // potential target
        _modquad = new Quad[MOD_COORDS.length];
        for (int ii = 0; ii < _modquad.length; ii++) {
            _modquad[ii] = RenderUtil.createIcon(
                    _modtst[0], TILE_SIZE/4f, TILE_SIZE/4f);
            _modquad[ii].setLocalTranslation(MOD_COORDS[ii]);
            _modquad[ii].setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            _modquad[ii].setRenderState(RenderUtil.alwaysZBuf);
            _modquad[ii].updateRenderState();
            bbn.attachChild(_modquad[ii]);
            _modquad[ii].setCullMode(CULL_ALWAYS);
        }

        // this icon is displayed when we have pending shots aimed at us
        _ptquad = RenderUtil.createIcon(_crosstst[2]);
        _ptquad.setLocalTranslation(new Vector3f(0, TILE_SIZE/2, 0));
        _ptquad.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _ptquad.setRenderState(RenderUtil.alwaysZBuf);
        _ptquad.updateRenderState();
        _ptquad.setTextureBuffer(
                0, BufferUtils.createFloatBuffer(PTARG_COORDS));
        bbn.attachChild(_ptquad);
        _ptquad.setCullMode(CULL_ALWAYS);
    }

    /**
     * Helper function to update a quad with a texture state and display it.
     */
    protected void displayTextureQuad (Quad quad, TextureState tst)
    {
        displayTextureQuad(quad, tst, null);
    }

    /**
     * Helper function to update a quad with a texture state and color
     * then display it.
     */
    protected void displayTextureQuad (
            Quad quad, TextureState tst, ColorRGBA color)
    {
        quad.setRenderState(tst);
        quad.setCullMode(CULL_DYNAMIC);
        if (color != null) {
            quad.setDefaultColor(color);
        }
        quad.updateRenderState();
    }

    protected static void loadTextures (BasicContext ctx)
    {
        if (_crosstst == null) {
            _crosstst = new TextureState[CROSS_TEXS.length];
            for (int ii = 0; ii < CROSS_TEXS.length; ii++) {
                _crosstst[ii] = RenderUtil.createTextureState(ctx,
                    "textures/ustatus/crosshairs" + CROSS_TEXS[ii] + ".png");
                _crosstst[ii].getTexture().setWrap(
                        Texture.WM_BCLAMP_S_BCLAMP_T);
            }
        }

        if (_modtst == null) {
            EnumSet<ModIcon> icons = EnumSet.allOf(ModIcon.class);
            _modtst = new TextureState[icons.size()];
            for (ModIcon icon : icons) {
                int idx = icon.ordinal();
                _modtst[idx] = RenderUtil.createTextureState(ctx, icon.png());
                _modtst[idx].getTexture().setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
            }
        }
    }

    /** Used when displaying bonus or penalty modifiers. */
    protected enum ModIcon {
        ARROW_UP (2, "arrow_up"),
        ARROW_DOWN (3, "arrow_down"),
        CANNOT (2, "cannot"),
        STAR (1, "star"),
        NUGGET (0, "nugget");

        ModIcon (int idx, String png) {
            _idx = idx;
            _png = png;
        }

        public int idx () {
            return _idx;
        }

        public String png () {
            return "/textures/ustatus/icon_" + _png + ".png";
        }

        protected final int _idx;
        protected final String _png;
    }

    /** Reference to the piece we are attached to. */
    protected Piece _piece;

    protected short _tick;

    protected Quad _tgtquad, _ptquad;
    protected Quad[] _modquad;

    protected short _pendingTick = -1;
    protected int _attackers;

    protected static TextureState[] _crosstst;
    protected static TextureState[] _modtst;

    protected static final String[] CROSS_TEXS = { "", "_q", "_1", "_2", "_n" };

    protected static final Vector2f[] PTARG_COORDS = {
        new Vector2f(0, 2),
        new Vector2f(0, 0),
        new Vector2f(2, 0),
        new Vector2f(2, 2),
    };

    protected static final float MOD_OFFSET = 3f * TILE_SIZE / 8f;
    protected static final Vector3f[] MOD_COORDS = {
        new Vector3f(-MOD_OFFSET,  MOD_OFFSET, 0f),
        new Vector3f(-MOD_OFFSET, -MOD_OFFSET, 0f),
        new Vector3f( MOD_OFFSET,  MOD_OFFSET, 0f),
        new Vector3f( MOD_OFFSET, -MOD_OFFSET, 0f),
    };
}
