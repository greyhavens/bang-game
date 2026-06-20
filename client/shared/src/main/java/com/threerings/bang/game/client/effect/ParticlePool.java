//
// $Id$

package com.threerings.bang.game.client.effect;

import java.util.ArrayList;
import java.util.HashMap;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Spatial;

import com.samskivert.util.ResultListener;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.ParticleUtil;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Creates and recycles transient particle systems.
 *
 * <h3>jME3 cutover (Phase 2, cluster 2 — particle re-author)</h3>
 *
 * The fork built each pooled system as a {@code com.jmex.effects.particles.ParticleMesh} with a
 * {@code ParticleController} and fork render-state objects. jME3's analogue is a
 * {@link ParticleEmitter} (migration map §2.11): a {@code Geometry} carrying a single
 * {@code ParticleInfluencer}, so the pooled object is itself a {@link Spatial} that is attached to
 * the scene and recycled. The fork field mapping used below:
 *
 * <ul>
 *   <li>{@code setMinimum/MaximumLifeTime(ms)} → {@link ParticleEmitter#setLowLife}/
 *   {@link ParticleEmitter#setHighLife}{@code (seconds)}.</li>
 *   <li>{@code setReleaseRate(int)} → {@link ParticleEmitter#setParticlesPerSec(float)}.</li>
 *   <li>{@code setInitialVelocity}/{@code setEmissionDirection} → the influencer's initial-velocity
 *   vector.</li>
 *   <li>the fork {@code originOffset} (the moving emission point the streamer/swirl vizes animate)
 *   → the emitter's local translation (it <em>is</em> the geometry).</li>
 *   <li>{@code isActive()} / {@code getParticleController().setActive(boolean)} → emitting state
 *   ({@link ParticleEmitter#getParticlesPerSec()} {@code > 0} / {@link #setActive}).</li>
 * </ul>
 *
 * <p><b>Phase-4 fidelity deferral:</b> the in-code emitter params below are a faithful translation
 * of the fork values, but the fork particle <em>appearance</em> (sprite texture atlas frames, the
 * exact fade/blend curves) is part of the offline {@code particles.j3o} re-authoring scheduled for
 * Phase 4. These pooled emitters render with a single dust-texture {@code Unshaded} material in the
 * transparent bucket so they compile and degrade to a plausible blob; the precise per-effect MatDef
 * is a Phase-4 visual-review item.
 */
public class ParticlePool
{
    public static void warmup (BangContext ctx)
    {
        _ctx = ctx;
        // jME3: the pooled dust/streamer/sparkle/steam emitters share a single Unshaded material
        // textured with the dust sprite (the fork shared a TextureState). The per-effect MatDef is
        // re-authored in Phase 4.
        _dustmat = RenderUtil.createTextureMaterial(ctx, "textures/effects/dust.png");
        RenderUtil.applyBlendAlpha(_dustmat);
        RenderUtil.applyOverlayZBuf(_dustmat);

        // The fork gave the streamer + sparkle emitters an ADDITIVE blend (RenderUtil.addAlpha:
        // SRC_ALPHA / ONE) for emissive glow accumulation, while dustRing + steamCloud used the
        // plain alpha blendAlpha. Replicate that split with a second additive dust material (same
        // texture + overlay depth: less-or-equal test, no depth write).
        _dustaddmat = RenderUtil.createTextureMaterial(ctx, "textures/effects/dust.png");
        RenderUtil.applyAddAlpha(_dustaddmat);
        RenderUtil.applyOverlayZBuf(_dustaddmat);
    }

    public static void clear ()
    {
        _effects.clear();
        _dustRings.clear();
        _streamers.clear();
        _sparkles.clear();
        _steamClouds.clear();
    }

    public static ParticleEmitter getDustRing ()
    {
        for (int x = 0, tSize = _dustRings.size(); x < tSize; x++) {
            ParticleEmitter e = _dustRings.get(x);
            if (!isActive(e)) {
                setActive(e, true);
                return e;
            }
        }
        ParticleEmitter dustRing = createDustRing();
        _dustRings.add(dustRing);
        return dustRing;
    }

    public static ParticleEmitter getStreamer ()
    {
        for (int x = 0, tSize = _streamers.size(); x < tSize; x++) {
            ParticleEmitter e = _streamers.get(x);
            if (!isActive(e)) {
                setActive(e, true);
                return e;
            }
        }
        ParticleEmitter streamer = createStreamer();
        _streamers.add(streamer);
        return streamer;
    }

    public static ParticleEmitter getSparkles ()
    {
        for (int x = 0, tSize = _sparkles.size(); x < tSize; x++) {
            ParticleEmitter e = _sparkles.get(x);
            if (!isActive(e)) {
                setActive(e, true);
                return e;
            }
        }
        ParticleEmitter sparkles = createSparkles();
        _sparkles.add(sparkles);
        return sparkles;
    }

    public static ParticleEmitter getSteamCloud ()
    {
        for (int x = 0, tSize = _steamClouds.size(); x < tSize; x++) {
            ParticleEmitter e = _steamClouds.get(x);
            if (!isActive(e)) {
                setActive(e, true);
                return e;
            }
        }
        ParticleEmitter steamCloud = createSteamCloud();
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
                spatial.updateGeometricState();
                return;
            }
        }
        final ArrayList<Spatial> fparticles = particles;
        _ctx.loadParticles(name, new ResultListener<Spatial>() {
            public void requestCompleted (Spatial result) {
                result.addControl(new ParticleUtil.ParticleRemover(result));
                fparticles.add(result);
                rl.requestCompleted(result);
                result.updateGeometricState();
                ParticleUtil.forceRespawn(result);
            }
            public void requestFailed (Exception cause) {
                rl.requestFailed(cause);
            }
        });
    }

    /**
     * Whether the supplied pooled emitter is currently emitting or still has live particles
     * (the jME3 analogue of the fork {@code ParticleMesh.isActive()}).
     */
    public static boolean isActive (ParticleEmitter emitter)
    {
        return emitter.getParticlesPerSec() > 0f || emitter.getNumVisibleParticles() > 0;
    }

    /**
     * Reactivates ({@code true}) or stops ({@code false}) a pooled emitter (the jME3 analogue of
     * the fork {@code getParticleController().setActive(boolean)}). Reactivation restores the
     * emitter's configured release rate and forces a respawn; stopping clamps the release rate to
     * zero and detaches the emitter from its parent (the fork's transient-controller behaviour).
     */
    public static void setActive (ParticleEmitter emitter, boolean active)
    {
        if (active) {
            Float rate = _rates.get(emitter);
            emitter.setParticlesPerSec(rate == null ? 0f : rate);
            emitter.emitAllParticles();
        } else {
            emitter.setParticlesPerSec(0f);
            if (emitter.getParent() != null) {
                emitter.getParent().detachChild(emitter);
            }
        }
    }

    protected static ParticleEmitter newEmitter (String name, int count)
    {
        return newEmitter(name, count, _dustmat);
    }

    protected static ParticleEmitter newEmitter (String name, int count, Material material)
    {
        ParticleEmitter emitter = new ParticleEmitter(name, ParticleMesh.Type.Triangle, count);
        emitter.setMaterial(material);
        emitter.setQueueBucket(Bucket.Transparent);
        emitter.setShadowMode(com.jme3.renderer.queue.RenderQueue.ShadowMode.Off);
        return emitter;
    }

    /** Records (and applies) a pooled emitter's nominal release rate so {@link #setActive}
     * can restore it on recycle. */
    protected static void setReleaseRate (ParticleEmitter emitter, float rate)
    {
        _rates.put(emitter, rate);
        emitter.setParticlesPerSec(rate);
    }

    protected static ParticleEmitter createDustRing ()
    {
        ParticleEmitter particles = newEmitter("dustring", 64);
        particles.setLowLife(0.5f);
        particles.setHighLife(1.5f);
        particles.getParticleInfluencer().setInitialVelocity(
            Vector3f.UNIT_Z.mult(0.02f));
        particles.getParticleInfluencer().setVelocityVariation(1f);
        particles.setRotateSpeed(0.1f);
        particles.setStartSize(TILE_SIZE / 5);
        particles.setEndSize(TILE_SIZE / 3);
        setReleaseRate(particles, 0f); // emits in a burst on respawn
        return particles;
    }

    protected static ParticleEmitter createStreamer ()
    {
        ParticleEmitter particles = newEmitter("streamer", 64, _dustaddmat);
        particles.setLowLife(0.25f);
        particles.setHighLife(0.75f);
        particles.getParticleInfluencer().setInitialVelocity(Vector3f.ZERO.clone());
        particles.getParticleInfluencer().setVelocityVariation(0f);
        particles.setStartSize(TILE_SIZE / 25);
        particles.setEndSize(TILE_SIZE / 10);
        particles.setStartColor(new ColorRGBA(1f, 1f, 0.5f, 1f));
        particles.setEndColor(new ColorRGBA(1f, 0.25f, 0f, 1f));
        setReleaseRate(particles, 512f);
        return particles;
    }

    protected static ParticleEmitter createSparkles ()
    {
        ParticleEmitter particles = newEmitter("sparkles", 64, _dustaddmat);
        particles.setLowLife(0.25f);
        particles.setHighLife(0.75f);
        particles.getParticleInfluencer().setInitialVelocity(Vector3f.ZERO.clone());
        particles.getParticleInfluencer().setVelocityVariation(0f);
        particles.setStartSize(TILE_SIZE / 25);
        particles.setEndSize(TILE_SIZE / 10);
        particles.setStartColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 1f));
        particles.setEndColor(new ColorRGBA(0f, 0f, 0f, 1f));
        setReleaseRate(particles, 512f);
        return particles;
    }

    protected static ParticleEmitter createSteamCloud ()
    {
        ParticleEmitter particles = newEmitter("steamcloud", 32);
        particles.setLowLife(0.5f);
        particles.setHighLife(1.5f);
        particles.setLocalTranslation(0f, 0f, TILE_SIZE * 0.75f);
        particles.getParticleInfluencer().setInitialVelocity(
            Vector3f.UNIT_Z.mult(0.001f));
        particles.getParticleInfluencer().setVelocityVariation(0.5f);
        particles.setStartSize(TILE_SIZE / 2);
        particles.setEndSize(TILE_SIZE);
        particles.setStartColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
        particles.setEndColor(new ColorRGBA(0.75f, 0.75f, 0.75f, 0f));
        setReleaseRate(particles, 0f); // emits in a burst on respawn
        return particles;
    }

    protected static BangContext _ctx;
    protected static HashMap<String, ArrayList<Spatial>> _effects =
        new HashMap<String, ArrayList<Spatial>>();

    protected static ArrayList<ParticleEmitter> _dustRings =
        new ArrayList<ParticleEmitter>();
    protected static ArrayList<ParticleEmitter> _streamers =
        new ArrayList<ParticleEmitter>();
    protected static ArrayList<ParticleEmitter> _sparkles =
        new ArrayList<ParticleEmitter>();
    protected static ArrayList<ParticleEmitter> _steamClouds =
        new ArrayList<ParticleEmitter>();

    /** The shared alpha-blended dust-texture material used by the dustRing + steamCloud emitters
     * (fork {@code blendAlpha}; Phase-4 re-author target). */
    protected static Material _dustmat;

    /** The shared additive dust-texture material used by the streamer + sparkle emitters for
     * emissive glow accumulation (fork {@code addAlpha}: SRC_ALPHA / ONE). */
    protected static Material _dustaddmat;

    /** Each pooled emitter's nominal release rate, restored by {@link #setActive}. */
    protected static HashMap<ParticleEmitter, Float> _rates =
        new HashMap<ParticleEmitter, Float>();
}
