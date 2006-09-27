//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.ToggleSwitch;

import static com.threerings.bang.Log.log;

/**
 * Displays a toggle switch for the Wendigo Attack scenario.
 */
public class ToggleSwitchSprite extends ActiveSprite
{
    public ToggleSwitchSprite ()
    {
        super("props", "indian_post/special/wendigo_toggle_switch");
    }

    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        return "indian_post/special/wendigo_toggle_switch";
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        ToggleSwitch ts = (ToggleSwitch)_piece;
        State state;
        if (ts.isActive(tick)) {
            state = (ts.state == ToggleSwitch.State.SQUARE ?
                    State.SQUARE : State.CIRCLE);
        } else if (ts.activator != -1) {
            state = State.TICK_4;
        } else {
            state = State.values()[ts.ticksUntilMovable(tick)];
        }
        if (state != _state) {
            String action = _state.getEndAnimation();
            if (action != null) {
                queueAction(action);
            }
            _state = state;
            queueAction(_state.getStartAnimation());
        }
    }

    @Override // documentation inherited
    protected void startNextIdle (boolean offset)
    {
        queueAction(_state.getIdleAnimation());
    }

    protected static enum State
    {
        TICK_0("charge_4", false), TICK_1("charge_3", false), 
        TICK_2("charge_2", false), TICK_3("charge_1", false), 
        TICK_4("charge_0", false), 
        SQUARE("square", true), CIRCLE("circle", true);

        State (String name, boolean offAnim)
        {
            _name = name;
            _offAnim = offAnim;
        }
        
        public String getStartAnimation ()
        {
            return _name + (_offAnim ? "_active_start" : "_start");
        }

        public String getIdleAnimation ()
        {
            return _name + (_offAnim ? "_active_loop" : "_loop");
        }

        public String getEndAnimation ()
        {
            return (_offAnim ? _name + "_discharge" : null);
        }

        protected String _name;
        protected boolean _offAnim;
    };

    protected State _state = State.SQUARE;
}
