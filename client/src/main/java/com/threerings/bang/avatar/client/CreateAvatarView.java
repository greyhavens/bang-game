//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.HashSet;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.BGroup;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.LengthLimitedDocument;

import com.samskivert.util.RandomUtil;
import com.samskivert.util.Runnables;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.avatar.data.AvatarCodes;

/**
 * Displays an interface via which the player can create their avatar: name,
 * sex and default look.
 */
public class CreateAvatarView extends SteelWindow
    implements BangClient.NonClearablePopup
{
    /**
     * Shows the create character interface.
     */
    public static void show (BangContext ctx)
    {
        show(ctx, Runnables.NOOP);
    }

    /**
     * Shows the create character interface and calls the supplied runnable if and when the player
     * creates their character.
     */
    public static void show (BangContext ctx, Runnable onCreate)
    {
        CreateAvatarView view = new CreateAvatarView(ctx);
        view._onCreate = onCreate;
        ctx.getBangClient().displayPopup(view, true, WIDTH_HINT);
    }

    protected CreateAvatarView (BangContext ctx)
    {
        super(ctx, ctx.xlate(AvatarCodes.AVATAR_MSGS, "m.create_title"));
        _contents.setLayoutManager(BGroup.vert().alignCenter().gap(15).make());

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(AvatarCodes.AVATAR_MSGS);
        setModal(true);

        _contents.add(new BLabel(_msgs.get("m.create_intro"), "dialog_text"));
        _contents.setStyleClass("padded");

        BContainer inner = BGroup.vert().offStretch().alignTop().gap(15).makeBox();
        inner.setStyleClass("fa_inner_box");
        _contents.add(inner);
        _status = new StatusLabel(ctx);
        _status.setStyleClass("dialog_text");
        _contents.add(_status);
        _buttons.add(new BButton(_msgs.get("m.cancel"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _ctx.getBangClient().clearPopup(CreateAvatarView.this, true);
                _ctx.getBangClient().createAvatarDismissed(false);
            }
        }, "cancel"));
        _buttons.add(_done = new BButton(_msgs.get("m.done"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                createAvatar();
            }
        }, "done"));
        _done.setEnabled(false);

        // this all goes in the inner box
        BContainer row = BGroup.horizStretch().offStretch().makeBox();
        BContainer col = BGroup.horiz().alignLeft().makeBox();
        col.add(new Spacer(20, 1));
        col.add(new BLabel(_msgs.get("m.persuasion"), "dialog_label"));
        String[] gensel = new String[] {
            _msgs.get("m.male"), _msgs.get("m.female") };
        col.add(_gender = new BComboBox(gensel));
        _gender.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                _look.setGender(_gender.getSelectedIndex() == 0);
                pickRandomHandle();
                maybeClearStatus();
            }
        });
        row.add(col);

        col = BGroup.horiz().alignRight().makeBox();
        col.add(new BLabel(_msgs.get("m.handle"), "dialog_label"));
        col.add(_handle = new BTextField(""));
        _handle.setPreferredWidth(150);
        _handle.setDocument(new HandleDocument());
        _handle.addListener(new HandleListener(_done, _status, _msgs.get("m.create_defstatus"),
                                               _msgs.get("m.invalid_handle")));

        col.add(BangUI.createDiceButton(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                pickRandomHandle();
                maybeClearStatus();
            }
        }, "random"), GroupLayout.FIXED);
        row.add(col);
        inner.add(row);
        inner.add(_look = new FirstLookView(ctx, _status));
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        // start with a random gender which will trigger name list and avatar
        // display configuration
        _gender.selectItem(RandomUtil.getInt(2));
    }

    protected void pickRandomHandle ()
    {
        boolean isMale = (_gender.getSelectedIndex() == 0);
        HashSet<String> first, second, initials;
        initials = NameFactory.getCreator().getHandleInitials();
        String initial;
        int random = RandomUtil.getInt(2 + initials.size());
        if (random == 0) {
            first = NameFactory.getCreator().getHandlePrefixes(isMale);
            second = NameFactory.getCreator().getHandleRoots(isMale);
            initial = " ";
        } else if (random == 1) {
            first = NameFactory.getCreator().getHandleRoots(isMale);
            second = NameFactory.getCreator().getHandleSuffixes(isMale);
            initial = " ";
        } else {
            first = NameFactory.getCreator().getHandleRoots(isMale);
            second = NameFactory.getCreator().getHandleFamily();
            initial = " " + RandomUtil.pickRandom(initials.iterator(), initials.size()) + " ";
        }
        String fname = RandomUtil.pickRandom(first.iterator(), first.size());
        String sname = RandomUtil.pickRandom(second.iterator(), second.size());
        _handle.setText(fname + initial + sname);
    }

    protected void createAvatar ()
    {
        AvatarService asvc = _ctx.getClient().requireService(AvatarService.class);
        AvatarService.ConfirmListener cl = new AvatarService.ConfirmListener() {
            public void requestProcessed () {
                // move to the next phase of the intro
                _ctx.getBangClient().clearPopup(CreateAvatarView.this, true);
                _ctx.getBangClient().createAvatarDismissed(true);
                _onCreate.run();
            }
            public void requestFailed (String reason) {
                _status.setStatus(_msgs.xlate(reason), true);
                _failed = true;
                _handle.setEnabled(true);
                _done.setEnabled(true);
            }
        };
        _handle.setEnabled(false);
        _done.setEnabled(false);

        Handle handle = new Handle(_handle.getText());
        boolean isMale = (_gender.getSelectedIndex() == 0);
        asvc.createAvatar(handle, isMale, _look.getLookConfig(),
                          _look.getDefaultArticleColorizations(), cl);
    }

    protected void maybeClearStatus ()
    {
        if (_failed) {
            _status.setStatus(_msgs.get("m.create_defstatus"), false);
        }
    }

    protected static class HandleListener implements TextListener
    {
        public HandleListener (BButton button, StatusLabel status,
                               String defaultStatus, String invalidStatus)
        {
            _button = button;
            _status = status;
            _defstatus = defaultStatus;
            _invstatus = invalidStatus;
        }

        public void textChanged (TextEvent event) {
            String text = ((BTextField)event.getSource()).getText();
            boolean valid = NameFactory.getValidator().isValidHandle(new Handle(text));
            _button.setEnabled(valid);
            int minLength = NameFactory.getValidator().getMinHandleLength();
            _status.setStatus((!valid && text.length() >= minLength) ?
                              _invstatus : _defstatus, false);
        }

        protected BButton _button;
        protected StatusLabel _status;
        protected String _defstatus, _invstatus;
    }

    protected static class HandleDocument extends LengthLimitedDocument
    {
        public HandleDocument () {
            super(NameFactory.getValidator().getMaxHandleLength());
        }

        public boolean replace (int offset, int length, String text) {
            StringBuffer buf = new StringBuffer();
            for (int ii = 0, ll = text.length(); ii < ll; ii++) {
                char c = text.charAt(ii);
                // filter out non-letters and whitespace
                if (!Character.isLetter(c) && !Character.isWhitespace(c)) {
                    continue;
                }
                // if they're just starting to type from a blank slate,
                // capitalize the first letter they type; doing it at any other
                // time has the potential to result in unintended behavior
                if (getLength() == 0 && ii == 0 && Character.isLowerCase(c)) {
                    buf.append(Character.toUpperCase(c));
                } else {
                    buf.append(c);
                }
            }
            text = buf.toString();

            // if we've reduced this to a NOOP, we have to indicate that we've rejected the edit
            if (length == 0 && text.length() == 0) {
                return false;
            }

            return super.replace(offset, length, text);
        }

        protected boolean validateEdit (String oldText, String newText) {
            // disallow consecutive spaces
            return super.validateEdit(oldText, newText) &&
                !newText.matches(".*\\s\\s.*");
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected StatusLabel _status;
    protected boolean _failed;
    protected Runnable _onCreate = Runnables.NOOP;

    protected BComboBox _gender;
    protected BTextField _handle;
    protected FirstLookView _look;
    protected BButton _done;

    protected static final int WIDTH_HINT = 800;
}
