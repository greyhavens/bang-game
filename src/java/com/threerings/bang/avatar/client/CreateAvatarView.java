//
// $Id$

package com.threerings.bang.avatar.client;

import java.util.HashSet;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;

import com.threerings.bang.client.bui.StatusLabel;
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
        setStyleClass("fa_window");
        setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER));
        ((GroupLayout)getLayoutManager()).setGap(15);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(AvatarCodes.AVATAR_MSGS);

        add(new BLabel(_msgs.get("m.create_title"), "scroll_title"));
        add(new BLabel(_msgs.get("m.create_intro"), "fa_text"));

        GroupLayout glay = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        glay.setGap(15);
        BContainer inner = new BContainer(glay);
        inner.setStyleClass("fa_inner_box");
        add(inner);
        _status = new StatusLabel(ctx);
        _status.setStyleClass("fa_text");
        _status.setStatus(_msgs.get("m.create_tip"), false);
        add(_status);
        add(_done = new BButton(_msgs.get("m.done"), this, "done"));

        // this all goes in the inner box
        BContainer hcont = GroupLayout.makeHBox(GroupLayout.LEFT);
        hcont.add(new Spacer(20, 1));
        hcont.add(new BLabel(_msgs.get("m.persuasion"), "fa_label"));
        String[] gensel = new String[] {
            _msgs.get("m.male"), _msgs.get("m.female") };
        hcont.add(_gender = new BComboBox(gensel));
        _gender.addListener(_sexer);
        inner.add(hcont);

        inner.add(_look = new FirstLookView(ctx, _status));

        hcont = GroupLayout.makeHBox(GroupLayout.LEFT);
        hcont.add(new Spacer(20, 1));
        hcont.add(new BLabel(_msgs.get("m.handle"), "fa_label"));
        hcont.add(_handle = new BTextField(""));
        _handle.setPreferredWidth(125);
        // TODO: wire up handle validation stuff

        hcont.add(new Spacer(25, 5));
        hcont.add(_prefix = new BComboBox());
        _prefix.addListener(_namer);
        _root = new BComboBox();
        _root.addListener(_namer);
        hcont.add(_root);
        hcont.add(_suffix = new BComboBox());
        _suffix.addListener(_namer);

        hcont.add(new Spacer(15, 5));
        ImageIcon dicon = new ImageIcon(ctx.loadImage("ui/icons/dice.png"));
        hcont.add(new BButton(dicon, this, "random"));
        inner.add(hcont);

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
                _status.setStatus(_msgs.xlate(reason), true);
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
            _status.setStatus("", false);
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
    protected StatusLabel _status;
    protected boolean _failed;

    protected BComboBox _gender;
    protected BTextField _handle;
    protected BComboBox _prefix, _root, _suffix;
    protected FirstLookView _look;
    protected BButton _done;

    protected static final int PREF_WIDTH = 640;
}
