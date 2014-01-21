//
// $Id$

package com.threerings.bang.bounty.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.layout.TableLayout;

import com.threerings.bang.client.PlayerPopupMenu;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bounty.data.OfficeCodes;
import com.threerings.bang.bounty.data.RecentCompleters;

/**
 * Displays recent completers of this bounty.
 */
public class RecentCompletersView extends BContainer
{
    public RecentCompletersView (BangContext ctx)
    {
        _ctx = ctx;
        setLayoutManager(new TableLayout(3, 5, 25));
        for (int ii = 0; ii < _completers.length; ii++) {
            add(_completers[ii] = new BLabel("", "bounty_recent") {
                public boolean dispatchEvent (BEvent event) {
                    boolean popped = false;
                    Handle handle = (Handle)getProperty("handle");
                    if (handle != null) {
                        // pop up a player menu if they click the mouse
                        popped = PlayerPopupMenu.checkPopup(
                            _ctx, getWindow(), event, handle, false);
                    }
                    return popped || super.dispatchEvent(event);
                }
            });
        }
    }

    public void setCompleters (RecentCompleters recent)
    {
        String[] handles = (recent == null) ? new String[0] : recent.handles;
        for (int ii = 0; ii < _completers.length; ii++) {
            String defval = (ii == 0) ? _ctx.xlate(OfficeCodes.OFFICE_MSGS, "m.no_completers") : "";
            _completers[ii].setText(handles.length > ii ? handles[ii] : defval);
            _completers[ii].setProperty(
                "handle", handles.length > ii ? new Handle(handles[ii]) : null);
        }
    }

    protected BangContext _ctx;
    protected BLabel[] _completers = new BLabel[RecentCompleters.MAX_COMPLETERS];
}
