//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.bounding.BoundingBox;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.Geometry;
import com.jme.scene.state.TextureState;
import com.jmex.effects.ParticleManager;

import com.threerings.jme.sprite.Path;

import com.threerings.bang.client.Config;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Train;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a train piece.
 */
public class TrainSprite extends MobileSprite
{
    public TrainSprite (byte type)
    {
        super("extras/train", TYPE_NAMES[type]);
        _type = type;
    }
    
    @Override // documentation inherited
    protected void createGeometry (BasicContext ctx)
    {
        super.createGeometry(ctx);
        
        // for engines, create the smoke plume particle system
        if (((Train)_piece).type != Train.ENGINE) {
            return;
        }
        _smokemgr = new ParticleManager(64);
        _smokemgr.setParticlesMinimumLifeTime(2000f);
        _smokemgr.setInitialVelocity(0.01f);
        _smokemgr.setEmissionDirection(Vector3f.UNIT_Z);
        _smokemgr.setEmissionMaximumAngle(FastMath.PI / 64);
        _smokemgr.setRandomMod(0f);
        _smokemgr.setPrecision(FastMath.FLT_EPSILON);
        _smokemgr.setControlFlow(true);
        _smokemgr.setReleaseRate(256);
        _smokemgr.setReleaseVariance(0f);
        _smokemgr.setParticleSpinSpeed(0.01f);
        _smokemgr.setStartSize(TILE_SIZE / 8);
        _smokemgr.setEndSize(TILE_SIZE / 2);
        _smokemgr.setStartColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 0.75f));
        _smokemgr.setEndColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 0f));
        if (_smoketex == null) {
            _smoketex = RenderUtil.createTextureState(
                ctx, "textures/effects/dust.png");
        }
        _smokemgr.getParticles().setRenderState(_dusttex);
        _smokemgr.getParticles().setRenderState(RenderUtil.blendAlpha);
        _smokemgr.getParticles().setRenderState(RenderUtil.overlayZBuf);
        _smokemgr.getParticles().updateRenderState();
        _smokemgr.getParticles().addController(_smokemgr);
        _smokemgr.getParticles().addController(new Controller() {
            public void update (float time) {
                // position the emitter
                _smokemgr.setParticlesOrigin(
                    getEmitterTranslation(STACK_EMITTER));
            }
        });
        
        // put them in the highlight node so that they are positioned relative
        // to the board
        _hnode.attachChild(_smokemgr.getParticles());
    }
    
    @Override // documentation inherited
    protected void createDustManager (BasicContext ctx)
    {
        // trains do not kick up dust
    }
    
    @Override // documentation inherited
    protected Path createPath (BangBoard board, Piece opiece, Piece npiece)
    {
        Train otrain = (Train)opiece, ntrain = (Train)npiece;
        boolean last = (otrain.lastX != Train.UNSET),
            next = (ntrain.nextX != Train.UNSET);
        int ncoords = 2 + (last ? 1 : 0) + (next ? 1 : 0), idx = 0;
        Vector3f[] coords = new Vector3f[ncoords];
        float[] durations = new float[ncoords - 1];
        
        if (last) {
            setCoord(board, coords, idx++, otrain.lastX, otrain.lastY);
        }
        durations[idx] = 1f / Config.display.getMovementSpeed();
        setCoord(board, coords, idx++, otrain.x, otrain.y);
        setCoord(board, coords, idx++, ntrain.x, ntrain.y);
        if (next) {
            setCoord(board, coords, idx, ntrain.nextX, ntrain.nextY);
        }
        return new TrainPath(coords, durations, last);
    }

    @Override // documentation inherited
    protected void reorient ()
    {
        // don't do it; whatever the path left us at is good
    }
    
    /** A special path class for trains that incorporates the previous and next
     * positions. */
    protected class TrainPath extends MoveUnitPath
    {
        public TrainPath (Vector3f[] coords, float[] durations, boolean last)
        {
            super(TrainSprite.this, coords, durations);
            if (last) {
                advance();
            }
        }
        
        @Override // documentation inherited
        public void update (float time)
        {
            // bail out (without changing the position) after the first
            // leg
            float naccum = _accum + time;
            if (naccum > _durations[_current]) {
                _sprite.pathCompleted();
                return;
                
            } else {
                super.update(time);
            }
        }
    }
    
    protected int _type;
    
    /** The smoke plume particle system. */
    protected ParticleManager _smokemgr;
    
    /** The smoke texture. */
    protected static TextureState _smoketex;
    
    /** The relative position of the stack emitter. */
    protected static Vector3f _stackEmitterTranslation;
    
    /** The model names for each train type. */    
    protected static final String[] TYPE_NAMES = { "locomotive", "caboose",
        "cattle", "freight" };
    
    /** The name of the smoke stack emitter marker. */
    protected static final String STACK_EMITTER = "emitter_stack";
}
