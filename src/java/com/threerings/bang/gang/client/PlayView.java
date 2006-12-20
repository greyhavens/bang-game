//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.HideoutObject;

/**
 * Allows the user to play a game from the hideout.
 */
public class PlayView extends BContainer
{
    public PlayView (BangContext ctx, HideoutObject hideoutobj)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        
        setStyleClass("play_view");
    }
    
    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
}
