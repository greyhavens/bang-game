//
// $Id$

package com.samskivert.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;

import static com.samskivert.bang.client.BangMetrics.*;

/**
 * A sprite that moves around and displays its remaining energy.
 */
public class MobileSprite extends PieceSprite
{
    public MobileSprite ()
    {
        super(SQUARE-3, SQUARE-3 + DBAR_HEIGHT + DBAR_GAP);
        _oyoff = DBAR_HEIGHT + DBAR_GAP;
        _renderOrder = 5;
    }

    @Override // documentation inherited
    public void paint (Graphics2D gfx)
    {
        // paint our damage level
        if (_piece.damage < 100) {
            int pct = 100 - _piece.damage;
            gfx.setColor(DAMAGE_COLORS[pct / 10]);
            float epix = (_bounds.width-2) * pct / 100f;
            gfx.fillRect(_bounds.x+1, _bounds.y,
                         (int)Math.ceil(epix), DBAR_HEIGHT);
            gfx.setColor(Color.black);
            gfx.drawRect(_bounds.x, _bounds.y, _bounds.width-1, DBAR_HEIGHT-1);
        }
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

    protected static final int DBAR_HEIGHT = 4;
    protected static final int DBAR_GAP = 3;

    protected static final Color[] DAMAGE_COLORS = {
        Color.red, Color.red, Color.yellow, Color.yellow, Color.green,
        Color.green, Color.green, Color.green, Color.green, Color.green,
        Color.green.brighter() };
}
