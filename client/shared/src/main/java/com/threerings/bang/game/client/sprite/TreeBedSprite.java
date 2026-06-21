//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.control.AbstractControl;
import com.jme3.texture.Texture2D;

import com.samskivert.util.RandomUtil;

import com.threerings.jme.model.Model;
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
            setCullHint(CullHint.Inherit);
            _hnode.setCullHint(CullHint.Inherit);

            _growth = tree.growth;
            _nextIdle = FastMath.FLT_EPSILON;
        }

        // update the blended textures
        updateTextureStates();
    }

    @Override // documentation inherited
    public void updateLogicalState (float time)
    {
        // transition into the max texture during the third growth stage
        if (_nextAction > 0 && _action.equals("grow_stage3"))  {
            float alpha = Math.max(0f, (_nextAction - time) /
                _finalGrowthDuration);
            setTextureStates(_mtex, _btex, alpha);
        }
        super.updateLogicalState(time);
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
        if (_btex == null) {
            // jME3: the fork combined an emissive overlay unit with the base texture in a
            // multi-unit TextureState; the emissive overlay is a custom-MatDef concern deferred to
            // Phase 4, so here we just load the base/max/dead textures and swap the ColorMap.
            String troot = _type + "/" + _name + "/alpha";
            _btex = _ctx.getTextureCache().getTexture(troot + ".png");
            _mtex = _ctx.getTextureCache().getTexture(troot + "_max.png");
            _dtex = _ctx.getTextureCache().getTexture(troot + "_dead.png");
        }
        _ptex = _btex;

        // update the textures now that the model is loaded
        updateTextureStates();
    }

    /**
     * Blends between the base or maxed texture and the damaged texture. jME3: the fork cross-faded
     * two TextureStates via an alpha overlay; without the re-authored blend MatDef (Phase 4) we
     * pick the dominant texture (damaged once past half damage) and swap the ColorMap.
     */
    protected void updateTextureStates ()
    {
        setTextureStates(
            (_growth == TreeBed.FULLY_GROWN) ? _mtex : _btex,
            _dtex, ((TreeBed)_piece).getPercentDamage());
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

        final SpriteMaterialState mstate = new SpriteMaterialState();
        mstate.getAmbient().set(ColorRGBA.White);
        mstate.getDiffuse().set(ColorRGBA.White);

        model.addControl(new AbstractControl() {
            protected void controlUpdate (float time) {
                if ((_elapsed += time) >= TRUNK_FALL_DURATION) {
                    detachChild(model);
                    return;
                }
                float alpha = _elapsed / TRUNK_FALL_DURATION;
                mstate.getDiffuse().a = Math.min(2f - alpha*2, 1f);
                mstate.apply(model);
                Quaternion rot = new Quaternion();
                rot.fromAngleNormalAxis(alpha * FastMath.HALF_PI, axis);
                model.setLocalRotation(rot);
            }
            protected void controlRender (RenderManager rm, ViewPort vp) {}
            protected float _elapsed;
        });
    }

    /**
     * Selects the displayed texture. jME3: the fork cross-faded a primary and a damaged texture
     * via a per-geometry alpha overlay (fork {@code addOverlay}); jME3 geometries carry a single
     * material with no overlay stack, so until the Phase-4 blend MatDef exists we pick the dominant
     * texture (the damaged one once damage exceeds half) and swap the alpha geometries' ColorMap.
     */
    protected void setTextureStates (Texture2D t1, Texture2D t2, float alpha)
    {
        if (_model == null) {
            // wait until the model is loaded
            return;
        }
        final Texture2D tex = (alpha >= 0.5f && t2 != null) ? t2 : t1;
        if (_ptex == tex) {
            return;
        }
        _ptex = tex;
        new SpatialVisitor<Geometry>(Geometry.class) {
            protected void visit (Geometry mesh) {
                Material mat = mesh.getMaterial();
                if (mat == null) {
                    return;
                }
                // only swap the "alpha" texture geometries (the tree foliage); leave others.
                Object cm = mat.getTextureParam("ColorMap");
                if (cm != null && String.valueOf(
                        ((com.jme3.material.MatParamTexture)cm).getTextureValue().getName())
                            .indexOf("alpha") != -1) {
                    mat.setTexture("ColorMap", tex);
                }
            }
        }.traverse(_model);
    }

    /** The currently depicted growth stage. */
    protected byte _growth;

    /** The currently displayed texture. */
    protected Texture2D _ptex;

    /** The duration of the final growth animation. */
    protected static float _finalGrowthDuration;

    /** The base, max, and damaged textures. */
    protected static Texture2D _btex, _mtex, _dtex;

    /** The duration of the falling trunk animation. */
    protected static final float TRUNK_FALL_DURATION = 1f;
}
