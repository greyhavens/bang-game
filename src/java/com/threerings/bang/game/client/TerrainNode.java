//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Color;
import java.awt.Rectangle;

import java.util.ArrayList;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;

import com.jme.bounding.BoundingBox;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.intersection.TrianglePickResults;
import com.jme.math.FastMath;
import com.jme.math.Plane;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.CompositeMesh;
import com.jme.scene.Line;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.lod.AreaClodMesh;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.CullState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
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

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.Terrain;

import static com.threerings.bang.Log.*;
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
            _verts.clear();
            if (_board != null) {
                float step = FastMath.TWO_PI / CURSOR_SEGMENTS, angle = 0.0f;
                for (int i = 0; i < CURSOR_SEGMENTS; i++) {
                    addVertex(_v1.set(x + radius*FastMath.cos(angle),
                        y + radius*FastMath.sin(angle)));
                    _v2.set(x + radius*FastMath.cos(angle+step),
                        y + radius*FastMath.sin(angle+step));
                    while (getBoundaryIntersection(_v1, _v2, _between)) {
                        addVertex(_v1.set(_between));
                    }
                    angle += step;
                }
            }
            setVertexBuffer(BufferUtils.createFloatBuffer(
                _verts.toArray(new Vector3f[_verts.size()])));
            generateIndices();
        }

        /**
         * Adds the three-dimensional vertex corresponding to the given
         * two-dimensional location to the vertex list.
         */
        protected void addVertex (Vector2f v)
        {
            _verts.add(new Vector3f(v.x, v.y,
                getHeightfieldHeight(v.x, v.y) + 0.1f));
        }
        
        protected ArrayList<Vector3f> _verts = new ArrayList<Vector3f>();
        protected Vector2f _v1 = new Vector2f(), _v2 = new Vector2f(),
            _between = new Vector2f();
    }

    /**
     * Represents a highlight draped over the terrain.
     */
    public class Highlight extends TriMesh
    {
        /** The position of the center of the highlight. */
        public float x, y;

        protected Highlight (int x, int y, boolean overPieces)
        {
            this((x + 0.5f) * TILE_SIZE, (y + 0.5f) * TILE_SIZE, TILE_SIZE,
                TILE_SIZE, true, overPieces);
        }

        protected Highlight (float x, float y, float width, float height)
        {
            this(x, y, width, height, false, false);
        }

        protected Highlight (float x, float y, float width, float height,
            boolean onTile, boolean overPieces)
        {
            super("highlight");
            this.x = x;
            this.y = y;
            _width = width;
            _height = height;
            _onTile = onTile;
            _overPieces = overPieces;

            setLightCombineMode(LightState.OFF);
            setRenderState(RenderUtil.overlayZBuf);
            setRenderState(RenderUtil.blendAlpha);
            setRenderState(RenderUtil.backCull);
            updateRenderState();

            // set the vertices, which change according to position and terrain
            if (_onTile) {
                _vwidth = _vheight = BangBoard.HEIGHTFIELD_SUBDIVISIONS + 1;
                
            } else {
                _vwidth = (int)FastMath.ceil(_width / SUB_TILE_SIZE) + 2;
                _vheight = (int)FastMath.ceil(_height / SUB_TILE_SIZE) + 2;
            }
            setVertexBuffer(BufferUtils.createFloatBuffer(
                _vwidth * _vheight * 3));

            // set the texture coords, which change for highlights not aligned
            // with tiles
            if (_onTile) {
                if (_htbuf == null) {
                    _htbuf = BufferUtils.createFloatBuffer(
                        _vwidth * _vheight * 2);
                    float step = 1f / BangBoard.HEIGHTFIELD_SUBDIVISIONS;
                    for (int iy = 0; iy < _vheight; iy++) {
                        for (int ix = 0; ix < _vwidth; ix++) {
                            _htbuf.put(ix * step);
                            _htbuf.put(iy * step);
                        }
                    }
                }
                setTextureBuffer(_htbuf);

            } else {
                setTextureBuffer(BufferUtils.createFloatBuffer(
                    _vwidth * _vheight * 2));
            }

            // update the vertices and possibly the texture coords
            updateVertices();

            // set the indices, which never change
            IntBuffer ibuf;
            if (_onTile && _hibuf != null) {
                ibuf = _hibuf;
                
            } else {
                int swidth = _vwidth - 1, sheight = _vheight - 1;
                ibuf = BufferUtils.createIntBuffer(swidth * sheight * 2 * 3);
                for (int iy = 0; iy < sheight; iy++) {
                    for (int ix = 0; ix < swidth; ix++) {
                        // upper left triangle
                        ibuf.put(iy*_vwidth + ix);
                        ibuf.put((iy+1)*_vwidth + (ix+1));
                        ibuf.put((iy+1)*_vwidth + ix);

                        // lower right triangle
                        ibuf.put(iy*_vwidth + ix);
                        ibuf.put(iy*_vwidth + (ix+1));
                        ibuf.put((iy+1)*_vwidth + (ix+1));
                    }
                }
                if (_onTile) {
                    _hibuf = ibuf;
                }
            }
            setIndexBuffer(ibuf);

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

            float x0 = x - _width/2, y0 = y - _height/2;
            int sx0 = (int)(x0 / SUB_TILE_SIZE),
                sy0 = (int)(y0 / SUB_TILE_SIZE);
            Vector3f vertex = new Vector3f();
            for (int sy = sy0, sy1 = sy0 + _vheight, idx = 0; sy < sy1; sy++) {
                for (int sx = sx0, sx1 = sx0 + _vwidth; sx < sx1; sx++) {
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
            float sstep = SUB_TILE_SIZE / _width,
                tstep = SUB_TILE_SIZE / _height,
                s0 = (sx0 * SUB_TILE_SIZE - x0) / _width,
                t0 = (sy0 * SUB_TILE_SIZE - y0) / _height;
            for (int iy = 0, idx = 0; iy < _vheight; iy++) {
                for (int ix = 0; ix < _vwidth; ix++) {
                    tcoord.set(s0 + ix * sstep, t0 + iy * tstep);
                    BufferUtils.setInBuffer(tcoord, tbuf, idx++);
                }
            }
        }

        /** If true, the highlight will always be aligned with a tile. */
        protected boolean _onTile;

        /** The dimensions of the highlight in world units. */
        protected float _width, _height;
        
        /** The dimensions of the highlight in vertices. */
        protected int _vwidth, _vheight;
        
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
        
        MaterialState mstate = ctx.getRenderer().createMaterialState();
        mstate.setAmbient(ColorRGBA.white);
        setRenderState(RenderUtil.createColorMaterialState(mstate, true));
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
        
        updateRenderState();
        updateGeometricState(0, true);
        
        // compute the bounding box planes with some extra space
        updateWorldBound();
        float bbxmax = _board.getWidth() * TILE_SIZE,
            bbymax = _board.getHeight() * TILE_SIZE,
            bbzmax = 129 * TILE_SIZE / BangBoard.ELEVATION_UNITS_PER_TILE;
        _bbplanes = new Plane[] {
            new Plane(Vector3f.UNIT_X, -SUB_TILE_SIZE),
            new Plane(Vector3f.UNIT_Y, -SUB_TILE_SIZE),
            new Plane(Vector3f.UNIT_Z, -bbzmax),
            new Plane(Vector3f.UNIT_X.negate(), -bbxmax),
            new Plane(Vector3f.UNIT_Y.negate(), -bbymax),
            new Plane(Vector3f.UNIT_Z.negate(), -bbzmax),
        };
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
                }
            }
        }
        updateGeometricState(0, true);
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
     * Refreshes the shadow colors obtained from the board.
     */
    public void refreshShadows ()
    {
        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                _blocks[x][y].refreshColors();
            }
        }
    }
    
    /**
     * Populates the board's shadow map by finding the height of the shadow
     * volume above each heightfield vertex.
     */
    public void generateShadows ()
    {
        int hfwidth = _board.getHeightfieldWidth(),
            hfheight = _board.getHeightfieldHeight();
        
        // make sure terrain collision trees are up-to-date
        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                _blocks[x][y].mesh.updateCollisionTree();
                _blocks[x][y].mesh.updateModelBound();
            }
        }
        updateGeometricState(0, true);
        
        // generate the shadow buffer
        byte[] shadows = _board.getShadows();
        float azimuth = _board.getLightAzimuth(0),
            elevation = _board.getLightElevation(0),
            hstep = TILE_SIZE / BangBoard.ELEVATION_UNITS_PER_TILE, theight;
        Vector3f origin = new Vector3f(),
            dir = new Vector3f(FastMath.cos(azimuth) * FastMath.cos(elevation),
                FastMath.sin(azimuth) * FastMath.cos(elevation),
                FastMath.sin(elevation));
        Ray ray = new Ray(origin, dir);
        TrianglePickResults results = new TrianglePickResults();
        for (int x = 0; x < hfwidth; x++) {
            for (int y = 0; y < hfheight; y++) {
                getHeightfieldVertex(x, y, origin);
                theight = origin.z;
                
                // determine the shadow height from self-shadowing
                int sheight = (int)((getSelfShadowHeight(ray) - theight) /
                    hstep);
                
                // use a binary search to find the highest piece shadow
                int lower = 0, upper = 256, middle = 128;
                while (middle > lower && middle < upper) {
                    origin.z = theight + middle * hstep;
                    results.clear();
                    _view.getPieceNode().calculatePick(ray, results);
                    if (containTriangles(results)) {
                        lower = middle;
                    } else {
                        upper = middle;
                    }
                    middle = (lower + upper) / 2;
                }
                
                _board.setShadowValue(x, y, Math.max(sheight, middle));
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
        return new Highlight(x, y, overPieces &&
                             Config.display.floatHighlights);
    }

    /**
     * Creates and returns a highlight over this terrain at the specified world
     * coordinates.  The highlight must be added to the scene graph before it
     * becomes visible.
     */
    public Highlight createHighlight (float x, float y, float width,
        float height)
    {
        return new Highlight(x, y, width, height);
    }

    /**
     * Computes the location at which the given ray intersects the terrain.
     *
     * @return true if an intersection was found, otherwise false
     */
    public boolean calculatePick (Ray ray, Vector3f result)
    {
        // make sure the ray intersects the bounding box
        if (worldBound == null || !worldBound.intersects(ray)) {
            return false;
        }
        
        // check for special case of vertical ray
        Vector3f origin = ray.getOrigin(), dir = ray.getDirection();
        if (dir.x == 0f && dir.y == 0f) {
            float height = getHeightfieldHeight(origin.x, origin.y);
            if ((origin.z > height) != (dir.z < 0f)) {
                return false;
            }
            result.set(origin.x, origin.y, height);
            return true;
        }
        
        // step from entrance to exit until we find a transition
        Vector3f entrance = new Vector3f(), exit = new Vector3f();
        computeEntranceExit(ray, entrance, exit);
        float slope = dir.z / FastMath.sqrt(dir.x*dir.x + dir.y*dir.y),
            r1 = entrance.z, r2,
            h1 = getHeightfieldHeight(entrance.x, entrance.y), h2;
        boolean over = r1 > h1;
        Vector2f v1 = new Vector2f(entrance.x, entrance.y),
            v2 = new Vector2f(exit.x, exit.y), between = new Vector2f();
        for (boolean cont = true; cont; ) {
            cont = getBoundaryIntersection(v1, v2, between);
            r2 = r1 + slope * getDistance(v1, between);
            h2 = getHeightfieldHeight(between.x, between.y);
            if ((r2 > h2) != over) {
                float t = (h1 - r1) / (r2 + h1 - r1 - h2);
                result.set(v1.x + t*(between.x - v1.x),
                    v1.y + t*(between.y - v1.y), h1 + t*(h2 - h1));
                return true;
            }
            v1.set(between);
            r1 = r2;
            h1 = h2;
        }
        return false;
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

        if (ax < ay) {
            return FastMath.LERP(ax, FastMath.LERP(ay, ff, fc),
                FastMath.LERP(ay, cc + ff - fc, cc));
            
        } else {
            return FastMath.LERP(ax, FastMath.LERP(ay, ff, cc + ff - cf),
                FastMath.LERP(ay, cf, cc));
        }
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
        return (_board == null) ? 0f : _board.getHeightfieldValue(x, y) *
            (TILE_SIZE / BangBoard.ELEVATION_UNITS_PER_TILE);
    }
    
    /**
     * Computes and returns the interpolated shadow height at the specified
     * world coordinates.
     */
    public float getShadowHeight (float x, float y)
    {
        // scale down to sub-tile coordinates
        float stscale = BangBoard.HEIGHTFIELD_SUBDIVISIONS / TILE_SIZE;
        x *= stscale;
        y *= stscale;

        // sample at the four closest points and find the fractional components
        int fx = (int)FastMath.floor(x), cx = (int)FastMath.ceil(x),
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y);
        float ff = getShadowHeight(fx, fy),
            fc = getShadowHeight(fx, cy),
            cf = getShadowHeight(cx, fy),
            cc = getShadowHeight(cx, cy),
            ax = x - fx, ay = y - fy;

        return FastMath.LERP(ax, FastMath.LERP(ay, ff, fc),
            FastMath.LERP(ay, cf, cc));
    }
    
    /**
     * Returns the scaled height of the shadow volume at the specified sub-tile
     * coordinates.
     */
    protected float getShadowHeight (int x, int y)
    {
        return (_board.getHeightfieldValue(x, y) +
            _board.getShadowValue(x, y)) *
                (TILE_SIZE / BangBoard.ELEVATION_UNITS_PER_TILE);
    }
    
    /**
     * Computes the height of the shadow volume from terrain self-shadowing.
     *
     * @param ray the ray from the point of interest to the light source
     */
    protected float getSelfShadowHeight (Ray ray)
    {
        // check for special case of vertical ray
        Vector3f origin = ray.getOrigin(), dir = ray.getDirection();
        if (dir.x == 0f && dir.y == 0f) {
            return -Float.MAX_VALUE;
        }
        
        // step from exit to origin updating height of volume
        Vector3f xydir = new Vector3f(dir.x, dir.y, 0f).normalizeLocal(),
            entrance = new Vector3f(), exit = new Vector3f();
        computeEntranceExit(new Ray(origin, xydir), entrance, exit);
        float slope = dir.z / FastMath.sqrt(dir.x*dir.x + dir.y*dir.y),
            height = getHeightfieldHeight(exit.x, exit.y);
        Vector2f v1 = new Vector2f(exit.x, exit.y),
            v2 = new Vector2f(origin.x, origin.y), between = new Vector2f();
        for (boolean cont = true; cont; ) {
            cont = getBoundaryIntersection(v1, v2, between);
            height = Math.max(getHeightfieldHeight(between.x, between.y),
                height - slope * v1.subtractLocal(between).length());
            v1.set(between);
        }
        return height;
    }
    
    /**
     * Determines whether the pick results contain any triangles.
     */
    protected boolean containTriangles (TrianglePickResults results)
    {
        for (int i = 0, size = results.getNumber(); i < size; i++) {
            ArrayList tris = results.getPickData(i).getTargetTris();
            if (tris != null && tris.size() > 0) {
                Object sprite = _view.getSprite(
                    results.getPickData(i).getTargetMesh());
                if (sprite == null || (sprite instanceof PieceSprite &&
                                       ((PieceSprite)sprite).getShadowType() ==
                                       PieceSprite.Shadow.STATIC)) {
                    return true;
                }   
            }
        }
        return false;
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
        block.cbuf = BufferUtils.createFloatBuffer(vwidth * vheight * 4);

        // refresh sets the vertices and normals from the heightfield
        block.bounds = new Rectangle(vx, vy, bwidth, bheight);
        block.ebounds = new Rectangle(vx - (le ? 2 : 0), vy - (be ? 2 : 0),
            vwidth, vheight);
        block.refreshGeometry(block.ebounds);
        
        // set the colors based on shadow values
        block.refreshColors();
        
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
        // static, use a VBO to store the vertices and compile to display list
        block.mesh = new TriMesh("terrain", block.vbuf, block.nbuf, block.cbuf,
            tbuf0, ibuf);
        block.mesh.setTextureBuffer(tbuf1, 1);
        block.mesh.setModelBound(new BoundingBox());
        block.mesh.updateModelBound();
        if (isHeightfieldStatic()) {
            // in order to ensure that texture coords are sent when compiling
            // the shared geometry to a display list, we must include a dummy
            // texture state
            TextureState tstate = _ctx.getRenderer().createTextureState();
            tstate.setTexture(null, 0);
            tstate.setTexture(null, 1);
            block.mesh.setRenderState(tstate);
            
            block.mesh.setVBOInfo(new VBOInfo());
            block.mesh.lockBounds();
            block.mesh.lockMeshes();
        }
        
        // create the splat meshes
        block.refreshSplats(block.bounds);
        
        return block;
    }

    /**
     * Checks whether the heightfield can be assumed to be static (and thus a
     * candidate for rendering optimization).  Default implementation returns
     * <code>true</code>.
     */
    protected boolean isHeightfieldStatic ()
    {
        return true;
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
                    alpha += weight;
                }
                total += weight;
            }
        }
        return alpha / total;
    }
    
    /**
     * Computes the base color value at the given sub-tile coordinates.
     */
    protected void getTerrainColor (float x, float y, ColorRGBA result)
    {
        int fx = (int)FastMath.floor(x), cx = (int)FastMath.ceil(x),
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y);
        Terrain ff = Terrain.fromCode(_board.getTerrainValue(fx, fy)),
            fc = Terrain.fromCode(_board.getTerrainValue(fx, cy)),
            cf = Terrain.fromCode(_board.getTerrainValue(cx, fy)),
            cc = Terrain.fromCode(_board.getTerrainValue(cx, cy));
        float ax = x - fx, ay = y - fy;

        _c1.interpolate(RenderUtil.getGroundColor(ff),
            RenderUtil.getGroundColor(fc), ay);
        _c2.interpolate(RenderUtil.getGroundColor(cf),
            RenderUtil.getGroundColor(cc), ay);
        result.interpolate(_c1, _c2, ax);
    }
 
    /**
     * Returns the smoothed shadow value for the specified sub-tile coordinate.
     * 0.0 is completely unshadowed, 1.0 is completely shadowed.
     */
    protected float getShadowValue (int x, int y)
    {
        float value = 0f, total = 0f;
        for (int sx = x - 1, sxn = x + 1; sx <= sxn; sx++) {
            for (int sy = y - 1, syn = y + 1; sy <= syn; sy++) {
                float xdist = (x - sx), ydist = (y - sy),
                    weight = Math.max(0f,
                        1f - (xdist*xdist + ydist*ydist)/(1.75f*1.75f));
                if (_board.getShadowValue(sx, sy) > 0) {
                    value += weight;
                }
                total += weight;
            }
        }
        return (value / total) * _board.getShadowIntensity();
    }
    
    /**
     * Computes the points at which the given ray enters and exits the bounding
     * volume.  If the ray originates from within the bounding volume, the
     * entrance point will be stored as the ray's origin.
     */
    protected void computeEntranceExit (Ray ray, Vector3f entrance,
        Vector3f exit)
    {
        // test against the six sides of the bounding box
        float tentrance = 0f, texit = -1f;
        for (int ii = 0; ii < _bbplanes.length; ii++) {
            float ndd = _bbplanes[ii].normal.dot(ray.direction);
            if (FastMath.abs(ndd) < FastMath.FLT_EPSILON) {
                continue;
            }
            float t = (-_bbplanes[ii].normal.dot(ray.origin) +
                _bbplanes[ii].constant) / ndd;
            if (Float.isNaN(t) || t <= 0f) {
                continue;
            }
            _isect.scaleAdd(t, ray.direction, ray.origin);
            if (!boundsContain(_isect, ii % 3)) {
                continue;
            }
            if (texit < 0f) {
                texit = t;
                
            } else {
                tentrance = Math.min(t, texit);
                texit = Math.max(t, texit);
            }
        }
        entrance.scaleAdd(tentrance, ray.direction, ray.origin);
        exit.scaleAdd(texit, ray.direction, ray.origin);
    }
    
    /**
     * Determines whether the bounding box contains the given point.
     *
     * @param skip the index modulo three of the plane indices to skip
     */
    protected boolean boundsContain (Vector3f pt, int skip)
    {
        for (int ii = 0; ii < _bbplanes.length; ii++) {
            if (ii % 3 != skip && 
                _bbplanes[ii].whichSide(pt) == Plane.NEGATIVE_SIDE) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * If the two vertices lie on either side of a horizontal, vertical, or
     * diagonal boundary, find the location of the boundary crossing nearest to
     * the first point.
     *
     * @return true if there was a boundary between the two points, false if
     * not (in which case the result will contain v2)
     */
    protected static boolean getBoundaryIntersection (Vector2f v1, Vector2f v2,
        Vector2f result)
    {
        float t = Math.min(getHorizontalIntersection(v1, v2),
            Math.min(getVerticalIntersection(v1, v2),
                getDiagonalIntersection(v1, v2)));
        if (t == Float.MAX_VALUE) {
            result.set(v2);
            return false;
        }
        result.set(v1.x + t*(v2.x - v1.x), v1.y + t*(v2.y - v1.y));
        return true;
    }
    
    /**
     * If the two vertices lie on either side of a horizontal boundary, find
     * the location of the boundary crossing nearest to the first point.
     *
     * @return <code>Float.MAX_VALUE</code> if there was no boundary between
     * the points, otherwise a number from 0 to 1 indicating the distance to
     * the boundary as a proportion of the distance between v1 and v2
     */
    protected static float getHorizontalIntersection (Vector2f v1,
        Vector2f v2)
    {
        int b1 = getBoundaryIndex(v1.y, SUB_TILE_SIZE),
            b2 = getBoundaryIndex(v2.y, SUB_TILE_SIZE);
        if (b1 == b2) {
            return Float.MAX_VALUE;
        }
        int step = (b1 < b2) ? +1 : -1;
        for (int b = b1 + step; b != b2; b += step) {
            if (b % 2 != 0) {
                continue;
            }
            return ((b/2)*SUB_TILE_SIZE - v1.y) / (v2.y - v1.y);
        }
        return Float.MAX_VALUE;
    }

    /**
     * If the two vertices lie on either side of a vertical boundary, find
     * the location of the boundary crossing nearest to the first point.
     *
     * @return <code>Float.MAX_VALUE</code> if there was no boundary between
     * the points, otherwise a number from 0 to 1 indicating the distance to
     * the boundary as a proportion of the distance between v1 and v2
     */
    protected static float getVerticalIntersection (Vector2f v1, Vector2f v2)
    {
        int b1 = getBoundaryIndex(v1.x, SUB_TILE_SIZE),
            b2 = getBoundaryIndex(v2.x, SUB_TILE_SIZE);
        if (b1 == b2) {
            return Float.MAX_VALUE;
        }
        int step = (b1 < b2) ? +1 : -1;
        for (int b = b1 + step; b != b2; b += step) {
            if (b % 2 != 0) {
                continue;
            }
            return ((b/2)*SUB_TILE_SIZE - v1.x) / (v2.x - v1.x);
        }
        return Float.MAX_VALUE;
    }

    /**
     * If the two vertices lie on either side of a diagonal boundary, find
     * the location of the boundary crossing nearest to the first point.
     *
     * @return <code>Float.MAX_VALUE</code> if there was no boundary between
     * the points, otherwise a number from 0 to 1 indicating the distance to
     * the boundary as a proportion of the distance between v1 and v2
     */
    protected static float getDiagonalIntersection (Vector2f v1, Vector2f v2)
    {
        float d1 = v1.y - v1.x, d2 = v2.y - v2.x;
        int b1 = getBoundaryIndex(d1, SUB_TILE_SIZE),
            b2 = getBoundaryIndex(d2, SUB_TILE_SIZE);
        if (b1 == b2) {
            return Float.MAX_VALUE;
        }
        int step = (b1 < b2) ? +1 : -1;
        for (int b = b1 + step; b != b2; b += step) {
            if (b % 2 != 0) {
                continue;
            }
            return ((b/2)*SUB_TILE_SIZE - d1) / (d2 - d1);
        }
        return Float.MAX_VALUE;
    }
    
    /**
     * Returns a boundary index for the specified value.  Starting at zero,
     * every other index represents a boundary between two regions.  The
     * other indices represent regions between the boundaries.
     *
     * @param step the size of the regions between boundaries
     */
    protected static int getBoundaryIndex (float v, float step)
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
    protected static boolean epsilonEquals (float a, float b)
    {
        return FastMath.abs(a - b) < 0.001f;
    }
        
    /**
     * Returns the distance between two 2D points.
     */
    protected static float getDistance (Vector2f v1, Vector2f v2)
    {
        float dx = v1.x - v2.x, dy = v1.y - v2.y;
        return FastMath.sqrt(dx*dx + dy*dy);
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

        /** The vertex, normal, and color buffers. */
        public FloatBuffer vbuf, nbuf, cbuf;

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
         * Refreshes the entire color buffer in response to a change in the
         * shadow map.
         */
        public void refreshColors ()
        {
            int idx = 0;
            ColorRGBA color = new ColorRGBA();
            for (int y = ebounds.y, ymax = y+ebounds.height; y < ymax; y++) {
                for (int x = ebounds.x, xmax = x+ebounds.width; x < xmax;
                        x++) {
                    color.interpolate(ColorRGBA.white, ColorRGBA.black,
                        getShadowValue(x, y));
                    BufferUtils.setInBuffer(color, cbuf, idx++);
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
                splat.setIsCollidable(false);
                
                // initialize the texture state
                TextureState tstate =
                    _ctx.getDisplay().getRenderer().createTextureState();
                tstate.setTexture(createAlphaTexture(code, rect), 0);
                Texture ground = getGroundTexture(code).getTexture();
                // before creating a clone, make sure the texture is bound
                if (ground.getTextureId() == 0) {
                    RenderUtil.createTextureState(_ctx, ground).apply();
                }
                ground = ground.createSimpleClone();
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
            ColorRGBA color = new ColorRGBA();
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    int idx = (y*TEXTURE_SIZE + x)*3;

                    getTerrainColor(bounds.x + x * step, bounds.y + y * step,
                        color);
                    baseBuffer.put((byte)(color.r * 255));
                    baseBuffer.put((byte)(color.g * 255));
                    baseBuffer.put((byte)(color.b * 255));
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

    /** Reusable objects for efficiency. */
    protected ColorRGBA _c1 = new ColorRGBA(), _c2 = new ColorRGBA();
    
    /** The shared texture coordinate buffer for highlights on tiles. */
    protected static FloatBuffer _htbuf;

    /** The shared index buffer for highlights on tiles. */
    protected static IntBuffer _hibuf;
    
    /** The planes of the node's bounding box. */
    protected Plane[] _bbplanes;
    
    /** A temporary result vector. */
    protected Vector3f _isect = new Vector3f();
    
    /** The size of the terrain splats in sub-tiles. */
    protected static final int SPLAT_SIZE = 32;

    /** The size of the splat alpha textures. */
    protected static final int TEXTURE_SIZE = SPLAT_SIZE * 2;

    /** The size of the sub-tiles. */
    protected static final float SUB_TILE_SIZE = TILE_SIZE /
        BangBoard.HEIGHTFIELD_SUBDIVISIONS;

    /** The size of the board edges that hide the void. */
    protected static final float EDGE_SIZE = 10000f;
    
    /** The number of segments in the cursor. */
    protected static final int CURSOR_SEGMENTS = 32;
}
