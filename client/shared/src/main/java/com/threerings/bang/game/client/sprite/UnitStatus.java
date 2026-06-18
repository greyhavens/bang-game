//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.control.BillboardControl;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;

import com.jmex.bui.background.BBackground;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Influence;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.IconConfig;

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
    public UnitStatus (BasicContext ctx, TerrainNode.Highlight highlight,
                       UnitSprite sprite)
    {
        super(ctx, highlight);
        _sprite = sprite;
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
            setLayerTexture(2, _ticktexs[tickidx]);

            // update our outline texture
            setLayerTexture(0, (_ticksToMove > 0) ? _outtex : _routtex);
        }

        if (_pendo == null || _pendo != pendo) {
            _pendo = pendo;

            Texture2D otex;
            switch (_pendo) {
            case MOVE: otex = _movetex; break;
            case MOVE_SHOOT: otex = _shoottex; break;
            default: otex = null; break;
            }
            if (otex == null) {
                _info[3].setCullHint(CullHint.Always);
                _icon[3].setCullHint(CullHint.Always);
            } else {
                _info[3].setCullHint(CullHint.Dynamic);
                _icon[3].setCullHint(CullHint.Dynamic);
                setLayerTexture(3, otex);
            }
        }

        Unit unit = (Unit)piece;
        if (unit.getMainInfluence() == null || unit.getMainInfluence() != _influence) {
            _influence = unit.getMainInfluence();
            // the influence indicator (e.g. power_up) has no on-unit InfluenceViz, so the fork
            // showed it via layer 4 of the status display: textured onto the ground highlight quad
            // and then shrunk/offset/rotated each frame on the texture transform (rotateWithCamera)
            // so it read as a small upright icon up-and-right of the unit. jME3 Texture2D has no
            // such transform, which left the icon stretched flat across the whole ground tile, so
            // we instead float it as a small screen-facing billboard above the unit (the same
            // mechanism IconInfluenceViz uses for influences that do define a viz).
            if (_influence == null ||
                    (_influence.hidden() && unit.owner != pidx)) {
                clearInfluenceIcon();
            } else {
                updateInfluenceIcon(_influence.getName());
            }
        }
        // layers 4 (info + icon) are never drawn directly; the influence icon is a billboard
        _info[4].setCullHint(CullHint.Always);
        _icon[4].setCullHint(CullHint.Always);
    }

    /**
     * Builds (or retextures) the small screen-facing influence icon floating above the unit.
     */
    protected void updateInfluenceIcon (String name)
    {
        clearInfluenceIcon();
        if (_sprite == null) {
            return;
        }
        // a Node with a BillboardControl keeps the icon screen-facing/upright
        _influenceBillboard = new Node("influence_icon");
        _influenceBillboard.addControl(new BillboardControl());
        _influenceBillboard.setLocalTranslation(new Vector3f(
                    0f, 0f, _sprite.getHeight() - TILE_SIZE/2));

        Geometry iconQuad = IconConfig.createIcon(
                    _ctx, "influences/icons/" + name + ".png",
                    INFLUENCE_ICON_SIZE, INFLUENCE_ICON_SIZE);
        // jME3 Quad is corner-origin; recenter on the billboard pivot and lift it up-and-right so it
        // reads as a small upright icon beside the unit's head (matching the fork's icon placement).
        iconQuad.setLocalTranslation(new Vector3f(
                    TILE_SIZE/4 - INFLUENCE_ICON_SIZE/2f,
                    TILE_SIZE/4 - INFLUENCE_ICON_SIZE/2f, 0f));
        _influenceBillboard.attachChild(iconQuad);
        _sprite.attachChild(_influenceBillboard);
        // refresh transforms/bounds so the billboard orients upright on the first render
        _influenceBillboard.updateGeometricState();
    }

    /** Detaches the floating influence icon, if any. */
    protected void clearInfluenceIcon ()
    {
        if (_influenceBillboard != null && _sprite != null) {
            _sprite.detachChild(_influenceBillboard);
        }
        _influenceBillboard = null;
    }

    @Override // documentation inherited
    public void rotateWithCamera (Quaternion camrot, Vector3f camtrans)
    {
        super.rotateWithCamera(camrot, camtrans);
        // the influence icon is now a screen-facing billboard child of the sprite, so it needs no
        // per-frame texture-transform fixup here (the fork did that for the flat ground layer).
    }

    @Override // documentation inherited
    protected void loadTextures ()
    {
        super.loadTextures();

        if (_ticktexs == null) {
            // load up our various static textures
            _ticktexs = new Texture2D[5];
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

    /** The unit sprite this status is attached to (for the floating influence icon). */
    protected UnitSprite _sprite;

    /** The screen-facing billboard holding the influence icon, or null if none. */
    protected Node _influenceBillboard;

    protected static Texture2D[] _ticktexs;
    protected static Texture2D _outtex, _routtex, _movetex, _shoottex;

    /** The world-space size of the floating influence icon (matches IconInfluenceViz). */
    protected static final float INFLUENCE_ICON_SIZE = TILE_SIZE / 3;

    protected static final float MOD_OFFSET = 3f * TILE_SIZE / 8f;
    protected static final Vector3f[] MOD_COORDS = {
        new Vector3f( MOD_OFFSET, -MOD_OFFSET, 0f),
        new Vector3f(-MOD_OFFSET, -MOD_OFFSET, 0f)
    };
}
