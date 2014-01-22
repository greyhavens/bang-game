//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.LengthLimitedDocument;

import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.gang.data.GangCodes;

/**
 * Displays a dialog allowing a player to confirm their desire to invite a
 * gang member and specify an invitation message.
 */
public class InviteMemberDialog extends RequestDialog
    implements GangCodes
{
    public InviteMemberDialog (BangContext ctx, StatusLabel status)
    {
        this(ctx, status, null);
    }

    public InviteMemberDialog (
        BangContext ctx, StatusLabel status, Handle handle)
    {
        super(ctx, GangCodes.GANG_MSGS, "m.confirm_invite", "m.invite", "m.cancel",
            "m.member_invited", status);
        _handle = handle;

        setRequiresString(300, "");

        // if the handle is not specified already, add the fields to enter it
        if (_handle == null) {
            add(0, new BLabel(ctx.xlate(GANG_MSGS, "m.invite_name")), GroupLayout.FIXED);
            add(1, _hfield = new BTextField(NameFactory.getValidator().getMaxHandleLength()),
                GroupLayout.FIXED);
            _hfield.setPreferredWidth(200);
            new EnablingValidator(_hfield, _buttons[0]);
            add(2, new Spacer(1, 15), GroupLayout.FIXED);
        }

        // limit the length of the invite message, over 255 will break
        _input.setDocument(new LengthLimitedDocument(200));

        // shove the title up on top
        add(0, new BLabel(ctx.xlate(GANG_MSGS, "t.invite_dialog"), "window_title"));
    }

    // documentation inherited
    protected void fireRequest (Object result)
    {
        GangService gsvc = _ctx.getClient().requireService(GangService.class);
        if (_hfield != null) {
            _handle = new Handle(_hfield.getText());
        }
        gsvc.inviteMember(_handle, (String)result, this);
    }

    protected Handle _handle;
    protected BTextField _hfield;
}
