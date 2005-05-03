//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.samskivert.util.HashIntMap;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;

import com.threerings.media.image.ImageUtil;

import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

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

        BufferedImage dimage = createRingImage(PIECE_COLORS[_piece.owner]);
        _ready = RenderUtil.createTexture(ctx, dimage);

        dimage = createRingImage(DARKER_COLORS[_piece.owner]);
        _waiting = RenderUtil.createTexture(ctx, dimage);

        AlphaState astate = ctx.getDisplay().getRenderer().createAlphaState();
        astate.setBlendEnabled(true);
        astate.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        astate.setDstFunction(AlphaState.DB_ONE);
        astate.setEnabled(true);

        _ownquad = new Quad("selected", TILE_SIZE, TILE_SIZE);
        _ownquad.setLocalTranslation(
            new Vector3f(TILE_SIZE/2, TILE_SIZE/2, 0f));
        _ownquad.setRenderState(astate);
        attachChild(_ownquad);

        _selquad = new Quad("selected", TILE_SIZE, TILE_SIZE);
        _selquad.setLocalTranslation(
            new Vector3f(TILE_SIZE/2, TILE_SIZE/2, 0f));
        _selquad.setSolidColor(ColorRGBA.green);
        _selquad.setLightCombineMode(LightState.OFF);
        _selquad.setRenderState(astate);
        attachChild(_selquad);
        _selquad.setForceCull(true);

        _model = ctx.getModelCache().getModel(_type);
//         _model.setRenderState(getTextureState(tick));
//         _model.updateRenderState();
        attachChild(_model);
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
        if (state == null) {
            _ownquad.setForceCull(true);
        } else if (_ownquad.getRenderStateList()[state.getType()] != state) {
            _ownquad.setRenderState(state);
            _ownquad.updateRenderState();
        }
    }

    @Override // documentation inherited
    public boolean isSelectable ()
    {
        return ((_piece.ticksUntilMovable(_tick) == 0) ||
                (_piece.ticksUntilFirable(_tick) == 0));
    }

    protected TextureState getTextureState (short tick)
    {
        if (!_piece.isAlive()) {
            return null;
        } else if (_piece.ticksUntilMovable(_tick) > 0) {
            return _waiting;
        } else {
            return _ready;
        }
    }

    protected BufferedImage createRingImage (Color color)
    {
        int size = 100;
        BufferedImage img =
            new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gfx = (Graphics2D)img.getGraphics();
        gfx.setColor(color);
        for (int ii = 0; ii < size/10; ii++) {
            gfx.drawOval(ii, ii, size-2*ii, size-2*ii);
        }
        gfx.dispose();
        return img;
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
    protected Node _model;
    protected Quad _ownquad, _selquad;
    protected TextureState _ready, _waiting;
}
