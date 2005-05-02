//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.samskivert.util.HashIntMap;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.shape.Box;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;

import com.threerings.media.image.ImageUtil;

import com.threerings.bang.data.BangBoard;
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

        BufferedImage image = ctx.loadImage("media/units/" + _type + ".png");
        _dead = createTexture(ctx, image);

        BufferedImage dimage = new BufferedImage(
            image.getWidth(), image.getHeight(), image.getType());
        Graphics2D gfx = (Graphics2D)dimage.getGraphics();
        gfx.setColor(PIECE_COLORS[_piece.owner]);
        gfx.fillRect(0, 0, image.getWidth(), image.getHeight());
        gfx.drawImage(image, 0, 0, null);
        gfx.dispose();
        _ready = createTexture(ctx, dimage);

        dimage = new BufferedImage(
            image.getWidth(), image.getHeight(), image.getType());
        gfx = (Graphics2D)dimage.getGraphics();
        gfx.setColor(DARKER_COLORS[_piece.owner]);
        gfx.fillRect(0, 0, image.getWidth(), image.getHeight());
        gfx.drawImage(image, 0, 0, null);
        gfx.dispose();
        _waiting = createTexture(ctx, dimage);

        _selquad = new Quad("selected", TILE_SIZE, TILE_SIZE);
        _selquad.setLocalTranslation(
            new Vector3f(TILE_SIZE/2, TILE_SIZE/2, 0f));
        _selquad.setSolidColor(ColorRGBA.green);
        _selquad.setLightCombineMode(LightState.OFF);
        attachChild(_selquad);
        _selquad.setForceCull(true);

        _box = new Box("piece", new Vector3f(1, 1, 0),
                       new Vector3f(TILE_SIZE-1, TILE_SIZE-1, TILE_SIZE-2));
        _box.setModelBound(new BoundingBox());
        _box.updateModelBound();
        _box.setRenderState(getTextureState(tick));
        _box.updateRenderState();
        attachChild(_box);
    }

    @Override // documentation inherited
    public void setSelected (boolean selected)
    {
        super.setSelected(selected);
        _selquad.setForceCull(!selected);
    }

    @Override // documentation inherited
    public void updated (BangBoard board, Piece piece, short tick)
    {
        super.updated(board, piece, tick);

        // make sure we're using the correct texture
        TextureState state = getTextureState(tick);
        if (_box.getRenderStateList()[state.getType()] != state) {
            _box.setRenderState(state);
            _box.updateRenderState();
        }
    }

    @Override // documentation inherited
    public boolean isSelectable ()
    {
        return ((_piece.ticksUntilMovable(_tick) == 0) ||
                (_piece.ticksUntilFirable(_tick) == 0));
    }

    protected TextureState createTexture (BangContext ctx, BufferedImage image)
    {
        Texture texture = TextureManager.loadTexture(
            image, Texture.MM_LINEAR_LINEAR, Texture.FM_NEAREST, false);
        TextureState tstate =
            ctx.getDisplay().getRenderer().createTextureState();
        tstate.setEnabled(true);
        tstate.setTexture(texture);
        return tstate;
    }

    protected TextureState getTextureState (short tick)
    {
        if (!_piece.isAlive()) {
            return _dead;
        } else if (_piece.ticksUntilMovable(_tick) > 0) {
            return _waiting;
        } else {
            return _ready;
        }
    }

//     @Override // documentation inherited
//     public void paint (Graphics2D gfx)
//     {
//         if (_piece.isAlive()) {
//             Color color = (_piece.ticksUntilMovable(_tick) > 0) ?
//                 DARKER_COLORS[_piece.owner] : PIECE_COLORS[_piece.owner];
//             gfx.setColor(color);
//             gfx.fillRect(_ox+1, _oy+1, SQUARE-DBAR_SIZE-1, SQUARE-1);
//         }
//         super.paint(gfx);
//     }

//     @Override // documentation inherited
//     protected void paintPiece (Graphics2D gfx)
//     {
//         int width = _bounds.width - _oxoff, iwidth = _image.getWidth();
//         int height = _bounds.height - _oyoff, iheight = _image.getHeight();

//         gfx.drawImage(_image, _ox + (width-iwidth)/2,
//                       _oy + (height-iheight)/2, null);

//         if (_selected) {
//             gfx.setColor(Color.green);
//             gfx.drawRect(_ox, _oy, width-1, height-1);
//         }
//     }

    protected String _type;
    protected Box _box;
    protected Quad _selquad;
    protected TextureState _ready, _waiting, _dead;
}
