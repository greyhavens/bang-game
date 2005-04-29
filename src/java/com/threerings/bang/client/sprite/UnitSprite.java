//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.samskivert.util.HashIntMap;

import com.threerings.media.image.ImageUtil;

import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a particular unit.
 */
public class UnitSprite extends MobileSprite
{
    public static final Color[] PIECE_COLORS = {
        Color.blue.brighter(), Color.red, Color.green, Color.yellow
    };

    public static final Color[] DARKER_COLORS = {
        Color.blue.darker().darker(), Color.red.darker(),
        Color.green.darker(), Color.yellow.darker()
    };

    public UnitSprite (String type)
    {
        _type = type;
    }

    @Override // documentation inherited
    public void init (BangContext ctx, Piece piece, short tick)
    {
        super.init(ctx, piece, tick);
        _image = ctx.loadImage("media/units/" + _type + ".png");
    }

    @Override // documentation inherited
    public boolean isSelectable ()
    {
        return ((_piece.ticksUntilMovable(_tick) == 0) ||
                (_piece.ticksUntilFirable(_tick) == 0));
    }

    @Override // documentation inherited
    public void paint (Graphics2D gfx)
    {
        if (_piece.isAlive()) {
            Color color = (_piece.ticksUntilMovable(_tick) > 0) ?
                DARKER_COLORS[_piece.owner] : PIECE_COLORS[_piece.owner];
            gfx.setColor(color);
            gfx.fillRect(_ox+1, _oy+1, SQUARE-DBAR_SIZE-1, SQUARE-1);
        }
        super.paint(gfx);
    }

    @Override // documentation inherited
    protected void paintPiece (Graphics2D gfx)
    {
        int width = _bounds.width - _oxoff, iwidth = _image.getWidth();
        int height = _bounds.height - _oyoff, iheight = _image.getHeight();

        gfx.drawImage(_image, _ox + (width-iwidth)/2,
                      _oy + (height-iheight)/2, null);

        if (_selected) {
            gfx.setColor(Color.green);
            gfx.drawRect(_ox, _oy, width-1, height-1);
        }
    }

    protected String _type;
    protected BufferedImage _image;
}
