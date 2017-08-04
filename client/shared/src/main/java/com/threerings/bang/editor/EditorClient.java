//
// $Id: EditorClient.java 3283 2004-12-22 19:23:00Z ray $

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.swing.GroupLayout;
import com.samskivert.util.Config;
import com.samskivert.util.RunQueue;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.SessionObserver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.net.BootstrapData;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.ClientResolutionListener;
import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.client.PlaceView;

import com.threerings.bang.client.BasicClient;

import static com.threerings.bang.Log.log;

/**
 * Takes care of instantiating all of the proper managers and loading up
 * all of the necessary configuration and getting the client bootstrapped.
 */
@Singleton
public class EditorClient extends BasicClient
    implements SessionObserver
{
    /**
     * Returns a reference to the context in effect for this client. This
     * reference is valid for the lifetime of the application.
     */
    public EditorContext getContext ()
    {
        return _ctx;
    }

    /**
     * Initializes the editor client and prepares it for operation.
     */
    public void init (EditorApp app, JFrame frame)
    {
        // create our context
        _frame = frame;
        _frame.setJMenuBar(new JMenuBar());
        JPanel statusPanel = GroupLayout.makeHBox(GroupLayout.STRETCH);
        statusPanel.add(_status = new JLabel(" "));
        statusPanel.add(_coords = new JLabel("x:  , y:  "), GroupLayout.FIXED);
        _frame.getContentPane().add(statusPanel, BorderLayout.SOUTH);

        // we can't use lightweight popups because our OpenGL display is a
        // heavyweight component
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        initClient(_ctx, app, RunQueue.AWT);

        // listen for logon/logoff
        _ctx.getClient().addClientObserver(this);
    }

    /**
     * Called by the application when we're ready to go.
     */
    public void start ()
    {
        // TODO: display progress in a UI somewhere
        initResources(new InitObserver() {
            public void progress (int percent) {
                // once we're fully unpacked, continue our initialization
                if (percent == 100) {
                    logon();
                }
            }
        });
    }

    // documentation inherited from interface SessionObserver
    public void clientWillLogon (Client client)
    {
        // NADA
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

    protected void logon ()
    {
        // create our client object
        ClientResolutionListener clr = new ClientResolutionListener() {
            public void clientResolved (Name username, ClientObject clobj) {
                // prepare to do our standalone logon...
                String[] groups = _ctx.getClient().prepareStandaloneLogon();

                // ...fake up a bootstrap...
                BootstrapData data = new BootstrapData();
                data.clientOid = clobj.getOid();
                data.services = _invmgr.getBootstrapServices(groups);

                // ...and configure the client to use the server's distributed object manager
                _ctx.getClient().standaloneLogon(data, _omgr);
            }

            public void resolutionFailed (Name username, Exception reason) {
                log.warning("Failed to resolve client", "who", username, reason);
                // TODO: display this error
            }
        };
        _clmgr.resolveClientObject(new Name("editor"), clr);
    }

    /**
     * The context implementation. This provides access to all of the
     * objects and services that are needed by the operating client.
     */
    protected class EditorContextImpl extends BasicContextImpl
        implements EditorContext
    {
        /**
         * Apparently the default constructor has default access, rather
         * than protected access, even though this class is declared to be
         * protected. Why, I don't know, but we need to be able to extend
         * this class elsewhere, so we need this.
         */
        protected EditorContextImpl () {
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
            displayStatus(status, false);
        }

        public void displayStatus (String status, boolean paint) {
            _status.setText(status);
            if (paint) {
                _status.paintImmediately(0, 0, _status.getWidth(),
                    _status.getHeight());
            }
        }

        public void displayCoords (int x, int y) {
            _coords.setText("x:" + x + ", y:" + y);
        }

        public JFrame getFrame () {
            return _frame;
        }
    }

    protected EditorContextImpl _ctx = new EditorContextImpl();
    protected Config _config = new Config("editor");

    protected JFrame _frame;
    protected JLabel _status;
    protected JLabel _coords;

    @Inject protected ClientManager _clmgr;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected InvocationManager _invmgr;
}
