//
// $Id$

package com.threerings.bang.client.bui;

import com.jme.renderer.Renderer;

import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.icon.ImageIcon;

import com.samskivert.util.Interval;

import com.threerings.bang.util.BangContext;
import com.jmex.bui.icon.BlankIcon;

/**
 * Provides a convenient component for displaying feedback.
 */
public class StatusLabel extends BLabel
{
    public StatusLabel (BangContext ctx)
    {
        super("");
        _ctx = ctx;
        setStyleClass("status_label");
    }

    /**
     * Displays an <em>already translated</em> status message.
     *
     * @param flash if true, an icon will be flashed three times next to the
     * status message to grab the users attention.
     */
    public void setStatus (String message, boolean flash)
    {
        setText(message);
        if (flash) {
            _flashCount = 0;
            final ImageIcon alert = new ImageIcon(
                    _ctx.getImageCache().getBImage("ui/icons/alert.png"));
            final BlankIcon blank = new BlankIcon(
                alert.getWidth(), alert.getHeight());
            setIcon(alert);
            Interval flashAlert = new Interval(_ctx.getApp()) {
                public void expired () {
                    _flashCount++;
                    if (_flashCount >= 5) {
                        cancel();
                        _flashCount = 5;
                    }
                    setIcon(_flashCount % 2 == 0 ? alert : blank);
                }
            };
            flashAlert.schedule(FLASH_DELAY, true);
        }
    }

    /**
     * Translates and displays the specified status message.
     *
     * @param flash if true, an icon will be flashed three times next to the
     * status message to grab the users attention.
     */
    public void setStatus (String bundle, String message, boolean flash)
    {
        setStatus(_ctx.xlate(bundle, message), flash);
    }

    protected BangContext _ctx;

    /** Number of flashes (x2 flash on & flash off) */
    protected int _flashCount;

    protected static final long FLASH_DELAY = 300L;
}
