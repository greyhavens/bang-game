//
// $Id$

package com.threerings.bang.editor;

import com.google.inject.Injector;

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
    public static class Module extends CrowdServer.Module
    {
        @Override protected void configure () {
            bind(PresentsDObjectMgr.class).to(LocalDObjectMgr.class);
        }
    }

    /** The parlor manager in operation on this server. */
    public static ParlorManager parmgr = new ParlorManager();

    @Override // from CrowdServer
    public void init (Injector injector)
        throws Exception
    {
        super.init(injector);

        // initialize our managers
        parmgr.init(invmgr, plreg);

        log.info("Bang! Editor server initialized.");
    }
}
