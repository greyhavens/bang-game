//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Rectangle;

import java.util.ArrayList;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jme.bounding.BoundingBox;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.scene.Line;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.TriMesh;
import com.jme.scene.lod.AreaClodMesh;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.util.geom.BufferUtils;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interator;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.Terrain;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the rendering of Bang's terrain and related elements.
 */
public class TerrainNode extends Node
{
    /**
     * Represents a circle draped over the terrain.
     */
    public class Cursor extends Line
    {
        /** The coordinates of the cursor in node space. */
        public float x, y, radius;
        
        protected Cursor ()
        {
            super("cursor");
            setMode(LOOP);
            setVertexBuffer(BufferUtils.createVector3Buffer(CURSOR_SEGMENTS));
            setLightCombineMode(LightState.OFF);
            updateRenderState();
        }
        
        /**
         * Sets the position of this cursor and updates it.
         */
        public void setPosition (float x, float y)
        {
            this.x = x;
            this.y = y;
            update();
        }
        
        /**
         * Sets the radius of this cursor and updates it.
         */
        public void setRadius (float radius)
        {
            this.radius = radius;
            update();
        }
        
        /**
         * Updates the geometry of the cursor to reflect a change in position
         * or in the underlying terrain.
         */
        public void update ()
        {
            if (_board == null) {
                return;
            }
            
            FloatBuffer vbuf = getVertexBuffer();
            Vector3f vector = new Vector3f();
            
            float step = FastMath.TWO_PI / CURSOR_SEGMENTS;
            for (int i = 0; i < CURSOR_SEGMENTS; i++) {
                float angle = i * step,
                    vx = x + radius*FastMath.cos(angle),
                    vy = y + radius*FastMath.sin(angle);
                vector.set(vx, vy, getHeightfieldHeight(vx, vy));
                BufferUtils.setInBuffer(vector, vbuf, i);
            }
        }
    }
    
    public TerrainNode (BasicContext ctx)
    {
        super("terrain");
        _ctx = ctx;
        
        // always perform backface culling
        CullState cs = _ctx.getDisplay().getRenderer().createCullState();
        cs.setCullMode(CullState.CS_BACK);
        setRenderState(cs);
    }
    
    /**
     * Initializes the terrain geometry using terrain data from the given board
     * and saves the board reference for later updates.
     */
    public void createBoardTerrain (BangBoard board)
    {
        _board = board;
        
        // clean up any existing geometry
        detachAllChildren();
        
        // create, store, and attach the splat blocks
        int swidth = (int)Math.ceil((_board.getHeightfieldWidth() + 1.0) /
                SPLAT_SIZE),
            sheight = (int)Math.ceil((_board.getHeightfieldHeight() + 1.0) /
                SPLAT_SIZE);
        _blocks = new SplatBlock[swidth][sheight];
        for (int x = 0; x < swidth; x++) {
            for (int y = 0; y < sheight; y++) {
                _blocks[x][y] = createSplatBlock(x, y);
                attachChild(_blocks[x][y].node);
            }
        }
        
        setWorldBound(new BoundingBox());
        updateWorldBound();
        
        updateRenderState();
        updateGeometricState(0, true);
    }
    
    /**
     * Refreshes the entire heightfield.
     */
    public void refreshHeightfield ()
    {
        refreshHeightfield(0, 0, _board.getHeightfieldWidth() + 2,
            _board.getHeightfieldHeight() + 2);
    }
    
    /**
     * Refreshes a region of the heightfield as specified in sub-tile
     * coordinates.  Remember that changing any height point affects the
     * normals of its neighbors, so be sure to include them in the region.
     */
    public void refreshHeightfield (int rx, int ry, int rwidth, int rheight)
    {
        Rectangle rect = new Rectangle(rx, ry, rwidth, rheight);
        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                SplatBlock block = _blocks[x][y];
                Rectangle isect = rect.intersection(block.bounds);
                if (!isect.isEmpty()) {
                    block.refreshGeometry(isect);
                }
            }
        }
    }
    
    /**
     * Refreshes all terrain splats.
     */
    public void refreshTerrain ()
    {
        refreshTerrain(0, 0, _board.getHeightfieldWidth() + 2,
            _board.getHeightfieldHeight() + 2);
    }
    
    /**
     * Refreshes the terrain splats over the specified region in sub-tile
     * coordinates.
     */
    public void refreshTerrain (int rx, int ry, int rwidth, int rheight)
    {
        Rectangle rect = new Rectangle(rx, ry, rwidth, rheight);
        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                SplatBlock block = _blocks[x][y];
                Rectangle isect = rect.intersection(block.bounds);
                if (!isect.isEmpty()) {
                    block.refreshSplats(isect);
                }
            }
        }
    }
    
    /**
     * Creates and returns a cursor over this terrain.  The cursor must be
     * added to the scene graph before it becomes visible.
     */
    public Cursor createCursor ()
    {
        return new Cursor();
    }
    
    /**
     * Creates and returns a splat block for the specified splat coordinates.
     */
    protected SplatBlock createSplatBlock (int sx, int sy)
    {
        // create the block and containing node
        SplatBlock block = new SplatBlock();
        block.node = new Node("block_" + sx + "_" + sy);
        
        // compute the dimensions in terms of vertices and create buffers for
        // the vertices and normals
        int vx = sx * SPLAT_SIZE, vy = sy * SPLAT_SIZE,
            vwidth = Math.min(SPLAT_SIZE + 1,
                _board.getHeightfieldWidth() + 2 - vx),
            vheight = Math.min(SPLAT_SIZE + 1,
                _board.getHeightfieldHeight() + 2 - vy),
            vbufsize = vwidth * vheight * 3;
        block.vbuf = BufferUtils.createFloatBuffer(vbufsize);
        block.nbuf = BufferUtils.createFloatBuffer(vbufsize);
        
        // refresh sets the vertices and normals from the heightfield
        block.bounds = new Rectangle(vx, vy, vwidth, vheight);
        block.refreshGeometry(block.bounds);
        
        // set the texture coordinates
        FloatBuffer tbuf0 = BufferUtils.createFloatBuffer(vwidth*vheight*2),
            tbuf1 = BufferUtils.createFloatBuffer(vwidth*vheight*2);
        float step0 = 1.0f / BangBoard.HEIGHTFIELD_SUBDIVISIONS,
            step1 = 1.0f / (SPLAT_SIZE+1);
        for (int y = 0; y < vheight; y++) {
            for (int x = 0; x < vwidth; x++) {
                tbuf0.put(x * step0);
                tbuf0.put(y * step0);
                
                tbuf1.put(0.5f*step1 + x * step1);
                tbuf1.put(0.5f*step1 + y * step1);
            }
        }
        
        // compute the dimensions in terms of squares and set the triangle
        // indices
        int swidth = vwidth - 1, sheight = vheight - 1;
        IntBuffer ibuf = BufferUtils.createIntBuffer(swidth * sheight * 2 * 3);
        for (int y = 0; y < sheight; y++) {
            for (int x = 0; x < swidth; x++) {
                int ll = y*vwidth + x, lr = ll + 1,
                    ul = ll + vwidth, ur = ul + 1;
                
                // two triangles for each square: one including the upper left
                // vertex, one the lower right, ccw winding order
                ibuf.put(ll); ibuf.put(ur); ibuf.put(ul);
                ibuf.put(ll); ibuf.put(lr); ibuf.put(ur);
            }
        }
        
        // depending on whether we can assume the heightfield to remain static,
        // either create a trimesh or a clod mesh with the computed values
        if (isHeightfieldStatic()) {
            block.mesh = new AreaClodMesh("terrain", block.vbuf, block.nbuf, null,
                tbuf0, ibuf, null);
            
        } else {
            block.mesh = new TriMesh("terrain", block.vbuf, block.nbuf, null,
                tbuf0, ibuf);
        }
        block.mesh.setTextureBuffer(tbuf1, 1);
        block.mesh.setModelBound(new BoundingBox());
        block.mesh.updateModelBound();
            
        // initialize the splats
        block.refreshSplats(block.bounds);
        
        return block;  
    }
    
    /**
     * Checks whether the heightfield can be assumed to be static (and thus a
     * candidate for rendering optimization).  Default implementation returns
     * <code>false</code>.
     */
    protected boolean isHeightfieldStatic ()
    {
        return false;
    }
    
    /**
     * Computes the heightfield vertex at the specified location in sub-tile
     * coordinates.
     *
     * @param result a vector to hold the result
     */
    protected void getHeightfieldVertex (int x, int y, Vector3f result)
    {
        float step = TILE_SIZE / BangBoard.HEIGHTFIELD_SUBDIVISIONS;
        result.set(x * step, y * step, getHeightfieldValue(x, y));
    }
    
    /**
     * Computes the normal at the specified heightfield location in sub-tile
     * coordinates.
     *
     * @param result a vector to hold the result
     */
    protected void getHeightfieldNormal (int x, int y, Vector3f result)
    {
        float step = TILE_SIZE / BangBoard.HEIGHTFIELD_SUBDIVISIONS;
        
        // the normal is determined by taking the cross product of the vector
        // from the left height point to the right height point...
        boolean le = (x == 0), re = (x == _board.getHeightfieldWidth()+1);
        Vector3f vx = new Vector3f((le || re) ? step : step*2, 0,
            getHeightfieldValue(re ? x : x+1, y) -
                getHeightfieldValue(le ? x : x-1, y));
        
        // ...and the vector from the bottom height point to the top height
        // point, substituting this height point in the edge cases
        boolean be = (y == 0), te = (y == _board.getHeightfieldHeight()+1);
        Vector3f vy = new Vector3f(0, (te || be) ? step : step*2,
            getHeightfieldValue(x, te ? y : y+1) -
                getHeightfieldValue(x, be ? y : y-1));
              
        vx.cross(vy, result);
        result.normalizeLocal();
    }
 
    /**
     * Returns the scaled height of the specified location in sub-tile
     * coordinates.
     */
    protected float getHeightfieldValue (int x, int y)
    {
        // return zero for vertices at the edge or beyond
        if (x <= 0 || y <= 0 || x > _board.getHeightfieldWidth() ||
            y > _board.getHeightfieldHeight()) {
            return 0.0f;
        
        // otherwise, subtract one for the rim and look up in the heightfield
        } else {
            return _board.getHeightfieldValue(x-1, y-1) *
                (TILE_SIZE / BangBoard.ELEVATION_UNITS_PER_TILE);
        }
    }
    
    /**
     * Returns the interpolated height at the specified set of node space
     * coordinates.
     */
    protected float getHeightfieldHeight (float x, float y)
    {
        // scale down to sub-tile coordinates
        float stscale = BangBoard.HEIGHTFIELD_SUBDIVISIONS / TILE_SIZE;
        x *= stscale;
        y *= stscale;
        
        // sample at the four closest points and find the fractional components
        int fx = (int)FastMath.floor(x), cx = (int)FastMath.ceil(x),
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y);
        float ff = getHeightfieldValue(fx, fy),
            fc = getHeightfieldValue(fx, cy),
            cf = getHeightfieldValue(cx, fy),
            cc = getHeightfieldValue(cx, cy),
            ax = x - fx, ay = y - fy;
        
        // if the fractional component of y is greater than that of x, we're in
        // the upper left triangle; otherwise, we're in the lower right
        if (ay < ax) {
            return FastMath.LERP(ax, FastMath.LERP(ay, ff, fc), cc);
            
        } else {
            return FastMath.LERP(ax, ff, FastMath.LERP(ay, cf, cc));
        }
    }
    
    /**
     * Computes and returns the alpha value for the specified terrain code at
     * the given sub-tile coordinates.
     */
    protected float getTerrainAlpha (int code, float x, float y)
    {
         int fx = (int)FastMath.floor(x), cx = (int)FastMath.ceil(x),
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y);
        float ff = getTerrainCode(fx, fy) == code ? 1.0f : 0.0f,
            fc = getTerrainCode(fx, cy) == code ? 1.0f : 0.0f,
            cf = getTerrainCode(cx, fy) == code ? 1.0f : 0.0f,
            cc = getTerrainCode(cx, cy) == code ? 1.0f : 0.0f,
            ax = x - fx, ay = y - fy;
            
        return FastMath.LERP(ax, FastMath.LERP(ay, ff, fc), FastMath.LERP(ay, cf, cc));
    }
    
    /**
     * Returns the terrain code at the specified location in sub-tile
     * coordinates.
     */
    protected int getTerrainCode (int x, int y)
    {
        // return zero for vertices beyond the edge
        if (x < 0 || y < 0 || x >= _board.getTerrainWidth() ||
            y >= _board.getTerrainHeight()) {
            return 0;
        
        // otherwise, look it up in the terrain map
        } else {
            return _board.getTerrainValue(x, y);
        }
    }

    /**
     * Clamps the given value between the specified beginning and end points.
     */
    protected static float clamp (float v, float a, float b)
    {
        return Math.min(Math.max(v, a), b);
    }
    
    /** Contains all the state associated with a splat block (a collection of
     * splats covering a single block of terrain). */
    protected class SplatBlock
    {
        /** The node containing the {@link SharedMesh} splats. */
        public Node node;
        
        /** The bounds of this block in sub-tile coordinates. */
        public Rectangle bounds;
        
        /** The shared, unparented mesh instance. */
        public TriMesh mesh;
        
        /** The vertex and normal buffers. */
        public FloatBuffer vbuf, nbuf;
        
        /** Maps terrain codes to ground texture states. */
        public HashIntMap groundTextures = new HashIntMap();
        
        /** Maps terrain codes to alpha texture buffers. */
        public HashIntMap alphaBuffers = new HashIntMap();
        
        /**
         * Refreshes the geometry covered by the specified rectangle (in
         * sub-tile coordinates).
         */
        public void refreshGeometry (Rectangle rect)
        {
            Vector3f vector = new Vector3f();
            
            for (int y = rect.y, ymax = y + rect.height; y < ymax; y++) {
                for (int x = rect.x, xmax = x + rect.width; x < xmax; x++) {
                    int index = (y-bounds.y)*bounds.width + (x-bounds.x);
                
                    getHeightfieldVertex(x, y, vector);
                    BufferUtils.setInBuffer(vector, vbuf, index);
                
                    getHeightfieldNormal(x, y, vector);
                    BufferUtils.setInBuffer(vector, nbuf, index);
                }
            }
        }
        
        /**
         * Refreshes the splats according to terrain changes over the
         * specified rectangle (in sub-tile coordinates).
         */
        public void refreshSplats (Rectangle rect)
        {
            // remove all the existing children
            node.detachAllChildren();
            
            // find out which terrain codes this block contains and which is
            // the most common
            IntIntMap counts = new IntIntMap();
            int baseCode = 0, baseCount = 0;
            for (int y = bounds.y, ymax = y+bounds.height; y < ymax; y++) {
                for (int x = bounds.x, xmax = x+bounds.width; x < xmax; x++) {
                    int code = getTerrainCode(x, y);
                    counts.increment(code, 1);
                    int count = counts.get(code);
                    if (count > baseCount) {
                        baseCode = code;
                        baseCount = count;
                    }
                }
            }

            // use the most common terrain for the base mesh (which both tests
            // and writes to the z buffer)
            SharedMesh base = new SharedMesh("base", mesh);
            base.setRenderState(getGroundTexture(baseCode));
            ZBufferState zbstate =
                _ctx.getDisplay().getRenderer().createZBufferState();
            zbstate.setWritable(true);
            zbstate.setFunction(ZBufferState.CF_LEQUAL);
            base.setRenderState(zbstate);
            node.attachChild(base);
            
            // add the rest as splats (which only test the z buffer)
            counts.remove(baseCode);
            int[] codes = counts.getKeys();
            for (int i = 0; i < codes.length; i++) {
                SharedMesh splat = new SharedMesh("splat" + i, mesh);
                
                // initialize the texture state
                TextureState tstate =
                    _ctx.getDisplay().getRenderer().createTextureState();
                Texture ground = getGroundTexture(codes[i]).getTexture().
                    createSimpleClone();
                ground.setApply(Texture.AM_COMBINE);
                ground.setCombineFuncAlpha(Texture.ACF_REPLACE);
                ground.setCombineSrc0Alpha(Texture.ACS_TEXTURE);
                ground.setCombineFuncRGB(Texture.ACF_MODULATE);
                ground.setCombineSrc0RGB(Texture.ACS_TEXTURE);
                ground.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
                tstate.setTexture(ground, 0);
                tstate.setTexture(createAlphaTexture(codes[i], rect), 1);
                splat.setRenderState(tstate);
                
                // and the z buffer state
                zbstate = _ctx.getDisplay().getRenderer().createZBufferState();
                zbstate.setWritable(false);
                zbstate.setFunction(ZBufferState.CF_LEQUAL);
                splat.setRenderState(zbstate);
                
                // and the alpha state
                AlphaState astate =
                    _ctx.getDisplay().getRenderer().createAlphaState();
                astate.setBlendEnabled(true);
                astate.setSrcFunction(AlphaState.SB_SRC_ALPHA);
                astate.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
                splat.setRenderState(astate);
                
                node.attachChild(splat);
            }
            
            // prune any unused alpha buffers from the map
            for (Interator it = alphaBuffers.keys(); it.hasNext(); ) {
                if (!IntListUtil.contains(codes, it.nextInt())) {
                    it.remove();
                }
            }
            
            node.updateRenderState();
        }
        
        /**
         * Returns the ground texture state for the given terrain code, making
         * sure that we always pick the same "random" texture for this splat.
         */
        protected TextureState getGroundTexture (int code)
        {
            TextureState tstate = (TextureState)groundTextures.get(code);
            if (tstate == null) {
                groundTextures.put(code, tstate = RenderUtil.getGroundTexture(
                    Terrain.fromCode(code)));
            }
            return tstate;
        }
        
        /**
         * Creates and returns an alpha texture for the specified terrain
         * code, using preexisting buffers when possible.
         *
         * @param rect the modified region
         */
        protected Texture createAlphaTexture (int code, Rectangle rect)
        {
            // create the buffer if it doesn't already exist
            ByteBuffer abuf = (ByteBuffer)alphaBuffers.get(code);
            if (abuf == null) {
                alphaBuffers.put(code, abuf = ByteBuffer.allocateDirect(
                    TEXTURE_SIZE*TEXTURE_SIZE*4));
                rect = bounds;
            }
            
            // update the affected region of the buffer
            float step = (SPLAT_SIZE + 1.0f) / TEXTURE_SIZE;
            int x1 = (int)((rect.x - bounds.x) / step),
                y1 = (int)((rect.y - bounds.y) / step),
                x2 = (int)FastMath.ceil((rect.x + rect.width - 1 - bounds.x) /
                    step),
                y2 = (int)FastMath.ceil((rect.y + rect.height - 1 - bounds.y) /
                    step);
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    int idx = (y*TEXTURE_SIZE + x)*4;
                    
                    byte alpha = (byte)(getTerrainAlpha(code,
                        bounds.x + x * step, bounds.y + y * step)*255);
                    abuf.putInt(idx, (alpha << 24) | (alpha << 16) |
                        (alpha << 8) | alpha);
                }
            }
            
            Texture texture = new Texture();
            texture.setImage(new Image(Image.RGBA8888, TEXTURE_SIZE,
                TEXTURE_SIZE, abuf));
            
            // set the filter parameters
            texture.setFilter(Texture.FM_LINEAR);
            
            // and the combination parameters
            texture.setApply(Texture.AM_COMBINE);
            texture.setCombineFuncAlpha(Texture.ACF_REPLACE);
            texture.setCombineSrc0Alpha(Texture.ACS_TEXTURE);
            texture.setCombineFuncRGB(Texture.ACF_REPLACE);
            texture.setCombineSrc0RGB(Texture.ACS_PREVIOUS);
                
            return texture;
        }
    }
    
    /** The application context. */
    protected BasicContext _ctx;
    
    /** The board with the terrain. */
    protected BangBoard _board;
    
    /** The array of splat blocks containing the terrain geometry/textures. */
    protected SplatBlock[][] _blocks;
    
    /** The size of the terrain splats in sub-tiles. */
    protected static final int SPLAT_SIZE = 32;
    
    /** The size of the splat alpha textures. */
    protected static final int TEXTURE_SIZE = SPLAT_SIZE * 2;
    
    /** The number of segments in the cursor. */
    protected static final int CURSOR_SEGMENTS = 64;
}
