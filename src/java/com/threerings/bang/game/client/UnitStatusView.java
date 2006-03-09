//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.background.BBackground;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.game.client.sprite.UnitSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.util.BangContext;

/**
 * Displays the status of the various units in iconic form.
 */
public class UnitStatusView extends BWindow
{
    public UnitStatusView (BangContext ctx, BangObject bangobj)
    {
        super(ctx.getStyleSheet(), GroupLayout.makeVert(GroupLayout.TOP));

        _ctx = ctx;
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
            setLocation(width - getWidth() - 5, height - getHeight() - 5);
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

    protected void resort ()
    {
        Collections.sort(_labels);
        removeAll();
        for (UnitLabel label : _labels) {
            add(label);
        }
    }

    protected class UnitLabel extends BLabel
        implements UnitSprite.UpdateObserver, Comparable<UnitLabel>
    {
        public int pieceId;

        public UnitLabel () {
            super("", "unit_status_label");
        }

        public void setUnitSprite (UnitSprite sprite) {
            // clear out our old sprite
            clearSprite();

            // observer our new sprite
            _sprite = sprite;
            _sprite.addObserver(this);
            pieceId = _sprite.getPieceId();

//             // setup our background
//             BImage status = new BImage(
//                 sprite.getStatusTexture().getStatusState(), 64, 64);
//             _bground = new ImageBackground(ImageBackground.CENTER_XY, status);

            // set up our icon image
            Unit unit = (Unit)sprite.getPiece();
            String ipath = "units/" + unit.getType() + "/icon_head.png";
            setIcon(new ImageIcon(_ctx.loadImage(ipath)));
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
                    // clear out our background when our unit dies
                    _bground = null;
                    // and draw our icon at 50% alpha
                    setAlpha(0.5f);
                }
            } else {
                // TODO: update our influence icon if necessary
            }
            resort();
        }

        // documentation inherited from interface Comparable
        public int compareTo (UnitLabel other) {
            Unit u1 = getUnit();
            Unit u2 = other.getUnit();
            if (u1.isAlive() != u2.isAlive()) {
                return u1.isAlive() ? -1 : 1;
            }
            if (u1.lastActed != u2.lastActed) {
                return u1.lastActed - u2.lastActed;
            }
            String t1 = u1.getType(), t2 = u2.getType();
            int cv = t1.compareTo(t2);
            if (cv != 0) {
                return cv;
            }
            return u1.pieceId - u2.pieceId;
        }

        @Override // documentation inherited
        public BBackground getBackground () {
            return _bground;
        }

        protected void clearSprite () {
            if (_sprite != null) {
                _sprite.removeObserver(this);
                _sprite = null;
            }
            _bground = null;
        }

        protected UnitSprite _sprite;
        protected ImageBackground _bground;
    }

    protected BangContext _ctx;
    protected int _pidx;
    protected ArrayList<UnitLabel> _labels = new ArrayList<UnitLabel>();
}
