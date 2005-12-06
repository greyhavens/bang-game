//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Color;
import java.awt.Rectangle;

import java.util.ArrayList;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jme.bounding.BoundingBox;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.intersection.TrianglePickResults;
import com.jme.math.FastMath;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.CompositeMesh;
import com.jme.scene.Line;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.TriMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.lod.AreaClodMesh;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.util.geom.BufferUtils;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interator;

import com.threerings.media.image.ColorUtil;

import com.threerings.bang.client.Config;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.Terrain;

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
     * Represents a tile-sized highlight draped over the terrain.
     */
    public class Highlight extends TriMesh
    {
        /** The position of the center of the highlight. */
        public float x, y;

        protected Highlight (int x, int y, boolean overPieces)
        {
            this((x + 0.5f) * TILE_SIZE, (y + 0.5f) * TILE_SIZE, true,
                overPieces);
        }

        protected Highlight (float x, float y)
        {
            this(x, y, false, false);
        }

        protected Highlight (float x, float y, boolean onTile,
            boolean overPieces)
        {
            super("highlight");
            this.x = x;
            this.y = y;
            _onTile = onTile;
            _overPieces = overPieces;

            setLightCombineMode(LightState.OFF);
            setRenderState(RenderUtil.overlayZBuf);
            setRenderState(RenderUtil.blendAlpha);
            setRenderState(RenderUtil.backCull);
            updateRenderState();

            // set the vertices, which change according to position and terrain
            int size = BangBoard.HEIGHTFIELD_SUBDIVISIONS + (_onTile ? 1 : 2);
            setVertexBuffer(BufferUtils.createFloatBuffer(size * size * 3));

            // set the texture coords, which change for highlights not aligned
            // with tiles
            if (_onTile) {
                if (_htbuf == null) {
                    _htbuf = BufferUtils.createFloatBuffer(size * size * 2);
                    float step = 1f / BangBoard.HEIGHTFIELD_SUBDIVISIONS;
                    for (int iy = 0; iy < size; iy++) {
                        for (int ix = 0; ix < size; ix++) {
                            _htbuf.put(ix * step);
                            _htbuf.put(iy * step);
                        }
                    }
                }
                setTextureBuffer(_htbuf);

            } else {
                setTextureBuffer(BufferUtils.createFloatBuffer(
                    size * size * 2));
            }

            // update the vertices and possibly the texture coords
            updateVertices();

            // set the indices, which never change
            int on = (_onTile ? 0 : 1);
            if (_hibufs[on] == null) {
                int ssize = size - 1;
                _hibufs[on] = BufferUtils.createIntBuffer(
                    ssize * ssize * 2 * 3);
                for (int iy = 0; iy < ssize; iy++) {
                    for (int ix = 0; ix < ssize; ix++) {
                        // upper left triangle
                        _hibufs[on].put(iy*size + ix);
                        _hibufs[on].put((iy+1)*size + (ix+1));
                        _hibufs[on].put((iy+1)*size + ix);

                        // lower right triangle
                        _hibufs[on].put(iy*size + ix);
                        _hibufs[on].put(iy*size + (ix+1));
                        _hibufs[on].put((iy+1)*size + (ix+1));
                    }
                }
            }
            setIndexBuffer(_hibufs[on]);

            setModelBound(new BoundingBox());
            updateModelBound();
        }

        /**
         * Returns the x tile coordinate of this highlight.
         */
        public int getTileX ()
        {
            return (int)(x / TILE_SIZE);
        }

        /**
         * Returns the y tile coordinate of this highlight.
         */
        public int getTileY ()
        {
            return (int)(y / TILE_SIZE);
        }

        /**
         * Sets the position of this highlight in tile coordinates and updates
         * it.
         */
        public void setPosition (int x, int y)
        {
            setPosition((x + 0.5f) * TILE_SIZE, (y + 0.5f) * TILE_SIZE);
        }

        /**
         * Sets the position of this highlight in world coordinates and
         * updates it.
         */
        public void setPosition (float x, float y)
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
            int tx = getTileX(), ty = getTileY();
            if (_onTile && _overPieces &&
                _board.getPieceElevation(tx, ty) > 0) {
                constantElevation = true;
                elevation = _board.getElevation(tx, ty) * (TILE_SIZE /
                    BangBoard.ELEVATION_UNITS_PER_TILE);
            }

            float x0 = x - TILE_SIZE/2, y0 = y - TILE_SIZE/2;
            int size = BangBoard.HEIGHTFIELD_SUBDIVISIONS + (_onTile ? 1 : 2),
                sx0 = (int)(x0 / SUB_TILE_SIZE),
                sy0 = (int)(y0 / SUB_TILE_SIZE);
            Vector3f vertex = new Vector3f();
            for (int sy = sy0, sy1 = sy0 + size, idx = 0; sy < sy1; sy++) {
                for (int sx = sx0, sx1 = sx0 + size; sx < sx1; sx++) {
                    if (constantElevation) {
                        vertex.set(sx * SUB_TILE_SIZE, sy * SUB_TILE_SIZE,
                            elevation);

                    } else {
                        getHeightfieldVertex(sx, sy, vertex);
                    }
                    BufferUtils.setInBuffer(vertex, vbuf, idx++);
                }
            }
            updateModelBound();

            // if the highlight is aligned with a tile, we're done; otherwise,
            // we must update the texture coords as well
            if (_onTile) {
                return;
            }
            FloatBuffer tbuf = getTextureBuffer();
            Vector2f tcoord = new Vector2f();
            float step = 1f / BangBoard.HEIGHTFIELD_SUBDIVISIONS,
                s0 = (sx0 * SUB_TILE_SIZE - x0) / TILE_SIZE,
                t0 = (sy0 * SUB_TILE_SIZE - y0) / TILE_SIZE;
            for (int iy = 0, idx = 0; iy < size; iy++) {
                for (int ix = 0; ix < size; ix++) {
                    tcoord.set(s0 + ix * step, t0 + iy * step);
                    BufferUtils.setInBuffer(tcoord, tbuf, idx++);
                }
            }
        }

        /** If true, the highlight will always be aligned with a tile. */
        protected boolean _onTile;

        /** If true, the highlight will be over pieces occupying the tile. */
        protected boolean _overPieces;
    }

    public TerrainNode (BasicContext ctx, BoardView view)
    {
        super("terrain");
        _ctx = ctx;
        _view = view;

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
        int swidth = (int)Math.ceil((_board.getHeightfieldWidth() - 1.0) /
                SPLAT_SIZE),
            sheight = (int)Math.ceil((_board.getHeightfieldHeight() - 1.0) /
                SPLAT_SIZE);
        _blocks = new SplatBlock[swidth][sheight];
        for (int x = 0; x < swidth; x++) {
            for (int y = 0; y < sheight; y++) {
                _blocks[x][y] = createSplatBlock(x, y);
                attachChild(_blocks[x][y].node);
            }
        }
    }

    /**
     * Performs the second part of initial board creation, which is performed
     * after the pieces are in place.
     */
    public void initBoardTerrain ()
    {
        // create the shadow buffer automatically for static terrain
        if (isHeightfieldStatic()) {
            createShadowBuffer();
            
        } else {
            _sbuf = null;
        }
        
        // refresh the terrain now that the shadow buffer is up-to-date
        refreshTerrain();
        
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
        refreshHeightfield(0, 0, _board.getHeightfieldWidth() - 1,
            _board.getHeightfieldWidth() - 1);
    }

    /**
     * Refreshes a region of the heightfield as specified in sub-tile
     * coordinates.
     */
    public void refreshHeightfield (int x1, int y1, int x2, int y2)
    {
        // if the region includes the edges, we have to update the whole
        // shebang
        Rectangle rect;
        if (x1 <= 0 || y1 <= 0 || x2 >= _board.getHeightfieldWidth() - 1 ||
            y2 >= _board.getHeightfieldHeight() - 1) {
            _board.updateMinEdgeHeight();
            rect = new Rectangle(-1, -1, _board.getHeightfieldWidth() + 1,
                _board.getHeightfieldHeight() + 1);

        } else {
            rect = new Rectangle(x1, y1, 1 + x2 - x1, 1 + y2 - y1);
        }

        // grow the rectangle to make sure it includes the normals
        rect.grow(1, 1);

        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                SplatBlock block = _blocks[x][y];
                Rectangle isect = rect.intersection(block.ebounds);
                if (!isect.isEmpty()) {
                    block.refreshGeometry(isect);
                    block.mesh.updateModelBound();
                }
            }
        }
    }

    /**
     * Refreshes all terrain splats.
     */
    public void refreshTerrain ()
    {
        refreshTerrain(0, 0, _board.getHeightfieldWidth() - 1,
            _board.getHeightfieldWidth() - 1);
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
     * Creates the shadow buffer by casting rays from each heightfield vertex.
     */
    public void createShadowBuffer ()
    {
        int hfwidth = _board.getHeightfieldWidth(),
            hfheight = _board.getHeightfieldHeight();
        _sbuf = new boolean[hfwidth][hfheight];
        
        float azimuth = _board.getLightAzimuth(0),
            elevation = _board.getLightElevation(0);
        Vector3f origin = new Vector3f(),
            dir = new Vector3f(FastMath.cos(azimuth) * FastMath.cos(elevation),
                FastMath.sin(azimuth) * FastMath.cos(elevation),
                FastMath.sin(elevation));
        Ray ray = new Ray(origin, dir);
        TrianglePickResults results = new TrianglePickResults();
        for (int x = 0; x < hfwidth; x++) {
            for (int y = 0; y < hfheight; y++) {
                getHeightfieldVertex(x, y, origin);
                //origin.z += FastMath.FLT_EPSILON;
                
                // intersect with pieces
                results.clear();
                _view.getPieceNode().calculatePick(ray, results);
                
                _sbuf[x][y] = results.getNumber() > 0;
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
     * Creates and returns a tile-aligned highlight over this terrain at the
     * specified tile coordinates.  The highlight must be added to the scene
     * graph before it becomes visible.
     *
     * @param overPieces if true, place the highlight above any pieces
     * occupying the tile
     */
    public Highlight createHighlight (int x, int y, boolean overPieces)
    {
        return new Highlight(x, y, overPieces && Config.display.floatHighlights);
    }

    /**
     * Creates and returns a highlight over this terrain at the specified world
     * coordinates.  The highlight must be added to the scene graph before it
     * becomes visible.
     */
    public Highlight createHighlight (float x, float y)
    {
        return new Highlight(x, y);
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
        // expand the edges to hide the void
        result.x = x * SUB_TILE_SIZE;
        if (x < -1) {
            result.x -= EDGE_SIZE;

        } else if (x > _board.getHeightfieldWidth()) {
            result.x += EDGE_SIZE;
        }

        result.y = y * SUB_TILE_SIZE;
        if (y < -1) {
            result.y -= EDGE_SIZE;

        } else if (y > _board.getHeightfieldHeight()) {
            result.y += EDGE_SIZE;
        }

        result.z = getHeightfieldValue(x, y);
    }

    /**
     * Returns the scaled height of the specified location in sub-tile
     * coordinates.
     */
    public float getHeightfieldValue (int x, int y)
    {
        return _board.getHeightfieldValue(x, y) *
            (TILE_SIZE / BangBoard.ELEVATION_UNITS_PER_TILE);
    }

    /**
     * Creates and returns a splat block for the specified splat coordinates.
     */
    protected SplatBlock createSplatBlock (int sx, int sy)
    {
        // create the block and containing node
        SplatBlock block = new SplatBlock();
        block.node = new Node("block_" + sx + "_" + sy);

        // determine which edges this splat contains, if any
        boolean le = (sx == 0), re = (sx == _blocks.length - 1),
            be = (sy == 0), te = (sy == _blocks[0].length - 1);

        // compute the dimensions in terms of vertices and create buffers for
        // the vertices and normals
        int vx = sx * SPLAT_SIZE, vy = sy * SPLAT_SIZE,
            bwidth = Math.min(SPLAT_SIZE + 1,
                _board.getHeightfieldWidth() - vx),
            bheight = Math.min(SPLAT_SIZE + 1,
                _board.getHeightfieldHeight() - vy),
            vwidth = bwidth + (le ? 2 : 0) + (re ? 2 : 0),
            vheight = bheight + (be ? 2 : 0) + (te ? 2 : 0),
            vbufsize = vwidth * vheight * 3;
        block.vbuf = BufferUtils.createFloatBuffer(vbufsize);
        block.nbuf = BufferUtils.createFloatBuffer(vbufsize);

        // refresh sets the vertices and normals from the heightfield
        block.bounds = new Rectangle(vx, vy, bwidth, bheight);
        block.ebounds = new Rectangle(vx - (le ? 2 : 0), vy - (be ? 2 : 0),
            vwidth, vheight);
        block.refreshGeometry(block.ebounds);

        // set the texture coordinates
        FloatBuffer tbuf0 = BufferUtils.createFloatBuffer(vwidth*vheight*2),
            tbuf1 = BufferUtils.createFloatBuffer(vwidth*vheight*2);
        float step0 = 1.0f / (SPLAT_SIZE+1),
            step1 = 1.0f / BangBoard.HEIGHTFIELD_SUBDIVISIONS;
        for (int y = (be ? -2 : 0), ymax = y + vheight; y < ymax; y++) {
            for (int x = (le ? -2 : 0), xmax = x + vwidth; x < xmax; x++) {
                tbuf0.put(0.5f*step0 + x * step0);
                tbuf0.put(0.5f*step0 + y * step0);

                float xoff = 0f;
                if (le && x == -2) {
                    xoff = -EDGE_SIZE / TILE_SIZE;

                } else if (re && x == xmax - 1) {
                    xoff = EDGE_SIZE / TILE_SIZE;
                }
                tbuf1.put(x * step1 + xoff);

                float yoff = 0f;
                if (be && y == -2) {
                    yoff = -EDGE_SIZE / TILE_SIZE;

                } else if (te && y == ymax - 1) {
                    yoff = EDGE_SIZE / TILE_SIZE;
                }
                tbuf1.put(y * step1 + yoff);
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

        // create a trimesh with the computed values; if the heightfield is
        // static, use a VBO to store the vertices
        block.mesh = new TriMesh("terrain", block.vbuf, block.nbuf, null,
            tbuf0, ibuf);
        if (isHeightfieldStatic()) {
            block.mesh.setVBOInfo(new VBOInfo());
        }
        block.mesh.setTextureBuffer(tbuf1, 1);
        block.mesh.setModelBound(new BoundingBox());
        block.mesh.updateModelBound();

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
        // return straight up for vertices beyond the edge
        if (x < 0 || y < 0 || x >= _board.getHeightfieldWidth() ||
                y >= _board.getHeightfieldHeight()) {
            result.set(Vector3f.UNIT_Z);
            return;
        }

        result.set(getHeightfieldValue(x-1, y) - getHeightfieldValue(x+1, y),
            getHeightfieldValue(x, y-1) - getHeightfieldValue(x, y+1),
            2*SUB_TILE_SIZE);
        result.normalizeLocal();
    }
    
    /**
     * Computes and returns the alpha value for the specified terrain code at
     * the given sub-tile coordinates.
     */
    protected float getTerrainAlpha (int code, float x, float y)
    {
        int rx = (int)FastMath.floor(x + 0.5f),
            ry = (int)FastMath.floor(y + 0.5f);
        float alpha = 0f, total = 0f;
        for (int sx = rx - 1, sxmax = rx + 1; sx <= sxmax; sx++) {
            for (int sy = ry - 1, symax = ry + 1; sy <= symax; sy++) {
                float xdist = (x - sx), ydist = (y - sy),
                    weight = Math.max(0f,
                        1f - (xdist*xdist + ydist*ydist)/(1.75f*1.75f));
                if (_board.getTerrainValue(sx, sy) == code) {
                    alpha += (isInShadow(sx, sy) ? weight * SHADOW_MULTIPLIER :
                        weight);
                }
                total += weight;
            }
        }
        return alpha / total;
    }
    
    /**
     * Computes and returns the base color value at the given sub-tile
     * coordinates.
     */
    protected Color getTerrainColor (float x, float y)
    {
        int fx = (int)FastMath.floor(x), cx = (int)FastMath.ceil(x),
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y);
        Color ff = getTerrainColor(fx, fy), fc = getTerrainColor(fx, cy),
            cf = getTerrainColor(cx, fy), cc = getTerrainColor(cx, cy);
        float ax = x - fx, ay = y - fy;

        return ColorUtil.blend(ColorUtil.blend(cc, cf, ay),
            ColorUtil.blend(fc, ff, ay), ax);
    }

    /**
     * Returns the base color value at the given sub-tile coordinates.
     */
    protected Color getTerrainColor (int x, int y)
    {
        byte code = _board.getTerrainValue(x, y);
        Color color = (Color)_tcolors.get(code);
        if (color == null) {
            // if we haven't computed it already, determine the overall color
            // average for the texture
            Image img = RenderUtil.getGroundTile(Terrain.fromCode(code));
            ByteBuffer imgdata = img.getData();
            int r = 0, g = 0, b = 0, bytes = imgdata.limit(), pixels = bytes/3;
            for (int ii = 0; ii < bytes; ii += 3) {
                // the bytes are stored unsigned in the image but java is going
                // to interpret them as signed, so we need to do some fiddling
                r += btoi(imgdata.get(ii));
                g += btoi(imgdata.get(ii+1));
                b += btoi(imgdata.get(ii+2));
            }
            color = new Color(r / pixels, g / pixels, b / pixels);
            _tcolors.put(code, color);
        }
        return (isInShadow(x, y) ? ColorUtil.blend(color, Color.black,
            SHADOW_MULTIPLIER) : color);
    }

    protected static final int btoi (byte value)
    {
        return (value < 0) ? 256 + value : value;
    }

    /**
     * Determines whether the specified sub-tile coordinate is in shadow.
     */
    protected boolean isInShadow (int x, int y)
    {
        return _sbuf != null && x >= 0 && y >= 0 && x < _sbuf.length &&
            y < _sbuf[x].length && _sbuf[x][y];
    }
    
    /** Contains all the state associated with a splat block (a collection of
     * splats covering a single block of terrain). */
    protected class SplatBlock
    {
        /** The node containing the {@link SharedMesh} splats. */
        public Node node;

        /** The bounds of this block in sub-tile coordinates and the bounds
         * that include the edge. */
        public Rectangle bounds, ebounds;

        /** The shared, unparented mesh instance. */
        public TriMesh mesh;

        /** The vertex and normal buffers. */
        public FloatBuffer vbuf, nbuf;

        /** Maps terrain codes to ground texture states. */
        public HashIntMap groundTextures = new HashIntMap();

        /** The base texture buffer. */
        public ByteBuffer baseBuffer;

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
                    int index = (y-ebounds.y)*ebounds.width + (x-ebounds.x);

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

            // find out which terrain codes this block contains
            ArrayIntSet codes = new ArrayIntSet();
            for (int y = bounds.y, ymax = y+bounds.height; y < ymax; y++) {
                for (int x = bounds.x, xmax = x+bounds.width; x < xmax; x++) {
                    codes.add(_board.getTerrainValue(x, y));
                }
            }

            // use the most common terrain for the base mesh (which both tests
            // and writes to the z buffer)
            SharedMesh base = new SharedMesh("base", mesh);
            base.setRenderState(createBaseTexture(rect));
            base.setRenderState(RenderUtil.lequalZBuf);
            node.attachChild(base);

            // add the rest as splats (which only test the z buffer)
            for (Interator it = codes.interator(); it.hasNext(); ) {
                int code = it.nextInt();
                SharedMesh splat = new SharedMesh("splat" + code, mesh);

                // initialize the texture state
                TextureState tstate =
                    _ctx.getDisplay().getRenderer().createTextureState();
                tstate.setTexture(createAlphaTexture(code, rect), 0);
                Texture ground = getGroundTexture(code).getTexture().
                    createSimpleClone();
                ground.setApply(Texture.AM_COMBINE);
                ground.setCombineFuncAlpha(Texture.ACF_REPLACE);
                ground.setCombineSrc0Alpha(Texture.ACS_PREVIOUS);
                ground.setCombineFuncRGB(Texture.ACF_MODULATE);
                ground.setCombineSrc0RGB(Texture.ACS_TEXTURE);
                ground.setCombineSrc1RGB(Texture.ACS_PRIMARY_COLOR);
                tstate.setTexture(ground, 1);
                splat.setRenderState(tstate);

                // and the z buffer state
                splat.setRenderState(RenderUtil.overlayZBuf);

                // and the alpha state
                splat.setRenderState(RenderUtil.blendAlpha);

                node.attachChild(splat);
            }

            // prune any unused alpha buffers from the map
            for (Interator it = alphaBuffers.keys(); it.hasNext(); ) {
                if (!codes.contains(it.nextInt())) {
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
         * Creates and returns the base texture, using preexisting buffers when
         * possible.
         */
        protected TextureState createBaseTexture (Rectangle rect)
        {
            // create the buffer if it doesn't already exist
            if (baseBuffer == null) {
                baseBuffer = ByteBuffer.allocateDirect(TEXTURE_SIZE *
                    TEXTURE_SIZE * 3);
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
                    int idx = (y*TEXTURE_SIZE + x)*3;

                    Color color = getTerrainColor(bounds.x + x * step,
                        bounds.y + y * step);
                    baseBuffer.put((byte)color.getRed());
                    baseBuffer.put((byte)color.getGreen());
                    baseBuffer.put((byte)color.getBlue());
                }
            }

            Texture texture = new Texture();
            baseBuffer.rewind();
            texture.setImage(new Image(Image.RGB888, TEXTURE_SIZE,
                TEXTURE_SIZE, baseBuffer));

            // set the filter parameters
            texture.setFilter(Texture.FM_LINEAR);
            texture.setMipmapState(Texture.MM_LINEAR_LINEAR);

            TextureState tstate =
                _ctx.getDisplay().getRenderer().createTextureState();
            tstate.setTexture(texture);
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
            texture.setMipmapState(Texture.MM_LINEAR_LINEAR);

            // and the combination parameters
            texture.setApply(Texture.AM_COMBINE);
            texture.setCombineFuncAlpha(Texture.ACF_REPLACE);
            texture.setCombineSrc0Alpha(Texture.ACS_TEXTURE);

            return texture;
        }
    }

    /** The application context. */
    protected BasicContext _ctx;

    /** The board view. */
    protected BoardView _view;
    
    /** The board with the terrain. */
    protected BangBoard _board;
    
    /** A shadow buffer indicating, for static heightfields, which heightfield
     * points are in shadow (as determined by raycasting). */
    protected boolean[][] _sbuf;
    
    /** The array of splat blocks containing the terrain geometry/textures. */
    protected SplatBlock[][] _blocks;

    /** The shared texture coordinate buffer for highlights on tiles. */
    protected static FloatBuffer _htbuf;

    /** The shared index buffers for highlights (on tiles and arbitrarily
     * positioned). */
    protected static IntBuffer[] _hibufs = new IntBuffer[2];

    /** Maps terrain codes to colors. */
    protected static HashIntMap _tcolors = new HashIntMap();

    /** The size of the terrain splats in sub-tiles. */
    protected static final int SPLAT_SIZE = 32;

    /** The size of the splat alpha textures. */
    protected static final int TEXTURE_SIZE = SPLAT_SIZE * 2;

    /** The size of the sub-tiles. */
    protected static final float SUB_TILE_SIZE = TILE_SIZE /
        BangBoard.HEIGHTFIELD_SUBDIVISIONS;

    /** The size of the board edges that hide the void. */
    protected static final float EDGE_SIZE = 10000f;

    /** The color multiplier for heightfield vertices in shadow. */
    protected static final float SHADOW_MULTIPLIER = 0.25f;
    
    /** The number of segments in the cursor. */
    protected static final int CURSOR_SEGMENTS = 32;
}
