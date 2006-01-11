//
// $Id$

package com.threerings.bang.game.client;

import com.jme.system.DisplaySystem;

import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BTextArea;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.layout.BorderLayout;

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

        // create and add the window in which we'll display info text
        _tutwin = new BDecoratedWindow(_ctx.getStyleSheet(), null);
        _tutwin.add(_info = new BTextArea(), BorderLayout.SOUTH);
        _tutwin.addListener(_clicklist);
        _info.addListener(_clicklist);
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
        if (_pending != null && event.equals(_pending.event)) {
            processedAction(_pending);
            _pending = null;
        }
    }

    /** Called from {@link BangController#gameDidEnd}. */
    public void gameDidEnd ()
    {
        if (_tutwin.isAdded()) {
            _ctx.getRootNode().removeWindow(_tutwin);
        }
    }

    /** Called from {@link BangController#didLeavePlace}. */
    public void didLeavePlace (BangObject bangobj)
    {
        if (_tutwin.isAdded()) {
            _ctx.getRootNode().removeWindow(_tutwin);
        }
        if (_bangobj != null) {
            _bangobj.removeListener(_acl);
            _bangobj = null;
        }
    }

    protected void processAction (int actionId)
    {
        TutorialConfig.Action action = _config.getAction(actionId);
        if (action instanceof TutorialConfig.Text) {
            displayMessage(((TutorialConfig.Text)action).message);

        } else if (action instanceof TutorialConfig.Wait) {
            // wait for the specified event
            _pending = (TutorialConfig.Wait)action;

        } else if (action instanceof TutorialConfig.AddUnit) {
            // nothing to do here

        } else {
            log.warning("Unknown action " + action);
        }

        if (_pending == null) {
            processedAction(action);
        }
    }

    protected void displayMessage (String message)
    {
        message = "m." + message;
        _info.setText(_ctx.xlate("tutorials." + _config.ident, message));

        // display our window the first time we need it
        if (!_tutwin.isAdded()) {
            _ctx.getRootNode().addWindow(_tutwin);
        }
        int width = _ctx.getDisplay().getWidth();
        int height = _ctx.getDisplay().getHeight();
        _tutwin.pack(300, -1);
        _tutwin.setLocation(width-_tutwin.getWidth()-25,
                            (height-_tutwin.getHeight())/2);
    }

    protected void processedAction (TutorialConfig.Action action)
    {
        _bangobj.postMessage(TutorialCodes.ACTION_PROCESSED,
                             new Object[] { action.index });
    }

    protected MouseAdapter _clicklist = new MouseAdapter() {
        public void mousePressed (MouseEvent event) {
            handleEvent(TutorialCodes.TEXT_CLICKED);
        }
    };

    protected AttributeChangeListener _acl = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(BangObject.ACTION_ID)) {
                processAction(event.getIntValue());
            }
        }
    };

    protected BangContext _ctx;
    protected BangObject _bangobj;

    protected BDecoratedWindow _tutwin;
    protected BTextArea _info;

    protected TutorialConfig _config;
    protected TutorialConfig.Wait _pending;
}
