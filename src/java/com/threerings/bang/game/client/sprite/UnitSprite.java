//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.EnumSet;
import java.util.HashMap;

import com.jme.image.Texture;
import com.jme.util.geom.BufferUtils;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector2f;
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

import com.samskivert.util.ObserverList;

import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.SpriteObserver;

import com.threerings.media.image.Colorization;

import com.threerings.bang.client.util.ModelAttacher;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.client.sprite.Spinner;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a particular unit.
 */
public class UnitSprite extends MobileSprite
    implements Targetable
{
    /** Used to notify observers of updates to this sprite. */
    public interface UpdateObserver extends SpriteObserver
    {
        /** Called when {@link UnitSprite#updated} is called. */
        public void updated (UnitSprite sprite);
    }

    public static enum AdvanceOrder { NONE, MOVE, MOVE_SHOOT };

    /** For sprites added before the first tick, this flag indicates whether
     * the sprite has started running towards its initial position. */
    public boolean movingToStart;

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
            updateUnitStatus();

            // if we have a pending node, adjust its highlight as well
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

    /**
     * Returns the status texture used by this unit.
     */
    public UnitStatus getUnitStatus ()
    {
        return _status;
    }

    /**
     * Returns this unit's colorizations.
     */
    public Colorization[] getColorizations ()
    {
        return _zations;
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
        updateUnitStatus();
    }

    // documentation inherited from Targetable
    public void setTargeted (TargetMode mode, Unit attacker)
    {
        _target.setTargeted(mode, attacker);
    }

    /**
     * Configures this sprite with a pending order or not.
     */
    public void setAdvanceOrder (AdvanceOrder pendo)
    {
        if (_pendo != pendo) {
            _pendo = pendo;
            updateUnitStatus();
        }
    }

    /**
     * Returns the advance order configured on this sprite.
     */
    public AdvanceOrder getAdvanceOrder ()
    {
        return _pendo;
    }

    // documentation inherited from Targetable
    public void setPendingShot (boolean pending)
    {
        _target.setPendingShot(pending);
    }

    /**
     * Provides this unit with a reference to the tile highlight node on which
     * it should display its "pending action" icon.
     */
    public void setPendingNode (TerrainNode.Highlight pnode)
    {
        _pendnode = pnode;
        int ticks;
        if (_pendnode != null) {
            if ((ticks = _piece.ticksUntilMovable(_tick)) <= 0) {
                log.warning("Am pending but am movable!? " + this);
                ticks = 1;
            }
            _pendtst.setTexture(createPendingTexture(ticks-1));
            _pendnode.setRenderState(_pendtst);
            _pendnode.setDefaultColor(JPIECE_COLORS[_piece.owner]);
            _pendnode.updateRenderState();
        }
    }

    // documentation inhertied from Targetable
    public void configureAttacker (int pidx, int delta)
    {
        _target.configureAttacker(pidx, delta);
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);

        Unit unit = (Unit)piece;
        int ticks = unit.ticksUntilMovable(_tick);

        _target.updated(piece, tick);

        // update our status display
        updateUnitStatus();

        // update our colors in the event that our owner changes
        configureOwnerColors();

        // display our bonus if appropriate
        if (unit.holding != null) {
            if (!unit.holding.equals(_holdingType)) {
                _holding.detachAllChildren();
                _ctx.loadModel("bonuses", unit.holding,
                               new ModelAttacher(_holding));
            }
            if (_holding.getParent() == null) {
                attachChild(_holding);
                _holding.updateRenderState();
            }
        } else if (_holding.getParent() != null) {
            detachChild(_holding);
        }

        // if our pending node is showing, update it to reflect our correct
        // ticks until movable
        if (_pendnode != null && ticks > 0) {
            _pendtst.setTexture(createPendingTexture(ticks-1));
        }

        // notify any updated observers
        if (_observers != null) {
            _observers.apply(_updater);
        }

        // if we were dead but are once again alive, switch back to our rest
        // pose
        if (_dead && piece.isAlive()) {
            log.info("Resurrected " + piece.info());
            loadModel(_ctx, _type, _name, _zations);
            _dead = false;
        }
    }

    @Override // documentation inherited
    public void setLocation (int tx, int ty, int elevation)
    {
        super.setLocation(tx, ty, elevation);
        updateTileHighlight(tx, ty);
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
        updateTileHighlight(_piece.x, _piece.y);
        updateUnitStatus();
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();
        updateTileHighlight(_piece.x, _piece.y);
        updateUnitStatus();
    }

    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);

        // of up and forward, use the one with the greater x/y
        Vector3f dir = _ctx.getCameraHandler().getCamera().getDirection(),
            up = _ctx.getCameraHandler().getCamera().getUp(),
            vec = (dir.x*dir.x+dir.y*dir.y > up.x*up.x+up.y*up.y) ? dir : up;
        float angle = -FastMath.atan2(-vec.x, vec.y);
        _camrot.fromAngleAxis(angle, Vector3f.UNIT_Z);
        _camrot.mult(HALF_UNIT, _camtrans);
        _camtrans.set(0.5f - _camtrans.x, 0.5f - _camtrans.y, 0f);

        // rotate our unit status with the camera
        _status.rotateWithCamera(_camrot, _camtrans);

        // we have to do extra fiddly business here because our texture is
        // additionally scaled and translated to center the texture at half
        // size within the highlight node
        Texture gptex = _pendtst.getTexture();
        _gcamrot.fromAngleAxis(angle, Vector3f.UNIT_Z);
        _gcamrot.mult(WHOLE_UNIT, _gcamtrans);
        _gcamtrans.set(1f - _gcamtrans.x - 0.5f,
                       1f - _gcamtrans.y - 0.5f, 0f);
        gptex.setRotation(_gcamrot);
        gptex.setTranslation(_gcamtrans);
    }

    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        // set our colorization
        _zations = new Colorization[] {
            ctx.getAvatarLogic().getColorPository().getColorization("unit",
                PIECE_COLOR_IDS[_piece.owner] ) };

        // load up our model
        super.createGeometry(ctx);

        // if we're a range unit, make sure the "bullet" model is loaded
        Unit unit = (Unit)_piece;
        if (unit.getConfig().mode == UnitConfig.Mode.RANGE) {
            ctx.loadModel("units", "frontier_town/artillery/shell", null);
        }

        // if we're a mechanized unit, make sure the "dead" model is loaded
        if (unit.getConfig().make == UnitConfig.Make.STEAM) {
            ctx.loadModel(_type, _name + "/dead", null);
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
                RenderUtil.createTextureState(ctx, _pendtexs[ii]).load();
            }
        }
        _pendtst = RenderUtil.createTextureState(
            ctx, createPendingTexture(0));

        // this composite of icons combines to display our status
        _tlight = _view.getTerrainNode().createHighlight(
            _piece.x, _piece.y, true, true);
        attachHighlight(_status = new UnitStatus(ctx, _tlight));
        _status.update(_piece, _piece.ticksUntilMovable(_tick), _pendo, false);

        _target = new PieceTarget(_piece, ctx);
        attachChild(_target);

        // when holding a bonus it is shown over our head
        _holding = new Node("holding");
        _holding.addController(new Spinner(_holding, FastMath.PI/2));
        _holding.setLocalTranslation(new Vector3f(0, 0, TILE_SIZE));
        _holding.setLocalScale(0.5f);

        // configure our colors
        configureOwnerColors();
    }

    /**
     * Updates the position of the highlight geometry that covers or floats
     * over the nearest tile.
     */
    protected void updateTileHighlight (int tx, int ty)
    {
        if (_tlight == null) {
            return;
        }
        if (_tlight.getTileX() != tx || _tlight.getTileY() != ty) {
            _tlight.setPosition(tx, ty);
            _status.updateTranslations(_tlight);
        }
    }

    /**
     * Updates the visibility and location of the status display.
     */
    protected void updateUnitStatus ()
    {
        if (_piece.isAlive() && !isMoving()) {
            int ticks = _piece.ticksUntilMovable(_tick);
            _status.update(_piece, ticks, _pendo, _hovered || _selected);
            _status.setCullMode(CULL_DYNAMIC);
        } else {
            _status.setCullMode(CULL_ALWAYS);
        }
    }

    @Override // documentation inherited
    protected void setCoord (BangBoard board, Vector3f[] coords, int idx,
                             int nx, int ny, boolean moving)
    {
        // make an exception for the death flights of flyers: only the
        // last coordinate is on the ground
        int elev;
        if (_piece.isAirborn() && !_piece.isAlive() && idx != coords.length-1) {
            elev = ((Unit)_piece).computeAreaFlightElevation(board, nx, ny);
        } else {
            elev = _piece.computeElevation(board, nx, ny, moving);
        }
        coords[idx] = new Vector3f();
        toWorldCoords(nx, ny, elev, coords[idx]);
    }

    @Override // documentation inherited
    protected String[] getPreloadSounds ()
    {
        return PRELOAD_SOUNDS;
    }

    /** Sets up our colors according to our owning player. */
    protected void configureOwnerColors ()
    {
        // nothing to do at present
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

    /** Used to dispatch {@link UpdateObserver#updated}. */
    protected ObserverList.ObserverOp<SpriteObserver> _updater =
        new ObserverList.ObserverOp<SpriteObserver>() {
        public boolean apply (SpriteObserver observer) {
            if (observer instanceof UpdateObserver) {
                ((UpdateObserver)observer).updated(UnitSprite.this);
            }
            return true;
        }
    };

    protected TerrainNode.Highlight _tlight, _pendnode;
    protected TextureState _pendtst;
    protected Texture[] _pendtexs;

    protected Quaternion _camrot = new Quaternion();
    protected Quaternion _gcamrot = new Quaternion();
    protected Vector3f _camtrans = new Vector3f();
    protected Vector3f _gcamtrans = new Vector3f();

    protected UnitStatus _status;
    protected PieceTarget _target;
    protected AdvanceOrder _pendo = AdvanceOrder.NONE;
    protected boolean _hovered;

    protected Node _holding;
    protected String _holdingType;

    protected static HashMap<String,Texture[]> _pendtexmap =
        new HashMap<String,Texture[]>();

    protected static final Vector3f HALF_UNIT = new Vector3f(0.5f, 0.5f, 0f);
    protected static final Vector3f WHOLE_UNIT = new Vector3f(1f, 1f, 0f);

    protected static final float DBAR_WIDTH = TILE_SIZE-2;
    protected static final float DBAR_HEIGHT = (TILE_SIZE-2)/6f;

    /** The height above ground at which flyers fly (in tile lengths). */
    protected static final int FLYER_GROUND_HEIGHT = 1;

    /** The height above props at which flyers fly (in tile lengths). */
    protected static final float FLYER_PROP_HEIGHT = 0.25f;

    /** Sounds that we preload for mobile units if they exist. */
    protected static final String[] PRELOAD_SOUNDS = {
        "launch",
        "shooting",
        "returning_fire",
    };
}
