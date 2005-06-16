//
// $Id$

package com.threerings.bang.editor;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
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
    /** The parlor manager in operation on this server. */
    public static ParlorManager parmgr = new ParlorManager();

    /**
     * Initializes all of the server services and prepares for operation.
     */
    public void init ()
        throws Exception
    {
        // do the base server initialization
        super.init();

        // initialize our managers
        parmgr.init(invmgr, plreg);

        log.info("Bang! Editor server initialized.");
    }

    // documentation inherited
    protected PresentsDObjectMgr createDObjectManager ()
    {
        return new LocalDObjectMgr();
    }
}
