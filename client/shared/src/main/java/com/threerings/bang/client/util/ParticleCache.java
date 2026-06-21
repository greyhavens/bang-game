//
// $Id$

package com.threerings.bang.client.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

import com.jme3.asset.ModelKey;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import com.samskivert.util.ResultListener;

import com.threerings.media.image.Colorization;

import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Maintains a cache of particle system effects.
 *
 * <h3>jME3 cutover (Phase 2, cluster 6 — particle load path)</h3>
 *
 * The fork loaded each effect's {@code particles.jme} (a fork {@code BinaryExporter}
 * {@code ParticleGeometry} graph) through the fork {@code BinaryImporter}, then deep-copied every
 * field of the prototype {@code ParticleGeometry} to build an instance. jME3 cannot read the
 * fork-format {@code .jme} binaries (migration map §5.2: the 60 effect definitions are an
 * <b>offline re-authoring</b> to jME3 {@code ParticleEmitter} params, scheduled for Phase 4).
 *
 * <p>So the load path is retargeted at the jME3-native converted asset
 * ({@code effects/<key>/particles.j3o}, produced by the Phase-4 converter) loaded through the
 * {@code AssetManager}, and instancing becomes a structural {@code clone()} rather than a
 * field-by-field copy. The {@code particles.properties} sidecar (scale, bounds) is still honoured.
 * Until the Phase-4 converter has produced the {@code .j3o} corpus, {@link #loadPrototype} returns
 * an empty placeholder node and logs a warning rather than failing the build/run; the
 * effect-spawn call sites degrade to "no visible effect", which is the agreed Phase-2 behaviour
 * (visual fidelity is Phase 4).
 */
public class ParticleCache extends PrototypeCache<String, Spatial>
{
    /** The rotation from y-up coordinates to z-up coordinates. */
    public static final Quaternion Z_UP_ROTATION = new Quaternion();
    static {
        Z_UP_ROTATION.fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
    }

    public ParticleCache (BasicContext ctx)
    {
        super(ctx);
        Collections.addAll(_particles, BangUtil.townResourceToStrings(
            "rsrc/effects/TOWN/particles.txt"));
    }

    /**
     * Determines whether the named particle effect exists.
     */
    public boolean haveParticles (String name)
    {
        return _particles.contains(name);
    }

    /**
     * Loads an instance of the specified effect.
     *
     * @param rl the listener to notify with the resulting effect
     */
    public void getParticles (String name, ResultListener<Spatial> rl)
    {
        getInstance(name, null, rl);
    }

    // documentation inherited
    protected Spatial loadPrototype (final String key)
        throws Exception
    {
        // jME3: load the converted, jME3-native particle asset rather than the fork .jme binary.
        Spatial particles;
        String assetPath = "effects/" + key + "/particles.j3o";
        try {
            particles = _ctx.getAssetManager().loadModel(new ModelKey(assetPath));
        } catch (Exception e) {
            // Phase-4 gate: the converted .j3o corpus is not yet produced; degrade gracefully.
            log.warning("Particle effect not yet converted to jME3 format; " +
                "showing nothing (Phase 4 will re-author the effect)", "effect", key);
            return new Node("effect:" + key);
        }
        if (!(particles instanceof Node)) {
            // wrap in container to preserve relative transforms
            Node container = new Node("effect");
            container.attachChild(particles);
            particles = container;
        }

        Properties props = new Properties();
        props.load(_ctx.getResourceManager().getResource(
            "effects/" + key + "/particles.properties"));
        particles.setLocalScale(Float.parseFloat(
            props.getProperty("scale", "0.025")));
        particles.setLocalRotation(Z_UP_ROTATION.clone());

        // jME3 computes geometry bounds automatically; the fork's explicit box/sphere model-bound
        // prototype is no longer needed. The "bounds" property is preserved for the Phase-4
        // converter (which can bake the desired bound type onto each emitter's mesh if needed).
        return particles;
    }

    // documentation inherited
    protected void initPrototype (Spatial prototype)
    {
    }

    // documentation inherited
    protected Spatial createInstance (
        String key, Spatial prototype, Colorization[] zations)
    {
        // jME3: a structural clone reproduces the emitter graph and its parameters (the fork copied
        // every ParticleGeometry field by hand; ParticleEmitter.clone() does this for us).
        Spatial instance = prototype.clone();
        // Emitters carry their own materials from the converted asset; the overlay depth preset is
        // baked into the effect's MatDef during conversion (Phase 4). Here we only ensure the
        // effect renders in the transparent bucket (the fork applied RenderUtil.overlayZBuf).
        instance.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
        return instance;
    }

    /** The particle effects available for loading. */
    protected HashSet<String> _particles = new HashSet<String>();
}
