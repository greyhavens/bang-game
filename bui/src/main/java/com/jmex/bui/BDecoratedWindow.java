//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import com.jmex.bui.layout.GroupLayout;

/**
 * A top-level window with a border, a background and a title bar. Note that a
 * decorated window uses a stretching {@link GroupLayout} and adds a label at
 * the top in the <code>window_title</code> style if a title was specified.
 */
public class BDecoratedWindow extends BWindow
{
    /**
     * Creates a decorated window using the supplied look and feel.
     *
     * @param title the title of the window or null if no title bar is
     * desired.
     */
    public BDecoratedWindow (BStyleSheet style, String title)
    {
        super(style, GroupLayout.makeVStretch());
        ((GroupLayout)getLayoutManager()).setOffAxisPolicy(
            GroupLayout.CONSTRAIN);

        if (title != null) {
            setTitle(title);
        }
    }

    /**
     * Configures the title for this window.
     */
    public void setTitle (String title)
    {
        if (_title == null) {
            add(_title = new BLabel(title, "window_title"), GroupLayout.FIXED);
        } else {
            _title.setText(title);
        }
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "decoratedwindow";
    }

    protected BLabel _title;
}
