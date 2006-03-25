//
// $Id$

package com.threerings.bang.client;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.jme.input.KeyInput;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.InputEvent;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.admin.client.RuntimeConfigView;
import com.threerings.bang.client.PickTutorialView;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

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
        _ctx.getKeyManager().registerCommand(KeyInput.KEY_F2, this);
        _ctx.getKeyManager().registerCommand(KeyInput.KEY_F3, this);
        _ctx.getKeyManager().registerCommand(KeyInput.KEY_F4, this);
        _ctx.getKeyManager().registerCommand(KeyInput.KEY_F12, this);
        _ctx.getKeyManager().registerCommand(KeyInput.KEY_T, this);
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
    }

    // documentation inherited from interface GlobalKeyManager.Command
    public void invoke (int keyCode, int modifiers)
    {
        // special hackery to handle Ctrl-Shift-F2 which submits an
        // auto-bug-report and exits the client
        if (keyCode == KeyInput.KEY_F2 && modifiers ==
            (InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK)) {
            if (!_autoBugged) { // avoid repeat pressage
                BangClient.submitBugReport(_ctx, "Autobug!", true);
            }
            return;
        }

        // other hackery to handle taking screen shots
        if (keyCode == KeyInput.KEY_F12) {
            String fname = "bang_screen_" + _sfmt.format(new Date());
            _ctx.getRenderer().takeScreenShot(fname);
            String msg = MessageBundle.tcompose(
                "m.screenshot_taken", fname + ".png");
            _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, msg);
            return;
        }

        // if they pressed the same key as the current popup window, just
        // dismiss it
        if (keyCode == _poppedKey && _popped.isAdded()) {
            clearPopup();
            return;
        }

        // only some of our popups should check before they popup, others (like
        // the admin interfaces, debug log and bug report) should work
        // regardless of what's going on in the game
        boolean requiresCheck = true;
        switch (keyCode) {
        case KeyInput.KEY_F2:
        case KeyInput.KEY_F3:
        case KeyInput.KEY_F4:
            requiresCheck = false;
            break;
        }

        // make sure we can display an FKEY popup right now (but only if we
        // don't already have one popped up, in which case we'll replace it)
        if ((_popped == null || !_popped.isAdded()) && requiresCheck &&
            !_ctx.getBangClient().canDisplayPopup(MainView.Type.FKEY)) {
            return;
        }

        // otherwise pop up the dialog associated with they key they pressed
        // (clearing any other dialog before doing so)
        BDecoratedWindow popup = null;
        switch (keyCode) {
        default:
        case KeyInput.KEY_F1:
            popup = createHelp();
            break;
        case KeyInput.KEY_F2:
            popup = createReportBug();
            break;
        case KeyInput.KEY_F3:
            if (modifiers == InputEvent.SHIFT_DOWN_MASK) {
                popup = createRecentLog();
            }
            break;
        case KeyInput.KEY_F4:
            if (_ctx.getUserObject().tokens.isAdmin()) {
                popup = new RuntimeConfigView(_ctx);
            }
            break;
        case KeyInput.KEY_T:
            popup = new PickTutorialView(_ctx, PickTutorialView.Mode.FKEY);
            break;
        }

        if (popup != null) {
            clearPopup();
            _poppedKey = keyCode;
            _ctx.getBangClient().displayPopup(_popped = popup, true, 500);
        }
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
        BDecoratedWindow help = createDialogWindow("m.key_help_title");
        help.add(new BLabel(_msgs.get("m.key_help"), "dialog_text_left"));
        help.add(makeDismiss(help), GroupLayout.FIXED);
        return help;
    }

    protected BDecoratedWindow createReportBug ()
    {
        final BDecoratedWindow bug = createDialogWindow("m.bug_title");
        ((GroupLayout)bug.getLayoutManager()).setOffAxisPolicy(
            GroupLayout.STRETCH);
        bug.add(new BLabel(_msgs.get("m.bug_intro"), "dialog_text_left"));
        final BTextField descrip = new BTextField("");
        bug.add(descrip, GroupLayout.FIXED);
        descrip.requestFocus();
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        bug.add(buttons, GroupLayout.FIXED);

        ActionListener buglist = new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                if (event.getAction().equals("submit")) {
                    BangClient.submitBugReport(_ctx, descrip.getText(), false);
                }
                _ctx.getBangClient().clearPopup(bug, true);
            }
        };
        BButton submit =
            new BButton(_msgs.get("m.bug_submit"), buglist, "submit");
        buttons.add(submit);
        buttons.add(new BButton(_msgs.get("m.cancel"), buglist, "cancel"));
        // disable the submit button until a description is entered
        new EnablingValidator(descrip, submit);
        return bug;
    }

    protected BDecoratedWindow createRecentLog ()
    {
        BDecoratedWindow window = new BDecoratedWindow(
            _ctx.getStyleSheet(), _msgs.get("m.log_title"));
        ((GroupLayout)window.getLayoutManager()).setGap(15);
        StringBuffer buf = new StringBuffer();
        for (int ii = BangApp.recentLog.size()-1; ii >= 0; ii--) {
            String line = (String)BangApp.recentLog.get(ii);
            buf.append(line.replace("@", "@@"));
        }
        window.add(new BScrollPane(new BLabel(buf.toString(), "debug_log")));
        window.add(makeDismiss(window), GroupLayout.FIXED);
        window.setPreferredSize(new Dimension(1000, 700));
        return window;
    }

    protected BDecoratedWindow createDialogWindow (String title)
    {
        BDecoratedWindow window =
            new BDecoratedWindow(_ctx.getStyleSheet(), _msgs.get(title));
        ((GroupLayout)window.getLayoutManager()).setGap(15);
        window.setStyleClass("dialog_window");
        return window;
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
    protected boolean _autoBugged;

    protected static SimpleDateFormat _sfmt =
        new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
}
