//
// $Id$

package com.threerings.bang.game.client.effect;

import java.util.ArrayList;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.TriMesh;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jmex.effects.ParticleManager;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Temporarily hijacked explosion factory.
 */
public class ParticleFactory
{
    public static void warmup (BangContext ctx)
    {
        _dusttex = RenderUtil.createTextureState(
            ctx, "textures/effects/dust.png");
    }

    public static ParticleManager getDustRing ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _dustRings.size(); x < tSize; x++) {
            ParticleManager e = _dustRings.get(x);
            if (!e.isActive()) {
                return e;
            }
        }
        ParticleManager dustRing = createDustRing();
        _dustRings.add(dustRing);
        return dustRing;
    }
    
    public static ParticleManager getFireball ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _fireballs.size(); x < tSize; x++) {
            ParticleManager e = _fireballs.get(x);
            if (!e.isActive()) {
                return e;
            }
        }
        ParticleManager fireball = createFireball();
        _fireballs.add(fireball);
        return fireball;
    }
    
    public static ParticleManager getStreamer ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _streamers.size(); x < tSize; x++) {
            ParticleManager e = _streamers.get(x);
            if (!e.isActive()) {
                return e;
            }
        }
        ParticleManager streamer = createStreamer();
        _streamers.add(streamer);
        return streamer;
    }
    
    public static ParticleManager getSmokePuff ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _smokePuffs.size(); x < tSize; x++) {
            ParticleManager e = _smokePuffs.get(x);
            if (!e.isActive()) {
                return e;
            }
        }
        ParticleManager smokePuff = createSmokePuff();
        _smokePuffs.add(smokePuff);
        return smokePuff;
    }
    public static ParticleManager getSparkles ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _sparkles.size(); x < tSize; x++) {
            ParticleManager e = _sparkles.get(x);
            if (!e.isActive()) {
                return e;
            }
        }
        ParticleManager sparkles = createSparkles();
        _sparkles.add(sparkles);
        return sparkles;
    }
    
    protected static ParticleManager createDustRing ()
    {
        ParticleManager manager = new TransientParticleManager(64);
        manager.setParticlesMinimumLifeTime(500f);
        manager.setInitialVelocity(0.02f);
        manager.setEmissionDirection(Vector3f.UNIT_Z);
        manager.setEmissionMinimumAngle(FastMath.HALF_PI);
        manager.setEmissionMaximumAngle(FastMath.HALF_PI);
        manager.setRandomMod(0f);
        manager.setPrecision(FastMath.FLT_EPSILON);
        manager.setControlFlow(false);
        manager.setParticleSpinSpeed(0.1f);
        manager.setStartSize(TILE_SIZE / 5);
        manager.setEndSize(TILE_SIZE / 3);
        manager.setRepeatType(Controller.RT_CLAMP);
        
        TriMesh particles = manager.getParticles();
        particles.addController(manager);

        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.blendAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        particles.updateRenderState();
        
        return manager;
    }
    
    protected static ParticleManager createFireball ()
    {
        ParticleManager manager = new TransientParticleManager(16);
        manager.setParticlesMinimumLifeTime(250f);
        manager.setInitialVelocity(0.004f);
        manager.setEmissionMaximumAngle(FastMath.PI);
        manager.setRandomMod(0f);
        manager.setPrecision(FastMath.FLT_EPSILON);
        manager.setControlFlow(false);
        manager.setParticleSpinSpeed(0.01f);
        manager.setStartSize(TILE_SIZE / 3);
        manager.setEndSize(TILE_SIZE / 2);
        manager.setStartColor(new ColorRGBA(1f, 1f, 0.5f, 1f));
        manager.setEndColor(new ColorRGBA(1f, 0.25f, 0f, 0f));
        manager.setRepeatType(Controller.RT_CLAMP);
        
        TriMesh particles = manager.getParticles();
        particles.addController(manager);

        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.addAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        particles.updateRenderState();
        
        return manager;
    }
    
    protected static ParticleManager createStreamer ()
    {
        ParticleManager manager = new ParticleManager(64);
        manager.setParticlesMinimumLifeTime(250f);
        manager.setInitialVelocity(0f);
        manager.setRandomMod(0f);
        manager.setPrecision(FastMath.FLT_EPSILON);
        manager.setControlFlow(true);
        manager.setReleaseRate(512);
        manager.setReleaseVariance(0f);
        manager.setParticleSpinSpeed(0f);
        manager.setStartSize(TILE_SIZE / 25);
        manager.setEndSize(TILE_SIZE / 10);
        manager.setStartColor(new ColorRGBA(1f, 1f, 0.5f, 1f));
        manager.setEndColor(new ColorRGBA(1f, 0.25f, 0f, 1f));
        
        TriMesh particles = manager.getParticles();
        particles.addController(manager);

        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.addAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        particles.updateRenderState();
        
        return manager;
    }
    
    protected static ParticleManager createSmokePuff ()
    {
        ParticleManager manager = new TransientParticleManager(32);
        manager.setParticlesMinimumLifeTime(500f);
        manager.getParticlesOrigin().z += (TILE_SIZE * 0.75f);
        manager.setInitialVelocity(0.01f);
        manager.setEmissionDirection(Vector3f.UNIT_Z);
        manager.setEmissionMaximumAngle(FastMath.PI / 4);
        manager.setRandomMod(0f);
        manager.setPrecision(FastMath.FLT_EPSILON);
        manager.setControlFlow(false);
        manager.setStartSize(TILE_SIZE / 2);
        manager.setEndSize(TILE_SIZE);
        manager.setStartColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 0.75f));
        manager.setEndColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 0f));
        manager.setRepeatType(Controller.RT_CLAMP);
        
        TriMesh particles = manager.getParticles();
        particles.addController(manager);

        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.blendAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        particles.updateRenderState();
        
        return manager;
    }
    
    protected static ParticleManager createSparkles ()
    {
        ParticleManager manager = new ParticleManager(64);
        manager.setParticlesMinimumLifeTime(250f);
        manager.setInitialVelocity(0f);
        manager.setRandomMod(0f);
        manager.setPrecision(FastMath.FLT_EPSILON);
        manager.setControlFlow(true);
        manager.setReleaseRate(512);
        manager.setReleaseVariance(0f);
        manager.setParticleSpinSpeed(0f);
        manager.setStartSize(TILE_SIZE / 25);
        manager.setEndSize(TILE_SIZE / 10);
        manager.setStartColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 1f));
        manager.setEndColor(new ColorRGBA(0f, 0f, 0f, 1f));
        
        TriMesh particles = manager.getParticles();
        particles.addController(manager);

        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.addAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        particles.updateRenderState();
        
        return manager;
    }
    
    /** Extends the particle manager class to detach the particles from their
     * parent when the manager becomes inactive. */
    protected static class TransientParticleManager extends ParticleManager
    {
        public TransientParticleManager (int nparticles)
        {
            super(nparticles);
        }
        
        public void update (float secondsPassed)
        {
            super.update(secondsPassed);
            if (!isActive()) {
                getParticles().getParent().detachChild(getParticles());
            }
        }
    }
    
    protected static ArrayList<ParticleManager> _dustRings =
        new ArrayList<ParticleManager>();
    protected static ArrayList<ParticleManager> _fireballs =
        new ArrayList<ParticleManager>();
    protected static ArrayList<ParticleManager> _streamers =
        new ArrayList<ParticleManager>();
    protected static ArrayList<ParticleManager> _smokePuffs =
        new ArrayList<ParticleManager>();
    protected static ArrayList<ParticleManager> _sparkles =
        new ArrayList<ParticleManager>();
    protected static TextureState _dusttex;
}
