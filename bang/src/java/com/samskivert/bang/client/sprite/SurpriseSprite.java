//
// $Id$

package com.samskivert.bang.client.sprite;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.threerings.toybox.util.ToyBoxContext;

import com.samskivert.bang.data.piece.Piece;

/**
 * Displays a surprise piece of some sort.
 */
public class SurpriseSprite extends PieceSprite
{
    public SurpriseSprite (String type)
    {
        _type = type;
    }

    @Override // documentation inherited
    public void init (ToyBoxContext ctx, Piece piece)
    {
        super.init(ctx, piece);
        // load our source image
        _image = ctx.loadImage("media/surprises/" + _type + ".png");
    }

    @Override // documentation inherited
    public void paint (Graphics2D gfx)
    {
        super.paint(gfx);

        int width = _bounds.width - _oxoff, iwidth = _image.getWidth();
        int height = _bounds.height - _oyoff, iheight = _image.getHeight();
        gfx.drawImage(_image, _ox + (width-iwidth)/2,
                      _oy + (height-iheight)/2, null);
    }

    protected String _type;
    protected BufferedImage _image;
}
