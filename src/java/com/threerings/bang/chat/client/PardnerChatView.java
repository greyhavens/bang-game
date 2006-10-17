//
// $Id$

package com.threerings.bang.chat.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.chat.client.TabbedChatView.UserTab;

import com.threerings.bang.client.MainView;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * A dialog through which users can exchange tells with one or more
 * of their pardners, with a display that shows the pardners' avatars
 * next to the text.
 */
public class PardnerChatView extends BDecoratedWindow
    implements ActionListener, BangCodes
{
    public PardnerChatView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        _ctx = ctx;
        setStyleClass("pardner_chat_view");
        setModal(true);
        setLayer(1);

        add(_tabView = new PardnerChatTabs(ctx));

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(_mute = new BButton(ctx.xlate(BANG_MSGS, "m.chat_mute"),
            this, "mute"));
        buttons.add(_resume = new BButton(ctx.xlate(BANG_MSGS, "m.dismiss"),
                                          this, "resume"));
        add(buttons, GroupLayout.FIXED);
    }

    /**
     * Displays the chat view, if possible, with a tab for talking to the
     * named pardner.
     *
     * @return true if we managed to display the view, false if we can't
     * at the moment
     */
    public boolean display (Handle pardner, boolean grabFocus)
    {
        PardnerEntry entry = _ctx.getUserObject().pardners.get(pardner);
        if (entry == null) {
            return false;
        }
        return _tabView.openUserTab(pardner, entry.avatar, grabFocus) != null;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        Object src = ae.getSource();
        if (src == _mute) {
            ((UserTab)_tabView._pane.getSelectedTab()).mute();

        } else if (src == _resume) {
            _ctx.getBangClient().clearPopup(this, false);
        }
    }

    /** A subclass that knows how to display and clear the chat popup */
    protected class PardnerChatTabs extends TabbedChatView
    {
        public PardnerChatTabs (BangContext ctx)
        {
            super(ctx, new Dimension(400, 400));
        }

        @Override // from TabbedChatView
        protected boolean displayTabs ()
        {
            if (isAdded()) {
                return true;
            }
            if (!_ctx.getBangClient().canDisplayPopup(MainView.Type.CHAT)) {
                return false;
            }
            _ctx.getBangClient().displayPopup(PardnerChatView.this, false);
            pack(-1, -1);
            center();
            return true;
        }

        @Override // from TabbedChatView
        protected void lastTabClosed ()
        {
            _ctx.getBangClient().clearPopup(PardnerChatView.this, false);
        }
    }

    protected BangContext _ctx;
    protected PardnerChatTabs _tabView;
    protected BButton _mute, _close, _resume;
}
