//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Arc2D;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;

import com.jme3.scene.Spatial.CullHint;

import com.jmex.bui.backend.BackendProvider;
import com.jmex.bui.backend.Jme3RenderBackend;
import com.jmex.bui.background.BBackground;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.jme.util.ImageCache;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A helper class to manage the composition of our piece status display.
 *
 * <h3>jME3 cutover (Phase 2, cluster 1)</h3>
 *
 * The fork textured the status onto the highlight node via a {@code SharedMesh} (now
 * {@link TerrainNode.SharedHighlight}, a {@link Geometry} sharing the highlight's mesh) and onto a
 * set of {@code Quad}s for the iconic UI status. Each layer now carries its own {@link Material}
 * (textured + colour-param tinted) instead of a fork {@code TextureState} + batch default colour.
 *
 * <p>The iconic {@link BBackground} drew the quads in immediate mode ({@code Quad.draw(Renderer)});
 * jME3 has no immediate draw, but the icon layers are already textured/owner-tinted {@link Geometry}
 * objects, so {@link #getIconBackground} now renders those geometries through the BUI jME3
 * {@link RenderManager} (positioned via the backend translate stack) -- reproducing the fork's
 * iconic plate.
 *
 * <p><b>Still deferred:</b> the fork rotated each on-ground status texture to follow the camera via
 * {@code Texture.setRotation/setTranslation} ({@link #rotateWithCamera}) so the flat ground-highlight
 * status icons read upright as the camera orbited. jME3 {@code Texture2D} carries no transform, and
 * the {@code _info} layers share the highlight's mesh ({@link TerrainNode.SharedHighlight}), so the
 * UVs cannot be rotated in place without corrupting the shared highlight. A faithful fix needs a
 * per-layer UV-transform material (custom shader param); see {@link #rotateWithCamera}. The one
 * layer that genuinely needed to stay upright -- the influence icon -- was already migrated to a
 * screen-facing {@code BillboardControl} in {@link UnitStatus}, so the remaining gap is only the
 * cosmetic spin of the flat damage/tick ground indicators.
 */
public class PieceStatus extends Node
{
    /** The size of the status textures. */
    public static final int STATUS_SIZE = 128;

    /** The size of the status icon on screen. */
    public static final int ICON_SIZE = 64;

    /**
     * Creates a piece status helper with the supplied piece sprite highlight node.
     */
    public PieceStatus (BasicContext ctx, TerrainNode.Highlight highlight)
    {
        this(ctx, highlight, null, null);
    }

    /**
     * Creates a piece status helper with the supplied piece sprite highlight node.
     *
     * @param color the primary indicator color, or <code>null</code> to use the one corresponding
     * to the piece owner
     * @param dcolor the darker indicator color, or <code>null</code>
     */
    public PieceStatus (
        BasicContext ctx, TerrainNode.Highlight highlight, ColorRGBA color,
        ColorRGBA dcolor)
    {
        super("piece_status");
        _ctx = ctx;
        _color = color;
        _dcolor = dcolor;

        loadTextures();

        _info = new TerrainNode.SharedHighlight[numLayers()];
        _infoMats = new Material[numLayers()];
        _icon = new Geometry[numLayers()];
        _iconMats = new Material[numLayers()];
        // configure the info layers
        for (int ii = 0; ii < _info.length; ii++) {
            _info[ii] = new TerrainNode.SharedHighlight("info" + ii, highlight);
            _infoMats[ii] = newLayerMaterial();
            _info[ii].setMaterial(_infoMats[ii]);
            attachChild(_info[ii]);

            _icon[ii] = new Geometry("icon" + ii, new Quad(ICON_SIZE, ICON_SIZE));
            _iconMats[ii] = newLayerMaterial();
            _icon[ii].setMaterial(_iconMats[ii]);
            RenderUtil.setOverlay(_icon[ii]);
            _icon[ii].setLocalTranslation(ICON_SIZE/2f, ICON_SIZE/2f, 0f);
        }
    }

    /** Creates a fresh textured/colour-tinted layer material (Unshaded + Color, alpha-blended). */
    protected Material newLayerMaterial ()
    {
        Material mat = new Material(_ctx.getAssetManager(),
            "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(ColorRGBA.White));
        RenderUtil.applyBlendAlpha(mat);
        return mat;
    }

    /**
     * Called to keep our on-ground status textures rotated in line with the camera.
     *
     * <p>The fork applied {@code camrot}/{@code camtrans} as a per-texture matrix transform on each
     * {@code _info} ground-highlight layer, spinning the texture about its centre to counter the
     * camera azimuth so the flat status icons stayed upright on screen as the camera orbited. jME3
     * {@code Texture2D} has no transform matrix, and the {@code _info} layers share the highlight's
     * mesh ({@link TerrainNode.SharedHighlight}), so the UVs cannot be rotated in place without
     * corrupting the shared highlight. Reproducing this requires a per-layer material whose shader
     * rotates the texture coordinates (the stock {@code Unshaded.j3md} exposes no such param), which
     * is left as a follow-up. The one layer that had to read upright -- the influence icon -- is
     * already handled as a screen-facing {@code BillboardControl} in {@link UnitStatus}; the
     * remaining gap is only the cosmetic spin of the flat damage/tick/order ground indicators.
     */
    public void rotateWithCamera (Quaternion camrot, Vector3f camtrans)
    {
        // see Javadoc: a faithful texture-matrix spin needs a custom UV-transform material; the
        // upright influence icon is already a billboard, so this stays a no-op for the flat layers.
    }

    /**
     * Returns a background that can be used to render this unit's status in iconic form in the unit
     * status user interface.
     *
     * <p>The fork drew each iconic status {@code Quad} in BUI's immediate-mode painter via
     * {@code _icon[ii].draw(renderer)}. jME3 has no immediate draw, but the icon layers are already
     * full {@link Geometry} objects carrying their own textured/owner-tinted {@link Material}
     * (composed in {@link #update}); we render those same geometries through the BUI jME3
     * {@link RenderManager}, positioning each at the component's GUI-space origin (the BUI translate
     * stack offset plus the supplied {@code x,y}). This reproduces the fork's iconic plate exactly:
     * same textures, same per-layer tint, same {@code ICON_SIZE} quad, same centered placement.
     */
    public BBackground getIconBackground ()
    {
        return new BBackground() {
            public int getMinimumWidth () {
                return ICON_SIZE;
            }
            public int getMinimumHeight () {
                return ICON_SIZE;
            }
            public void render (RenderManager renderer, int x, int y,
                                int width, int height, float alpha) {
                if (renderer == null) {
                    return;
                }
                // honor the BUI translate stack so the icons land at the component's screen origin
                int tx = x, ty = y;
                if (BackendProvider.get() instanceof Jme3RenderBackend) {
                    Jme3RenderBackend backend = (Jme3RenderBackend)BackendProvider.get();
                    tx += backend.getTranslateX();
                    ty += backend.getTranslateY();
                }
                for (int ii = 0; ii < _icon.length; ii++) {
                    // the fork skipped CULL_ALWAYS layers; mirror that (layer 4 etc. are culled)
                    if (_icon[ii].getCullHint() == CullHint.Always) {
                        continue;
                    }
                    // jME3 Quad is corner-origin (spans [0,ICON_SIZE]); place its lower-left at the
                    // component origin so it fills the ICON_SIZE x ICON_SIZE background rect
                    _icon[ii].setLocalTranslation(tx, ty, 0f);
                    _icon[ii].updateGeometricState();
                    renderer.renderGeometry(_icon[ii]);
                }
            }
        };
    }

    /**
     * Copies the highlight translation to the info translations.
     */
    public void updateTranslations (TerrainNode.Highlight highlight)
    {
        Vector3f trans = highlight.getLocalTranslation();
        for (int ii = 0; ii < _info.length; ii++) {
            _info[ii].setLocalTranslation(new Vector3f(trans));
        }
    }

    /**
     * Recomposites if necessary our status texture and updates the materials.
     */
    public void update (Piece piece, boolean selected)
    {
        if (_owner != piece.owner) {
            // set up our starting outline color the first time we're updated
            _owner = piece.owner;
            ColorRGBA color = getColor(), dcolor = getDarkerColor();
            setLayerColor(0, dcolor);
            for (int ii = 1; ii < recolorLayers(); ii++) {
                setLayerColor(ii, color);
            }
            setLayerTexture(0, _damout);
        }

        int dlevel = Math.max(0, (int)Math.floor(piece.damage/10f));
        if (_dlevel != dlevel) {
            _dlevel = dlevel;
            setLayerTexture(1, _damtexs[dlevel]);
        }

        if (_selected != selected) {
            _selected = selected;
            ColorRGBA color = _selected ? ColorRGBA.White : getDarkerColor();
            setLayerColor(0, color);
        }
    }

    /** Sets the colour param on both the info and icon material for the given layer. */
    protected void setLayerColor (int layer, ColorRGBA color)
    {
        _infoMats[layer].setColor("Color", new ColorRGBA(color));
        _iconMats[layer].setColor("Color", new ColorRGBA(color));
    }

    /** Sets the texture on both the info and icon material for the given layer. */
    protected void setLayerTexture (int layer, Texture tex)
    {
        _infoMats[layer].setTexture("ColorMap", tex);
        _iconMats[layer].setTexture("ColorMap", tex);
    }

    /**
     * Returns the primary color of the status view.
     */
    protected ColorRGBA getColor ()
    {
        return (_color == null) ? getJPieceColor(_owner) : _color;
    }

    /**
     * Returns the darker color of the status view.
     */
    protected ColorRGBA getDarkerColor ()
    {
        return (_dcolor == null) ? getDarkerPieceColor(_owner) : _dcolor;
    }

    /**
     * Loads up the textures used by the status display.
     */
    protected void loadTextures ()
    {
        if (_damtexs == null) {
            // we generate ten discrete damage levels and pick the closest one
            // to represent a unit's damage (this is to avoid slow and
            // expensive BufferedImage rendering during the game)
            BufferedImage empty = _ctx.getImageCache().getBufferedImage(
                PPRE + "health_meter_empty.png");
            BufferedImage full = _ctx.getImageCache().getBufferedImage(
                PPRE + "health_meter_full.png");
            _damtexs = new Texture2D[11];
            _damtexs[0] = RenderUtil.createTexture(_ctx, ImageCache.createImage(full, false));
            _damtexs[10] = RenderUtil.createTexture(_ctx, ImageCache.createImage(empty, false));
            for (int ii = 1; ii < 10; ii++) {
                _damtexs[ii] = createDamageTexture(_ctx, empty, full, ii*10);
            }
            for (int ii = 0; ii < _damtexs.length; ii++) {
                _damtexs[ii].setWrap(WrapMode.Clamp);
            }
            _damout = prepare("damage_outline.png");
        }
    }

    /**
     * Number of layers.
     */
    protected int numLayers ()
    {
        return 2;
    }

    /**
     * Number of layers to recolor.
     */
    protected int recolorLayers ()
    {
        return numLayers();
    }

    protected Texture2D prepare (String path)
    {
        Texture2D tex = RenderUtil.createTexture(_ctx,
            _ctx.getImageCache().getImage(PPRE + path, false));
        tex.setWrap(WrapMode.Clamp);
        return tex;
    }

    protected static Texture2D createDamageTexture (
        BasicContext ctx, BufferedImage empty, BufferedImage full, int level)
    {
        BufferedImage target = ImageCache.createCompatibleImage(STATUS_SIZE, STATUS_SIZE, true);
        Graphics2D gfx = (Graphics2D)target.getGraphics();
        try {
            // combine the empty and full images with a custom clip
            gfx.drawImage(empty, 0, 0, null);
            float extent = (100 - level) / 100f * (90 - 2*ARC_INSETS);
            gfx.setClip(new Arc2D.Float(
                            -STATUS_SIZE/8, -STATUS_SIZE/8,
                            // expand the width and height a smidge to avoid
                            // funny business around the edges
                            10*STATUS_SIZE/8, 10*STATUS_SIZE/8,
                            90 - ARC_INSETS - extent, extent, Arc2D.PIE));
            gfx.drawImage(full, 0, 0, null);
        } finally {
            gfx.dispose();
        }
        return RenderUtil.createTexture(ctx, ImageCache.convertImage(target));
    }

    protected BasicContext _ctx;
    protected ColorRGBA _color, _dcolor;
    protected int _owner = -2, _dlevel = -1;
    protected boolean _selected;

    protected TerrainNode.SharedHighlight[] _info;
    protected Material[] _infoMats;
    protected Geometry[] _icon;
    protected Material[] _iconMats;

    protected static Texture2D[] _damtexs;
    protected static Texture2D _damout;

    /** Defines the amount by which the damage arc image is inset from a
     * full quarter circle (on each side): 8 degrees. */
    protected static final float ARC_INSETS = 7;

    /** The path prefix for all of our textures. */
    protected static final String PPRE = "textures/ustatus/";
}
