//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.BangConfig;

import com.threerings.bang.util.BangContext;

/**
 * Works with the {@link BangController} to manage tutorials on the client
 * side.
 */
public class TutorialController
{
    /** Called from {@link BangController#init}. */
    public void init (BangContext ctx, BangConfig config)
    {
        _ctx = ctx;

        // TODO: load up tutorial configuration
    }

    /** Called from {@link BangController#willEnterPlace}. */
    public void willEnterPlace (BangObject bangobj)
    {
        _bangobj = bangobj;

        // TODO: add listeners, etc.
    }

    /**
     * Called by the controller when some user interface event has taken place
     * (unit selected, unit deselected, etc.).
     */
    public void handleEvent (String event)
    {
        // TODO: process events
    }

    /** Called from {@link BangController#didLeavePlace}. */
    public void didLeavePlace (BangObject bangobj)
    {
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
}
