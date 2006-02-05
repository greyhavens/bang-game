//
// $Id$

package com.threerings.bang.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.RunAnywhere;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.util.EscapeListener;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Allows options to be viewed and adjusted. Presently that's just video
 * mode and whether or not we're in full screen mode.
 */
public class OptionsView extends BDecoratedWindow
    implements ActionListener
{
    public OptionsView (BangContext ctx, LogonView parent)
    {
        super(ctx.getStyleSheet(), null);
        setLayoutManager(GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                              GroupLayout.STRETCH));
        setModal(true);

        _ctx = ctx;
        _parent = parent;
        _msgs = ctx.getMessageManager().getBundle("options");

        addListener(new EscapeListener() {
            public void escapePressed() {
                dismiss();
            }
        });

        BContainer cont = GroupLayout.makeHBox(GroupLayout.CENTER);
        cont.add(new BLabel(_msgs.get("m.title"), "scroll_title"));
        add(cont);

        cont = GroupLayout.makeHBox(GroupLayout.LEFT);
        cont.add(new BLabel(_msgs.get("m.video_mode")));
        cont.add(_modes = new BComboBox());
        add(cont);

        cont = GroupLayout.makeHBox(GroupLayout.LEFT);
        cont.add(_fullscreen = new BCheckBox(_msgs.get("m.fullscreen_mode")));
        _fullscreen.setSelected(Display.isFullscreen());
        _fullscreen.addListener(_modelist);
        add(cont);

        cont = GroupLayout.makeHBox(GroupLayout.RIGHT);
        BButton btn;
        cont.add(btn = new BButton(_msgs.get("m.dismiss"), "dismiss"));
        btn.addListener(this);
        add(cont);

        _mode = Display.getDisplayMode();
        refreshDisplayModes();
        _modes.addListener(_modelist);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("dismiss".equals(event.getAction())) {
            dismiss();
        }
    }

    @Override // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        Dimension d = super.computePreferredSize(whint, hhint);
        d.width = Math.max(d.width, 400);
        return d;
    }

    protected void refreshDisplayModes ()
    {
        try {
            ArrayList<DisplayMode> modes = new ArrayList<DisplayMode>();
            CollectionUtil.addAll(modes, Display.getAvailableDisplayModes());
            for (Iterator<DisplayMode> iter = modes.iterator();
                 iter.hasNext(); ) {
                DisplayMode mode = iter.next();
                // our minimum display size is 1024x768
                if (mode.getWidth() < 1024 || mode.getHeight() < 768) {
                    iter.remove();
                }
            }

            // if there is only one display mode, and we're on Linux, it's
            // probably because of Xinerama wackiness, so slip in a couple
            // of sensible modes for use in non-fullscreen mode
            if (RunAnywhere.isLinux() && modes.size() < 2) {
                modes.add(new DisplayMode(1024, 768));
                modes.add(new DisplayMode(1280, 1024));
            }

            ModeItem current = null;
            ModeItem[] items = new ModeItem[modes.size()];
            for (int ii = 0; ii < items.length; ii++) {
                DisplayMode mode = (DisplayMode)modes.get(ii);
                items[ii] = new ModeItem(mode);
                if (isCurrent(mode)) {
                    current = items[ii];
                }
            }
            Arrays.sort(items);
            _modes.setItems(items);
            _modes.selectItem(current);

        } catch (LWJGLException e) {
            log.log(Level.WARNING, "Failed to obtain display modes", e);
        }
    }

    protected void updateDisplayMode (DisplayMode mode)
    {
        if (_mode != null && _mode.equals(mode) &&
            Display.isFullscreen() == _fullscreen.isSelected()) {
            return;
        }
        if (mode != null) {
            log.info("Switching to " + mode + " (from " + _mode + ")");
            _mode = mode;
        }

        // we fake up non-full screen display modes above, but there's no
        // way to set the bit depth to anything but zero, so we have to
        // adjust that here so that JME doesn't freak out
        int bpp = Math.max(16, _mode.getBitsPerPixel());
        int width = _mode.getWidth(), height = _mode.getHeight();
        _ctx.getDisplay().recreateWindow(
            width, height, bpp, _mode.getFrequency(), _fullscreen.isSelected());

        // reconfigure the camera frustum in case the aspect ratio changed
        _ctx.getCameraHandler().getCamera().setFrustumPerspective(
            45.0f, width/(float)height, 1, 10000);

        // recenter the logon view and options window
        _parent.center();
        center();

        // store these settings for later
        BangPrefs.updateDisplayMode(_mode);
        BangPrefs.updateFullscreen(_fullscreen.isSelected());
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

    protected static class ModeItem implements Comparable
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

        public int compareTo (Object other) {
            DisplayMode omode = ((ModeItem)other).mode;
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
            updateDisplayMode(((ModeItem)_modes.getSelectedItem()).mode);
        }
    };

    protected BangContext _ctx;
    protected LogonView _parent;
    protected MessageBundle _msgs;
    protected DisplayMode _mode;

    protected BComboBox _modes;
    protected BCheckBox _fullscreen;
}
