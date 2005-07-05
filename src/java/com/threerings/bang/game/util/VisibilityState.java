//
// $Id$

package com.threerings.bang.game.util;

import java.util.Arrays;

/**
 * Used to track the visibility state for a particular player.
 */
public class VisibilityState
{
    public VisibilityState (int width, int height)
    {
        _width = width;
        _state = _state1 = new byte[width*height];
        _ostate = _state2 = new byte[width*height];
    }

    public void swap ()
    {
        if (_state == _state1) {
            _state = _state2;
            _ostate = _state1;
        } else {
            _state = _state1;
            _ostate = _state2;
        }
        Arrays.fill(_state, (byte)0);
    }

    public void reveal ()
    {
        Arrays.fill(_state, (byte)1);
    }

    public void setVisible (int tx, int ty)
    {
        _state[ty * _width + tx] = (byte)1;
    }

    public boolean visibilityChanged (int tx, int ty)
    {
        int index = ty * _width + tx;
        return (_ostate[index] != _state[index]);
    }

    public boolean getVisible (int tx, int ty)
    {
        return getVisible(ty * _width + tx);
    }

    public boolean getVisible (int index)
    {
        return _state[index] == (byte)1;
    }

    protected int _width;
    protected byte[] _state1, _state2, _state, _ostate;
}
