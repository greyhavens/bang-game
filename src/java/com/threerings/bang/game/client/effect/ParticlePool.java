//
// $Id$

package com.threerings.bang.game.client.effect;

import java.util.ArrayList;
import java.util.HashMap;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.scene.state.TextureState;
import com.jmex.effects.particles.ParticleController;
import com.jmex.effects.particles.ParticleFactory;
import com.jmex.effects.particles.ParticleMesh;
import com.samskivert.util.ResultListener;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.ParticleUtil;
import com.threerings.bang.util.RenderUtil;

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
        _ctx = ctx;
        _dusttex = RenderUtil.createTextureState(
            ctx, "textures/effects/dust.png");
        _dusttex.getTexture().setWrap(Texture.WM_BCLAMP_S_BCLAMP_T);
    }

    public static void clear ()
    {
        _effects.clear();
        _dustRings.clear();
        _streamers.clear();
        _sparkles.clear();
        _steamClouds.clear();
    }

    public static ParticleMesh getDustRing ()
    {
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

    public static ParticleMesh getStreamer ()
    {
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

    public static ParticleMesh getSparkles ()
    {
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

    public static void getParticles (
        String name, final ResultAttacher<Spatial> rl)
    {
        ArrayList<Spatial> particles = _effects.get(name);
        if (particles == null) {
            _effects.put(name, particles = new ArrayList<Spatial>());
        }
        for (Spatial spatial : particles) {
            if (spatial.getParent() == null) {
                rl.requestCompleted(spatial);
                ParticleUtil.forceRespawn(spatial);
                spatial.updateGeometricState(0f, false);
                return;
            }
        }
        final ArrayList<Spatial> fparticles = particles;
        _ctx.loadParticles(name, new ResultListener<Spatial>() {
            public void requestCompleted (Spatial result) {
                result.addController(new ParticleUtil.ParticleRemover(result));
                fparticles.add(result);
                rl.requestCompleted(result);
                result.updateGeometricState(0f, false);
                ParticleUtil.forceRespawn(result);
            }
            public void requestFailed (Exception cause) {
                rl.requestFailed(cause);
            }
        });
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
        particles.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        particles.getParticleController().setControlFlow(false);
        particles.setParticleSpinSpeed(0.1f);
        particles.setStartSize(TILE_SIZE / 5);
        particles.setEndSize(TILE_SIZE / 3);
        particles.getParticleController().setRepeatType(Controller.RT_CLAMP);
        particles.setModelBound(new BoundingBox());
        particles.setIsCollidable(false);

        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.blendAlpha);
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
        particles.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        particles.getParticleController().setControlFlow(true);
        particles.setReleaseRate(512);
        particles.getParticleController().setReleaseVariance(0f);
        particles.setParticleSpinSpeed(0f);
        particles.setStartSize(TILE_SIZE / 25);
        particles.setEndSize(TILE_SIZE / 10);
        particles.setStartColor(new ColorRGBA(1f, 1f, 0.5f, 1f));
        particles.setEndColor(new ColorRGBA(1f, 0.25f, 0f, 1f));
        particles.setModelBound(new BoundingBox());
        particles.setIsCollidable(false);

        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.addAlpha);
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
        particles.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        particles.getParticleController().setControlFlow(true);
        particles.setReleaseRate(512);
        particles.getParticleController().setReleaseVariance(0f);
        particles.setParticleSpinSpeed(0f);
        particles.setStartSize(TILE_SIZE / 25);
        particles.setEndSize(TILE_SIZE / 10);
        particles.setStartColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 1f));
        particles.setEndColor(new ColorRGBA(0f, 0f, 0f, 1f));
        particles.setModelBound(new BoundingBox());
        particles.setIsCollidable(false);

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
        particles.getParticleController().setPrecision(FastMath.FLT_EPSILON);
        particles.getParticleController().setControlFlow(false);
        particles.setStartSize(TILE_SIZE / 2);
        particles.setEndSize(TILE_SIZE);
        particles.setStartColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
        particles.setEndColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 0f));
        particles.getParticleController().setRepeatType(Controller.RT_CLAMP);
        particles.setModelBound(new BoundingBox());
        particles.setIsCollidable(false);

        particles.setRenderState(_dusttex);
        particles.setRenderState(RenderUtil.blendAlpha);
        particles.setRenderState(RenderUtil.overlayZBuf);

        return particles;
    }

    protected static BangContext _ctx;
    protected static HashMap<String, ArrayList<Spatial>> _effects =
        new HashMap<String, ArrayList<Spatial>>();

    protected static ArrayList<ParticleMesh> _dustRings =
        new ArrayList<ParticleMesh>();
    protected static ArrayList<ParticleMesh> _streamers =
        new ArrayList<ParticleMesh>();
    protected static ArrayList<ParticleMesh> _sparkles =
        new ArrayList<ParticleMesh>();
    protected static ArrayList<ParticleMesh> _steamClouds =
        new ArrayList<ParticleMesh>();
    protected static TextureState _dusttex;
}
