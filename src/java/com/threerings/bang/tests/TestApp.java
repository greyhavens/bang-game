//
// $Id$

package com.threerings.bang.tests;

import java.awt.image.BufferedImage;

import com.samskivert.util.Config;

import com.jme.image.Image;
import com.jme.input.InputHandler;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;

import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLookAndFeel;
import com.jmex.bui.BRootNode;

import com.threerings.media.image.ImageManager;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.jme.JmeApp;
import com.threerings.jme.camera.CameraHandler;
import com.threerings.jme.tile.FringeConfiguration;
import com.threerings.openal.SoundManager;

import com.threerings.bang.client.BangApp;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.Model;
import com.threerings.bang.client.util.ImageCache;
import com.threerings.bang.client.util.ModelCache;
import com.threerings.bang.util.BasicContext;

/**
 * Initializes the various services needed to bootstrap any test program.
 */
public abstract class TestApp extends JmeApp
{
    protected void initTest ()
    {
        _ctx = new BasicContextImpl();
        _rsrcmgr = new ResourceManager("rsrc");
        _msgmgr = new MessageManager(MESSAGE_MANAGER_PREFIX);
        _lnf = BLookAndFeel.getDefaultLookAndFeel();
        _icache = new ImageCache(_ctx);
        _mcache = new ModelCache(_ctx);
        BangUI.init(_ctx);

        BDecoratedWindow window = new BDecoratedWindow(_lnf, "Test");
        createInterface(window);
        _ctx.getRootNode().addWindow(window);
        window.pack();
        window.center();
    }

    protected abstract void createInterface (BDecoratedWindow window);

    /**
     * The context implementation. This provides access to all of the
     * objects and services that are needed by the operating client.
     */
    protected class BasicContextImpl implements BasicContext
    {
        public DisplaySystem getDisplay () {
            return getContext().getDisplay();
        }

        public Renderer getRenderer () {
            return getContext().getRenderer();
        }

        public CameraHandler getCameraHandler () {
            return getContext().getCameraHandler();
        }

        public Node getGeometry () {
            return getContext().getGeometry();
        }

        public Node getInterface () {
            return getContext().getInterface();
        }

        public InputHandler getInputHandler () {
            return getContext().getInputHandler();
        }

        public BRootNode getRootNode () {
            return getContext().getRootNode();
        }

        public ResourceManager getResourceManager () {
            return _rsrcmgr;
        }

        public MessageManager getMessageManager () {
            return _msgmgr;
        }

        public BLookAndFeel getLookAndFeel () {
            return _lnf;
        }

        public FringeConfiguration getFringeConfig () {
            return null;
        }

        public BangApp getApp () {
            return null;
        }

        public ImageManager getImageManager () {
            return null;
        }

        public SoundManager getSoundManager () {
            return null;
        }

        public String xlate (String bundle, String message) {
            MessageBundle mb = getMessageManager().getBundle(bundle);
            return (mb == null) ? message : mb.xlate(message);
        }

        public Model loadModel (String type, String name) {
            return _mcache.getModel(type, name);
        }

        public Image loadImage (String rsrcPath) {
            return _icache.getImage(rsrcPath);
        }

        public BufferedImage loadBufferedImage (String rsrcPath) {
            return _icache.getBufferedImage(rsrcPath);
        }
    }

    protected BasicContext _ctx;
    protected Config _config = new Config("bang");
    protected ResourceManager _rsrcmgr;
    protected MessageManager _msgmgr;
    protected BLookAndFeel _lnf;
    protected ImageCache _icache;
    protected ModelCache _mcache;

    /** The prefix prepended to localization bundle names before looking
     * them up in the classpath. */
    protected static final String MESSAGE_MANAGER_PREFIX = "rsrc.i18n";
}
