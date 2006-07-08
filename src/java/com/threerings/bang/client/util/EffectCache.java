//
// $Id$

package com.threerings.bang.client.util;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Properties;

import com.jme.bounding.BoundingBox;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.state.RenderState;
import com.jme.util.TextureKey;
import com.jme.util.export.binary.BinaryImporter;
import com.jmex.effects.particles.ParticleInfluence;
import com.jmex.effects.particles.ParticleFactory;
import com.jmex.effects.particles.ParticleGeometry;

import com.samskivert.util.ResultListener;

import com.threerings.media.image.Colorization;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;

/**
 * Maintains a cache of particle system effects.
 */
public class EffectCache extends PrototypeCache<Spatial>
{
    public EffectCache (BasicContext ctx)
    {
        super(ctx);
    }
    
    /**
     * Loads an instance of the specified effect.
     *
     * @param rl the listener to notify with the resulting effect
     */
    public void getEffect (String name, ResultListener<Spatial> rl)
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
                return new URL(parent.toURL(), file);
            }
        });
        Spatial particles = (Spatial)BinaryImporter.getInstance().load(
            _ctx.getResourceManager().getResource(
                "effects/" + key + "/effect.jme"));
        TextureKey.setLocationOverride(null);
        Properties props = new Properties();
        props.load(_ctx.getResourceManager().getResource(
            "effects/" + key + "/effect.properties"));
        particles.getLocalScale().multLocal(Float.parseFloat(
            props.getProperty("scale", "0.025")));
        particles.getLocalRotation().multLocal(Z_UP_ROTATION);
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
        
        // copy origin parameters
        instance.setLocalTranslation(prototype.getLocalTranslation());
        instance.setLocalRotation(prototype.getLocalRotation());
        instance.setLocalScale(prototype.getLocalScale());
        instance.setOriginOffset(prototype.getOriginOffset());
        instance.setGeometry(prototype.getLine());
        instance.setGeometry(prototype.getRectangle());
        instance.setGeometry(prototype.getRing());
        instance.setEmitType(prototype.getEmitType());
        
        // copy emission parameters
        instance.setRotateWithScene(true);
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
        ArrayList<ParticleInfluence> infs = prototype.getInfluences();
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
        
        instance.setModelBound(new BoundingBox());
        
        return instance;
    }
    
    /** The rotation from y-up coordinates to z-up coordinates. */
    public static final Quaternion Z_UP_ROTATION = new Quaternion();
    static {
        Z_UP_ROTATION.fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
    }
}
