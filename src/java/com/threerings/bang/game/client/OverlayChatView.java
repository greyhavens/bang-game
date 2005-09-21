//
// $Id$

package com.threerings.bang.game.client;

import java.util.StringTokenizer;

import com.jmex.bui.BComponent;
import com.jmex.bui.BLabel;
import com.jmex.bui.BLookAndFeel;
import com.jmex.bui.BTextField;
import com.jmex.bui.BWindow;
import com.jmex.bui.background.BBackground;
import com.jmex.bui.background.BlankBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jme.renderer.ColorRGBA;

import com.samskivert.util.Interval;
import com.threerings.util.Name;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays chat within a game.
 */
public class OverlayChatView extends BWindow
    implements ChatDisplay
{
    public OverlayChatView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), GroupLayout.makeVert(
                  GroupLayout.NONE, GroupLayout.BOTTOM, GroupLayout.STRETCH));

        _ctx = ctx;
        _chatdtr = _ctx.getChatDirector();

        _stamps = new long[CHAT_LINES];
        _lnfs = new BLookAndFeel[CHAT_LINES];
        _history = new BLabel[CHAT_LINES];
        for (int ii = 0; ii < _history.length; ii++) {
            _lnfs[ii] = getLookAndFeel().deriveLookAndFeel();
            add(_history[ii] = new BLabel(""));
            _history[ii].setLookAndFeel(_lnfs[ii]);
        }
        add(_input = new BTextField());

        _input.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                if (handleInput(_input.getText())) {
                    _input.setText("");
                }
            }
        });
    }

    /**
     * Called by the main game view when we enter the game room.
     */
    public void willEnterPlace (PlaceObject plobj)
    {
        _chatdtr.addChatDisplay(this);
        _bangobj = (BangObject)plobj;

        // start our chat expiration timer
        _timer = new Interval(_ctx.getClient().getRunQueue()) {
            public void expired () {
                expireChat();
            }
        };
        _timer.schedule(1000L, true);
    }

    /**
     * Called by the main game view when we leave the game room.
     */
    public void didLeavePlace (PlaceObject plobj)
    {
        _chatdtr.removeChatDisplay(this);
        _timer.cancel();
    }

    /**
     * Instructs our chat input field to request focus.
     */
    public void requestFocus ()
    {
        _input.requestFocus();
        _input.setBackground(_inputbg);
    }

    // documentation inherited from interface ChatDisplay
    public void clear ()
    {
        for (int ii = 0; ii < _history.length; ii++) {
            _history[ii].setText("");
            _stamps[ii] = 0L;
        }
    }

    // documentation inherited from interface ChatDisplay
    public void displayMessage (ChatMessage msg)
    {
        if (msg instanceof UserMessage) {
            UserMessage umsg = (UserMessage) msg;
            if (umsg.localtype == ChatCodes.USER_CHAT_TYPE) {
                // TODO: note whisper
                appendMessage(umsg.speaker, umsg.message);
            } else {
                appendMessage(umsg.speaker, umsg.message);
            }

        } else if (msg instanceof SystemMessage) {
            appendMessage(msg.message, ColorRGBA.white);

        } else {
            log.warning("Received unknown message type: " + msg + ".");
        }
    }

    @Override // we never want the chat window to accept clicks
    public BComponent getHitComponent (int mx, int my) {
        return null;
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        _inputbg = _input.getBackground();
        // we need to set a blank background with the same insets to
        // ensure that the text field is laid out properly
        _blankbg = new BlankBackground(
            _inputbg.getLeftInset(), _inputbg.getTopInset(),
            _inputbg.getRightInset(), _inputbg.getBottomInset());
        _input.setBackground(_blankbg);
    }

    protected void displayError (String message)
    {
        appendMessage(message, ColorRGBA.red);
    }

    protected void appendMessage (Name speaker, String message)
    {
        ColorRGBA color = ColorRGBA.white;
        int pidx;
        if ((pidx = _bangobj.getPlayerIndex(speaker)) != -1) {
            color = JPIECE_COLORS[pidx];
        }
        appendMessage(speaker + ": " + message, color);
    }

    protected void appendMessage (String text, ColorRGBA color)
    {
        // first scroll any previous messages up
        int lidx = _history.length-1;
        for (int ii = 0; ii < lidx; ii++) {
            _stamps[ii] = _stamps[ii+1];
            _lnfs[ii].setForeground(true, _lnfs[ii+1].getForeground(true));
            _history[ii].setText(_history[ii+1].getText());
        }

        // now stuff this message at the bottom
        _lnfs[lidx].setForeground(true, color);
        _history[lidx].setText(text);
        _stamps[lidx] = System.currentTimeMillis();
    }

    protected boolean handleInput (String text)
    {
        String errmsg = _chatdtr.requestChat(null, text, true);
        if (errmsg.equals(ChatCodes.SUCCESS)) {
            _ctx.getRootNode().requestFocus(null);
            _input.setBackground(_blankbg);
            return true;

        } else {
            displayError(_ctx.xlate(BangCodes.CHAT_MSGS, errmsg));
            return false;
        }
    }

    protected void expireChat ()
    {
        long now = System.currentTimeMillis();
        for (int ii = 0; ii < _history.length; ii++) {
            if (_stamps[ii] != 0L && (now - _stamps[ii]) > CHAT_EXPIRATION) {
                _stamps[ii] = 0L;
                _history[ii].setText("");
            }
        }
    }

    protected BangContext _ctx;
    protected ChatDirector _chatdtr;
    protected BangObject _bangobj;

    protected BTextField _input;
    protected BBackground _inputbg, _blankbg;

    protected Interval _timer;
    protected BLabel[] _history;
    protected long[] _stamps;
    protected BLookAndFeel[] _lnfs;

    protected static final int CHAT_LINES = 5;
    protected static final long CHAT_EXPIRATION = 20 * 1000L;
}
