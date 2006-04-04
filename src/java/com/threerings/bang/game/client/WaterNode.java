//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;

import com.jme.bounding.BoundingBox;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Line;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Skybox;
import com.jme.scene.TriMesh;
import com.jme.scene.state.LightState;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.util.TextureManager;
import com.jme.util.geom.BufferUtils;
import com.jme.util.geom.Debugger;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;
import com.threerings.bang.util.WaveUtil;

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
        setRenderState(RenderUtil.overlayZBuf);
        setRenderState(_smtstate = _ctx.getRenderer().createTextureState());
        
        // combine the board light state with one that enables specular
        // properties
        LightState lstate = _ctx.getRenderer().createLightState();
        lstate.setLocalViewer(true);
        lstate.setSeparateSpecular(true);
        setRenderState(lstate);
        setLightCombineMode(LightState.COMBINE_CLOSEST);
        
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
        
        // refresh the sphere map
        refreshSphereMap();
        
        // and the wave amplitudes
        refreshWaveAmplitudes();
        
        // initialize the array of blocks
        int bwidth = (int)Math.ceil(_board.getWidth() /
                (double)HEIGHT_MAP_TILES),
            bheight = (int)Math.ceil(_board.getHeight() /
                (double)HEIGHT_MAP_TILES);
        _blocks = new SharedMesh[bwidth][bheight];
        _bcount = 0;
        refreshSurface();
    }
    
    /**
     * Releases the resources created by this node.
     */
    public void cleanup ()
    {
        _smtstate.deleteAll();
    }
    
    /**
     * Creates and attaches the sphere map that blends the water and sky colors
     * according to the Fresnel term.  This code is based on the RenderMan
     * shader in Jerry Tessendorf's
     * <a href="http://www.finelightvisualtechnology.com/docs/coursenotes2004.pdf">
     * Simulating Ocean Water</a>.
     */
    public void refreshSphereMap ()
    {
        _smtstate.deleteAll();
        
        ByteBuffer pbuf = ByteBuffer.allocateDirect(SPHERE_MAP_SIZE *
            SPHERE_MAP_SIZE * 4);
        ColorRGBA wcolor = RenderUtil.createColorRGBA(
            _board.getWaterColor()),
                scolor = RenderUtil.createColorRGBA(
            _board.getSkyOverheadColor()),
                color = new ColorRGBA();
        wcolor.a = WATER_ALPHA;
        
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
        
        Texture texture = new Texture();
        texture.setImage(new Image(Image.RGBA8888, SPHERE_MAP_SIZE,
            SPHERE_MAP_SIZE, pbuf));
        texture.setEnvironmentalMapMode(Texture.EM_SPHERE);
        texture.setFilter(Texture.FM_LINEAR);
        texture.setMipmapState(Texture.MM_LINEAR_LINEAR);
        texture.setApply(Texture.AM_REPLACE);
        _smtstate.setTexture(texture);
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
        WaveUtil.getInitialAmplitudes(HEIGHT_MAP_SIZE, HEIGHT_MAP_SIZE,
            PATCH_SIZE, PATCH_SIZE, new WaveUtil.PhillipsSpectrum(
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
        for (int bx = x1 / HEIGHT_MAP_TILES, bxmax = x2 / HEIGHT_MAP_TILES;
                bx <= bxmax; bx++) {
            for (int by = y1 / HEIGHT_MAP_TILES, bymax = y2 / HEIGHT_MAP_TILES;
                    by <= bymax; by++) {
                if (isUnderWater(bx, by)) {
                    if (_blocks[bx][by] == null) {
                        if (_patch == null) {
                            createWavePatch();
                        }
                        _blocks[bx][by] = new SharedMesh("block", _patch);
                        _blocks[bx][by].setLocalTranslation(
                            new Vector3f(bx * HEIGHT_MAP_TILES * TILE_SIZE,
                                by * HEIGHT_MAP_TILES * TILE_SIZE, 0f));
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
        if (_blocks == null || _bcount == 0) {
            return;
        }
        
        // update the shared geometry (the vertices go last because they
        // overwrite the amplitudes)
        _t += time;
        WaveUtil.getAmplitudes(HEIGHT_MAP_SIZE, HEIGHT_MAP_SIZE,
            PATCH_SIZE, PATCH_SIZE, _iramps, _iiamps, _disp, _t, _ramps,
            _iamps);
        WaveUtil.getDisplacements(HEIGHT_MAP_SIZE, HEIGHT_MAP_SIZE,
            PATCH_SIZE, PATCH_SIZE, _ramps, _iamps, _rgradx, _igradx,
            _rgrady, _igrady, 1f, _patch.getVertexBuffer());
        WaveUtil.addVertices(HEIGHT_MAP_SIZE, HEIGHT_MAP_SIZE,
            PATCH_SIZE, PATCH_SIZE, _ramps, _iamps, _patch.getVertexBuffer());
        WaveUtil.getNormals(HEIGHT_MAP_SIZE, HEIGHT_MAP_SIZE,
            PATCH_SIZE, PATCH_SIZE, _patch.getVertexBuffer(),
            _patch.getNormalBuffer());
    }

    /**
     * Determines whether any part of the block at the specified block
     * coordinates is underwater.
     */
    protected boolean isUnderWater (int bx, int by)
    {
        for (int x = bx * HEIGHT_MAP_TILES, xmax = x + HEIGHT_MAP_TILES;
                x < xmax; x++) {
            for (int y = by * HEIGHT_MAP_TILES, ymax = y + HEIGHT_MAP_TILES;
                    y < ymax; y++) {
                if (_board.isUnderWater(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Creates the state necessary to render the wave patch.
     */
    protected void createWavePatch ()
    {
        // reuse the dispersion model and index buffer
        _disp = new WaveUtil.DeepWaterModel(GRAVITY);
        int vwidth = HEIGHT_MAP_SIZE + 1, vheight = HEIGHT_MAP_SIZE + 1,
            vsize = vwidth * vheight;
        IntBuffer ibuf = BufferUtils.createIntBuffer(HEIGHT_MAP_SIZE *
            HEIGHT_MAP_SIZE * 2 * 3);
        for (int ii = 0; ii < HEIGHT_MAP_SIZE; ii++) {
            for (int jj = 0; jj < HEIGHT_MAP_SIZE; jj++) {
                // upper left triangle
                ibuf.put((jj+1)*vwidth + ii);
                ibuf.put((jj+1)*vwidth + (ii+1));
                ibuf.put(jj*vwidth + ii);

                // lower right triangle
                ibuf.put((jj+1)*vwidth + (ii+1));
                ibuf.put(jj*vwidth + (ii+1));
                ibuf.put(jj*vwidth + ii);
            }
        }
        
        // create the trimesh
        _patch = new TriMesh("waves", BufferUtils.createVector3Buffer(vsize),
            BufferUtils.createVector3Buffer(vsize), null, null, ibuf);
        _patch.setModelBound(new BoundingBox(Vector3f.ZERO, PATCH_SIZE, PATCH_SIZE,
            1f));
    }
    
    /** The application context. */
    protected BasicContext _ctx;
    
    /** The board with the terrain information. */
    protected BangBoard _board;

    /** The shared wave patch. */
    protected TriMesh _patch;
    
    /** The array of surface blocks referring to instances of the patch. */
    protected SharedMesh[][] _blocks;
    
    /** The sphere map texture state. */
    protected TextureState _smtstate;
    
    /** The number of active blocks. */
    protected int _bcount;
    
    /** Our many, many FFT arrays. */
    float[][] _iramps = new float[HEIGHT_MAP_SIZE][HEIGHT_MAP_SIZE],
        _iiamps = new float[HEIGHT_MAP_SIZE][HEIGHT_MAP_SIZE],
        _ramps = new float[HEIGHT_MAP_SIZE][HEIGHT_MAP_SIZE],
        _iamps = new float[HEIGHT_MAP_SIZE][HEIGHT_MAP_SIZE],
        _rgradx = new float[HEIGHT_MAP_SIZE][HEIGHT_MAP_SIZE],
        _igradx = new float[HEIGHT_MAP_SIZE][HEIGHT_MAP_SIZE],
        _rgrady = new float[HEIGHT_MAP_SIZE][HEIGHT_MAP_SIZE],
        _igrady = new float[HEIGHT_MAP_SIZE][HEIGHT_MAP_SIZE];
    
    /** The dispersion model. */
    protected WaveUtil.DispersionModel _disp;
    
    /** The time of the last frame within the animation period. */
    protected float _t;
    
    /** The size in samples of the wave height map. */
    protected static final int HEIGHT_MAP_SIZE = 64;
    
    /** The number of tiles spanned by the wave height map. */
    protected static final int HEIGHT_MAP_TILES = 8; 
    
    /** The actual size of the wave patch in world units. */
    protected static final float PATCH_SIZE = HEIGHT_MAP_TILES * TILE_SIZE;
    
    /** The size of the Fresnel sphere map. */
    protected static final int SPHERE_MAP_SIZE = 256;
    
    /** The minimum alpha of the water. */
    protected static final float WATER_ALPHA = 0.95f;
    
    /** The air/water Snell ratio. */
    protected static final float SNELL_RATIO = 1.34f;
    
    /** The acceleration due to gravity in Bang! units. */
    protected static final float GRAVITY = 16f;
}
