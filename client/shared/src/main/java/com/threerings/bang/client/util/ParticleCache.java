//
// $Id$

package com.threerings.bang.client.util;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.bounding.BoundingVolume;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.FogState;
import com.jme.scene.state.RenderState;
import com.jme.util.TextureKey;
import com.jme.util.export.binary.BinaryImporter;
import com.jmex.effects.particles.ParticleInfluence;
import com.jmex.effects.particles.ParticleFactory;
import com.jmex.effects.particles.ParticleGeometry;

import com.samskivert.util.ResultListener;

import com.threerings.jme.util.SpatialVisitor;
import com.threerings.media.image.Colorization;

import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;

/**
 * Maintains a cache of particle system effects.
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
        final File parent = _ctx.getResourceManager().getResourceFile(
            "effects/" + key);
        TextureKey.setLocationOverride(new TextureKey.LocationOverride() {
            public URL getLocation (String file)
                throws MalformedURLException {
                return new URL(parent.toURI().toURL(), file);
            }
        });
        Spatial particles = (Spatial)BinaryImporter.getInstance().load(
            _ctx.getResourceManager().getResource(
                "effects/" + key + "/particles.jme"));
        if (particles instanceof ParticleGeometry) {
            // wrap geometry in container to preserve relative transforms
            Node container = new Node("effect");
            container.attachChild(particles);
            particles = container;
        }
        TextureKey.setLocationOverride(null);
        Properties props = new Properties();
        props.load(_ctx.getResourceManager().getResource(
            "effects/" + key + "/particles.properties"));
        particles.setLocalScale(Float.parseFloat(
            props.getProperty("scale", "0.025")));
        particles.getLocalRotation().set(Z_UP_ROTATION);

        String bounds = props.getProperty("bounds", "box");
        BoundingVolume bproto = null;
        if ("box".equals(bounds)) {
            bproto = new BoundingBox();
        } else if ("sphere".equals(bounds)) {
            bproto = new BoundingSphere();
        } else if (!"none".equals(bounds)) {
            log.warning("Unknown bounding type for effect", "effect", key, "bounds", bounds);
        }
        if (bproto != null) {
            final BoundingVolume fproto = bproto;
            new SpatialVisitor<ParticleGeometry>(ParticleGeometry.class) {
                protected void visit (ParticleGeometry geom) {
                    if (!geom.isTransformParticles()) {
                        geom.getBatch(0).setModelBound(fproto.clone(null));
                    }
                }
            }.traverse(particles);
        }

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
        Spatial instance;
        if (prototype instanceof Node) {
            Node pnode = (Node)prototype,
                inode = new Node(prototype.getName());
            for (int ii = 0, nn = pnode.getQuantity(); ii < nn; ii++) {
                inode.attachChild(createInstance(
                    (ParticleGeometry)pnode.getChild(ii)));
            }
            inode.getLocalTranslation().set(pnode.getLocalTranslation());
            inode.getLocalRotation().set(pnode.getLocalRotation());
            inode.getLocalScale().set(pnode.getLocalScale());
            instance = inode;
        } else {
            instance = createInstance((ParticleGeometry)prototype);
        }
        instance.setRenderState(RenderUtil.overlayZBuf);
        instance.setIsCollidable(false);
        return instance;
    }

    protected ParticleGeometry createInstance (ParticleGeometry prototype)
    {
        // build an instance of the same type
        ParticleGeometry instance;
        int ptype = prototype.getParticleType();
        if (ptype == ParticleGeometry.PT_LINE) {
            instance = ParticleFactory.buildLineParticles(prototype.getName(),
                prototype.getNumParticles());
        } else if (ptype == ParticleGeometry.PT_POINT) {
            instance = ParticleFactory.buildPointParticles(prototype.getName(),
                prototype.getNumParticles());
        } else {
            instance = ParticleFactory.buildParticles(prototype.getName(),
                prototype.getNumParticles(), ptype);
        }

        // copy appearance parameters
        instance.setVelocityAligned(prototype.isVelocityAligned());
        instance.setStartColor(prototype.getStartColor());
        instance.setEndColor(prototype.getEndColor());
        instance.setStartSize(prototype.getStartSize());
        instance.setEndSize(prototype.getEndSize());
        instance.setAlphaFalloff(prototype.getAlphaFalloff());

        // copy origin parameters
        instance.getLocalTranslation().set(prototype.getLocalTranslation());
        instance.getLocalRotation().set(prototype.getLocalRotation());
        instance.getLocalScale().set(prototype.getLocalScale());
        instance.setOriginOffset(prototype.getOriginOffset());
        instance.setGeometry(prototype.getLine());
        instance.setGeometry(prototype.getRectangle());
        instance.setGeometry(prototype.getRing());
        instance.setGeometry(prototype.getFrustum());
        instance.setEmitType(prototype.getEmitType());

        // copy emission parameters
        instance.setRotateWithScene(true);
        instance.setTransformParticles(prototype.isTransformParticles());
        instance.setEmissionDirection(prototype.getEmissionDirection());
        instance.setMinimumAngle(prototype.getMinimumAngle());
        instance.setMaximumAngle(prototype.getMaximumAngle());
        instance.setInitialVelocity(prototype.getInitialVelocity());
        instance.setParticleSpinSpeed(prototype.getParticleSpinSpeed());

        // copy flow parameters
        instance.setControlFlow(prototype.getParticleController().isControlFlow());
        instance.setReleaseRate(prototype.getReleaseRate());
        instance.setReleaseVariance(prototype.getReleaseVariance());
        instance.setRepeatType(prototype.getParticleController().getRepeatType());

        // copy world parameters
        instance.setSpeed(prototype.getParticleController().getSpeed());
        instance.getParticleController().setPrecision(
            prototype.getParticleController().getPrecision());
        instance.setParticleMass(prototype.getParticle(0).getMass());
        instance.setMinimumLifeTime(prototype.getMinimumLifeTime());
        instance.setMaximumLifeTime(prototype.getMaximumLifeTime());

        // copy influence parameters
        List<ParticleInfluence> infs = prototype.getInfluences();
        if (infs != null) {
            for (ParticleInfluence inf : infs) {
                instance.addInfluence(inf);
            }
        }

        // copy render states
        for (int ii = 0; ii < RenderState.RS_MAX_STATE; ii++) {
            RenderState rs = prototype.getRenderState(ii);
            if (rs != null) {
                instance.setRenderState(rs);
            }
        }

        // if the particle system is emissive, disable fog
        AlphaState astate = (AlphaState)prototype.getRenderState(
            RenderState.RS_ALPHA);
        if (astate != null && astate.getDstFunction() == AlphaState.DB_ONE) {
            FogState fstate = _ctx.getRenderer().createFogState();
            fstate.setEnabled(false);
            instance.setRenderState(fstate);
        }

        // recreate the particles with new parameters and do any warmup
        // required
        instance.updateGeometricState(0f, false);
        instance.forceRespawn();
        instance.warmUp(instance.getParticleController().getIterations());

        // clone model bounds, if present
        BoundingVolume bounds = prototype.getBatch(0).getModelBound();
        if (bounds != null) {
            instance.getBatch(0).setModelBound(bounds.clone(null));
        }

        return instance;
    }

    /** The particle effects available for loading. */
    protected HashSet<String> _particles = new HashSet<String>();
}
