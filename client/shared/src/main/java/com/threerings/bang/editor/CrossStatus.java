//
// $Id$

package com.threerings.bang.editor;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;
import com.threerings.jme.util.ImageCache;

/**
 * A helper class that highlights which sides of a tile can be crossed.
 *
 * <p>jME3 cutover (Phase 2): the fork built one {@code com.jme.scene.SharedMesh} per direction
 * sharing the highlight's geometry, each carrying a fork {@code TextureState} (built through
 * {@code Renderer.createTextureState} and placed in {@code QUEUE_TRANSPARENT}). jME3 shares a
 * {@code Mesh} natively, so each direction is a {@link Geometry} over the highlight's shared mesh
 * with its own {@link Material} (an {@code Unshaded.j3md} ColorMap material, alpha-blended, in
 * {@link Bucket#Transparent}).
 *
 * <p>Editor-only; this is Phase-5 work and is reconciled then. It is migrated fork-import-free now
 * so the single client/shared compile unit builds.
 *
 * <p>TODO(render-core reconcile): codes against {@code RenderUtil.createTexture/ensureLoaded}
 * returning jME3 {@code com.jme3.texture.Texture} and {@code TerrainNode.Highlight} being a jME3
 * geometry that {@code new Geometry(name, highlight.getMesh())} can share; both are owned by other
 * clusters (render-core / board-renderer) and not yet landed. The per-direction material is built
 * inline because RenderUtil's shared-Material library is not yet defined.
 */
public class CrossStatus extends Node
    implements PieceCodes
{
    public TerrainNode.Highlight highlight;

    public CrossStatus (BasicContext ctx, TerrainNode.Highlight highlight)
    {
        super("cross_status");
        _ctx = ctx;

        loadTextures();
        this.highlight = highlight;

        _info = new Geometry[DIRECTIONS.length];
        _materials = new Material[DIRECTIONS.length];
        for (int ii = 0; ii < _info.length; ii++) {
            _info[ii] = new Geometry("info" + ii, highlight.getMesh());
            Material mat = new Material(ctx.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", _sidetexs[ii]);
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            _info[ii].setMaterial(mat);
            _info[ii].setQueueBucket(Bucket.Transparent);
            _materials[ii] = mat;
        }
    }

    /**
     * Recomposites the texture if necessary.
     */
    public boolean update (BangBoard board, int x, int y)
    {
        boolean update = false;
        if (!board.getPlayableArea().contains(x, y)) {
            return false;
        }
        for (int ii = 0; ii < _info.length; ii++) {
            if (board.canCross(x, y, x + DX[ii], y + DY[ii])) {
                if (_info[ii].getParent() != null) {
                    detachChild(_info[ii]);
                }
            } else {
                if (_info[ii].getParent() == null) {
                    attachChild(_info[ii]);
                }
                update = true;
            }
        }
        if (update) {
            highlight.updateVertices();
        }
        return update;
    }

    /**
     * Sets the default color.
     */
    public void setDefaultColor(ColorRGBA color)
    {
        for (int ii = 0; ii < _materials.length; ii++) {
            _materials[ii].setColor("Color", new ColorRGBA(color));
        }
    }

    /**
     * Loads up the textures used by the status display.
     */
    protected void loadTextures ()
    {
        if (_sidetexs == null) {
            // we flip the source image to generate the four different sides
            BufferedImage side = _ctx.getImageCache().getBufferedImage(
                    TEXTURE_PATH);
            _sidetexs = new Texture[DIRECTIONS.length];
            AffineTransform rot90 = AffineTransform.getRotateInstance(
                    Math.PI/2, side.getWidth()/2, side.getHeight()/2);
            AffineTransformOp rot90op = new AffineTransformOp(
                    rot90, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            for (int ii = 0; ii < DIRECTIONS.length; ii++) {
                _sidetexs[ii] = RenderUtil.createTexture(_ctx, ImageCache.createImage(side, false));
                RenderUtil.ensureLoaded(_ctx, _sidetexs[ii]);
                side = rot90op.filter(side, null);
            }
        }
    }

    protected BasicContext _ctx;
    protected Geometry[] _info;
    protected Material[] _materials;

    protected static Texture[] _sidetexs;

    /** The path to our texture. */
    protected static final String TEXTURE_PATH = "textures/editor/fence.png";
}
