//
// $Id: EditorClient.java 3283 2004-12-22 19:23:00Z ray $

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;

import java.util.logging.Level;

import com.jme.bui.BLookAndFeel;
import com.jme.bui.event.InputDispatcher;
import com.jme.input.InputHandler;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.system.DisplaySystem;

import com.samskivert.util.Config;
import com.samskivert.util.RunQueue;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageManager;
import com.threerings.util.Name;

import com.threerings.jme.JmeApp;
import com.threerings.jme.tile.FringeConfiguration;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.SessionObserver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.presents.net.BootstrapData;
import com.threerings.presents.server.ClientResolutionListener;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.parlor.client.ParlorDirector;

import com.threerings.bang.client.BasicClient;
import com.threerings.bang.client.ModelCache;

import static com.threerings.bang.Log.log;

/**
 * Takes care of instantiating all of the proper managers and loading up
 * all of the necessary configuration and getting the client bootstrapped.
 */
public class EditorClient extends BasicClient
    implements RunQueue, SessionObserver
{
    /** Initializes the editor client and prepares it for operation. */
    public EditorClient (EditorApp app, JFrame frame)
    {
        // create our context
        _ctx = new EditorContextImpl();
        _frame = frame;
        _frame.setJMenuBar(new JMenuBar());
        _frame.getContentPane().add(
            _status = new JLabel(" "), BorderLayout.SOUTH);

        // we can't use lightweight popups because our OpenGL display is a
        // heavyweight component
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        initClient(_ctx, app, this);

        // listen for logon/logoff
        _ctx.getClient().addClientObserver(this);
    }

    /**
     * Returns a reference to the context in effect for this client. This
     * reference is valid for the lifetime of the application.
     */
    public EditorContext getContext ()
    {
        return _ctx;
    }

    public void logon ()
    {
        // create our client object
        ClientResolutionListener clr = new ClientResolutionListener() {
            public void clientResolved (Name username, ClientObject clobj) {
                // fake up a bootstrap...
                BootstrapData data = new BootstrapData();
                data.clientOid = clobj.getOid();
                data.services = EditorServer.invmgr.bootlist;

                // ...and configure the client to operate using the
                // server's distributed object manager
                _ctx.getClient().gotBootstrap(data, EditorServer.omgr);
            }

            public void resolutionFailed (Name username, Exception reason) {
                log.log(Level.WARNING, "Failed to resolve client [who=" +
                        username + "].", reason);
                // TODO: display this error
            }
        };
        EditorServer.clmgr.resolveClientObject(new Name("editor"), clr);
    }

    // documentation inherited from interface SessionObserver
    public void clientDidLogon (Client client)
    {
        EditorConfig config = new EditorConfig();
        _ctx.getParlorDirector().startSolitaire(
            config, new InvocationService.ConfirmListener() {
                public void requestProcessed () {
                    // yay! nothing to do here
                }
                public void requestFailed (String cause) {
                    log.warning("Failed to create editor: " + cause);
                }
            });
    }

    // documentation inherited from interface SessionObserver
    public void clientObjectDidChange (Client client)
    {
        // NADA
    }

    // documentation inherited from interface SessionObserver
    public void clientDidLogoff (Client client)
    {
        System.exit(0);
    }

    // documentation inherited from interface RunQueue
    public void postRunnable (Runnable run)
    {
        // queue it on up on the awt thread
        EventQueue.invokeLater(run);
    }

    // documentation inherited from interface RunQueue
    public boolean isDispatchThread ()
    {
        return EventQueue.isDispatchThread();
    }

    /**
     * The context implementation. This provides access to all of the
     * objects and services that are needed by the operating client.
     */
    protected class EditorContextImpl extends EditorContext
    {
        /**
         * Apparently the default constructor has default access, rather
         * than protected access, even though this class is declared to be
         * protected. Why, I don't know, but we need to be able to extend
         * this class elsewhere, so we need this.
         */
        protected EditorContextImpl () {
        }

        public Client getClient () {
            return _client;
        }

        public DObjectManager getDObjectManager () {
            return _client.getDObjectManager();
        }

        public LocationDirector getLocationDirector () {
            return _locdir;
        }

        public OccupantDirector getOccupantDirector () {
            return _occdir;
        }

        public ChatDirector getChatDirector () {
            return _chatdir;
        }

        public ParlorDirector getParlorDirector () {
            return _pardir;
        }

        public ResourceManager getResourceManager () {
            return _rsrcmgr;
        }

        public MessageManager getMessageManager () {
            return _msgmgr;
        }

        public JmeApp getApp () {
            return _app;
        }

        public ModelCache getModelCache () {
            return _mcache;
        }

        public FringeConfiguration getFringeConfig () {
            return _fringeconf;
        }

        public DisplaySystem getDisplay () {
            return _app.getContext().getDisplay();
        }

        public Renderer getRenderer () {
            return _app.getContext().getRenderer();
        }

        public Camera getCamera () {
            return _app.getContext().getCamera();
        }

        public Node getGeometry () {
            return _app.getContext().getGeometry();
        }

        public Node getInterface () {
            return _app.getContext().getInterface();
        }

        public InputHandler getInputHandler () {
            return _app.getContext().getInputHandler();
        }

        public InputDispatcher getInputDispatcher () {
            return _app.getContext().getInputDispatcher();
        }

        public BLookAndFeel getLookAndFeel () {
            return _lnf;
        }

        public Config getConfig () {
            return _config;
        }

        public void setPlaceView (PlaceView view) {
            Container pane = _frame.getContentPane();
            if (pane.getComponentCount() > 2) {
                pane.remove(2);
            }
            pane.add((Component)view, BorderLayout.EAST);
            _frame.validate();
        }

        public void clearPlaceView (PlaceView view) {
            Container pane = _frame.getContentPane();
            if (pane.getComponentCount() > 1) {
                pane.remove(1);
            }
        }

        public void setWindowTitle (String title) {
            _frame.setTitle(title);
        }

        public void displayStatus (String status) {
            _status.setText(status);
        }

        public JFrame getFrame () {
            return _frame;
        }
    }

    protected EditorContext _ctx;
    protected Config _config = new Config("editor");

    protected JFrame _frame;
    protected JLabel _status;
}
