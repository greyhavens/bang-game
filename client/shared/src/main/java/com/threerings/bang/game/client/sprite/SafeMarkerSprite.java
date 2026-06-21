//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.material.Material;

import com.samskivert.util.HashIntMap;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Marker;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.SafeMarker;

import com.threerings.openal.SoundGroup;

/**
 * Sprite for the safe markers.
 */
public class SafeMarkerSprite extends MarkerSprite
{
    public SafeMarkerSprite (int type)
    {
        super(type);
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, BangBoard board,
            SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        int type = ((Marker)_piece).getType();

        // The fork built DIRECTIONS.length * HIGHLIGHT_TEXS.length (= 8) TextureStates: the on/off
        // base texture in each of the 4 cardinal facings. The safe markers (concentric squares /
        // circles) are rotationally symmetric, so the fork's per-facing 90-degree rotation has no
        // visible effect; we keep the per-facing array (the on/off selection indexes by orientation)
        // but bind the SAME alpha-correct cached texture for every facing. (An earlier attempt
        // rotated the source image and rebuilt the texture via ImageCache.createImage, which dropped
        // the PNG's alpha.) The fork's emissive "glow" overlay (a second additive texture unit) is
        // NOT reproduced: the _on_emis/_off_emis art is glyph-on-OPAQUE-black and the additive blend
        // did not survive the Highlight render path, so it painted a black square over the tile. The
        // base marker alone is the safe-zone indicator.
        Material[] mats = _highlightMaterials.get(type);
        if (mats == null) {
            String root = (String)SPRITES[type*2+1];
            mats = new Material[DIRECTIONS.length * HIGHLIGHT_TEXS.length];
            for (int ii = 0; ii < HIGHLIGHT_TEXS.length; ii++) {
                Material bmat = RenderUtil.createTextureMaterial(
                    ctx, ctx.getTextureCache().getTexture(root + HIGHLIGHT_TEXS[ii] + ".png"));
                RenderUtil.applyBlendAlpha(bmat);
                RenderUtil.applyOverlayZBuf(bmat);
                for (int dd = 0; dd < DIRECTIONS.length; dd++) {
                    mats[ii * DIRECTIONS.length + dd] = bmat;
                }
            }
            _highlightMaterials.put(type, mats);
        }
        _tlight = view.getTerrainNode().createHighlight(
                piece.x, piece.y, false, (byte)1);
        setOrientation(piece.orientation);
        attachHighlight(_tlight);
    }

    @Override // documenatation inherited
    public void setOrientation (int orientation)
    {
        setOnOff(orientation, ((SafeMarker)_piece).isOn());
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        setOnOff(_piece.orientation, ((SafeMarker)_piece).isOn());
    }

    /**
     * Set the on/off texture for a highlight marker, keyed on the piece's orientation, exactly as
     * the fork indexed its 8 texture states.
     */
    public void setOnOff (int orientation, boolean on)
    {
        if (_tlight == null) {
            return;
        }
        // fork layout: idx = (on ? 0 : 1) * DIRECTIONS.length + orientation
        int idx = (on ? 0 : DIRECTIONS.length) + orientation;
        int type = ((Marker)_piece).getType();
        Material base = _highlightMaterials.get(type)[idx];
        _tlight.setTextures(base, base);
    }

    protected static final String[] HIGHLIGHT_TEXS = {
        "_on", "_off"
    };

    protected static HashIntMap<Material[]> _highlightMaterials =
        new HashIntMap<Material[]>();
}
