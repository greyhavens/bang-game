//
// $Id$

package com.threerings.bang.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.jme.system.DisplaySystem;
import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BList;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BSlider;
import com.jmex.bui.BWindow;
import com.jmex.bui.BoundedRangeModel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.ChangeEvent;
import com.jmex.bui.event.ChangeListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.StringUtil;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.crowd.chat.client.CurseFilter;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.TabbedPane;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Allows options to be viewed and adjusted. Presently that's just video
 * mode and whether or not we're in full screen mode.
 */
public class OptionsView extends BDecoratedWindow
    implements ActionListener
{
    /** The types of sound that can be configured. */
    public static enum SoundType { MUSIC, EFFECTS };

    /**
     * Creates a slider for adjusting the specified sound type.
     */
    public static BContainer createSoundSlider (
        final BangContext ctx, final SoundType type)
    {
        int value = 0;
        switch (type) {
        case MUSIC: value = BangPrefs.getMusicVolume(); break;
        case EFFECTS: value = BangPrefs.getEffectsVolume(); break;
        }

        // create our slider and label display
        BSlider slider = new BSlider(BSlider.HORIZONTAL, 0, 100, value);
        final BLabel vallbl = new BLabel(slider.getModel().getValue() + "%");
        vallbl.setPreferredSize(new Dimension(50, 10));
        slider.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                BoundedRangeModel model = (BoundedRangeModel)event.getSource();
                switch (type) {
                case MUSIC:
                    BangPrefs.updateMusicVolume(model.getValue());
                    ctx.getBangClient().setMusicVolume(model.getValue());
                    break;
                case EFFECTS:
                    BangPrefs.updateEffectsVolume(model.getValue());
                    ctx.getSoundManager().setBaseGain(model.getValue()/100f);
                    break;
                }
                vallbl.setText(model.getValue() + "%");
            }
        });

        // create a wrapper to hold them both
        BContainer wrapper = new BContainer(GroupLayout.makeHStretch());
        wrapper.add(slider);
        wrapper.add(vallbl, GroupLayout.FIXED);
        return wrapper;
    }

    public OptionsView (BangContext ctx, BWindow parent)
    {
        super(ctx.getStyleSheet(), ctx.xlate(BangCodes.OPTS_MSGS, "m.title"));

        ((GroupLayout)getLayoutManager()).setGap(15);
        setModal(true);

        _ctx = ctx;
        _parent = parent;
        _msgs = ctx.getMessageManager().getBundle(BangCodes.OPTS_MSGS);

        TabbedPane tabs = new TabbedPane(false);
        tabs.setPreferredSize(new Dimension(375, 275));

        // create the General tab
        TableLayout layout = new TableLayout(2, 10, 10);
        layout.setHorizontalAlignment(TableLayout.CENTER);
        layout.setVerticalAlignment(TableLayout.CENTER);
        BContainer cont = new BContainer(layout);
        cont.setStyleClass("options_tab");
        tabs.addTab(_msgs.get("t.general"), cont);

        cont.add(new BLabel(_msgs.get("m.video_mode"), "right_label"));
        cont.add(_modes = new BComboBox());

        cont.add(new BLabel(_msgs.get("m.fullscreen"), "right_label"));
        cont.add(_fullscreen = new BCheckBox(""));
        _fullscreen.setSelected(BangPrefs.isFullscreen());
        _fullscreen.addListener(_modelist);

        cont.add(new BLabel(_msgs.get("m.detail_lev"), "right_label"));
        cont.add(createDetailSlider());

        cont.add(new BLabel(_msgs.get("m.music_vol"), "right_label"));
        cont.add(createSoundSlider(_ctx, SoundType.MUSIC));
        cont.add(new BLabel(_msgs.get("m.effects_vol"), "right_label"));
        cont.add(createSoundSlider(_ctx, SoundType.EFFECTS));

        // create the Chat tab if we're logged on
        if (ctx.getMuteDirector() != null) {
            cont = new BContainer(
                GroupLayout.makeVert(GroupLayout.STRETCH, GroupLayout.CENTER,
                                     GroupLayout.CONSTRAIN));
            ((GroupLayout)cont.getLayoutManager()).setGap(10);
            cont.setStyleClass("options_tab");
            tabs.addTab(_msgs.get("t.chat"), cont);

            BContainer top = new BContainer(layout);
            cont.add(top, GroupLayout.FIXED);

            top.add(new BLabel(_msgs.get("m.chat_mode"), "right_label"));
            final BComboBox chatmode = new BComboBox();
            top.add(chatmode);
            BComboBox.Item selitem = null;
            for (CurseFilter.Mode mode : CurseFilter.Mode.values()) {
                String label = _msgs.get("m.cfm_" + StringUtil.toUSLowerCase(mode.toString()));
                BComboBox.Item item = new BComboBox.Item(mode, label);
                chatmode.addItem(item);
                if (mode.equals(BangPrefs.getChatFilterMode())) {
                    selitem = item;
                }
            }
            chatmode.selectItem(selitem);
            chatmode.addListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    BangPrefs.setChatFilterMode((CurseFilter.Mode)chatmode.getSelectedValue());
                }
            });

            top.add(new BLabel(_msgs.get("m.chat_mogrify"), "right_label"));
            final BCheckBox mogrify = new BCheckBox("");
            mogrify.setSelected(BangPrefs.getChatMogrifierEnabled());
            mogrify.addListener(new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    BangPrefs.setChatMogrifierEnabled(mogrify.isSelected());
                    _ctx.getChatDirector().setMogrifyChat(mogrify.isSelected());
                }
            });
            top.add(mogrify);

            cont.add(new BLabel(_msgs.get("m.ignore_list")), GroupLayout.FIXED);

            BScrollPane sp = new BScrollPane(
                _muted = new BList(ctx.getMuteDirector().getMuted()));
            sp.setStyleClass("mute_list_pane");
            sp.setPreferredSize(new Dimension(250, 50));
            cont.add(sp);
            _muted.addListener(this);

            _remove = new BButton(_msgs.get("b.remove"), this, "remove");
            cont.add(_remove, GroupLayout.FIXED);
            _remove.setEnabled(false);
        }

        add(tabs);

        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)bcont.getLayoutManager()).setGap(25);
        bcont.add(new BButton(_msgs.get("m.quit"), this, "quit"));
        bcont.add(new BButton(_msgs.get("m.resume"), this, "dismiss"));
        add(bcont, GroupLayout.FIXED);

        _mode = Display.getDisplayMode();
        refreshDisplayModes();
        _modes.addListener(_modelist);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("dismiss".equals(event.getAction())) {
            _ctx.getBangClient().clearPopup(this, true);
        } else if ("quit".equals(event.getAction())) {
            _ctx.getApp().stop();
        } else if (BList.SELECT.equals(event.getAction())) {
            _remove.setEnabled(_muted.getSelectedValue() != null);
        } else if ("remove".equals(event.getAction())) {
            Name name = (Name)_muted.getSelectedValue();
            _ctx.getMuteDirector().setMuted(name, false);
            _muted.removeValue(name);
            _remove.setEnabled(false);
        }
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        Dimension d = super.computePreferredSize(whint, hhint);
        d.width = Math.max(d.width, 350);
        return d;
    }

    protected BContainer createDetailSlider ()
    {
        // create our slider and label display
        final BangPrefs.DetailLevel[] levels =
            BangPrefs.DetailLevel.class.getEnumConstants();
        BangPrefs.DetailLevel level = BangPrefs.getDetailLevel();
        BSlider slider = new BSlider(BSlider.HORIZONTAL, 0, levels.length - 1,
            level.ordinal());
        final BLabel vallbl = new BLabel(getDetailText(level));
        vallbl.setPreferredSize(new Dimension(65, 10));
        slider.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                BangPrefs.DetailLevel level =
                    levels[((BoundedRangeModel)event.getSource()).getValue()];
                BangPrefs.updateDetailLevel(level);
                vallbl.setText(getDetailText(level));

                // as soon as the user makes a detail choice on their own, we
                // can stop making suggestions
                BangPrefs.setSuggestDetail(false);
            }
        });

        // create a wrapper to hold them both
        BContainer wrapper = new BContainer(GroupLayout.makeHStretch());
        wrapper.add(slider);
        wrapper.add(vallbl, GroupLayout.FIXED);
        return wrapper;
    }

    protected String getDetailText (BangPrefs.DetailLevel level)
    {
        return _msgs.get("m.detail_" + StringUtil.toUSLowerCase(level.name()));
    }

    protected void refreshDisplayModes ()
    {
        int maxwidth = 0, maxheight = 0;
        boolean have1024 = false, have1280 = false;

        try {
            ArrayList<DisplayMode> modes = new ArrayList<DisplayMode>();
            CollectionUtil.addAll(modes, Display.getAvailableDisplayModes());
            for (Iterator<DisplayMode> iter = modes.iterator();
                 iter.hasNext(); ) {
                DisplayMode mode = iter.next();
                // our minimum display size is 1024x768
                if (mode.getWidth() < BangUI.MIN_WIDTH ||
                        mode.getHeight() < BangUI.MIN_HEIGHT) {
                    iter.remove();
                }

                // note our maximum available display sizes
                maxwidth = Math.max(maxwidth, mode.getWidth());
                maxheight = Math.max(maxheight, mode.getHeight());

                // note whether we have our two basic sizes
                if (mode.getWidth() == 1024 && mode.getHeight() == 768) {
                    have1024 = true;
                }
                if (mode.getWidth() == 1280 && mode.getHeight() == 1024) {
                    have1280 = true;
                }
            }

            // if we're on Linux and don't have 1024x768 or 1280x1024 but we
            // have larger modes, it's probably because of Xinerama wackiness,
            // so we add non-fullscreen versions for those
            if (RunAnywhere.isLinux()) {
                if (!have1024 && maxwidth >= 1024 && maxheight >= 768) {
                    modes.add(new DisplayMode(1024, 768));
                }
                if (!have1280 && maxwidth >= 1280 && maxheight >= 1024) {
                    modes.add(new DisplayMode(1280, 1024));
                }
            }

            ModeItem current = null;
            ModeItem[] items = new ModeItem[modes.size()];
            for (int ii = 0; ii < items.length; ii++) {
                DisplayMode mode = modes.get(ii);
                items[ii] = new ModeItem(mode);
                if (isCurrent(mode)) {
                    current = items[ii];
                }
            }
            Arrays.sort(items);
            _modes.setItems(items);
            _modes.selectItem(current);

        } catch (LWJGLException e) {
            log.warning("Failed to obtain display modes", e);
        }
    }

    protected void updateDisplayMode (DisplayMode mode, boolean confirm)
    {
        boolean wantFullscreen = _fullscreen.isSelected();
        if (mode == null || (_mode != null && _mode.equals(mode) &&
                             wantFullscreen == BangPrefs.isFullscreen())) {
            return;
        }

        final DisplayMode omode = _mode;
        _mode = mode;
        log.info("Switching to " + _mode + " (from " + omode + ")");

        // we fake up non-full screen display modes above, but there's no way
        // to set the bit depth to anything but zero, so we have to adjust that
        // here so that JME doesn't freak out
        int bpp = Math.max(16, _mode.getBitsPerPixel());
        int width = _mode.getWidth(), height = _mode.getHeight();
        DisplaySystem ds = _ctx.getDisplay();
        ds.recreateWindow(width, height, bpp, _mode.getFrequency(), wantFullscreen);

        // reconfigure the camera frustum in case the aspect ratio changed
        _ctx.getCameraHandler().getCamera().setFrustumPerspective(
            45.0f, width/(float)height, 1, 10000);

        // recenter and invalidate the main view and options window
        if (_parent != null) {
            if (_parent instanceof ShopView || _parent instanceof LogonView) {
                _parent.center();
            } else {
                _parent.setBounds(0, 0, _ctx.getDisplay().getWidth(),
                                  _ctx.getDisplay().getHeight());
            }
        }
        center();

        // if we don't need to confirm, stop here
        if (!confirm) {
            return;
        }

        OptionDialog.ResponseReceiver rr = new OptionDialog.ResponseReceiver() {
            public void resultPosted (int button, Object result) {
                switch (button) {
                case OptionDialog.OK_BUTTON:
                    // store these settings for later
                    BangPrefs.updateDisplayMode(_mode);
                    BangPrefs.updateFullscreen(_fullscreen.isSelected());
                    break;

                default:
                    // revert to the old mode
                    updateDisplayMode(omode, false);
                    break;
                }
            }
        };
        OptionDialog.showConfirmDialog(
            _ctx, BangCodes.OPTS_MSGS, "m.keep_mode", rr);

    }

    protected void fullscreenRestart ()
    {
        if (_ctx.getDisplay().isFullScreen() == BangPrefs.isFullscreen()) {
            _fullscreen.setText("");
            return;
        }

        _fullscreen.setText(_msgs.get("m.restart_required"));
        OptionDialog.ResponseReceiver rr = new OptionDialog.ResponseReceiver() {
            public void resultPosted (int button, Object result) {
                if (button == OptionDialog.OK_BUTTON) {
                    if (!BangClient.relaunchGetdown(_ctx, 500L)) {
                        log.info("Failed to restart Bang, exiting");
                        _ctx.getApp().stop();
                    }
                }
            }
        };
        OptionDialog.showConfirmDialog(_ctx, BangCodes.OPTS_MSGS,
                "m.fullscreen_changed", "m.restart", "m.resume", rr);
    }

    protected boolean isCurrent (DisplayMode mode)
    {
        return (_mode.getWidth() == mode.getWidth() &&
                _mode.getHeight() == mode.getHeight() &&
                (_mode.getFrequency() == 0 ||
                 _mode.getFrequency() == mode.getFrequency()) &&
                (_mode.getBitsPerPixel() == 0 ||
                 _mode.getBitsPerPixel() == mode.getBitsPerPixel()));
    }

    protected static class ModeItem implements Comparable<ModeItem>
    {
        public DisplayMode mode;

        public ModeItem (DisplayMode mode) {
            this.mode = mode;
        }

        public String toString () {
            String text = mode.getWidth() + "x" + mode.getHeight();
            if (mode.getBitsPerPixel() > 0) {
                text += ("x" + mode.getBitsPerPixel() + " " +
                         mode.getFrequency() + "Hz");
            }
            return text;
        }

        public int compareTo (ModeItem other) {
            DisplayMode omode = other.mode;
            if (mode.getWidth() != omode.getWidth()) {
                return mode.getWidth() - omode.getWidth();
            } else if (mode.getHeight() != omode.getHeight()) {
                return mode.getHeight() - omode.getHeight();
            } else if (mode.getBitsPerPixel() != omode.getBitsPerPixel()) {
                return mode.getBitsPerPixel() - omode.getBitsPerPixel();
            } else {
                return mode.getFrequency() - omode.getFrequency();
            }
        }
    }

    protected ActionListener _modelist = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            updateDisplayMode(((ModeItem)_modes.getSelectedItem()).mode, true);
        }
    };

    protected BangContext _ctx;
    protected BWindow _parent;
    protected MessageBundle _msgs;
    protected DisplayMode _mode;

    protected BComboBox _modes;
    protected BCheckBox _fullscreen;

    protected BList _muted;
    protected BButton _remove;
}
