//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import com.jme3.material.Material;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

import com.samskivert.util.HashIntMap;

import com.threerings.jme.util.ImageCache;

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

        // The fork built DIRECTIONS.length * HIGHLIGHT_TEXS.length (= 8) TextureStates: the
        // on/off base texture in each of the 4 cardinal facings (rotated 90 degrees per facing via
        // Texture.setRotation) plus an additive emissive overlay unit for the safe-zone glow. jME3
        // Texture2D carries no rotation transform, so we instead rotate the source image 90 degrees
        // per facing and build one base Material per (on/off x facing) state; the emissive overlay
        // is a parallel set of additive Materials driven onto a second highlight (REPLACE light
        // combine in the fork -> AlphaAdditive blend here).
        Material[] mats = _highlightMaterials.get(type);
        Material[] emis = _emissiveMaterials.get(type);
        if (mats == null) {
            String root = (String)SPRITES[type*2+1];
            mats = new Material[DIRECTIONS.length * HIGHLIGHT_TEXS.length];
            emis = new Material[DIRECTIONS.length * HIGHLIGHT_TEXS.length];
            for (int ii = 0; ii < HIGHLIGHT_TEXS.length; ii++) {
                BufferedImage bimg = ctx.getImageCache().getBufferedImage(
                    root + HIGHLIGHT_TEXS[ii] + ".png");
                BufferedImage eimg = ctx.getImageCache().getBufferedImage(
                    root + EMISSIVE_TEXS[ii] + ".png");
                for (int dd = 0; dd < DIRECTIONS.length; dd++) {
                    int idx = ii * DIRECTIONS.length + dd;
                    mats[idx] = RenderUtil.createTextureMaterial(
                        ctx, rotatedTexture(root + HIGHLIGHT_TEXS[ii], bimg, dd));
                    RenderUtil.applyBlendAlpha(mats[idx]);
                    RenderUtil.applyOverlayZBuf(mats[idx]);

                    emis[idx] = RenderUtil.createTextureMaterial(
                        ctx, rotatedTexture(root + EMISSIVE_TEXS[ii], eimg, dd));
                    RenderUtil.applyAddAlpha(emis[idx]);
                    RenderUtil.applyOverlayZBuf(emis[idx]);
                }
            }
            _highlightMaterials.put(type, mats);
            _emissiveMaterials.put(type, emis);
        }
        _tlight = view.getTerrainNode().createHighlight(
                piece.x, piece.y, false, (byte)1);
        // a second highlight one layer up carries the additive emissive glow
        _elight = view.getTerrainNode().createHighlight(
                piece.x, piece.y, false, (byte)2);
        setOrientation(piece.orientation);
        attachHighlight(_tlight);
        attachHighlight(_elight);
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
     * Set the on/off texture for a highlight marker. Selects the facing-rotated base material (and
     * the matching additive emissive overlay) keyed on the piece's orientation, exactly as the fork
     * indexed its 8 texture states.
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
        if (_elight != null) {
            Material glow = _emissiveMaterials.get(type)[idx];
            _elight.setTextures(glow, glow);
            // the additive glow only shows for the "on" (safe) state
            _elight.setCullHint(on ? CullHint.Dynamic : CullHint.Always);
        }
    }

    /**
     * Returns (caching) a {@link Texture2D} for the supplied source image rotated by
     * {@code dir * 90} degrees, the jME3 stand-in for the fork's per-facing
     * {@code Texture.setRotation}.
     */
    protected Texture2D rotatedTexture (String key, BufferedImage src, int dir)
    {
        if (dir == 0) {
            // facing NORTH: no rotation, reuse the cached upright texture
            return _ctx.getTextureCache().getTexture(key + ".png");
        }
        BufferedImage rot = rotateImage(src, dir);
        Image img = ImageCache.createImage(rot, true);
        Texture2D tex = new Texture2D();
        RenderUtil.configureTexture(tex, img);
        tex.setName(key + "_r" + dir);
        return tex;
    }

    /**
     * Rotates a square marker image by {@code dir} quarter-turns (90 degrees each) about its
     * centre.
     */
    protected static BufferedImage rotateImage (BufferedImage src, int dir)
    {
        int w = src.getWidth(), h = src.getHeight();
        AffineTransform xform = new AffineTransform();
        xform.translate(w / 2.0, h / 2.0);
        xform.rotate(dir * Math.PI / 2.0);
        xform.translate(-w / 2.0, -h / 2.0);
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        new AffineTransformOp(xform, AffineTransformOp.TYPE_BILINEAR).filter(src, dst);
        return dst;
    }

    protected TerrainNode.Highlight _elight;

    protected static final String[] HIGHLIGHT_TEXS = {
        "_on", "_off"
    };

    protected static final String[] EMISSIVE_TEXS = {
        "_on_emis", "_off_emis"
    };

    protected static HashIntMap<Material[]> _highlightMaterials =
        new HashIntMap<Material[]>();

    protected static HashIntMap<Material[]> _emissiveMaterials =
        new HashIntMap<Material[]>();
}
