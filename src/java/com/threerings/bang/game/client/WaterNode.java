//
// $Id$

package com.threerings.bang.game.client;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jme.bounding.BoundingBox;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Line;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.TriMesh;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.ZBufferState;
import com.jme.util.geom.BufferUtils;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the rendering of Bang's wet spots.
 */
public class WaterNode extends Node
{
    public WaterNode (BasicContext ctx)
    {
        super("water");
        _ctx = ctx;
        
        setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        setRenderState(RenderUtil.blendAlpha);
        setRenderState(RenderUtil.backCull);
        setRenderState(RenderUtil.lequalZBuf);
        MaterialState mstate = _ctx.getRenderer().createMaterialState();
        mstate.setAmbient(new ColorRGBA(0f, 0.1f, 0.2f, 0.75f));
        mstate.setDiffuse(new ColorRGBA(0f, 0.2f, 0.3f, 0.5f));
        setRenderState(mstate);
        updateRenderState();
    }
    
    /**
     * Initializes the water geometry using terrain data from the given board
     * and saves the board reference for later updates.
     */
    public void createBoardWater (BangBoard board)
    {
        _board = board;
        
        // clean up any existing geometry
        detachAllChildren();
        
        // initialize the array of blocks
        _blocks = new SharedMesh[_board.getWidth()][_board.getHeight()];
        setWorldBound(new BoundingBox());
        refreshSurface();
    }
    
    /**
     * Updates the entire visible set of surface blocks.
     */
    public void refreshSurface ()
    {
        refreshSurface(0, 0, _board.getWidth() - 1, _board.getHeight() - 1);
    }
    
    /**
     * Updates the visibile set of surface blocks within the specified tile
     * coordinate rectangle based on the state of the board terrain and
     * water level.
     */
    public void refreshSurface (int x1, int y1, int x2, int y2)
    {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                if (_board.isUnderWater(x, y)) {
                    if (_blocks[x][y] == null) {
                        _blocks[x][y] = new SharedMesh("block", _tile);
                        _blocks[x][y].setLocalTranslation(
                            new Vector3f(x * TILE_SIZE, y * TILE_SIZE, 0f));
                    }
                    if (_blocks[x][y].getParent() == null) {
                        attachChild(_blocks[x][y]);
                        _bcount++;
                    }
                    
                } else if (_blocks[x][y] != null &&
                    _blocks[x][y].getParent() != null) {
                    detachChild(_blocks[x][y]);
                    _bcount--;
                }
            }
        }
        
        updateWorldBound();
        
        updateRenderState();
        updateGeometricState(0, true);
    }
    
    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);
        if (_blocks == null) {
            return;
        }
        
        // adjust the phase based on time elapsed
        _phase += time * WAVE_SPEED;
        if (_phase > WAVE_LENGTH) {
            _phase -= WAVE_LENGTH;
        }
        
        // update the vertices of the tile if there are any blocks
        // showing
        if (_bcount > 0) {
            _tile.updateVertices();
        }
    }
    
    /** Represents a tilable, tile-sized block of water. */
    protected class SurfaceTile extends TriMesh
    {
        public SurfaceTile ()
        {
            super("surface");
            
            // set the vertices and normals, which change over time
            int size = SURFACE_SUBDIVISIONS + 1, bsize = size * size * 3;
            setVertexBuffer(BufferUtils.createFloatBuffer(bsize));
            setNormalBuffer(BufferUtils.createFloatBuffer(bsize));
            updateVertices();
            
            // set the indices and ranges, which never change
            IntBuffer ibuf = BufferUtils.createIntBuffer(SURFACE_SUBDIVISIONS *
                SURFACE_SUBDIVISIONS * 2 * 3);
            for (int iy = 0; iy < SURFACE_SUBDIVISIONS; iy++) {
                for (int ix = 0; ix < SURFACE_SUBDIVISIONS; ix++) {
                    // upper left triangle
                    ibuf.put(iy*size + ix);
                    ibuf.put((iy+1)*size + (ix+1));
                    ibuf.put((iy+1)*size + ix);
                    
                    // lower right triangle
                    ibuf.put(iy*size + ix);
                    ibuf.put(iy*size + (ix+1));
                    ibuf.put((iy+1)*size + (ix+1));
                }
            }
            setIndexBuffer(ibuf);
            
            setModelBound(new BoundingBox());
            updateModelBound();
        }
        
        /**
         * Updates the vertices of the block.
         */
        public void updateVertices ()
        {
            if (_board == null) {
                return;
            }
            
            FloatBuffer vbuf = getVertexBuffer(), nbuf = getNormalBuffer();
            
            float step = TILE_SIZE / SURFACE_SUBDIVISIONS,
                waterline = (_board.getWaterLevel() - 1) *
                    (TILE_SIZE / BangBoard.ELEVATION_UNITS_PER_TILE),
                a = FastMath.TWO_PI / WAVE_LENGTH, p1 = _phase,
                p2 = _phase + PHASE_DIFFERENCE;
            Vector3f vertex = new Vector3f(), normal = new Vector3f();
            for (int i = 0, idx = 0; i <= SURFACE_SUBDIVISIONS; i++) {
                for (int j = 0; j <= SURFACE_SUBDIVISIONS; j++) {
                    vertex.x = j * step;
                    vertex.y = i * step;
                    float wx = a*(vertex.x + p1), wy = a*(vertex.y + p2);
                    vertex.z = waterline + WAVE_AMPLITUDE *
                        (FastMath.sin(wx) + FastMath.sin(wy));
                    BufferUtils.setInBuffer(vertex, vbuf, idx);
                    
                    normal.set(-WAVE_AMPLITUDE * a * FastMath.cos(wx),
                        -WAVE_AMPLITUDE * a * FastMath.cos(wy), 1f);
                    normal.normalizeLocal();
                    BufferUtils.setInBuffer(normal, nbuf, idx++);
                }
            }
            
            updateModelBound();
        }
    }
    
    /** The application context. */
    protected BasicContext _ctx;
    
    /** The board with the terrain information. */
    protected BangBoard _board;
    
    /** The tile geometry shared between the blocks. */
    protected SurfaceTile _tile = new SurfaceTile();
    
    /** The array of tiled surface blocks for the tile locations. */
    protected SharedMesh[][] _blocks;
    
    /** The number of currently active blocks. */
    protected int _bcount;
    
    /** The current wave phase in node units. */
    protected float _phase;
    
    /** The number of subdivisions in the tile-sized surface blocks. */
    protected static final int SURFACE_SUBDIVISIONS = 8;
    
    /** The amplitude of the waves in node units. */
    protected static final float WAVE_AMPLITUDE =
        (TILE_SIZE / BangBoard.ELEVATION_UNITS_PER_TILE) * 0.5f;
    
    /** The length of the waves in node units. */
    protected static final float WAVE_LENGTH = TILE_SIZE * 0.5f;
    
    /** The speed of the waves in node units per second. */
    protected static final float WAVE_SPEED = TILE_SIZE * 0.25f;
    
    /** The phase difference between x and y. */
    protected static final float PHASE_DIFFERENCE = WAVE_LENGTH * 0.123142f;
}
