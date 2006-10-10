//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.text.LengthLimitedDocument;

import com.threerings.presents.client.InvocationService;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.StatusLabel;
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
    public InvitePardnerDialog (
        BangContext ctx, StatusLabel status, Handle handle)
    {
        super(ctx, BangCodes.BANG_MSGS, "m.confirm_invite",
              new String[] { "m.invite", "m.cancel" }, null);

        _status = status;
        _handle = handle;
        _receiver = this;

        setRequiresString(300, "");
        // limit the length of the invite message, over 255 will break
        _input.setDocument(new LengthLimitedDocument(200));
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
                    displayStatus(MessageBundle.tcompose(
                                      "m.pardner_invited", _handle), false);
                }
                public void requestFailed (String cause) {
                    displayStatus(cause, true);
                }
            });
    }

    protected void displayStatus (String message, boolean flash)
    {
        if (_status == null) {
            _ctx.getChatDirector().displayFeedback(
                BangCodes.BANG_MSGS, message);
        } else {
            _status.setStatus(_ctx.xlate(BangCodes.BANG_MSGS, message), flash);
        }
    }

    protected StatusLabel _status;
    protected Handle _handle;
}
