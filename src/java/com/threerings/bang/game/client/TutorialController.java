//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.jme.renderer.Renderer;
import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

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
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Homestead;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Prop;
import com.threerings.bang.game.util.TutorialUtil;

import static com.threerings.bang.Log.log;

/**
 * Works with the {@link BangController} to manage tutorials on the client
 * side.
 */
public class TutorialController
    implements ActionListener
{
    /** Called from {@link BangController#init}. */
    public void init (BangContext ctx, BangConfig config, BangView view)
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
        _view.tutwin = new BDecoratedWindow(_ctx.getStyleSheet(), null) {
            public BComponent getHitComponent (int mx, int my) {
                BComponent comp = super.getHitComponent(mx, my);
                return (comp == _back || comp == _forward ||
                    _pending == null || TutorialCodes.TEXT_CLICKED.equals(
                        _pending.getEvent())) ? comp : null;
            }
            protected void renderBackground (Renderer renderer) {
                getBackground().render(renderer, 0, 0, _width, _height, 0.5f);
            }
        };
        _view.tutwin.setStyleClass("tutorial_window");
        _view.tutwin.setLayer(1);
        _view.tutwin.setLayoutManager(new BorderLayout(5, 15));

        BContainer north = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.STRETCH, GroupLayout.CENTER,
                                  GroupLayout.CONSTRAIN));
        _view.tutwin.add(north, BorderLayout.NORTH);
        _back = new BButton("", this, "back");
        _back.setStyleClass("tutorial_back");
        _back.setEnabled(false);
        north.add(_back, GroupLayout.FIXED);

        north.add(_title = new BLabel("", "tutorial_title"));

        _forward = new BButton("", this, "forward");
        _forward.setStyleClass("tutorial_forward");
        _forward.setEnabled(false);
        north.add(_forward, GroupLayout.FIXED);

        _view.tutwin.add(_info = new BLabel("", "tutorial_text"),
                         BorderLayout.CENTER);

        BContainer south = new BContainer(GroupLayout.makeHStretch());
        _view.tutwin.add(south, BorderLayout.SOUTH);
        south.add(new BLabel("", "tutorial_click"));
        south.add(_click = new BLabel("", "tutorial_click"), GroupLayout.FIXED);
        south.add(_steps = new BLabel("", "tutorial_steps"));

        _view.tutwin.addListener(_clicklist);
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
        // increment this event's counter
        int count = getEventCount(event)+1;
        _events.put(event, count);

        log.info("Received tutorial event: " + event + " (" + count + ").");

        if (_pending != null && event.matches(_pending.getEvent()) &&
            count >= _pending.getCount()) {
            processedAction(((TutorialConfig.Action)_pending).index);
            _pending = null;
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if ("back".equals(action)) {
            _hidx--;
        } else if ("forward".equals(action)) {
            _hidx++;
        } else {
            return;
        }
        TutorialConfig.Text text = _history.get(_hidx);
        displayMessage(text.message, text.step);
        _back.setEnabled(_hidx > 0);
        _forward.setEnabled(_hidx < _history.size() - 1);
        _click.setEnabled(!_forward.isEnabled());
    }

    /** Called from {@link BangController#gameDidEnd}. */
    public void gameDidEnd ()
    {
        if (_view.tutwin.isAdded()) {
            _ctx.getRootNode().removeWindow(_view.tutwin);
        }

        // display the pick tutorial view in "finished tutorial" mode
        _ctx.getBangClient().displayPopup(
            new PickTutorialView(_ctx, PickTutorialView.Mode.COMPLETED), true);
    }

    /** Called from {@link BangController#didLeavePlace}. */
    public void didLeavePlace (BangObject bangobj)
    {
        if (_view.tutwin.isAdded()) {
            _ctx.getRootNode().removeWindow(_view.tutwin);
        }
        if (_bangobj != null) {
            _bangobj.removeListener(_acl);
            _bangobj = null;
        }
    }

    protected boolean processAction (int actionId)
    {
        TutorialConfig.Action action = _config.getAction(actionId);
        if (action instanceof TutorialConfig.Text) {
            TutorialConfig.Text text = (TutorialConfig.Text)action;
            displayMessage(text.message, text.step);
            _hidx = _history.size();
            _history.add(text);
            _back.setEnabled(_hidx > 0);
            _forward.setEnabled(false);
            _click.setEnabled(true);

        } else if (action instanceof TutorialConfig.AddPiece) {
            // nothing to do here

        } else if (action instanceof TutorialConfig.CenterOn) {
            String what = ((TutorialConfig.CenterOn)action).what;
            int id = ((TutorialConfig.CenterOn)action).id;
            Piece p = null;
            if (what.equals("piece")) {
                p = _bangobj.pieces.get(id);

            } else if (what.equals("special")) {
                // locate the specified special piece
                for (Piece cp : _bangobj.pieces) {
                    if (((cp instanceof Counter ||
                          cp instanceof Homestead) && cp.owner == id) ||
                        (cp instanceof Prop && // extends Prop but is not Prop
                         !cp.getClass().equals(Prop.class) &&
                         (cp.x * 100 + cp.y == id))) {
                        p = cp;
                        break;
                    }
                }
            }
            if (p != null) {
                _view.view.centerCameraOnPiece(p);
            } else {
                log.warning("Requested to center camera on unknown entity " +
                            "[what=" + what + ", id=" + id + "].");
            }

        } else if (action instanceof TutorialConfig.MoveUnit) {
            // nothing to do here

        } else if (action instanceof TutorialConfig.ShowView) {
            String name = ((TutorialConfig.ShowView)action).name;
            if (name.equals("player_status")) {
                _view.showPlayerStatus();
            } else if (name.equals("unit_status")) {
                _view.showUnitStatus();
            } else if (name.equals("round_timer")) {
                _view.showRoundTimer();
            }

        } else if (action instanceof TutorialConfig.ScenarioAction) {
            // currently nothing to do here

        } else if (action instanceof TutorialConfig.WaitAction) {
            // we'll handle this later

        } else {
            log.warning("Unknown action " + action);
        }

        if (action instanceof TutorialConfig.WaitAction) {
            // wait for the specified event
            _pending = (TutorialConfig.WaitAction)action;

            log.info("Waiting [event=" + _pending.getEvent() +
                     ", action=" + _pending + "].");

            // if an event's count is already satified, turn it into a "Click
            // to continue..." event
            if (_pending.getCount() > 0 &&
                getEventCount(_pending.getEvent()) >= _pending.getCount()) {
                log.info("Converting to TEXT_CLICKED....");
                TutorialConfig.Wait wait = new TutorialConfig.Wait();
                wait.event = TutorialCodes.TEXT_CLICKED;
                wait.index = action.index;
                _pending = wait;
            }

            // only allow attacking for actions that allow it
            _view.view._attackEnabled = _pending.allowAttack();

            // let them know if we're waiting for them to click
            if (TutorialCodes.TEXT_CLICKED.matches(_pending.getEvent())) {
                _click.setText(_gmsgs.get("m.tutorial_click"));
            }
            return true;
        }

        return false;
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
        if (!_view.tutwin.isAdded()) {
            _ctx.getRootNode().addWindow(_view.tutwin);
        }
        int width = _ctx.getDisplay().getWidth();
        int height = _ctx.getDisplay().getHeight();
        // take up all the space between the two player status views
        _view.tutwin.pack(width - 2*(246+10), -1);
        _view.tutwin.setLocation((width - _view.tutwin.getWidth())/2, 2);
    }

    protected void processedAction (int index)
    {
        // send a message to the server indicating we've processed this action
        _bangobj.manager.invoke(TutorialCodes.ACTION_PROCESSED, index);
    }

    protected int getEventCount (String event)
    {
        Integer count = _events.get(event);
        return (count == null) ? 0 : count;
    }

    protected MouseAdapter _clicklist = new MouseAdapter() {
        public void mousePressed (MouseEvent event) {
            if (_click.isEnabled()) {
                _click.setText("");
                handleEvent(TutorialCodes.TEXT_CLICKED);
            }
        }
    };

    protected AttributeChangeListener _acl = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(BangObject.ACTION_ID)) {
                int actionId = event.getIntValue();
                while (!processAction(actionId)) {
                    actionId++;
                }
            }
        }
    };

    protected BangContext _ctx;
    protected BangView _view;
    protected BangObject _bangobj;
    protected MessageBundle _msgs, _gmsgs;

    protected BLabel _title, _info, _click, _steps;
    protected BButton _back, _forward;

    protected TutorialConfig _config;
    protected TutorialConfig.WaitAction _pending;

    /** Counts up all events received during the tutorial. */
    protected HashMap<String,Integer> _events = new HashMap<String,Integer>();

    protected ArrayList<TutorialConfig.Text> _history =
        new ArrayList<TutorialConfig.Text>();
    protected int _hidx = -1;
}
