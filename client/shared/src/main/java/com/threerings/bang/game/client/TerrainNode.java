//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Rectangle;

import java.util.ArrayList;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jme3.bounding.BoundingBox;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.util.BufferUtils;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Interator;
import com.samskivert.util.Invoker;

import com.threerings.jme.util.RenderOrderComparator;

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
 *
 * <h3>jME3 cutover (Phase 2, cluster 3 — REBUILD, migration map §4 risk #3)</h3>
 *
 * The 2,100-line fork terrain renderer is ported to jME3 idioms. What changed:
 * <ul>
 *   <li><b>Geometry types:</b> the fork built every surface from {@code TriMesh}/{@code SharedMesh}
 *   sub-classes ({@code Highlight}, {@code Skirt}, {@code Cursor extends Line}, the splat passes).
 *   jME3 splits mesh data ({@link Mesh}) from the scene node ({@link Geometry}). So {@code Highlight}
 *   and {@code Skirt} now extend {@link Geometry} (each owns its {@link Mesh}); {@code Cursor} is a
 *   {@code Lines}-mode {@link Mesh}; {@code SharedHighlight} is a {@link Geometry} sharing a
 *   Highlight's mesh. {@code VBOInfo}/{@code TriangleBatch}/display-list locking are dropped (jME3
 *   manages VBOs).</li>
 *   <li><b>Render states &rarr; Material:</b> all {@code setRenderState(...)} sites use jME3
 *   {@link Material}s built through {@link RenderUtil} (blend/cull/depth presets, textured Unshaded
 *   materials). {@code MaterialState}/{@code LightState} go away (terrain uses an Unshaded base +
 *   vertex-color shadow modulation).</li>
 *   <li><b>Multi-texture splat (the hard part):</b> the fork blended layers with per-layer alpha
 *   <em>textures</em> via fixed-function multi-unit combine or a GLSL terrain shader
 *   ({@code shaders/terrain.frag}, NUM_SPLATS define) through the now fork-only {@code ShaderCache}.
 *   jME3 has neither. <b>The faithful per-pixel alpha-texture splat is a custom terrain
 *   {@code .j3md} (ColorMap &times; AlphaMap per layer) and is a Phase-4 asset task.</b> For Phase 2
 *   the base layer renders as an opaque textured Geometry and each splat layer as an alpha-blended
 *   textured Geometry sharing the mesh; the CPU alpha-map generation is preserved (as A8
 *   {@link Texture2D}s, carried on each splat material's user-data for the Phase-4 MatDef). The
 *   GLSL shader branch is collapsed into this fixed-function path until the MatDef lands. Flagged
 *   for Phase-4 visual review.</li>
 *   <li><b>Picking:</b> the analytic heightfield ray-cast ({@link #calculatePick},
 *   {@link #getHeightfieldHeight} et al.) is engine-neutral math and is kept verbatim. The
 *   shadow-generation pass that probed piece geometry used fork {@code TrianglePickResults}; it now
 *   uses {@code spatial.collideWith(Ray, CollisionResults)} via the BoardView piece node.</li>
 *   <li><b>GL-capability gating:</b> the fork queried {@code GLContext.getCapabilities()} /
 *   {@code TextureState.getNumberOfFragmentUnits()}; under jME3 these are detail-pref decisions
 *   ({@link #shouldRenderSplats}) until the renderer caps query is wired (Phase 3/4).</li>
 * </ul>
 */
public class TerrainNode extends Node
{
    /**
     * Represents a circle draped over the terrain. jME3: a {@code Lines}-mode {@link Mesh} held by
     * this {@link Geometry} (was a fork {@code Line}).
     */
    public class Cursor extends Geometry
    {
        /** The coordinates of the cursor in node space. */
        public float x, y, radius;

        protected Cursor ()
        {
            super("cursor");
            _mesh = new Mesh();
            _mesh.setMode(Mesh.Mode.LineLoop);
            setMesh(_mesh);

            Material mat = new Material(_ctx.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", ColorRGBA.White);
            RenderUtil.applyLequalZBuf(mat);
            setMaterial(mat);
            setQueueBucket(Bucket.Transparent);

            update();
        }

        /** Sets the position of this cursor and updates it. */
        public void setPosition (float x, float y)
        {
            this.x = x;
            this.y = y;
            update();
        }

        /** Sets the radius of this cursor and updates it. */
        public void setRadius (float radius)
        {
            this.radius = radius;
            update();
        }

        /**
         * Updates the geometry of the cursor to reflect a change in position or in the underlying
         * terrain.
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
            _mesh.setBuffer(VertexBuffer.Type.Position, 3,
                BufferUtils.createFloatBuffer(_verts.toArray(new Vector3f[_verts.size()])));
            _mesh.updateBound();
            _mesh.updateCounts();
        }

        /**
         * Adds the 3D vertex corresponding to the given 2D location to the vertex list.
         */
        protected void addVertex (Vector2f v)
        {
            _verts.add(new Vector3f(v.x, v.y,
                getHeightfieldHeight(v.x, v.y) + 0.1f));
        }

        protected Mesh _mesh;
        protected ArrayList<Vector3f> _verts = new ArrayList<Vector3f>();
        protected Vector2f _v1 = new Vector2f(), _v2 = new Vector2f(),
            _between = new Vector2f();
    }

    /**
     * Represents a highlight draped over the terrain. jME3: a {@link Geometry} owning a {@link Mesh}
     * (was a fork {@code TriMesh}).
     */
    public class Highlight extends Geometry
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
            _mesh = new Mesh();
            setMesh(_mesh);

            setQueueBucket(Bucket.Transparent);

            // a default color-only material so a highlight is renderable before setTextures()
            _defaultMaterial = new Material(_ctx.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
            _defaultMaterial.setColor("Color", new ColorRGBA(ColorRGBA.White));
            RenderUtil.applyBlendAlpha(_defaultMaterial);
            RenderUtil.applyOverlayZBuf(_defaultMaterial);
            RenderUtil.applyBackCull(_defaultMaterial);
            setMaterial(_defaultMaterial);

            // set the vertices, which change according to position and terrain
            if (_onTile) {
                _vwidth = _vheight = BangBoard.HEIGHTFIELD_SUBDIVISIONS + 1;
            } else {
                _vwidth = (int)FastMath.ceil(_width / SUB_TILE_SIZE) + 2;
                _vheight = (int)FastMath.ceil(_height / SUB_TILE_SIZE) + 2;
            }
            _vbuf = BufferUtils.createFloatBuffer(_vwidth * _vheight * 3);
            _mesh.setBuffer(VertexBuffer.Type.Position, 3, _vbuf);

            // set the texture coords, which change for highlights not aligned with tiles
            if (_onTile) {
                if (_htbuf == null) {
                    _htbuf = BufferUtils.createFloatBuffer(_vwidth * _vheight * 2);
                    float step = 1f / BangBoard.HEIGHTFIELD_SUBDIVISIONS;
                    for (int iy = 0; iy < _vheight; iy++) {
                        for (int ix = 0; ix < _vwidth; ix++) {
                            _htbuf.put(ix * step);
                            _htbuf.put(iy * step);
                        }
                    }
                }
                _tbuf = _htbuf;
            } else {
                _tbuf = BufferUtils.createFloatBuffer(_vwidth * _vheight * 2);
            }
            _mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, _tbuf);
            _ibuf = BufferUtils.createIntBuffer((_vwidth - 1) * (_vheight - 1) * 6);
            _mesh.setBuffer(VertexBuffer.Type.Index, 3, _ibuf);

            // update the vertices, indices, and possibly the texture coords
            _mesh.setBound(new BoundingBox());
            updateVertices();
        }

        /** Returns the x tile coordinate of this highlight. */
        public int getTileX ()
        {
            return (int)(x / TILE_SIZE);
        }

        /** Returns the y tile coordinate of this highlight. */
        public int getTileY ()
        {
            return (int)(y / TILE_SIZE);
        }

        /** Sets the position of this highlight in tile coordinates and updates it. */
        public void setPosition (int x, int y)
        {
            setPosition((x + 0.5f) * TILE_SIZE, (y + 0.5f) * TILE_SIZE);
        }

        /** Sets the position of this highlight in world coordinates and updates it. */
        public void setPosition (float x, float y)
        {
            this.x = x;
            this.y = y;
            updateVertices();
        }

        /** Sets the default and hover colors for this highlight. */
        public void setColors (ColorRGBA defaultColor, ColorRGBA hoverColor)
        {
            _defaultColor = defaultColor;
            _hoverColor = hoverColor;
            updateHoverState();
        }

        /**
         * Sets the (single) highlight color, replacing the fork's
         * {@code getBatch(0).getDefaultColor()} access used by shadow-fade callers
         * (EffectHandler/CrossStatus/EditorBoardView). Applies to both default and hover.
         */
        public void setDefaultColor (ColorRGBA color)
        {
            _defaultColor = color;
            _hoverColor = color;
            updateHoverState();
        }

        /**
         * Sets the default and hover materials for this highlight. jME3: was fork
         * {@code TextureState}; now {@link Material} (built via {@link RenderUtil}).
         */
        public void setTextures (Material defaultMaterial, Material hoverMaterial)
        {
            _defaultMaterial = defaultMaterial;
            _hoverMaterial = hoverMaterial;
            updateHoverState();
        }

        /** Sets the hover state of this highlight. */
        public void setHover (boolean hover)
        {
            this.hover = hover;
            updateHoverState();
        }

        /** Sets whether this highlight has normals. */
        public void setHasNormals (boolean normals)
        {
            if (normals && _mesh.getBuffer(VertexBuffer.Type.Normal) == null) {
                _nbuf = BufferUtils.createFloatBuffer(_vwidth * _vheight * 3);
                _mesh.setBuffer(VertexBuffer.Type.Normal, 3, _nbuf);
                updateVertices();
            } else if (!normals) {
                _nbuf = null;
                _mesh.clearBuffer(VertexBuffer.Type.Normal);
            }
        }

        /**
         * Updates the vertices of the highlight to reflect a change in position or in the underlying
         * terrain.
         */
        public void updateVertices ()
        {
            if (_board == null) {
                return;
            }

            FloatBuffer vbuf = _vbuf, nbuf = _nbuf;
            IntBuffer ibuf = _ibuf;
            ibuf.rewind();

            int tx = getTileX(), ty = getTileY();
            Vector3f offset = null;
            setLocalTranslation(0f, 0f, 0f);
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
                    setLocalTranslation(x, y, belev * _elevationScale);
                }
            }

            float x0 = x - _width/2, y0 = y - _height/2;
            int sx0 = (int)(x0 / SUB_TILE_SIZE),
                sy0 = (int)(y0 / SUB_TILE_SIZE);
            Vector3f vertex = new Vector3f();
            for (int sy = sy0, sy1 = sy0 + _vheight, idx = 0; sy < sy1; sy++) {
                for (int sx = sx0, sx1 = sx0 + _vwidth; sx < sx1; sx++) {
                    if (nbuf != null) {
                        if (flat) {
                            BufferUtils.setInBuffer(Vector3f.UNIT_Z, nbuf, idx);
                        } else {
                            getHeightfieldNormal(sx, sy, vertex);
                            BufferUtils.setInBuffer(vertex, nbuf, idx);
                        }
                    }

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
            _mesh.getBuffer(VertexBuffer.Type.Position).updateData(vbuf);
            if (nbuf != null) {
                _mesh.getBuffer(VertexBuffer.Type.Normal).updateData(nbuf);
            }
            _mesh.getBuffer(VertexBuffer.Type.Index).updateData(ibuf);
            _mesh.updateBound();
            _mesh.updateCounts();
            // jME3 picking against this geometry is enabled only when it is raised off the terrain
            // (flat or offset); collision is mesh-based via collideWith().
            _collidable = flat || offset != null;

            if (_onTile || flat) {
                return;
            }
            FloatBuffer tbuf = _tbuf;
            tbuf.rewind();
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
            _mesh.getBuffer(VertexBuffer.Type.TexCoord).updateData(tbuf);
        }

        /** Whether this highlight should participate in mesh collision picking. */
        public boolean isCollidable ()
        {
            return _collidable;
        }

        /**
         * Updates the material/color associated with the hover status.
         */
        protected void updateHoverState ()
        {
            Material mat = hover ? _hoverMaterial : _defaultMaterial;
            ColorRGBA color = hover ? _hoverColor : _defaultColor;
            if (mat != null) {
                RenderUtil.applyBlendAlpha(mat);
                RenderUtil.applyOverlayZBuf(mat);
                RenderUtil.applyBackCull(mat);
                if (mat.getMaterialDef().getMaterialParam("Color") != null) {
                    mat.setColor("Color", new ColorRGBA(color));
                }
                setMaterial(mat);
            }
        }

        protected Mesh _mesh;
        protected FloatBuffer _vbuf, _nbuf, _tbuf;
        protected IntBuffer _ibuf;
        protected boolean _collidable;

        /** If true, the highlight will always be aligned with a tile. */
        protected boolean _onTile;

        /** The dimensions of the highlight in world units. */
        protected float _width, _height;

        /** The dimensions of the highlight in vertices. */
        protected int _vwidth, _vheight;

        /** The colors for normal and hover modes. */
        protected ColorRGBA _defaultColor = ColorRGBA.White,
            _hoverColor = ColorRGBA.White;

        /** The materials for normal and hover modes. */
        protected Material _defaultMaterial, _hoverMaterial;

        /** The zoffset for each layer. */
        protected static final float LAYER_OFFSET = TILE_SIZE/1000;
    }

    /**
     * Allows sharing the geometry of terrain highlights. jME3: a {@link Geometry} sharing a
     * Highlight's {@link Mesh} (was a fork {@code SharedMesh}).
     */
    public static class SharedHighlight extends Geometry
    {
        public SharedHighlight (String name, Highlight target)
        {
            super(name, target.getMesh());
            _target = target;
            setMaterial(target.getMaterial());
        }

        /** Returns the highlight whose mesh this shares. */
        public Highlight getTarget ()
        {
            return _target;
        }

        @Override // documentation inherited
        public int collideWith (com.jme3.collision.Collidable other, CollisionResults results)
        {
            // the target mesh participates in picking only if it does not lie on the terrain
            if (_target.isCollidable()) {
                return super.collideWith(other, results);
            }
            return 0;
        }

        protected Highlight _target;
    }

    /**
     * An interface for progress update callbacks.
     */
    public interface ProgressListener
    {
        /**
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
    }

    /**
     * Initializes the terrain geometry using terrain data from the given board and saves the board
     * reference for later updates.
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

        // splat shading currently runs through the fixed-function path (Phase-4 splat MatDef).
        boolean useShaders = false;

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

        // compute the bounding box planes used for ray casting
        computeBoundingBoxPlanes();
    }

    /**
     * Releases the resources created by this node. jME3 reclaims meshes/textures with the node.
     */
    public void cleanup ()
    {
        // no explicit GL deletes needed under jME3.
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
     * Refreshes a region of the heightfield as specified in sub-tile coordinates.
     */
    public void refreshHeightfield (int x1, int y1, int x2, int y2)
    {
        float elevationScale = _board.getElevationScale(TILE_SIZE);
        if (_elevationScale != elevationScale) {
            _elevationScale = elevationScale;
            computeBoundingBoxPlanes();
        }

        boolean updateEdges = (x1 <= 0 || y1 <= 0 ||
            x2 >= _board.getHeightfieldWidth() - 1 ||
            y2 >= _board.getHeightfieldHeight() - 1);

        Rectangle rect = new Rectangle(x1-1, y1-1, x2-x1+3, y2-y1+3);
        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                SplatBlock block = _blocks[x][y];
                if (block == null) {
                    continue;
                }
                if (block.ebounds.intersects(rect) ||
                        (updateEdges && block.isOnEdge())) {
                    block.refreshGeometry(block.ebounds.intersection(rect));
                    block.mesh.updateBound();
                }
            }
        }
        if (updateEdges && _skirt != null) {
            _skirt.updateVertices();
        }
    }

    /**
     * Refreshes the entire terrain (textures).
     */
    public void refreshTerrain ()
    {
        refreshTerrain(0, 0, _board.getHeightfieldWidth() - 1,
            _board.getHeightfieldHeight() - 1);
    }

    /**
     * Refreshes a region of the terrain (textures) in sub-tile coordinates.
     */
    public void refreshTerrain (int x1, int y1, int x2, int y2)
    {
        Rectangle rect = new Rectangle(x1, y1, x2-x1+1, y2-y1+1);
        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                SplatBlock block = _blocks[x][y];
                if (block == null) {
                    continue;
                }
                Rectangle isect = block.bounds.intersection(rect);
                if (!isect.isEmpty()) {
                    block.refreshSplats(isect);
                }
            }
        }
        if (_skirt != null) {
            _skirt.updateTexture();
        }
    }

    /**
     * Refreshes the shadow colors.
     */
    public void refreshShadows ()
    {
        for (int x = 0; x < _blocks.length; x++) {
            for (int y = 0; y < _blocks[x].length; y++) {
                if (_blocks[x][y] != null) {
                    _blocks[x][y].refreshColors();
                }
            }
        }
    }

    /**
     * Refreshes the splat shaders. jME3: the GLSL splat shader is a Phase-4 MatDef; no-op for now.
     */
    public void refreshShaders ()
    {
        // Phase 4: re-select the terrain splat MatDef's defines once it is authored.
    }

    /**
     * Generates the terrain shadow buffer by ray-casting the terrain and pieces toward the light.
     */
    public void generateShadows (byte[] shadows, ProgressListener listener)
    {
        int hfwidth = _board.getHeightfieldWidth(),
            hfheight = _board.getHeightfieldHeight();

        updateGeometricState();

        float azimuth = _board.getLightAzimuth(0),
            elevation = _board.getLightElevation(0),
            hstep = _elevationScale, theight,
            total = hfwidth * hfheight;
        Vector3f origin = new Vector3f(),
            dir = new Vector3f(FastMath.cos(azimuth) * FastMath.cos(elevation),
                FastMath.sin(azimuth) * FastMath.cos(elevation),
                FastMath.sin(elevation));
        Ray ray = new Ray(origin, dir);
        CollisionResults results = new CollisionResults();
        for (int x = 0, complete = 0; x < hfwidth; x++) {
            for (int y = 0; y < hfheight; y++, complete++) {
                if ((complete % 32) == 0) {
                    listener.update(complete / total);
                }

                getHeightfieldVertex(x, y, origin);
                theight = origin.z;
                int sheight = (int)((getSelfShadowHeight(ray) - theight) /
                    hstep);

                int lower = 0, upper = 256, middle = 128;
                while (middle > lower && middle < upper) {
                    origin.z = theight + middle * hstep;
                    results.clear();
                    _view.getPieceNode().collideWith(ray, results);
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
     * Creates and returns a cursor over this terrain.
     */
    public Cursor createCursor ()
    {
        return new Cursor();
    }

    public Highlight createHighlight (int x, int y, boolean overPieces)
    {
        return createHighlight(x, y, overPieces, false);
    }

    public Highlight createHighlight (
            int x, int y, boolean overPieces, byte layer)
    {
        return new Highlight(
                x, y, overPieces && Config.floatHighlights, false, layer);
    }

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
        // a pick can arrive before the terrain heightfield is built (e.g. an early click on
        // game entry); _diags is allocated only when the heightfield is generated, so report
        // no intersection until then rather than NPE deep in getHeightfieldHeight.
        if (_diags == null) {
            return false;
        }
        if (getWorldBound() == null || getWorldBound().intersects(ray)) {
            // bounding box test passed or unavailable; continue
        } else {
            return false;
        }

        Vector3f origin = ray.getOrigin(), dir = ray.getDirection();
        if (dir.x == 0f && dir.y == 0f) {
            float height = getHeightfieldHeight(origin.x, origin.y);
            if ((origin.z > height) != (dir.z < 0f)) {
                return false;
            }
            result.set(origin.x, origin.y, height);
            return true;
        }

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
     * Returns the interpolated height at the specified node-space coordinates.
     */
    public float getHeightfieldHeight (float x, float y)
    {
        float stscale = BangBoard.HEIGHTFIELD_SUBDIVISIONS / TILE_SIZE;
        x *= stscale;
        y *= stscale;

        int fx = (int)FastMath.floor(x), cx = (int)FastMath.ceil(x),
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y),
            dx = fx + 2, dy = fy + 2;
        float ff = getHeightfieldValue(fx, fy),
            fc = getHeightfieldValue(fx, cy),
            cf = getHeightfieldValue(cx, fy),
            cc = getHeightfieldValue(cx, cy),
            ax = x - fx, ay = y - fy;

        if (_diags != null && dx >= 0 && dy >= 0 && dy < _diags.length &&
            dx < _diags[dy].length && _diags[dy][dx]) {
            if ((1f - ax) < ay) {
                return FastMath.interpolateLinear(ax, FastMath.interpolateLinear(ay, fc + cf - cc, fc),
                    FastMath.interpolateLinear(ay, cf, cc));
            } else {
                return FastMath.interpolateLinear(ax, FastMath.interpolateLinear(ay, ff, fc),
                    FastMath.interpolateLinear(ay, cf, cf + fc - ff));
            }
        } else {
            if (ax < ay) {
                return FastMath.interpolateLinear(ax, FastMath.interpolateLinear(ay, ff, fc),
                    FastMath.interpolateLinear(ay, cc + ff - fc, cc));
            } else {
                return FastMath.interpolateLinear(ax, FastMath.interpolateLinear(ay, ff, ff + cc - cf),
                    FastMath.interpolateLinear(ay, cf, cc));
            }
        }
    }

    /**
     * Returns the interpolated normal at the specified node-space coordinates.
     */
    public Vector3f getHeightfieldNormal (float x, float y)
    {
        float stscale = BangBoard.HEIGHTFIELD_SUBDIVISIONS / TILE_SIZE;
        x *= stscale;
        y *= stscale;

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

        fffc.interpolateLocal(ff, fc, ay);
        cfcc.interpolateLocal(cf, cc, ay);
        result.interpolateLocal(fffc, cfcc, ax);
        result.normalizeLocal();
        return result;
    }

    /**
     * Computes the heightfield vertex at the specified sub-tile location.
     */
    public void getHeightfieldVertex (int x, int y, Vector3f result)
    {
        result.set(x * SUB_TILE_SIZE, y * SUB_TILE_SIZE,
            getHeightfieldValue(x, y));
    }

    /**
     * Returns the scaled height of the specified sub-tile location.
     */
    public float getHeightfieldValue (int x, int y)
    {
        return (_board == null) ?
            0f : _board.getHeightfieldValue(x, y) * _elevationScale;
    }

    /**
     * Computes the interpolated shadow height at the specified world coordinates.
     */
    public float getShadowHeight (float x, float y)
    {
        float stscale = BangBoard.HEIGHTFIELD_SUBDIVISIONS / TILE_SIZE;
        x *= stscale;
        y *= stscale;

        int fx = (int)FastMath.floor(x), cx = (int)FastMath.ceil(x),
            fy = (int)FastMath.floor(y), cy = (int)FastMath.ceil(y);
        float ff = getShadowHeight(fx, fy),
            fc = getShadowHeight(fx, cy),
            cf = getShadowHeight(cx, fy),
            cc = getShadowHeight(cx, cy),
            ax = x - fx, ay = y - fy;

        return FastMath.interpolateLinear(ax, FastMath.interpolateLinear(ay, ff, fc),
            FastMath.interpolateLinear(ay, cf, cc));
    }

    protected float getShadowHeight (int x, int y)
    {
        return (_board.getHeightfieldValue(x, y) +
            _board.getShadowValue(x, y)) * _elevationScale;
    }

    protected float getSelfShadowHeight (Ray ray)
    {
        Vector3f origin = ray.getOrigin(), dir = ray.getDirection();
        if (dir.x == 0f && dir.y == 0f) {
            return -Float.MAX_VALUE;
        }

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
     * Determines whether the collision results contain any static-shadow-casting piece geometry.
     * jME3: was fork {@code TrianglePickResults}; now {@link CollisionResults} from
     * {@code collideWith}.
     */
    protected boolean containTriangles (CollisionResults results)
    {
        for (int i = 0, size = results.size(); i < size; i++) {
            Geometry geom = results.getCollision(i).getGeometry();
            if (geom == null) {
                continue;
            }
            Object sprite = _view.getPieceSprite(geom);
            if (sprite == null || (sprite instanceof PieceSprite &&
                                   ((PieceSprite)sprite).getShadowType() ==
                                   PieceSprite.Shadow.STATIC)) {
                return true;
            }
        }
        return false;
    }

    protected void getHeightfieldNormal (int x, int y, Vector3f result)
    {
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
        // scale by the Esc-options "shadow darkness" slider (clamped: 200% would over-darken past
        // black); the terrain shadow is baked into vertex colors, so refreshShadows() re-runs this
        // when the slider changes for live feedback.
        return FastMath.clamp(
            (value / total) * _board.getShadowIntensity() * BoardView.shadowScale(), 0f, 1f);
    }

    protected void computeBoundingBoxPlanes ()
    {
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

    protected void computeEntranceExit (Ray ray, Vector3f entrance,
        Vector3f exit)
    {
        float tentrance = 0f, texit = -1f;
        for (int ii = 0; ii < _bbplanes.length; ii++) {
            Vector3f normal = _bbplanes[ii].getNormal();
            float ndd = normal.dot(ray.direction);
            if (FastMath.abs(ndd) < FastMath.FLT_EPSILON) {
                continue;
            }
            float t = (-normal.dot(ray.origin) +
                _bbplanes[ii].getConstant()) / ndd;
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

    protected boolean boundsContain (Vector3f pt, int skip)
    {
        for (int ii = 0; ii < _bbplanes.length; ii++) {
            if (ii % 3 != skip &&
                _bbplanes[ii].whichSide(pt) == Plane.Side.Negative) {
                return false;
            }
        }
        return true;
    }

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

    protected static int getBoundaryIndex (float v, float step)
    {
        int base = (int)Math.floor(v / step), adjust;
        if (epsilonEquals(v, base*step)) {
            adjust = 0;
        } else if (epsilonEquals(v, (base+1)*step)) {
            adjust = 2;
        } else {
            adjust = 1;
        }
        return base*2 + adjust;
    }

    protected static boolean epsilonEquals (float a, float b)
    {
        return FastMath.abs(a - b) < 0.001f;
    }

    protected static float getDistance (Vector2f v1, Vector2f v2)
    {
        float dx = v1.x - v2.x, dy = v1.y - v2.y;
        return FastMath.sqrt(dx*dx + dy*dy);
    }

    protected static int iclamp (int value, int min, int max)
    {
        return (value <= min) ? min : (value >= max ? (max - 1) : value);
    }

    /**
     * Whether we should render terrain splats (vs. a single base texture per block).
     */
    protected static boolean shouldRenderSplats ()
    {
        return BangPrefs.isMediumDetail();
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

        protected int _x, _y;
        protected boolean _useShaders;
    }

    /**
     * Contains all the state associated with a splat block (a collection of splats covering a single
     * block of terrain).
     */
    protected class SplatBlock
    {
        /** The node containing the splat geometries. */
        public Node node;

        /** The bounds of this block in sub-tile coordinates and the bounds that include the edge. */
        public Rectangle bounds, ebounds;

        /** The shared, unparented terrain mesh. */
        public Mesh mesh;

        /** The vertex, normal, and color buffers. */
        public FloatBuffer vbuf, nbuf, cbuf, tbuf0;

        /** The index buffer. */
        public IntBuffer ibuf;

        /** Maps terrain codes to ground textures. */
        public HashIntMap<Texture2D> groundTextures = new HashIntMap<Texture2D>();

        /** Maps terrain codes to alpha texture buffers. */
        public HashIntMap<ByteBuffer> alphaBuffers = new HashIntMap<ByteBuffer>();

        /** Contains the code for each terrain layer (plus one). */
        public int[] layers;

        /** The generated alpha textures. */
        public ArrayList<Texture2D> alphaTextures = new ArrayList<Texture2D>();

        /** Whether or not we're using shaders. */
        public boolean useShaders;

        public SplatBlock (int sx, int sy, boolean useShaders)
        {
            node = new Node("block_" + sx + "_" + sy);
            this.useShaders = useShaders;

            boolean le = (sx == 0), re = (sx == _blocks.length - 1),
                be = (sy == 0), te = (sy == _blocks[0].length - 1);

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
            ibuf = BufferUtils.createIntBuffer(
                (vwidth - 1) * (vheight - 1) * 2 * 3);

            bounds = new Rectangle(vx, vy, bwidth, bheight);
            ebounds = new Rectangle(vx - (le ? 2 : 0), vy - (be ? 2 : 0),
                vwidth, vheight);
            refreshGeometry(ebounds);

            refreshColors();

            FloatBuffer
                tb0 = BufferUtils.createFloatBuffer(vwidth*vheight*2),
                tb1 = BufferUtils.createFloatBuffer(vwidth*vheight*2);
            float step0 = 1.0f / BangBoard.HEIGHTFIELD_SUBDIVISIONS,
                step1 = 1.0f / (SPLAT_SIZE+1);
            for (int y = (be ? -2 : 0), ymax = y + vheight; y < ymax; y++) {
                for (int x = (le ? -2 : 0), xmax = x + vwidth; x < xmax; x++) {
                    tb0.put(x * step0);
                    tb0.put(y * step0);

                    tb1.put(0.5f*step1 + iclamp(x, 0, bwidth) * step1);
                    tb1.put(0.5f*step1 + iclamp(y, 0, bheight) * step1);
                }
            }
            tbuf0 = tb0;

            mesh = new Mesh();
            mesh.setMode(Mesh.Mode.Triangles);
            vbuf.rewind();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, vbuf);
            nbuf.rewind();
            mesh.setBuffer(VertexBuffer.Type.Normal, 3, nbuf);
            cbuf.rewind();
            mesh.setBuffer(VertexBuffer.Type.Color, 4, cbuf);
            tb0.rewind();
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, tb0);
            if (shouldRenderSplats()) {
                tb1.rewind();
                mesh.setBuffer(VertexBuffer.Type.TexCoord2, 2, tb1);
            }
            ibuf.rewind();
            mesh.setBuffer(VertexBuffer.Type.Index, 3, ibuf);
            mesh.setBound(new BoundingBox());
            mesh.updateBound();
            mesh.updateCounts();

            refreshSplats(bounds);
        }

        /**
         * Finishes creation of the block on the main thread (attaches it to the scene).
         */
        public void finishCreation ()
        {
            attachChild(node);
        }

        /**
         * Checks whether this block includes an edge.
         */
        public boolean isOnEdge ()
        {
            return !ebounds.equals(bounds);
        }

        /**
         * Refreshes all of the shader parameters. jME3: Phase-4 splat MatDef; no-op.
         */
        public void refreshShaders ()
        {
        }

        /**
         * Refreshes the geometry covered by the specified rectangle (in sub-tile coordinates).
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
                        boolean prev = _diags[y+1][x];
                        if (prev != _diags[y+1][x+1] &&
                            ((prev && (urll - ullr < 0.001f)) ||
                            (!prev && (ullr - urll < 0.001f)))) {
                            _diags[y+1][x+1] = prev;
                        }
                    }
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
            if (mesh != null) {
                vbuf.rewind();
                mesh.getBuffer(VertexBuffer.Type.Position).updateData(vbuf);
                nbuf.rewind();
                mesh.getBuffer(VertexBuffer.Type.Normal).updateData(nbuf);
                ibuf.rewind();
                mesh.getBuffer(VertexBuffer.Type.Index).updateData(ibuf);
            }
        }

        /**
         * Refreshes the entire color (shadow) buffer.
         */
        public void refreshColors ()
        {
            int idx = 0;
            ColorRGBA color = new ColorRGBA();
            for (int y = ebounds.y, ymax = y+ebounds.height; y < ymax; y++) {
                for (int x = ebounds.x, xmax = x+ebounds.width; x < xmax;
                        x++) {
                    color.interpolateLocal(ColorRGBA.White, ColorRGBA.Black,
                        getShadowValue(x, y));
                    BufferUtils.setInBuffer(color, cbuf, idx++);
                }
            }
            if (mesh != null) {
                cbuf.rewind();
                mesh.getBuffer(VertexBuffer.Type.Color).updateData(cbuf);
            }
        }

        /**
         * Refreshes the splats according to terrain changes over the specified rectangle.
         */
        public void refreshSplats (Rectangle rect)
        {
            node.detachAllChildren();
            alphaTextures.clear();

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

            buildFixedLayers(rect);

            for (Interator it = alphaBuffers.keys(); it.hasNext(); ) {
                if (!codes.containsKey(it.nextInt()+1)) {
                    it.remove();
                }
            }
        }

        protected void buildFixedLayers (Rectangle rect)
        {
            // base layer: most common terrain, opaque, writes depth. Uses the terrain splat
            // MatDef with no AlphaMap (fully opaque) so it shares the lighting/fog path with the
            // splat layers.
            Geometry base = new Geometry("base", mesh);
            int ccode = layers[0] - 1;
            Texture2D gtex = getGroundTexture(ccode);
            Material bmat = RenderUtil.createTerrainMaterial(_ctx, gtex, null);
            RenderUtil.applyBackCull(bmat);
            applyFog(bmat);
            base.setMaterial(bmat);
            node.attachChild(base);

            if (!shouldRenderSplats() || layers.length == 1) {
                return;
            }

            // splat layers: per-pixel alpha-blended via the A8 splat alpha map, depth-test only.
            initAlphaTotals(ccode, rect);
            for (int ii = 1; ii < layers.length; ii++) {
                int code = layers[ii] - 1;
                Texture2D ground = getGroundTexture(code);
                if (ground == null) {
                    continue;
                }
                // the faithful multi-texture splat: this layer's per-pixel coverage comes from its
                // A8 alpha map (sampled in 0..1 block space via TexCoord2 by TerrainSplat.frag),
                // so terrain types blend at their boundaries instead of overwriting opaquely.
                Texture2D alpha = createAlphaTexture(code, rect, false);
                alphaTextures.add(alpha);

                Geometry splat = new Geometry("layer" + ii, mesh);
                Material smat = RenderUtil.createTerrainMaterial(_ctx, ground, alpha);
                RenderUtil.applyBlendAlpha(smat);
                RenderUtil.applyOverlayZBuf(smat);
                RenderUtil.applyBackCull(smat);
                applyFog(smat);

                splat.setMaterial(smat);
                splat.setQueueBucket(Bucket.Transparent);
                // tag so the main viewport's RenderOrderComparator draws this transparent splat
                // overlay before the (default-order) move/attack highlights, instead of
                // interleaving with them by centroid distance (BUG-31).
                splat.setUserData(RenderOrderComparator.RENDER_ORDER,
                    RenderOrderComparator.TERRAIN_ORDER);
                node.attachChild(splat);
            }
        }

        /** Applies the board's fog ambience to a terrain layer material, if any. */
        protected void applyFog (Material mat)
        {
            if (_board == null) {
                return;
            }
            float density = _board.getFogDensity();
            if (density <= 0f) {
                return;
            }
            int fc = _board.getFogColor();
            mat.setBoolean("UseFog", true);
            mat.setColor("FogColor", new ColorRGBA(
                ((fc >> 16) & 0xFF) / 255f, ((fc >> 8) & 0xFF) / 255f,
                (fc & 0xFF) / 255f, 1f));
            // the fork density is per board-unit; scale to the view-space depth fog used here.
            mat.setFloat("FogDensity", density);
        }

        /**
         * Returns the ground texture for the given terrain code (stable per splat).
         */
        protected Texture2D getGroundTexture (int code)
        {
            Texture2D tex = groundTextures.get(code);
            if (tex == null) {
                groundTextures.put(code, tex = RenderUtil.getGroundTexture(_ctx, code));
            }
            return tex;
        }

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
         * Creates an A8 alpha {@link Texture2D} for the specified terrain code.
         */
        protected Texture2D createAlphaTexture (int code, Rectangle rect, boolean additive)
        {
            ByteBuffer abuf = alphaBuffers.get(code);
            if (abuf == null) {
                alphaBuffers.put(code, abuf = ByteBuffer.allocateDirect(
                    TEXTURE_SIZE*TEXTURE_SIZE));
                rect = bounds;
            }

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

            abuf.rewind();
            Image img = new Image(Image.Format.Alpha8, TEXTURE_SIZE, TEXTURE_SIZE, abuf,
                com.jme3.texture.image.ColorSpace.Linear);
            Texture2D texture = new Texture2D(img);
            texture.setName("terrain.alpha." + code);
            texture.setMagFilter(MagFilter.Bilinear);
            texture.setMinFilter(MinFilter.BilinearNearestMipMap);
            return texture;
        }
    }

    /** Surrounds the board with the most common edge terrain. jME3: a {@link Geometry}. */
    protected class Skirt extends Geometry
    {
        public Skirt ()
        {
            super("skirt");
            _mesh = new Mesh();
            _mesh.setMode(Mesh.Mode.TriangleStrip);
            setMesh(_mesh);

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

            vbuf.rewind(); nbuf.rewind(); tbuf.rewind(); ibuf.rewind();
            _mesh.setBuffer(VertexBuffer.Type.Position, 3, vbuf);
            _mesh.setBuffer(VertexBuffer.Type.Normal, 3, nbuf);
            _mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, tbuf);
            _mesh.setBuffer(VertexBuffer.Type.Index, 2, ibuf);
            _mesh.setBound(new BoundingBox());
            _mesh.updateBound();
            _mesh.updateCounts();
            _vbuf = vbuf;
            updateTexture();
        }

        /**
         * Updates the height of the skirt's vertices to match the current edge height.
         */
        public void updateVertices ()
        {
            float z = getHeightfieldValue(-1, -1);
            for (int ii = 0; ii < 8; ii++) {
                _vbuf.put(ii*3+2, z);
            }
            _vbuf.rewind();
            _mesh.getBuffer(VertexBuffer.Type.Position).updateData(_vbuf);
            _mesh.updateBound();
        }

        /**
         * Updates the skirt's texture to match the most common edge terrain.
         */
        public void updateTexture ()
        {
            byte code = _board.getTerrainValue(-1, -1);
            if (code != _tcode) {
                Texture2D tex = RenderUtil.getGroundTexture(_ctx, _tcode = code);
                if (tex != null) {
                    Material mat = RenderUtil.createTextureMaterial(_ctx, tex);
                    RenderUtil.applyBackCull(mat);
                    setMaterial(mat);
                }
            }
        }

        protected Mesh _mesh;
        protected FloatBuffer _vbuf;

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

    /** For each sub-tile, whether the diagonal goes from upper left to lower right instead of lower
     * left to upper right. */
    protected boolean[][] _diags;

    /** A temporary result vector. */
    protected Vector3f _isect = new Vector3f();

    /** Used to store alpha totals when computing alpha maps. */
    protected float[] _atotals;

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
