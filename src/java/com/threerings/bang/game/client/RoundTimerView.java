//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.layout.BorderLayout;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.util.BangContext;

/**
 * Displays the round countdown timer.
 */
public class RoundTimerView extends BWindow
    implements AttributeChangeListener
{
    public RoundTimerView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new BorderLayout());
        _ctx = ctx;
        add(_clock = new BLabel("", "round_timer"), BorderLayout.CENTER);
    }

    public void init (BangObject bangobj)
    {
        _bangobj = bangobj;
        _bangobj.addListener(this);
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        String name = event.getName();
        if (name.equals(BangObject.STATE)) {
            if (_bangobj.state == BangObject.IN_PLAY) {
                _clock.setText("" + (_bangobj.lastTick - _bangobj.tick));
            } else {
                _clock.setText("");
            }

        } else if (name.equals(BangObject.LAST_TICK)) {
            _clock.setText("" + (_bangobj.lastTick - _bangobj.tick));

        } else if (name.equals(BangObject.TICK)) {
            _clock.setText("" + (_bangobj.lastTick - _bangobj.tick));
        }
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected BLabel _clock;
}
