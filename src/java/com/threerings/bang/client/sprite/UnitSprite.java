//
// $Id$

package com.threerings.bang.client.sprite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.BillboardNode;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;

import com.threerings.bang.client.Model;
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
        if (!_pendingShot) {
            _tgtquad.setSolidColor(ColorRGBA.white);
            _tgtquad.setForceCull(!targeted);
        }
    }

    /**
     * Indicates that we have requested to shoot this piece but it is not
     * yet confirmed by the server.
     */
    public void setPendingShot (boolean pending)
    {
        if (_pendingShot != pending) {
            _pendingShot = pending;
            _tgtquad.setSolidColor(ColorRGBA.red);
            _tgtquad.setForceCull(!pending);
        }
    }

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

        // update our damage texture if necessary
        if (_piece.damage != _odamage) {
            _damtex.setTexture(createDamageTexture());
            _damage.updateRenderState();
            _odamage = _piece.damage;
        }
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
        _hovquad.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        attachChild(_hovquad);
        _hovquad.setForceCull(true);

        // this composite of icons combines to display our status
        _status = new StatusNode();
        _status.setRenderState(RenderUtil.iconAlpha);
        _status.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _status.updateRenderState();
        _status.setLocalTranslation(new Vector3f(0, 0, 0.1f));
        attachChild(_status);
        _ticks = RenderUtil.createIcon(TILE_SIZE/2, TILE_SIZE/2);
        _ticks.setLocalTranslation(new Vector3f(-TILE_SIZE/4, TILE_SIZE/4, 0));
        int tick = _piece.ticksUntilMovable(_tick), tidx = Math.max(0, 4-tick);
        _ticks.setRenderState(_ticktex[tidx]);
        _ticks.updateRenderState();
        _status.attachChild(_ticks);
        _ticks.setSolidColor(JPIECE_COLORS[_piece.owner]);

        _damage = RenderUtil.createIcon(TILE_SIZE/2, TILE_SIZE/2);
        _damage.setLocalTranslation(new Vector3f(TILE_SIZE/4, TILE_SIZE/4, 0));
        _damtex.setTexture(createDamageTexture());
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
        _movable.setForceCull(tick > 0);

        // our models are centered at the origin, but we need to shift
        // them to the center of the tile
        _model = ctx.getModelCache().getModel("units", _type);
        Node[] meshes = _model.getMeshes("standing");
        for (int ii = 0; ii < meshes.length; ii++) {
            attachChild(meshes[ii]);
            meshes[ii].updateRenderState();
        }

        // this icon is displayed when we're a target
        _tgtquad = RenderUtil.createIcon(_tgttex);
        _tgtquad.setLocalTranslation(new Vector3f(0, 0, 0));
        _tgtquad.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _tgtquad.setRenderState(RenderUtil.alwaysZBuf);
        _tgtquad.updateRenderState();
        BillboardNode bbn = new BillboardNode("target");
        bbn.setLocalTranslation(new Vector3f(0, 0, TILE_SIZE/3));
        bbn.attachChild(_tgtquad);
        attachChild(bbn);
        _tgtquad.setForceCull(true);

        // we display a simple shadow texture on the ground beneath us
        _shadow = RenderUtil.createIcon(TILE_SIZE, TILE_SIZE);
        _shadow.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        float height = _piece.isFlyer() ? -2 * TILE_SIZE : 0;
        height += 0.1f;
        _shadow.setLocalTranslation(new Vector3f(0, 0, height));
        _shadow.setRenderState(_shadtex);
        _shadow.updateRenderState();
        attachChild(_shadow);
    }

    @Override // documentation inherited
    protected int computeElevation (BangBoard board, int tx, int ty)
    {
        int offset = 0;
        if (_piece.isFlyer()) {
            offset = board.getElevation(tx, ty);
        }
        return super.computeElevation(board, tx, ty) + offset;
    }

    /** Converts tile coordinates plus elevation into (3D) world
     * coordinates. */
    protected Vector3f toWorldCoords (int tx, int ty, int elev, Vector3f target)
    {
        // flyers are always up in the air
        elev = _piece.isFlyer() ? 2 : elev;
        return super.toWorldCoords(tx, ty, elev, target);
    }

    protected Texture createDamageTexture ()
    {
        int width = _dempty.getWidth(), height = _dempty.getHeight();
        BufferedImage comp = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D gfx = (Graphics2D)comp.getGraphics();
        try {
            gfx.drawImage(_dempty, 0, 0, null);
            float percent = (100 - _piece.damage) / 100f;
            float extent = percent * (90 - 2*ARC_INSETS);
            // expand the width and height a smidge to avoid funny
            // business around the edges
            Arc2D.Float arc = new Arc2D.Float(
                -5*width/4, -height/4, 10*width/4, 10*height/4,
                90 - ARC_INSETS - extent, extent, Arc2D.PIE);
            gfx.setClip(arc);
            gfx.drawImage(_dfull, 0, 0, null);

        } finally {
            gfx.dispose();
        }

        return TextureManager.loadTexture(
            comp, Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, true);
    }

    protected static void loadTextures (BangContext ctx)
    {
        _hovtex = RenderUtil.createTexture(
            ctx, ctx.loadImage("media/textures/ustatus/selected.png"));
        _tgttex = RenderUtil.createTexture(
            ctx, ctx.loadImage("media/textures/ustatus/crosshairs.png"));
        _movetex = RenderUtil.createTexture(
            ctx, ctx.loadImage("media/textures/ustatus/tick_ready.png"));
        _shadtex = RenderUtil.createTexture(
            ctx, ctx.loadImage("media/textures/ustatus/shadow.png"));
        _ticktex = new TextureState[5];
        for (int ii = 0; ii < 5; ii++) {
            _ticktex[ii] = RenderUtil.createTexture(
                ctx, ctx.loadImage(
                    "media/textures/ustatus/tick_counter_" + ii + ".png"));
        }
        _dfull = ctx.loadImage("media/textures/ustatus/health_meter_full.png");
        _dempty = ctx.loadImage("media/textures/ustatus/health_meter_empty.png");
        _damtex = ctx.getRenderer().createTextureState();
        _damtex.setEnabled(true);
    }

    /** A node that rotates itself around the up vector as the camera
     * rotates so as to keep the status textures properly oriented toward
     * the player. */
    protected class StatusNode extends Node
    {
        public StatusNode () {
            super("status");
        }

	public void updateWorldData (float time) {
            _lastUpdate = time;
            updateWorldBound();
	}

	public void draw (Renderer r) {
            Camera cam = r.getCamera();

            // obtain our current world coordinates
            worldScale.set(parent.getWorldScale()).multLocal(localScale);
            worldTranslation = parent.getWorldRotation().mult(
                localTranslation, worldTranslation).multLocal(
                    parent.getWorldScale()).addLocal(
                        parent.getWorldTranslation());
            // we don't want our parent's world rotation, which would
            // normally by obtained like so:
            // parent.getWorldRotation().mult(localRotation, worldRotation);
            worldRotation.set(localRotation);

            // project the camera forward vector onto the "ground":
            // camdir - (camdir . UP) * UP
            Vector3f camdir = cam.getDirection();
            UP.mult(camdir.dot(UP), _tvec);
            camdir.subtract(_tvec, _tvec);
            _tvec.normalizeLocal();

            // compute the angle between LEFT and the camera direction to
            // find the camera rotation around the up vector
            _tvec.normalizeLocal();
            float theta = FastMath.acos(_tvec.dot(LEFT));
            // when y is negative, we need to flip the sign of the angle
            if (_tvec.y < 0) {
                theta *= -1f;
            }
            // we offset theta by PI/2 because our "natural" orientation
            // is a bit sideways
            _tquat.fromAngleAxis(theta + FastMath.PI/2, UP);
            worldRotation.multLocal(_tquat);

            // now we can update our children
            for (int ii = 0, ll = children.size(); ii < ll; ii++) {
                Spatial child = (Spatial)children.get(ii);
                if (child != null) {
                    child.updateGeometricState(_lastUpdate, false);
                }
            }

            super.draw(r);
	}

        protected float _lastUpdate;
    }

    protected String _type;
    protected Model _model;
    protected Quad _hovquad, _tgtquad, _shadow;

    protected StatusNode _status;
    protected Quad _ticks, _damage, _movable;

    protected int _odamage;
    protected boolean _pendingShot;

    protected static Vector3f _tvec = new Vector3f();
    protected static Quaternion _tquat = new Quaternion();

    protected static BufferedImage _dfull, _dempty;
    protected static TextureState _hovtex, _tgttex, _movetex, _damtex, _shadtex;
    protected static TextureState[] _ticktex;

    protected static final float DBAR_WIDTH = TILE_SIZE-2;
    protected static final float DBAR_HEIGHT = (TILE_SIZE-2)/6f;

    /** Defines the amount by which the damage arc image is inset from a
     * full quarter circle (on each side): 8 degrees. */
    protected static final float ARC_INSETS = 7;
}
