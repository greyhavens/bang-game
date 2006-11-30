//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.text.LengthLimitedDocument;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangCodes;

/**
 * Displays a dialog allowing a player to confirm their desire to invite a
 * gang member and specify an invitation message.
 */
public class InviteMemberDialog extends RequestDialog
{
    public InviteMemberDialog (
        BangContext ctx, StatusLabel status, Handle handle)
    {
        super(ctx, GangCodes.GANG_MSGS, "m.confirm_invite", "m.invite", "m.cancel",
            MessageBundle.tcompose("m.member_invited", handle), status);
        _handle = handle;

        setRequiresString(300, "");
        // limit the length of the invite message, over 255 will break
        _input.setDocument(new LengthLimitedDocument(200));
    }

    // documentation inherited
    protected void fireRequest (Object result)
    {
        GangService gsvc = (GangService)_ctx.getClient().requireService(GangService.class);
        gsvc.inviteMember(_ctx.getClient(), _handle, (String)result, this);
    }
    
    protected Handle _handle;
}
