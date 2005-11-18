//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.openal.SoundGroup;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;

import com.threerings.bang.util.BangContext;

/**
 * Handles the visual representation of a complex effect on the client.
 */
public abstract class EffectHandler
{
    /**
     * Initializes and fires up the handler.
     */
    public void init (BangContext ctx, BangObject bangobj, BangBoardView view,
                      SoundGroup sounds, Effect effect)
    {
        _ctx = ctx;
        _bangobj = bangobj;
        _view = view;
        _sounds = sounds;
        _effect = effect;
    }
    
    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected BangBoardView _view;
    protected SoundGroup _sounds;
    protected Effect _effect;
}
