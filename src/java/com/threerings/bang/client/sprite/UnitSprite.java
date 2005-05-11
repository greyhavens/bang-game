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

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a particular unit.
 */
public class UnitSprite extends MobileSprite
{
    public static final Color[] PIECE_COLORS = {
        Color.blue.brighter(), Color.red, Color.green, Color.yellow
    };

    public static final ColorRGBA[] JPIECE_COLORS = {
        ColorRGBA.blue, ColorRGBA.red, ColorRGBA.green,
        new ColorRGBA(1, 1, 0, 0)
    };

    public static final ColorRGBA[] DARKER_COLORS = {
        ColorRGBA.blue.mult(ColorRGBA.darkGray),
        ColorRGBA.red.mult(ColorRGBA.darkGray),
        ColorRGBA.green.mult(ColorRGBA.darkGray),
        new ColorRGBA(1, 1, 0, 0).mult(ColorRGBA.darkGray)
    };

    public UnitSprite (String type)
    {
        _type = type;
    }

    /**
     * Indicates that the mouse is hovering over this piece.
     */
    public void setHovered (boolean hovered)
    {
        _hovquad.setForceCull(!hovered);
    }

    /**
     * Indicates that this piece is a potential target.
     */
    public void setTargeted (boolean targeted)
    {
        _tgtquad.setForceCull(!targeted);
    }

    @Override // documentation inherited
    public void init (BangContext ctx, Piece piece, short tick)
    {
        super.init(ctx, piece, tick);

        // this icon is displayed when the mouse is hovered over us
        _hovquad = RenderUtil.createIcon(ctx, "media/textures/hovered.png");
        attachChild(_hovquad);
        _hovquad.setForceCull(true);

        // this icon displays who we are
        _ownquad = RenderUtil.createIcon(ctx, "media/textures/circle.png");
        attachChild(_ownquad);

        _model = ctx.getModelCache().getModel(_type);
        attachChild(_model);
        _model.updateRenderState();

        // this icon is displayed when we're a target
        _tgtquad = RenderUtil.createIcon(ctx, "media/textures/crosshair.png");
        attachChild(_tgtquad);
        _tgtquad.setForceCull(true);

        // this will display our damage
        _damage = new Quad("damage", DBAR_WIDTH, DBAR_HEIGHT);
        _damage.setLocalTranslation(
            new Vector3f(DBAR_WIDTH/2+1, DBAR_HEIGHT/2+1, 0));
        _damage.setSolidColor(ColorRGBA.green);
        _damage.setLightCombineMode(LightState.OFF);
        attachChild(_damage);
    }

//     @Override // documentation inherited
//     public void setSelected (boolean selected)
//     {
//         super.setSelected(selected);
//     }

    @Override // documentation inherited
    public void updated (BangBoard board, Piece piece, short tick)
    {
        super.updated(board, piece, tick);

        if (!_piece.isAlive()) {
            _ownquad.setForceCull(true);
        } else if (_piece.ticksUntilMovable(_tick) > 0) {
            _ownquad.setSolidColor(DARKER_COLORS[_piece.owner]);
        } else {
            _ownquad.setSolidColor(JPIECE_COLORS[_piece.owner]);
        }

        float size = (100 - piece.damage) * DBAR_WIDTH / 100f;
        _damage.resize(size, DBAR_HEIGHT);
        _damage.getLocalTranslation().x = size/2 + 1;
        _damage.updateGeometricState(0, true);
    }

    @Override // documentation inherited
    public boolean isSelectable ()
    {
        return ((_piece.ticksUntilMovable(_tick) == 0) ||
                (_piece.ticksUntilFirable(_tick) == 0));
    }

    protected String _type;
    protected Node _model;
    protected Quad _damage;
    protected Quad _ownquad, _hovquad, _tgtquad;

    protected static final float DBAR_WIDTH = TILE_SIZE-2;
    protected static final float DBAR_HEIGHT = (TILE_SIZE-2)/6f;
}
