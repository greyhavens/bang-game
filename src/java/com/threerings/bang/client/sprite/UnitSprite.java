//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import com.samskivert.util.HashIntMap;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.BillboardNode;
import com.jme.scene.Node;
import com.jme.scene.shape.Box;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;

import com.threerings.jme.sprite.LineSegmentPath;
import com.threerings.jme.sprite.Path;
import com.threerings.media.image.ImageUtil;
import com.threerings.media.util.MathUtil;

import com.threerings.bang.data.BangBoard;
import com.threerings.bang.data.piece.Dirigible;
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
        new ColorRGBA(1, 1, 0, 1)
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

//     @Override // documentation inherited
//     public void setSelected (boolean selected)
//     {
//         super.setSelected(selected);
//     }

    @Override // documentation inherited
    public void updated (BangBoard board, Piece piece, short tick)
    {
        super.updated(board, piece, tick);

        int ticks;
        if (!_piece.isAlive()) {
            _status.setForceCull(true);
        } else if ((ticks = _piece.ticksUntilMovable(_tick)) > 0) {
            _ticks.setRenderState(_ticktex[Math.max(0, 4-ticks)]);
            _ticks.updateRenderState();
            _movable.setForceCull(true);
        } else {
            _ticks.setRenderState(_ticktex[4]);
            _ticks.updateRenderState();
            _movable.setForceCull(false);
        }

        // TODO: deal with damage

//         float size = (100 - piece.damage) * DBAR_WIDTH / 100f;
//         _damage.resize(size, DBAR_HEIGHT);
//         _damage.getLocalTranslation().x = size/2 + 1;
//         _damage.updateGeometricState(0, true);
    }

    @Override // documentation inherited
    public boolean isSelectable ()
    {
        return ((_piece.ticksUntilMovable(_tick) == 0) ||
                (_piece.ticksUntilFirable(_tick) == 0));
    }

    @Override // documentation inherited
    protected void createGeometry (BangContext ctx)
    {
        if (_hovtex == null) {
            loadTextures(ctx);
        }

        // this icon is displayed when the mouse is hovered over us
        _hovquad = RenderUtil.createIcon(_hovtex);
        attachChild(_hovquad);
        _hovquad.setForceCull(true);

        // this composite of icons combines to display our status
        _status = new Node("status");
        _status.setLocalTranslation(
            new Vector3f(TILE_SIZE/2, TILE_SIZE/2, 0));
        attachChild(_status);
        _ticks = RenderUtil.createIcon(TILE_SIZE/2, TILE_SIZE/2);
        _ticks.setLocalTranslation(new Vector3f(-TILE_SIZE/4, TILE_SIZE/4, 0));
        int tick = Math.max(0, 4-_piece.ticksUntilMovable(_tick));
        _ticks.setRenderState(_ticktex[tick]);
        _ticks.updateRenderState();
        _status.attachChild(_ticks);
        _ticks.setSolidColor(JPIECE_COLORS[_piece.owner]);

        _damage = RenderUtil.createIcon(TILE_SIZE/2, TILE_SIZE/2);
        _damage.setLocalTranslation(new Vector3f(TILE_SIZE/4, TILE_SIZE/4, 0));
        _damage.setRenderState(_damtex);
        _damage.updateRenderState();
        _status.attachChild(_damage);
        _damage.setSolidColor(JPIECE_COLORS[_piece.owner]);

        _movable = RenderUtil.createIcon(TILE_SIZE, TILE_SIZE/2);
        _movable.setLocalTranslation(new Vector3f(0, -TILE_SIZE/4, 0));
        _movable.setRenderState(_movetex);
        _movable.updateRenderState();
        _status.attachChild(_movable);
        attachChild(_status);
        _movable.setSolidColor(JPIECE_COLORS[_piece.owner]);

        _model = ctx.getModelCache().getModel(_type);
        attachChild(_model);
        _model.updateRenderState();

        // this icon is displayed when we're a target
        _tgtquad = RenderUtil.createIcon(_tgttex);
        _tgtquad.setLocalTranslation(new Vector3f(0, 0, 0));
        _tgtquad.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _tgtquad.setRenderState(RenderUtil.alwaysZBuf);
        _tgtquad.updateRenderState();
        BillboardNode bbn = new BillboardNode("target");
        bbn.setLocalTranslation(
            new Vector3f(TILE_SIZE/2, TILE_SIZE/2, TILE_SIZE/3));
        bbn.attachChild(_tgtquad);
        attachChild(bbn);
        _tgtquad.setForceCull(true);

//         // this will display our damage
//         _damage = new Quad("damage", DBAR_WIDTH, DBAR_HEIGHT);
//         _damage.setLocalTranslation(
//             new Vector3f(DBAR_WIDTH/2+1, DBAR_HEIGHT/2+1, 0));
//         _damage.setSolidColor(ColorRGBA.green);
//         _damage.setLightCombineMode(LightState.OFF);
//         attachChild(_damage);
    }

    @Override // documentation inherited
    protected int computeElevation (BangBoard board, int tx, int ty)
    {
        int offset = 0;
        if (_piece instanceof Dirigible) {
            offset = board.getElevation(tx, ty);
        }
        return super.computeElevation(board, tx, ty) + offset;
    }

    @Override // documentation inherited
    protected Path createPath (BangBoard board, Piece opiece, Piece npiece)
    {
        if (_piece instanceof Dirigible) {
            ArrayList<Vector3f> nodes = new ArrayList<Vector3f>();
            int oelev = computeElevation(board, opiece.x, opiece.y);
            if (oelev == 0) {
                nodes.add(toWorldCoords(opiece.x, opiece.y, 0, new Vector3f()));
            }
            nodes.add(toWorldCoords(opiece.x, opiece.y, 1, new Vector3f()));
            nodes.add(toWorldCoords(npiece.x, npiece.y, 1, new Vector3f()));
            int nelev = computeElevation(board, npiece.x, npiece.y);
            if (nelev == 0) {
                nodes.add(toWorldCoords(npiece.x, npiece.y, 0, new Vector3f()));
            }

            Vector3f[] coords = nodes.toArray(new Vector3f[nodes.size()]);
            float[] durations = new float[coords.length-1];
            Arrays.fill(durations, 0.1f);
            durations[oelev == 0 ? 1 : 0] = (float)MathUtil.distance(
                opiece.x, opiece.y, npiece.x, npiece.y) * .1f;
            return new LineSegmentPath(this, coords, durations);

        } else {
            return super.createPath(board, opiece, npiece);
        }
    }

    protected static void loadTextures (BangContext ctx)
    {
        _hovtex = RenderUtil.createTexture(
            ctx, ctx.loadImage("media/textures/ustatus/selected.png"));
        _tgttex = RenderUtil.createTexture(
            ctx, ctx.loadImage("media/textures/ustatus/crosshairs.png"));
        _movetex = RenderUtil.createTexture(
            ctx, ctx.loadImage("media/textures/ustatus/tick_ready.png"));
        _damtex = RenderUtil.createTexture(
            ctx, ctx.loadImage("media/textures/ustatus/health_meter_full.png"));
        _ticktex = new TextureState[5];
        for (int ii = 0; ii < 5; ii++) {
            _ticktex[ii] = RenderUtil.createTexture(
                ctx, ctx.loadImage(
                    "media/textures/ustatus/tick_counter_" + ii + ".png"));
        }
    }

    protected String _type;
    protected Node _model;
    protected Quad _hovquad, _tgtquad;

    protected Node _status;
    protected Quad _ticks, _damage, _movable;

    protected static TextureState _hovtex, _tgttex, _movetex, _damtex;
    protected static TextureState[] _ticktex;

    protected static final float DBAR_WIDTH = TILE_SIZE-2;
    protected static final float DBAR_HEIGHT = (TILE_SIZE-2)/6f;
}
