//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * A sprite that moves around and displays its remaining energy.
 */
public class MobileSprite extends PieceSprite
{
    public MobileSprite ()
    {
        super(SQUARE, SQUARE);
        _renderOrder = 5;
    }

    @Override // documentation inherited
    public void paint (Graphics2D gfx)
    {
        // paint our damage level
        if (_piece.damage < 100) {
            int pct = 100 - _piece.damage;
            gfx.setColor(DAMAGE_COLORS[pct / 10]);
            float epix = (_bounds.height-1) * pct / 100f;
            int height = (int)Math.ceil(epix);
            int sx = _bounds.x+_bounds.width-DBAR_SIZE;
            gfx.fillRect(sx, _bounds.y + (_bounds.height-height),
                         DBAR_SIZE, height);
            gfx.setColor(Color.black);
            gfx.drawLine(sx, _bounds.y, sx, _bounds.y+_bounds.height-1);
        }

        // paint the piece itself
        paintPiece(gfx);

        // now paint an indication of the number of ticks remaining before
        // this piece can again move
        int ttm = _piece.ticksUntilMovable(_tick);
        int by = _bounds.y;
        gfx.setColor(Color.white);
        for (int ii = 0; ii < ttm; ii++) {
            gfx.fillRect(_bounds.x, by, 4, 4);
            by += 5;
        }
    }

    /**
     * Derives classes should override this method and do their actual
     * piece painting here so that we can properly decorate their
     * rendering.
     */
    protected  void paintPiece (Graphics2D gfx)
    {
    }

    /** Returns the color of this piece. */
    protected Color getColor ()
    {
        Color color = _piece.owner == 0 ? Color.white : Color.yellow;
        if (_piece.energy <= 0 || _piece.damage >= 100) {
            color = Color.gray;
        }
        return color;
    }

    protected static final int DBAR_SIZE = 4;

    protected static final Color[] DAMAGE_COLORS = {
        Color.red, Color.red, Color.yellow, Color.yellow, Color.green,
        Color.green, Color.green, Color.green, Color.green, Color.green,
        Color.green.brighter() };
}
