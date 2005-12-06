//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.BillboardNode;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;

import com.threerings.jme.sprite.Path;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a particular unit.
 */
public class UnitSprite extends MobileSprite
{
    public UnitSprite (String type)
    {
        super("units", type);
    }

    /**
     * Indicates that the mouse is hovering over this piece.
     */
    public void setHovered (boolean hovered)
    {
        if (_hovered != hovered) {
            _hovered = hovered;
            _hov.setCullMode((_selected || _hovered) ?
                             CULL_DYNAMIC : CULL_ALWAYS);
        }
    }

    @Override // documentation inherited
    public void setSelected (boolean selected)
    {
        super.setSelected(selected);
        _hov.setCullMode((_selected || _hovered) ? CULL_DYNAMIC : CULL_ALWAYS);
    }

    /**
     * Indicates that this piece is a potential target.
     */
    public void setTargeted (boolean targeted)
    {
        if (_pendingTick == -1) {
            _tgtquad.setSolidColor(ColorRGBA.white);
            _tgtquad.setCullMode(targeted ? CULL_DYNAMIC : CULL_ALWAYS);
        }
    }

    /**
     * Indicates that we have requested to shoot this piece but it is not
     * yet confirmed by the server.
     */
    public void setPendingShot (boolean pending)
    {
        if (pending) {
            if (_pendingTick == -1) {
                _tgtquad.setSolidColor(ColorRGBA.red);
            }
            _pendingTick = _tick;
        } else {
            _pendingTick = -1;
        }
        _tgtquad.setCullMode(pending ? CULL_DYNAMIC : CULL_ALWAYS);
    }

    /**
     * Indicates that we have queued up an action to be taken when our
     * piece is once again able to move and shoot.
     */
    public void setPendingAction (boolean pending)
    {
        _pendquad.setCullMode(pending ? CULL_DYNAMIC : CULL_ALWAYS);
        int ticks;
        if (pending && (ticks = _piece.ticksUntilMovable(_tick)) > 0) {
            _pendquad.setTextureBuffer(_pendtc[ticks-1]);
        }
    }

    @Override // documentation inherited
    public void updated (BangBoard board, Piece piece, short tick)
    {
        super.updated(board, piece, tick);

        Unit unit = (Unit)piece;
        int ticks = unit.ticksUntilMovable(_tick);

        // clear our pending shot once we've been ticked
        if (_pendingTick != -1 && tick > _pendingTick) {
            setPendingShot(false);
        }

        // update our status display
        updateStatus();
        TextureState tstate =
            (TextureState)_ticks.getRenderState(RenderState.RS_TEXTURE);
        if (ticks > 0) {
            tstate.setTexture(
                _ticktex[Math.max(0, 4-ticks)].createSimpleClone());
            _movable.setCullMode(CULL_ALWAYS);
        } else {
            tstate.setTexture(_ticktex[4].createSimpleClone());
            //_ticks.updateRenderState();
            _movable.setCullMode(CULL_DYNAMIC);
        }

        // update our colors in the event that our owner changes
        configureOwnerColors();

        // update our damage texture if necessary
        if (unit.damage != _odamage) {
            _damtex.setTexture(createDamageTexture());
            _odamage = unit.damage;
        }

        // update our icon if necessary
        if (unit.benuggeted && _icon.getCullMode() == CULL_ALWAYS) {
            _icon.setRenderState(_nugtex);
            _icon.updateRenderState();
            _icon.setCullMode(CULL_DYNAMIC);
        } else if (!unit.benuggeted && _icon.getCullMode() != CULL_ALWAYS) {
            _icon.setCullMode(CULL_ALWAYS);
        }

        // if our pending quad is showing, update it to reflect our correct
        // ticks until movable
        if (_pendquad.getCullMode() == CULL_DYNAMIC) {
            _pendquad.setTextureBuffer(_pendtc[ticks-1]);
        }
    }

    @Override // documentation inherited
    public boolean isSelectable ()
    {
//        return (_piece.ticksUntilMovable(_tick) == 0);
        return true;
    }

    @Override // documentation inherited
    public void move (Path path)
    {
        super.move(path);
        _status.setCullMode(CULL_ALWAYS);
    }

    @Override // documentation inherited
    public void cancelMove ()
    {
        super.cancelMove();
        updateStatus();
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();
        updateStatus();
    }

    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);

        // of up and forward, use the one with the greater x/y
        Vector3f dir = _ctx.getCameraHandler().getCamera().getDirection(),
            up = _ctx.getCameraHandler().getCamera().getUp(),
            vec = (dir.x*dir.x+dir.y*dir.y > up.x*up.x+up.y*up.y) ? dir : up;
        float angle = FastMath.PI + FastMath.atan2(-vec.x, vec.y);
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(-angle, Vector3f.UNIT_Z);
        Vector3f trans = rot.mult(new Vector3f(0.5f, 0.5f, 0f));
        trans.set(0.5f - trans.x, 0.5f - trans.y, 0f);

        Texture mtex = ((TextureState)_movable.getRenderState(
            RenderState.RS_TEXTURE)).getTexture();
        mtex.setRotation(rot);
        mtex.setTranslation(trans);

        Texture ttex = ((TextureState)_ticks.getRenderState(
            RenderState.RS_TEXTURE)).getTexture();
        ttex.setRotation(rot);
        ttex.setTranslation(trans);

        Texture dtex = ((TextureState)_damage.getRenderState(
            RenderState.RS_TEXTURE)).getTexture();
        dtex.setRotation(rot);
        dtex.setTranslation(trans);
    }

    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        if (_hovtex == null) {
            loadTextures(ctx);
        }

        _ctx = ctx;

        // load up our model
        super.createGeometry(ctx);

        // this composite of icons combines to display our status
        _status = new Node("status");
        _hnode.attachChild(_status);

        // this icon is displayed when the mouse is hovered over us
        _hov = new SharedMesh("hov", _highlight);
        TextureState tstate = ctx.getRenderer().createTextureState();
        tstate.setTexture(_hovtex.createSimpleClone());
        _hov.setRenderState(tstate);
        _hov.updateRenderState();
        _status.attachChild(_hov);
        _hov.setCullMode(CULL_ALWAYS);

        _ticks = new SharedMesh("ticks", _highlight);
        int tick = _piece.ticksUntilMovable(_tick), tidx = Math.max(0, 4-tick);
        tstate = ctx.getRenderer().createTextureState();
        tstate.setTexture(_ticktex[tidx].createSimpleClone());
        _ticks.setRenderState(tstate);
        _ticks.updateRenderState();
        _status.attachChild(_ticks);

        _damage = new SharedMesh("damage", _highlight);
        _damtex = ctx.getRenderer().createTextureState();
        _damtex.setEnabled(true);
        _damtex.setTexture(createDamageTexture());
        _damage.setRenderState(_damtex);
        _damage.updateRenderState();
        _status.attachChild(_damage);

        _movable = new SharedMesh("movable", _highlight);
        tstate = ctx.getRenderer().createTextureState();
        tstate.setTexture(_movetex.createSimpleClone());
        _movable.setRenderState(tstate);
        _movable.updateRenderState();
        _status.attachChild(_movable);
        _movable.setCullMode(tick > 0 ? CULL_ALWAYS : CULL_DYNAMIC);

        // configure our colors
        configureOwnerColors();

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
        _tgtquad.setCullMode(CULL_ALWAYS);

        // this icon is displayed when we have a pending action queued
        _pendquad = RenderUtil.createIcon(4, 4);
        _pendquad.setRenderState(_pendtex);
        _pendquad.setLocalTranslation(new Vector3f(0, 0, 0));
        _pendquad.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _pendquad.setRenderState(RenderUtil.alwaysZBuf);
        _pendquad.updateRenderState();
        bbn = new BillboardNode("pending");
        bbn.setLocalTranslation(new Vector3f(0, 0, TILE_SIZE));
        bbn.attachChild(_pendquad);
        attachChild(bbn);
        _pendquad.setCullMode(CULL_ALWAYS);

        // this icon is displayed when we are modified in some way (we're
        // carrying a nugget, for example)
        _icon = RenderUtil.createIcon(5, 5);
        _icon.setLocalTranslation(new Vector3f(0, 0, 0));
        _icon.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _icon.setRenderState(RenderUtil.alwaysZBuf);
        _icon.updateRenderState();
        bbn = new BillboardNode("icon");
        bbn.setLocalTranslation(new Vector3f(0, 0, TILE_SIZE/3));
        bbn.attachChild(_icon);
        attachChild(bbn);
        _icon.setCullMode(CULL_ALWAYS);
    }

    /**
     * Updates the visibility and location of the status display.
     */
    protected void updateStatus ()
    {
        if (_piece.isAlive() /* && !isMoving() */) {
            _status.setCullMode(CULL_DYNAMIC);

        } else {
            _status.setCullMode(CULL_ALWAYS);
        }
    }

    @Override // documentation inherited
    protected int computeElevation (BangBoard board, int tx, int ty)
    {
        int elev = super.computeElevation(board, tx, ty);
        if (_piece.isFlyer()) {
            // flying pieces hover 1 "units" above the ground; not sinking into
            // holes, but raising up to two units above hills
            elev = Math.max(elev, 0) + 1 * BangBoard.ELEVATION_UNITS_PER_TILE;
        }
        return elev;
    }

    /** Sets up our colors according to our owning player. */
    protected void configureOwnerColors ()
    {
        _highlight.setDefaultColor(JPIECE_COLORS[_piece.owner]);
        _highlight.updateRenderState();
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
                -width/8, -height/8, 10*width/8, 10*height/8,
                90 - ARC_INSETS - extent, extent, Arc2D.PIE);
            gfx.setClip(arc);
            gfx.drawImage(_dfull, 0, 0, null);

        } finally {
            gfx.dispose();
        }

        Texture dtex = TextureManager.loadTexture(
            comp, Texture.MM_LINEAR_LINEAR, Texture.FM_LINEAR, true);
        dtex.setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
        return dtex;
    }

    protected static void loadTextures (BasicContext ctx)
    {
        _hovtex = RenderUtil.createTexture(
            ctx.loadImage("textures/ustatus/selected.png"));

        _tgttex = RenderUtil.createTextureState(
            ctx, ctx.loadImage("textures/ustatus/crosshairs.png"));
        _pendtex = RenderUtil.createTextureState(
            ctx, ctx.loadImage("textures/ustatus/pending.png"));
        _movetex = RenderUtil.createTexture(
            ctx.loadImage("textures/ustatus/tick_ready.png"));
        _movetex.setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);

        _nugtex = RenderUtil.createTextureState(
            ctx, ctx.loadImage("textures/ustatus/nugget.png"));
        _ticktex = new Texture[5];
        for (int ii = 0; ii < 5; ii++) {
            _ticktex[ii] = RenderUtil.createTexture(
                ctx.loadImage("textures/ustatus/tick_counter_" + ii + ".png"));
            _ticktex[ii].setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
        }
        _dfull = ctx.loadBufferedImage("textures/ustatus/health_meter_full.png");
        _dempty = ctx.loadBufferedImage(
            "textures/ustatus/health_meter_empty.png");
    }

    protected BasicContext _ctx;
    protected Quad _tgtquad, _pendquad;

    protected Node _status;
    protected SharedMesh _hov, _ticks, _damage, _movable;
    protected Quad _icon;
    protected TextureState _damtex;
    protected int _odamage;
    protected short _pendingTick = -1;
    protected boolean _hovered;

    protected static Vector3f _tvec = new Vector3f();
    protected static Quaternion _tquat = new Quaternion();

    protected static BufferedImage _dfull, _dempty;
    protected static Texture _hovtex, _movetex;
    protected static Texture[] _ticktex;
    protected static TextureState _tgttex, _pendtex, _nugtex;

    protected static FloatBuffer[] _pendtc = RenderUtil.createGridTexCoords(2);

    protected static final float DBAR_WIDTH = TILE_SIZE-2;
    protected static final float DBAR_HEIGHT = (TILE_SIZE-2)/6f;

    /** Defines the amount by which the damage arc image is inset from a
     * full quarter circle (on each side): 8 degrees. */
    protected static final float ARC_INSETS = 7;
}
