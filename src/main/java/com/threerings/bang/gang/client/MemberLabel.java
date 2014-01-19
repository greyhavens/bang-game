//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.event.BEvent;

import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangMemberEntry;

/**
 * A label that displays the name of a gang member and allows users to right-click to bring up a
 * {@link MemberPopupMenu}.
 */
public class MemberLabel extends BLabel
{
    public MemberLabel (
        BangContext ctx, GangMemberEntry member, boolean allowMute, StatusLabel status,
        String style)
    {
        super(member.handle.toString(), style);
        _ctx = ctx;
        _member = member;
        _allowMute = allowMute;
        _status = status;
    }
    
    public MemberLabel (BangContext ctx, boolean allowMute, StatusLabel status, String style)
    {
        super("", style);
        _ctx = ctx;
        _allowMute = allowMute;
        _status = status;
    }
    
    @Override // documentation inherited
    public boolean dispatchEvent (BEvent event)
    {
        return super.dispatchEvent(event) || (_member != null &&
            MemberPopupMenu.checkPopup(_ctx, getWindow(), event, _member, _allowMute, _status));
    }
    
    protected void setMember (GangMemberEntry member)
    {
        _member = member;
        setText(member == null ? "" : member.handle.toString());
    }
    
    protected BangContext _ctx;
    protected GangMemberEntry _member;
    protected boolean _allowMute;
    protected StatusLabel _status;
}
