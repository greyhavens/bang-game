//
// $Id$

package com.threerings.bang.tests;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.logging.Level;

import com.samskivert.util.Config;

import com.jme.input.InputHandler;
import com.jme.util.LoggingSystem;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;

import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLookAndFeel;
import com.jmex.bui.BRootNode;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.media.image.ImageManager;
import com.threerings.resource.ResourceManager;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.jme.JmeApp;
import com.threerings.jme.camera.CameraHandler;
import com.threerings.jme.tile.FringeConfiguration;
import com.threerings.openal.SoundManager;

import com.threerings.bang.client.BangApp;
import com.threerings.bang.client.ModelCache;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.ranch.client.UnitIcon;
import com.threerings.bang.util.BasicContext;

/**
 * A test program for displaying unit model icons.
 */
public class ModelIconTest extends JmeApp
{
    public static void main (String[] args)
    {
        LoggingSystem.getLogger().setLevel(Level.WARNING);
        ModelIconTest test = new ModelIconTest();
        if (test.init()) {
            test.initTest();
            test.run();
        } else {
            System.exit(-1);
        }
    }

    protected void initTest ()
    {
        _ctx = new BasicContextImpl();
        _rsrcmgr = new ResourceManager("rsrc");
        _lnf = BLookAndFeel.getDefaultLookAndFeel();
        _mcache = new ModelCache(_ctx);

        BDecoratedWindow window = new BDecoratedWindow(_lnf, "Test");
        createInterface(window);
        _ctx.getRootNode().addWindow(window);
        window.pack();
        window.center();
    }

    protected void createInterface (BDecoratedWindow window)
    {
        window.add(new UnitIcon(_ctx, -1, UnitConfig.getConfig("steamgunman")),
                   BorderLayout.WEST);
        window.add(new UnitIcon(_ctx, -1, UnitConfig.getConfig("gunslinger")),
                   BorderLayout.CENTER);
        window.add(new UnitIcon(_ctx, -1, UnitConfig.getConfig("dirigible")),
                   BorderLayout.EAST);
    }

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
            return null;
        }

        public BLookAndFeel getLookAndFeel () {
            return _lnf;
        }

        public ModelCache getModelCache () {
            return _mcache;
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

        public BufferedImage loadImage (String rsrcPath) {
            try {
                return ImageIO.read(
                    getResourceManager().getImageResource(rsrcPath));
            } catch (IOException ioe) {
                ioe.printStackTrace(System.err);
                return null;
            }
        }
    }

    protected BasicContext _ctx;
    protected Config _config = new Config("bang");
    protected ResourceManager _rsrcmgr;
    protected BLookAndFeel _lnf;
    protected ModelCache _mcache;
}
