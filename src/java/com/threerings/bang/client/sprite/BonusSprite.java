//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BangContext;

/**
 * Displays a bonus piece of some sort.
 */
public class BonusSprite extends PieceSprite
{
    public BonusSprite (String type)
    {
        _type = type;
    }

    @Override // documentation inherited
    public void init (BangContext ctx, Piece piece, short tick)
    {
        super.init(ctx, piece, tick);
        // load our source image
        _image = ctx.loadImage("media/bonuses/" + _type + ".png");
    }

    @Override // documentation inherited
    public void paint (Graphics2D gfx)
    {
        int width = _bounds.width - _oxoff, iwidth = _image.getWidth();
        int height = _bounds.height - _oyoff, iheight = _image.getHeight();
        gfx.drawImage(_image, _ox + (width-iwidth)/2,
                      _oy + (height-iheight)/2, null);
    }

    protected String _type;
    protected BufferedImage _image;
}
