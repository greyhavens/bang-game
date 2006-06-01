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
import com.jmex.effects.particles.ParticleController;
import com.jmex.effects.particles.ParticleFactory;
import com.jmex.effects.particles.ParticleMesh;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Creates and recycles transient particle systems.
 */
public class ParticlePool
{
    /** Extends the particle controller class to detach the particles from
     * their parent when the controller becomes inactive. */
    public static class TransientParticleController
        extends ParticleController
    {
        public TransientParticleController (ParticleMesh particles)
        {
            super(particles);
        }
        
        @Override // documentation inherited
        public void setActive (boolean active)
        {
            super.setActive(active);
            if (!active && getParticles().getParent() != null) {
                getParticles().getParent().detachChild(getParticles());
            }
        }
    }
    
    public static void warmup (BangContext ctx)
    {
        _dusttex = RenderUtil.createTextureState(
            ctx, "textures/effects/dust.png");
    }

    public static ParticleMesh getDustRing ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _dustRings.size(); x < tSize; x++) {
            ParticleMesh e = _dustRings.get(x);
            if (!e.isActive()) {
                e.getParticleController().setActive(true);
                return e;
            }
        }
        ParticleMesh dustRing = createDustRing();
        _dustRings.add(dustRing);
        return dustRing;
    }
    
    public static ParticleMesh getFireball ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _fireballs.size(); x < tSize; x++) {
            ParticleMesh e = _fireballs.get(x);
            if (!e.isActive()) {
                e.getParticleController().setActive(true);
                return e;
            }
        }
        ParticleMesh fireball = createFireball();
        _fireballs.add(fireball);
        return fireball;
    }
    
    public static ParticleMesh getStreamer ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _streamers.size(); x < tSize; x++) {
            ParticleMesh e = _streamers.get(x);
            if (!e.isActive()) {
                e.getParticleController().setActive(true);
                return e;
            }
        }
        ParticleMesh streamer = createStreamer();
        _streamers.add(streamer);
        return streamer;
    }
    
    public static ParticleMesh getSmokePuff ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _smokePuffs.size(); x < tSize; x++) {
            ParticleMesh e = _smokePuffs.get(x);
            if (!e.isActive()) {
                e.getParticleController().setActive(true);
                return e;
            }
        }
        ParticleMesh smokePuff = createSmokePuff();
        _smokePuffs.add(smokePuff);
        return smokePuff;
    }
    
    public static ParticleMesh getSparkles ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _sparkles.size(); x < tSize; x++) {
            ParticleMesh e = _sparkles.get(x);
            if (!e.isActive()) {
                e.getParticleController().setActive(true);
                return e;
            }
        }
        ParticleMesh sparkles = createSparkles();
        _sparkles.add(sparkles);
        return sparkles;
    }
    
    public static ParticleMesh getSteamCloud ()
    {
        int count = 0, index = -1;
        for (int x = 0, tSize = _steamClouds.size(); x < tSize; x++) {
            ParticleMesh e = _steamClouds.get(x);
            if (!e.isActive()) {
                e.getParticleController().setActive(true);
                return e;
            }
        }
        ParticleMesh steamCloud = createSteamCloud();
        _steamClouds.add(steamCloud);
        return steamCloud;
    }
    
    protected static ParticleMesh createDustRing ()
    {
        ParticleMesh particles = new ParticleMesh("dustring", 64);
        particles.addController(new TransientParticleController(particles));
        particles.setMinimumLifeTime(500f);
        particles.setMaximumLifeTime(1500f);
        particles.setInitialVelocity(0.02f);
        particles.setEmissionDirection(Vector3f.UNIT_Z);
        particles.setMinimumAngle(FastMath.HALF_PI);
        particles.setMaximumAngle(FastMath.HALF_PI);
        particles.setRandomMod(0f);
        particles.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        particles.getParticleController().setControlFlow(false);
        particles.setParticleSpinSpeed(0.1f);
        particles.setStartSize(TILE_SIZE / 5);
        particles.setEndSize(TILE_SIZE / 3);
        particles.getParticleController().setRepeatType(Controller.RT_CLAMP);
        
        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.blendAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        
        return particles;
    }
    
    protected static ParticleMesh createFireball ()
    {
        ParticleMesh particles = new ParticleMesh("fireball", 16);
        particles.addController(new TransientParticleController(particles));
        particles.setMinimumLifeTime(250f);
        particles.setMaximumLifeTime(750f);
        particles.setInitialVelocity(0.004f);
        particles.setMaximumAngle(FastMath.PI);
        particles.setRandomMod(0f);
        particles.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        particles.getParticleController().setControlFlow(false);
        particles.setParticleSpinSpeed(0.01f);
        particles.setStartSize(TILE_SIZE / 3);
        particles.setEndSize(TILE_SIZE / 2);
        particles.setStartColor(new ColorRGBA(1f, 1f, 0.5f, 1f));
        particles.setEndColor(new ColorRGBA(1f, 0.25f, 0f, 0f));
        particles.getParticleController().setRepeatType(Controller.RT_CLAMP);
        
        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.addAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        
        return particles;
    }
    
    protected static ParticleMesh createStreamer ()
    {
        ParticleMesh particles =
            ParticleFactory.buildParticles("streamer", 64);
        particles.setMinimumLifeTime(250f);
        particles.setMaximumLifeTime(750f);
        particles.setInitialVelocity(0f);
        particles.setRandomMod(0f);
        particles.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        particles.getParticleController().setControlFlow(true);
        particles.setReleaseRate(512);
        particles.getParticleController().setReleaseVariance(0f);
        particles.setParticleSpinSpeed(0f);
        particles.setStartSize(TILE_SIZE / 25);
        particles.setEndSize(TILE_SIZE / 10);
        particles.setStartColor(new ColorRGBA(1f, 1f, 0.5f, 1f));
        particles.setEndColor(new ColorRGBA(1f, 0.25f, 0f, 1f));
        
        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.addAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        
        return particles;
    }
    
    protected static ParticleMesh createSmokePuff ()
    {
        ParticleMesh particles = new ParticleMesh("smokepuff", 32);
        particles.addController(new TransientParticleController(particles));
        particles.setMinimumLifeTime(500f);
        particles.setMaximumLifeTime(1500f);
        particles.getOriginOffset().z += (TILE_SIZE * 0.75f);
        particles.setInitialVelocity(0.01f);
        particles.setEmissionDirection(Vector3f.UNIT_Z);
        particles.setMaximumAngle(FastMath.PI / 4);
        particles.setRandomMod(0f);
        particles.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        particles.getParticleController().setControlFlow(false);
        particles.setStartSize(TILE_SIZE / 2);
        particles.setEndSize(TILE_SIZE);
        particles.setStartColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 0.75f));
        particles.setEndColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 0f));
        particles.getParticleController().setRepeatType(Controller.RT_CLAMP);
        
        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.blendAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        
        return particles;
    }
    
    protected static ParticleMesh createSparkles ()
    {
        ParticleMesh particles =
            ParticleFactory.buildParticles("sparkles", 64);
        particles.setMinimumLifeTime(250f);
        particles.setMaximumLifeTime(750f);
        particles.setInitialVelocity(0f);
        particles.setRandomMod(0f);
        particles.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        particles.getParticleController().setControlFlow(true);
        particles.setReleaseRate(512);
        particles.getParticleController().setReleaseVariance(0f);
        particles.setParticleSpinSpeed(0f);
        particles.setStartSize(TILE_SIZE / 25);
        particles.setEndSize(TILE_SIZE / 10);
        particles.setStartColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 1f));
        particles.setEndColor(new ColorRGBA(0f, 0f, 0f, 1f));
        
        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.addAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        
        return particles;
    }
    
    protected static ParticleMesh createSteamCloud ()
    {
        ParticleMesh particles = new ParticleMesh("steamcloud", 32);
        particles.addController(new TransientParticleController(particles));
        particles.setMinimumLifeTime(500f);
        particles.setMaximumLifeTime(1500f);
        particles.getOriginOffset().z += (TILE_SIZE * 0.75f);
        particles.setInitialVelocity(0.001f);
        particles.setEmissionDirection(Vector3f.UNIT_Z);
        particles.setMaximumAngle(FastMath.PI / 4);
        particles.setRandomMod(0f);
        particles.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        particles.getParticleController().setControlFlow(false);
        particles.setStartSize(TILE_SIZE / 2);
        particles.setEndSize(TILE_SIZE);
        particles.setStartColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
        particles.setEndColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 0f));
        particles.getParticleController().setRepeatType(Controller.RT_CLAMP);
        
        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.blendAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);
        
        return particles;
    }
    
    protected static ArrayList<ParticleMesh> _dustRings =
        new ArrayList<ParticleMesh>();
    protected static ArrayList<ParticleMesh> _fireballs =
        new ArrayList<ParticleMesh>();
    protected static ArrayList<ParticleMesh> _streamers =
        new ArrayList<ParticleMesh>();
    protected static ArrayList<ParticleMesh> _smokePuffs =
        new ArrayList<ParticleMesh>();
    protected static ArrayList<ParticleMesh> _sparkles =
        new ArrayList<ParticleMesh>();
    protected static ArrayList<ParticleMesh> _steamClouds =
        new ArrayList<ParticleMesh>();
    protected static TextureState _dusttex;
}
