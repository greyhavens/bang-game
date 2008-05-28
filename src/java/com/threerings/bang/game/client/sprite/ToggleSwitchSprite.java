//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.ToggleSwitch;

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
            action = _state.getStartAnimation();
            if (action != null) {
                queueAction(action);
            }
        }
    }

    @Override // documentation inherited
    protected String getHelpIdent (int pidx)
    {
        return "indian_post/special/wendigo_toggle_switch";
    }

    @Override // documentation inherited
    protected void startNextIdle (boolean offset)
    {
        queueAction(_state.getIdleAnimation());
    }

    protected static enum State
    {
        TICK_0("charge_4", true, false), TICK_1("charge_3", true, false),
        TICK_2("charge_2", true, false), TICK_3("charge_1", true, false),
        TICK_4("charge_0", false, false),
        SQUARE("square", true, true), CIRCLE("circle", true, true);

        State (String name, boolean onAnim, boolean offAnim)
        {
            _name = name;
            _onAnim = onAnim;
            _offAnim = offAnim;
        }

        public String getStartAnimation ()
        {
            return (_onAnim ? _name + (_offAnim ? "_active_start" : "_start") :
                    null);
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
        protected boolean _offAnim, _onAnim;
    };

    protected State _state = State.SQUARE;
}
