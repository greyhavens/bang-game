//
// $Id$

package com.samskivert.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.threerings.media.image.ImageUtil;
import com.threerings.toybox.util.ToyBoxContext;

import com.samskivert.bang.data.piece.Piece;

/**
 * Displays a particular unit.
 */
public class UnitSprite extends MobileSprite
{
    public UnitSprite (String type)
    {
        _type = type;
    }

    @Override // documentation inherited
    public void init (ToyBoxContext ctx, Piece piece)
    {
        super.init(ctx, piece);

        // load our source image and rotate it appropriately
        BufferedImage src = _images[WEST] =
            ctx.loadImage("media/" + _type + ".png");
        int width = src.getWidth(), height = src.getHeight();

        double theta = Math.PI/2;
        for (int ii = 1; ii < 4; ii++) {
            BufferedImage dest = _images[(WEST+ii)%4] =
                ImageUtil.createCompatibleImage(src, width, height);
            Graphics2D gfx = (Graphics2D)dest.getGraphics();
            gfx.translate(width/2, height/2);
            gfx.rotate(theta);
            gfx.translate(-width/2, -height/2);
            gfx.drawImage(src, 0, 0, null);
            gfx.dispose();
            theta += Math.PI/2;
        }
    }

    @Override // documentation inherited
    public boolean isSelectable ()
    {
        return true;
    }

    @Override // documentation inherited
    public void paint (Graphics2D gfx)
    {
        super.paint(gfx);

        BufferedImage image = _images[_piece.orientation];
        int width = _bounds.width - _oxoff, iwidth = image.getWidth();
        int height = _bounds.height - _oyoff, iheight = image.getHeight();

        gfx.drawImage(image, _ox + (width-iwidth)/2,
                      _oy + (height-iheight)/2, null);

        if (_piece.hasPath()) {
            gfx.setColor(Color.blue);
            gfx.drawRect(_ox, _oy, width-1, height-1);
        } else if (_selected) {
            gfx.setColor(Color.green);
            gfx.drawRect(_ox, _oy, width-1, height-1);
        }
    }

    protected String _type;
    protected BufferedImage[] _images = new BufferedImage[4];
}
