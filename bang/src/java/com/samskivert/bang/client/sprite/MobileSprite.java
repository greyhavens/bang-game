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
        super(SQUARE-4, SQUARE-4 + EBAR_HEIGHT + EBAR_GAP);
        _oyoff = EBAR_HEIGHT + EBAR_GAP;
    }

    @Override // documentation inherited
    public void paint (Graphics2D gfx)
    {
        super.paint(gfx);

        // paint our remaining energy
        int pct = _piece.getPercentEnergy();
        gfx.setColor(ENERGY_COLORS[pct / 10]);
        gfx.fillRect(_bounds.x, _bounds.y,
                     _bounds.width * pct / 100, EBAR_HEIGHT);
        gfx.setColor(Color.black);
        gfx.drawRect(_bounds.x, _bounds.y, _bounds.width-1, EBAR_HEIGHT-1);
    }

    protected static final int EBAR_HEIGHT = 4;
    protected static final int EBAR_GAP = 2;

    protected static final Color[] ENERGY_COLORS = {
        Color.red, Color.red, Color.yellow, Color.yellow, Color.green,
        Color.green, Color.green, Color.green, Color.green, Color.green };
}
