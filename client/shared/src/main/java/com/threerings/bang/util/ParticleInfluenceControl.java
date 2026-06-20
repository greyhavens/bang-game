//
// $Id$

package com.threerings.bang.util;

import java.io.IOException;
import java.util.ArrayList;

import com.jme3.effect.Particle;
import com.jme3.effect.ParticleEmitter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.clone.Cloner;

/**
 * A per-frame particle-velocity influence baked into a jME3 {@code particles.j3o} and re-applied
 * every frame at runtime — the jME3 analogue of the fork's per-frame
 * {@code com.jmex.effects.particles.ParticleInfluence#apply(dt, particle, index)}.
 *
 * <h3>Why a {@code Control} and not a {@code ParticleInfluencer}</h3>
 *
 * jME3's stock {@link com.jme3.effect.influencers.ParticleInfluencer} runs only <b>once, at
 * spawn</b> (it sets a particle's initial position/velocity); jME3's
 * {@link ParticleEmitter#updateParticle} then integrates {@code position += velocity * tpf} and
 * applies only a constant {@code gravity} each frame — it never calls the influencer again. The
 * fork's Drag/Vortex/Swarm/Wander influences instead modify each live particle's <b>velocity every
 * frame</b>. There is therefore no stock jME3 hook for them. This {@link AbstractControl}, attached
 * directly to the {@link ParticleEmitter} spatial, supplies the missing per-frame pass: each update
 * it walks {@link ParticleEmitter#getParticles()} and re-applies the fork influence math to every
 * live particle's {@code velocity}. Being an {@code AbstractControl} it is {@code Savable} (baked
 * into the {@code .j3o} and reloaded by the runtime {@code AssetManager}) and clone-correct
 * (jME3 re-attaches the cloned control to the cloned emitter via the {@code Cloner}), so the
 * build-time {@code ParticleConverter} can emit it and {@code ParticleCache.createInstance}'s
 * {@code clone()} reproduces it.
 *
 * <h3>Implemented influences (faithful)</h3>
 * <ul>
 *   <li>{@link Type#DRAG} — the fork {@code SimpleParticleInfluenceFactory.BasicDrag}: viscous
 *       deceleration {@code velocity += velocity * (-dragCoefficient * dt * invMass)} (invMass==1
 *       for Bang particles). This is a pure per-step scaling of the velocity vector, so it is
 *       <b>magnitude/scale-invariant</b>: applying it to the converter's baked velocity (which
 *       carries the fork's {@code x1000} ms factor and the world-space node scale) reproduces the
 *       fork's deceleration exactly with no extra scaling.</li>
 *   <li>{@link Type#WANDER} — the fork {@code WanderInfluence}: a per-particle random-walk that
 *       jitters a unit "wander target", blends it with the particle's heading, and re-points the
 *       velocity along the result while <b>preserving the velocity's length</b>. Because it only
 *       rotates the (already-baked) velocity and renormalizes to the same length, it too is
 *       scale-invariant and faithful against the baked velocities. The fork seeds each target from
 *       the emitter's emission direction; the per-particle targets are kept here keyed by particle
 *       index (the fork keyed them the same way).</li>
 * </ul>
 *
 * <p>{@code Vortex} and {@code Swarm} are <b>not</b> implemented here: both are position-dependent
 * (they read the particle's world position relative to a world-space axis/swarm point that the fork
 * established in {@code prepare()} from the emitter transform) and add velocity in fork per-ms units
 * that would need the same {@code x1000}/scale bake as the initial velocity. They are recorded as a
 * bounded remainder by the converter (see {@code ParticleConverter.applyInfluences}).
 */
public class ParticleInfluenceControl extends AbstractControl
{
    /** Which fork influence this control reproduces. */
    public enum Type { DRAG, WANDER, WIND }

    /** Required no-arg constructor for {@code Savable} deserialization. */
    public ParticleInfluenceControl ()
    {
    }

    /** Drag: {@code dragCoefficient} is the fork {@code BasicDrag.dragCoefficient}. */
    public static ParticleInfluenceControl drag (float dragCoefficient)
    {
        ParticleInfluenceControl c = new ParticleInfluenceControl();
        c._type = Type.DRAG;
        c._dragCoefficient = dragCoefficient;
        return c;
    }

    /**
     * Wander: the fork {@code WanderInfluence} radius/distance/jitter plus the emitter's emission
     * direction (normalized) used to seed each per-particle wander target.
     */
    public static ParticleInfluenceControl wander (
        float wanderRadius, float wanderDistance, float wanderJitter, Vector3f emissionDirection)
    {
        ParticleInfluenceControl c = new ParticleInfluenceControl();
        c._type = Type.WANDER;
        c._wanderRadius = wanderRadius;
        c._wanderDistance = wanderDistance;
        c._wanderJitter = wanderJitter;
        c._emissionDirection = emissionDirection.clone();
        return c;
    }

    /**
     * Wind: the fork board {@code BasicWind} added to runtime-spawned effects by
     * {@code BoardView.addWindInfluence}. {@code windAccel} is the per-second velocity change
     * already expressed in the converter's baked world-velocity units (the fork {@code windDir *
     * strength} multiplied by the same {@code x1000 * effectScale} motion factor the converter
     * bakes into initial velocity), so the per-frame add is simply {@code velocity += windAccel *
     * (random * tpf)} — matching {@code BasicWind.apply}'s {@code velocity.scaleAdd(tStr*dt, vector,
     * velocity)} with {@code tStr = random ? rand*strength : strength}. The fork board wind was
     * created with {@code random == true}.
     */
    public static ParticleInfluenceControl wind (Vector3f windAccel, boolean random)
    {
        ParticleInfluenceControl c = new ParticleInfluenceControl();
        c._type = Type.WIND;
        c._windAccel = windAccel.clone();
        c._windRandom = random;
        return c;
    }

    /**
     * Updates the wind acceleration of an existing WIND control in place (the fork shared a single
     * mutable {@code BasicWind} whose direction/strength {@code BoardView.refreshWindInfluence}
     * rewrote when the board wind changed).
     */
    public void setWindAccel (Vector3f windAccel)
    {
        _windAccel.set(windAccel);
    }

    /** @return this control's {@link Type}. */
    public Type getInfluenceType ()
    {
        return _type;
    }

    @Override // documentation inherited
    protected void controlUpdate (float tpf)
    {
        if (tpf <= 0f || !(spatial instanceof ParticleEmitter emitter)) {
            return;
        }
        switch (_type) {
        case DRAG:
            applyDrag(emitter, tpf);
            break;
        case WANDER:
            applyWander(emitter, tpf);
            break;
        case WIND:
            applyWind(emitter, tpf);
            break;
        }
    }

    /**
     * Fork {@code SimpleParticleInfluenceFactory.BasicWind.apply}:
     * <pre>float tStr = random ? rand*strength : strength;
     * velocity.scaleAdd(tStr * dt, windVector, velocity);</pre>
     * Here {@code _windAccel} already folds {@code windDir * strength} (and the converter's baked
     * world-velocity scale), so the per-particle step is {@code velocity += _windAccel * (r * dt)}.
     */
    protected void applyWind (ParticleEmitter emitter, float tpf)
    {
        for (Particle p : emitter.getParticles()) {
            if (p.life > 0f) {
                float r = _windRandom ? FastMath.nextRandomFloat() : 1f;
                p.velocity.x += _windAccel.x * r * tpf;
                p.velocity.y += _windAccel.y * r * tpf;
                p.velocity.z += _windAccel.z * r * tpf;
            }
        }
    }

    /**
     * Fork {@code SimpleParticleInfluenceFactory.BasicDrag.apply}:
     * <pre>velocity += velocity * (-dragCoefficient * dt * invMass)</pre>
     * with invMass == 1 (the fork {@code Particle} default; Bang never sets a particle mass).
     */
    protected void applyDrag (ParticleEmitter emitter, float tpf)
    {
        float factor = 1f + (-_dragCoefficient * tpf); // invMass == 1
        for (Particle p : emitter.getParticles()) {
            if (p.life > 0f) {
                p.velocity.multLocal(factor);
            }
        }
    }

    /**
     * Fork {@code WanderInfluence.apply}: jitter a unit per-particle wander target, blend it with
     * the (unit) particle heading scaled by {@code wanderDistance}, renormalize, and scale back to
     * the particle's current speed (so wander only re-points the velocity, never changes its
     * magnitude).
     */
    protected void applyWander (ParticleEmitter emitter, float tpf)
    {
        if (_wanderRadius == 0f && _wanderDistance == 0f && _wanderJitter == 0f) {
            return;
        }
        Particle[] particles = emitter.getParticles();
        // (re)seed per-particle wander targets from the emission direction, exactly as the fork did
        // in WanderInfluence.prepare() (one normalized emissionDirection per particle).
        if (_wanderTargets == null || _wanderTargets.size() != particles.length) {
            _wanderTargets = new ArrayList<Vector3f>(particles.length);
            Vector3f seed = _emissionDirection.clone();
            if (seed.lengthSquared() < 1e-8f) {
                seed.set(0f, 1f, 0f);
            }
            seed.normalizeLocal();
            for (int x = particles.length; --x >= 0; ) {
                _wanderTargets.add(seed.clone());
            }
        }
        if (_workVect == null) {
            _workVect = new Vector3f();
        }
        for (int index = 0; index < particles.length; index++) {
            Particle p = particles[index];
            if (p.life <= 0f) {
                continue;
            }
            Vector3f target = _wanderTargets.get(index);
            target.addLocal(calcNewJitter(), calcNewJitter(), calcNewJitter());
            target.normalizeLocal();
            target.multLocal(_wanderRadius);

            float speed = p.velocity.length();
            if (speed == 0f) {
                continue;
            }
            _workVect.set(p.velocity).normalizeLocal().multLocal(_wanderDistance);
            _workVect.addLocal(target).normalizeLocal();
            _workVect.multLocal(speed);
            p.velocity.set(_workVect);
        }
    }

    private float calcNewJitter ()
    {
        return ((FastMath.nextRandomFloat() * 2.0f) - 1.0f) * _wanderJitter;
    }

    @Override // documentation inherited
    protected void controlRender (RenderManager rm, ViewPort vp)
    {
        // nothing to render
    }

    @Override // documentation inherited
    public void cloneFields (Cloner cloner, Object original)
    {
        super.cloneFields(cloner, original);
        // per-particle wander targets are transient working state; let the clone re-seed lazily
        // against its own (cloned) emitter rather than sharing the source list.
        _wanderTargets = null;
        _workVect = null;
        _emissionDirection = (_emissionDirection == null) ? null : _emissionDirection.clone();
        _windAccel = (_windAccel == null) ? null : _windAccel.clone();
    }

    @Override // documentation inherited
    public void write (JmeExporter ex) throws IOException
    {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(_type.name(), "influenceType", Type.DRAG.name());
        oc.write(_dragCoefficient, "dragCoefficient", 0f);
        oc.write(_wanderRadius, "wanderRadius", 0f);
        oc.write(_wanderDistance, "wanderDistance", 0f);
        oc.write(_wanderJitter, "wanderJitter", 0f);
        oc.write(_emissionDirection, "emissionDirection", null);
        oc.write(_windAccel, "windAccel", null);
        oc.write(_windRandom, "windRandom", true);
    }

    @Override // documentation inherited
    public void read (JmeImporter im) throws IOException
    {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        _type = Type.valueOf(ic.readString("influenceType", Type.DRAG.name()));
        _dragCoefficient = ic.readFloat("dragCoefficient", 0f);
        _wanderRadius = ic.readFloat("wanderRadius", 0f);
        _wanderDistance = ic.readFloat("wanderDistance", 0f);
        _wanderJitter = ic.readFloat("wanderJitter", 0f);
        _emissionDirection = (Vector3f)ic.readSavable("emissionDirection", null);
        if (_emissionDirection == null) {
            _emissionDirection = new Vector3f(0f, 1f, 0f);
        }
        _windAccel = (Vector3f)ic.readSavable("windAccel", null);
        if (_windAccel == null) {
            _windAccel = new Vector3f();
        }
        _windRandom = ic.readBoolean("windRandom", true);
    }

    /** Which influence this control reproduces. */
    protected Type _type = Type.DRAG;

    /** DRAG: the fork BasicDrag drag coefficient. */
    protected float _dragCoefficient;

    /** WANDER: the fork WanderInfluence parameters. */
    protected float _wanderRadius, _wanderDistance, _wanderJitter;

    /** WANDER: emission direction used to seed each per-particle wander target. */
    protected Vector3f _emissionDirection = new Vector3f(0f, 1f, 0f);

    /** WIND: per-second wind acceleration in the converter's baked world-velocity units. */
    protected Vector3f _windAccel = new Vector3f();

    /** WIND: whether the wind strength is randomly scaled per particle per frame (fork: true). */
    protected boolean _windRandom = true;

    /** WANDER: transient per-particle wander targets (re-seeded lazily, not serialized). */
    protected transient ArrayList<Vector3f> _wanderTargets;

    /** WANDER: transient scratch vector. */
    protected transient Vector3f _workVect;
}
