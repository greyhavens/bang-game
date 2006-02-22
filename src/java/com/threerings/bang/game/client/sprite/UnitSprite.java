//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

import java.util.HashMap;

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

import com.threerings.bang.client.Model;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.client.sprite.Spinner;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

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
            if (_pendnode != null) {
                if (_hovered) {
                    _pendnode.setDefaultColor(ColorRGBA.white);
                } else {
                    _pendnode.setDefaultColor(JPIECE_COLORS[_piece.owner]);
                }
                _pendnode.updateRenderState();
            }
        }
    }

    @Override // documentation inherited
        public String getHelpIdent (int pidx)
    {
        return "unit_" + ((Unit)_piece).getType();
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
            _tgtquad.setDefaultColor(ColorRGBA.white);
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
                _tgtquad.setDefaultColor(ColorRGBA.red);
            }
            _pendingTick = _tick;
        } else {
            _pendingTick = -1;
        }
        _tgtquad.setCullMode(pending ? CULL_DYNAMIC : CULL_ALWAYS);
    }

    /**
     * Provides this unit with a reference to the tile highlight node on which
     * it should display its "pending action" icon.
     */
    public void setPendingNode (TerrainNode.Highlight pnode)
    {
        _pendnode = pnode;
        int ticks;
        if (_pendnode != null &&
            (ticks = _piece.ticksUntilMovable(_tick)) > 0) {
            _pendtst.setTexture(createPendingTexture(ticks-1));
            _pendnode.setRenderState(_pendtst);
            _pendnode.setDefaultColor(JPIECE_COLORS[_piece.owner]);
            _pendnode.updateRenderState();
        }
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);

        Unit unit = (Unit)piece;
        int ticks = unit.ticksUntilMovable(_tick);

        // clear our pending shot once we've been ticked (or if we die)
        if (!piece.isAlive() || (_pendingTick != -1 && tick > _pendingTick)) {
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
            _movable.setCullMode(CULL_DYNAMIC);
        }
        _ticks.updateRenderState();

        // update our colors in the event that our owner changes
        configureOwnerColors();

        // update our damage texture if necessary
        if (unit.damage != _odamage) {
            _damtex.setTexture(createDamageTexture());
            _odamage = unit.damage;
        }

        // display our nugget if appropriate
        if (unit.benuggeted && _nugget.getParent() == null) {
            attachChild(_nugget);
        } else if (!unit.benuggeted && _nugget.getParent() != null) {
            detachChild(_nugget);
        }

        // if our pending node is showing, update it to reflect our correct
        // ticks until movable
        if (_pendnode != null) {
            _pendtst.setTexture(createPendingTexture(ticks-1));
        }
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
        _camrot.fromAngleAxis(-angle, Vector3f.UNIT_Z);
        _camrot.mult(HALF_UNIT, _camtrans);
        _camtrans.set(0.5f - _camtrans.x, 0.5f - _camtrans.y, 0f);

        Texture mtex = ((TextureState)_movable.getRenderState(
            RenderState.RS_TEXTURE)).getTexture();
        mtex.setRotation(_camrot);
        mtex.setTranslation(_camtrans);

        Texture ttex = ((TextureState)_ticks.getRenderState(
            RenderState.RS_TEXTURE)).getTexture();
        ttex.setRotation(_camrot);
        ttex.setTranslation(_camtrans);

        Texture dtex = ((TextureState)_damage.getRenderState(
            RenderState.RS_TEXTURE)).getTexture();
        dtex.setRotation(_camrot);
        dtex.setTranslation(_camtrans);

        // we have to do extra fiddly business here because our texture is
        // additionally scaled and translated to center the texture at half
        // size within the highlight node; plus we want it to be 180 degrees
        // rotated from the status orientation
        Texture gptex = _pendtst.getTexture();
        _gcamrot.fromAngleAxis(FastMath.PI-angle, Vector3f.UNIT_Z);
        _gcamrot.mult(WHOLE_UNIT, _gcamtrans);
        _gcamtrans.set(1f - _gcamtrans.x - 0.5f,
                       1f - _gcamtrans.y - 0.5f, 0f);
        gptex.setRotation(_gcamrot);
        gptex.setTranslation(_gcamtrans);
    }

    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        if (_hovtex == null) {
            loadTextures(ctx);
        }

        // load up our model
        super.createGeometry(ctx);

        // if we're a range unit, make sure the "bullet" model is loaded
        Unit unit = (Unit)_piece;
        if (unit.getConfig().mode == UnitConfig.Mode.RANGE) {
            ctx.loadModel("units", "artillery/shell").resolveActions();
        }

        // make sure the pending move textures for our unit type are loaded
        String type = unit.getType();
        _pendtexs = _pendtexmap.get(type);
        if (_pendtexs == null) {
            _pendtexmap.put(type, _pendtexs = new Texture[4]);
            for (int ii = 0; ii < _pendtexs.length; ii++) {
                _pendtexs[ii] = ctx.getTextureCache().getTexture(
                    "units/" + type + "/pending.png", 64, 64, 2, ii);
                _pendtexs[ii].setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
            }
        }

        // this composite of icons combines to display our status
        _status = new Node("status");
        _hnode.attachChild(_status);

        // this icon is displayed when the mouse is hovered over us
        _hov = new SharedMesh("hov", _highlight);
        _hov.setRenderState(
            RenderUtil.createTextureState(ctx, _hovtex.createSimpleClone()));
        _hov.updateRenderState();
        _status.attachChild(_hov);
        _hov.setCullMode(CULL_ALWAYS);

        _ticks = new SharedMesh("ticks", _highlight);
        int tick = _piece.ticksUntilMovable(_tick), tidx = Math.max(0, 4-tick);
        _ticks.setRenderState(RenderUtil.createTextureState(
                                  ctx, _ticktex[tidx].createSimpleClone()));
        _ticks.updateRenderState();
        _status.attachChild(_ticks);

        _damage = new SharedMesh("damage", _highlight);
        _damtex = RenderUtil.createTextureState(ctx, createDamageTexture());
        _damage.setRenderState(_damtex);
        _damage.updateRenderState();
        _status.attachChild(_damage);

        _movable = new SharedMesh("movable", _highlight);
        _movable.setRenderState(
            RenderUtil.createTextureState(ctx, _movetex.createSimpleClone()));
        _movable.updateRenderState();
        _status.attachChild(_movable);
        _movable.setCullMode(tick > 0 ? CULL_ALWAYS : CULL_DYNAMIC);

        // this icon is displayed when we're a target
        _tgtquad = RenderUtil.createIcon(_tgttst);
        _tgtquad.setLocalTranslation(new Vector3f(0, 0, 0));
        _tgtquad.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        _tgtquad.setRenderState(RenderUtil.alwaysZBuf);
        _tgtquad.updateRenderState();
        BillboardNode bbn = new BillboardNode("target");
        bbn.setLocalTranslation(new Vector3f(0, 0, TILE_SIZE/3));
        bbn.attachChild(_tgtquad);
        attachChild(bbn);
        _tgtquad.setCullMode(CULL_ALWAYS);

        _pendtst = RenderUtil.createTextureState(
            ctx, createPendingTexture(0));

        // the nugget is shown when we're carrying a nugget
        _nugget = new Node("nugget");
        _nugget.addController(new Spinner(_nugget, FastMath.PI/2));
        _nugget.setLocalTranslation(new Vector3f(0, 0, TILE_SIZE));
        _nugget.setLocalScale(0.5f);
        Model nugmod = ctx.loadModel("bonuses", "nugget");
        _nugbind = nugmod.getAnimation("normal").bind(_nugget, 0, null);

        // configure our colors
        configureOwnerColors();
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
    protected void setParent (Node parent)
    {
        super.setParent(parent);

        // clear our nugget model binding when we're removed
        if (parent == null) {
            if (_nugbind != null) {
                _nugbind.detach();
            }
        }
    }

    @Override // documentation inherited
    protected int computeElevation (BangBoard board, int tx, int ty)
    {
        int elev = super.computeElevation(board, tx, ty);
        if (_piece.isFlyer() && _piece.isAlive()) {
            // flying pieces hover 1 "units" above the ground while they're
            // alive
            elev += FLYER_HEIGHT * BangBoard.ELEVATION_UNITS_PER_TILE;
        }
        return elev;
    }

    @Override // documentation inherited
    protected void setCoord (
        BangBoard board, Vector3f[] coords, int idx, int nx, int ny)
    {
        // make an exception for the death flights of flyers: only the
        // last coordinate is on the ground
        int elev = computeElevation(board, nx, ny);
        if (_piece.isFlyer() && !_piece.isAlive() && idx != coords.length-1) {
            elev += FLYER_HEIGHT * BangBoard.ELEVATION_UNITS_PER_TILE;
        }
        coords[idx] = new Vector3f();
        toWorldCoords(nx, ny, elev, coords[idx]);
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

    protected Texture createPendingTexture (int tidx)
    {
        Texture gpendtex = _pendtexs[tidx].createSimpleClone();
        // start with a translation that will render nothing until we are
        // properly updated with our camera rotation on the next call to
        // updateWorldData()
        gpendtex.setTranslation(new Vector3f(-2f, -2f, 0));
        // the ground textures are "shrunk" by 50% and centered
        gpendtex.setScale(new Vector3f(2f, 2f, 0));
        return gpendtex;
    }

    protected static void loadTextures (BasicContext ctx)
    {
        _hovtex = ctx.getTextureCache().getTexture(
            "textures/ustatus/selected.png");

        _tgttst = RenderUtil.createTextureState(
            ctx, "textures/ustatus/crosshairs.png");
        _movetex = ctx.getTextureCache().getTexture(
            "textures/ustatus/tick_ready.png");
        _movetex.setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);

        _ticktex = new Texture[5];
        for (int ii = 0; ii < 5; ii++) {
            _ticktex[ii] = ctx.getTextureCache().getTexture(
                "textures/ustatus/tick_counter_" + ii + ".png");
            _ticktex[ii].setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
        }
        _dfull = ctx.getImageCache().getBufferedImage(
            "textures/ustatus/health_meter_full.png");
        _dempty = ctx.getImageCache().getBufferedImage(
            "textures/ustatus/health_meter_empty.png");
    }

    protected Quad _tgtquad;
    protected TerrainNode.Highlight _pendnode;
    protected TextureState _pendtst;
    protected Quaternion _camrot = new Quaternion();
    protected Quaternion _gcamrot = new Quaternion();
    protected Vector3f _camtrans = new Vector3f(), _gcamtrans = new Vector3f();
    protected Texture[] _pendtexs;

    protected Node _status;
    protected SharedMesh _hov, _ticks, _damage, _movable;
    protected TextureState _damtex;
    protected int _odamage;
    protected short _pendingTick = -1;
    protected boolean _hovered;

    protected Node _nugget;
    protected Model.Binding _nugbind;

    protected static BufferedImage _dfull, _dempty;
    protected static Texture _hovtex, _movetex;
    protected static Texture[] _ticktex;
    protected static TextureState _tgttst;

    protected static HashMap<String,Texture[]> _pendtexmap =
        new HashMap<String,Texture[]>();

    protected static final Vector3f HALF_UNIT = new Vector3f(0.5f, 0.5f, 0f);
    protected static final Vector3f WHOLE_UNIT = new Vector3f(1f, 1f, 0f);

    protected static final float DBAR_WIDTH = TILE_SIZE-2;
    protected static final float DBAR_HEIGHT = (TILE_SIZE-2)/6f;

    /** Defines the amount by which the damage arc image is inset from a
     * full quarter circle (on each side): 8 degrees. */
    protected static final float ARC_INSETS = 7;
    
    /** The height above terrain in tile lengths at which flyers fly. */
    protected static final int FLYER_HEIGHT = 1;
}
