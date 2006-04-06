//
// $Id$

package com.threerings.bang.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.swing.JFrame;

import java.util.logging.Level;

import com.jme.image.Texture;
import com.jme.input.InputHandler;
import com.jme.light.PointLight;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.math.FastMath;
import com.jme.math.Matrix3f;
import com.jme.renderer.ColorRGBA;
import com.jme.math.Vector3f;
import com.jme.util.TextureManager;
import com.jme.util.LoggingSystem;

import com.jmex.bui.BImage;
import com.jmex.bui.BRootNode;
import com.jmex.bui.BStyleSheet;

import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.cast.CharacterManager;
import com.threerings.media.image.ImageManager;
import com.threerings.openal.SoundManager;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.jme.JmeApp;
import com.threerings.jme.JmeCanvasApp;
import com.threerings.jme.camera.CameraHandler;

import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.GlobalKeyManager;
import com.threerings.bang.client.Model;
import com.threerings.bang.client.util.ImageCache;
import com.threerings.bang.client.util.ModelCache;
import com.threerings.bang.client.util.TextureCache;
import com.threerings.bang.game.data.Terrain;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.*;

/**
 * The main entry point for the application.
 */
public class ViewerApp extends JmeCanvasApp
{
    public static String[] appArgs;

    public static ViewerApp app;

    public static void main (String[] args)
    {
        appArgs = args;

        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();
        LoggingSystem.getLogger().setLevel(Level.WARNING);

        app = new ViewerApp();
        app.create();
        app.run();
    }

    public void create ()
    {
        JFrame frame = new JFrame("Model Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(app.getCanvas(), BorderLayout.CENTER);
        frame.getContentPane().add(
            _cpanel = new ControlPanel(frame, _bctx), BorderLayout.EAST);
        frame.setSize(new Dimension(1149, 768));
        frame.setVisible(true);
    }

    public boolean init ()
    {
        // these manage local client resources
        _rsrcmgr = new ResourceManager("rsrc");
        _msgmgr = new MessageManager(MESSAGE_MANAGER_PREFIX);
        _icache = new ImageCache(_bctx);
        _tcache = new TextureCache(_bctx);
        _mcache = new ModelCache(_bctx);

        // create our media managers
        _imgmgr = new ImageManager(
            _rsrcmgr, new ImageManager.OptimalImageCreator() {
            public BufferedImage createImage (int w, int h, int t) {
                // TODO: take screen depth into account
                switch (t) {
                case Transparency.OPAQUE:
                    return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                default:
                    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                }
            }
        });
        
        try {
            _rsrcmgr.initBundles(
                null, "config/resource/manager.properties", null);
        } catch (IOException ioe) {
            // TODO: report to the client
            log.log(Level.WARNING, "Failed to initialize rsrcmgr.", ioe);
        }
        
        RenderUtil.init(_bctx);
        
        if (!super.init()) {
            return false;
        }
        
        // initialize our user interface bits
        _keymgr.init(_bctx);
        BangUI.init(_bctx);
        
        _cpanel.init();
        return true;
    }

    protected void initRoot ()
    {
        super.initRoot();

        TextureState ts = RenderUtil.getGroundTexture(Terrain.DIRT);

        // add some reference floor tiles
        _geom.attachChild(makeFloor(-1, -1, ColorRGBA.blue, ts));
        _geom.attachChild(makeFloor(1, -1, ColorRGBA.gray, ts));
        _geom.attachChild(makeFloor(1, 1, ColorRGBA.blue, ts));
        _geom.attachChild(makeFloor(-1, 1, ColorRGBA.gray, ts));

        // position and rotate the camera
        Vector3f loc = new Vector3f(5, -15, 15);
        _camera.setLocation(loc);
        Matrix3f rotm = new Matrix3f();
        rotm.fromAngleAxis(-FastMath.PI/2, _camera.getLeft());
        rotm.mult(_camera.getDirection(), _camera.getDirection());
        rotm.mult(_camera.getUp(), _camera.getUp());
        rotm.mult(_camera.getLeft(), _camera.getLeft());
        rotm.fromAngleAxis(FastMath.PI/8, _camera.getUp());
        rotm.mult(_camera.getDirection(), _camera.getDirection());
        rotm.mult(_camera.getUp(), _camera.getUp());
        rotm.mult(_camera.getLeft(), _camera.getLeft());
        rotm.fromAngleAxis(FastMath.PI/6, _camera.getLeft());
        rotm.mult(_camera.getDirection(), _camera.getDirection());
        rotm.mult(_camera.getUp(), _camera.getUp());
        rotm.mult(_camera.getLeft(), _camera.getLeft());
        _camera.update();
    }

    protected Quad makeFloor (int x, int y, ColorRGBA color, TextureState ts)
    {
        Quad floor = new Quad("floor", 25, 25);
        floor.setLocalTranslation(new Vector3f(x * 12.5f, y * 12.5f, 0));
        floor.setSolidColor(color);
        floor.setRenderState(ts);
        floor.updateRenderState();
//         floor.setLightCombineMode(LightState.OFF);
        return floor;
    }

    protected void initLighting ()
    {
        PointLight light = new PointLight();
        light.setDiffuse(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
        light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        light.setLocation(new Vector3f(50, 0, 50));
        light.setAttenuate(true);
        light.setLinear(0.005f);
        light.setEnabled(true);

        LightState lights = _display.getRenderer().createLightState();
        lights.setEnabled(true);
        lights.attach(light);
        _geom.setRenderState(lights);
    }

    protected ViewerApp ()
    {
        super(1024, 768);
    }

    /** Provides access to various needed bits. */
    protected BasicContext _bctx = new BasicContext() {
        public DisplaySystem getDisplay () {
            return _ctx.getDisplay();
        }

        public Renderer getRenderer () {
            return _ctx.getRenderer();
        }

        public CameraHandler getCameraHandler () {
            return _ctx.getCameraHandler();
        }

        public Node getGeometry () {
            return _ctx.getGeometry();
        }

        public Node getInterface () {
            return _ctx.getInterface();
        }

        public InputHandler getInputHandler () {
            return _ctx.getInputHandler();
        }

        public BRootNode getRootNode () {
            return _ctx.getRootNode();
        }
        
        public ResourceManager getResourceManager () {
            return _rsrcmgr;
        }

        public MessageManager getMessageManager () {
            return _msgmgr;
        }

        public BStyleSheet getStyleSheet () {
            return BangUI.stylesheet;
        }

        public JmeApp getApp () {
            return ViewerApp.this;
        }

        public GlobalKeyManager getKeyManager () {
            return _keymgr;
        }

        public ImageManager getImageManager () {
            return _imgmgr;
        }

        public SoundManager getSoundManager () {
            return null;
        }

        public ImageCache getImageCache () {
            return _icache;
        }

        public TextureCache getTextureCache () {
            return _tcache;
        }

        public CharacterManager getCharacterManager () {
            return null;
        }

        public AvatarLogic getAvatarLogic () {
            return null;
        }

        public String xlate (String bundle, String message) {
            MessageBundle mb = getMessageManager().getBundle(bundle);
            return (mb == null) ? message : mb.xlate(message);
        }

        public Model loadModel (String type, String name) {
            return _mcache.getModel(type, name); 
        }

        public BImage loadImage (String rsrcPath) {
            return _icache.getBImage(rsrcPath);
        }
    };
    
    protected ControlPanel _cpanel;
    
    protected GlobalKeyManager _keymgr = new GlobalKeyManager();

    protected MessageManager _msgmgr;
    protected ResourceManager _rsrcmgr;
    protected ImageManager _imgmgr;

    protected ImageCache _icache;
    protected TextureCache _tcache;
    protected ModelCache _mcache;
    
    /** The prefix prepended to localization bundle names before looking
     * them up in the classpath. */
    protected static final String MESSAGE_MANAGER_PREFIX = "rsrc.i18n";
}
