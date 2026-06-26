//
// $Id$

package com.threerings.bang.client;

import java.util.Arrays;

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

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.crowd.chat.client.CurseFilter;

import com.threerings.jme.JmeApp;
import com.threerings.jme.camera.GodViewHandler;

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

    /**
     * Creates a slider for adjusting the WASD/arrow camera pan speed (BUG-5). The change takes
     * effect immediately (via {@link GodViewHandler#setPanSpeed}) and is persisted in
     * {@link BangPrefs}. The slider runs over the comfortable
     * {@link GodViewHandler#MIN_PAN_SPEED}..{@link GodViewHandler#MAX_PAN_SPEED} range.
     */
    public static BContainer createPanSpeedSlider (final BangContext ctx)
    {
        final int min = Math.round(GodViewHandler.MIN_PAN_SPEED);
        final int max = Math.round(GodViewHandler.MAX_PAN_SPEED);
        int value = Math.max(min, Math.min(max, BangPrefs.getCameraPanSpeed()));

        BSlider slider = new BSlider(BSlider.HORIZONTAL, min, max, value);
        final BLabel vallbl = new BLabel(String.valueOf(value));
        vallbl.setPreferredSize(new Dimension(50, 10));
        slider.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                int val = ((BoundedRangeModel)event.getSource()).getValue();
                BangPrefs.updateCameraPanSpeed(val);
                GodViewHandler.setPanSpeed(val);
                vallbl.setText(String.valueOf(val));
            }
        });

        // create a wrapper to hold them both
        BContainer wrapper = new BContainer(GroupLayout.makeHStretch());
        wrapper.add(slider);
        wrapper.add(vallbl, GroupLayout.FIXED);
        return wrapper;
    }

    /**
     * Creates a slider for one of the board-lighting tuning knobs ("diffuse" = building/sun
     * brightness, "ambient" = flat fill, "shadow" = shadow darkening), as percent (0..200, 100 =
     * the board's authored 1.0x). The change persists in {@link BangPrefs} and applies live to the
     * active board view (to dial the jME3 lighting toward the fork's flatter look).
     */
    public static BContainer createLightSlider (final BangContext ctx, final String which)
    {
        int value = "ambient".equals(which) ? BangPrefs.getLightAmbient() :
            "shadow".equals(which) ? BangPrefs.getLightShadow() : BangPrefs.getLightDiffuse();
        BSlider slider = new BSlider(BSlider.HORIZONTAL, 0, 200, value);
        final BLabel vallbl = new BLabel(value + "%");
        vallbl.setPreferredSize(new Dimension(50, 10));
        slider.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                int val = ((BoundedRangeModel)event.getSource()).getValue();
                if ("ambient".equals(which)) {
                    BangPrefs.updateLightAmbient(val);
                } else if ("shadow".equals(which)) {
                    BangPrefs.updateLightShadow(val);
                } else {
                    BangPrefs.updateLightDiffuse(val);
                }
                com.threerings.bang.game.client.BoardView.applyLightingPrefs();
                vallbl.setText(val + "%");
            }
        });

        // create a wrapper to hold them both
        BContainer wrapper = new BContainer(GroupLayout.makeHStretch());
        wrapper.add(slider);
        wrapper.add(vallbl, GroupLayout.FIXED);
        return wrapper;
    }

    /**
     * Creates the "Warmth" slider (0..100%), which drives the world-viewport warm-grade post-process
     * tint (the fork's golden sun-baked look). Persists in {@link BangPrefs} (key "sepia") and applies
     * live to the active board view (full in the town/menu, reduced during gameplay).
     */
    public static BContainer createSepiaSlider (final BangContext ctx)
    {
        int value = BangPrefs.getSepia();
        BSlider slider = new BSlider(BSlider.HORIZONTAL, 0, 100, value);
        final BLabel vallbl = new BLabel(value + "%");
        vallbl.setPreferredSize(new Dimension(50, 10));
        slider.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                int val = ((BoundedRangeModel)event.getSource()).getValue();
                BangPrefs.updateSepia(val);
                com.threerings.bang.game.client.BoardView.applySepiaPref();
                vallbl.setText(val + "%");
            }
        });
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
        // taller than the original 275 to fit the added lighting + sepia tuning sliders w/o clipping
        tabs.setPreferredSize(new Dimension(375, 410));

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

        // board-lighting tuning knobs (dial jME3 lighting toward the fork's flatter look); these
        // apply to any active board view, including the town hub's TownBoardView
        cont.add(new BLabel("Building light", "right_label"));
        cont.add(createLightSlider(_ctx, "diffuse"));
        cont.add(new BLabel("Ambient fill", "right_label"));
        cont.add(createLightSlider(_ctx, "ambient"));
        cont.add(new BLabel("Shadow darkness", "right_label"));
        cont.add(createLightSlider(_ctx, "shadow"));
        cont.add(new BLabel("Warmth", "right_label"));
        cont.add(createSepiaSlider(_ctx));

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

        // seed _mode from the live LWJGL3 context size if available (was Display.getDisplayMode()),
        // falling back to the stored preference for the offscreen/headless case.
        int liveW = _ctx.getApp().getDisplayWidth(), liveH = _ctx.getApp().getDisplayHeight();
        int curW = liveW > 0 ? liveW : BangPrefs.getDisplayWidth();
        int curH = liveH > 0 ? liveH : BangPrefs.getDisplayHeight();
        _mode = new ModeItem(curW, curH, BangPrefs.getDisplayBPP(), BangPrefs.getDisplayFreq());
        // reflect the live fullscreen state in the checkbox (not just the stored preference).
        _wasFullscreen = _ctx.getApp().isFullscreen();
        _fullscreen.setSelected(_wasFullscreen);
        refreshDisplayModes();
        _modes.addListener(_modelist);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _ctx.getApp().addResizeListener(_resizer);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getApp().removeResizeListener(_resizer);
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

    /** Common windowed resolutions offered in the chooser; filtered to &gt;= the playable minimum
     * and capped at the monitor's native size. */
    protected static final int[][] WINDOWED_MODES = {
        { 1024, 768 }, { 1280, 720 }, { 1280, 800 }, { 1280, 1024 }, { 1366, 768 },
        { 1440, 900 }, { 1600, 900 }, { 1680, 1050 }, { 1920, 1080 }, { 1920, 1200 },
        { 2560, 1440 }, { 3840, 2160 },
    };

    protected void refreshDisplayModes ()
    {
        // jME3 cutover: the fork called Display.getAvailableDisplayModes(); the LWJGL3 DisplayInfo
        // only exposes the monitor's current (native) mode, so we offer a curated windowed list
        // filtered to >= MIN_WIDTH/MIN_HEIGHT and capped at the monitor's native resolution, plus
        // the monitor's native mode itself, plus the current mode (so the chooser always contains
        // the running mode). bpp/freq come from the monitor where known.
        int[] mon = _ctx.getApp().getPrimaryMonitorMode();
        int maxW = (mon != null) ? mon[0] : Integer.MAX_VALUE;
        int maxH = (mon != null) ? mon[1] : Integer.MAX_VALUE;
        int freq = (mon != null && mon[2] > 0) ? mon[2] : BangPrefs.getDisplayFreq();

        java.util.LinkedHashSet<ModeItem> set = new java.util.LinkedHashSet<ModeItem>();
        // always include the current running mode so the selection resolves.
        set.add(_mode);
        for (int[] wm : WINDOWED_MODES) {
            if (wm[0] >= BangUI.MIN_WIDTH && wm[1] >= BangUI.MIN_HEIGHT &&
                wm[0] <= maxW && wm[1] <= maxH) {
                set.add(new ModeItem(wm[0], wm[1], 0, 0));
            }
        }
        // the monitor's native mode (also used as the fullscreen target).
        if (mon != null && mon[0] >= BangUI.MIN_WIDTH && mon[1] >= BangUI.MIN_HEIGHT) {
            set.add(new ModeItem(mon[0], mon[1], 0, freq));
        }

        ModeItem[] items = set.toArray(new ModeItem[set.size()]);
        Arrays.sort(items);
        _modes.setItems(items);
        // select the entry matching the current size (the chooser keys on width/height).
        ModeItem sel = _mode;
        for (ModeItem mi : items) {
            if (mi.width == _mode.width && mi.height == _mode.height) {
                sel = mi;
                break;
            }
        }
        _modes.selectItem(sel);
    }

    protected void updateDisplayMode (ModeItem mode, boolean confirm)
    {
        boolean wantFullscreen = _fullscreen.isSelected();
        // compare against the currently-applied state (_wasFullscreen), not the persisted pref, so
        // a fullscreen-only toggle (or its revert) is not swallowed when the size is unchanged.
        if (mode == null || (_mode != null && _mode.equals(mode) &&
                             wantFullscreen == _wasFullscreen)) {
            return;
        }

        final ModeItem omode = _mode;
        final boolean owasFullscreen = _wasFullscreen;
        _mode = mode;
        _wasFullscreen = wantFullscreen;
        log.info("Switching to " + _mode + (wantFullscreen ? " fullscreen" : " windowed") +
                 " (from " + omode + (owasFullscreen ? " fullscreen" : " windowed") + ")");

        // we fake up non-full screen display modes above, but there's no way to set the bit depth
        // to anything but zero, so adjust that here so jME3 doesn't freak out.
        int bpp = Math.max(16, _mode.bpp);
        int width = _mode.width, height = _mode.height;
        // a fullscreen switch targets the monitor's native resolution if known (the LWJGL3 context
        // creates the window against the primary monitor); otherwise use the chosen windowed size.
        if (wantFullscreen) {
            int[] mon = _ctx.getApp().getPrimaryMonitorMode();
            if (mon != null) {
                width = mon[0];
                height = mon[1];
            }
        }

        // recreate the LWJGL3 window at the new mode (jME3 AppSettings restart; the fork called
        // _ctx.getDisplay().recreateWindow(...)). The GLFW framebuffer-resize callback re-fits the
        // viewport cameras via RenderManager.notifyReshape; we re-fit the BUI views below.
        _ctx.getApp().switchDisplayMode(width, height, bpp, _mode.freq, wantFullscreen);

        // reconfigure the camera frustum in case the aspect ratio changed (the context restart
        // also does this via notifyReshape, but the popup relayout below reads it immediately).
        _ctx.getCameraHandler().getCamera().setFrustumPerspective(
            45.0f, width/(float)height,
            com.threerings.jme.JmeApp.FRUSTUM_NEAR, com.threerings.jme.JmeApp.FRUSTUM_FAR);

        // Re-fitting the main view (_parent) to the new size is now driven by the host resize
        // listener (BangClient.refitMainView), which fires once the jME3 restart() above has
        // actually recreated the GLFW window and re-fit the cameras. Doing it synchronously here --
        // as the fork did after its synchronous recreateWindow -- would read the still-old camera
        // size, because jME3's restart() is asynchronous (posted to the next render frame). This
        // popup re-centers itself off the same host resize event (see _resizer), which the host
        // fires after the backend display size is updated, so center() reads the new size.
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
                    BangPrefs.updateDisplayMode(_mode.width, _mode.height, _mode.bpp,
                        _mode.freq);
                    BangPrefs.updateFullscreen(_wasFullscreen);
                    break;

                default:
                    // revert to the old mode (and restore the fullscreen checkbox state)
                    _fullscreen.setSelected(owasFullscreen);
                    updateDisplayMode(omode, false);
                    break;
                }
            }
        };
        OptionDialog.showConfirmDialog(
            _ctx, BangCodes.OPTS_MSGS, "m.keep_mode", rr);

    }

    /**
     * A host-neutral display-mode descriptor. jME3 cutover: replaces the LWJGL2
     * {@code org.lwjgl.opengl.DisplayMode} this view used to wrap; the Phase-3 LWJGL3 host
     * supplies width/height/bpp/freq.
     */
    protected static class ModeItem implements Comparable<ModeItem>
    {
        public int width, height, bpp, freq;

        public ModeItem (int width, int height, int bpp, int freq) {
            this.width = width;
            this.height = height;
            this.bpp = bpp;
            this.freq = freq;
        }

        public String toString () {
            String text = width + "x" + height;
            if (bpp > 0) {
                text += ("x" + bpp + " " + freq + "Hz");
            }
            return text;
        }

        @Override public boolean equals (Object o) {
            if (!(o instanceof ModeItem)) {
                return false;
            }
            ModeItem m = (ModeItem)o;
            return width == m.width && height == m.height && bpp == m.bpp && freq == m.freq;
        }

        @Override public int hashCode () {
            return (width * 31 + height) * 31 + bpp * 31 + freq;
        }

        public int compareTo (ModeItem other) {
            if (width != other.width) {
                return width - other.width;
            } else if (height != other.height) {
                return height - other.height;
            } else if (bpp != other.bpp) {
                return bpp - other.bpp;
            } else {
                return freq - other.freq;
            }
        }
    }

    protected ActionListener _modelist = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            updateDisplayMode((ModeItem)_modes.getSelectedItem(), true);
        }
    };

    /** Re-centers this popup once the host display has actually resized (after the asynchronous
     * jME3 restart()), so it lands centered in the new resolution rather than the old one. */
    protected JmeApp.ResizeListener _resizer = new JmeApp.ResizeListener() {
        public void displayResized (int width, int height) {
            if (isAdded()) {
                center();
            }
        }
    };

    protected BangContext _ctx;
    protected BWindow _parent;
    protected MessageBundle _msgs;
    protected ModeItem _mode;

    protected BComboBox _modes;
    protected BCheckBox _fullscreen;

    /** The fullscreen state currently applied to the live context (distinct from the persisted
     * preference): tracks in-place switches so reverting a confirm dialog restores it. */
    protected boolean _wasFullscreen;

    protected BList _muted;
    protected BButton _remove;
}
