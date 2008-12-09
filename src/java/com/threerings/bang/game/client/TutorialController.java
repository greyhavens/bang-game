//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import com.jme.renderer.Renderer;
import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

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
import com.threerings.bang.game.data.piece.Unit;
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
        _config = TutorialUtil.loadTutorial(ctx.getResourceManager(), config.getScenario(0));
        _gconfig = config;
        _msgs = _ctx.getMessageManager().getBundle("tutorials." + _config.ident);
        _gmsgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);

        // create and add the window in which we'll display info text
        _view.tutwin = new BDecoratedWindow(_ctx.getStyleSheet(), null) {
            public BComponent getHitComponent (int mx, int my) {
                BComponent comp = super.getHitComponent(mx, my);
                return (comp == _back || comp == _forward || _pending == null ||
                        TutorialCodes.TEXT_CLICKED.equals(_pending.getEvent())) ? comp : null;
            }
            protected void renderBackground (Renderer renderer) {
                getBackground().render(renderer, 0, 0, _width, _height, 0.5f);
            }
        };
        _view.tutwin.setStyleClass("tutorial_window");
        _view.tutwin.setLayer(1);
        _view.tutwin.setLayoutManager(new BorderLayout(5, 15));
        _default = new ImageBackground(
                ImageBackground.FRAME_XY, _ctx.loadImage("ui/tutorials/bubble.png"));
        _glow = new ImageBackground(
                ImageBackground.FRAME_XY, _ctx.loadImage("ui/tutorials/bubble_glow.png"));
        _talk = new TutorialTalkBackground(_ctx.loadImage("ui/tutorials/bubble_tail.png"));
        _talkglow = new TutorialTalkBackground(_ctx.loadImage("ui/tutorials/bubble_tail_glow.png"));

        BContainer north = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.CONSTRAIN));
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

        _view.tutwin.add(_info = new BLabel("", "tutorial_text"), BorderLayout.CENTER);

        BContainer south = new BContainer(GroupLayout.makeHStretch());
        _view.tutwin.add(south, BorderLayout.SOUTH);
        south.add(new BLabel("", "tutorial_click"));
        south.add(_click = new BLabel("", "tutorial_click"), GroupLayout.FIXED);
        south.add(_steps = new BLabel("", "tutorial_steps"));

        _view.tutwin.addListener(_clicklist);
        _info.addListener(_clicklist);

        _tutavatar = new BWindow(_ctx.getStyleSheet(), GroupLayout.makeHStretch());
        _tutavatar.setLayer(1);
        _tutavatar.add(_avatarLabel = new BLabel(new BlankIcon(135, 160)));
    }

    /** Called from {@link BangController#willEnterPlace}. */
    public void willEnterPlace (BangObject bangobj)
    {
        _bangobj = bangobj;
        _bangobj.addListener(_acl);
    }

    /**
     * Called by the controller when some user interface event has taken place (unit selected, unit
     * deselected, etc.) or from our event handler when an eventful game object event has arrived
     * (unit moved, etc.).
     */
    public void handleEvent (String event, int id)
    {
        // increment this event's counter
        int count = getEventCount(event)+1;
        _events.put(event, count);

        if (_pending == null || !event.matches(_pending.getEvent()) ||
            count < _pending.getCount() || (_pending.getId() != -1 && _pending.getId() != id)) {
            log.info("Ignoring tutorial event: " + event + " (" + count + "), id", id +".");
            return;
        }
        log.info("Matched tutorial event: " + event + " (" + count + ").");

        // process the action
        processedAction(((TutorialConfig.Action)_pending).index);
        _pending = null;

        // clear the pointer in case a previous action had it set
        _view.view.clearPointer();
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
        displayMessage(text.message, text.step, text.avatar);
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
        if (_tutavatar.isAdded()) {
            _ctx.getRootNode().removeWindow(_tutavatar);
        }
    }

    /** Called from {@link BangController#didLeavePlace}. */
    public void didLeavePlace (BangObject bangobj)
    {
        if (_view.tutwin.isAdded()) {
            _ctx.getRootNode().removeWindow(_view.tutwin);
        }
        if (_tutavatar.isAdded()) {
            _ctx.getRootNode().removeWindow(_tutavatar);
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
            if (text.avatar == null) {
                text.avatar = _avatar;
            } else if (StringUtil.isBlank(text.avatar)) {
                text.avatar = null;
                _avatar = null;
            } else {
                _avatar = text.avatar;
            }
            displayMessage(text.message, text.step, text.avatar);
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
                    if (((cp instanceof Counter || cp instanceof Homestead) && cp.owner == id) ||
                        (cp instanceof Prop && // extends Prop but is not Prop
                         !cp.getClass().equals(Prop.class) && (cp.x * 100 + cp.y == id))) {
                        p = cp;
                        break;
                    }
                }
            }
            if (p != null) {
                if (((TutorialConfig.CenterOn)action).arrow) {
                    _view.view.activatePointer(p);
                }
                _view.view.centerCameraOnPiece(p);
            } else {
                log.warning("Requested to center camera on unknown entity", "what", what, "id", id);
            }

        } else if (action instanceof TutorialConfig.MoveUnit) {
            // nothing to do here

        } else if (action instanceof TutorialConfig.ShowView) {
            String name = ((TutorialConfig.ShowView)action).name;
            if (name.equals("player_status")) {
                _showingStatus = true;
                _view.showPlayerStatus();
            } else if (name.equals("unit_status")) {
                _view.showUnitStatus();
            } else if (name.equals("round_timer")) {
                _view.showRoundTimer();
            } else if (name.equals("hero_hud")) {
                _view.showScenarioHUD(new HeroBuildingView(_ctx, _bangobj));
            }

        } else if (action instanceof TutorialConfig.ScenarioAction) {
            // currently nothing to do here

        } else if (action instanceof TutorialConfig.WaitAction) {
            // we'll handle this later

        } else if (action instanceof TutorialConfig.SetCard) {
            // currently nothing to do here

        } else {
            log.warning("Unknown action " + action);
        }

        if (action instanceof TutorialConfig.WaitAction) {
            // wait for the specified event
            _pending = (TutorialConfig.WaitAction)action;

            log.info("Waiting", "event", _pending.getEvent(), "action", _pending);

            boolean completed = false;

            if (action instanceof TutorialConfig.WaitHolding) {
                TutorialConfig.WaitHolding whaction = (TutorialConfig.WaitHolding)action;
                Piece piece = _bangobj.pieces.get(whaction.holderId);
                completed = (piece != null && piece instanceof Unit &&
                        whaction.holding.equals(((Unit)piece).holding));
            }

            // if an event's count is already satified, turn it into a "Click to continue..." event
            if (completed || (_pending.getCount() > 0 &&
                getEventCount(_pending.getEvent()) >= _pending.getCount())) {
                log.info("Converting to TEXT_CLICKED....");
                TutorialConfig.Wait wait = new TutorialConfig.Wait();
                wait.event = TutorialCodes.TEXT_CLICKED;
                wait.index = action.index;
                _pending = wait;
            }

            // only allow attacking for actions that allow it
            int[] allowAttack = _pending.allowAttack();
            Point attackEnabled = null;
            if (allowAttack.length == 2) {
                attackEnabled = new Point(allowAttack[0], allowAttack[1]);
            }
            _view.view._attackEnabled = attackEnabled;
            _view.view.setInteractive(true);

            // let them know if we're waiting for them to click
            if (TutorialCodes.TEXT_CLICKED.matches(_pending.getEvent())) {
                _click.setText(_gmsgs.get("m.tutorial_click"));
                _bubbleGlow = true;
                updateBubbleBackground();
                // disable the ability to move their units at this time (unless we override that
                // with allow attack which is sort of a hack but does the job)
                if (attackEnabled != null) {
                    _view.view.setInteractive(false);
                }
            }
            return true;
        }

        return false;
    }

    protected void displayMessage (String message, int step, String avatar)
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
            _view.tutwin.setBackground(BComponent.HOVER, null);
        }

        // see if our avatar has changed
        if (avatar != _currentAvatar) {
            _currentAvatar = avatar;
            _avatarLabel.setIcon(avatar == null ?
                    new BlankIcon(135, 160) : new ImageIcon(_ctx.loadImage(avatar)));
            if (avatar != null && !_tutavatar.isAdded()) {
                _ctx.getRootNode().addWindow(_tutavatar);
                _tutavatar.pack(135, 160);
                _bubbleTalk = true;

            } else if (avatar == null && _tutavatar.isAdded()) {
                _ctx.getRootNode().removeWindow(_tutavatar);
                _bubbleTalk = false;
            }
            updateBubbleBackground();
        }

        int width = _ctx.getDisplay().getWidth();
        // take up all the space between the two player status views
        _view.tutwin.pack(width - 2*(246+10), -1);
        _view.tutwin.setLocation((width - _view.tutwin.getWidth())/2, 2);

        if (_tutavatar.isAdded()) {
            int y = (_showingStatus) ? 69 : 0;
            _tutavatar.setLocation(100, y);
            y += 80;
            _talkglow.setTailPosition(y);
            _talk.setTailPosition(y);
        }
    }

    /**
     * A helper function that updates the background for our text bubble.
     */
    protected void updateBubbleBackground ()
    {
        _view.tutwin.setBackground(BComponent.DEFAULT,
                (_bubbleTalk ? (_bubbleGlow ? _talkglow : _talk) :
                               (_bubbleGlow ? _glow : _default)));
        _view.tutwin.getInsets().left = (_bubbleTalk ? 50 : 25);
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
                _bubbleGlow = false;
                updateBubbleBackground();
                handleEvent(TutorialCodes.TEXT_CLICKED, -1);
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

            } else if (event.getName().equals(BangObject.AWARDS)) {
                _ctx.getBangClient().displayPopup(new TutorialGameOverView(
                            _ctx, _config.ident, _gconfig, _bangobj, _ctx.getUserObject()), true);
            }
        }
    };

    protected BangContext _ctx;
    protected BangView _view;
    protected BangObject _bangobj;
    protected MessageBundle _msgs, _gmsgs;

    protected BLabel _title, _info, _click, _steps;
    protected BButton _back, _forward;
    protected ImageBackground _default, _glow;
    protected TutorialTalkBackground _talk, _talkglow;
    protected BLabel _avatarLabel;
    protected BWindow _tutavatar;
    protected boolean _showingStatus, _bubbleTalk, _bubbleGlow;

    protected TutorialConfig _config;
    protected TutorialConfig.WaitAction _pending;
    protected BangConfig _gconfig;

    /** Counts up all events received during the tutorial. */
    protected HashMap<String,Integer> _events = new HashMap<String,Integer>();

    protected ArrayList<TutorialConfig.Text> _history = new ArrayList<TutorialConfig.Text>();
    protected String _avatar, _currentAvatar;
    protected int _hidx = -1;
}
