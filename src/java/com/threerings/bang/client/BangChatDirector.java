//
// $Id$

package com.threerings.bang.client;

import java.util.Iterator;
import java.util.StringTokenizer;

import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.bang.client.Config;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangUserObject;

import com.threerings.bang.util.BangContext;

/**
 * Handles custom chat bits for Bang.
 */
public class BangChatDirector extends ChatDirector
{
    public BangChatDirector (BangContext ctx)
    {
        super(ctx, ctx.getMessageManager(), BangCodes.CHAT_MSGS);
        _ctx = ctx;

        // register our user chat command handlers
        MessageBundle msg = _msgmgr.getBundle(_bundle);
        registerCommandHandler(msg, "slowmo", new SlowMoHandler());
        registerCommandHandler(msg, "tell", new TellHandler());
    }

    /** A temporary hack to adjust the global animation speed. */
    protected class SlowMoHandler extends CommandHandler
    {
        public String handleCommand (
            SpeakService speakSvc, String command, String args, String[] history)
        {
            if (Config.display.animationSpeed == 1) {
                Config.display.animationSpeed = 0.25f;
                displayFeedback(_bundle, "m.slowmo_activated");
            } else {
                Config.display.animationSpeed = 1f;
                displayFeedback(_bundle, "m.slowmo_deactivated");
            }
            return ChatCodes.SUCCESS;
        }
    }

    /** Implements <code>/tell</code>. */
    protected class TellHandler extends CommandHandler
    {
        public String handleCommand (
            SpeakService speakSvc, final String command, String args,
            String[] history)
        {
            // there should be at least two arg tokens: '/tell target word'
            StringTokenizer tok = new StringTokenizer(args);
            if (tok.countTokens() < 2) {
                return "m.usage_tell";
            }

            // now strip off everything up to the username for the message
            String username = tok.nextToken();
            int uidx = args.indexOf(username);
            String message = args.substring(uidx + username.length()).trim();
            if (StringUtil.blank(message)) {
                return "m.usage_tell";
            }

            // make sure we're not trying to tell something to ourselves
            Name target = new Name(username);
            BangUserObject self = _ctx.getUserObject();
            if (self.username.equals(target)) {
                return "m.talk_self";
            }

            // clear out from the history any tells that are mistypes
            for (Iterator iter = _history.iterator(); iter.hasNext(); ) {
                String hist = (String) iter.next();
                if (hist.startsWith("/" + command) &&
                    (new StringTokenizer(hist).countTokens() > 2)) {
                    iter.remove();
                }
            }

            // mogrify the chat
            message = mogrifyChat(message);

            // store the full command in the history, even if it was mistyped
            final String histEntry = command + " " + target + " " + message;
            history[0] = histEntry;

            // request to send this text as a tell message
            requestTell(target, message, new ResultListener() {
                public void requestCompleted (Object result) {
                    // replace the full one in the history with just
                    // "/tell <username>"
                    String newEntry = "/" + command + " " + result + " ";
                    _history.remove(newEntry);
                    int dex = _history.lastIndexOf("/" + histEntry);
                    if (dex >= 0) {
                        _history.set(dex, newEntry);
                    } else {
                        _history.add(newEntry);
                    }
                }
                public void requestFailed (Exception cause) {
                    // do nothing
                }
            });

            return ChatCodes.SUCCESS;
        }
    }

    protected BangContext _ctx;
}
