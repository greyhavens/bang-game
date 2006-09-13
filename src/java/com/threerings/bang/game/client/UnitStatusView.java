//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jmex.bui.BButton;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.background.BBackground;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.game.client.sprite.UnitSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Displays the status of the various units in iconic form.
 */
public class UnitStatusView extends BWindow
{
    public UnitStatusView (
        BangContext ctx, BangBoardView view, BangObject bangobj)
    {
        super(ctx.getStyleSheet(), GroupLayout.makeVert(GroupLayout.TOP));

        _ctx = ctx;
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
            int width = _ctx.getDisplay().getWidth();
            int height = _ctx.getDisplay().getHeight();
            setLocation(13, height - getHeight() - 5);
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
        UnitLabel ulabel = null;
        for (UnitLabel label : _labels) {
            if (label.pieceId == pieceId) {
                ulabel = label;
                break;
            }
        }
        if (ulabel == null) {
            _labels.add(ulabel = new UnitLabel());
        }
        ulabel.setUnitSprite(usprite);
        resort();
        reposition();
    }

    /**
     * Called when a unit sprite is removed from the game.
     */
    public void unitRemoved (UnitSprite usprite)
    {
        int pieceId = usprite.getPieceId();
        UnitLabel ulabel = null;
        for (UnitLabel label : _labels) {
            if (label.pieceId == pieceId) {
                ulabel = label;
                break;
            }
        }
        if (ulabel == null) {
            return;
        }

        // if this unit was hijacked from another player, or a duplicate,
        // remove this label
        if (((Unit)usprite.getPiece()).originalOwner != _pidx) {
            _labels.remove(ulabel);
            resort();
            reposition();
        }
    }

    protected void resort ()
    {
        Collections.sort(_labels);
        removeAll();
        for (UnitLabel label : _labels) {
            add(label);
        }
    }

    protected class UnitLabel extends BButton
        implements UnitSprite.UpdateObserver, Comparable<UnitLabel>
    {
        public int pieceId;

        public UnitLabel () {
            super("");
            setStyleClass("unit_status_label");
        }

        public void setUnitSprite (UnitSprite sprite) {
            // clear out our old sprite
            clearSprite();

            // observer our new sprite
            _sprite = sprite;
            _sprite.addObserver(this);
            pieceId = _sprite.getPieceId();

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

        // documentation inherited from interface UnitSprite.UpdateObserver
        public void updated (UnitSprite sprite) {
            Unit unit = (Unit)sprite.getPiece();
            if (!unit.isAlive()) {
                // if this unit is not originally ours, remove ourselves
                if (unit.originalOwner != _pidx) {
                    UnitStatusView.this.remove(this);
                } else {
                    // clear out any influence when the unit dies
                    setInfluence(null);
                    // clear out our background when our unit dies
                    _bground = null;
                    // and draw our icon at 50% alpha
                    _unit.setDefaultColor(ColorRGBA.white);
                    setAlpha(0.5f);
                }
            } else {
                if (unit.influence == null) {
                    setInfluence(null);
                } else {
                    // we don't worry about refetching the image from the cache
                    // if our influence didn't actually change, it's cheap
                    String path =
                        "influences/" + unit.influence.getName() + ".png";
                    setInfluence(_ctx.getImageCache().getBImage(path));
                }
                // tint the unit icon red if it has a visible hindrance
                if (unit.hindrance == null || !unit.hindrance.isVisible()) {
                    _unit.setDefaultColor(ColorRGBA.white);
                } else {
                    _unit.setDefaultColor(new ColorRGBA(1f, 0.5f, 0.5f, 0.8f));
                }
            }
        }

        // documentation inherited from interface Comparable
        public int compareTo (UnitLabel other) {
            Unit u1 = getUnit();
            Unit u2 = other.getUnit();
            UnitConfig uc1 = u1.getConfig(), uc2 = u2.getConfig();
            if (uc1.rank != uc2.rank) {
                return (uc1.rank == UnitConfig.Rank.BIGSHOT ? -1 : 
                        (uc2.rank == UnitConfig.Rank.BIGSHOT ? 1 : -1));
            }
            return -1;
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
            if (_influence != null) {
                _influence.reference();
            }
        }

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            super.wasRemoved();

            // release our influence image (but don't clear it as we may be
            // readded)
            if (_influence != null) {
                _influence.release();
            }
        }

        @Override // documentation inherited
        protected void renderComponent (Renderer renderer)
        {
            super.renderComponent(renderer);

            // render our influence icon if we have one
            if (_influence != null) {
                _influence.render(renderer, getWidth()-19, getHeight()-19, 1f);
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
            if (_influence == influence) {
                return;
            }
            if (_influence != null) {
                _influence.release();
                _influence = null;
            }
            _influence = influence;
            if (_influence != null) {
                _influence.reference();
            }
        }

        protected void clearSprite () {
            if (_sprite != null) {
                _sprite.removeObserver(this);
                _sprite = null;
            }
            _bground = null;
        }

        protected UnitSprite _sprite;
        protected BBackground _bground;
        protected BImage _influence;
        protected BImage _unit;
    }

    protected BangContext _ctx;
    protected BangBoardView _view;
    protected BangObject _bangobj;
    protected int _pidx;
    protected ArrayList<UnitLabel> _labels = new ArrayList<UnitLabel>();
}
