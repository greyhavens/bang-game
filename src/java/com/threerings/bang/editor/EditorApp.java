//
// $Id$

package com.threerings.bang.editor;

import java.io.IOException;
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

import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Sets up the necessary business for the Bang! editor.
 */
public class EditorApp
{
    public EditorApp ()
    {
//         // create a frame
//         _frame = new ToyBoxFrame("...", gameId, username);

        // create and initialize our client instance
        _client = new EditorClient();
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

        // this is the entry point for all the "client-side" stuff
        final EditorApp app = new EditorApp();

        // post a runnable that will get executed after everything is
        // initialized and happy
        EditorServer.omgr.postRunnable(new Runnable() {
            public void run () {
                app.logon();
            }
        });
    }

    protected EditorClient _client;
}
