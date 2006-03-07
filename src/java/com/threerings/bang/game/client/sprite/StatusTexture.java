//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
        _tstate = ctx.getRenderer().createTextureState();
        _texture = new Texture();
        _texture.setCorrection(Texture.CM_PERSPECTIVE);
        _texture.setFilter(Texture.FM_LINEAR);
        _texture.setMipmapState(Texture.MM_LINEAR_LINEAR);
        _texture.setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);

        // load up our images if we have not yet done so
        if (_ready == null) {
            _ready = ctx.getImageCache().getBufferedImage(
                "textures/ustatus/tick_ready.png");
            _dfull = ctx.getImageCache().getBufferedImage(
                "textures/ustatus/health_meter_full.png");
            _dempty = ctx.getImageCache().getBufferedImage(
                "textures/ustatus/health_meter_empty.png");
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
    public TextureState getTextureState ()
    {
        return _tstate;
    }

    /**
     * Recomposites if necessary our status texture and updates the texture
     * state.
     */
    public void update (Piece piece, int ticksToMove, boolean selected)
    {
        if (_texture.getImage() != null && _ticksToMove == ticksToMove &&
            _owner == piece.owner && _damage == piece.damage &&
            _selected == selected) {
            return;
        }

        // if we have a previous texture, delete it
        if (_texture.getTextureId() > 0) {
            _tstate.deleteAll();
        }

        _owner = piece.owner;
        _damage = piece.damage;
        _ticksToMove = ticksToMove;
        _selected = selected;

        BufferedImage target = new BufferedImage(
            STATUS_SIZE, STATUS_SIZE, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D gfx = (Graphics2D)target.getGraphics();
        try {
            gfx.scale(1, -1);
            gfx.translate(0, -STATUS_SIZE);

            // draw the ready indicator if appropriate
            if (_ticksToMove <= 0) {
                gfx.drawImage(_ready, 0, 0, null);
            }

            // draw the appropriate tick imagery
            gfx.drawImage(_ticks[Math.max(0, 4-_ticksToMove)], 0, 0, null);

            // draw the current damage level
            gfx.drawImage(_dempty, 0, 0, null);
            float percent = (100 - _damage) / 100f;
            float extent = percent * (90 - 2*ARC_INSETS);
            // expand the width and height a smidge to avoid funny business
            // around the edges
            Arc2D.Float arc = new Arc2D.Float(
                -STATUS_SIZE/8, -STATUS_SIZE/8,
                10*STATUS_SIZE/8, 10*STATUS_SIZE/8,
                90 - ARC_INSETS - extent, extent, Arc2D.PIE);
            gfx.setClip(arc);
            gfx.drawImage(_dfull, 0, 0, null);

        } finally {
            gfx.dispose();
        }

        // turn the buffered image into image data for our texture
        ByteBuffer scratch = ByteBuffer.allocateDirect(
            4 * STATUS_SIZE * STATUS_SIZE).order(ByteOrder.nativeOrder());
        scratch.clear();
        scratch.put((byte[])target.getRaster().getDataElements(
                        0, 0, STATUS_SIZE, STATUS_SIZE, null));
        scratch.flip();
        Image teximg = new Image();
        teximg.setType(Image.RGBA8888);
        teximg.setWidth(STATUS_SIZE);
        teximg.setHeight(STATUS_SIZE);
        teximg.setData(scratch);

        // configure the texture and load it into OpenGL
        _texture.setImage(teximg);
        _texture.setNeedsFilterRefresh(true);
        _texture.setNeedsWrapRefresh(true);
        _tstate.setTexture(_texture);
        _tstate.apply();
    }

    public void cleanup ()
    {
        // if we have a previous texture, delete it
        if (_texture.getTextureId() > 0) {
            _tstate.deleteAll();
        }
    }

    protected BasicContext _ctx;
    protected int _owner;
    protected int _ticksToMove;
    protected int _damage;
    protected boolean _selected;

    protected Texture _texture;
    protected TextureState _tstate;

    protected static BufferedImage _ready;
    protected static BufferedImage _dfull, _dempty;
    protected static BufferedImage _darkout, _lightout;
    protected static BufferedImage[] _ticks;

    /** Defines the amount by which the damage arc image is inset from a
     * full quarter circle (on each side): 8 degrees. */
    protected static final float ARC_INSETS = 7;
}
