//
// $Id$

package com.threerings.bang.client;

/**
 * Provides a mechanism for the game client to play nicely with whatever view is currently being
 * displayed.
 */
public interface MainView
{
    /** Defines the different types of popups. */
    public static enum Type {
        CHAT, NOTIFICATION, POSTER_DISPLAY, STATUS, FKEY, SYSTEM, DETAIL_SUGGESTION
    };

    /**
     * Returns true if it's OK to pop up a window over this view.
     */
    public boolean allowsPopup (Type type);
}
