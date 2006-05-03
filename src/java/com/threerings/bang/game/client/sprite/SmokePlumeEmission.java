//
// $Id$

package com.threerings.bang.game.client.sprite;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.state.TextureState;
import com.jme.renderer.CloneCreator;
import com.jme.renderer.ColorRGBA;

import com.jmex.effects.ParticleManager;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;

import static com.threerings.bang.client.BangMetrics.*;

import com.threerings.bang.util.RenderUtil;

/**
 * A plume of smoke represented by a particle system.
 */
public class SmokePlumeEmission extends SpriteEmission
{
    @Override // documentation inherited
    public void init (Model model)
    {
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
        if (RenderUtil.blendAlpha == null) {
            RenderUtil.initStates();
        }
        _smokemgr.getParticles().setRenderState(RenderUtil.blendAlpha);
        _smokemgr.getParticles().setRenderState(RenderUtil.overlayZBuf);
        _smokemgr.getParticles().updateRenderState();
        _smokemgr.getParticles().addController(_smokemgr);
        _smokemgr.forceRespawn();
        
        model.getEmissionNode().attachChild(_smokemgr.getParticles());
        
        super.init(model);
    }
    
    @Override // documentation inherited
    public void setActive (boolean active)
    {
        super.setActive(active);
        _smokemgr.setReleaseRate(active ? 256 : 0);
    }
    
    @Override // documentation inherited
    public void resolveTextures (TextureProvider tprov)
    {
        if (_smoketex == null) {
            _smoketex = tprov.getTexture("/textures/effects/dust.png");
        }
        _smokemgr.getParticles().setRenderState(_smoketex);
    }
    
    @Override // documentation inherited
    public Controller putClone (Controller store, CloneCreator properties)
    {
        SmokePlumeEmission spstore;
        if (store == null) {
            spstore = new SmokePlumeEmission();
        } else {
            spstore = (SmokePlumeEmission)store;
        }
        super.putClone(spstore, properties);
        return spstore;
    }
    
    // documentation inherited
    public void update (float time)
    {
        if (!isActive()) {
            return;
        }
        getEmitterLocation(_smokemgr.getParticlesOrigin());
    }
    
    /** The smoke plume particle system. */
    protected ParticleManager _smokemgr;
    
    /** The smoke texture. */
    protected static TextureState _smoketex;
    
    private static final long serialVersionUID = 1;
}
