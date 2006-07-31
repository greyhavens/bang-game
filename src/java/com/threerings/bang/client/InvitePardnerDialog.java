//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.text.LengthLimitedDocument;

import com.threerings.presents.client.InvocationService;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

/**
 * Displays a dialog allowing a player to confirm their desire to invite a
 * pardner and specify an invitation message.
 */
public class InvitePardnerDialog extends OptionDialog
    implements OptionDialog.ResponseReceiver
{
    public InvitePardnerDialog (BangContext ctx, Handle handle)
    {
        super(ctx, BangCodes.BANG_MSGS, "m.confirm_invite",
              new String[] { "m.invite", "m.cancel" }, null);
        _handle = handle;
        _receiver = this;
        // limit the length of the invite message, over 255 will break
        _input.setDocument(new LengthLimitedDocument(200));
        setRequiresString(300, "");
    }

    // from interface OptionDialog.ResponseReceiver
    public void resultPosted (int button, Object result)
    {
        if (button != OK_BUTTON) {
            return;
        }

        PlayerService psvc = (PlayerService)
            _ctx.getClient().requireService(PlayerService.class);
        psvc.invitePardner(_ctx.getClient(), _handle, (String)result,
            new InvocationService.ConfirmListener() {
                public void requestProcessed () {
                    displayFeedback(
                        MessageBundle.tcompose("m.pardner_invited", _handle));
                }
                public void requestFailed (String cause) {
                    displayFeedback(cause);
                }
            });
    }

    protected void displayFeedback (String message)
    {
        _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, message);
    }

    protected Handle _handle;
}
