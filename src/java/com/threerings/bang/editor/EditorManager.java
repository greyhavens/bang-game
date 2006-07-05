//
// $Id$

package com.threerings.bang.editor;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.bang.game.data.BangObject;

/**
 * Handles the server side of the "editor" mode of the game.
 */
public class EditorManager extends GameManager
{
    // documentation inherited
    protected Class<? extends PlaceObject> getPlaceObjectClass ()
    {
        return BangObject.class;
    }
}
