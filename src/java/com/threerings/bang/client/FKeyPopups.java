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

import com.threerings.bang.client.PickTutorialView;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;
import com.threerings.util.MessageBundle;

/**
 * Handles popping up various windows when the user presses a function key or
 * some other globally mapped keys.
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
        _ctx.getKeyManager().registerCommand(KeyInput.KEY_T, this);
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
    }

    // documentation inherited from interface GlobalKeyManager.Command
    public void invoke (int keyCode)
    {
        // if they pressed the same key as the current popup window, just
        // dismiss it
        if (keyCode == _poppedKey) {
            clearPopup();
            return;
        }

        // make sure we can display an FKEY popup right now
        if (!_ctx.getBangClient().canDisplayPopup(MainView.Type.FKEY)) {
            return;
        }

        // otherwise pop up the dialog associated with they key they pressed
        // (clearing any other dialog before doing so)
        BDecoratedWindow popup;
        switch (keyCode) {
        default:
        case KeyInput.KEY_F1: popup = createHelp(); break;
        case KeyInput.KEY_T: popup = new PickTutorialView(_ctx, null); break;
        }

        clearPopup();
        _poppedKey = keyCode;
        _ctx.getBangClient().displayPopup(_popped = popup, true);
    }

    protected void clearPopup ()
    {
        _poppedKey = -1;
        if (_popped != null) {
            _ctx.getBangClient().clearPopup(_popped, true);
            _popped = null;
        }
    }

    protected BDecoratedWindow createHelp ()
    {
        BDecoratedWindow help = new BDecoratedWindow(
            _ctx.getStyleSheet(), _msgs.get("m.key_help_title"));
        help.add(new BLabel(_msgs.get("m.key_help"), "dialog_text_left"));
        help.add(makeDismiss(help), GroupLayout.FIXED);
        return help;
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

    protected int _poppedKey = -1;
    protected BDecoratedWindow _popped;
}
