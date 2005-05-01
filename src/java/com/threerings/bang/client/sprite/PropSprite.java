//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a prop piece.
 */
public class PropSprite extends PieceSprite
{
    public PropSprite (String type, int width, int height)
    {
//         super(width*SQUARE, height*SQUARE);
        _type = type;
    }

//     @Override // documentation inherited
//     public void init (BangContext ctx, Piece piece, short tick)
//     {
//         super.init(ctx, piece, tick);

//         // load our source image and rotate it appropriately
//         _image = ctx.loadImage("media/props/" + _type + ".png");
//     }

//     // documentation inherited
//     public void paint (Graphics2D gfx)
//     {
//         int width = _bounds.width - _oxoff, iwidth = _image.getWidth();
//         int height = _bounds.height - _oyoff, iheight = _image.getHeight();

//         gfx.drawImage(_image, _ox + (width-iwidth)/2,
//                       _oy + (height-iheight)/2, null);

// //         // TEMP: render our id so I can debug prop jiggling
// //         int lx = _ox+(width-_idLabel.getSize().width)/2;
// //         int ly = _oy+(height-_idLabel.getSize().height)/2;
// //         _idLabel.render(gfx, lx, ly);
//     }

    protected String _type;
//     protected BufferedImage _image;
}
