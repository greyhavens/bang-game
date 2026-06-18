//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import com.threerings.jme.model.Model;

/**
 * BUG-11 offscreen verification harness: renders a <b>real baked unit model</b> with a floating
 * effect icon billboarded above it, from several camera azimuths around the Z-up board, so the
 * icon-vs-head offset and its azimuth dependence can be measured empirically and diffed against the
 * fork.
 *
 * <p>It reproduces the floating-icon placement done by {@code IconViz}/{@code DamageIconViz} in the
 * live client (see {@code com.threerings.bang.game.client.effect.IconViz#didInit}): a {@link Node}
 * carrying a {@link BillboardControl} is attached to the sprite at a local Z of
 * {@code piece.getHeight() * TILE_SIZE}, and a centered, screen-facing textured {@link Quad} of side
 * {@code ICON_SIZE = TILE_SIZE/2} hangs on it. Because {@code IconConfig.createIcon} needs a live
 * {@code BasicContext} (and {@code :client:shared} is deliberately off this harness's isolated
 * jME3+LWJGL3+{@code :app} classpath), the icon geometry is built inline here with the SAME constants
 * and the SAME corner-anchored-Quad re-centering the live code uses — the visual placement is
 * identical, which is all BUG-11 is about.
 *
 * <p>The whole point of BUG-11: the billboard anchor is the unit sprite's <i>origin</i> raised by
 * {@code getHeight()*TILE_SIZE}, but a unit model's visual HEAD is horizontally/vertically offset
 * from that origin inside the model's bounding box. Under perspective the head and the
 * origin-anchored icon project to different screen pixels, and the gap swings as the camera orbits.
 * This harness renders both anchor modes so the fix can be judged:
 * <ul>
 *   <li>{@code --anchor=origin} (default): the live anchor — sprite origin + {@code getHeight()*TILE}.
 *       Expect the icon to drift relative to the head as azimuth changes (the bug as observed).</li>
 *   <li>{@code --anchor=top}: BUG-11 option (a) — anchor at the model's world bounding-box TOP-CENTER
 *       (the geometric apex of the actual mesh). Expect the icon to sit over the head at every
 *       azimuth (azimuth-stable). If this run is stable and the origin run is not, option (a) is
 *       empirically validated.</li>
 * </ul>
 *
 * <p>Emits {@code <outPrefix>-azNNN.png} for each azimuth (PNG channels/orientation handled exactly
 * like {@link OffscreenRenderApp} / {@link RenderCounterToPng}). Feed the PNGs to {@code bin/qa}
 * (e.g. {@code qa_locate.py --template skull} to read the icon centre, or {@code qa_diff.py} against a
 * matched fork capture) to get the head-vs-icon pixel offset per azimuth without a server or client.
 *
 * <p>Usage: {@code RenderEffectToPng <rsrc root> <model type path> <icon texture> <outPrefix>
 * [origin|top] [anim] [time]}, e.g.
 * {@code RenderEffectToPng .../staging/rsrc units/frontier_town/shotgunner
 * textures/effects/killed.png /tmp/bug11-origin origin standing 0}.
 */
public class RenderEffectToPng extends SimpleApplication
{
    public static void main (String[] args)
    {
        if (args.length < 4) {
            System.err.println("Usage: RenderEffectToPng <rsrc root> <model type path> " +
                "<icon texture path> <outPrefix> [origin|top] [anim] [time]");
            System.exit(1);
        }
        String anchor = args.length > 4 ? args[4] : "origin";
        String anim = args.length > 5 ? args[5] : null;
        float time = args.length > 6 ? Float.parseFloat(args[6]) : 0f;

        RenderEffectToPng app = new RenderEffectToPng(
            args[0], args[1], args[2], args[3], anchor, anim, time);
        app.setShowSettings(false);
        AppSettings settings = new AppSettings(true);
        settings.setWidth(WIDTH);
        settings.setHeight(HEIGHT);
        settings.setSamples(4);
        app.setSettings(settings);
        app.setPauseOnLostFocus(false);
        app.start(JmeContext.Type.OffscreenSurface);
    }

    public RenderEffectToPng (String rsrcRoot, String modelType, String iconTex, String outPrefix,
                              String anchor, String anim, float time)
    {
        _rsrcRoot = new File(rsrcRoot).getAbsoluteFile();
        _modelType = modelType;
        _iconTex = iconTex;
        _outPrefix = outPrefix;
        _anchorTop = "top".equalsIgnoreCase(anchor);
        _anim = anim;
        _time = time;
    }

    @Override public void simpleInitApp ()
    {
        flyCam.setEnabled(false);
        assetManager.registerLocator(_rsrcRoot.getPath(), FileLocator.class);

        // --- the real baked unit model, through the runtime load/pose path ---
        Model model = Models.loadModel(assetManager, _modelType);
        if (!"list".equals(_anim)) {
            Models.poseAnimation(model, _anim, _time);
        }
        Node sprite = new Node("sprite");      // stands in for the PieceSprite Node
        sprite.attachChild(model);
        sprite.updateGeometricState();

        // measure the model's bounding box (world space, with the model attached to the sprite).
        BoundingBox bb = (BoundingBox)model.getWorldBound();
        // a typical Bang unit piece is 1 tile high; getHeight() defaults to 1 => zOffset = TILE_SIZE.
        float zOffset;
        Vector3f anchorXY;
        if (_anchorTop) {
            // BUG-11 option (a): anchor at the actual mesh apex (bbox top-centre), so the icon
            // tracks the head regardless of camera azimuth.
            Vector3f c = bb.getCenter();
            zOffset = c.z + bb.getZExtent();
            anchorXY = new Vector3f(c.x, c.y, 0f);
            System.out.println("RenderEffectToPng: anchor=top  bboxTopZ=" + zOffset +
                " centerXY=(" + c.x + "," + c.y + ")");
        } else {
            // the LIVE anchor: sprite origin raised by getHeight()*TILE_SIZE (IconViz._zOffset).
            zOffset = PIECE_HEIGHT * TILE_SIZE;
            anchorXY = new Vector3f(0f, 0f, 0f);
            System.out.println("RenderEffectToPng: anchor=origin  zOffset=" + zOffset +
                " (getHeight()=" + PIECE_HEIGHT + " * TILE_SIZE=" + TILE_SIZE + ")");
        }
        System.out.println("  model bbox center=" + bb.getCenter() +
            " extent=(" + bb.getXExtent() + "," + bb.getYExtent() + "," + bb.getZExtent() + ")");

        // --- the floating icon billboard, exactly as IconViz builds it ---
        Node billboard = new Node("billboard");
        billboard.addControl(new BillboardControl());           // screen-facing, like the live viz
        billboard.attachChild(makeCenteredIcon(_iconTex, ICON_SIZE, ICON_SIZE));
        billboard.setLocalTranslation(anchorXY.x, anchorXY.y, zOffset);
        sprite.attachChild(billboard);

        rootNode.attachChild(sprite);
        rootNode.updateGeometricState();

        // light + frame around the sprite origin so all azimuth shots are comparable.
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.5f, -0.5f, -1f).normalizeLocal());
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);
        AmbientLight al = new AmbientLight();
        al.setColor(new ColorRGBA(0.35f, 0.35f, 0.35f, 1f));
        rootNode.addLight(al);

        // orbit radius/height sized to the model so it fills a comparable area at every azimuth.
        _radius = Math.max(zOffset, Math.max(bb.getXExtent(), bb.getYExtent())) * 3.0f;
        _lookZ = zOffset * 0.55f;       // aim between the feet and the icon

        _fb = new FrameBuffer(WIDTH, HEIGHT, 1);
        _colorTex = new Texture2D(WIDTH, HEIGHT, Image.Format.RGBA8);
        _colorTex.getImage().setColorSpace(ColorSpace.sRGB);
        _fb.setDepthBuffer(Image.Format.Depth);
        _fb.setColorTexture(_colorTex);

        _offCam = new Camera(WIDTH, HEIGHT);
        _offCam.setFrustumPerspective(45f, (float)WIDTH / HEIGHT, 0.1f, _radius * 10f);

        ViewPort off = renderManager.createMainView("offscreen", _offCam);
        off.setClearFlags(true, true, true);
        off.setBackgroundColor(new ColorRGBA(0.45f, 0.5f, 0.55f, 1f));
        off.setOutputFrameBuffer(_fb);
        off.attachScene(rootNode);
        off.addProcessor(new CaptureProcessor());
    }

    /** Builds a centered, alpha-blended, screen-facing icon quad — same placement as IconViz. */
    protected Geometry makeCenteredIcon (String texPath, float width, float height)
    {
        Texture2D tex = (Texture2D)assetManager.loadTexture(texPath);
        Geometry icon = new Geometry("icon", new Quad(width, height));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        icon.setMaterial(mat);
        icon.setQueueBucket(Bucket.Transparent);
        // jME3 Quad is corner-anchored (0,0)-(w,h); re-center on the billboard origin, exactly like
        // IconViz.attachCentered does, so the icon hangs on the pivot rather than up-and-to-the-side.
        icon.setLocalTranslation(-width / 2f, -height / 2f, 0f);
        return icon;
    }

    @Override public void simpleUpdate (float tpf)
    {
        if (_idx >= AZIMUTHS.length) {
            stop();
            return;
        }
        if (_pending) {
            return;     // waiting for the capture of the current azimuth
        }
        float az = AZIMUTHS[_idx] * FastMath.DEG_TO_RAD;
        Vector3f eye = new Vector3f(
            _radius * FastMath.cos(az), _radius * FastMath.sin(az), _radius * 0.6f);
        _offCam.setLocation(eye);
        _offCam.lookAt(new Vector3f(0f, 0f, _lookZ), Vector3f.UNIT_Z);
        rootNode.updateGeometricState();
        _pending = true;
    }

    protected void capture ()
    {
        ByteBuffer buf = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
        renderer.readFrameBuffer(_fb, buf);
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        buf.rewind();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int i = ((HEIGHT - 1 - y) * WIDTH + x) * 4;
                int rr = buf.get(i) & 0xFF, g = buf.get(i+1) & 0xFF;
                int b = buf.get(i+2) & 0xFF, a = buf.get(i+3) & 0xFF;
                img.setRGB(x, y, (a << 24) | (rr << 16) | (g << 8) | b);
            }
        }
        File out = new File(_outPrefix + "-az" + (int)AZIMUTHS[_idx] + ".png");
        try {
            ImageIO.write(img, "png", out);
            System.out.println("Wrote " + out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        _idx++;
        _pending = false;
    }

    protected class CaptureProcessor implements SceneProcessor
    {
        @Override public void initialize (RenderManager rm, ViewPort vp) { _rm = rm; }
        @Override public void reshape (ViewPort vp, int w, int h) { }
        @Override public boolean isInitialized () { return _rm != null; }
        @Override public void preFrame (float tpf) { }
        @Override public void postQueue (RenderQueue rq) { }
        @Override public void setProfiler (AppProfiler profiler) { }
        @Override public void cleanup () { }
        @Override public void postFrame (FrameBuffer out) {
            if (_pending) {
                capture();
            }
        }
        private RenderManager _rm;
    }

    protected final File _rsrcRoot;
    protected final String _modelType, _iconTex, _outPrefix, _anim;
    protected final boolean _anchorTop;
    protected final float _time;

    protected Camera _offCam;
    protected FrameBuffer _fb;
    protected Texture2D _colorTex;
    protected float _radius, _lookZ;
    protected int _idx;
    protected boolean _pending;

    // mirrors com.threerings.bang.client.BangMetrics.TILE_SIZE and the IconViz icon size; kept as
    // local constants so the harness stays off the :client:shared classpath (jME3+LWJGL3+:app only).
    protected static final float TILE_SIZE = 10f;
    protected static final float ICON_SIZE = TILE_SIZE / 2f;
    // Piece.getHeight() returns 1 for a standard unit (UnitSprite floats its icons at 1*TILE_SIZE).
    protected static final float PIECE_HEIGHT = 1f;

    protected static final float[] AZIMUTHS = { 0, 45, 90, 135, 180, 225, 270, 315 };
    protected static final int WIDTH = 400, HEIGHT = 400;
}
