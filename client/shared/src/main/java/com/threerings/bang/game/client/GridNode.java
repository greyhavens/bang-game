//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Rectangle;
import java.nio.FloatBuffer;

import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.data.BangBoard;

/**
 * Displays a grid along the tile boundaries.
 *
 * <p>jME3 cutover (Phase 2): the fork {@code com.jme.scene.Line} (a multi-segment line geometry
 * with fixed-function {@code LightState.OFF} + {@code AlphaState} + {@code ZBufferState} render
 * states, plus manual {@code VBOInfo} management) becomes a jME3 {@link Geometry} over a
 * {@link Mesh} in {@link Mesh.Mode#Lines}. jME3 manages VBOs automatically, so the
 * {@code VBOInfo}/{@code lockBounds}/{@code supportsVBO} machinery is dropped. Lighting is off
 * by virtue of the {@code Unshaded} material; the overlay blend/no-depth-write behaviour moves to
 * the material's {@link com.jme3.material.RenderState}.
 *
 * <p>The grid colour was previously set by {@code BoardView} via
 * {@code grid.getBatch(0).getDefaultColor()}; that call site now uses {@link #setColor}.
 *
 * <p>TODO(render-core reconcile): the material is built inline here against {@code Unshaded.j3md}
 * because RenderUtil's shared-Material library (the documented blend/overlay-depth presets) is not
 * yet landed; reconcile with {@code RenderUtil} once its Material API exists.
 */
public class GridNode extends Geometry
{
    public GridNode (BasicContext ctx, BangBoard board, TerrainNode tnode, boolean editorMode)
    {
        super("grid");
        _ctx = ctx;
        _tnode = tnode;
        _board = board;

        Rectangle parea = _board.getPlayableArea();
        int vertices = (parea.height + 1) *
            (parea.width * BangBoard.HEIGHTFIELD_SUBDIVISIONS) * 2 +
            (parea.width + 1) *
            (parea.height * BangBoard.HEIGHTFIELD_SUBDIVISIONS) * 2;

        _mesh = new Mesh();
        _mesh.setMode(Mesh.Mode.Lines);
        _mesh.setBuffer(VertexBuffer.Type.Position, 3,
            BufferUtils.createFloatBuffer(vertices * 3));
        setMesh(_mesh);

        _material = new Material(ctx.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        _material.setColor("Color", new ColorRGBA(ColorRGBA.White));
        _material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        // overlay z-buffer: test but do not write depth (fork RenderUtil.overlayZBuf)
        _material.getAdditionalRenderState().setDepthWrite(false);
        setMaterial(_material);
        setQueueBucket(Bucket.Transparent);

        updateVertices();

        setModelBound(new BoundingBox());
        updateModelBound();
    }

    /**
     * Sets the colour of the grid lines (replaces the fork
     * {@code getBatch(0).getDefaultColor()} access used by {@code BoardView}).
     */
    public void setColor (ColorRGBA color)
    {
        _material.setColor("Color", new ColorRGBA(color));
    }

    /**
     * Updates the vertices of the grid (called when the heightfield changes in
     * the editor).
     */
    public void updateVertices ()
    {
        Vector3f vertex = new Vector3f();
        FloatBuffer vbuf = _mesh.getFloatBuffer(VertexBuffer.Type.Position);
        vbuf.rewind();
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

        _mesh.getBuffer(VertexBuffer.Type.Position).updateData(vbuf);
        _mesh.updateBound();
    }

    /**
     * Releases the resources created by this node. (jME3 manages GPU buffers
     * automatically, so this is now a no-op retained for the call-site API.)
     */
    public void cleanup ()
    {
        // no-op: jME3 owns VBO lifecycle
    }

    protected BasicContext _ctx;
    protected BangBoard _board;
    protected TerrainNode _tnode;
    protected Mesh _mesh;
    protected Material _material;
}
