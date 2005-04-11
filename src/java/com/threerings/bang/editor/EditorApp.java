//
// $Id$

package com.threerings.bang.editor;

import java.util.logging.Level;

import com.samskivert.util.LoggingLogProvider;
import com.samskivert.util.OneLineLogFormatter;

import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.SessionObserver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.BootstrapData;
import com.threerings.presents.server.ClientResolutionListener;

import com.threerings.toybox.client.ToyBoxApp;
import com.threerings.toybox.data.GameDefinition;
import com.threerings.toybox.data.Parameter;
import com.threerings.toybox.data.RangeParameter;
import com.threerings.toybox.data.TableMatchConfig;
import com.threerings.toybox.data.ToyBoxBootstrapData;
import com.threerings.toybox.data.ToyBoxGameConfig;
import com.threerings.toybox.util.ToyBoxContext;

import static com.threerings.bang.Log.log;

/**
 * Sets up the necessary business for the Bang! editor.
 */
public class EditorApp extends ToyBoxApp
    implements SessionObserver
{
    public void logon ()
    {
        // keep a handle on this for later
        _ctx = (ToyBoxContext)_client.getContext();
        _ctx.getClient().addClientObserver(this);

        // create our client object
        ClientResolutionListener clr = new ClientResolutionListener() {
            public void clientResolved (Name username, ClientObject clobj) {
                // fake up a bootstrap; I need to expose the mechanisms in
                // Presents that create it in a network environment
                ToyBoxBootstrapData data = new ToyBoxBootstrapData();
                data.clientOid = clobj.getOid();
                data.services = EditorServer.invmgr.bootlist;
                data.resourceURL = "file:///";

                // and configure the client to operate using the server's
                // distributed object manager
                _client.getContext().getClient().gotBootstrap(
                    data, EditorServer.omgr);
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
        ToyBoxGameConfig config = new ToyBoxGameConfig(0, createEditorGameDef());
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
        // NADA
    }

    protected GameDefinition createEditorGameDef ()
    {
        GameDefinition gdef = new GameDefinition();
        gdef.ident = "editor";
        gdef.controller = EditorController.class.getName();
        gdef.manager = EditorManager.class.getName();
        gdef.match = new TableMatchConfig();
        gdef.params = new Parameter[] { createRange("board_size", 8, 16, 48),
                                        createRange("density", 0, 8, 10) };
        return gdef;
    }

    protected RangeParameter createRange (
        String ident, int min, int start, int max)
    {
        RangeParameter param = new RangeParameter();
        param.ident = ident;
        param.minimum = min;
        param.start = start;
        param.maximum = max;
        return param;
    }

    public static void main (String[] args)
    {
        // set up the proper logging services
        com.samskivert.util.Log.setLogProvider(new LoggingLogProvider());
        OneLineLogFormatter.configureDefaultHandler();

        // create our editor server which we're going to run in the same
        // JVM with the client
        EditorServer server = new EditorServer();
        try {
            server.init();
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to initialize server.", e);
        }

        // let the BangClientController know we're in editor mode
        System.setProperty("editor", "true");

        // now we create the client: we aren't actually logging in so we
        // don't need or want a server or username or whatnot
        final EditorApp app = new EditorApp();
        start(app, "localhost", -1, null, null);

        // post a runnable that will get executed after everything is
        // initialized and happy
        EditorServer.omgr.postRunnable(new Runnable() {
            public void run () {
                app.logon();
            }
        });
    }

    protected ToyBoxContext _ctx;
}
