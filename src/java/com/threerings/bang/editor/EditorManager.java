//
// $Id$

package com.threerings.bang.editor;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.parlor.game.data.GameObject;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.bang.game.data.BangObject;

/**
 * Handles the server side of the "editor" mode of the game.
 */
public class EditorManager extends GameManager
{
    @Override // documentation inherited
    public void playerReady (BodyObject caller)
    {
        // caller is null because the message originated on the server;
        // start things up directly
        gameWillStart();
        _gameobj.setState(GameObject.IN_PLAY);
    }
    
    // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new BangObject();
    }
}
