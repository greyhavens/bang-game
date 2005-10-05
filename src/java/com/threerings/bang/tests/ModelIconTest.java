//
// $Id$

package com.threerings.bang.tests;

import java.util.logging.Level;

import com.samskivert.util.Config;

import com.jme.input.InputHandler;
import com.jme.util.LoggingSystem;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;

import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BLookAndFeel;
import com.jmex.bui.BRootNode;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.cast.CharacterManager;
import com.threerings.jme.JmeApp;
import com.threerings.jme.tile.FringeConfiguration;
import com.threerings.openal.SoundManager;
import com.threerings.resource.ResourceManager;
import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.parlor.client.ParlorDirector;

import com.threerings.bang.client.BangApp;
import com.threerings.bang.client.Model;
import com.threerings.bang.client.ModelCache;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.ranch.client.UnitIcon;
import com.threerings.bang.util.BangContext;

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
        _ctx = new BangContextImpl();
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
    protected class BangContextImpl extends BangContext
    {
        /**
         * Apparently the default constructor has default access, rather
         * than protected access, even though this class is declared to be
         * protected. Why, I don't know, but we need to be able to extend
         * this class elsewhere, so we need this.
         */
        protected BangContextImpl () {
        }

        public Client getClient () {
            return null;
        }

        public DObjectManager getDObjectManager () {
            return null;
        }

        public Config getConfig () {
            return _config;
        }

        public LocationDirector getLocationDirector () {
            return null;
        }

        public OccupantDirector getOccupantDirector () {
            return null;
        }

        public ChatDirector getChatDirector () {
            return null;
        }

        public ParlorDirector getParlorDirector () {
            return null;
        }

        public FringeConfiguration getFringeConfig () {
            return null;
        }

        public void setPlaceView (PlaceView view) {
            // TBD
        }

        public void clearPlaceView (PlaceView view) {
            // we'll just let the next place view replace our old one
        }

        public ResourceManager getResourceManager () {
            return _rsrcmgr;
        }

        public MessageManager getMessageManager () {
            return null;
        }

        public BangApp getApp () {
            return null;
        }

        public SoundManager getSoundManager () {
            return null;
        }

        public CharacterManager getCharacterManager () {
            return null;
        }

        public ModelCache getModelCache () {
            return _mcache;
        }

        public DisplaySystem getDisplay () {
            return getContext().getDisplay();
        }

        public Renderer getRenderer () {
            return getContext().getRenderer();
        }

        public Camera getCamera () {
            return getContext().getCamera();
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

        public BLookAndFeel getLookAndFeel () {
            return _lnf;
        }
    }

    protected BangContext _ctx;
    protected Config _config = new Config("bang");
    protected ResourceManager _rsrcmgr;
    protected BLookAndFeel _lnf;
    protected ModelCache _mcache;
}
