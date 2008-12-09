//
// $Id$

package com.threerings.bang.editor;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import com.jme.image.Texture;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;
import com.threerings.jme.util.ImageCache;

/**
 * A helper class that highlights which sides of a tile can be crossed.
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

        _info = new SharedMesh[DIRECTIONS.length];
        for (int ii = 0; ii < _info.length; ii++) {
            _info[ii] = new SharedMesh("info" + ii, highlight);
            _info[ii].setIsCollidable(false);
            _info[ii].setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            _info[ii].setRenderState(ctx.getRenderer().createTextureState());
            _info[ii].updateRenderState();
            getTextureState(_info[ii]).setTexture(
                    _sidetexs[ii].createSimpleClone());
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
        for (int ii = 0; ii < _info.length; ii++) {
            _info[ii].setDefaultColor(color);
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

    protected final TextureState getTextureState (Spatial spatial)
    {
        return (TextureState)spatial.getRenderState(RenderState.RS_TEXTURE);
    }

    protected BasicContext _ctx;
    protected SharedMesh[] _info;

    protected static Texture[] _sidetexs;

    /** The path to our texture. */
    protected static final String TEXTURE_PATH = "textures/editor/fence.png";
}
