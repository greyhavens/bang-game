//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.HashMap;

import com.jme.image.Texture;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

import com.jme.renderer.ColorRGBA;

import com.jme.scene.Node;
import com.jme.scene.Spatial;

import com.jme.scene.state.TextureState;

import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ObserverList;
import com.threerings.util.MessageBundle;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.SpriteObserver;
import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.ParticleUtil;
import com.threerings.bang.util.RenderUtil;
import com.threerings.bang.util.SoundUtil;

import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.TerrainNode;
import com.threerings.bang.game.client.effect.ExplosionViz;
import com.threerings.bang.game.client.effect.InfluenceViz;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Influence;
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
        UnitConfig uconfig = UnitConfig.getConfig(type, true);
        _name = uconfig.model;
        _variant = uconfig.variant;
    }

    @Override // documentation inherited
    public Coloring getColoringType ()
    {
        return Coloring.STATIC;
    }

    @Override // documentation inherited
    public boolean isHoverable ()
    {
        return true;
    }

    @Override // documentation inherited
    public void setHovered (boolean hovered)
    {
        if (_hovered != hovered) {
            // if we have a pending node, adjust its highlight as well
            if (_pendnode != null) {
                if (hovered) {
                    _pendnode.getBatch(0).getDefaultColor().set(ColorRGBA.white);
                } else {
                    _pendnode.getBatch(0).getDefaultColor().set(getJPieceColor(_piece.owner));
                }
                _pendnode.updateRenderState();
            }
        }
        super.setHovered(hovered);
    }

    /**
     * Sets the player index observing this sprite.
     */
    public void setPidx (int pidx)
    {
        _pidx = pidx;
    }

    /**
     * Returns the status texture used by this unit.
     */
    public UnitStatus getUnitStatus ()
    {
        return _ustatus;
    }

    /**
     * Returns a reference to the node containing anything held by the
     * unit.
     */
    public Node getHoldingNode ()
    {
        return _holding;
    }

    /**
     * Returns the delay to use before releasing the shot sprite from the
     * unit sprite (in order to match the shooting animation) if this is
     * a ballistic unit.
     */
    public float getBallisticShotDelay ()
    {
        return _ballisticShotDelay;
    }

    /**
     * Returns the source node whose location should be used as the starting
     * point of ballistic shots, or <code>null</code> if a source was not
     * configured or is outside of the view frustum.
     */
    public Spatial getBallisticShotSource ()
    {
        return (_ballisticShotSource != null &&
            !RenderUtil.isOutsideFrustum(_ballisticShotSource)) ?
                _ballisticShotSource : null;
    }

    // documentation inherited from Targetable
    public void setTargeted (BangObject bangobj, TargetMode mode, Unit attacker)
    {
        _target.setTargeted(bangobj, mode, attacker);
    }

    /**
     * Configures this sprite with a pending order or not.
     */
    public void setAdvanceOrder (AdvanceOrder pendo)
    {
        if (_pendo != pendo) {
            _pendo = pendo;
            updateStatus();
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

    // from interface Targetable
    public void setPossibleShot (boolean possible)
    {
        _target.setPossibleShot(possible);
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
            _pendnode.getBatch(0).getDefaultColor().set(getJPieceColor(_piece.owner));
            _pendnode.updateRenderState();
        }
    }

    // documentation inhertied from Targetable
    public void configureAttacker (int pidx, int delta)
    {
        _target.configureAttacker(pidx, delta);
    }

    @Override // documentation inherited
    public String getTooltip (int pidx)
    {
        Unit unit = (Unit)_piece;
        String type = unit.getType();
        String msg = "m.unit_icon";
        String[] args = new String[] {
            UnitConfig.getName(type), UnitConfig.getTip(type) };
        if (unit.holding != null) {
            msg = "m.holding_unit_icon";
            String hroot = unit.holding.substring(
                unit.holding.lastIndexOf("/")+1);
            args = new String[] { args[0], args[1], null, null };
            args[2] = MessageBundle.qualify(
                GameCodes.GAME_MSGS, "m.help_bonus_" + hroot + "_title");
            args[3] = MessageBundle.qualify(
                GameCodes.GAME_MSGS, "m.help_bonus_" + hroot);
        }
        return MessageBundle.qualify(
            BangCodes.UNITS_MSGS, MessageBundle.compose(msg, args));
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);

        Unit unit = (Unit)piece;
        int ticks = unit.ticksUntilMovable(_tick);

        _target.updated(piece, tick);

        // update our colors in the event that our owner changes
        configureOwnerColors();

        // display our bonus if appropriate
        if (unit.holding != null) {
            if (!unit.holding.equals(_holdingType)) {
                _holding.detachAllChildren();
                _ctx.getModelCache().getModel("bonuses", unit.holding,
                        _zations, new ResultAttacher<Model>(_holding));
            }
            if (_holding.getParent() == null) {
                _holding.setLocalTranslation(new Vector3f(0, 0, getHeight()));
                attachChild(_holding);
                _holding.updateRenderState();
            }
        } else if (_holding.getParent() != null) {
            detachChild(_holding);
        }

        // display visualizations for influences and hindrances
        for (Unit.InfluenceType type : Unit.InfluenceType.values()) {
            if (!ObjectUtil.equals(unit.getInfluence(type), _influences.get(type))) {
                if (_influenceVizs.get(type) != null) {
                    _influenceVizs.remove(type).destroy();
                }
                Influence influence = unit.getInfluence(type);
                _influences.put(type, influence);
                if (influence != null) {
                    InfluenceViz viz = influence.createViz(BangPrefs.isHighDetail());
                    if (viz != null) {
                        _influenceVizs.put(type, viz);
                        viz.init(_ctx, this);
                    }
                }
            }
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
        updateStatus();
    }

    @Override // documentation inherited
    public void setSelected (boolean selected)
    {
        if (_selected != selected) {
            super.setSelected(selected);
            if (_observers != null) {
                _observers.apply(_updater);
            }
        }
    }

    @Override // documentation inherited
    public void move (Path path)
    {
        super.move(path);
        _ustatus.setCullMode(CULL_ALWAYS);

        // load the fire effect for the death flights of airborne steam units
        if (path instanceof MoveUnitPath && !_piece.isAlive() &&
            _piece.isAirborne() && ((Unit)_piece).getConfig().make == UnitConfig.Make.STEAM) {
            _ctx.loadParticles(FIRE_EFFECT, new ResultAttacher<Spatial>(this) {
                public void requestCompleted (Spatial result) {
                    if (isMoving()) {
                        super.requestCompleted(result);
                        _fire = result;
                    }
                }
            });
        }
    }

    @Override // documentation inherited
    public void pathCompleted ()
    {
        super.pathCompleted();

        // turn the fire off and display an explosion when the unit reaches the
        // end of its death flight
        if (_fire != null) {
            ParticleUtil.stopAndRemove(_fire);
            _fire = null;
            ExplosionViz eviz = new ExplosionViz(null, true);
            PieceSprite sprite = _view.getPieceSprite(_piece);
            if (sprite != null) {
                eviz.init((BangContext)_ctx, (BangBoardView)_view, sprite, null);
                eviz.display();
            }
        }
    }

    @Override // documentation inherited
    public boolean removed ()
    {
        // make sure the unit is "dead" so that the unit status display doesn't
        // pop back up
        _piece.damage = 100;
        queueAction(REMOVED);
        return true;
    }

    /**
     * Configure an effect handler that gets called during the "shot" part
     * of a path.
     */
    public void setShootHandler (EffectHandler handler)
    {
        _effectHandler = handler;
    }

    /**
     * Returns an instance of the sound to be used for the given shot effect,
     * or <code>null</code> for none.
     */
    public Sound getShotSound (SoundGroup sounds, ShotEffect shot)
    {
        // no sound for collateral damage shot; the main shot will produce a
        // sound
        String type = ShotEffect.SHOT_ACTIONS[shot.type];
        if (shot.type == ShotEffect.COLLATERAL_DAMAGE) {
            return null;
        } else if (shot.type == ShotEffect.DUD) {
            return sounds.getSound("rsrc/cards/frontier_town/dud/shot.ogg");
        } else if (shot.type == ShotEffect.MISFIRE) {
            // for now just use our normal shot sound
            type = "shooting";
        }
        String path = "rsrc/units/" + _name + "/" + type + ".ogg";
        // TODO: fall back to a generic sound if we don't have a
        // special sound for this unit for this shot type
        // TODO: go back to complaining if we don't have shot sounds
        return (SoundUtil.haveSound(path) ? sounds.getSound(path) : null);
    }

    /**
     * Returns an instance of the sound to be used when the unit dies, or
     * <code>null</code> for none.
     */
    public Sound getDyingSound (SoundGroup sounds)
    {
        String path = "rsrc/units/" + _name + "/dying.ogg";
        return SoundUtil.haveSound(path) ? sounds.getSound(path) : null;
    }

    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        super.updateWorldData(time);

        // we have to do extra fiddly business here because our texture is
        // additionally scaled and translated to center the texture at half
        // size within the highlight node
        Texture gptex = _pendtst.getTexture();
        _gcamrot.fromAngleAxis(_angle, Vector3f.UNIT_Z);
        _gcamrot.mult(WHOLE_UNIT, _gcamtrans);
        _gcamtrans.set(1f - _gcamtrans.x - 0.5f,
                       1f - _gcamtrans.y - 0.5f, 0f);
        gptex.setRotation(_gcamrot);
        gptex.setTranslation(_gcamtrans);
    }

    @Override // documentation inherited
    protected void createGeometry ()
    {
        // load up our model
        super.createGeometry();

        // if we're a range unit, make sure the "bullet" model is loaded
        Unit unit = (Unit)_piece;
        if (unit.getConfig().mode == UnitConfig.Mode.RANGE) {
            _ctx.loadModel("units", "frontier_town/artillery/shell", null);
        }

        // if we're a mechanized unit, make sure the "dead" model is loaded
        if (unit.getConfig().make == UnitConfig.Make.STEAM) {
            _ctx.loadModel(_type, _name + "/dead", null);
        }

        // make sure the pending move textures for our unit type are loaded
        _pendtexs = _pendtexmap.get(_name);
        if (_pendtexs == null) {
            _pendtexmap.put(_name, _pendtexs = new Texture[4]);
            for (int ii = 0; ii < _pendtexs.length; ii++) {
                _pendtexs[ii] = _ctx.getTextureCache().getTexture(
                    "units/" + _name + "/pending.png", 64, 64, 2, ii);
                _pendtexs[ii].setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
                RenderUtil.ensureLoaded(_ctx, _pendtexs[ii]);
            }
        }
        _pendtst = RenderUtil.createTextureState(
            _ctx, createPendingTexture(0));

        // this composite of icons combines to display our status
        _tlight = _view.getTerrainNode().createHighlight(
            _piece.x, _piece.y, true, true);
        attachHighlight(_status = new UnitStatus(_ctx, _tlight));
        _ustatus = (UnitStatus)_status;
        _ustatus.update(
                _piece, _piece.ticksUntilMovable(_tick), _pendo, false, _pidx);

        _target = new PieceTarget(_piece, _ctx);
        attachChild(_target);

        // when holding a bonus it is shown over our head
        _holding = new Node("holding");
        _holding.addController(new Spinner(_holding, FastMath.PI/2));
        _holding.setLocalScale(0.5f);

        // configure our colors
        configureOwnerColors();
    }

    @Override // documentation inherited
    protected void modelLoaded (Model model)
    {
        super.modelLoaded(model);
        String bframe = _model.getProperties().getProperty(
            "ballistic_shot_frame");
        if (bframe != null) {
            _ballisticShotDelay = (float)Integer.parseInt(bframe) /
                _model.getAnimation("shooting").frameRate;
        }
        String bsource = _model.getProperties().getProperty(
            "ballistic_shot_source");
        _ballisticShotSource = (bsource == null ?
            null : _model.getDescendant(bsource));
    }

    @Override // from MobileSprite
    protected String getMoveSound ()
    {
        Unit unit = (Unit)_piece;
        if (unit.getConfig().moveSound != null) {
            return unit.getConfig().moveSound;
        } else {
            return super.getMoveSound();
        }
    }

    @Override // documentation inherited
    protected void moveEnded ()
    {
        super.moveEnded();
        updateTileHighlight();
        updateStatus();
    }

    /**
     * Updates the visibility and location of the status display.
     */
    protected void updateStatus ()
    {
        if (_piece.isAlive() && !isMoving()) {
            int ticks = _piece.ticksUntilMovable(_tick);
            _ustatus.update(
                    _piece, ticks, _pendo, _hovered || _selected, _pidx);
            _ustatus.setCullMode(CULL_DYNAMIC);
        } else {
            _ustatus.setCullMode(CULL_ALWAYS);
        }
    }

    @Override // documentation inherited
    protected void setCoord (BangBoard board, Vector3f[] coords, int idx,
                             int nx, int ny, boolean moving)
    {
        // make an exception for the death flights of flyers: only the last
        // coordinate is on the ground
        int elev;
        if (_piece.isAirborne() && !_piece.isAlive() &&
            idx != coords.length-1) {
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
        tidx = Math.min(tidx, _pendtexs.length - 1);
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

    protected float _ballisticShotDelay;
    protected Spatial _ballisticShotSource;

    protected TerrainNode.Highlight _pendnode;
    protected TextureState _pendtst;
    protected Texture[] _pendtexs;

    protected int _pidx = -1;

    protected Quaternion _gcamrot = new Quaternion();
    protected Vector3f _gcamtrans = new Vector3f();

    /** Casted reference to our status. */
    protected UnitStatus _ustatus;

    protected PieceTarget _target;
    protected AdvanceOrder _pendo = AdvanceOrder.NONE;

    protected Node _holding;
    protected String _holdingType;

    protected HashMap<Unit.InfluenceType, InfluenceViz> _influenceVizs =
        new HashMap<Unit.InfluenceType, InfluenceViz>();
    protected HashMap<Unit.InfluenceType, Influence> _influences =
        new HashMap<Unit.InfluenceType, Influence>();

    protected Spatial _fire;

    protected EffectHandler _effectHandler;

    protected static HashMap<String,Texture[]> _pendtexmap =
        new HashMap<String,Texture[]>();

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
        "proximity",
        "returning_fire",
        "dud",
        "misfire",
        "dying",
    };

    /** The effect displayed on the death flights of airborne steam units. */
    protected static final String FIRE_EFFECT = "frontier_town/fire";
}
