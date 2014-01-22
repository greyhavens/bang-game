//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.text.LengthLimitedDocument;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

/**
 * Displays a dialog allowing a player to confirm their desire to invite a
 * pardner and specify an invitation message.
 */
public class InvitePardnerDialog extends RequestDialog
{
    public InvitePardnerDialog (
        BangContext ctx, StatusLabel status, Handle handle)
    {
        super(ctx, BangCodes.BANG_MSGS, "m.confirm_invite", "m.invite", "m.cancel",
            MessageBundle.tcompose("m.pardner_invited", handle), status);
        _handle = handle;

        setRequiresString(300, "");
        // limit the length of the invite message, over 255 will break
        _input.setDocument(new LengthLimitedDocument(200));
    }

    // documentation inherited
    protected void fireRequest (Object result)
    {
        _ctx.getClient().requireService(PlayerService.class).invitePardner(
            _handle, (String)result, this);
    }

    protected Handle _handle;
}
