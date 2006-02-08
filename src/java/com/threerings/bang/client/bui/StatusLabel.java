//
// $Id$

package com.threerings.bang.client.bui;

import com.jmex.bui.BLabel;

import com.threerings.bang.util.BangContext;

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
            // TODO: flash our icon
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
}
