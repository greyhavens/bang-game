//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

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
        _bangobj.addListener(_acl);
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
            // TODO: send a processed action event
        }
    }

    /** Called from {@link BangController#didLeavePlace}. */
    public void didLeavePlace (BangObject bangobj)
    {
        if (_bangobj != null) {
            _bangobj.removeListener(_acl);
            _bangobj = null;
        }
    }

    protected void processAction (int actionId)
    {
        TutorialConfig.Action action = _config.getAction(actionId);
        log.info("Processing: " + action);
        if (action instanceof TutorialConfig.Text) {
            String message = ((TutorialConfig.Text)action).message;
            // TODO: display the specified text
            log.info("Message for you sir: " + message);

        } else if (action instanceof TutorialConfig.Wait) {
            // wait for the specified event
            _pending = ((TutorialConfig.Wait)action).event;
            log.info("Waiting for: " + _pending);

        } else if (action instanceof TutorialConfig.AddUnit) {
            // wait for the unit to show up in the game
            _pending = TutorialCodes.UNIT_ADDED;
            log.info("Waiting for: " + _pending);

        } else {
            log.warning("Unknown action " + action);
        }
    }

    protected AttributeChangeListener _acl = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(BangObject.ACTION_ID)) {
                processAction(event.getIntValue());
            }
        }
    };

    protected BangContext _ctx;
    protected BangObject _bangobj;

    protected TutorialConfig _config;
    protected String _pending;
}
