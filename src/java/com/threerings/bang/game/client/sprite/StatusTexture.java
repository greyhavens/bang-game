//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

import java.nio.ByteBuffer;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.scene.state.TextureState;

import com.threerings.bang.game.data.piece.Piece;

import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * A helper class to manage the composition of our unit status image.
 */
public class StatusTexture
{
    public static final int STATUS_SIZE = 128;

    public StatusTexture (BasicContext ctx)
    {
        _ctx = ctx;

        // create our image
        _image = new Image();
        _image.setType(Image.RGBA8888);
        _image.setWidth(STATUS_SIZE);
        _image.setHeight(STATUS_SIZE);

        // our texture
        _texture = new Texture();
        _texture.setCorrection(Texture.CM_PERSPECTIVE);
        _texture.setFilter(Texture.FM_LINEAR);
        _texture.setMipmapState(Texture.MM_LINEAR_LINEAR);
        _texture.setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);

        // and our texture states
        _spstate = ctx.getRenderer().createTextureState();
        _ststate = ctx.getRenderer().createTextureState();

        // load up our shared images if we have not yet done so
        if (_target == null) {
            _target = _ctx.getImageCache().createCompatibleImage(
                STATUS_SIZE, STATUS_SIZE, true);
            _ready = ctx.getImageCache().getBufferedImage(
                "textures/ustatus/tick_ready.png");
            _dfull = ctx.getImageCache().getBufferedImage(
                "textures/ustatus/health_meter_full.png");
            _dempty = ctx.getImageCache().getBufferedImage(
                "textures/ustatus/health_meter_empty.png");
            _morder = ctx.getImageCache().getBufferedImage(
                "textures/ustatus/move_order.png");
            _msorder = ctx.getImageCache().getBufferedImage(
                "textures/ustatus/move_shoot_order.png");
            _outline = ctx.getImageCache().getBufferedImage(
                "textures/ustatus/tick_outline.png");
            _ticks = new BufferedImage[5];
            for (int ii = 0; ii < _ticks.length; ii++) {
                _ticks[ii] = ctx.getImageCache().getBufferedImage(
                    "textures/ustatus/tick_counter_" + ii + ".png");
            }
        }
    }

    /**
     * Returns the texture state that can be used to display the status
     * texture.
     */
    public TextureState getSpriteState ()
    {
        return _spstate;
    }

    public TextureState getStatusState ()
    {
        return _ststate;
    }

    /**
     * Recomposites if necessary our status texture and updates the texture
     * state.
     */
    public void update (Piece piece, int ticksToMove,
                        UnitSprite.AdvanceOrder pendo, boolean selected)
    {
        if (_texture.getImage() != null && _ticksToMove == ticksToMove &&
            _owner == piece.owner && _damage == piece.damage &&
            _pendo == pendo && _selected == selected) {
            return;
        }

        // if we have a previous texture, delete it
        if (_texture.getTextureId() > 0) {
            _spstate.deleteAll();
        }

        _owner = piece.owner;
        _damage = piece.damage;
        _ticksToMove = ticksToMove;
        _pendo = pendo;
        _selected = selected;

        RescaleOp colorizer = RECOLORS[_owner];
        Graphics2D gfx = (Graphics2D)_target.getGraphics();
        try {
            // start with a blank slate
            gfx.setColor(CLEAR_COLOR);
            gfx.setComposite(AlphaComposite.SrcOut);
            gfx.fillRect(0, 0, STATUS_SIZE, STATUS_SIZE);
            gfx.setComposite(AlphaComposite.SrcOver);

            // draw our outline
            RescaleOp selop = (_selected ? WHITENER : colorizer);
            if (_ticksToMove > 0) {
                gfx.setClip(0, 0, STATUS_SIZE, STATUS_SIZE/2);
                gfx.drawImage(_outline, selop, 0, 0);
                gfx.setClip(null);
            } else {
                gfx.drawImage(_outline, selop, 0, 0);
            }

            // draw our pending order indicator
            switch (_pendo) {
            case MOVE:
                gfx.drawImage(_morder, colorizer, 0, 0);
                break;
            case MOVE_SHOOT:
                gfx.drawImage(_msorder, colorizer, 0, 0);
                break;
            }

            // draw the ready indicator if appropriate
            if (_ticksToMove <= 0) {
                gfx.drawImage(_ready, colorizer, 0, 0);
            }

            // draw the appropriate tick imagery
            gfx.drawImage(_ticks[Math.max(0, 4-_ticksToMove)], colorizer, 0, 0);

            // draw the current damage level
            gfx.drawImage(_dempty, colorizer, 0, 0);
            float percent = (100 - _damage) / 100f;
            float extent = percent * (90 - 2*ARC_INSETS);
            // expand the width and height a smidge to avoid funny business
            // around the edges
            Arc2D.Float arc = new Arc2D.Float(
                -STATUS_SIZE/8, -STATUS_SIZE/8,
                10*STATUS_SIZE/8, 10*STATUS_SIZE/8,
                90 - ARC_INSETS - extent, extent, Arc2D.PIE);
            gfx.setClip(arc);
            gfx.drawImage(_dfull, colorizer, 0, 0);

        } finally {
            gfx.dispose();
        }

        // turn the buffered image into image data for our texture
        _data = _ctx.getImageCache().convertImage(_target, _data);
        _image.setData(_data);

        // configure the texture and load it into OpenGL
        _texture.setImage(_image);
        _texture.setNeedsFilterRefresh(true);
        _texture.setNeedsWrapRefresh(true);
        _spstate.setTexture(_texture);
        _spstate.apply();

        // clone the texture and stick it in our status state which we will use
        // without rotating
        _ststate.setTexture(_texture.createSimpleClone());
    }

    public void cleanup ()
    {
        // if we have a previous texture, delete it
        if (_texture.getTextureId() > 0) {
            _spstate.deleteAll();
        }
    }

    protected BasicContext _ctx;
    protected int _owner;
    protected int _ticksToMove;
    protected int _damage;
    protected boolean _selected;
    protected UnitSprite.AdvanceOrder _pendo = UnitSprite.AdvanceOrder.NONE;

    protected ByteBuffer _data;
    protected Image _image;
    protected Texture _texture;
    protected TextureState _spstate, _ststate;

    protected static BufferedImage _target;
    protected static BufferedImage _ready;
    protected static BufferedImage _dfull, _dempty;
    protected static BufferedImage _morder, _msorder;
    protected static BufferedImage _outline;
    protected static BufferedImage[] _ticks;

    /** Defines the amount by which the damage arc image is inset from a
     * full quarter circle (on each side): 8 degrees. */
    protected static final float ARC_INSETS = 7;

    /** Used to recolor our 24-bit status images. */
    protected static final RescaleOp[] RECOLORS = new RescaleOp[] {
        new RescaleOp(new float[] { 0, 0, 1, 1 }, new float[4], null), // blue
        new RescaleOp(new float[] { 1, 0, 0, 1 }, new float[4], null), // red
        new RescaleOp(new float[] { 0, 1, 0, 1 }, new float[4], null), // green
        new RescaleOp(new float[] { 1, 1, 0, 1 }, new float[4], null), // yellow
    };

    /** Used to make our gray outline white. */
    protected static final RescaleOp WHITENER =
        new RescaleOp(new float[] { 1, 1, 1, 1 },
                      new float[] { 255, 255, 255, 0 }, null);

    /** Used to erase our image. */
    protected static final Color CLEAR_COLOR = new Color(255, 255, 255, 0);
}
