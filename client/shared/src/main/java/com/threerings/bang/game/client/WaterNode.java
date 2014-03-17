//
// $Id$

package com.threerings.bang.game.client;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GLContext;

import com.jme.bounding.BoundingBox;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.light.DirectionalLight;
import com.jme.math.FastMath;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.TriMesh;
import com.jme.scene.batch.TriangleBatch;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.gdx.GDXTextureState;
import com.jme.scene.state.gdx.records.TextureStateRecord;
import com.jme.util.geom.BufferUtils;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;
import com.threerings.bang.util.WaveUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the rendering of Bang's wet spots.
 */
public class WaterNode extends Node
{
    public WaterNode (BasicContext ctx, DirectionalLight light, boolean editorMode)
    {
        super("water");
        _ctx = ctx;
        _light = light;
        _editorMode = editorMode;

        setRenderState(RenderUtil.blendAlpha);
        setRenderState(RenderUtil.backCull);

        // we normalize things ourself
        setNormalsMode(NM_USE_PROVIDED);

        // use our fancy shaders if possible
        if (GLContext.getCapabilities().GL_ARB_vertex_shader &&
            GLContext.getCapabilities().GL_ARB_fragment_shader &&
            BangPrefs.isHighDetail() && !_disableShaders && initShaders()) {
            return;
        }

        // otherwise, use a combination of sphere map, lighting, and material
        // that approximates the effect
        setRenderState(_smtstate = _ctx.getRenderer().createTextureState());

        // use the board's main light in a new state that enables specular
        // properties
        LightState lstate = _ctx.getRenderer().createLightState();
        lstate.attach(light);
        lstate.setLocalViewer(true);
        lstate.setSeparateSpecular(true);
        setRenderState(lstate);

        MaterialState mstate = _ctx.getRenderer().createMaterialState();
        mstate.getDiffuse().set(ColorRGBA.black);
        mstate.getAmbient().set(ColorRGBA.black);
        mstate.getSpecular().set(ColorRGBA.white);
        mstate.setShininess(32f);
        setRenderState(mstate);
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

        // initialize the array of blocks and patches
        _blocks = new SharedMesh[_board.getWidth()][_board.getHeight()];
        if (BangPrefs.isHighDetail()) {
            _patches = new TriMesh[WAVE_MAP_TILES][WAVE_MAP_TILES];
            int vsize = (WAVE_MAP_SIZE + 1) * (WAVE_MAP_SIZE + 1);
            _vbuf = BufferUtils.createVector3Buffer(vsize);
            if (_sstate == null) {
                _nbuf = BufferUtils.createVector3Buffer(vsize);
            } else {
                initNormalMap();
            }
        } else {
            _patches = null;
        }
        _bcount = 0;
        refreshSurface();

        // refresh the sphere map and wave amplitudes if there are any
        // blocks visible
        if (_editorMode || _bcount > 0) {
            refreshColors();
            refreshShader();
            if (_patches != null) {
                refreshWaveAmplitudes();
            }
        }
    }

    /**
     * Releases the resources created by this node.
     */
    public void cleanup ()
    {
        if (_smtstate != null) {
            _smtstate.deleteAll();
        }
        if (_nmtstate != null) {
            _nmtstate.deleteAll();
        }
    }

    /**
     * Creates and attaches the sphere map that blends the water and sky colors
     * according to the Fresnel term, or sets those parameters in the shaders.
     * This code and that of the shaders is based on the RenderMan shader in
     * Jerry Tessendorf's
     * <a href="http://www.finelightvisualtechnology.com/docs/coursenotes2004.pdf">
     * Simulating Ocean Water</a>.
     */
    public void refreshColors ()
    {
        ColorRGBA wcolor = RenderUtil.createColorRGBA(
            _board.getWaterColor()),
                scolor = RenderUtil.createColorRGBA(
            _board.getSkyOverheadColor());
        wcolor.a = WATER_ALPHA;
        if (_sstate != null) {
            _sstate.setUniform("waterColor",
                wcolor.r, wcolor.g, wcolor.b, wcolor.a);
            _sstate.setUniform("skyOverheadColor",
                scolor.r, scolor.g, scolor.b, scolor.a);
            return;
        }
        if (_smtstate == null) {
            return;
        }
        _smtstate.deleteAll();

        ByteBuffer pbuf = ByteBuffer.allocateDirect(SPHERE_MAP_SIZE *
            SPHERE_MAP_SIZE * 4);
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
                    color.interpolate(wcolor, scolor, reflectivity);

                } else {
                    color.set(ColorRGBA.black);
                }
                pbuf.put((byte)(color.r * 255));
                pbuf.put((byte)(color.g * 255));
                pbuf.put((byte)(color.b * 255));
                pbuf.put((byte)(color.a * 255));
            }
        }
        pbuf.rewind();

        Texture texture = _ctx.getTextureCache().createTexture();
        RenderUtil.configureTexture(texture,
            new Image(Image.RGBA8888, SPHERE_MAP_SIZE, SPHERE_MAP_SIZE, pbuf));
        RenderUtil.enableTextureCompression(texture);
        texture.setEnvironmentalMapMode(Texture.EM_SPHERE);
        texture.setApply(Texture.AM_REPLACE);
        _smtstate.setTexture(texture);
    }

    /**
     * Updates the shader program based on the board state.
     */
    public void refreshShader ()
    {
        if (_sstate != null) {
            _ctx.getShaderCache().configureState(_sstate,
                "shaders/water.vert", "shaders/water.frag",
                (_board.getFogDensity() > 0f) ? new String[] { "ENABLE_FOG" } : new String[0]);
        }
    }

    /**
     * Updates the wave amplitudes based on the amplitude scale and environment
     * parameters.
     */
    public void refreshWaveAmplitudes ()
    {
        // create the initial set of wave amplitudes
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
     * Updates the visible set of surface blocks within the specified tile
     * coordinate rectangle based on the state of the board terrain and
     * water level.
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

        getLocalTranslation().set(0f, 0f,
            (_board.getWaterLevel() - 1) *
                _board.getElevationScale(TILE_SIZE));

        updateWorldBound();

        updateRenderState();
        updateGeometricState(0, true);
    }

    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);
        if (_blocks == null || _patches == null || _bcount == 0) {
            return;
        }

        // compute the vertices and normals for the entire wave map
        _t += time;
        WaveUtil.getAmplitudes(WAVE_MAP_SIZE, WAVE_MAP_SIZE,
            MAP_WORLD_SIZE, MAP_WORLD_SIZE, _iramps, _iiamps, _disp, _t,
            _ramps, _iamps);
        WaveUtil.getDisplacements(WAVE_MAP_SIZE, WAVE_MAP_SIZE,
            MAP_WORLD_SIZE, MAP_WORLD_SIZE, _ramps, _iamps, _rgradx, _igradx,
            _rgrady, _igrady, 1f, _vbuf);
        WaveUtil.addVertices(WAVE_MAP_SIZE, WAVE_MAP_SIZE,
            MAP_WORLD_SIZE, MAP_WORLD_SIZE, _ramps, _iamps, _vbuf);
        if (_sstate == null) {
            WaveUtil.getNormals(WAVE_MAP_SIZE, WAVE_MAP_SIZE,
                MAP_WORLD_SIZE, MAP_WORLD_SIZE, _vbuf, _nbuf);
        } else {
            Vector3f lvec = _light.getDirection();
            _sstate.setUniform("lightVector", -lvec.x, -lvec.y, -lvec.z);
            WaveUtil.getNormalMap(WAVE_MAP_SIZE, WAVE_MAP_SIZE,
                MAP_WORLD_SIZE, MAP_WORLD_SIZE, _vbuf, _nmap);
            _nmtstate.setNeedsRefresh(true);
        }
    }

    /**
     * Initializes the shader state.
     *
     * @return true if the shaders were compiled successfully, false to fall back on the fixed
     * function implementation.
     */
    protected boolean initShaders ()
    {
        _sstate = _ctx.getShaderCache().createState("shaders/water.vert", "shaders/water.frag");
        if (_sstate == null) {
            _disableShaders = true;
            return false;
        }
        _sstate.setUniform("normalMap", 0);

        setRenderState(_sstate);
        setRenderState(_nmtstate = new GDXTextureState() {
            public void apply () {
                super.apply();
                if (needsRefresh() && _nmap != null) {
                    TextureStateRecord rec = (TextureStateRecord)_ctx.getDisplay().
                        getCurrentContext().getStateRecord(RenderState.RS_TEXTURE);
                    if (rec.currentUnit != 0) {
                        GL13.glActiveTexture(GL13.GL_TEXTURE0);
                        rec.currentUnit = 0;
                    }
                    GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                        WAVE_MAP_SIZE, WAVE_MAP_SIZE,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, _nmap);
                }
            }
        });
        return true;
    }

    /**
     * Initializes the state required for the normal map.
     */
    protected void initNormalMap ()
    {
        // create the texture coordinate buffer
        int vsize = WAVE_MAP_SIZE + 1;
        float step = 1f / WAVE_MAP_SIZE;
        _tbuf = BufferUtils.createVector2Buffer(vsize * vsize);
        for (int xx = 0; xx < vsize; xx++) {
            for (int yy = 0; yy < vsize; yy++) {
                _tbuf.put((yy + 0.5f) * step);
                _tbuf.put((xx + 0.5f) * step);
            }
        }
        _tbuf.rewind();

        // create and add the texture
        _nmtstate.deleteAll();
        Texture tex = _ctx.getTextureCache().createTexture();
        tex.setImage(new Image(Image.RGBA8888, WAVE_MAP_SIZE, WAVE_MAP_SIZE,
            _nmap = BufferUtils.createByteBuffer(WAVE_MAP_SIZE * WAVE_MAP_SIZE * 4)));
        tex.setFilter(Texture.FM_LINEAR);
        tex.setWrap(Texture.WM_WRAP_S_WRAP_T);
        _nmtstate.setTexture(tex);
    }

    /**
     * Creates and returns the wave block at the given tile coordinates.
     */
    protected void createWaveBlock (int bx, int by)
    {
        // medium/low detail is just a tile-sized quad
        if (!BangPrefs.isHighDetail()) {
            if (_quad == null) {
                _quad = new Quad("quad", TILE_SIZE, TILE_SIZE);
                _quad.setModelBound(new BoundingBox());
                _quad.updateModelBound();
            }
            _blocks[bx][by] = new SharedMesh("block", _quad);
            _blocks[bx][by].getLocalTranslation().set((bx + 0.5f) * TILE_SIZE,
                (by + 0.5f) * TILE_SIZE, 0f);
            return;
        }
        int px = bx % WAVE_MAP_TILES, py = by % WAVE_MAP_TILES;
        if (_patches[px][py] == null) {
            createWavePatch(px, py);
        }
        _blocks[bx][by] = new SharedMesh("block", _patches[px][py]);
        int wx = bx / WAVE_MAP_TILES, wy = by / WAVE_MAP_TILES;
        _blocks[bx][by].getLocalTranslation().set(
            wx * WAVE_MAP_TILES * TILE_SIZE,
            wy * WAVE_MAP_TILES * TILE_SIZE, 0f);
    }

    /**
     * Creates the state necessary to render the wave patch at the given patch
     * coordinates.
     */
    protected void createWavePatch (int px, int py)
    {
        // reuse the dispersion model
        if (_disp == null) {
            _disp = new WaveUtil.DeepWaterModel(GRAVITY);
        }
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

        // create the trimesh
        _patches[px][py] = new TriMesh("patch", _vbuf, _nbuf, null, _tbuf, ibuf);
        _patches[px][py].getBatch(0).setMode(TriangleBatch.TRIANGLE_STRIP);
        _patches[px][py].setModelBound(new BoundingBox(
            new Vector3f((px+0.5f) * TILE_SIZE, (py+0.5f) * TILE_SIZE, 0f),
            TILE_SIZE/2, TILE_SIZE/2, 5f));
    }

    /** The application context. */
    protected BasicContext _ctx;

    /** The board's primary light. */
    protected DirectionalLight _light;

    /** Whether or not we are in editor mode. */
    protected boolean _editorMode;

    /** The board with the terrain information. */
    protected BangBoard _board;

    /** The shared wave patches. */
    protected TriMesh[][] _patches;

    /** The array of surface blocks referring to instances of the patches. */
    protected SharedMesh[][] _blocks;

    /** The water shader state, if GLSL shading is supported. */
    protected GLSLShaderObjectsState _sstate;

    /** The sphere map texture state, if GLSL shading is not supported. */
    protected TextureState _smtstate;

    /** The normal map texture state, if GLSL shading *is* supported. */
    protected TextureState _nmtstate;

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

    /** Buffers for the vertices, normals, and tex coords of the entire wave patch. */
    protected FloatBuffer _vbuf, _nbuf, _tbuf;

    /** If using the pixel shader, the normal map. */
    protected ByteBuffer _nmap;

    /** The time of the last frame within the animation period. */
    protected float _t;

    /** The dispersion model. */
    protected static WaveUtil.DispersionModel _disp;

    /** A tile-sized quad to share as a low resolution water surface. */
    protected static Quad _quad;

    /** If true, the shaders didn't link; don't try to compile them again. */
    protected static boolean _disableShaders;

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
