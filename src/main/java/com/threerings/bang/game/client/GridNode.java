//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Rectangle;
import java.nio.FloatBuffer;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.scene.Line;
import com.jme.scene.VBOInfo;
import com.jme.scene.state.LightState;
import com.jme.util.geom.BufferUtils;

import com.threerings.bang.client.Config;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.BangBoard;

/**
 * Displays a grid along the tile boundaries.
 */
public class GridNode extends Line
{
    public GridNode (BasicContext ctx, BangBoard board, TerrainNode tnode, boolean editorMode)
    {
        super("grid");
        _ctx = ctx;
        _tnode = tnode;
        _board = board;

        setLightCombineMode(LightState.OFF);
        setRenderState(RenderUtil.blendAlpha);
        setRenderState(RenderUtil.overlayZBuf);
        updateRenderState();

        Rectangle parea = _board.getPlayableArea();
        int vertices = (parea.height + 1) *
            (parea.width * BangBoard.HEIGHTFIELD_SUBDIVISIONS) * 2 +
            (parea.width + 1) *
            (parea.height * BangBoard.HEIGHTFIELD_SUBDIVISIONS) * 2;
        setVertexBuffer(0, BufferUtils.createFloatBuffer(vertices * 3));
        generateIndices(0);
        updateVertices();

        setModelBound(new BoundingBox());
        updateModelBound();

        if (!editorMode) {
            if (Config.useVBOs && _ctx.getRenderer().supportsVBO()) {
                VBOInfo vboinfo = new VBOInfo(true);
                vboinfo.setVBOIndexEnabled(true);
                setVBOInfo(vboinfo);
            }
            lockBounds();
        }
    }

    /**
     * Updates the vertices of the grid (callde when the heightfield changes in
     * the editor).
     */
    public void updateVertices ()
    {
        // delete any loaded VBOs
        cleanup();

        Vector3f vertex = new Vector3f();
        FloatBuffer vbuf = getVertexBuffer(0);
        int idx = 0;
        int ppt = BangBoard.HEIGHTFIELD_SUBDIVISIONS;

        // horizontal grid lines
        Rectangle parea = _board.getPlayableArea();
        for (int ty = 0; ty <= parea.height; ty++) {
            int y = (ty + parea.y) * ppt;
            for (int ox = 0, width = parea.width * ppt; ox < width; ox++) {
                int x = parea.x * ppt + ox;
                _tnode.getHeightfieldVertex(x, y, vertex);
                vertex.z += 0.1f;
                BufferUtils.setInBuffer(vertex, vbuf, idx++);

                _tnode.getHeightfieldVertex(x + 1, y, vertex);
                vertex.z += 0.1f;
                BufferUtils.setInBuffer(vertex, vbuf, idx++);
            }
        }

        // vertical grid lines
        for (int tx = 0; tx <= parea.width; tx++) {
            int x = (tx + parea.x) * ppt;
            for (int oy = 0, height = parea.height * ppt; oy < height; oy++) {
                int y = parea.y * ppt + oy;
                _tnode.getHeightfieldVertex(x, y, vertex);
                vertex.z += 0.1f;
                BufferUtils.setInBuffer(vertex, vbuf, idx++);

                _tnode.getHeightfieldVertex(x, y + 1, vertex);
                vertex.z += 0.1f;
                BufferUtils.setInBuffer(vertex, vbuf, idx++);
            }
        }
    }

    /**
     * Releases the resources created by this node.
     */
    public void cleanup ()
    {
        VBOInfo vboinfo = getBatch(0).getVBOInfo();
        if (vboinfo != null) {
            RenderUtil.deleteVBOs(_ctx, vboinfo);
        }
    }

    protected BasicContext _ctx;
    protected BangBoard _board;
    protected TerrainNode _tnode;
}
