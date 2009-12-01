//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.List;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GLContext;

import com.jme.bounding.BoundingBox;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.intersection.PickResults;
import com.jme.intersection.TrianglePickResults;
import com.jme.math.FastMath;
import com.jme.math.Plane;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Line;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.VBOInfo;
import com.jme.scene.batch.TriangleBatch;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.util.geom.BufferUtils;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interator;
import com.samskivert.util.Invoker;

import com.threerings.jme.util.ShaderCache;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.Config;
import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;

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
            getBatch(0).getDefaultColor().set(ColorRGBA.white);
            setLightCombineMode(LightState.OFF);
            setRenderState(RenderUtil.lequalZBuf);

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
                    while (getBoundaryIntersection(_v1, _v2, _between) &&
                        !_v1.equals(_between)) { // sanity check
                        addVertex(_v1.set(_between));
                    }
                    angle += step;
                }
            }
            setVertexBuffer(0, BufferUtils.createFloatBuffer(
                _verts.toArray(new Vector3f[_verts.size()])));
            generateIndices(0);
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

        @Override // documentation inherited
        protected void setParent (Node parent)
        {
            super.setParent(parent);
            updateRenderState();
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

        /** The layer of the highlight. */
        public byte layer = 2;

        /** If true, the highlight will be over pieces occupying the tile. */
        public boolean overPieces;

        /** If true, the highlight will be flat. */
        public boolean flatten;

        /** Whether or not the user is hovering over the highlight. */
        public boolean hover;

        /** Whether or not the user *can* hover over it. */
        public boolean hoverable;

        /** A specified height for the highlight. */
        public int minElev = Integer.MIN_VALUE;

        protected Highlight (
                int x, int y, boolean overPieces, boolean flatten, int minElev)
        {
            this((x + 0.5f) * TILE_SIZE, (y + 0.5f) * TILE_SIZE, TILE_SIZE,
                TILE_SIZE, true, overPieces, flatten, minElev);
        }

        protected Highlight (
                int x, int y, boolean overPieces, boolean flatten, byte layer)
        {
            this((x + 0.5f) * TILE_SIZE, (y + 0.5f) * TILE_SIZE, TILE_SIZE,
                TILE_SIZE, true, overPieces, flatten, layer, Integer.MIN_VALUE);
        }

        protected Highlight (float x, float y, float width, float height)
        {
            this(x, y, width, height, false, false, false, Integer.MIN_VALUE);
        }

        protected Highlight (float x, float y, float width, float height,
            boolean onTile, boolean overPieces, boolean flatten, int minElev)
        {
            this(x, y, width, height, onTile,
                    overPieces, flatten, (byte)2, minElev);
        }

        protected Highlight (
                float x, float y, float width, float height, boolean onTile,
                boolean overPieces, boolean flatten, byte layer, int minElev)
        {
            super("highlight");
            this.x = x;
            this.y = y;
            this.layer = layer;
            this.overPieces = overPieces;
            this.flatten = flatten;
            this.minElev = minElev;
            _width = width;
            _height = height;
            _onTile = onTile;

            setLightCombineMode(LightState.OFF);
            setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            setRenderState(RenderUtil.overlayZBuf);
            setRenderState(RenderUtil.blendAlpha);
            setRenderState(RenderUtil.backCull);

            // set the vertices, which change according to position and terrain
            if (_onTile) {
                _vwidth = _vheight = BangBoard.HEIGHTFIELD_SUBDIVISIONS + 1;

            } else {
                _vwidth = (int)FastMath.ceil(_width / SUB_TILE_SIZE) + 2;
                _vheight = (int)FastMath.ceil(_height / SUB_TILE_SIZE) + 2;
            }
            setVertexBuffer(0, BufferUtils.createFloatBuffer(
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
                setTextureBuffer(0, _htbuf);

            } else {
                setTextureBuffer(0, BufferUtils.createFloatBuffer(
                    _vwidth * _vheight * 2));
            }
            setIndexBuffer(0, BufferUtils.createIntBuffer(
                (_vwidth - 1) * (_vheight - 1) * 6));

            // update the vertices, indices, and possibly the texture coords
            setModelBound(new BoundingBox());
            updateVertices();
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
         * Sets the default and hover colors for this highlight.
         */
        public void setColors (ColorRGBA defaultColor, ColorRGBA hoverColor)
        {
            _defaultColor = defaultColor;
            _hoverColor = hoverColor;
            updateHoverState();
        }

        /**
         * Sets the default and hover textures for this highlight.
         */
        public void setTextures (
            TextureState defaultTexture, TextureState hoverTexture)
        {
            _defaultTexture = defaultTexture;
            _hoverTexture = hoverTexture;
            updateHoverState();
        }

        /**
         * Sets the hover state of this highlight.
         */
        public void setHover (boolean hover)
        {
            this.hover = hover;
            updateHoverState();
        }

        /**
         * Sets whether this highlight has normals.
         */
        public void setHasNormals (boolean normals)
        {
            if (normals && getNormalBuffer(0) == null) {
                setNormalBuffer(0, BufferUtils.createFloatBuffer(
                    _vwidth * _vheight * 3));
                updateVertices();
            } else {
                setNormalBuffer(0, null);
            }
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

            FloatBuffer vbuf = getVertexBuffer(0),
                nbuf = getNormalBuffer(0);
            IntBuffer ibuf = getIndexBuffer(0);
            ibuf.rewind();

            // if we're putting highlights over pieces and there's a piece
            // here, raise the highlight above it and make the center of the
            // highlight its origin
            int tx = getTileX(), ty = getTileY();
            Vector3f offset = null;
            getLocalTranslation().set(0f, 0f, 0f);
            float height = 0f;
            boolean flat = flatten && (_board.isBridge(tx, ty) ||
                    !_board.isTraversable(tx, ty));
            int belev = _board.getElevation(tx, ty);
            if (flat) {
                if (minElev > Integer.MIN_VALUE) {
                    belev = minElev;
                }
                int maxelev = _board.getMaxHeightfieldElevation(tx, ty);
                height = (Math.max(minElev, Math.max(belev, maxelev)) * _elevationScale);

            } else if (_onTile && overPieces) {
                int helev = _board.getHeightfieldElevation(tx, ty);
                if (belev > helev) {
                    offset = new Vector3f(x, y, helev * _elevationScale);
                    getLocalTranslation().set(x, y, belev * _elevationScale);
                }
            }

            float x0 = x - _width/2, y0 = y - _height/2;
            int sx0 = (int)(x0 / SUB_TILE_SIZE),
                sy0 = (int)(y0 / SUB_TILE_SIZE);
            Vector3f vertex = new Vector3f();
            for (int sy = sy0, sy1 = sy0 + _vheight, idx = 0; sy < sy1; sy++) {
                for (int sx = sx0, sx1 = sx0 + _vwidth; sx < sx1; sx++) {
                    // set the normal if required
                    if (nbuf != null) {
                        if (flat) {
                            BufferUtils.setInBuffer(Vector3f.UNIT_Z, nbuf, idx);
                        } else {
                            getHeightfieldNormal(sx, sy, vertex);
                            BufferUtils.setInBuffer(vertex, nbuf, idx);
                        }
                    }

                    // set the vertex
                    getHeightfieldVertex(sx, sy, vertex);
                    if (flat) {
                        vertex.z = height;
                    } else {
                        if (offset != null) {
                            vertex.subtractLocal(offset);
                        }
                    }
                    vertex.z += layer * LAYER_OFFSET;
                    BufferUtils.setInBuffer(vertex, vbuf, idx++);

                    // update the index buffer according to the diagonalization
                    // toggles set by the splat blocks
                    if (sy == sy0 || sx == sx0) {
                        continue;
                    }
                    int ur = (sy-sy0)*_vwidth + (sx-sx0),
                        ul = ur - 1, lr = ur - _vwidth, ll = lr - 1;
                    if (_diags.length <= sy+1) {
                        log.warning("Attempting to access _diags out of range",
                                    "_diags.length", _diags.length, "sy", sy, "sy0", sy0,
                                    "sy1", sy1);
                        ibuf.put(ll); ibuf.put(ur); ibuf.put(ul);
                        ibuf.put(ll); ibuf.put(lr); ibuf.put(ur);
                        continue;
                    } else if (_diags[sy+1].length <= sx+1) {
                        log.warning("Attempting to access _diags out of range",
                                    "_diags[sy+1].length", _diags[sy+1].length, "sx", sx,
                                    "sx0", sx0, "sx1", sx1);
                        ibuf.put(ll); ibuf.put(ur); ibuf.put(ul);
                        ibuf.put(ll); ibuf.put(lr); ibuf.put(ur);
                        continue;
                    }
                    if (_diags[sy+1][sx+1]) {
                        ibuf.put(ul); ibuf.put(ll); ibuf.put(lr);
                        ibuf.put(ul); ibuf.put(lr); ibuf.put(ur);
                    } else {
                        ibuf.put(ll); ibuf.put(ur); ibuf.put(ul);
                        ibuf.put(ll); ibuf.put(lr); ibuf.put(ur);
                    }
                }
            }
            updateModelBound();
            setIsCollidable(flat || offset != null);
            if (isCollidable()) {
                updateCollisionTree();
            }

            // if the highlight is aligned with a tile, we're done; otherwise,
            // we must update the texture coords as well
            if (_onTile || flat) {
                return;
            }
            FloatBuffer tbuf = getTextureBuffer(0, 0);
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

        /**
         * Updates the state associated with the hover status.
         */
        protected void updateHoverState ()
        {
            // here, we set by reference rather than by value, because the default color
            // may be one of our special "throbbing" colors
            setDefaultColor(hover ? _hoverColor : _defaultColor);
            setRenderState(hover ? _hoverTexture : _defaultTexture);
            updateRenderState();
        }

        @Override // documentation inherited
        protected void setParent (Node parent)
        {
            super.setParent(parent);
            updateRenderState();
        }

        /** If true, the highlight will always be aligned with a tile. */
        protected boolean _onTile;

        /** The dimensions of the highlight in world units. */
        protected float _width, _height;

        /** The dimensions of the highlight in vertices. */
        protected int _vwidth, _vheight;

        /** The colors for normal and hover modes. */
        protected ColorRGBA _defaultColor = ColorRGBA.white,
            _hoverColor = ColorRGBA.white;

        /** The textures for normal and hover modes. */
        protected TextureState _defaultTexture, _hoverTexture;

        /** The zoffset for each layer. */
        protected static final float LAYER_OFFSET = TILE_SIZE/1000;
    }

    /**
     * Allows sharing the geometry of terrain highlights.
     */
    public static class SharedHighlight extends SharedMesh
    {
        public SharedHighlight (String name, Highlight target)
        {
            super(name, target);
        }

        @Override // documentation inherited
        public void findPick(Ray ray, PickResults results)
        {
            // the target mesh will be collidable only if it does not
            // lie on the terrain
            if (getTarget().isCollidable()) {
                super.findPick(ray, results);
            }
        }
    }

    /**
     * An interface for progress update callbacks.
     */
    public interface ProgressListener
    {
        /**
         * An update on the activity's progress.
         *
         * @param complete the percentage completed: 0.0 for none, 1.0 for done
         */
        public void update (float complete);
    }

    public TerrainNode (BasicContext ctx, BoardView view, boolean editorMode)
    {
        super("terrain");
        _ctx = ctx;
        _view = view;
        _editorMode = editorMode;

        // always perform backface culling
        setRenderState(RenderUtil.backCull);
        setRenderQueueMode(Renderer.QUEUE_SKIP);

        // we normalize things ourself
        setNormalsMode(NM_USE_PROVIDED);

        MaterialState mstate = _ctx.getRenderer().createMaterialState();
        mstate.setColorMaterial(MaterialState.CM_DIFFUSE);
        mstate.getAmbient().set(ColorRGBA.white);

        // this is a workaround for a mysterious bug in JME (or maybe LWJGL, or my OpenGL drivers).
        // without some difference in the material parameters between the terrain node and the
        // default material, the color material state appears to get "stuck" when used with the
        // shader.  changing the shininess has no other effect here, because the material's
        // specular color remains at the default black
        mstate.setShininess(1f);

        setRenderState(mstate);
    }

    /**
     * Initializes the terrain geometry using terrain data from the given board
     * and saves the board reference for later updates.
     */
    public void createBoardTerrain (BangBoard board)
    {
        _board = board;
        _elevationScale = _board.getElevationScale(TILE_SIZE);
        _diags = new boolean[_board.getHeightfieldHeight()+3][
            _board.getHeightfieldWidth()+3];

        // clean up any existing geometry
        detachAllChildren();
        cleanup();

        // find out now whether we should use shaders (so that the block creator knows)
        boolean useShaders = false;
        if (shouldUseShaders()) {
            GLSLShaderObjectsState sstate = _ctx.getRenderer().createGLSLShaderObjectsState();
            if (!configureShaderState(sstate, 2, true)) {
                _disableShaders = true;
            } else {
                useShaders = true;
            }
        }

        // create, store, and attach the splat blocks immediately in the
        // editor, or through the invoker in the game
        int swidth = (int)Math.ceil((_board.getHeightfieldWidth() - 1.0) /
                SPLAT_SIZE),
            sheight = (int)Math.ceil((_board.getHeightfieldHeight() - 1.0) /
                SPLAT_SIZE);
        _blocks = new SplatBlock[swidth][sheight];
        for (int x = 0; x < swidth; x++) {
            for (int y = 0; y < sheight; y++) {
                if (_editorMode) {
                    _blocks[x][y] = new SplatBlock(x, y, useShaders);
                    _blocks[x][y].finishCreation();
                } else {
                    _ctx.getInvoker().postUnit(new BlockCreator(x, y, useShaders));
                }
            }
        }

        // attach the skirt surrounding the terrain
        attachChild(_skirt = new Skirt());
        _skirt.updateRenderState();

        // compute the bounding box planes used for ray casting
        computeBoundingBoxPlanes();
    }

    /**
     * Releases the resources created by this node.
     */
    public void cleanup ()
    {
        if (_blocks == null) {
            return;
        }
        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                if (_blocks[x][y] == null) {
                    continue;
                }
                _blocks[x][y].deleteCreatedTextures();
                _blocks[x][y].mesh.unlockMeshes(_ctx.getRenderer());
                VBOInfo vboinfo = _blocks[x][y].mesh.getVBOInfo(0);
                if (vboinfo != null) {
                    RenderUtil.deleteVBOs(_ctx, vboinfo);
                }
            }
        }
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
        // make sure the scale is up-to-date
        float elevationScale = _board.getElevationScale(TILE_SIZE);
        if (_elevationScale != elevationScale) {
            _elevationScale = elevationScale;
            computeBoundingBoxPlanes();
        }

        // if the region includes the edges, we have to update all blocks
        // on the edges, plus the skirt
        boolean updateEdges = false;
        if (x1 <= 0 || y1 <= 0 || x2 >= _board.getHeightfieldWidth() - 1 ||
            y2 >= _board.getHeightfieldHeight() - 1) {
            _board.updateMinEdgeHeight();
            _skirt.updateVertices();
            updateEdges = true;
        }

        // grow the rectangle to make sure it includes the normals
        Rectangle rect = new Rectangle(x1, y1, 1 + x2 - x1, 1 + y2 - y1);
        rect.grow(1, 1);

        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                SplatBlock block = _blocks[x][y];
                Rectangle isect = rect.intersection(block.ebounds);
                if (updateEdges && block.isOnEdge()) {
                    block.refreshGeometry(block.ebounds);
                    block.mesh.updateModelBound();
                } else if (!isect.isEmpty()) {
                    block.refreshGeometry(isect);
                    block.mesh.updateModelBound();
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
        // if the region includes the edges, we have to update the skirt
        if (x1 <= 0 || y1 <= 0 || x2 >= _board.getHeightfieldWidth() - 1 ||
            y2 >= _board.getHeightfieldHeight() - 1) {
            _board.updateEdgeTerrain();
            _skirt.updateTexture();
        }

        // grow the rectangle to make sure it includes surroundings
        Rectangle rect = new Rectangle(x1, y1, 1 + x2 - x1, 1 + y2 - y1);
        rect.grow(1, 1);

        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                SplatBlock block = _blocks[x][y];
                Rectangle isect = rect.intersection(block.bounds);
                if (!isect.isEmpty()) {
                    block.refreshSplats(isect);
                    block.refreshShaders();
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
     * Refreshes the board's shader parameters.
     */
    public void refreshShaders ()
    {
        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                _blocks[x][y].refreshShaders();
            }
        }
    }

    /**
     * Populates the a shadow map by finding the height of the shadow
     * volume above each heightfield vertex.
     *
     * @param listener a listener to notify periodically with progress updates
     */
    public void generateShadows (byte[] shadows, ProgressListener listener)
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
        float azimuth = _board.getLightAzimuth(0),
            elevation = _board.getLightElevation(0),
            hstep = _elevationScale, theight,
            total = hfwidth * hfheight;
        Vector3f origin = new Vector3f(),
            dir = new Vector3f(FastMath.cos(azimuth) * FastMath.cos(elevation),
                FastMath.sin(azimuth) * FastMath.cos(elevation),
                FastMath.sin(elevation));
        Ray ray = new Ray(origin, dir);
        TrianglePickResults results = new TrianglePickResults();
        for (int x = 0, complete = 0; x < hfwidth; x++) {
            for (int y = 0; y < hfheight; y++, complete++) {
                // notify the listener once in a while
                if ((complete % 32) == 0) {
                    listener.update(complete / total);
                }

                // determine the shadow height from self-shadowing
                getHeightfieldVertex(x, y, origin);
                theight = origin.z;
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

                shadows[y*hfwidth + x] = (byte)
                    (Math.max(sheight, middle) - 128);
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
        return createHighlight(x, y, overPieces, false);
    }

    /**
     * Creates and returns a tile-aligned highlight over this terrain at the
     * specified tile coordinates.  The highlight must be added to the scene
     * graph before it becomes visible.
     *
     * @param overPieces if true, place the highlight above any pieces
     * occupying the tile
     * @param layer the rendering order for the highlight
     */
    public Highlight createHighlight (
            int x, int y, boolean overPieces, byte layer)
    {
        return new Highlight(
                x, y, overPieces && Config.floatHighlights, false, layer);
    }

    /**
     * Creates and returns a tile-aligned highlight over this terrain at the
     * specified tile coordinates.  The highlight must be added to the scene
     * graph before it becomes visible.
     *
     * @param overPieces if true, place the highlight above any pieces
     * occupying the tile
     * @param flatten if true, the highlight will be flat aligned with
     * the highest point of the tile
     */
    public Highlight createHighlight (
            int x, int y, boolean overPieces, boolean flatten)
    {
        return createHighlight(x, y, overPieces, flatten, Integer.MIN_VALUE);
    }

    public Highlight createHighlight (
            int x, int y, boolean overPieces, boolean flatten, int minElev)
    {
        return new Highlight(
                x, y, overPieces && Config.floatHighlights,
                flatten && Config.flattenHighlights, minElev);
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
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y),
            dx = fx + 2, dy = fy + 2;
        float ff = getHeightfieldValue(fx, fy),
            fc = getHeightfieldValue(fx, cy),
            cf = getHeightfieldValue(cx, fy),
            cc = getHeightfieldValue(cx, cy),
            ax = x - fx, ay = y - fy;

        if (dx >= 0 && dy >= 0 && dy < _diags.length &&
            dx < _diags[dy].length && _diags[dy][dx]) {
            if ((1f - ax) < ay) {
                return FastMath.LERP(ax, FastMath.LERP(ay, fc + cf - cc, fc),
                    FastMath.LERP(ay, cf, cc));
            } else {
                return FastMath.LERP(ax, FastMath.LERP(ay, ff, fc),
                    FastMath.LERP(ay, cf, cf + fc - ff));
            }
        } else {
            if (ax < ay) {
                return FastMath.LERP(ax, FastMath.LERP(ay, ff, fc),
                    FastMath.LERP(ay, cc + ff - fc, cc));
            } else {
                return FastMath.LERP(ax, FastMath.LERP(ay, ff, ff + cc - cf),
                    FastMath.LERP(ay, cf, cc));
            }
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
        result.set(x * SUB_TILE_SIZE, y * SUB_TILE_SIZE,
            getHeightfieldValue(x, y));
    }

    /**
     * Returns the scaled height of the specified location in sub-tile
     * coordinates.
     */
    public float getHeightfieldValue (int x, int y)
    {
        return (_board == null) ?
            0f : _board.getHeightfieldValue(x, y) * _elevationScale;
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
            _board.getShadowValue(x, y)) * _elevationScale;
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
            List<?> tris = results.getPickData(i).getTargetTris();
            if (tris != null && tris.size() > 0) {
                Object sprite = _view.getPieceSprite(
                    results.getPickData(i).getTargetMesh().getParentGeom());
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
     * Computes and stores the planes of the bounding box.
     */
    protected void computeBoundingBoxPlanes ()
    {
        // the z extents include some extra space for the ray cast algorithm
        float bbxmax = _board.getWidth() * TILE_SIZE,
            bbymax = _board.getHeight() * TILE_SIZE,
            bbzmax = 129 * _elevationScale;
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
        float t = Math.min(Math.min(getBoundaryIntersection(v1.x, v2.x),
            getBoundaryIntersection(v1.y, v2.y)),
                Math.min(getBoundaryIntersection(v1.y - v1.x, v2.y - v2.x),
            getBoundaryIntersection(v1.y + v1.x, v2.y + v2.x)));
        if (t == Float.MAX_VALUE) {
            result.set(v2);
            return false;
        }
        result.set(v1.x + t*(v2.x - v1.x), v1.y + t*(v2.y - v1.y));
        return true;
    }

    /**
     * If the two values lie on either side of a boundary, find the location
     * of the boundary crossing nearest to the first point.
     *
     * @return <code>Float.MAX_VALUE</code> if there was no boundary between
     * the points, otherwise a number from 0 to 1 indicating the distance to
     * the boundary as a proportion of the distance between v1 and v2
     */
    protected static float getBoundaryIntersection (float v1, float v2)
    {
        int b1 = getBoundaryIndex(v1, SUB_TILE_SIZE),
            b2 = getBoundaryIndex(v2, SUB_TILE_SIZE);
        if (b1 == b2) {
            return Float.MAX_VALUE;
        }
        int step = (b1 < b2) ? +1 : -1;
        for (int b = b1 + step; b != b2; b += step) {
            if (b % 2 != 0) {
                continue;
            }
            return ((b/2)*SUB_TILE_SIZE - v1) / (v2 - v1);
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

    /**
     * Clamps the given integer to [min, max).
     */
    protected static int iclamp (int value, int min, int max)
    {
        return (value <= min) ? min : (value >= max ? (max - 1) : value);
    }

    /**
     * Determines whether we should use GLSL shaders when rendering the terrain.
     */
    protected boolean shouldUseShaders ()
    {
        return shouldRenderSplats() &&
            GLContext.getCapabilities().GL_ARB_vertex_shader &&
            GLContext.getCapabilities().GL_ARB_fragment_shader &&
            TextureState.getNumberOfFragmentUnits() >= 4 &&
            !_disableShaders;
    }

    /**
     * Configures a terrain shader state with the requested parameters.
     */
    protected boolean configureShaderState (GLSLShaderObjectsState sstate, int splats, boolean fog)
    {
        ShaderCache scache = _ctx.getShaderCache();
        String frag = "shaders/terrain.frag";

        // only generate the derived ADD_SPLATS definition if the shader isn't already loaded
        String sdef = "NUM_SPLATS " + splats;
        String[] defs = (fog ? new String[] { "ENABLE_FOG", sdef } : new String[] { sdef });
        if (scache.isLoaded(null, frag, defs)) {
            return scache.configureState(sstate, null, frag, defs);
        }
        StringBuffer abuf = new StringBuffer("ADD_SPLATS ");
        for (int ii = 0; ii < splats; ii++) {
            abuf.append("gl_FragColor += (texture2D(splatTextures[" + (ii * 2) +
                "], gl_TexCoord[0].st * terrainScales[" + ii + "]) * texture2D(splatTextures[" +
                    (ii * 2 + 1) + "], gl_TexCoord[1].st).a); ");
        }
        return scache.configureState(sstate, null, frag, defs, new String[] { abuf.toString() });
    }

    /**
     * Checks whether we should render terrain splats (as opposed to simply rendering the most
     * common kind of terrain for each block).
     */
    protected static boolean shouldRenderSplats ()
    {
        return (BangPrefs.isMediumDetail() && TextureState.getNumberOfFixedUnits() >= 2);
    }

    /** Creates and adds a single terrain block on the invoker thread. */
    protected class BlockCreator extends Invoker.Unit
    {
        public BlockCreator (int x, int y, boolean useShaders)
        {
            _x = x;
            _y = y;
            _useShaders = useShaders;
            _view.addResolving(this);
        }

        // documentation inherited
        public boolean invoke ()
        {
            _blocks[_x][_y] = new SplatBlock(_x, _y, _useShaders);
            return true;
        }

        @Override // documentation inherited
        public void handleResult ()
        {
            _blocks[_x][_y].finishCreation();
            _view.clearResolving(this);
        }

        /** The coordinates of the block to create. */
        protected int _x, _y;

        /** Whether or not to use shaders. */
        protected boolean _useShaders;
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

        /** The index buffer. */
        public IntBuffer ibuf;

        /** Maps terrain codes to ground textures. */
        public HashIntMap<Texture> groundTextures = new HashIntMap<Texture>();

        /** Maps terrain codes to alpha texture buffers. */
        public HashIntMap<ByteBuffer> alphaBuffers = new HashIntMap<ByteBuffer>();

        /** Contains the code for each terrain layer (plus one, because zeros
         * are interpreted as empty spaces by {@link IntListUtil}. */
        public int[] layers;

        /** The generated alpha textures. */
        public ArrayList<Texture> alphaTextures = new ArrayList<Texture>();

        /** Whether or not we're using shaders. */
        public boolean useShaders;

        public SplatBlock (int sx, int sy, boolean useShaders)
        {
            // create the containing node
            node = new Node("block_" + sx + "_" + sy);
            this.useShaders = useShaders;

            // determine which edges this splat contains, if any
            boolean le = (sx == 0), re = (sx == _blocks.length - 1),
                be = (sy == 0), te = (sy == _blocks[0].length - 1);

            // compute the dimensions in terms of vertices and create buffers
            // for the vertices and normals
            int vx = sx * SPLAT_SIZE, vy = sy * SPLAT_SIZE,
                bwidth = Math.min(SPLAT_SIZE + 1,
                    _board.getHeightfieldWidth() - vx),
                bheight = Math.min(SPLAT_SIZE + 1,
                    _board.getHeightfieldHeight() - vy),
                vwidth = bwidth + (le ? 2 : 0) + (re ? 2 : 0),
                vheight = bheight + (be ? 2 : 0) + (te ? 2 : 0),
                vbufsize = vwidth * vheight * 3;
            vbuf = BufferUtils.createFloatBuffer(vbufsize);
            nbuf = BufferUtils.createFloatBuffer(vbufsize);
            cbuf = BufferUtils.createFloatBuffer(vwidth * vheight * 4);
            if (_editorMode) {
                ibuf = BufferUtils.createIntBuffer(
                    (vwidth - 1) * (vheight - 1) * 2 * 3);
            }

            // refresh sets the vertices, normals, and indices from the
            // heightfield
            bounds = new Rectangle(vx, vy, bwidth, bheight);
            ebounds = new Rectangle(vx - (le ? 2 : 0), vy - (be ? 2 : 0),
                vwidth, vheight);
            refreshGeometry(ebounds);
            if (!_editorMode) {
                setIndices();
            }

            // set the colors based on shadow values
            refreshColors();

            // set the texture coordinates
            FloatBuffer
                tbuf0 = BufferUtils.createFloatBuffer(vwidth*vheight*2),
                tbuf1 = BufferUtils.createFloatBuffer(vwidth*vheight*2);
            float step0 = 1.0f / BangBoard.HEIGHTFIELD_SUBDIVISIONS,
                step1 = 1.0f / (SPLAT_SIZE+1);
            for (int y = (be ? -2 : 0), ymax = y + vheight; y < ymax; y++) {
                for (int x = (le ? -2 : 0), xmax = x + vwidth; x < xmax; x++) {
                    tbuf0.put(x * step0);
                    tbuf0.put(y * step0);

                    tbuf1.put(0.5f*step1 + iclamp(x, 0, bwidth) * step1);
                    tbuf1.put(0.5f*step1 + iclamp(y, 0, bheight) * step1);
                }
            }

            // create a trimesh with the computed values; if the heightfield is
            // static, use a VBO to store the vertices and compile to display
            // list
            mesh = new TriMesh("terrain", vbuf, nbuf, cbuf,
                tbuf0, ibuf);
            if (shouldRenderSplats()) {
                mesh.setTextureBuffer(0, tbuf1, 1);
            }
            if (!_editorMode) {
                mesh.getBatch(0).setMode(TriangleBatch.TRIANGLE_STRIP);
            }
            mesh.setModelBound(new BoundingBox());
            mesh.updateModelBound();

            // if we are using VBOs, we can set them here; display lists must
            // be compiled on the main thread
            if (!_editorMode) {
                if (Config.useVBOs && _ctx.getRenderer().supportsVBO()) {
                    VBOInfo vboinfo = new VBOInfo(true);
                    vboinfo.setVBOIndexEnabled(true);
                    mesh.setVBOInfo(vboinfo);
                }
                mesh.lockBounds();
            }

            // create the splat meshes
            refreshSplats(bounds);
        }

        /**
         * Performs the steps necessary to finish creation of the block on the
         * main thread, which can make OpenGL calls.
         */
        public void finishCreation ()
        {
            // compile display lists if not using VBOs
            if (!_editorMode && Config.useDisplayLists && !(Config.useVBOs &&
                    _ctx.getRenderer().supportsVBO())) {
                // in order to ensure that texture coords are sent when
                // compiling the shared geometry to a display list, we must
                // include a dummy texture state
                TextureState tstate = _ctx.getRenderer().createTextureState();
                tstate.setTexture(null, 0);
                tstate.setTexture(null, 1);
                mesh.setRenderState(tstate);
                mesh.lockMeshes();
            }

            // load the textures
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                TextureState tstate = (TextureState)node.getChild(ii).getRenderState(
                    RenderState.RS_TEXTURE);
                if (tstate != null) {
                    RenderUtil.ensureLoaded(tstate);
                }
            }

            // create the shader states
            refreshShaders();

            // attach self and update render state
            attachChild(node);
            node.updateRenderState();
        }

        /**
         * Checks whether this block includes an edge.
         */
        public boolean isOnEdge ()
        {
            return !ebounds.equals(bounds);
        }

        /**
         * Refreshes all of the shader parameters.
         */
        public void refreshShaders ()
        {
            if (!useShaders || layers.length <= 1) {
                return;
            }
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                Spatial pass = node.getChild(ii);
                TextureState tstate = (TextureState)pass.getRenderState(RenderState.RS_TEXTURE);
                if (tstate == null) {
                    continue;
                }
                int splats = tstate.getNumberOfSetTextures() / 2;
                GLSLShaderObjectsState sstate = (GLSLShaderObjectsState)pass.getRenderState(
                    RenderState.RS_GLSL_SHADER_OBJECTS);
                configureShaderState(sstate, splats, _board.getFogDensity() > 0f);
            }
        }

        /**
         * Refreshes the geometry covered by the specified rectangle (in
         * sub-tile coordinates).
         */
        public void refreshGeometry (Rectangle rect)
        {
            Vector3f v1 = new Vector3f(), v2 = new Vector3f();
            for (int y = rect.y, ymax = y + rect.height; y < ymax; y++) {
                for (int x = rect.x, xmax = x + rect.width; x < xmax; x++) {
                    int ur = (y-ebounds.y)*ebounds.width + (x-ebounds.x),
                        ul = ur - 1, lr = ur - ebounds.width, ll = lr - 1;

                    getHeightfieldVertex(x, y, v1);
                    BufferUtils.setInBuffer(v1, vbuf, ur);

                    getHeightfieldNormal(x, y, v1);
                    BufferUtils.setInBuffer(v1, nbuf, ur);

                    // update the indices, dividing the quad to separate the
                    // pair of vertices with the greater angle between their
                    // normals
                    if (y == rect.y || x == rect.x) {
                        continue;
                    }
                    BufferUtils.populateFromBuffer(v2, nbuf, ll);
                    float urll = v1.dot(v2);
                    BufferUtils.populateFromBuffer(v1, nbuf, ul);
                    BufferUtils.populateFromBuffer(v2, nbuf, lr);
                    float ullr = v1.dot(v2);
                    _diags[y+1][x+1] = urll < ullr;
                    if (x >= 0) {
                        // the difference must be greater than a certain
                        // amount, otherwise we prefer the previous value
                        // to avoid switching too often
                        boolean prev = _diags[y+1][x];
                        if (prev != _diags[y+1][x+1] &&
                            ((prev && (urll - ullr < 0.001f)) ||
                            (!prev && (ullr - urll < 0.001f)))) {
                            _diags[y+1][x+1] = prev;
                        }
                    }
                    if (_editorMode) {
                        int iidx = ((y-ebounds.y-1)*(ebounds.width-1) +
                            (x-ebounds.x-1)) * 6;
                        if (_diags[y+1][x+1]) {
                            ibuf.put(iidx++, ul);
                            ibuf.put(iidx++, ll);
                            ibuf.put(iidx++, lr);

                            ibuf.put(iidx++, ul);
                            ibuf.put(iidx++, lr);
                            ibuf.put(iidx, ur);
                        } else {
                            ibuf.put(iidx++, ll);
                            ibuf.put(iidx++, ur);
                            ibuf.put(iidx++, ul);

                            ibuf.put(iidx++, ll);
                            ibuf.put(iidx++, lr);
                            ibuf.put(iidx, ur);
                        }
                    }
                }
            }
        }

        /**
         * Sets the geometry for the entire block.  Because this method creates
         * a single triangle strip, it can only be used when the terrain will
         * not change after initialization.
         */
        protected void setIndices ()
        {
            // create a temporary buffer with the maximum possible space
            IntBuffer tbuf = BufferUtils.createIntBuffer(ebounds.width * 3 *
                (ebounds.height - 1));

            boolean even = true, ud = true, nud;
            for (int y = ebounds.y, ymax = y + (ebounds.height - 1);
                y < ymax; y++) {
                int x1 = even ? ebounds.x : (ebounds.x + ebounds.width - 1),
                    x2 = even ? (ebounds.x + ebounds.width) : (ebounds.x - 1),
                    dx = even ? +1 : -1,
                    iy = y - ebounds.y, ix;
                for (int x = x1; x != x2; x += dx) {
                    ix = x - ebounds.x;
                    if (x != x2 - dx) {
                        nud = even ^ _diags[y + 2][x + (even ? 2 : 1)];
                        if (x == x1 && y != ebounds.y) {
                            tbuf.put(iy*ebounds.width + ix);
                            tbuf.put(iy*ebounds.width + ix);
                            if (nud == ud) {
                                tbuf.put(iy*ebounds.width + ix);
                            }
                        } else if (nud != ud) {
                            if (nud) {
                                tbuf.put(iy*ebounds.width + ix);
                            } else {
                                tbuf.put((iy+1)*ebounds.width + ix);
                            }
                        }
                        ud = nud;
                    }
                    if (ud) {
                        tbuf.put((iy+1)*ebounds.width + ix);
                        tbuf.put(iy*ebounds.width + ix);
                    } else {
                        tbuf.put(iy*ebounds.width + ix);
                        tbuf.put((iy+1)*ebounds.width + ix);
                    }
                }
                even = !even;
            }
            tbuf.flip();

            // create a new buffer to hold the used part of the temporary one
            ibuf = BufferUtils.createIntBuffer(tbuf.limit());
            ibuf.put(tbuf);
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
            // remove all the existing children and delete created textures
            node.detachAllChildren();
            deleteCreatedTextures();

            // find out which terrain codes this block contains and determine
            // which one is the most common
            IntIntMap codes = new IntIntMap();
            int ccount = 0, ccode = 0, count, code;
            for (int y = ebounds.y, ymax = y + ebounds.height; y < ymax; y++) {
                for (int x = ebounds.x, xmax = x + ebounds.width; x < xmax; x++) {
                    code = _board.getTerrainValue(x, y)+1;
                    if ((count = codes.increment(code, 1)) > ccount) {
                        ccount = count;
                        ccode = code;
                    }
                }
            }

            // don't use certain textures as the base in low detail mode,
            // unless they're the only texture in that region
            if (!shouldRenderSplats() && !TerrainConfig.getConfig(ccode-1).lowDetail) {
                ccount = 0;
                for (IntIntMap.IntIntEntry entry : codes.entrySet()) {
                    count = entry.getIntValue();
                    code = entry.getIntKey();
                    if (count > ccount &&
                        TerrainConfig.getConfig(code-1).lowDetail) {
                        ccount = count;
                        ccode = code;
                    }
                }
            }

            if (layers == null || layers[0] != ccode) {
                layers = new int[] { ccode };
                rect = bounds;

            } else {
                for (int ii = 1; ii < layers.length; ii++) {
                    if (!codes.containsKey(layers[ii])) {
                        layers[ii] = 0;
                        rect = bounds;
                    }
                }
            }
            for (Interator it = codes.keys(); it.hasNext(); ) {
                code = it.nextInt();
                int[] nlayers = IntListUtil.testAndAdd(layers, code);
                if (nlayers != null) {
                    layers = nlayers;
                    rect = bounds;
                }
            }
            layers = IntListUtil.compact(layers);

            // build layers using shaders or fixed functionality pipeline
            if (useShaders && layers.length > 1) {
                buildShaderLayers(rect);
            } else {
                buildFixedLayers(rect);
            }

            // prune any unused alpha buffers from the map
            for (Interator it = alphaBuffers.keys(); it.hasNext(); ) {
                if (!codes.containsKey(it.nextInt()+1)) {
                    it.remove();
                }
            }

            node.updateRenderState();
        }

        protected void buildShaderLayers (Rectangle rect)
        {
            int units = TextureState.getNumberOfFragmentUnits();

            for (int ii = 0, lidx = 0; lidx < layers.length; ii++) {
                SharedMesh pass = new SharedMesh("pass" + ii, mesh);
                node.attachChild(pass);

                // determine the number of splats in this pass
                int splats = Math.min(units / 2, layers.length - lidx);

                if (ii > 0) {
                    // passes after the first are added on
                    if (_addAlpha == null) {
                        _addAlpha = _ctx.getRenderer().createAlphaState();
                        _addAlpha.setBlendEnabled(true);
                        _addAlpha.setSrcFunction(AlphaState.SB_ONE);
                        _addAlpha.setDstFunction(AlphaState.DB_ONE);
                    }
                    pass.setRenderState(_addAlpha);
                    pass.setRenderState(RenderUtil.overlayZBuf);
                    pass.setIsCollidable(false);
                }
                GLSLShaderObjectsState sstate = _ctx.getRenderer().createGLSLShaderObjectsState();
                pass.setRenderState(sstate);

                TextureState tstate = _ctx.getDisplay().getRenderer().createTextureState();
                pass.setRenderState(tstate);

                for (int jj = 0, tidx = 0; jj < splats; jj++, lidx++) {
                    int code = layers[lidx] - 1;
                    Texture gtex = getGroundTexture(code);
                    if (gtex == null) {
                        continue; // something's funny, skip it
                    }
                    gtex = gtex.createSimpleClone();
                    tstate.setTexture(gtex, tidx);
                    sstate.setUniform("splatTextures[" + tidx + "]", tidx++);

                    // we clear out the fixed-function transform and scale in the shader
                    sstate.setUniform("terrainScales[" + jj + "]", gtex.getScale().x);
                    gtex.setScale(null);

                    Texture alpha = createAlphaTexture(code, rect, true);
                    tstate.setTexture(alpha, tidx);
                    sstate.setUniform("splatTextures[" + tidx + "]", tidx++);
                    alphaTextures.add(alpha);
                }
            }
        }

        protected void buildFixedLayers (Rectangle rect)
        {
            // use the most common terrain for the base mesh (which both tests
            // and writes to the z buffer)
            SharedMesh base = new SharedMesh("base", mesh);
            int ccode = layers[0] - 1;
            Texture gtex = getGroundTexture(ccode);
            if (gtex != null) {
                base.setRenderState(RenderUtil.createTextureState(_ctx, gtex));
            }
            node.attachChild(base);

            // for low detail or single layers, just stop there
            if (!shouldRenderSplats() || layers.length == 1) {
                return;
            }

            // add the rest as splats (which only test the z buffer)
            initAlphaTotals(ccode, rect);
            for (int ii = 1; ii < layers.length; ii++) {

                SharedMesh splat = new SharedMesh("layer" + ii, mesh);
                splat.setIsCollidable(false);

                // initialize the texture state
                TextureState tstate =
                    _ctx.getDisplay().getRenderer().createTextureState();
                int code = layers[ii] - 1;
                Texture ground = getGroundTexture(code);
                if (ground == null) {
                    continue; // something's funny, skip it
                }

                tstate.setTexture(ground, 0);
                Texture alpha = createAlphaTexture(code, rect, false);
                alpha.setApply(Texture.AM_MODULATE);
                tstate.setTexture(alpha, 1);
                alphaTextures.add(alpha);
                splat.setRenderState(tstate);

                // and the z buffer state
                splat.setRenderState(RenderUtil.overlayZBuf);

                // and the alpha state
                splat.setRenderState(RenderUtil.blendAlpha);

                node.attachChild(splat);
            }
        }

        /**
         * Deletes the textures created for this block.
         */
        public void deleteCreatedTextures ()
        {
            // delete the generated alpha textures
            TextureState tstate = _ctx.getRenderer().createTextureState();
            for (Texture texture : alphaTextures) {
                if (texture.getTextureId() > 0) {
                    tstate.deleteTextureId(texture.getTextureId());
                }
            }
            alphaTextures.clear();
        }

        /**
         * Returns the ground texture for the given terrain code, making sure that we always pick
         * the same "random" texture for this splat.
         */
        protected Texture getGroundTexture (int code)
        {
            Texture tex = groundTextures.get(code);
            if (tex == null) {
                groundTextures.put(code, tex = RenderUtil.getGroundTexture(_ctx, code));
            }
            return tex;
        }

        /**
         * Initializes the alpha totals with the alpha values for the base
         * texture.
         */
        protected void initAlphaTotals (int code, Rectangle rect)
        {
            if (_atotals == null) {
                _atotals = new float[TEXTURE_SIZE * TEXTURE_SIZE];
            }
            float step = (SPLAT_SIZE + 1.0f) / TEXTURE_SIZE;
            int x1 = (int)((rect.x - bounds.x) / step),
                y1 = (int)((rect.y - bounds.y) / step),
                x2 = (int)FastMath.ceil((rect.x + rect.width - 1 - bounds.x) /
                    step),
                y2 = (int)FastMath.ceil((rect.y + rect.height - 1 - bounds.y) /
                    step);
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    _atotals[y*TEXTURE_SIZE + x] = getTerrainAlpha(code,
                        bounds.x + x*step, bounds.y + y*step);
                }
            }
        }

        /**
         * Creates and returns an alpha texture for the specified terrain
         * code, using preexisting buffers when possible.
         *
         * @param rect the modified region
         * @param additive if true, the texture will be used for additive blending; use the
         * interpolated alpha directly without scaling by and adjusting totals
         */
        protected Texture createAlphaTexture (int code, Rectangle rect, boolean additive)
        {
            // create the buffer if it doesn't already exist
            ByteBuffer abuf = alphaBuffers.get(code);
            if (abuf == null) {
                alphaBuffers.put(code, abuf = ByteBuffer.allocateDirect(
                    TEXTURE_SIZE*TEXTURE_SIZE));
                rect = bounds;
            }

            // update the affected region of the buffer
            float step = (SPLAT_SIZE + 1.0f) / TEXTURE_SIZE, alpha;
            int x1 = (int)((rect.x - bounds.x) / step),
                y1 = (int)((rect.y - bounds.y) / step),
                x2 = (int)FastMath.ceil((rect.x + rect.width - 1 - bounds.x) / step),
                y2 = (int)FastMath.ceil((rect.y + rect.height - 1 - bounds.y) / step), idx;
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    idx = y*TEXTURE_SIZE + x;
                    alpha = getTerrainAlpha(code, bounds.x + x*step,
                        bounds.y + y*step);
                    if (!additive) {
                        alpha /= (_atotals[idx] += alpha);
                    }
                    abuf.put(idx, (byte)(alpha * 255));
                }
            }

            Texture texture = _ctx.getTextureCache().createTexture();
            abuf.rewind();
            texture.setImage(new Image(Image.A8, TEXTURE_SIZE, TEXTURE_SIZE, abuf));

            // set the filter parameters
            texture.setFilter(Texture.FM_LINEAR);
            texture.setMipmapState(Texture.MM_LINEAR_NEAREST);

            return texture;
        }
    }

    /** Surrounds the board with the most common edge terrain. */
    protected class Skirt extends TriMesh
    {
        public Skirt ()
        {
            super("skirt");
            FloatBuffer vbuf = BufferUtils.createVector3Buffer(8),
                nbuf = BufferUtils.createVector3Buffer(8),
                tbuf = BufferUtils.createVector2Buffer(8);
            IntBuffer ibuf = BufferUtils.createIntBuffer(10);

            float bwidth = _board.getWidth() * TILE_SIZE,
                bheight = _board.getHeight() * TILE_SIZE,
                voffset = 2 * SUB_TILE_SIZE,
                z = getHeightfieldValue(-1, -1),
                twidth = _board.getWidth(),
                theight = _board.getHeight(),
                etiles = EDGE_SIZE / TILE_SIZE,
                toffset = 2 * (1f / BangBoard.HEIGHTFIELD_SUBDIVISIONS);
            vbuf.put(-EDGE_SIZE).put(EDGE_SIZE + bheight).put(z);
            tbuf.put(-etiles).put(etiles + theight);
            vbuf.put(EDGE_SIZE + bwidth).put(EDGE_SIZE + bheight).put(z);
            tbuf.put(etiles + twidth).put(etiles + theight);
            vbuf.put(-voffset).put(bheight + voffset).put(z);
            tbuf.put(-toffset).put(theight + toffset);
            vbuf.put(bwidth + voffset).put(bheight + voffset).put(z);
            tbuf.put(twidth + toffset).put(theight + toffset);
            vbuf.put(-voffset).put(-voffset).put(z);
            tbuf.put(-toffset).put(-toffset);
            vbuf.put(bwidth + voffset).put(-voffset).put(z);
            tbuf.put(twidth + toffset).put(-toffset);
            vbuf.put(-EDGE_SIZE).put(-EDGE_SIZE).put(z);
            tbuf.put(-etiles).put(-etiles);
            vbuf.put(EDGE_SIZE + bwidth).put(-EDGE_SIZE).put(z);
            tbuf.put(etiles + twidth).put(-etiles);

            for (int ii = 0; ii < 8; ii++) {
                nbuf.put(0f).put(0f).put(1f);
            }

            ibuf.put(3).put(0).put(2).put(6).put(4);
            ibuf.put(7).put(5).put(1).put(3).put(0);

            reconstruct(vbuf, nbuf, null, tbuf, ibuf);
            getBatch(0).setMode(TriangleBatch.TRIANGLE_STRIP);

            setModelBound(new BoundingBox());
            updateModelBound();
            updateTexture();
        }

        /**
         * Updates the height of the skirt's vertices to match the current
         * edge height.
         */
        public void updateVertices ()
        {
            FloatBuffer vbuf = getVertexBuffer(0);
            float z = getHeightfieldValue(-1, -1);
            for (int ii = 0; ii < 8; ii++) {
                vbuf.put(ii*3+2, z);
            }
            updateModelBound();
        }

        /**
         * Updates the skirt's texture to match the most common edge terrain.
         */
        public void updateTexture ()
        {
            byte code = _board.getTerrainValue(-1, -1);
            if (code != _tcode) {
                Texture tex = RenderUtil.getGroundTexture(_ctx, _tcode = code);
                if (tex != null) {
                    setRenderState(RenderUtil.createTextureState(_ctx, tex));
                    updateRenderState();
                }
            }
        }

        /** The current terrain code. */
        protected byte _tcode = -1;
    }

    /** The application context. */
    protected BasicContext _ctx;

    /** The board view. */
    protected BoardView _view;

    /** Whether or not we are in editor mode. */
    protected boolean _editorMode;

    /** The board with the terrain. */
    protected BangBoard _board;

    /** The elevation scale specified in the board. */
    protected float _elevationScale;

    /** The array of splat blocks containing the terrain geometry/textures. */
    protected SplatBlock[][] _blocks;

    /** The flat skirt that surrounds the board. */
    protected Skirt _skirt;

    /** Reusable objects for efficiency. */
    protected ColorRGBA _c1 = new ColorRGBA(), _c2 = new ColorRGBA();

    /** The shared texture coordinate buffer for highlights on tiles. */
    protected static FloatBuffer _htbuf;

    /** The planes of the node's bounding box. */
    protected Plane[] _bbplanes;

    /** For each sub-tile, whether the diagonal goes from upper left to lower
     * right instead of lower left to upper right. */
    protected boolean[][] _diags;

    /** A temporary result vector. */
    protected Vector3f _isect = new Vector3f();

    /** Used to store alpha totals when computing alpha maps. */
    protected float[] _atotals;

    /** If true, the shaders didn't link; don't try to compile them again. */
    protected static boolean _disableShaders;

    /** An additive state that doesn't bother with source alpha values. */
    protected static AlphaState _addAlpha;

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
