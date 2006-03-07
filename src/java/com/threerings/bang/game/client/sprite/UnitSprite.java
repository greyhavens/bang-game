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
        _stattex.update(_piece, ticks, false);
        _ustate.updateRenderState();
        setStatusVisible();

        // update our colors in the event that our owner changes
        configureOwnerColors();

        // display our nugget if appropriate
        if (unit.benuggeted && _nugget.getParent() == null) {
            attachChild(_nugget);
        } else if (!unit.benuggeted && _nugget.getParent() != null) {
            detachChild(_nugget);
        }

        // if our pending node is showing, update it to reflect our correct
        // ticks until movable
        if (_pendnode != null && ticks > 0) {
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
        setStatusVisible();
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();
        setStatusVisible();
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

        Texture ttex = ((TextureState)_ustate.getRenderState(
            RenderState.RS_TEXTURE)).getTexture();
        ttex.setRotation(_camrot);
        ttex.setTranslation(_camtrans);

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
                RenderUtil.createTextureState(ctx, _pendtexs[ii]).apply();
            }
        }

        // this composite of icons combines to display our status
        _status = new Node("status");
        attachHighlight(_status);

        // this icon is displayed when the mouse is hovered over us
        _hov = new SharedMesh("hov", _highlight);
        _hov.setRenderState(
            RenderUtil.createTextureState(ctx, _hovtex.createSimpleClone()));
        _hov.updateRenderState();
        _status.attachChild(_hov);
        _hov.setCullMode(CULL_ALWAYS);

        _stattex = new StatusTexture(ctx);
        _ustate = new SharedMesh("ustate", _highlight);
        _stattex.update(_piece, _piece.ticksUntilMovable(_tick), false);
        _ustate.setRenderState(_stattex.getTextureState());
        _ustate.updateRenderState();
        _status.attachChild(_ustate);

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
    protected void setStatusVisible ()
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
        if (_piece.isFlyer() && _piece.isAlive()) {
            return computeFlightElevation(board, tx, ty);
        } else {
            return super.computeElevation(board, tx, ty);
        }
    }

    @Override // documentation inherited
    protected void setCoord (
        BangBoard board, Vector3f[] coords, int idx, int nx, int ny)
    {
        // make an exception for the death flights of flyers: only the
        // last coordinate is on the ground
        int elev;
        if (_piece.isFlyer() && !_piece.isAlive() && idx != coords.length-1) {
            elev = computeFlightElevation(board, nx, ny);
        } else {
            elev = computeElevation(board, nx, ny);
        }
        coords[idx] = new Vector3f();
        toWorldCoords(nx, ny, elev, coords[idx]);
    }

    /** Computes the elevation of a flying piece. */
    protected int computeFlightElevation (BangBoard board, int tx, int ty)
    {
        int groundel = Math.max(board.getWaterLevel(),
            super.computeElevation(board, tx, ty)) +
                FLYER_GROUND_HEIGHT * BangBoard.ELEVATION_UNITS_PER_TILE,
            propel = (int)((_view.getPropHeight(tx, ty) / TILE_SIZE +
                FLYER_PROP_HEIGHT) * BangBoard.ELEVATION_UNITS_PER_TILE);
        return Math.max(groundel, propel);
    }

    /** Sets up our colors according to our owning player. */
    protected void configureOwnerColors ()
    {
        _highlight.setDefaultColor(JPIECE_COLORS[_piece.owner]);
        _highlight.updateRenderState();
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
        // we also need to ensure that the textures are bound to ids before
        // we clone them
        _hovtex = ctx.getTextureCache().getTexture(
            "textures/ustatus/selected.png");
        RenderUtil.createTextureState(ctx, _hovtex).apply();

        _tgttst = RenderUtil.createTextureState(
            ctx, "textures/ustatus/crosshairs.png");
    }

    protected Quad _tgtquad;
    protected TerrainNode.Highlight _pendnode;
    protected TextureState _pendtst;
    protected Quaternion _camrot = new Quaternion();
    protected Quaternion _gcamrot = new Quaternion();
    protected Vector3f _camtrans = new Vector3f(), _gcamtrans = new Vector3f();
    protected Texture[] _pendtexs;

    protected Node _status;
    protected SharedMesh _hov, _ustate;
    protected StatusTexture _stattex;
    protected short _pendingTick = -1;
    protected boolean _hovered;

    protected Node _nugget;
    protected Model.Binding _nugbind;

    protected static Texture _hovtex;
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

    /** The height above ground at which flyers fly (in tile lengths). */
    protected static final int FLYER_GROUND_HEIGHT = 1;

    /** The height above props at which flyers fly (in tile lengths). */
    protected static final float FLYER_PROP_HEIGHT = 0.25f;
}
