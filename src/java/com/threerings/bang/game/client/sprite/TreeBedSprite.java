//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;

import com.samskivert.util.RandomUtil;

import com.threerings.jme.model.Model;

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
    public String getHelpIdent (int pidx)
    {
        return "indian_post/special/tree_bed";
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
    protected String[] getIdleAnimations ()
    {
        return _dead ? null : new String[] { "idle_stage" + _growth };
    }
    
    @Override // documentation inherited
    protected String getDeadModel ()
    {
        return _name + "/stump" + _growth;
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
        // choose the drop direction from among the directions in which logging
        // robots abut the tree
        int bdirs = ((TreeBed)_piece).botDirs;
        int[] weights = new int[PieceCodes.DIRECTIONS.length];
        for (int dir : PieceCodes.DIRECTIONS) {
            if ((bdirs & (1 << dir)) != 0) {
                weights[dir] = 1;
            }
        }
        int dir = (RandomUtil.getWeightedIndex(weights) + 1 +
            PieceCodes.DIRECTIONS.length - _piece.orientation) %
            PieceCodes.DIRECTIONS.length;
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
                mstate.getDiffuse().a = 1f - alpha;
                model.getLocalRotation().fromAngleNormalAxis(
                    alpha * FastMath.HALF_PI, axis);
            }
            protected float _elapsed;
        });
    }
    
    /** The currently depicted growth stage. */
    protected byte _growth;
    
    /** The duration of the falling trunk animation. */
    protected static final float TRUNK_FALL_DURATION = 1f;
}
