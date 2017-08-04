//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JFrame;

import com.threerings.parlor.util.ParlorContext;

import com.threerings.bang.util.BasicContext;

/**
 * Defines services needed by the editor client.
 */
public interface EditorContext extends BasicContext, ParlorContext
{
    /** Updates the main window title. */
    public void setWindowTitle (String title);

    /** Displays a status message. */
    public void displayStatus (String status);

    /** Displays a status message with the option to paint it immediately. */
    public void displayStatus (String status, boolean paint);

    /** Displays a set of coordinates. */
    public void displayCoords (int x, int y);

    /** Provides access to the main editor window. */
    public JFrame getFrame ();
}
