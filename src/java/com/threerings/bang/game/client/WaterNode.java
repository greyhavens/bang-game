//
// $Id$

package com.threerings.bang.game.client;

import java.awt.image.BufferedImage;

import java.io.File;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import com.jme.bounding.BoundingBox;
import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Line;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Skybox;
import com.jme.scene.TriMesh;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.util.TextureManager;
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
        mstate.setAmbient(new ColorRGBA(0f, 0.1f, 0.2f, 0.5f));
        mstate.setDiffuse(new ColorRGBA(0.6f, 0.8f, 1f, 0.75f));
        mstate.setSpecular(ColorRGBA.white);
        mstate.setShininess(64f);
        setRenderState(mstate);
        
        TextureState tstate = ctx.getRenderer().createTextureState();
        tstate.setTexture(createReflectionMap());
        setRenderState(tstate);
        
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
    
    /**
     * Creates the reflection map using five sides of the sky box.
     */
    protected Texture createReflectionMap ()
    {
        // load the sky box images
        BufferedImage[] sides = new BufferedImage[5];
        for (int i = 0; i < sides.length; i++) {
            sides[i] = _ctx.loadImage("textures/environ/desertday" +
                SUFFIXES[i] + ".png");
        }
        
        int[] rgb = new int[REFLECTION_MAP_SIZE * REFLECTION_MAP_SIZE];
        float hsize = REFLECTION_MAP_SIZE * 0.5f;
        Vector3f vec = new Vector3f();
        for (int y = 0, idx = 0; y < REFLECTION_MAP_SIZE; y++) {
            for (int x = 0; x < REFLECTION_MAP_SIZE; x++, idx++) {
                float vx = x / hsize - 1f, vy = y / hsize - 1f,
                    d2 = vx*vx + vy*vy;
                int p = 0;
                if (d2 <= 1f) {
                    vec.set(vx, vy, FastMath.sqrt(1f - d2));
                    rgb[idx] = getCubeMapPixel(vec, sides);
                }
            }
        }
        
        BufferedImage image = new BufferedImage(REFLECTION_MAP_SIZE,
            REFLECTION_MAP_SIZE, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, REFLECTION_MAP_SIZE, REFLECTION_MAP_SIZE, rgb, 0,
            REFLECTION_MAP_SIZE);
        //try {
        //ImageIO.write(image, "png", new File("reflect.png"));
        //} catch (Exception e) { e.printStackTrace(); }
        
        Texture texture = TextureManager.loadTexture(image,
            Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, false);
        texture.setEnvironmentalMapMode(Texture.EM_SPHERE);
        return texture;
    }
    
    /**
     * Returns the pixel from the cube map to which the given vector points.
     */
    protected int getCubeMapPixel (Vector3f vec, BufferedImage[] sides)
    {
        int side = getCubeMapSide(vec);
        
        float s, t;
        switch (side) {
            case FRONT:
                s = -vec.x / vec.y;
                t = vec.z / vec.y;
                break;
            case BACK:
                s = -vec.x / vec.y;
                t = -vec.z / vec.y;
                break;
            case LEFT:
                s = vec.y / vec.x;
                t = -vec.z / vec.x;
                break;
            case RIGHT:
                s = vec.y / vec.x;
                t = vec.z / vec.x;
                break;
            default:
            case UP:
                s = vec.x / vec.z;
                t = -vec.y / vec.z;
                break;
        }
        int width = sides[side].getWidth(), height = sides[side].getHeight();
        return sides[side].getRGB((int)((width-1) * (s+1f)/2f),
            (int)((height-1) * (1f-t)/2f));
    }
    
    /**
     * Returns the side index identifying the face of the cube map to which
     * the given vector points.
     */
    protected int getCubeMapSide (Vector3f vec)
    {
        if (vec.x > vec.z && vec.x > vec.y && vec.x > -vec.y) {
            return RIGHT;
            
        } else if (vec.x < -vec.z && vec.x < -vec.y && vec.x < vec.y) {
            return LEFT;
        
        } else if (vec.y > vec.z) {
            return FRONT;
            
        } else if (vec.y < -vec.z) {
            return BACK;
            
        } else {
            return UP;
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
        (TILE_SIZE / BangBoard.ELEVATION_UNITS_PER_TILE) * 0.1f;
    
    /** The length of the waves in node units. */
    protected static final float WAVE_LENGTH = TILE_SIZE * 0.5f;
    
    /** The speed of the waves in node units per second. */
    protected static final float WAVE_SPEED = TILE_SIZE * 0.25f;
    
    /** The phase difference between x and y. */
    protected static final float PHASE_DIFFERENCE = WAVE_LENGTH * 0.1f;
    
    /** The size of the reflection map. */
    protected static final int REFLECTION_MAP_SIZE = 256;
    
    /** The sides of the cube map. */
    protected static final int FRONT = 0, BACK = 1, LEFT = 2, RIGHT = 3,
        UP = 4;
    
    /** The suffixes corresponding to the sides. */
    protected static final String[] SUFFIXES =
        { "ft", "bk", "lf", "rt", "up" };
}
