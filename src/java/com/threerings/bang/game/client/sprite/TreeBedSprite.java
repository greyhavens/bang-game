//
// $Id$

package com.threerings.bang.game.client.sprite;

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

import static com.threerings.bang.client.BangMetrics.*;

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
            _growth = tree.growth;
            _nextIdle = FastMath.FLT_EPSILON;
        }

        // perhaps create, update, or remove the damaged texture overlay
        float pdamage = tree.getPercentDamage();
        if (pdamage > 0f) {
            if (!_oadded) {
                addDamageOverlay();
            }
            _omstate.getDiffuse().a = pdamage;

        } else if (_oadded) {
            new SpatialVisitor<ModelMesh>(ModelMesh.class) {
                protected void visit (ModelMesh mesh) {
                    mesh.removeOverlay(_overlay);
                }
            }.traverse(_model);
            _oadded = false;
        }
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
        return "tree_bed";
    }

    @Override // documentation inherited
    protected String[] getIdleAnimations ()
    {
        return _dead ? null : new String[] { "idle_stage" + _growth };
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
        _oadded = false;
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
     * Adds the damaged texture overlay to the current model.
     */
    protected void addDamageOverlay ()
    {
        if (_overlay == null) {
            _overlay = new RenderState[3];
            _overlay[0] = RenderUtil.blendAlpha;
            _overlay[1] = _omstate = _ctx.getRenderer().createMaterialState();
            _overlay[2] = RenderUtil.createTextureState(_ctx,
                "props/indian_post/special/tree_bed/alpha_dead.png");
            _omstate.getAmbient().set(ColorRGBA.white);
            _omstate.getDiffuse().set(ColorRGBA.white);
        }
        new SpatialVisitor<ModelMesh>(ModelMesh.class) {
            protected void visit (ModelMesh mesh) {
                TextureState tstate = (TextureState)mesh.getRenderState(
                    RenderState.RS_TEXTURE);
                if (tstate.getTexture().getImageLocation().indexOf(
                        "alpha") != -1) {
                    mesh.addOverlay(_overlay);
                }
            }
        }.traverse(_model);
        _oadded = true;
    }

    /** The currently depicted growth stage. */
    protected byte _growth;

    /** The damaged texture overlay. */
    protected RenderState[] _overlay;

    /** The overlay's material state. */
    protected MaterialState _omstate;

    /** Whether or not the overlay has been added. */
    protected boolean _oadded;

    /** The duration of the falling trunk animation. */
    protected static final float TRUNK_FALL_DURATION = 1f;
}
