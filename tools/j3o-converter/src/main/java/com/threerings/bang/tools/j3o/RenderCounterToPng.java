//
// $Id$

package com.threerings.bang.tools.j3o;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.queue.RenderQueue;

/**
 * BUG-4 offscreen verification harness. Builds the live counter-text billboard exactly as
 * {@code CounterSprite}/{@code GenericCounterNode} do (a {@link BillboardControl} on a {@link Node}
 * carrying a textured {@link Quad} whose UVs come from the same AWT text-render + tcoords logic as
 * {@code RenderUtil.createTextTexture}) and renders it from several camera azimuths around the Z-up
 * board so the left-right mirror (azimuth-dependent) and the bottom-clip facets can be seen.
 *
 * <p>Usage: {@code RenderCounterToPng <fontTtf> <text> <alignment> <outPrefix>} — emits
 * {@code <outPrefix>-azNNN.png} for a handful of azimuths. {@code alignment} is one of
 * {@code Screen|Camera|AxialZ}.
 */
public class RenderCounterToPng extends SimpleApplication
{
    public static void main (String[] args) throws Exception
    {
        if (args.length < 4) {
            System.err.println("Usage: RenderCounterToPng <fontTtf> <text> " +
                "<Screen|Camera|AxialZ> <outPrefix>");
            System.exit(1);
        }
        Font font = Font.createFont(Font.TRUETYPE_FONT, new File(args[0]))
            .deriveFont(Font.BOLD, 72f);
        RenderCounterToPng app = new RenderCounterToPng(
            font, args[1], BillboardControl.Alignment.valueOf(args[2]), args[3]);
        AppSettings settings = new AppSettings(true);
        settings.setWidth(WIDTH);
        settings.setHeight(HEIGHT);
        settings.setSamples(4);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.start(JmeContext.Type.OffscreenSurface);
    }

    public RenderCounterToPng (Font font, String text,
            BillboardControl.Alignment align, String outPrefix)
    {
        _font = font;
        _text = text;
        _align = align;
        _outPrefix = outPrefix;
    }

    @Override public void simpleInitApp ()
    {
        flyCam.setEnabled(false);

        // build the billboard exactly like CounterSprite.createGeometry/updateCount
        Vector2f[] tcoords = new Vector2f[4];
        Texture2D tex = createTextTexture(_text, tcoords);
        // CounterSprite sizing
        float TILE = 16f;
        float qrat = TILE * 0.8f / tcoords[2].y;
        Quad mesh = new Quad(qrat * tcoords[2].x, qrat * tcoords[2].y);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(tcoords));
        mesh.updateBound();
        Geometry quad = new Geometry("counter", mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(
            com.jme3.material.RenderState.BlendMode.Alpha);
        quad.setMaterial(mat);
        quad.setQueueBucket(Bucket.Transparent);
        // center the quad so the billboard pivot is glyph-centre (visual clarity only)
        quad.setLocalTranslation(-qrat * tcoords[2].x / 2, -qrat * tcoords[2].y / 2, 0);

        Node bbn = new Node("cbillboard");
        BillboardControl bb = new BillboardControl();
        bb.setAlignment(_align);
        bbn.addControl(bb);
        bbn.attachChild(quad);

        // emulate the sprite parent: a yaw about Z-up (the board sprite orientation)
        Node sprite = new Node("sprite");
        sprite.attachChild(bbn);
        rootNode.attachChild(sprite);

        _bbn = bbn;
        _sprite = sprite;

        // simple frame buffer setup
        _fb = new FrameBuffer(WIDTH, HEIGHT, 1);
        _colorTex = new Texture2D(WIDTH, HEIGHT, Image.Format.RGBA8);
        _colorTex.getImage().setColorSpace(ColorSpace.sRGB);
        _fb.setDepthBuffer(Image.Format.Depth);
        _fb.setColorTexture(_colorTex);

        Camera offCam = new Camera(WIDTH, HEIGHT);
        offCam.setFrustumPerspective(45f, (float)WIDTH / HEIGHT, 0.1f, 1000f);
        _offCam = offCam;

        ViewPort off = renderManager.createMainView("offscreen", offCam);
        off.setClearFlags(true, true, true);
        off.setBackgroundColor(new ColorRGBA(0.2f, 0.2f, 0.25f, 1f));
        off.setOutputFrameBuffer(_fb);
        off.attachScene(rootNode);
        off.addProcessor(new CaptureProcessor());

        // give the sprite a fixed yaw so the parent has a non-trivial Z-up rotation
        sprite.setLocalRotation(new com.jme3.math.Quaternion().fromAngleAxis(
            FastMath.PI * 0.37f, Vector3f.UNIT_Z));
    }

    @Override public void simpleUpdate (float tpf)
    {
        if (_idx >= AZIMUTHS.length) {
            stop();
            return;
        }
        if (_pending) {
            return; // wait for capture
        }
        // place the camera at this azimuth around the billboard, Z-up, looking at origin
        float az = AZIMUTHS[_idx] * FastMath.DEG_TO_RAD;
        float r = 20f, h = 12f;
        Vector3f eye = new Vector3f(
            r * FastMath.cos(az), r * FastMath.sin(az), h);
        _offCam.setLocation(eye);
        _offCam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Z);
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

    /** Replicates RenderUtil.createTextTexture (outline path) exactly. */
    protected Texture2D createTextTexture (String text, Vector2f[] tcoords)
    {
        ColorRGBA color = ColorRGBA.Yellow, ocolor = ColorRGBA.Black;
        Color acolor = new Color(color.r, color.g, color.b, color.a);
        Color oacolor = new Color(ocolor.r, ocolor.g, ocolor.b, ocolor.a);

        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gfx = scratch.createGraphics();
        gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        TextLayout layout = new TextLayout(text, _font, gfx.getFontRenderContext());
        gfx.dispose();

        // mirror RenderUtil.createTextTexture's full-outline sizing (BUG-4 clip fix)
        int outline = 1;
        Rectangle2D ob = layout.getOutline(null).getBounds2D();
        int pad = Math.max(2, (outline + 1) / 2 + 1);
        int gw = (int)Math.ceil(ob.getWidth());
        int gh = (int)Math.ceil(ob.getHeight());
        int width = gw + 2 * pad;
        int height = gh + 2 * pad;
        float penX = (float)(-ob.getX() + pad);
        float penY = (float)(-ob.getY() + pad);

        int tsize = nextPOT(Math.max(width, Math.max(height, 1)));
        BufferedImage image = new BufferedImage(tsize, tsize, BufferedImage.TYPE_INT_ARGB);
        gfx = image.createGraphics();
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        gfx.setColor(acolor);
        // BUG-4 mirror fix: pre-flip the glyph vertically within [0,height] to match RenderUtil.
        gfx.translate(0, height);
        gfx.scale(1, -1);
        gfx.translate(penX, penY);
        gfx.fill(layout.getOutline(null));
        gfx.setColor(oacolor);
        gfx.draw(layout.getOutline(null));
        gfx.dispose();

        // BUG-4 mirror facet: flip V so AWT image top row -> quad top (convertImage does not flip).
        float tsf = tsize;
        tcoords[0] = new Vector2f(0, 0);
        tcoords[1] = new Vector2f(width/tsf, 0);
        tcoords[2] = new Vector2f(width/tsf, height/tsf);
        tcoords[3] = new Vector2f(0, height/tsf);

        // Replicate ImageCache.convertImage EXACTLY: copy AWT raster rows straight into a jME
        // RGBA8 Image, row 0 first, NO vertical flip. (This is the live behaviour; AWTLoader's own
        // flip would not match.) jME then samples texcoord V=0 at data row 0.
        Image jimg = toJmeNoFlip(image);
        Texture2D tex = new Texture2D(jimg);
        return tex;
    }

    /** Builds a jME RGBA8 Image from an INT_ARGB BufferedImage, row 0 first, no flip. */
    static Image toJmeNoFlip (BufferedImage bi)
    {
        int w = bi.getWidth(), h = bi.getHeight();
        java.nio.ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = bi.getRGB(x, y);
                buf.put((byte)((argb >> 16) & 0xFF)); // R
                buf.put((byte)((argb >> 8) & 0xFF));  // G
                buf.put((byte)(argb & 0xFF));         // B
                buf.put((byte)((argb >> 24) & 0xFF)); // A
            }
        }
        buf.flip();
        return new Image(Image.Format.RGBA8, w, h, buf, ColorSpace.sRGB);
    }

    static int nextPOT (int v)
    {
        int p = 1;
        while (p < v) p <<= 1;
        return p;
    }

    /** Minimal AWT->jME image conversion. */
    static class AWTLoaderHelper
    {
        Image load (BufferedImage bi)
        {
            // live path (ImageCache.convertImage) does NOT flip Y; match it (flipY=false) so the
            // offscreen render reproduces the live texture orientation exactly.
            return new com.jme3.texture.plugins.AWTLoader().load(bi, false);
        }
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

    protected final Font _font;
    protected final String _text;
    protected final BillboardControl.Alignment _align;
    protected final String _outPrefix;

    protected Node _bbn, _sprite;
    protected Camera _offCam;
    protected FrameBuffer _fb;
    protected Texture2D _colorTex;
    protected int _idx;
    protected boolean _pending;

    protected static final float[] AZIMUTHS = { 0, 45, 90, 135, 180, 225, 270, 315 };
    protected static final int WIDTH = 400, HEIGHT = 400;
}
