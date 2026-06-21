//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

/**
 * Shared scaffold for the headless offscreen render-to-PNG harnesses (Phase&nbsp;5, harness&nbsp;#1).
 *
 * <p>This is the reusable base extracted from the {@code RenderToPng}/{@code RenderWaterToPng}
 * seeds: it owns everything that is identical between every render mode so the individual modes only
 * supply the scene to draw and (optionally) the camera framing. Concretely it provides:
 *
 * <ul>
 *   <li>the {@link AppSettings}/{@link JmeContext.Type#OffscreenSurface} launch boilerplate — a
 *       hidden LWJGL3 GL context with no visible window ({@link #launch});</li>
 *   <li>a color-texture-backed {@link FrameBuffer} captured by a one-shot {@link CaptureProcessor}
 *       and written to a PNG. The readback builds the {@link BufferedImage} channel-by-channel
 *       (flipping Y, RGBA byte order) — the correct path established by the water harness;
 *       {@code Screenshots.convertScreenShot} assumes BGRA and tints the image blue on this
 *       driver;</li>
 *   <li>{@code BoardView}-style lighting — one white directional key light + a soft grey ambient
 *       ({@link #lightLikeBoard});</li>
 *   <li>bounding-box framing of a spatial from a 3/4 angle, in the board's <b>Z-up</b> convention
 *       ({@link #frameBoundingBox}). The game scene graph is Z-up (every {@code com.threerings.bang}
 *       client class frames with {@link Vector3f#UNIT_Z} as the up vector); a Y-up camera lays the
 *       Z-up character models on their side, which is exactly the framing bug to avoid when
 *       diagnosing the Phase-6 sprite defects.</li>
 * </ul>
 *
 * <p>A subclass implements {@link #buildScene} to return the lit, framing-ready root node and may
 * override {@link #createCamera} to control the view; the base renders one frame and writes the PNG.
 * The harness stays on the isolated jME3 + LWJGL3 + {@code project(":app")} classpath
 * (the {@code renderToPngRuntime} configuration) — no fork, no LWJGL2, no sealing clash.
 */
public abstract class OffscreenRenderApp extends SimpleApplication
{
    /**
     * Launches an offscreen render app with the standard settings and a hidden LWJGL3 window. The
     * subclass must already be constructed with its output PNG and scene parameters.
     */
    public static void launch (OffscreenRenderApp app)
    {
        app.setShowSettings(false);
        AppSettings settings = new AppSettings(true);
        settings.setWidth(WIDTH);
        settings.setHeight(HEIGHT);
        settings.setSamples(4);
        app.setSettings(settings);
        app.setPauseOnLostFocus(false);
        // a real GL context is needed to rasterise; LWJGL3 gives us a hidden offscreen window.
        app.start(JmeContext.Type.OffscreenSurface);
    }

    protected OffscreenRenderApp (File outPng)
    {
        _outPng = outPng.getAbsoluteFile();
    }

    @Override public void simpleInitApp ()
    {
        Node scene = buildScene();
        scene.updateGeometricState();

        Camera offCam = createCamera(scene);

        // render into our own color-texture-backed FrameBuffer and read that back — robust on an
        // OffscreenSurface context where the default framebuffer read is unreliable. Use an sRGB
        // color target so jME3's gamma-correct pipeline re-encodes correctly on write (a plain
        // linear RGBA8 target comes out dark/mis-tinted).
        _fb = new FrameBuffer(WIDTH, HEIGHT, 1);
        _colorTex = new Texture2D(WIDTH, HEIGHT, Image.Format.RGBA8);
        _colorTex.getImage().setColorSpace(ColorSpace.sRGB);
        _fb.setDepthBuffer(Image.Format.Depth);
        _fb.setColorTexture(_colorTex);

        ViewPort off = renderManager.createMainView("offscreen", offCam);
        off.setClearFlags(true, true, true);
        off.setBackgroundColor(backgroundColor());
        off.setOutputFrameBuffer(_fb);
        off.attachScene(scene);
        off.addProcessor(new CaptureProcessor());
    }

    /**
     * Builds and returns the scene root to render. Implementations should attach their content and
     * may call {@link #lightLikeBoard}; the base calls {@code updateGeometricState()} afterwards.
     */
    protected abstract Node buildScene ();

    /**
     * Returns the camera framing the scene. The default frames the scene's world bounding box from
     * a 3/4 angle in the board's Z-up convention; override for a fixed viewpoint.
     */
    protected Camera createCamera (Spatial scene)
    {
        return frameBoundingBox(scene);
    }

    /** The viewport clear color. Override to change the backdrop. */
    protected ColorRGBA backgroundColor ()
    {
        return new ColorRGBA(0.45f, 0.5f, 0.55f, 1f);
    }

    /**
     * Lights the given node the way {@code BoardView} does: a single white directional key light
     * plus a soft grey ambient term (the default board's light&nbsp;0 is white directional, its
     * ambient a dark grey).
     */
    protected void lightLikeBoard (Node node)
    {
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.5f, -0.5f, -1f).normalizeLocal());
        dl.setColor(ColorRGBA.White);
        node.addLight(dl);
        AmbientLight al = new AmbientLight();
        al.setColor(new ColorRGBA(0.35f, 0.35f, 0.35f, 1f));
        node.addLight(al);
    }

    /**
     * Aims a fresh camera at a spatial's world bounding box from a 3/4 angle, framing the whole
     * model. The up vector is {@link Vector3f#UNIT_Z} to match the board's Z-up scene graph (so
     * Z-up character models stand upright, not on their side).
     */
    protected Camera frameBoundingBox (Spatial spatial)
    {
        BoundingBox bb = (BoundingBox)spatial.getWorldBound();
        Vector3f center = bb.getCenter();
        float radius = Math.max(bb.getXExtent(), Math.max(bb.getYExtent(), bb.getZExtent())) * 2.4f;
        // a 3/4 elevated view in Z-up: back along -Y, off to +X, up along +Z.
        Vector3f eye = center.add(new Vector3f(radius, -radius, radius * 0.9f));
        Camera offCam = new Camera(WIDTH, HEIGHT);
        offCam.setFrustumPerspective(45f, (float)WIDTH / HEIGHT, 0.1f, radius * 10f);
        offCam.setLocation(eye);
        offCam.lookAt(center, Vector3f.UNIT_Z);
        return offCam;
    }

    /** Reads the rendered framebuffer back to a PNG once the first frame is drawn. */
    protected class CaptureProcessor implements SceneProcessor
    {
        @Override public void initialize (RenderManager rm, ViewPort vp) { _rm = rm; }
        @Override public void reshape (ViewPort vp, int w, int h) { }
        @Override public boolean isInitialized () { return _rm != null; }
        @Override public void preFrame (float tpf) { }
        @Override public void postQueue (RenderQueue rq) { }
        @Override public void setProfiler (AppProfiler profiler) { }
        @Override public void cleanup () { }

        @Override public void postFrame (FrameBuffer out)
        {
            if (_captured) {
                return;
            }
            _captured = true;
            ByteBuffer buf = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
            renderer.readFrameBuffer(_fb, buf);
            // readFrameBuffer returns RGBA byte order on this driver; build the image directly
            // (flipping Y) so the channels are correct — Screenshots.convertScreenShot assumes BGRA
            // and would swap R<->B (the "blue tint" the early harness notes describe).
            BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            buf.rewind();
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int i = ((HEIGHT - 1 - y) * WIDTH + x) * 4;
                    int r = buf.get(i) & 0xFF, g = buf.get(i+1) & 0xFF;
                    int b = buf.get(i+2) & 0xFF;
                    // Force the captured frame OPAQUE. The backdrop is always cleared opaque and the
                    // RGB channels already hold the fully-composited result; but ADDITIVE-blended
                    // particle quads (e.g. the get_bonus star-burst, explosion) write their texture's
                    // alpha=0 transparent texels straight into the framebuffer ALPHA, punching
                    // alpha=0 "holes" wherever a particle quad's transparent border drew. The live
                    // game renders to an opaque backbuffer with no alpha export, so those holes are an
                    // OFFSCREEN-ONLY artifact (they show as white/checkerboard in a PNG viewer and
                    // made the BUG-12 golden look like a "white box"). Writing alpha=255 makes the PNG
                    // match what the player sees and keeps the RGB bit-exact for the golden diff.
                    img.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
                }
            }
            try {
                ImageIO.write(img, "png", _outPng);
                System.out.println("Wrote " + _outPng);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // stop after one frame
            enqueue(() -> { stop(); return null; });
        }

        private RenderManager _rm;
        private boolean _captured;
    }

    protected final File _outPng;

    protected FrameBuffer _fb;
    protected Texture2D _colorTex;

    protected static final int WIDTH = 800;
    protected static final int HEIGHT = 600;
}
