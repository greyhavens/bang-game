//
// $Id$

package com.threerings.bang.client.bui;

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
            final ImageIcon alert = new ImageIcon(
                    _ctx.getImageCache().getBImage("ui/icons/alert.png"));
            final BlankIcon blank = new BlankIcon(
                alert.getWidth(), alert.getHeight());
            setIcon(alert);
            Interval flashAlert = new Interval(_ctx.getApp()) {
                public void expired () {
                    _flashCount++;
                    setIcon(_flashCount % 2 == 0 ? alert : blank);
                    if (_flashCount == 5) {
                        cancel();
                    }
                }
                
                protected int _flashCount = 0;
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

    protected static final long FLASH_DELAY = 300L;
}
