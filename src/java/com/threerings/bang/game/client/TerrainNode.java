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
import com.jme.renderer.ColorRGBA;
import com.jme.scene.CompositeMesh;
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
            
            setDefaultColor(ColorRGBA.white);
            setLightCombineMode(LightState.OFF);
            setRenderState(RenderUtil.lequalZBuf);
            
            updateRenderState();
            update();
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
            ArrayList<Vector3f> verts = new ArrayList<Vector3f>();
            if (_board != null) {
                float step = FastMath.TWO_PI / CURSOR_SEGMENTS, angle = 0.0f;
                for (int i = 0; i < CURSOR_SEGMENTS; i++) {
                    addSegment(verts,
                        new Vector2f(x + radius*FastMath.cos(angle),
                            y + radius*FastMath.sin(angle)),
                        new Vector2f(x + radius*FastMath.cos(angle+step),
                            y + radius*FastMath.sin(angle+step)));
                    angle += step;
                }
            }
            setVertexBuffer(BufferUtils.createFloatBuffer(
                verts.toArray(new Vector3f[verts.size()])));
            generateIndices();
        }
        
        /**
         * Adds a segment to the line, breaking it up into intermediate
         * segments if it crosses edge boundaries.
         */
        protected void addSegment (ArrayList<Vector3f> verts, Vector2f v1,
            Vector2f v2)
        {
            Vector2f between = getHorizontalIntermediate(v1, v2);
            if (between == null) {
                between = getVerticalIntermediate(v1, v2);
                if (between == null) {
                    between = getDiagonalIntermediate(v1, v2);
                }
            }
            
            if (between == null) {
                verts.add(new Vector3f(v1.x, v1.y, getHeightfieldHeight(v1.x,
                    v1.y) + 1.0f));   
                verts.add(new Vector3f(v2.x, v2.y, getHeightfieldHeight(v2.x,
                    v2.y) + 1.0f));
                    
            } else {
                addSegment(verts, v1, between);
                addSegment(verts, between, v2);
            }
        }
        
        /**
         * If the two vertices lie on either side of a horizontal boundary,
         * return the point on the boundary between them (otherwise null).
         */
        protected Vector2f getHorizontalIntermediate (Vector2f v1, Vector2f v2)
        {
            int b1 = getBoundaryIndex(v1.y, SUB_TILE_SIZE),
                b2 = getBoundaryIndex(v2.y, SUB_TILE_SIZE),
                bmin = Math.min(b1, b2), bmax = Math.max(b1, b2);
            for (int b = bmin+1; b < bmax; b++) {
                if (b % 2 != 0) {
                    continue;
                }
                float y = (b/2)*SUB_TILE_SIZE, t = (y - v1.y) / (v2.y - v1.y);
                return new Vector2f(v1.x + t*(v2.x - v1.x), y);
            }
            return null;
        }
        
        /**
         * If the two vertices lie on either side of a vertical boundary,
         * return the point on the boundary between them (otherwise null).
         */
        protected Vector2f getVerticalIntermediate (Vector2f v1, Vector2f v2)
        {
            int b1 = getBoundaryIndex(v1.x, SUB_TILE_SIZE),
                b2 = getBoundaryIndex(v2.x, SUB_TILE_SIZE),
                bmin = Math.min(b1, b2), bmax = Math.max(b1, b2);
            for (int b = bmin+1; b < bmax; b++) {
                if (b % 2 != 0) {
                    continue;
                }
                float x = (b/2)*SUB_TILE_SIZE, t = (x - v1.x) / (v2.x - v1.x);
                return new Vector2f(x, v1.y + t*(v2.y - v1.y));
            }
            return null;
        }
        
        /**
         * If the two vertices lie on either side of a diagonal boundary,
         * return the point on the boundary between them (otherwise null).
         */
        protected Vector2f getDiagonalIntermediate (Vector2f v1, Vector2f v2)
        {
            float r2 = FastMath.sqrt(2.0f), d1 = (v1.y - v1.x)/(2 * r2),
                d2 = (v2.y - v2.x)/(2 * r2), step = SUB_TILE_SIZE * r2;
            
            int b1 = getBoundaryIndex(d1, step),
                b2 = getBoundaryIndex(d2, step),
                bmin = Math.min(b1, b2), bmax = Math.max(b1, b2);
            for (int b = bmin+1; b < bmax; b++) {
                if (b % 2 != 0) {
                    continue;
                }
                float d = (b/2)*step, t = (d - d1) / (d2 - d1);
                return new Vector2f(v1.x + t*(v2.x - v1.x),
                    v1.y + t*(v2.y - v1.y));
            }
            return null;
        }
        
        /**
         * Returns a boundary index for the specified value.  Starting at zero,
         * every other index represents a boundary between two regions.  The
         * other indices represent regions between the boundaries.
         *
         * @param step the size of the regions between boundaries
         */
        protected int getBoundaryIndex (float v, float step)
        {
            int base = (int)Math.floor(v / step), adjust;
            if (epsilonEquals(v, base*step)) {
                adjust = 0; // lower boundary
            
            } else if (epsilonEquals(v, (base+1)*step)) {
                adjust = 2; // upper boundary
            
            } else {
                adjust = 1; // region between
            }
            return base*2 + adjust;
        }
        
        /**
         * Checks whether the two values are "close enough" to equal.
         */
        protected boolean epsilonEquals (float a, float b)
        {
            return FastMath.abs(a - b) < 0.001f;
        }
    }
    
    /** 
     * Represents a highlight draped over the terrain underneath a tile.
     */
    public class Highlight extends CompositeMesh
    {
        /** The tile coordinate to highlight. */
        public int x, y;
        
        protected Highlight (int x, int y, boolean overPieces)
        {
            super("highlight");
            this.x = x;
            this.y = y;
            _overPieces = overPieces;
            
            setLightCombineMode(LightState.OFF);
            setRenderState(RenderUtil.overlayZBuf);
            setRenderState(RenderUtil.blendAlpha);
            setRenderState(RenderUtil.backCull);
            updateRenderState();
            
            // set the vertices, which change according to position and terrain
            int size = BangBoard.HEIGHTFIELD_SUBDIVISIONS + 1;
            setVertexBuffer(BufferUtils.createFloatBuffer(size * size * 3));
            updateVertices();
            
            // set the texture coords, indices, and ranges, which never change
            if (_htbuf == null) {
                _htbuf = BufferUtils.createFloatBuffer(size * size * 2);
                float step = 1f / BangBoard.HEIGHTFIELD_SUBDIVISIONS;
                for (int iy = 0; iy < size; iy++) {
                    for (int ix = 0; ix < size; ix++) {
                        _htbuf.put(ix * step);
                        _htbuf.put(iy * step);
                    }
                }
            
                _hibuf = BufferUtils.createIntBuffer(
                    BangBoard.HEIGHTFIELD_SUBDIVISIONS * size * 2);
                for (int iy = 0; iy < BangBoard.HEIGHTFIELD_SUBDIVISIONS;
                        iy++) {
                    for (int ix = 0; ix < size; ix++) {
                        _hibuf.put((iy+1)*size + ix);
                        _hibuf.put(iy*size + ix);
                    }
                }
                
                _hranges = new CompositeMesh.IndexRange[
                    BangBoard.HEIGHTFIELD_SUBDIVISIONS];
                for (int i = 0; i < _hranges.length; i++) {
                    _hranges[i] = CompositeMesh.createTriangleStrip(size*2);
                }
            }
            setTextureBuffer(_htbuf);
            setIndexBuffer(_hibuf);
            setIndexRanges(_hranges);
            
            setModelBound(new BoundingBox());
            updateModelBound();
        }
        
        /**
         * Sets the position of this highlight and updates it.
         */
        public void setPosition (int x, int y)
        {
            this.x = x;
            this.y = y;
            updateVertices();
        }
        
        /**
         * Updates the vertices of the highlight to reflect a change in
         * position or in the underlying terrain.
         */
        public void updateVertices ()
        {
            if (_board == null) {
                return;
            }
            
            FloatBuffer vbuf = getVertexBuffer();
            
            // if we're putting highlights over pieces and there's a piece
            // here, use the same elevation over the entire highlight
            boolean constantElevation = false;
            float elevation = 0f;
            if (_overPieces && _board.getPieceElevation(x, y) > 0) {
                constantElevation = true;
                elevation = _board.getElevation(x, y) * (TILE_SIZE /
                    BangBoard.ELEVATION_UNITS_PER_TILE);
            }
            
            int size = BangBoard.HEIGHTFIELD_SUBDIVISIONS,
                tx0 = x*BangBoard.HEIGHTFIELD_SUBDIVISIONS,
                ty0 = y*BangBoard.HEIGHTFIELD_SUBDIVISIONS, idx = 0;
            Vector3f vertex = new Vector3f();
            for (int ty = ty0, ty1 = ty0 + size; ty <= ty1; ty++) {
                for (int tx = tx0, tx1 = tx0 + size; tx <= tx1; tx++) {
                    if (constantElevation) {
                        vertex.set(tx * SUB_TILE_SIZE, ty * SUB_TILE_SIZE,
                            elevation);
                    
                    } else {
                        getHeightfieldVertex(tx, ty, vertex);
                    }
                    BufferUtils.setInBuffer(vertex, vbuf, idx++);
                }
            }
            
            updateModelBound();
        }
        
        /** If true, place the highlight on top of any pieces occupying the
         * tile. */
        protected boolean _overPieces;
    }

    public TerrainNode (BasicContext ctx)
    {
        super("terrain");
        _ctx = ctx;
        
        // always perform backface culling
        setRenderState(RenderUtil.backCull);
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
        refreshHeightfield(0, 0, _board.getTerrainWidth() - 1,
            _board.getTerrainWidth() - 1);
    }
    
    /**
     * Refreshes a region of the heightfield as specified in sub-tile
     * coordinates.  Remember that changing any height point affects the
     * normals of its neighbors, so be sure to include them in the region.
     */
    public void refreshHeightfield (int x1, int y1, int x2, int y2)
    {
        Rectangle rect = new Rectangle(x1, y1, 1 + x2 - x1, 1 + y2 - y1);
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
        refreshTerrain(0, 0, _board.getTerrainWidth() - 1,
            _board.getTerrainWidth() - 1);
    }
    
    /**
     * Refreshes the terrain splats over the specified region in sub-tile
     * coordinates.
     */
    public void refreshTerrain (int x1, int y1, int x2, int y2)
    {
        Rectangle rect = new Rectangle(x1, y1, 1 + x2 - x1, 1 + y2 - y1);
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
     * Creates and returns a highlight over this terrain at the specified tile
     * coordinates.  The highlight must be added to the scene graph before it
     * becomes visible.
     *
     * @param overPieces if true, place the highlight above any pieces
     * occupying the tile
     */
    public Highlight createHighlight (int x, int y, boolean overPieces)
    {
        return new Highlight(x, y, overPieces);
    }
    
    /**
     * Returns the interpolated height at the specified set of node space
     * coordinates.
     */
    public float getHeightfieldHeight (float x, float y)
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
        
        return FastMath.LERP(ax, FastMath.LERP(ay, ff, fc),
            FastMath.LERP(ay, cf, cc));
    }
    
    /**
     * Returns the interpolated normal at the specified set of node space
     * coordinates.
     */
    public Vector3f getHeightfieldNormal (float x, float y)
    {
        // scale down to sub-tile coordinates
        float stscale = BangBoard.HEIGHTFIELD_SUBDIVISIONS / TILE_SIZE;
        x *= stscale;
        y *= stscale;
        
        // sample at the four closest points and find the fractional components
        int fx = (int)FastMath.floor(x), cx = (int)FastMath.ceil(x),
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y);
        Vector3f ff = new Vector3f(), fc = new Vector3f(), cf = new Vector3f(),
            cc = new Vector3f(), fffc = new Vector3f(), cfcc = new Vector3f(),
            result = new Vector3f();
        getHeightfieldNormal(fx, fy, ff);
        getHeightfieldNormal(fx, cy, fc);
        getHeightfieldNormal(cx, fy, cf);
        getHeightfieldNormal(cx, cy, cc);
        float ax = x - fx, ay = y - fy;
        
        fffc.interpolate(ff, fc, ay);
        cfcc.interpolate(cf, cc, ay);
        result.interpolate(fffc, cfcc, ax);
        result.normalizeLocal();
        return result;
    }
    
    /**
     * Computes the heightfield vertex at the specified location in sub-tile
     * coordinates.
     *
     * @param result a vector to hold the result
     */
    public void getHeightfieldVertex (int x, int y, Vector3f result)
    {
        result.set(x * SUB_TILE_SIZE, y * SUB_TILE_SIZE,
            getHeightfieldValue(x, y));
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
     * Computes the normal at the specified heightfield location in sub-tile
     * coordinates.
     *
     * @param result a vector to hold the result
     */
    protected void getHeightfieldNormal (int x, int y, Vector3f result)
    {
        result.set(getHeightfieldValue(x-1, y) - getHeightfieldValue(x+1, y),
            getHeightfieldValue(x, y-1) - getHeightfieldValue(x, y+1),
            2*SUB_TILE_SIZE);
        result.normalizeLocal();
    }
 
    /**
     * Returns the scaled height of the specified location in sub-tile
     * coordinates.
     */
    protected float getHeightfieldValue (int x, int y)
    {
        return _board.getHeightfieldValue(x, y) *
            (TILE_SIZE / BangBoard.ELEVATION_UNITS_PER_TILE);
    }
    
    /**
     * Computes and returns the alpha value for the specified terrain code at
     * the given sub-tile coordinates.
     */
    protected float getTerrainAlpha (int code, float x, float y)
    {
         int fx = (int)FastMath.floor(x), cx = (int)FastMath.ceil(x),
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y);
        float ff = _board.getTerrainValue(fx, fy) == code ? 1.0f : 0.0f,
            fc = _board.getTerrainValue(fx, cy) == code ? 1.0f : 0.0f,
            cf = _board.getTerrainValue(cx, fy) == code ? 1.0f : 0.0f,
            cc = _board.getTerrainValue(cx, cy) == code ? 1.0f : 0.0f,
            ax = x - fx, ay = y - fy;
            
        return FastMath.LERP(ax, FastMath.LERP(ay, ff, fc),
            FastMath.LERP(ay, cf, cc));
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
                    int code = _board.getTerrainValue(x, y);
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
            base.setRenderState(RenderUtil.lequalZBuf);
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
                splat.setRenderState(RenderUtil.overlayZBuf);
                
                // and the alpha state
                splat.setRenderState(RenderUtil.blendAlpha);
                
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
    
    /** The shared texture coordinate buffer for highlights. */
    protected static FloatBuffer _htbuf;
    
    /** The shared index buffer for highlights. */
    protected static IntBuffer _hibuf;
    
    /** The shared range array for highlights. */
    protected static CompositeMesh.IndexRange[] _hranges;
    
    /** The size of the terrain splats in sub-tiles. */
    protected static final int SPLAT_SIZE = 32;
    
    /** The size of the splat alpha textures. */
    protected static final int TEXTURE_SIZE = SPLAT_SIZE * 2;
    
    /** The size of the sub-tiles. */
    protected static final float SUB_TILE_SIZE = TILE_SIZE /
        BangBoard.HEIGHTFIELD_SUBDIVISIONS;
    
    /** The number of segments in the cursor. */
    protected static final int CURSOR_SEGMENTS = 32;
}
