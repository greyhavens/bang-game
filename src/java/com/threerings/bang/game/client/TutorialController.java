//
// $Id$

package com.threerings.bang.game.client;

import com.jme.system.DisplaySystem;

import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.client.PickTutorialView;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.util.TutorialUtil;

import static com.threerings.bang.Log.log;

/**
 * Works with the {@link BangController} to manage tutorials on the client
 * side.
 */
public class TutorialController
{
    /** Called from {@link BangController#init}. */
    public void init (BangContext ctx, BangConfig config, BangBoardView view)
    {
        _ctx = ctx;
        _view = view;

        // load up the tutorial configuration
        _config = TutorialUtil.loadTutorial(
            ctx.getResourceManager(), config.scenarios[0]);
        _msgs = _ctx.getMessageManager().getBundle(
            "tutorials." + _config.ident);
        _gmsgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);

        // create and add the window in which we'll display info text
        _tutwin = new BDecoratedWindow(_ctx.getStyleSheet(), null);
        _tutwin.setLayer(1);
        _tutwin.setLayoutManager(new BorderLayout(5, 15));
        _tutwin.add(_title = new BLabel("", "tutorial_title"),
                    BorderLayout.NORTH);
        _tutwin.add(_info = new BLabel("", "tutorial_text"),
                    BorderLayout.CENTER);

        BContainer south = new BContainer(new BorderLayout(15, 5));
        _tutwin.add(south, BorderLayout.SOUTH);
        south.add(_click = new BLabel("", "tutorial_steps"), BorderLayout.WEST);
        south.add(_steps = new BLabel("", "tutorial_steps"), BorderLayout.EAST);

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

        // display the pick tutorial view in "finished tutorial" mode
        PickTutorialView view = new PickTutorialView(_ctx, _config.ident);
        _ctx.getRootNode().addWindow(view);
        view.pack(-1, -1);
        view.center();
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
            TutorialConfig.Text text = (TutorialConfig.Text)action;
            displayMessage(text.message, text.step);

        } else if (action instanceof TutorialConfig.Wait) {
            // wait for the specified event
            _pending = (TutorialConfig.Wait)action;

            // let them know if we're waiting for them to click
            if (_pending.event.equals(TutorialCodes.TEXT_CLICKED)) {
                _click.setText(_gmsgs.get("m.tutorial_click"));
            }

        } else if (action instanceof TutorialConfig.AddUnit) {
            // nothing to do here

        } else if (action instanceof TutorialConfig.CenterOnUnit) {
            int pieceId = ((TutorialConfig.CenterOnUnit)action).id;
            Piece p = (Piece)_bangobj.pieces.get(pieceId);
            if (p != null) {
                _view.centerCameraOnUnit(p);
            }

        } else if (action instanceof TutorialConfig.MoveUnit) {
            // nothing to do here

        } else if (action instanceof TutorialConfig.AddBonus) {
            // nothing to do here
            
        } else {
            log.warning("Unknown action " + action);
        }

        if (_pending == null) {
            processedAction(action);
        }
    }

    protected void displayMessage (String message, int step)
    {
        String titkey = "m." + message + "_title";
        if (_msgs.exists(titkey)) {
            _title.setText(_msgs.get(titkey));
        }
        _info.setText(_msgs.get("m." + message));
        if (step > 0) {
            _steps.setText(_gmsgs.get("m.tutorial_step", String.valueOf(step),
                                      String.valueOf(_config.getSteps())));
        }

        // display our window the first time we need it
        if (!_tutwin.isAdded()) {
            _ctx.getRootNode().addWindow(_tutwin);
        }
        int width = _ctx.getDisplay().getWidth();
        int height = _ctx.getDisplay().getHeight();
        _tutwin.pack(500, -1);
        _tutwin.setLocation((width-_tutwin.getWidth())/2,
                            height-_tutwin.getHeight() - 10);
    }

    protected void processedAction (TutorialConfig.Action action)
    {
        _bangobj.postMessage(TutorialCodes.ACTION_PROCESSED,
                             new Object[] { action.index });
    }

    protected MouseAdapter _clicklist = new MouseAdapter() {
        public void mousePressed (MouseEvent event) {
            _click.setText("");
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
    protected BangBoardView _view;
    protected BangObject _bangobj;
    protected MessageBundle _msgs, _gmsgs;

    protected BDecoratedWindow _tutwin;
    protected BLabel _title, _info, _click, _steps;

    protected TutorialConfig _config;
    protected TutorialConfig.Wait _pending;
}
