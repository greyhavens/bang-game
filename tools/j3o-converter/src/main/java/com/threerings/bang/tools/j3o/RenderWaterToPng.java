//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

/**
 * Offscreen render-to-PNG harness for the Bang! water surface (Phase 4c verification).
 *
 * <p>It reproduces {@code WaterNode}'s rendering exactly without needing a live server or a
 * fork-format {@code .board}: it builds the {@code shaders/BangWater.j3md} material, bakes the
 * Fresnel sphere map (water-color &lt;-&gt; sky-color by reflectivity) the same way
 * {@code WaterNode.refreshColors} does, builds a wave-displaced surface grid with per-vertex
 * normals (a plausible stand-in for the CPU FFT sim — the shader only cares about the perturbed
 * normal), and renders one frame to an offscreen FrameBuffer through {@link OffscreenRenderApp}.
 * Passing a frame index displaces the waves so two captures can be compared to confirm the surface
 * animates.
 *
 * <p>Usage: {@code RenderWaterToPng <rsrc root> <output png> [frame]}
 */
public class RenderWaterToPng extends OffscreenRenderApp
{
    public static void main (String[] args)
    {
        if (args.length < 2) {
            System.err.println("Usage: RenderWaterToPng <rsrc root> <output png> [frame]");
            System.exit(1);
        }
        float frame = args.length > 2 ? Float.parseFloat(args[2]) : 0f;
        launch(new RenderWaterToPng(args[0], args[1], frame));
    }

    public RenderWaterToPng (String rsrcRoot, String outPng, float frame)
    {
        super(new File(outPng));
        _rsrcRoot = new File(rsrcRoot).getAbsoluteFile();
        _frame = frame;
    }

    @Override protected Node buildScene ()
    {
        assetManager.registerLocator(_rsrcRoot.getPath(), FileLocator.class);

        // --- build the water material exactly as WaterNode does ---
        Material mat = new Material(assetManager, "shaders/BangWater.j3md");
        mat.setFloat("Alpha", 0.95f);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Back);
        mat.setTexture("SphereMap", buildFresnelSphereMap(
            // a typical Bang! board: blue-green water under a pale sky
            new ColorRGBA(0.16f, 0.30f, 0.34f, 0.95f),   // water color
            new ColorRGBA(0.62f, 0.74f, 0.86f, 1f)));     // sky overhead color

        // --- build a wave-displaced surface grid (stand-in for the FFT sim) ---
        Geometry water = new Geometry("water", buildWaveMesh(_frame));
        water.setMaterial(mat);
        water.setQueueBucket(RenderQueue.Bucket.Transparent);

        Node scene = new Node("scene");
        scene.attachChild(water);
        return scene;
    }

    @Override protected Camera createCamera (Spatial scene)
    {
        // frame the surface the way the player sees the board: a 3/4 elevated view looking down at
        // an oblique angle, far enough back that the whole water patch and the land behind it are
        // visible. A sphere-map reflection reads as water when the surface curvature (wave normals)
        // makes the reflection vary across the surface — flat-on or edge-on it degenerates.
        float span = GRID_TILES * TILE_SIZE;
        Vector3f center = new Vector3f(span * 0.5f, span * 0.5f, 0f);
        Camera offCam = new Camera(WIDTH, HEIGHT);
        offCam.setFrustumPerspective(45f, (float)WIDTH / HEIGHT, 0.5f, span * 12f);
        offCam.setLocation(center.add(new Vector3f(-span * 0.9f, -span * 0.9f, span * 1.1f)));
        offCam.lookAt(center, Vector3f.UNIT_Z);
        return offCam;
    }

    @Override protected ColorRGBA backgroundColor ()
    {
        // a neutral land/dirt backdrop so we judge the water against terrain, not blue-on-blue
        return new ColorRGBA(0.42f, 0.36f, 0.28f, 1f);
    }

    /** Bakes the Fresnel sphere map identically to {@code WaterNode.refreshColors}. */
    protected Texture2D buildFresnelSphereMap (ColorRGBA wcolor, ColorRGBA scolor)
    {
        int size = 256, hsize = size / 2;
        float snell = 1.34f;
        ByteBuffer pbuf = ByteBuffer.allocateDirect(size * size * 4);
        ColorRGBA color = new ColorRGBA();
        for (int ii = -hsize; ii < hsize; ii++) {
            for (int jj = -hsize; jj < hsize; jj++) {
                float x = (float)ii / hsize, y = (float)jj / hsize;
                float d = FastMath.sqrt(x*x + y*y);
                if (d <= 1f) {
                    float thetai = FastMath.asin(d);
                    float thetat = FastMath.asin(d / snell);
                    float reflectivity;
                    if (thetai == 0f) {
                        reflectivity = (snell - 1f) / (snell + 1f);
                        reflectivity = reflectivity * reflectivity;
                    } else {
                        float fs = FastMath.sin(thetat - thetai) / FastMath.sin(thetat + thetai);
                        float ts = FastMath.tan(thetat - thetai) / FastMath.tan(thetat + thetai);
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
        // Linear (raw verbatim colors), matching WaterNode: a gamma-correct pipeline must NOT
        // sRGB-decode these baked water/sky colors or the surface darkens to near-black.
        Image img = new Image(Image.Format.RGBA8, size, size, pbuf, ColorSpace.Linear);
        Texture2D tex = new Texture2D(img);
        tex.setMagFilter(MagFilter.Bilinear);
        tex.setWrap(WrapMode.EdgeClamp);
        return tex;
    }

    /**
     * Builds a wave-displaced grid mesh with correct per-vertex normals. The displacement is a sum
     * of two travelling sinusoids advanced by the frame parameter — a plausible stand-in for the
     * FFT sim that exercises the same shader input (a perturbed surface normal that changes over
     * time).
     */
    protected Mesh buildWaveMesh (float t)
    {
        int n = GRID_TILES * SAMPLES;            // samples per side
        int vside = n + 1;
        float step = (GRID_TILES * TILE_SIZE) / n;

        FloatBuffer vbuf = BufferUtils.createVector3Buffer(vside * vside);
        FloatBuffer nbuf = BufferUtils.createVector3Buffer(vside * vside);
        for (int yy = 0; yy <= n; yy++) {
            for (int xx = 0; xx <= n; xx++) {
                float px = xx * step, py = yy * step;
                float h = waveHeight(px, py, t);
                vbuf.put(px).put(py).put(h);
                // analytic normal from the partial derivatives of waveHeight
                float dx = waveHeight(px + 0.1f, py, t) - waveHeight(px - 0.1f, py, t);
                float dy = waveHeight(px, py + 0.1f, t) - waveHeight(px, py - 0.1f, t);
                Vector3f nrm = new Vector3f(-dx / 0.2f, -dy / 0.2f, 1f).normalizeLocal();
                nbuf.put(nrm.x).put(nrm.y).put(nrm.z);
            }
        }
        vbuf.rewind(); nbuf.rewind();

        IntBuffer ibuf = BufferUtils.createIntBuffer(n * n * 6);
        for (int yy = 0; yy < n; yy++) {
            for (int xx = 0; xx < n; xx++) {
                int a = yy * vside + xx, b = a + 1, c = a + vside, d = c + 1;
                // CCW when viewed from +Z (above), so the top face is front-facing under Back cull
                ibuf.put(a).put(b).put(c).put(b).put(d).put(c);
            }
        }
        ibuf.rewind();

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, vbuf);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, nbuf);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, ibuf);
        mesh.updateBound();
        mesh.updateCounts();
        return mesh;
    }

    protected float waveHeight (float x, float y, float t)
    {
        return 0.9f * FastMath.sin(0.30f * x + 0.15f * y + 1.7f * t)
             + 0.6f * FastMath.sin(0.18f * x - 0.26f * y + 1.1f * t);
    }

    protected final File _rsrcRoot;
    protected final float _frame;

    protected static final float TILE_SIZE = 10f;
    protected static final int GRID_TILES = 8;     // 8x8 tiles of water
    protected static final int SAMPLES = 8;        // samples per tile
}
