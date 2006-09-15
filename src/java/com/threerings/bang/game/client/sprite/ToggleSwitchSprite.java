//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.scene.Spatial;
import com.jme.scene.state.TextureState;

import com.threerings.bang.game.client.BoardView;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.ToggleSwitch;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.jme.model.ModelSpatial;
import com.threerings.jme.model.TextureProvider;

import com.threerings.openal.SoundGroup;

import static com.threerings.bang.Log.log;

/**
 * Displays a toggle switch for the Wendigo Attack scenario.
 */
public class ToggleSwitchSprite extends PropSprite
{
    public ToggleSwitchSprite ()
    {
        super("indian_post/special/wendigo_toggle_switch");
    }

    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, BangBoard board,
            SoundGroup sounds, Piece piece, short tick)
    {
        super.init(ctx, view, board, sounds, piece, tick);
        _ts = (ToggleSwitch)piece;
    }

    @Override // documentation inherited
    public String getHelpIdent (int pidx)
    {
        return "indian_post/special/wendigo_toggle_switch";
    }

    @Override // documentation inherited
    public boolean hasTooltip ()
    {
        return true;
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        if (_ts.isActive(tick)) {
            _state = (_ts.state == ToggleSwitch.State.SQUARE ?
                    State.SQUARE : State.CIRCLE);
        } else if (_ts.activator != -1) {
            _state = State.TICK_4;
        } else {
            _state = State.values()[_ts.ticksUntilMovable(tick)];
        }
        _model.resolveTextures(new ToggleTextureProvider());
        _model.updateRenderState();
    }

    protected class ToggleTextureProvider
        implements TextureProvider
    {
        public TextureState getTexture (String name)
        {
            return _tstates[_state.ordinal()];
        }
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();
        if (_tstates[0] == null) {
            for (State state : State.values()) {
                _tstates[state.ordinal()] = RenderUtil.createTextureState(
                        _ctx, state.getTextureName());
            }
        }
    }

    protected ToggleSwitch _ts;

    protected static enum State
    {
        TICK_0("four"), TICK_1("three"), TICK_2("two"), TICK_3("one"), 
        TICK_4("zero"), SQUARE("square"), CIRCLE("circle");

        State (String name)
        {
            _name = name;
        }

        public String getTextureName ()
        {
            return "props/indian_post/special/wendigo_toggle_switch/" +
                "toggle_switch_" + _name + ".png";
        }

        protected String _name;
    };
    protected State _state;

    protected static TextureState[] _tstates = 
        new TextureState[State.values().length];
}
