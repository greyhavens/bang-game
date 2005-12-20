//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.util.TutorialUtil;

import static com.threerings.bang.Log.log;

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

        // load up the tutorial configuration
        _config = TutorialUtil.loadTutorial(
            ctx.getResourceManager(), config.scenarios[0]);
    }

    /** Called from {@link BangController#willEnterPlace}. */
    public void willEnterPlace (BangObject bangobj)
    {
        _bangobj = bangobj;

        // TODO: add listeners, etc.

        // TEMP
        processActions();
    }

    /**
     * Called by the controller when some user interface event has taken place
     * (unit selected, unit deselected, etc.) or from our event handler when an
     * eventful game object event has arrived (unit moved, etc.).
     */
    public void handleEvent (String event)
    {
        // if we're waiting for this event, continue our action processing
        log.info("Event: " + event);
        if (event.equals(_pending)) {
            _pending = null;
            processActions();
        }
    }

    /** Called from {@link BangController#didLeavePlace}. */
    public void didLeavePlace (BangObject bangobj)
    {
    }

    /**
     * Processes the next action in the tutorial, and continues to process
     * actions until an action that requires waiting is reached.
     *
     * @return true if the tutorial is finished, false if we stopped due to a
     * pending action.
     */
    protected boolean processActions ()
    {
        TutorialConfig.Action action;
        while ((action = _config.getNextAction()) != null) {
            log.info("Processing: " + action);
            if (action instanceof TutorialConfig.Text) {
                String message = ((TutorialConfig.Text)action).message;
                // TODO: display the specified text
                log.info("Message for you sir: " + message);

            } else if (action instanceof TutorialConfig.Wait) {
                // wait for the specified event
                _pending = ((TutorialConfig.Wait)action).event;
                log.info("Waiting for: " + _pending);
                break;

            } else if (action instanceof TutorialConfig.AddUnit) {
                // wait for the unit to show up in the game
                _pending = TutorialCodes.UNIT_ADDED;
                log.info("Waiting for: " + _pending);
                break;

            } else {
                log.warning("Unknown action " + action);
            }
        }
        return (action == null);
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;

    protected TutorialConfig _config;
    protected String _pending;
}
