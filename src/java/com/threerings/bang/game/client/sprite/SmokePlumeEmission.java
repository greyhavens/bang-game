//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.Properties;

import com.jme.bounding.BoundingBox;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Geometry;
import com.jme.scene.state.TextureState;
import com.jme.renderer.ColorRGBA;

import com.jmex.effects.ParticleManager;

import com.threerings.bang.client.Model;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * A plume of smoke represented by a particle system.
 */
public class SmokePlumeEmission extends SpriteEmission
{
    public SmokePlumeEmission (String name, Properties props)
    {
        super(name);
    }
    
    @Override // documentation inherited
    public void init (BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        super.init(ctx, view, sprite);
        
        _smokemgr = new ParticleManager(64);
        _smokemgr.setParticlesMinimumLifeTime(2000f);
        _smokemgr.setInitialVelocity(0.01f);
        _smokemgr.setParticlesOrigin(new Vector3f());
        _smokemgr.setEmissionDirection(Vector3f.UNIT_Z);
        _smokemgr.setEmissionMaximumAngle(FastMath.PI / 64);
        _smokemgr.setRandomMod(0f);
        _smokemgr.setPrecision(FastMath.FLT_EPSILON);
        _smokemgr.setControlFlow(true);
        _smokemgr.setReleaseRate(0);
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
        _smokemgr.getParticles().setRenderState(_smoketex);
        _smokemgr.getParticles().setRenderState(RenderUtil.blendAlpha);
        _smokemgr.getParticles().setRenderState(RenderUtil.overlayZBuf);
        _smokemgr.getParticles().updateRenderState();
        _smokemgr.getParticles().addController(_smokemgr);
        _smokemgr.getParticles().addController(new Controller() {
            public void update (float time) {
                // position the emitter
                getEmitterLocation(true, _smokemgr.getParticlesOrigin());
            }
        });
        _view.getPieceNode().attachChild(_smokemgr.getParticles());
    }
    
    @Override // documentation inherited
    public void cleanup ()
    {
        super.cleanup();
        _view.getPieceNode().detachChild(_smokemgr.getParticles());
    }
    
    @Override // documentation inherited
    public void start (Model.Animation anim, Model.Binding binding)
    {
        super.start(anim, binding);
        _smokemgr.setReleaseRate(256);
    }
    
    @Override // documentation inherited
    public void stop ()
    {
        super.stop();
        _smokemgr.setReleaseRate(0);
    }
    
    /** The smoke plume particle system. */
    protected ParticleManager _smokemgr;
    
    /** The smoke texture. */
    protected static TextureState _smoketex;
}
