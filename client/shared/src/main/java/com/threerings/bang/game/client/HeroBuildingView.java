//
// $Id$

package com.threerings.bang.game.client;

import com.jme.renderer.Renderer;
import com.jme.system.DisplaySystem;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.client.BangUI;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.ranch.client.UnitBonus;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.effect.CountEffect;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays hero bonuses during a hero building game.
 */
public class HeroBuildingView extends ScenarioHUD
{
    public HeroBuildingView (BangContext ctx, BangObject bangobj)
    {
        super(BangUI.stylesheet, GroupLayout.makeHoriz(GroupLayout.CENTER));
        setStyleClass("bounty_req_window");
        _ctx = ctx;
        _bangobj = bangobj;
        _pidx = bangobj.getPlayerIndex(_ctx.getUserObject().getVisibleName());

        _color = colorLookup[_pidx + 1];

        add(new Spacer(15, 1));
        add(new BLabel(_ctx.xlate(GameCodes.GAME_MSGS, "m.hero_level"), "hero_level_label"));
        add(_level = new BLabel("0", "hero_level" + _color));
        add(new Spacer(15, 1));
        add(new BLabel(UnitBonus.getBonusIcon(UnitBonus.BonusIcons.ATTACK, _ctx, false)));
        add(_attack = new BLabel("-", "unit_status_health" + _color));
        add(new BLabel(UnitBonus.getBonusIcon(UnitBonus.BonusIcons.DEFEND, _ctx, false)));
        add(_defense = new BLabel("-", "unit_status_health" + _color));
        add(_icons = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER)));
        add(new Spacer(15, 1));
        if (_ramblin == null) {
            _ramblin = new BLabel(new ImageIcon(_ctx.loadImage("influences/ramblin.png")));
            _spirit = new BLabel(new ImageIcon(_ctx.loadImage("influences/spirit_walk.png")));
            _hustle = new BLabel(new ImageIcon(_ctx.loadImage("influences/hustle.png")));
            _eagle = new BLabel(new ImageIcon(_ctx.loadImage("influences/eagle_eye.png")));
        }
    }

    // documentation inherited
    public void pieceWasAffected (Piece piece, String effect)
    {
        if (!CountEffect.COUNT_CHANGED.equals(effect) || piece.owner != _pidx) {
            return;
        }
        int level = ((Counter)piece).count;
        _level.setText("" + level);
        if (level == 0) {
            _attack.setText("-");
            _defense.setText("-");
        } else {
            _attack.setText("+" + (level * 3));
            _defense.setText("+" + (level * 3));
        }

        _icons.removeAll();
        if (level < 4) {
            return;
        }
        _icons.add(new Spacer(15, 1));
        _icons.add(_ramblin);
        int move = level / 4;
        if (move > 1) {
            _icons.add(new BLabel("x" + move, "unit_status_health" + _color));
        }
        if (level > 4) {
            _icons.add(_spirit);
        }
        if (level > 5) {
            _icons.add(_hustle);
        }
        if (level > 6) {
            _icons.add(_eagle);
        }

        if (isAdded()) {
            pack();
            DisplaySystem ds = DisplaySystem.getDisplaySystem();
            setLocation((ds.getWidth() - getWidth())/2, getY());
        }
    }

    @Override // from BWindow
    protected void renderBackground (Renderer renderer)
    {
        // render our background at 50% transparency
        getBackground().render(renderer, 0, 0, _width, _height, 0.5f);
    }

    @Override // from BComponent
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _icons.removeAll();
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected int _pidx, _color;
    protected BLabel _level, _attack, _defense;
    protected BContainer _icons;
    protected static BLabel _ramblin, _spirit, _hustle, _eagle;
}
