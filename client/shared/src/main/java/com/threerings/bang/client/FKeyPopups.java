//
// $Id$

package com.threerings.bang.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;

import com.badlogic.gdx.Input.Keys;

import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.InputEvent;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ResultListener;
import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.data.Look;

import com.threerings.bang.admin.client.RuntimeConfigView;
import com.threerings.bang.admin.client.ServerStatusView;
import com.threerings.bang.chat.client.BangChatDirector;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangTokenRing;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import static com.threerings.bang.Log.log;

/**
 * Handles popping up various windows when the user presses a function key or
 * some other globally mapped keys.
 */
public class FKeyPopups
    implements GlobalKeyManager.Command
{
    /** Enumerates the various types of popups we know about. */
    public static enum Type {
        HELP(Keys.F1, 0, 0, true),
        TUTORIALS(Keys.T, 0, 0, true),
        WHERETO(Keys.W, 0, 0, true),
        // REPORT_BUG(Keys.F2, 0, 0, false),
        CLIENT_LOG(Keys.F3, InputEvent.SHIFT_DOWN_MASK, 0, false),
        CHAT_HISTORY(Keys.F3, 0, 0, false),
        SERVER_STATUS(Keys.F4, 0, BangTokenRing.SUPPORT, false),
        SERVER_CONFIG(Keys.F5, 0, BangTokenRing.ADMIN, false),
        CLIENT_CONFIG(Keys.F6, CTRL_SHIFT, 0, false),
        AVATAR_SHOT(Keys.F11, CTRL_SHIFT, BangTokenRing.ADMIN, false),
        SCREEN_SHOT(Keys.F12, 0, 0, false);

        public int keyCode () {
            return _keyCode;
        }

        public int modifiers () {
            return _modifiers;
        }

        public int requiredToken () {
            return _requiredToken;
        }

        public boolean checkCanDisplay () {
            return _checkCanDisplay;
        }

        Type (int keyCode, int modifiers, int requiredToken,
              boolean checkCanDisplay) {
            _keyCode = keyCode;
            _modifiers = modifiers;
            _requiredToken = requiredToken;
            _checkCanDisplay = checkCanDisplay;
        }

        protected int _keyCode, _modifiers, _requiredToken;
        protected boolean _checkCanDisplay;
    };

    /**
     * Creates the function key popups manager and registers its key bindings.
     */
    public FKeyPopups (BangContext ctx)
    {
        _ctx = ctx;
        for (Type type : Type.values()) {
            _ctx.getKeyManager().registerCommand(type.keyCode(), this);
        }
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        _cmsgs = _ctx.getMessageManager().getBundle(BangCodes.CHAT_MSGS);
    }

    /**
     * Shows the popup of the specifide type, clearing any existing popup
     * prior to doing so.
     */
    public void showPopup (Type type)
    {
        // don't auto-replace a never clear popup
        if (_popped != null && _popped.isAdded() &&
            _popped.getLayer() == BangCodes.NEVER_CLEAR_LAYER) {
            return;
        }

        // if this is the same as the current popup window, just dismiss it
        if (type == _poppedType && _popped != null && _popped.isAdded()) {
            clearPopup();
            return;
        }

        // make sure we can display an FKEY popup right now (but only if we
        // don't already have one popped up, in which case we'll replace it)
        if (type.checkCanDisplay() && (_popped == null || !_popped.isAdded()) &&
            !_ctx.getBangClient().canDisplayPopup(MainView.Type.FKEY)) {
            return;
        }

        // if this popup requires admin privileges, make sure we've got 'em
        if (type.requiredToken() != 0 &&
            (_ctx.getUserObject() == null ||
             !_ctx.getUserObject().tokens.holdsToken(type.requiredToken()))) {
            return;
        }

        BDecoratedWindow popup = null;
        int whint = 500;
        switch (type) {
        default:
        case HELP:
            popup = createHelp();
            break;
        // case REPORT_BUG:
        //     popup = createReportBug();
        //     break;
        case TUTORIALS:
            popup = new TutorialView(_ctx);
            whint = TutorialView.WIDTH_HINT;
            break;
        case WHERETO:
            popup = new WhereToView(_ctx, false);
            whint = WhereToView.WIDTH_HINT;
            break;
        case CLIENT_LOG:
            popup = createRecentLog();
            break;
        case CHAT_HISTORY:
            popup = createChatHistory();
            break;
        case SERVER_STATUS:
            popup = new ServerStatusView(_ctx);
            break;
        case SERVER_CONFIG:
            popup = new RuntimeConfigView(_ctx);
            break;
        case CLIENT_CONFIG:
            popup = new ConfigEditorView(_ctx);
            break;
        }

        if (popup != null) {
            clearPopup();
            _poppedType = type;
            _ctx.getBangClient().displayPopup(_popped = popup, true, whint);
        }
    }

    // documentation inherited from interface GlobalKeyManager.Command
    public void invoke (int keyCode, int modifiers)
    {
        // // special hackery to handle Ctrl-Shift-F2 which submits an
        // // auto-bug-report and exits the client
        // if (keyCode == Keys.F2 && modifiers == CTRL_SHIFT) {
        //     if (!_autoBugged) { // avoid repeat pressage
        //         BangClient.submitBugReport(_ctx, "Autobug!", true);
        //     }
        //     return;
        // }

        // other hackery to handle taking screen shots
        if (keyCode == Keys.F12) {
            String fname = "bang_screen_" + _sfmt.format(new Date());
            _ctx.getRenderer().takeScreenShot(fname);
            String msg = MessageBundle.tcompose(
                "m.screenshot_taken", fname + ".png");
            _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, msg);
            return;
        }

        boolean isAdmin = (_ctx.getUserObject() != null) &&
            _ctx.getUserObject().tokens.isAdmin();

        // yet more hackery to handle dumping a copy of your current avatar
        // look to a file (only available to admins currently)
        if (keyCode == Keys.F11) {
            if (modifiers == CTRL_SHIFT && isAdmin) {
                createCurrentLookSnapshot();
            }
            return;
        }

        // otherwise pop up the dialog associated with they key they pressed
        for (Type type : Type.values()) {
            if (type.keyCode() == keyCode && type.modifiers() == modifiers) {
                showPopup(type);
                return;
            }
        }
    }

    protected void clearPopup ()
    {
        _poppedType = null;
        if (_popped != null) {
            _ctx.getBangClient().clearPopup(_popped, true);
            _popped = null;
        }
    }

    protected BDecoratedWindow createHelp ()
    {
        BDecoratedWindow help = BangUI.createDialog(_msgs.get("m.key_help_title"));
        String text = _msgs.get("m.key_help_key");
        if (!_ctx.getUserObject().tokens.isAnonymous()) {
            text += _msgs.get("m.key_help_bug");
        }
        text += _msgs.get("m.key_help");
        if (_ctx.getUserObject().tokens.isAdmin()) {
            text += _msgs.get("m.key_help_admin");
        }
        help.add(new BLabel(text, "dialog_text_left"));
        help.add(makeDismiss(help), GroupLayout.FIXED);
        return help;
    }

    // protected BDecoratedWindow createReportBug ()
    // {
    //     if (_ctx.getUserObject() != null && _ctx.getUserObject().tokens.isAnonymous()) {
    //         return null;
    //     }
    //     final BDecoratedWindow bug = BangUI.createDialog(_msgs.get("m.bug_title"));
    //     ((GroupLayout)bug.getLayoutManager()).setOffAxisPolicy(
    //         GroupLayout.STRETCH);
    //     bug.setLayer(BangCodes.NEVER_CLEAR_LAYER);
    //     bug.add(new BLabel(_msgs.get("m.bug_intro"), "dialog_text_left"));
    //     final BTextField descrip = new BTextField("", BangUI.TEXT_FIELD_MAX_LENGTH);
    //     bug.add(descrip, GroupLayout.FIXED);
    //     descrip.requestFocus();
    //     BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
    //     bug.add(buttons, GroupLayout.FIXED);

    //     ActionListener buglist = new ActionListener() {
    //         public void actionPerformed (ActionEvent event) {
    //             if (event.getAction().equals("submit")) {
    //                 BangClient.submitBugReport(_ctx, descrip.getText(), false);
    //             }
    //             _ctx.getBangClient().clearPopup(bug, true);
    //         }
    //     };
    //     BButton submit =
    //         new BButton(_msgs.get("m.bug_submit"), buglist, "submit");
    //     buttons.add(submit);
    //     buttons.add(new BButton(_msgs.get("m.cancel"), buglist, "cancel"));
    //     // disable the submit button until a description is entered
    //     new EnablingValidator(descrip, submit);
    //     bug.setModal(true);
    //     return bug;
    // }

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
        window.setModal(true);
        return window;
    }

    protected BDecoratedWindow createChatHistory ()
    {
        BDecoratedWindow window = new BDecoratedWindow(
            _ctx.getStyleSheet(), _msgs.get("m.chat_history_title"));
        ((GroupLayout) window.getLayoutManager()).setGap(10);
        window.setLayer(BangUI.POPUP_MENU_LAYER);

        BTextArea history = new BTextArea();
        history.setStyleClass("chat_history_log");
        history.setPreferredSize(new Dimension(800, 600));
        List<ChatMessage> list =
            ((BangChatDirector)_ctx.getChatDirector()).getMessageHistory();
        for (ChatMessage msg : list) {
            if (msg instanceof UserMessage) {
                UserMessage umsg = (UserMessage) msg;
                String who;
                ColorRGBA color;
                if (umsg instanceof TellFeedbackMessage) {
                    who = MessageBundle.tcompose("m.history_tell", umsg.speaker);
                    color = ColorRGBA.magenta;
                } else if (umsg.localtype == ChatCodes.USER_CHAT_TYPE) {
                    who = MessageBundle.tcompose("m.history_told", umsg.speaker);
                    color = ColorRGBA.magenta;
                } else {
                    who = MessageBundle.tcompose("m.history_speak", umsg.speaker);
                    color = ColorRGBA.blue;
                }
                history.appendText(_cmsgs.xlate(who), color);
                history.appendText(" " + umsg.message + "\n");

            } else if (msg instanceof SystemMessage) {
                history.appendText(msg.message + "\n", ColorRGBA.red);
            }
        }
        window.add(new BScrollPane(history));
        window.add(makeDismiss(window), GroupLayout.FIXED);
        window.setModal(true);
        return window;
    }

    protected void createCurrentLookSnapshot ()
    {
        PlayerObject user = _ctx.getUserObject();
        Look look = user.getLook(Look.Pose.DEFAULT);
        final File target = new File(System.getProperty("user.home") +
                                     File.separator + "Desktop" +
                                     File.separator + look.name + ".png");
        AvatarView.getImage(_ctx, look.getAvatar(user), new ResultListener<BufferedImage>() {
            public void requestCompleted (BufferedImage image) {
                try {
                    ImageIO.write(image, "PNG", target);
                    _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, "m.avatar_saved");
                } catch (Exception e) {
                    log.warning("Failed to write avatar image", "target", target, e);
                    _ctx.getChatDirector().displayFeedback(
                        BangCodes.BANG_MSGS, "m.avatar_save_failed");
                }
            }
            public void requestFailed (Exception cause) {
                // not called
            }
        });
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
    protected MessageBundle _msgs, _cmsgs;

    protected Type _poppedType = null;
    protected BDecoratedWindow _popped;
    protected boolean _autoBugged;

    protected static SimpleDateFormat _sfmt =
        new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    protected static final int CTRL_SHIFT =
        InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK;
}
