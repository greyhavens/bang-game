//
// $Id$

package com.threerings.bang.editor;

import javax.swing.JFrame;

import com.threerings.bang.util.BangContext;

/**
 * Extends the Bang client context with editor specific bits.
 */
public abstract class EditorContext extends BangContext
{
    /** Updates the main window title. */
    public abstract void setWindowTitle (String title);

    /** Displays a status message. */
    public abstract void displayStatus (String status);

    /** Provides access to the main editor window. */
    public abstract JFrame getFrame ();
}
