//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.Collections;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BComponent;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.Spacer;
import com.jmex.bui.background.BBackground;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.bang.game.client.sprite.PieceStatus;
import com.threerings.bang.game.client.sprite.UnitSprite;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Hindrance;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.ranch.data.RanchCodes;
import com.threerings.bang.ranch.client.UnitBonus;

import com.threerings.util.MessageBundle;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the status of the various units in iconic form.
 */
public class UnitStatusView extends BWindow
{
    public UnitStatusView (
        BangContext ctx, BangController ctrl, BangBoardView view, BangObject bangobj)
    {
        super(ctx.getStyleSheet(), new AbsoluteLayout(true));

        _ctx = ctx;
        _ctrl = ctrl;
        _view = view;
        _bangobj = bangobj;
        _pidx = bangobj.getPlayerIndex(_ctx.getUserObject().getVisibleName());
    }

    /**
     * Repacks this window and positions it in the upper right corner.
     */
    public void reposition ()
    {
        if (isAdded()) {
            pack();
            setLocation(0, _ctx.getDisplay().getHeight()- getHeight());
        }
    }

    /**
     * Called when a unit sprite is added to the game.
     */
    public void unitAdded (UnitSprite usprite)
    {
        if (usprite.getPiece().owner != _pidx) {
            return;
        }

        int pieceId = usprite.getPieceId();
        UnitStatus ustatus = null;
        for (UnitStatus status : _ustatuses) {
            if (status.pieceId == pieceId) {
                ustatus = status;
                break;
            }
        }
        if (ustatus == null) {
            _ustatuses.add(ustatus = new UnitStatus());
        }
        ustatus.setUnitSprite(usprite);
        resort();
        reposition();
    }

    /**
     * Called when a unit sprite is removed from the game.
     */
    public void unitRemoved (UnitSprite usprite)
    {
        int pieceId = usprite.getPieceId();
        UnitStatus ustatus = null;
        for (UnitStatus status : _ustatuses) {
            if (status.pieceId == pieceId) {
                ustatus = status;
                break;
            }
        }
        if (ustatus == null) {
            return;
        }

        // if this unit was hijacked from another player, or a duplicate, or there are no unit
        // respawns, remove this label
        if (((Unit)usprite.getPiece()).originalOwner != _pidx ||
                !((BangConfig)_ctrl.getPlaceConfig()).respawnUnits) {
            _ustatuses.remove(ustatus);
            resort();
            reposition();
        }
    }

    /**
     * Called when an order is reported as invalide by the server.
     */
    public void orderInvalidated (int unitId, int targetId)
    {
        for (UnitStatus status : _ustatuses) {
            if (status.pieceId == unitId) {
                status.label.orderInvalidated();
                break;
            }
        }
    }

    @Override // documentation inherited
    public BComponent getHitComponent (int mx, int my) {
        BComponent hit = super.getHitComponent(mx, my);
        // if we didn't find a hit and we're outside our normal width
        // then let it pass through
        if (hit == this && mx > _selected.getWidth()) {
            return null;
        }
        return hit;
    }

    protected void resort ()
    {
        Collections.sort(_ustatuses);
        removeAll();
        int yoff = 0;
        for (UnitStatus status : _ustatuses) {
            add(status, new Point(0, yoff));
            yoff += 90;
        }
    }

    protected class UnitStatus extends BContainer
        implements UnitSprite.UpdateObserver, Comparable<UnitStatus>
    {
        public int pieceId;

        public UnitLabel label;

        public UnitStatus () {
            super(new AbsoluteLayout(true));
            if (_selected == null) {
                _selected = _ctx.getImageCache().getBImage(
                        "ui/ustatus/unitstatus_select.png");
            }
            if (_closeArrow == null) {
                _closeArrow = new BImage[2];
                _closeArrow[0] = _ctx.getImageCache().getBImage(
                        "ui/ustatus/arrow_left_tiny_normal.png");
                _closeArrow[1] = _ctx.getImageCache().getBImage(
                        "ui/ustatus/arrow_left_tiny_hover.png");
            }
            _closer = _closeArrow[0];
            add(label = new UnitLabel(), new Point(15, 13));
            GroupLayout vlay = GroupLayout.makeVert(
                    GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
            vlay.setGap(4);
            _opened = new BContainer(vlay) {
                public BComponent getHitComponent (int mx, int my) {
                    if ((mx >= _x + 38) && (my >= _y) &&
                            (mx < _x + _width) && (my < _y + _height)) {
                        return this;
                    }
                    return null;
                }
            };
            _opened.setStyleClass("unit_status_container");
            _opened.addListener(new MouseAdapter() {
                public void mouseMoved (MouseEvent event) {
                    _closer = _closeArrow[1];
                }
                public void mouseExited (MouseEvent event) {
                    _closer = _closeArrow[0];
                }
                public void mousePressed (MouseEvent event) {
                    toggleDetails(false);
                    forceUpdate();
                }
            });
            _closed = new BButton(new BlankIcon(39, 32), "") {
                protected void fireAction (long when, int modifiers) {
                    toggleDetails(true);
                    forceUpdate();
                }
            };
            _closed.setStyleClass("unit_status_closed");
        }

        public BComponent getHitComponent (int mx, int my) {
            BComponent hit = super.getHitComponent(mx, my);
            // if we didn't find a hit and we're outside our normal width
            // then let it pass through
            if (hit == this && (mx > _selected.getWidth() ||
                        getHeight() - my > _selected.getHeight())) {
                return null;
            }
            return hit;
        }

        public void setUnitSprite (UnitSprite sprite) {
            // clear out our old sprite
            clearSprite();

            _sprite = sprite;
            _sprite.addObserver(this);
            pieceId = _sprite.getPieceId();
            label.setUnitSprite(sprite);
            Unit unit = getUnit();
            _health = new BLabel("", "unit_status_health" + colorLookup[unit.owner + 1]);
            BContainer healthcont = new BContainer(GroupLayout.makeHStretch());
            healthcont.add(new Spacer(0, 0));
            healthcont.add(_health, GroupLayout.FIXED);
            healthcont.add(new Spacer(0, 0));
            add(healthcont, new Rectangle(28, 78, 45, 18));
            _holdingL = new BLabel("", "unit_status_holding");
            _influenceL = new BLabel("", "unit_status_influence");
            _hindranceL = new BLabel("", "unit_status_hindrance");

            _opened.removeAll();

            UnitConfig uc = unit.getConfig();
            MessageBundle msgs =
                _ctx.getMessageManager().getBundle(RanchCodes.RANCH_MSGS);
            MessageBundle umsgs =
                _ctx.getMessageManager().getBundle(BangCodes.UNITS_MSGS);
            _opened.add(new BLabel(
                        umsgs.xlate(uc.getName()), "unit_status_name"));
            _opened.add(new Spacer(118, 2));

            // the unit stats
            TableLayout tlay = new TableLayout(6, 2, 4);
            tlay.setHorizontalAlignment(TableLayout.STRETCH);
            BContainer stats = new BContainer(tlay);
            stats.add(new Spacer(2, 0));
            stats.add(new BLabel(msgs.get("m.make"), "unit_status_details"));
            stats.add(new BLabel(
                        UnitBonus.getBonusIcon(uc.make, _ctx, true),
                        "unit_status_number"));
            stats.add(new Spacer(4, 0));
            stats.add(new BLabel(msgs.get("m.move"), "unit_status_details"));
            stats.add(new BLabel("" + uc.moveDistance, "unit_status_number"));

            stats.add(new Spacer(2, 0));
            stats.add(new BLabel(msgs.get("m.mode"), "unit_status_details"));
            stats.add(new BLabel(
                        UnitBonus.getBonusIcon(uc.mode, _ctx, true),
                        "unit_status_number"));
            stats.add(new Spacer(4, 0));
            stats.add(new BLabel(msgs.get("m.shoot"), "unit_status_details"));
            stats.add(new BLabel(
                        uc.getDisplayFireDistance(), "unit_status_number"));
            _opened.add(stats);

            // the unit attack/defend bonus/penalty
            tlay = new TableLayout(2, 2, 4);
            tlay.setVerticalAlignment(TableLayout.CENTER);
            BContainer bonuses = new BContainer(tlay);
            BLabel bl = new BLabel(":", "unit_status_details");
            bl.setIcon(UnitBonus.getBonusIcon(
                        UnitBonus.BonusIcons.ATTACK, _ctx, true));
            bonuses.add(bl);
            UnitBonus ub = new UnitBonus(_ctx, 8, true);
            ub.setUnitConfig(uc, false, UnitBonus.Which.ATTACK);
            bonuses.add(ub);
            bl = new BLabel(":", "unit_status_details");
            bl.setIcon(UnitBonus.getBonusIcon(
                        UnitBonus.BonusIcons.DEFEND, _ctx, true));
            bonuses.add(bl);
            ub = new UnitBonus(_ctx, 8, true);
            ub.setUnitConfig(uc, false, UnitBonus.Which.DEFEND);
            bonuses.add(ub);
            _opened.add(bonuses);
            toggleDetails(BangPrefs.getUnitStatusDetails());
        }

        public Unit getUnit () {
            return (Unit)_sprite.getPiece();
        }

        @Override // documentation inherited
        public void renderComponent (Renderer renderer)
        {
            if (_sprite.isSelected()) {
                if (_opened.getParent() == this) {
                    _opened.render(renderer);
                    _closer.render(renderer,
                            getWidth() - _closer.getWidth() - 4, 2, 1f);
                } else if (_closed.getParent() == this) {
                    _closed.render(renderer);
                }
                _selected.render(renderer, 0,
                        getHeight() - _selected.getHeight(), 1f);
            }
            for (int ii = 0, ll = getComponentCount(); ii < ll; ii++) {
                BComponent comp = getComponent(ii);
                if (comp != _opened && comp != _closed) {
                    comp.render(renderer);
                }
            }
        }

        // documentation inherited from interface UnitSprite.UpdateObserver
        public void updated (UnitSprite sprite) {
            Unit unit = getUnit();
            if (!unit.isAlive()) {
                // if this unit is not originally ours, remove ourselves
                if (unit.originalOwner != _pidx) {
                    UnitStatusView.this.remove(this);
                    return;
                }
            }
            label.updated(sprite);
            if (!sprite.isSelected()) {
                remove(_closed);
                remove(_opened);
                _health.setText("");
                forceUpdate();
                return;
            }
            _health.setText("" + (100 - unit.damage) + "%");
            toggleDetails(BangPrefs.getUnitStatusDetails());
            updateHolding(unit);
            updateInfluence(unit);
            updateHindrance(unit);
            forceUpdate();
        }

        protected void forceUpdate ()
        {
            validate();
            reposition();
        }

        protected void updateHolding (Unit unit) {
            if (unit.holding == _holdingU) {
                return;
            }
            _holdingU = unit.holding;
            if (_holdingL.getParent() != null) {
                _opened.remove(_holdingL);
            }
            if (_holdingU != null) {
                String name = _holdingU;
                _holdingL.setText(_ctx.xlate(
                    GameCodes.GAME_MSGS, "m.help_bonus_" +
                    name.substring(name.lastIndexOf("/")+1) +
                    "_title"));
                _holdingL.setIcon(new ImageIcon(_ctx.getImageCache().getBImage(
                                "bonuses/" + name + "/holding.png")));
                _opened.add(_holdingL);
            }
        }

        protected void updateInfluence (Unit unit) {
            if (unit.getMainInfluence() == _influenceU) {
                return;
            }
            _influenceU = unit.getMainInfluence();
            if (_influenceL.getParent() != null) {
                _opened.remove(_influenceL);
            }
            if (_influenceU != null) {
                String name = _influenceU.getName();
                _influenceL.setText(_ctx.xlate(
                    GameCodes.GAME_MSGS, "m.influence_" + name + "_title"));
                _influenceL.setIcon(new ImageIcon(label.influence));
                _opened.add(_influenceL);
            }
        }

        protected void updateHindrance (Unit unit) {
            if (unit.getHindrance() == _hindranceU) {
                return;
            }
            _hindranceU = unit.getHindrance();
            if (_hindranceU != null && !_hindranceU.isVisible()) {
                return;
            }
            if (_hindranceL.getParent() != null) {
                _opened.remove(_hindranceL);
            }
            if (_hindranceU != null) {
                String name = _hindranceU.getName();
                _hindranceL.setText(_ctx.xlate(
                    GameCodes.GAME_MSGS, "m.hindrance_" + name + "_title"));
                _hindranceL.setIcon(new ImageIcon(
                            _ctx.getImageCache().getBImage(
                        "influences/" + _hindranceU.getName() + ".png")));
                _opened.add(_hindranceL);
            }
        }

        // documentation inherited from interface Comparable
        public int compareTo (UnitStatus other) {
            Unit u1 = getUnit();
            Unit u2 = other.getUnit();
            UnitConfig uc1 = u1.getConfig(), uc2 = u2.getConfig();
            if (uc1.rank != uc2.rank) {
                return (uc1.rank == UnitConfig.Rank.BIGSHOT ? -1 :
                        (uc2.rank == UnitConfig.Rank.BIGSHOT ? 1 : -1));
            }
            return -1;
        }

        protected void clearSprite ()
        {
            if (_sprite != null) {
                _sprite.removeObserver(this);
                _sprite = null;
            }
        }

        protected void toggleDetails (boolean details)
        {
            BangPrefs.updateUnitStatusDetails(details);
            if (details) {
                if (_closed.getParent() != null) {
                    remove(_closed);
                }
                if (_opened.getParent() == null && _sprite.isSelected()) {
                    add(_opened, new Point(49, 0));
                    _closer = _closeArrow[0];
                }
            } else {
                if (_opened.getParent() != null) {
                    remove(_opened);
                }
                if (_closed.getParent() == null && _sprite.isSelected()) {
                    add(_closed, new Point(49, 65));
                }
            }
        }

        @Override // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();
            _selected.reference();
            _closeArrow[0].reference();
            _closeArrow[1].reference();
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            super.wasRemoved();
            _selected.release();
            _closeArrow[0].release();
            _closeArrow[1].release();

        }

        protected BContainer _opened;
        protected BButton _closed;
        protected UnitSprite _sprite;
        protected BLabel _health, _holdingL, _influenceL, _hindranceL;
        protected BImage _closer;
        protected String _holdingU;
        protected Influence _influenceU;
        protected Hindrance _hindranceU;
    }

    protected class UnitLabel extends BButton
    {
        protected BImage influence;

        public UnitLabel () {
            super("");
            setStyleClass("unit_status_label");
            if (_invalidated == null) {
                _invalidated = _ctx.getImageCache().getBImage(
                        "ui/ustatus/aborted.png");
            }
        }

        public void setUnitSprite (UnitSprite sprite) {
            // observer our new sprite
            _sprite = sprite;

            // setup our background
            _bground = sprite.getUnitStatus().getIconBackground();

            // set up our icon image
            Unit unit = (Unit)sprite.getPiece();
            String ipath = "units/" + unit.getType() + "/icon_head.png";
            _unit = _ctx.loadImage(ipath);
            setIcon(new ImageIcon(_unit));
            setAlpha(1f);
        }

        public Unit getUnit () {
            return (Unit)_sprite.getPiece();
        }

        public void orderInvalidated () {
            _isInvalid = true;
        }

        @Override // documentation inherited
        public Dimension computePreferredSize (int whint, int hhint)
        {
            return LABEL_PREFERRED_SIZE;
        }

        public void updated (UnitSprite sprite) {
            Unit unit = (Unit)sprite.getPiece();
            if (!unit.isAlive()) {
                // clear out any influence when the unit dies
                setInfluence(null);
                // clear out our background when our unit dies
                _bground = null;
                // and draw our icon at 50% alpha
                _unit.getBatch(0).getDefaultColor().set(ColorRGBA.white);
                setAlpha(0.5f);
                _isInvalid = false;
            } else {
                if (_sprite.isSelected()) {
                    _isInvalid = false;
                }
                if (unit.getMainInfluence() == null) {
                    setInfluence(null);
                } else {
                    // we don't worry about refetching the image from the cache
                    // if our influence didn't actually change, it's cheap
                    String path =
                        "influences/" + unit.getMainInfluence().getName() + ".png";
                    setInfluence(_ctx.getImageCache().getBImage(path));
                }

                // tint the unit icon red if it has a visible hindrance
                /*
                if (unit.getHindrance() == null || !unit.getHindrance().isVisible()) {
                    _unit.getBatch(0).getDefaultColor().set(ColorRGBA.white);
                } else {
                    _unit.getBatch(0).getDefaultColor().set(1f, 0.5f, 0.5f, 0.8f);
                }
                */
            }
        }

        @Override // documentation inherited
        public BBackground getBackground () {
            return _bground;
        }

        @Override // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();

            // reference our influence image if we have one
            if (influence != null) {
                influence.reference();
            }
            _invalidated.reference();
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            super.wasRemoved();

            // release our influence image (but don't clear it as we may be
            // readded)
            if (influence != null) {
                influence.release();
            }
            _invalidated.release();
        }

        @Override // documentation inherited
        protected void renderComponent (Renderer renderer)
        {
            super.renderComponent(renderer);

            // render our influence icon if we have one
            if (influence != null) {
                influence.render(renderer, getWidth()-19, getHeight()-19, 1f);
            }
            if (_isInvalid) {
                _invalidated.render(renderer,
                        (getWidth() - _invalidated.getWidth())/2,
                        (getHeight() - _invalidated.getHeight())/2, 1f);
            }
        }

        @Override // documentation inherited
        protected void fireAction (long when, int modifiers)
        {
            Unit unit = getUnit();
            if (_bangobj != null && _bangobj.isInteractivePlay() &&
                unit != null && unit.isAlive()) {
                _view.selectUnit(unit, true);
            }
        }

        protected void setInfluence (BImage influence)
        {
            if (this.influence == influence) {
                return;
            }
            if (isAdded() && this.influence != null) {
                this.influence.release();
            }
            this.influence = influence;
            if (isAdded() && this.influence != null) {
                this.influence.reference();
            }
        }

        protected UnitSprite _sprite;
        protected BBackground _bground;
        protected BImage _unit;
        protected boolean _isInvalid;
    }

    protected BangContext _ctx;
    protected BangController _ctrl;
    protected BangBoardView _view;
    protected BangObject _bangobj;
    protected int _pidx;
    protected ArrayList<UnitStatus> _ustatuses = new ArrayList<UnitStatus>();

    protected static BImage _selected;
    protected static BImage[] _closeArrow;
    protected static BImage _invalidated;
    protected static final Dimension LABEL_PREFERRED_SIZE =
        new Dimension(PieceStatus.ICON_SIZE, PieceStatus.ICON_SIZE);
}
