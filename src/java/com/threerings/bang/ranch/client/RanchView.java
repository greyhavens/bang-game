//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.bui.BWindow;
import com.jme.bui.layout.BorderLayout;
import com.jme.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.util.BangContext;

/**
 * Displays the main ranch interface wherein a player's Big Shot units can
 * be inspected, new Big Shots can be browsed and purchased and normal
 * units can also be inspected.
 */
public class RanchView extends BWindow
{
    public RanchView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), new BorderLayout(5, 5));
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("ranch");

        // center panel: tabbed view with big shots, units, recruits

        // side panel: unit inspector, unit status, "customize" or
        // "recruit" and back to town button
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
}
