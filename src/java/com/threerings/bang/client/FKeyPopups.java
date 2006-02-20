//
// $Id$

package com.threerings.bang.client;

import com.jme.input.KeyInput;
import com.jmex.bui.BButton;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;
import com.threerings.util.MessageBundle;

/**
 * Handles popping up various windows when the user presses a function key.
 */
public class FKeyPopups
    implements GlobalKeyManager.Command
{
    /**
     * Creates the function key popups manager and registers its key bindings.
     */
    public FKeyPopups (BangContext ctx)
    {
        _ctx = ctx;
        _ctx.getKeyManager().registerCommand(KeyInput.KEY_F1, this);
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
    }

    // documentation inherited from interface GlobalKeyManager.Command
    public void invoke (int keyCode)
    {
        BDecoratedWindow popup;
        switch (keyCode) {
        default:
        case KeyInput.KEY_F1: popup = getHelp(); break;
        }

        if (popup.isAdded()) {
            _ctx.getBangClient().clearPopup(popup, true);
        } else {
            _ctx.getBangClient().displayPopup(popup, true);
        }
    }

    protected BDecoratedWindow getHelp ()
    {
        if (_help == null) {
            _help = new BDecoratedWindow(
                _ctx.getStyleSheet(), _msgs.get("m.key_help_title"));
            _help.add(new BLabel(_msgs.get("m.key_help"), "dialog_text_left"));
            _help.add(makeDismiss(_help), GroupLayout.FIXED);
        }
        return _help;
    }

    protected BButton makeDismiss (final BDecoratedWindow popup)
    {
        return new BButton(_msgs.get("m.dismiss"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _ctx.getBangClient().clearPopup(popup, true);
            }
        }, "dismiss");
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected BDecoratedWindow _help, _bug, _console;
    protected BDecoratedWindow _serverStatus, _runtimeConfig;
}
