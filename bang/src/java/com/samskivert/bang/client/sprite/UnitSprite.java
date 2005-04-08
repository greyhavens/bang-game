//
// $Id$

package com.samskivert.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.samskivert.util.HashIntMap;

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
            ctx.loadImage("media/units/" + _type + ".png");
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
        return ((_piece.ticksUntilMovable(_tick) == 0) ||
                (_piece.ticksUntilFirable(_tick) == 0));
    }

    @Override // documentation inherited
    protected void paintPiece (Graphics2D gfx)
    {
        BufferedImage image = getImage(_piece.owner, _piece.orientation);
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

    protected BufferedImage getImage (int player, int orient)
    {
        BufferedImage[] colored = (BufferedImage[])_outlined.get(player);
        if (colored == null) {
            colored = new BufferedImage[_images.length];
            for (int ii = 0; ii < colored.length; ii++) {
                colored[ii] = ImageUtil.createCompatibleImage(
                    _images[ii], _images[ii].getWidth(),
                    _images[ii].getHeight());
                ImageUtil.createTracedImage(
                    _images[ii], colored[ii], PIECE_COLORS[player],
                    1, 1.0f, 1.0f);
            }
            _outlined.put(player, colored);
        }
        return colored[orient];
    }

    protected String _type;
    protected BufferedImage[] _images = new BufferedImage[4];
    protected HashIntMap _outlined = new HashIntMap();

    protected static final Color[] PIECE_COLORS = {
        Color.blue, Color.red, Color.green, Color.yellow
    };
}
