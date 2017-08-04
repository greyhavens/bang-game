//
// $Id$

package com.threerings.bang.editor;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import com.threerings.presents.client.Client;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.LocalDObjectMgr;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.server.CrowdServer;
import com.threerings.parlor.server.ParlorManager;

import static com.threerings.bang.Log.log;

/**
 * Handles the server-side of the Bang! editor.
 */
public class EditorServer extends CrowdServer
{
    /** Configures dependencies needed by the Editor server. */
    public static class Module extends CrowdServer.CrowdModule
    {
        @Override protected void configure () {
            super.configure();
            bind(PresentsDObjectMgr.class).to(LocalDObjectMgr.class);
            bind(Client.class).toProvider(EditorClientProvider.class);
        }
    }

    private static class EditorClientProvider implements Provider<Client> {
        @Inject public EditorClientProvider (EditorClient client) {
            _client = client;
        }
        public Client get () {
            return _client.getContext().getClient();
        }
        private final EditorClient _client;
    }

    @Override // from CrowdServer
    public void init (Injector injector)
        throws Exception
    {
        super.init(injector);

        log.info("Bang! Editor server initialized.");
    }

    @Inject protected ParlorManager _parmgr;
}
