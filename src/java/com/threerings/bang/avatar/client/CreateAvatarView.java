//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.HashSet;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.BTextArea;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;

import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.avatar.data.AvatarCodes;

/**
 * Displays an interface via which the player can create their avatar: name,
 * sex and default look.
 */
public class CreateAvatarView extends BDecoratedWindow
    implements ActionListener
{
    public CreateAvatarView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        setLayoutManager(GroupLayout.makeVStretch());

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(AvatarCodes.AVATAR_MSGS);

        _status = new BTextArea(_msgs.get("m.create_tip"));
        _status.setPreferredWidth(PREF_WIDTH);

        BLabel title = new BLabel(_msgs.get("m.create_title"), "dialog_title");
        add(title, GroupLayout.FIXED);

        BTextArea intro = new BTextArea(_msgs.get("m.create_intro"));
        intro.setPreferredWidth(PREF_WIDTH);
        add(intro, GroupLayout.FIXED);

        BContainer hcont = GroupLayout.makeHBox(GroupLayout.LEFT);
        hcont.add(new Spacer(25, 5));
        hcont.add(new BLabel(_msgs.get("m.persuasion")));
        String[] gensel = new String[] {
            _msgs.get("m.male"), _msgs.get("m.female") };
        hcont.add(_gender = new BComboBox(gensel));
        _gender.addListener(_sexer);
        hcont.add(new Spacer(5, 5));

        hcont.add(new BLabel(_msgs.get("m.handle")));
        hcont.add(_handle = new BTextField(""));
        _handle.setPreferredWidth(125);
        // TODO: wire up handle validation stuff

        hcont.add(new Spacer(5, 5));
        hcont.add(new BButton("*", this, "random"));

        hcont.add(_prefix = new BComboBox());
        _prefix.addListener(_namer);

        _root = new BComboBox();
        _root.addListener(_namer);
        hcont.add(_root);

        hcont.add(_suffix = new BComboBox());
        _suffix.addListener(_namer);

        add(hcont, GroupLayout.FIXED);

        BTextArea tip = new BTextArea(_msgs.get("m.avatar_tip"));
        tip.setPreferredWidth(PREF_WIDTH);
        add(tip, GroupLayout.FIXED);
        add(_look = new FirstLookView(ctx, null)); // TODO: sort out status

        add(_status, GroupLayout.FIXED);

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(_done = new BButton(_msgs.get("m.done"), this, "done"));
        add(buttons, GroupLayout.FIXED);

        // start with a random gender which will trigger name list and avatar
        // display configuration
        _gender.selectItem(RandomUtil.getInt(2));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("random")) {
            if (RandomUtil.getInt(100) >= 50) {
                _prefix.selectItem(RandomUtil.getInt(_prefix.getItemCount()));
                _root.selectItem(RandomUtil.getInt(_root.getItemCount()));
            } else {
                _root.selectItem(RandomUtil.getInt(_root.getItemCount()));
                _suffix.selectItem(RandomUtil.getInt(_suffix.getItemCount()));
            }
            maybeClearStatus();

        } else if (cmd.equals("done")) {
            createAvatar();
        }
    }

    protected void updateLists (boolean isMale)
    {
        // configure the proper gendered lists
        HashSet<String> names;
        names = NameFactory.getCreator().getHandlePrefixes(isMale);
        _prefix.setItems(names.toArray(new String[names.size()]));
        names = NameFactory.getCreator().getHandleRoots(isMale);
        _root.setItems(names.toArray(new String[names.size()]));
        names = NameFactory.getCreator().getHandleSuffixes(isMale);
        _suffix.setItems(names.toArray(new String[names.size()]));

        // start with a random selection
        _prefix.selectItem(RandomUtil.getInt(_prefix.getItemCount()));
        _root.selectItem(RandomUtil.getInt(_root.getItemCount()));
    }

    protected void createAvatar ()
    {
        AvatarService asvc = (AvatarService)
            _ctx.getClient().requireService(AvatarService.class);
        AvatarService.ConfirmListener cl = new AvatarService.ConfirmListener() {
            public void requestProcessed () {
                dismiss();
                // move to the next phase of the intro
                _ctx.getBangClient().checkShowIntro();
            }
            public void requestFailed (String reason) {
                _status.setText(_msgs.xlate(reason));
                _failed = true;
                _done.setEnabled(true);
            }
        };
        _done.setEnabled(false);

        Handle handle = new Handle(_handle.getText());
        boolean isMale = (_gender.getSelectedIndex() == 0);
        asvc.createAvatar(
            _ctx.getClient(), handle, isMale, _look.getLookConfig(), cl);
    }

    protected void maybeClearStatus ()
    {
        if (_failed) {
            _status.setText("");
        }
    }

    protected ActionListener _namer = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            if (event.getSource() == _prefix &&
                _prefix.getSelectedItem() != null) {
                _suffix.selectItem(-1);
            } else if (event.getSource() == _suffix &&
                       _suffix.getSelectedItem() != null) {
                _prefix.selectItem(-1);
            }

            String prefix = (String)_prefix.getSelectedItem();
            String root = (String)_root.getSelectedItem();
            String suffix = (String)_suffix.getSelectedItem();
            _handle.setText((prefix == null) ?
                            (root + " " + suffix) : (prefix + " " + root));
            maybeClearStatus();
        }
    };

    protected ActionListener _sexer = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            boolean isMale = (_gender.getSelectedIndex() == 0);
            updateLists(isMale);
            _look.setGender(isMale);
            maybeClearStatus();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BTextArea _status;
    protected boolean _failed;

    protected BComboBox _gender;
    protected BTextField _handle;
    protected BComboBox _prefix, _root, _suffix;
    protected FirstLookView _look;
    protected BButton _done;

    protected static final int PREF_WIDTH = 640;
}
