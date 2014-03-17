//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.samskivert.util.RandomUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.ModelMesh;
import com.threerings.jme.util.SpatialVisitor;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.PieceCodes;
import com.threerings.bang.game.data.piece.TreeBed;

/**
 * Displays trees for the forest guardians scenario.
 */
public class TreeBedSprite extends ActiveSprite
{
    /** The color of the tree's status display. */
    public static final ColorRGBA STATUS_COLOR =
        new ColorRGBA(0.388f, 1f, 0.824f, 1f);

    /** The border color of the status display. */
    public static final ColorRGBA DARKER_STATUS_COLOR =
        new ColorRGBA(0.194f, 0.5f, 0.412f, 1f);

    public TreeBedSprite ()
    {
        super("props", "indian_post/special/tree_bed");
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        TreeBed tree = (TreeBed)piece;

        // grow to the next stage
        while (_growth < tree.growth) {
            queueAction("grow_stage" + (++_growth));
        }
        if (_growth != tree.growth) {
            // make sure the tree is visible; it may have been
            // hidden temporarily for counting by RobotWaveHandler
            setCullMode(CULL_INHERIT);
            _hnode.setCullMode(CULL_INHERIT);

            _growth = tree.growth;
            _nextIdle = FastMath.FLT_EPSILON;
        }

        // update the blended textures
        updateTextureStates();
    }

    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        // transition into the max texture during the third growth stage
        if (_nextAction > 0 && _action.equals("grow_stage3"))  {
            float alpha = Math.max(0f, (_nextAction - time) /
                _finalGrowthDuration);
            setTextureStates(_mtstate, _btstate, alpha);
        }
        super.updateWorldData(time);
    }

    @Override // documentation inherited
    protected void addProceduralActions ()
    {
        super.addProceduralActions();
        final ProceduralAction daction = _procActions.get(DEAD);
        _procActions.put(DEAD, new ProceduralAction() {
            public float activate () {
                daction.activate();
                dropTrunk();
                return TRUNK_FALL_DURATION;
            }
        });
        _procActions.put("reacting", new ProceduralAction() {
            public float activate () {
                return setAction("react_stage" + _growth);
            }
        });
    }

    @Override // from PieceSprite
    protected void createGeometry ()
    {
        super.createGeometry();

        _growth = ((TreeBed)_piece).growth;

        _tlight = _view.getTerrainNode().createHighlight(
            _piece.x, _piece.y, false, false);
        attachHighlight(_status = new PieceStatus(_ctx, _tlight, STATUS_COLOR,
            DARKER_STATUS_COLOR));
        updateStatus();
    }

    @Override // documentation inherited
    protected String getHelpIdent (int pidx)
    {
        return "tree_bed" + _growth;
    }

    @Override // documentation inherited
    protected String[] getIdleAnimations ()
    {
        return (_dead ? null : new String[] { "idle_stage" + _growth });
    }

    @Override // documentation inherited
    protected String getDeadModel ()
    {
        return _name + "/stump" + _growth;
    }

    @Override // documentation inherited
    protected void modelLoaded (Model model)
    {
        super.modelLoaded(model);
        if (_finalGrowthDuration == 0f) {
            _finalGrowthDuration =
                model.getAnimation("grow_stage3").getDuration();
        }
        if (_btstate == null) {
            String troot = _type + "/" + _name + "/alpha";
            if (TextureState.getNumberOfFixedUnits() >= 2) {
                Texture etex = _ctx.getTextureCache().getTexture(
                    troot + "_emissive.png");
                etex.setApply(Texture.AM_BLEND);
                etex.setBlendColor(ColorRGBA.white);
                _btstate = _ctx.getRenderer().createTextureState();
                _btstate.setTexture(etex, 0);
                _btstate.setTexture(
                    _ctx.getTextureCache().getTexture(troot + ".png"), 1);
                _mtstate = _ctx.getRenderer().createTextureState();
                _mtstate.setTexture(etex, 0);
                _mtstate.setTexture(
                    _ctx.getTextureCache().getTexture(troot + "_max.png"), 1);
            } else {
                _btstate = RenderUtil.createTextureState(_ctx, troot + ".png");
                _mtstate = RenderUtil.createTextureState(_ctx, troot + "_max.png");
            }
            _dtstate = RenderUtil.createTextureState(_ctx, troot + "_dead.png");
        }
        _ptstate = _btstate;
        _ststate = null;

        // update the textures now that the model is loaded
        updateTextureStates();
    }

    /**
     * Blends between the base or maxed texture and the damaged texture.
     */
    protected void updateTextureStates ()
    {
        setTextureStates(
            (_growth == TreeBed.FULLY_GROWN) ? _mtstate : _btstate,
            _dtstate, ((TreeBed)_piece).getPercentDamage());
    }

    /**
     * Adds a tree trunk model above the stump and animates its falling
     * over towards the nearest logging robot.
     */
    protected void dropTrunk ()
    {
        _ctx.loadModel(_type, _name + "/dead" + _growth,
            new ResultAttacher<Model>(this) {
            public void requestCompleted (Model result) {
                super.requestCompleted(result);
                continueDroppingTrunk(result);
            }
        });
    }

    /**
     * Continues the trunk drop animation after the model has been loaded.
     */
    protected void continueDroppingTrunk (final Model model)
    {
        int dir = PieceCodes.DIRECTIONS[
            RandomUtil.getInt(PieceCodes.DIRECTIONS.length)];
        final Vector3f axis = new Vector3f(PieceCodes.DX[dir],
            PieceCodes.DY[dir], 0f);

        final MaterialState mstate = _ctx.getRenderer().createMaterialState();
        mstate.getAmbient().set(ColorRGBA.white);
        mstate.getDiffuse().set(ColorRGBA.white);
        model.setRenderState(mstate);
        model.setRenderState(RenderUtil.blendAlpha);
        model.updateRenderState();

        model.addController(new Controller() {
            public void update (float time) {
                if ((_elapsed += time) >= TRUNK_FALL_DURATION) {
                    detachChild(model);
                    return;
                }
                float alpha = _elapsed / TRUNK_FALL_DURATION;
                mstate.getDiffuse().a = Math.min(2f - alpha*2, 1f);
                model.getLocalRotation().fromAngleNormalAxis(
                    alpha * FastMath.HALF_PI, axis);
            }
            protected float _elapsed;
        });
    }

    /**
     * Blends between two texture states.
     */
    protected void setTextureStates (
        TextureState t1, TextureState t2, float alpha)
    {
        if (_model == null) {
            // wait until the model is loaded
            return;
        }
        if (alpha == 0f) {
            t2 = null;
        } else if (alpha == 1f) {
            t1 = t2;
            t2 = null;
        } else {
            if (_overlay == null) {
                _overlay = new RenderState[2];
                _overlay[0] = _omstate =
                    _ctx.getRenderer().createMaterialState();
                _omstate.getAmbient().set(ColorRGBA.white);
                _omstate.getDiffuse().set(ColorRGBA.white);
            }
            _omstate.getDiffuse().a = alpha;
            _overlay[1] = t2;
        }
        final boolean swap = (_ptstate != t1),
            add = (_ststate == null && t2 != null),
            remove = (_ststate != null && t2 == null);
        _ptstate = t1;
        _ststate = t2;
        if (swap || add || remove) {
            new SpatialVisitor<ModelMesh>(ModelMesh.class) {
                protected void visit (ModelMesh mesh) {
                    TextureState tstate = (TextureState)mesh.getRenderState(
                        RenderState.RS_TEXTURE);
                    if (tstate.getTexture(0).getImageLocation().indexOf(
                            "alpha") != -1) {
                        if (swap) {
                            mesh.setRenderState(_ptstate);
                        }
                        if (add) {
                            mesh.addOverlay(_overlay);
                        } else if (remove) {
                            mesh.removeOverlay(_overlay);
                        }
                    }
                }
            }.traverse(_model);
            if (swap) {
                _model.updateRenderState();
            }
        }
    }

    /** The currently depicted growth stage. */
    protected byte _growth;

    /** The damaged or maxed texture overlay. */
    protected RenderState[] _overlay;

    /** The overlay's material state. */
    protected MaterialState _omstate;

    /** The current primary and secondary texture states. */
    protected TextureState _ptstate, _ststate;

    /** The duration of the final growth animation. */
    protected static float _finalGrowthDuration;

    /** The base, max, and damaged texture states. */
    protected static TextureState _btstate, _mtstate, _dtstate;

    /** The duration of the falling trunk animation. */
    protected static final float TRUNK_FALL_DURATION = 1f;
}
