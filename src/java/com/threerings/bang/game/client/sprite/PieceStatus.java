//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Arc2D;

import com.jme.image.Texture;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;

import com.jmex.bui.background.BBackground;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.jme.util.ImageCache;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A helper class to manage the composition of our piece status display.
 */
public class PieceStatus extends Node
{
    /** The size of the status textures. */
    public static final int STATUS_SIZE = 128;

    /** The size of the status icon on screen. */
    public static final int ICON_SIZE = 64;

    /**
     * Creates a piece status helper with the supplied piece sprite highlight
     * node. The status will be textured onto the highlight node (using a
     * {@link SharedMesh}) and will be textured onto a set of quads which will
     * be used to display our iconic unit status (which we make available as a
     * {@link BBackground}.
     */
    public PieceStatus (BasicContext ctx, TerrainNode.Highlight highlight)
    {
        this(ctx, highlight, null, null);
    }

    /**
     * Creates a piece status helper with the supplied piece sprite highlight
     * node. The status will be textured onto the highlight node (using a
     * {@link SharedMesh}) and will be textured onto a set of quads which will
     * be used to display our iconic unit status (which we make available as a
     * {@link BBackground}.
     *
     * @param color the primary indicator color, or <code>null</code> to use
     * the one corresponding to the piece owner
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

        _info = new SharedMesh[numLayers()];
        _icon = new Quad[numLayers()];
        // configure the info layers
        for (int ii = 0; ii < _info.length; ii++) {
            _info[ii] = new TerrainNode.SharedHighlight(
                "info" + ii, highlight);
            _info[ii].setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            _info[ii].setRenderState(ctx.getRenderer().createTextureState());
            _info[ii].updateRenderState();
            attachChild(_info[ii]);

            _icon[ii] = new Quad("icon" + ii, ICON_SIZE, ICON_SIZE);
            _icon[ii].setRenderState(ctx.getRenderer().createTextureState());
            _icon[ii].setRenderState(RenderUtil.blendAlpha);
            _icon[ii].getLocalTranslation().x = ICON_SIZE/2f;
            _icon[ii].getLocalTranslation().y = ICON_SIZE/2f;
            _icon[ii].updateGeometricState(0, true);
            _icon[ii].updateRenderState();
        }
    }

    /**
     * Called to keep our textures rotated in line with the camera.
     */
    public void rotateWithCamera (Quaternion camrot, Vector3f camtrans)
    {
        for (int ii = 0; ii < _info.length; ii++) {
            if (_info[ii].getCullMode() != CULL_ALWAYS) {
                Texture tex = getTextureState(_info[ii]).getTexture(0);
                if (tex != null) {
                    tex.setRotation(camrot);
                    tex.setTranslation(camtrans);
                }
            }
        }
    }

    /**
     * Returns a background that can be used to render this unit's status in
     * iconic form in the unit status user interface.
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
            public void render (Renderer renderer, int x, int y,
                                int width, int height, float alpha) {
                for (int ii = 0; ii < _icon.length; ii++) {
                    if (_icon[ii].getCullMode() != CULL_ALWAYS) {
                        _icon[ii].draw(renderer);
                    }
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
            _info[ii].getLocalTranslation().set(trans);
        }
    }

    /**
     * Recomposites if necessary our status texture and updates the texture
     * state.
     */
    public void update (Piece piece, boolean selected)
    {
        if (_owner != piece.owner) {
            // set up our starting outline color the first time we're updated
            _owner = piece.owner;
            ColorRGBA color = getColor(), dcolor = getDarkerColor();
            _info[0].getBatch(0).getDefaultColor().set(dcolor);
            _icon[0].getBatch(0).getDefaultColor().set(dcolor);
            for (int ii = 1; ii < recolorLayers(); ii++) {
                _info[ii].getBatch(0).getDefaultColor().set(color);
                _icon[ii].getBatch(0).getDefaultColor().set(color);
            }
            getTextureState(_info[0]).setTexture(_damout.createSimpleClone());
            getTextureState(_icon[0]).setTexture(_damout.createSimpleClone());
        }

        int dlevel = Math.max(0, (int)Math.floor(piece.damage/10f));
        if (_dlevel != dlevel) {
            _dlevel = dlevel;
            Texture dtex = _damtexs[dlevel];
            getTextureState(_info[1]).setTexture(dtex.createSimpleClone());
            getTextureState(_icon[1]).setTexture(dtex.createSimpleClone());
        }

        if (_selected != selected) {
            _selected = selected;
            ColorRGBA color = _selected ? ColorRGBA.white : getDarkerColor();
            _info[0].getBatch(0).getDefaultColor().set(color);
            _icon[0].getBatch(0).getDefaultColor().set(color);
        }
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
        if (_tempstate == null) {
            // we'll use this to load our textures into OpenGL as we go
            _tempstate = _ctx.getRenderer().createTextureState();

            // we generate ten discrete damage levels and pick the closest one
            // to represent a unit's damage (this is to avoid slow and
            // expensive BufferedImage rendering during the game)
            BufferedImage empty = _ctx.getImageCache().getBufferedImage(
                PPRE + "health_meter_empty.png");
            BufferedImage full = _ctx.getImageCache().getBufferedImage(
                PPRE + "health_meter_full.png");
            _damtexs = new Texture[11];
            _damtexs[0] = RenderUtil.createTexture(_ctx, ImageCache.createImage(full, false));
            _damtexs[10] = RenderUtil.createTexture(_ctx, ImageCache.createImage(empty, false));
            for (int ii = 1; ii < 10; ii++) {
                _damtexs[ii] = createDamageTexture(_ctx, empty, full, ii*10);
            }
            for (int ii = 0; ii < _damtexs.length; ii++) {
                prepare(_damtexs[ii]);
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

    protected Texture prepare (String path)
    {
        Texture tex = RenderUtil.createTexture(_ctx,
            _ctx.getImageCache().getImage(PPRE + path, false));
        return prepare(tex);
    }

    protected Texture prepare (Texture texture)
    {
        texture.setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
        _tempstate.setTexture(texture);
        RenderUtil.ensureLoaded(_tempstate);
        return texture;
    }

    protected final TextureState getTextureState (Spatial spatial)
    {
        return (TextureState)spatial.getRenderState(RenderState.RS_TEXTURE);
    }

    protected static Texture createDamageTexture (
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

    protected SharedMesh[] _info;
    protected Quad[] _icon;

    protected static TextureState _tempstate;
    protected static Texture[] _damtexs;
    protected static Texture _damout;

    /** Defines the amount by which the damage arc image is inset from a
     * full quarter circle (on each side): 8 degrees. */
    protected static final float ARC_INSETS = 7;

    /** The path prefix for all of our textures. */
    protected static final String PPRE = "textures/ustatus/";
}
