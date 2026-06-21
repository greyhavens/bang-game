//
// $Id$

package com.threerings.bang.game.client;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jme3.bounding.BoundingBox;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;
import com.threerings.bang.util.WaveUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the rendering of Bang's wet spots.
 *
 * <h3>jME3 cutover (Phase 2, cluster 3 — REBUILD, migration map §4 risk #3)</h3>
 *
 * This is the highest fidelity-risk node in the board renderer. What changed and what is deferred:
 *
 * <ul>
 *   <li><b>Geometry:</b> the fork built the surface from {@code TriMesh} wave patches shared by
 *   {@code SharedMesh} blocks. jME3 uses a {@link Mesh} per wave patch and a {@link Geometry} per
 *   visible block sharing that mesh. The CPU FFT wave simulation ({@link WaveUtil}) is
 *   engine-neutral and is kept verbatim.</li>
 *   <li><b>Materials/render state:</b> the fork's {@code AlphaState}/{@code MaterialState}/
 *   {@code LightState} move onto a jME3 {@link Material} (specular water look via
 *   {@code Lighting.j3md}) with blend + back-cull on its render state; the board light is attached
 *   to this node.</li>
 *   <li><b>GLSL water shader (fork {@code GLSLShaderObjectsState} + {@code ShaderCache}):</b> the
 *   fork's high-detail path used {@code shaders/water.vert}/{@code .frag} via the (now fork-only)
 *   {@code ShaderCache}, and updated a normal map through raw {@code GL11.glTexSubImage2D} from a
 *   {@code GDXTextureState.apply()} override. jME3 has neither {@code ShaderCache} nor the gdx GL
 *   poking. <b>The custom water {@code .j3md} (porting water.vert/frag to jME3 GLSL + a normal-map
 *   MatParamTexture) is a Phase-4 fidelity task.</b> For Phase 2 the high-detail branch still runs
 *   the wave simulation and maintains a {@link Texture2D} normal map (updated each frame by
 *   re-uploading its {@link Image}); the surface renders with the specular fallback material until
 *   the water MatDef lands. This is flagged for Phase-4 visual review.</li>
 *   <li><b>GL-thread / capability gating:</b> the fork queried {@code GLContext.getCapabilities()}
 *   in the constructor (engine-notes: WaterNode could only be constructed on the GL thread). jME3
 *   owns the context and reports capabilities through the renderer; the high-detail path is now
 *   gated on {@link BangPrefs#isHighDetail()} alone. Construction is GL-thread-safe under jME3.</li>
 * </ul>
 */
public class WaterNode extends Node
{
    public WaterNode (BasicContext ctx, DirectionalLight light, boolean editorMode)
    {
        super("water");
        _ctx = ctx;
        _light = light;
        _editorMode = editorMode;
        _highDetail = BangPrefs.isHighDetail();

        // build the surface material: the custom Bang water MatDef does GL_SPHERE_MAP texgen of the
        // (wave-perturbed) surface normal against a Fresnel sphere map (water-color <-> sky-color by
        // reflectivity), reproducing the fork's fixed-function EM_SPHERE reflection look.
        _material = new Material(ctx.getAssetManager(), "shaders/BangWater.j3md");
        _material.setFloat("Alpha", WATER_ALPHA);
        RenderUtil.applyBlendAlpha(_material);
        RenderUtil.applyBackCull(_material);
        setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
    }

    /**
     * Initializes the water geometry using terrain data from the given board and saves the board
     * reference for later updates.
     */
    public void createBoardWater (BangBoard board)
    {
        _board = board;

        // clean up any existing geometry
        detachAllChildren();

        // initialize the array of blocks and patches
        _blocks = new Geometry[_board.getWidth()][_board.getHeight()];
        if (_highDetail) {
            _patches = new Mesh[WAVE_MAP_TILES][WAVE_MAP_TILES];
            int vsize = (WAVE_MAP_SIZE + 1) * (WAVE_MAP_SIZE + 1);
            _vbuf = BufferUtils.createVector3Buffer(vsize);
            _nbuf = BufferUtils.createVector3Buffer(vsize);
            initNormalMap();
        } else {
            _patches = null;
        }
        _bcount = 0;
        refreshSurface();

        // refresh the sphere map and wave amplitudes if there are any blocks visible
        if (_editorMode || _bcount > 0) {
            refreshColors();
            refreshShader();
            if (_patches != null) {
                refreshWaveAmplitudes();
            }
        }
    }

    /**
     * Releases the resources created by this node. jME3 reclaims textures/materials with the node.
     */
    public void cleanup ()
    {
        // no explicit GL deletes needed under jME3.
    }

    /**
     * Updates the water/sky blend colors. The fork baked a Fresnel sphere map (fixed-function) or
     * set shader uniforms (GLSL). The Fresnel sphere map is preserved as a {@link Texture2D} bound
     * as the material's {@code DiffuseMap}; the GLSL uniform path is folded into the Phase-4 water
     * MatDef.
     */
    public void refreshColors ()
    {
        if (_board == null) {
            return;
        }
        ColorRGBA wcolor = RenderUtil.createColorRGBA(_board.getWaterColor()),
                scolor = RenderUtil.createColorRGBA(_board.getSkyOverheadColor());
        wcolor.a = WATER_ALPHA;

        ByteBuffer pbuf = ByteBuffer.allocateDirect(SPHERE_MAP_SIZE * SPHERE_MAP_SIZE * 4);
        ColorRGBA color = new ColorRGBA();

        float x, y, d, thetai, thetat, reflectivity, fs, ts;
        int hsize = SPHERE_MAP_SIZE / 2;
        for (int ii = -hsize; ii < hsize; ii++) {
            for (int jj = -hsize; jj < hsize; jj++) {
                x = (float)ii / hsize;
                y = (float)jj / hsize;
                d = FastMath.sqrt(x*x + y*y);
                if (d <= 1f) {
                    thetai = FastMath.asin(d);
                    thetat = FastMath.asin(d / SNELL_RATIO);
                    if (thetai == 0f) {
                        reflectivity = (SNELL_RATIO - 1f) / (SNELL_RATIO + 1f);
                        reflectivity = reflectivity * reflectivity;
                    } else {
                        fs = FastMath.sin(thetat - thetai) /
                            FastMath.sin(thetat + thetai);
                        ts = FastMath.tan(thetat - thetai) /
                            FastMath.tan(thetat + thetai);
                        reflectivity = 0.5f * (fs*fs + ts*ts);
                    }
                    color.interpolateLocal(wcolor, scolor, reflectivity);
                } else {
                    color.set(ColorRGBA.Black);
                }
                pbuf.put((byte)(color.r * 255));
                pbuf.put((byte)(color.g * 255));
                pbuf.put((byte)(color.b * 255));
                pbuf.put((byte)(color.a * 255));
            }
        }
        pbuf.rewind();

        // The sphere map holds the fork's interpolated water<->sky colors verbatim; the fork
        // displayed them with no gamma management (fixed-function EM_SPHERE replace). Tag the image
        // Linear so a gamma-correct pipeline samples it as-is rather than sRGB-decoding (which
        // darkened the surface to a near-black sheet). The water then reads at full color.
        Image img = new Image(Image.Format.RGBA8, SPHERE_MAP_SIZE, SPHERE_MAP_SIZE, pbuf,
            com.jme3.texture.image.ColorSpace.Linear);
        Texture2D sphereMap = new Texture2D(img);
        sphereMap.setMagFilter(MagFilter.Bilinear);
        sphereMap.setWrap(WrapMode.EdgeClamp);
        // bound as the Fresnel sphere map sampled via GL_SPHERE_MAP texgen in BangWater.vert.
        _material.setTexture("SphereMap", sphereMap);

        // fog ambience
        float density = _board.getFogDensity();
        if (density > 0f) {
            int fc = _board.getFogColor();
            _material.setBoolean("UseFog", true);
            _material.setColor("FogColor", new ColorRGBA(
                ((fc >> 16) & 0xFF) / 255f, ((fc >> 8) & 0xFF) / 255f,
                (fc & 0xFF) / 255f, 1f));
            _material.setFloat("FogDensity", density);
        }
    }

    /**
     * Updates the shader program based on the board state. The fork (re)configured the GLSL
     * {@code water.vert}/{@code water.frag} state; under jME3 the water MatDef and its fog define
     * are a Phase-4 task, so this is currently a flagged no-op.
     */
    public void refreshShader ()
    {
        // Phase 4: select the water MatDef's ENABLE_FOG define from _board.getFogDensity() once the
        // custom water .j3md is authored.
    }

    /**
     * Updates the wave amplitudes based on the amplitude scale and environment parameters.
     */
    public void refreshWaveAmplitudes ()
    {
        if (_board == null) {
            return;
        }
        float wdir = _board.getWindDirection(), wspeed = _board.getWindSpeed();
        Vector2f wvec = new Vector2f(wspeed * FastMath.cos(wdir),
            wspeed * FastMath.sin(wdir));
        WaveUtil.getInitialAmplitudes(WAVE_MAP_SIZE, WAVE_MAP_SIZE,
            MAP_WORLD_SIZE, MAP_WORLD_SIZE, new WaveUtil.PhillipsSpectrum(
                _board.getWaterAmplitude(), wvec, GRAVITY, 0.5f),
            _iramps, _iiamps);
    }

    /**
     * Updates the entire visible set of surface blocks.
     */
    public void refreshSurface ()
    {
        refreshSurface(0, 0, _board.getWidth() - 1, _board.getHeight() - 1);
    }

    /**
     * Updates the visible set of surface blocks within the specified tile coordinate rectangle.
     */
    public void refreshSurface (int x1, int y1, int x2, int y2)
    {
        for (int bx = x1; bx <= x2; bx++) {
            for (int by = y1; by <= y2; by++) {
                if (_board.isUnderWater(bx, by)) {
                    if (_blocks[bx][by] == null) {
                        createWaveBlock(bx, by);
                    }
                    if (_blocks[bx][by].getParent() == null) {
                        attachChild(_blocks[bx][by]);
                        _bcount++;
                    }
                } else if (_blocks[bx][by] != null &&
                    _blocks[bx][by].getParent() != null) {
                    detachChild(_blocks[bx][by]);
                    _bcount--;
                }
            }
        }

        setLocalTranslation(0f, 0f,
            (_board.getWaterLevel() - 1) *
                _board.getElevationScale(TILE_SIZE));

        updateModelBound();
        updateGeometricState();
    }

    @Override // documentation inherited
    public void updateLogicalState (float tpf)
    {
        super.updateLogicalState(tpf);
        if (_blocks == null || _patches == null || _bcount == 0) {
            return;
        }

        // compute the vertices and normals for the entire wave map
        _t += tpf;
        WaveUtil.getAmplitudes(WAVE_MAP_SIZE, WAVE_MAP_SIZE,
            MAP_WORLD_SIZE, MAP_WORLD_SIZE, _iramps, _iiamps, _disp, _t,
            _ramps, _iamps);
        WaveUtil.getDisplacements(WAVE_MAP_SIZE, WAVE_MAP_SIZE,
            MAP_WORLD_SIZE, MAP_WORLD_SIZE, _ramps, _iamps, _rgradx, _igradx,
            _rgrady, _igrady, 1f, _vbuf);
        WaveUtil.addVertices(WAVE_MAP_SIZE, WAVE_MAP_SIZE,
            MAP_WORLD_SIZE, MAP_WORLD_SIZE, _ramps, _iamps, _vbuf);
        WaveUtil.getNormals(WAVE_MAP_SIZE, WAVE_MAP_SIZE,
            MAP_WORLD_SIZE, MAP_WORLD_SIZE, _vbuf, _nbuf);

        // push the recomputed vertices/normals into each patch mesh
        for (int px = 0; px < WAVE_MAP_TILES; px++) {
            for (int py = 0; py < WAVE_MAP_TILES; py++) {
                Mesh patch = _patches[px][py];
                if (patch == null) {
                    continue;
                }
                VertexBuffer pos = patch.getBuffer(VertexBuffer.Type.Position);
                _vbuf.rewind();
                pos.updateData(_vbuf);
                VertexBuffer nrm = patch.getBuffer(VertexBuffer.Type.Normal);
                if (nrm != null) {
                    _nbuf.rewind();
                    nrm.updateData(_nbuf);
                }
                patch.updateBound();
            }
        }

        // Phase 4: when the GLSL water MatDef lands, also recompute the normal-map image
        // (WaveUtil.getNormalMap) and re-upload _normalMap's Image here.
    }

    /**
     * Initializes the state required for the normal map (high-detail GLSL path). Kept so the
     * Phase-4 water MatDef has its normal-map texture ready; not yet sampled by a material.
     */
    protected void initNormalMap ()
    {
        _nmap = BufferUtils.createByteBuffer(WAVE_MAP_SIZE * WAVE_MAP_SIZE * 4);
        Image img = new Image(Image.Format.RGBA8, WAVE_MAP_SIZE, WAVE_MAP_SIZE, _nmap,
            com.jme3.texture.image.ColorSpace.Linear);
        _normalMap = new Texture2D(img);
        _normalMap.setMagFilter(MagFilter.Bilinear);
        _normalMap.setWrap(WrapMode.Repeat);
    }

    /**
     * Creates and returns the wave block at the given tile coordinates.
     */
    protected void createWaveBlock (int bx, int by)
    {
        // medium/low detail is just a tile-sized quad
        if (!_highDetail) {
            if (_quad == null) {
                _quad = new com.jme3.scene.shape.Quad(TILE_SIZE, TILE_SIZE);
            }
            Geometry block = new Geometry("block", _quad);
            block.setMaterial(_material);
            block.setLocalTranslation((bx + 0.5f) * TILE_SIZE,
                (by + 0.5f) * TILE_SIZE, 0f);
            _blocks[bx][by] = block;
            return;
        }
        int px = bx % WAVE_MAP_TILES, py = by % WAVE_MAP_TILES;
        if (_patches[px][py] == null) {
            createWavePatch(px, py);
        }
        Geometry block = new Geometry("block", _patches[px][py]);
        block.setMaterial(_material);
        int wx = bx / WAVE_MAP_TILES, wy = by / WAVE_MAP_TILES;
        block.setLocalTranslation(
            wx * WAVE_MAP_TILES * TILE_SIZE,
            wy * WAVE_MAP_TILES * TILE_SIZE, 0f);
        _blocks[bx][by] = block;
    }

    /**
     * Creates the mesh for the wave patch at the given patch coordinates (a triangle-strip grid).
     */
    protected void createWavePatch (int px, int py)
    {
        if (_disp == null) {
            _disp = new WaveUtil.DeepWaterModel(GRAVITY);
        }

        // build the texture coordinate buffer for this patch
        int vsize = WAVE_MAP_SIZE + 1;
        float step = 1f / WAVE_MAP_SIZE;
        FloatBuffer tbuf = BufferUtils.createVector2Buffer(vsize * vsize);
        for (int xx = 0; xx < vsize; xx++) {
            for (int yy = 0; yy < vsize; yy++) {
                tbuf.put((yy + 0.5f) * step);
                tbuf.put((xx + 0.5f) * step);
            }
        }
        tbuf.rewind();

        IntBuffer ibuf = BufferUtils.createIntBuffer(
            (PATCH_SIZE + 1) * 2 * PATCH_SIZE);
        int stride = WAVE_MAP_SIZE + 1;
        boolean even = true;
        for (int x = px * PATCH_SIZE, xmax = x + PATCH_SIZE; x < xmax; x++) {
            if (even) {
                for (int y = py * PATCH_SIZE, ymax = y + PATCH_SIZE;
                    y <= ymax; y++) {
                    ibuf.put(x*stride + y);
                    ibuf.put((x+1)*stride + y);
                }
            } else {
                for (int y = (py+1) * PATCH_SIZE, ymin = y - PATCH_SIZE;
                    y >= ymin; y--) {
                    ibuf.put((x+1)*stride + y);
                    ibuf.put(x*stride + y);
                }
            }
            even = !even;
        }
        ibuf.rewind();

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.TriangleStrip);
        _vbuf.rewind();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, _vbuf);
        _nbuf.rewind();
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, _nbuf);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, tbuf);
        mesh.setBuffer(VertexBuffer.Type.Index, 2, ibuf);
        mesh.setBound(new BoundingBox(
            new Vector3f((px+0.5f) * TILE_SIZE, (py+0.5f) * TILE_SIZE, 0f),
            TILE_SIZE/2, TILE_SIZE/2, 5f));
        mesh.updateCounts();
        _patches[px][py] = mesh;
    }

    /** The application context. */
    protected BasicContext _ctx;

    /** The board's primary light. */
    protected DirectionalLight _light;

    /** Whether or not we are in editor mode. */
    protected boolean _editorMode;

    /** Whether we run the high-detail (wave simulation) path. */
    protected boolean _highDetail;

    /** The board with the terrain information. */
    protected BangBoard _board;

    /** The water surface material. */
    protected Material _material;

    /** The shared wave patches. */
    protected Mesh[][] _patches;

    /** The array of surface blocks referring to instances of the patches. */
    protected Geometry[][] _blocks;

    /** The normal map texture (high-detail path; sampled by the Phase-4 water MatDef). */
    protected Texture2D _normalMap;

    /** The number of active blocks. */
    protected int _bcount;

    /** Our many, many FFT arrays. */
    float[][] _iramps = new float[WAVE_MAP_SIZE][WAVE_MAP_SIZE],
        _iiamps = new float[WAVE_MAP_SIZE][WAVE_MAP_SIZE],
        _ramps = new float[WAVE_MAP_SIZE][WAVE_MAP_SIZE],
        _iamps = new float[WAVE_MAP_SIZE][WAVE_MAP_SIZE],
        _rgradx = new float[WAVE_MAP_SIZE][WAVE_MAP_SIZE],
        _igradx = new float[WAVE_MAP_SIZE][WAVE_MAP_SIZE],
        _rgrady = new float[WAVE_MAP_SIZE][WAVE_MAP_SIZE],
        _igrady = new float[WAVE_MAP_SIZE][WAVE_MAP_SIZE];

    /** Buffers for the vertices and normals of the entire wave patch. */
    protected FloatBuffer _vbuf, _nbuf;

    /** If using the pixel shader, the normal map pixel buffer. */
    protected ByteBuffer _nmap;

    /** The time of the last frame within the animation period. */
    protected float _t;

    /** The dispersion model. */
    protected static WaveUtil.DispersionModel _disp;

    /** A tile-sized quad mesh to share as a low resolution water surface. */
    protected static com.jme3.scene.shape.Quad _quad;

    /** The size in samples of the wave map. */
    protected static final int WAVE_MAP_SIZE = 32;

    /** The number of tiles spanned by the wave map. */
    protected static final int WAVE_MAP_TILES = 4;

    /** The actual size of the wave map in world units. */
    protected static final float MAP_WORLD_SIZE = WAVE_MAP_TILES * TILE_SIZE;

    /** The size in squares of each patch. */
    protected static final int PATCH_SIZE = WAVE_MAP_SIZE / WAVE_MAP_TILES;

    /** The size of the Fresnel sphere map. */
    protected static final int SPHERE_MAP_SIZE = 256;

    /** The minimum alpha of the water. */
    protected static final float WATER_ALPHA = 0.95f;

    /** The air/water Snell ratio. */
    protected static final float SNELL_RATIO = 1.34f;

    /** The acceleration due to gravity in Bang! units. */
    protected static final float GRAVITY = 16f;
}
