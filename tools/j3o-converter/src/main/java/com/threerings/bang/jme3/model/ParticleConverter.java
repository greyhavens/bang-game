//
// $Id$

package com.threerings.bang.jme3.model;

import java.util.ArrayList;
import java.util.List;

import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.AlphaState;
import com.jmex.effects.particles.ParticleGeometry;
import com.jmex.effects.particles.ParticleController;
import com.jmex.effects.particles.ParticleInfluence;
import com.jmex.effects.particles.SimpleParticleInfluenceFactory;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.effect.shapes.EmitterPointShape;
import com.jme3.effect.shapes.EmitterBoxShape;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 * Build-time re-authoring bridge: converts a fork
 * {@link com.jmex.effects.particles.ParticleGeometry} graph (deserialized from a fork-format
 * {@code particles.jme} via the fork {@code BinaryImporter}) into a jMonkeyEngine&nbsp;3
 * {@link ParticleEmitter} tree, so the runtime {@code ParticleCache} can load a jME3-native
 * {@code effects/<key>/particles.j3o} through the stock {@code AssetManager}.
 *
 * <p>This is the particle analogue of {@link ModelConverter}: it lives on the fork-coupled
 * build-time classpath (it reads {@code com.jmex.effects.particles.*} / {@code com.jme.*}) and
 * writes pure jME3, so no fork dependency reaches the runtime.
 *
 * <h3>Parameter mapping (fork {@code ParticleGeometry} &rarr; jME3 {@code ParticleEmitter})</h3>
 * <p>The fork's per-instance deep-copy in the pre-cutover {@code ParticleCache} is the de-facto
 * spec of which params matter; this converter mirrors it:
 * <ul>
 *   <li>{@code numParticles} &rarr; {@code setNumParticles}. Emission mode is keyed on the fork
 *       controller's <b>repeat type</b> (the one-shot-vs-loop signal): {@code RT_CLAMP} &rarr; a
 *       <em>one-shot burst</em> baked with {@code setParticlesPerSec(0)} (the runtime spawn path
 *       fires the single pool-sized burst via {@code emitAllParticles()}, then it fades and is
 *       detached); {@code RT_WRAP}/{@code RT_CYCLE} &rarr; a <em>continuous/ambient</em> emitter
 *       with a steady {@code setParticlesPerSec} (the fork {@code releaseRate} when {@code
 *       controlFlow} is set, else a refill at count/mean-lifetime), clamped to count/mean-lifetime
 *       so even an ambient effect can't blow out. Baking a positive rate on a one-shot emitter is
 *       what produced the runaway additive bloom the live acceptance caught.</li>
 *   <li>{@code startSize}/{@code endSize} &rarr; {@code setStartSize}/{@code setEndSize}.</li>
 *   <li>{@code startColor}/{@code endColor} &rarr; {@code setStartColor}/{@code setEndColor}.</li>
 *   <li>{@code minimumLifeTime}/{@code maximumLifeTime} (fork ms) &rarr;
 *       {@code setLowLife}/{@code setHighLife} (jME3 seconds; divided by 1000).</li>
 *   <li>{@code emissionDirection} &times; {@code initialVelocity} &rarr;
 *       {@code getParticleInfluencer().setInitialVelocity(...)}; the fork emits in a cone up to
 *       {@code maximumAngle} about that direction &rarr; {@code setVelocityVariation(...)}
 *       (1.0 at a full hemisphere / {@code PI/2} max angle).</li>
 *   <li>{@code particleSpinSpeed} &rarr; {@code setRotateSpeed} + {@code setRandomAngle(true)};
 *       {@code velocityAligned} &rarr; {@code setFacingVelocity(true)}.</li>
 *   <li>emit type (point/line/rectangle/ring) &rarr; {@code setShape(...)}: ET_POINT &rarr;
 *       {@link EmitterPointShape}; ET_RECTANGLE &rarr; {@link EmitterBoxShape}; ET_RING &rarr;
 *       {@link EmitterSphereShape} (closest stock shape); ET_LINE &rarr; a thin
 *       {@link EmitterBoxShape}.</li>
 *   <li>{@link SimpleParticleInfluenceFactory.BasicGravity} &rarr; {@code setGravity(...)};
 *       {@link SimpleParticleInfluenceFactory.BasicWind} folded into the gravity vector
 *       (jME3 has no separate wind force); {@code BasicDrag}/{@code SwarmInfluence}/
 *       {@code WanderInfluence} have <b>no jME3 equivalent</b> and are recorded as dropped.</li>
 *   <li>texture (the fork {@code TextureState}'s first texture's image location) &rarr; a
 *       {@code Common/MatDefs/Misc/Particle.j3md} material with that texture, {@code ImagesX=1}/
 *       {@code ImagesY=1} (the fork particle system carries no sprite sub-image grid); blend mode
 *       from the {@code AlphaState} (additive {@code DB_ONE} &rarr; {@link BlendMode#Additive},
 *       else {@link BlendMode#Alpha}).</li>
 *   <li>geometry type: fork {@code ParticleMesh} (quads, {@code PT_QUAD}) &rarr;
 *       {@link Type#Triangle} (jME3's quad-per-particle mesh); {@code PT_POINT}/{@code PT_LINE}
 *       degrade to triangle particles (jME3 stock has no point/line emitter) and are recorded.</li>
 * </ul>
 */
public class ParticleConverter
{
    /** What was and was not converted for a single particles.jme. */
    public static class Result
    {
        /** The converted jME3 root (a Node of one or more ParticleEmitters). */
        public Spatial root;
        /** Number of ParticleEmitters produced. */
        public int emitters;
        /** Fork influence/feature class names with no jME3 equivalent (recorded, not converted). */
        public final List<String> droppedInfluences = new ArrayList<>();
        /** Notes about lossy/degraded mappings (point/line particles, ring->sphere, etc.). */
        public final List<String> notes = new ArrayList<>();
        /** True if every emitter found a texture. */
        public boolean texturesResolved = true;
    }

    public ParticleConverter (AssetManager assetManager, String typePath)
    {
        this(assetManager, typePath, 1f);
    }

    /**
     * @param effectScale the effect node's {@code particles.properties} {@code scale} (the value the
     * runtime {@code ParticleCache} applies as the loaded prototype's {@code localScale}). The fork
     * particle systems author per-particle sizes in <em>pre-scale</em> world units (e.g. the
     * frontier_town explosion's 22.5/66 etc.) and rely on the scaled effect node to shrink the
     * rendered billboards. jME3's {@code ParticleEmitter}, however, draws world-space (non-following)
     * particles with {@code setIgnoreTransform(true)} — so the node {@code localScale} never reaches
     * the quad size (jME3's {@code emitParticle} comment: "TODO: Make scale relevant somehow??"). To
     * preserve the fork's apparent size we bake the scale into {@code start/endSize} here. (Spawn
     * <em>positions</em> are still scaled correctly at runtime: world-space emit transforms the
     * particle position by the node's world transform, which does include the scale.)
     */
    public ParticleConverter (AssetManager assetManager, String typePath, float effectScale)
    {
        _assets = assetManager;
        _typePath = typePath;
        _effectScale = (effectScale > 0f) ? effectScale : 1f;
    }

    /**
     * Converts a fork particle spatial (a {@link ParticleGeometry} or a {@code com.jme.scene.Node}
     * of them) to a jME3 {@link Spatial}.
     */
    public Result convert (com.jme.scene.Spatial forkRoot)
    {
        Result result = new Result();
        result.root = convertSpatial(forkRoot, result);
        return result;
    }

    protected Spatial convertSpatial (com.jme.scene.Spatial spatial, Result result)
    {
        if (spatial instanceof ParticleGeometry geom) {
            ParticleEmitter emitter = convertEmitter(geom, result);
            copyTransform(spatial, emitter);
            result.emitters++;
            return emitter;
        } else if (spatial instanceof com.jme.scene.Node fnode) {
            Node node = new Node(name(spatial, "effect"));
            for (int ii = 0, nn = fnode.getQuantity(); ii < nn; ii++) {
                Spatial child = convertSpatial(fnode.getChild(ii), result);
                if (child != null) {
                    node.attachChild(child);
                }
            }
            copyTransform(spatial, node);
            return node;
        }
        result.notes.add("skipped non-particle spatial " + spatial.getClass().getName());
        return null;
    }

    protected ParticleEmitter convertEmitter (ParticleGeometry geom, Result result)
    {
        int count = Math.max(1, geom.getNumParticles());

        // fork PT_QUAD -> jME3 Triangle (quad-per-particle) mesh; point/line degrade to triangle.
        int ptype = geom.getParticleType();
        if (ptype == ParticleGeometry.PT_POINT) {
            result.notes.add(name(geom, "?") + ": PT_POINT degraded to triangle particles");
        } else if (ptype == ParticleGeometry.PT_LINE) {
            result.notes.add(name(geom, "?") + ": PT_LINE degraded to triangle particles");
        }

        ParticleEmitter emitter =
            new ParticleEmitter(name(geom, "emitter"), Type.Triangle, count);

        // The fork authors per-particle sizes/velocities in PRE-scale world units and relies on the
        // scaled effect node (particles.properties scale) to shrink the whole rendered geometry. A
        // jME3 world-space (non-following) emitter is drawn with ignoreTransform == true, so the
        // node localScale never reaches the quad size NOR the per-step velocity integration (jME3's
        // emitParticle even comments "TODO: Make scale relevant somehow??"); we therefore bake the
        // effect scale into size/velocity/gravity for those emitters so the rendered size matches
        // the fork (BUG-7: the explosion's world-space emitters otherwise rendered ~1/scale = ~20x
        // too large). An emitter-local (transformParticles == true) emitter instead keeps
        // ignoreTransform == false, so the node scale DOES apply and we must NOT pre-bake it (that
        // would double-scale). Spawn positions/shape offsets are always transformed by the node at
        // emit time, so only size/velocity/gravity need the pre-bake.
        boolean worldSpace = !geom.isTransformParticles();
        float sizeScale = worldSpace ? _effectScale : 1f;

        // The fork integrates a particle's position in MILLISECONDS, not seconds:
        // {@code Particle.updateAndCheck} does {@code position.scaleAdd(secondsPassed * 1000f,
        // velocity, position)} — i.e. each step advances position by {@code velocity * (dt_seconds *
        // 1000)}. jME3's ParticleEmitter integrates {@code position += velocity * tpf_seconds}
        // (no x1000). So to reproduce the fork's per-second world travel the baked velocity must be
        // multiplied by 1000 on top of the node-scale factor. (Total burst spread over a particle's
        // life is {@code forkVel * lifeSpan_ms * nodeScale}; jME3 travel is {@code bakedVel *
        // life_sec = bakedVel * lifeSpan_ms/1000}; equating gives {@code bakedVel = forkVel * 1000 *
        // nodeScale}.) Without the x1000 the converter baked velocities ~1000x too small, so every
        // world-space burst (get_bonus star-burst, explosion) collapsed to a motionless clump at the
        // origin instead of radiating outward (BUG-12). The same ms factor applies to gravity (the
        // fork accumulates gravity into velocity, which then drives position through the same ms
        // integration). emitter-local (transformParticles==true) emitters keep the node transform at
        // runtime, but the ms-vs-seconds integration mismatch is independent of that, so the x1000
        // applies to them too (their node-scale is applied at render, not baked, hence no sizeScale).
        float motionScale = (worldSpace ? _effectScale : 1f) * MS_PER_SEC;

        // ---- appearance ----
        emitter.setStartSize(geom.getStartSize() * sizeScale);
        emitter.setEndSize(geom.getEndSize() * sizeScale);
        emitter.setStartColor(color(geom.getStartColor()));
        emitter.setEndColor(color(geom.getEndColor()));

        // ---- lifetime (fork ms -> jME3 seconds) ----
        float low = Math.max(MIN_LIFE_SEC, geom.getMinimumLifeTime() / 1000f);
        float high = Math.max(low, geom.getMaximumLifeTime() / 1000f);
        emitter.setLowLife(low);
        emitter.setHighLife(high);

        // ---- count / emission mode (burst vs ambient) ----
        // The fork controller's repeat type is the one-shot-vs-loop signal (ParticleController:
        // RT_CLAMP makes a dead particle stay dead and the controller go inactive once all particles
        // have died -> a single burst that fades; RT_WRAP/RT_CYCLE recreate particles forever -> a
        // continuous, looping/ambient population). Baking a non-zero particles-per-sec on a CLAMP
        // (one-shot) emitter is wrong: the runtime spawn path (ParticlePool.getParticles ->
        // ParticleUtil.forceRespawn -> emitAllParticles, then a ParticleRemover that detaches once
        // the emitter is idle) fires the burst itself, so a baked positive rate makes the emitter
        // emit forever, the remover never detaches it, and a dense additive burst accumulates into a
        // runaway bloom (the live-acceptance white/orange explosion swallowing the screen).
        emitter.setNumParticles(count);
        ParticleController ctrl = geom.getParticleController();
        int repeatType = (ctrl != null) ? ctrl.getRepeatType() : ParticleController.RT_WRAP;
        boolean oneShot = (repeatType == ParticleController.RT_CLAMP);
        if (oneShot) {
            // one-shot / burst: do NOT auto-emit. The spawn code's emitAllParticles() fires the
            // single pool-sized burst; it then fades over its lifetime and is detached.
            emitter.setParticlesPerSec(0f);
        } else {
            // ambient / continuous (RT_WRAP / RT_CYCLE): keep a sane steady emission so a looping
            // effect stays populated. Prefer the fork's regulated releaseRate when controlFlow is
            // set; otherwise refill the pool once per mean lifetime. Clamp the result so a stored
            // rate (or a dense pool over a short life) can't blow out -- the fork's controller only
            // ever recreates dead particles, so the effective rate is bounded by count/mean-life.
            float mean = (low + high) * 0.5f;
            float refill = (mean > 0f) ? count / mean : count;
            boolean controlFlow = ctrl != null && ctrl.isControlFlow();
            float rate = (controlFlow && geom.getReleaseRate() > 0) ? geom.getReleaseRate() : refill;
            // never emit faster than the pool can sustain (count particles per mean lifetime): a
            // higher rate just churns the fixed-size pool and reads as an over-bright blob.
            emitter.setParticlesPerSec(Math.min(rate, refill));
        }

        // ---- emission cone (direction * velocity, spread from min/max angle) ----
        com.jme.math.Vector3f dir = geom.getEmissionDirection();
        float vel = geom.getInitialVelocity();
        Vector3f initial = new Vector3f(dir.x, dir.y, dir.z);
        if (initial.lengthSquared() < 1e-8f) {
            initial.set(0, 1, 0);
        }
        // Bake the fork's per-second world-space velocity: forkVelocity * 1000 (the fork integrates
        // position in milliseconds) * the node scale (world-space emitters ignore the node transform
        // at runtime, so the scale must be pre-baked). See the motionScale comment above — without
        // the x1000 the burst collapsed to a clump at the origin (BUG-12).
        initial.normalizeLocal().multLocal(vel * motionScale);
        emitter.getParticleInfluencer().setInitialVelocity(initial);
        // map the fork emission cone [minAngle, maxAngle] (radians off the direction) to jME3's
        // single 0..1 velocity variation: full hemisphere (maxAngle >= PI/2) -> 1.0.
        float maxAngle = geom.getMaximumAngle();
        float variation = clamp01(maxAngle / com.jme.math.FastMath.HALF_PI);
        emitter.getParticleInfluencer().setVelocityVariation(variation);

        // ---- spin / alignment ----
        float spin = geom.getParticleSpinSpeed();
        if (spin != 0f) {
            emitter.setRotateSpeed(Math.abs(spin));
            emitter.setRandomAngle(true);
        }
        if (geom.isVelocityAligned()) {
            emitter.setFacingVelocity(true);
        }

        // ---- emitter shape ----
        emitter.setShape(shapeFor(geom, result));

        // ---- influences (gravity / wind) ----
        // gravity/wind drive the same ms-integrated position, so they take the motion scale, not the
        // size scale.
        applyInfluences(geom, emitter, result, motionScale);

        // ---- material (texture + blend) ----
        applyMaterial(geom, emitter, result);

        // The fork's transformParticles==false emits particles into world space (they don't follow
        // the emitter once spawned); transformParticles==true keeps them in emitter-local space.
        // jME3's setInWorldSpace(true) is the world-space (non-following) behaviour.
        emitter.setInWorldSpace(!geom.isTransformParticles());

        return emitter;
    }

    protected com.jme3.effect.shapes.EmitterShape shapeFor (ParticleGeometry geom, Result result)
    {
        switch (geom.getEmitType()) {
        case ParticleGeometry.ET_RECTANGLE: {
            com.jme.math.Rectangle r = geom.getRectangle();
            if (r != null) {
                com.jme.math.Vector3f a = r.getA(), b = r.getB(), c = r.getC();
                // Rectangle is defined by corner A and edge vectors to B and C; build a box that
                // spans those corners (zero-thickness on the unused axis).
                Vector3f min = new Vector3f(
                    Math.min(a.x, Math.min(b.x, c.x)),
                    Math.min(a.y, Math.min(b.y, c.y)),
                    Math.min(a.z, Math.min(b.z, c.z)));
                Vector3f max = new Vector3f(
                    Math.max(a.x, Math.max(b.x, c.x)),
                    Math.max(a.y, Math.max(b.y, c.y)),
                    Math.max(a.z, Math.max(b.z, c.z)));
                return new EmitterBoxShape(min, max);
            }
            break;
        }
        case ParticleGeometry.ET_RING: {
            com.jme.math.Ring ring = geom.getRing();
            if (ring != null) {
                result.notes.add(name(geom, "?") + ": ET_RING approximated by sphere shape");
                com.jme.math.Vector3f c = ring.getCenter();
                return new EmitterSphereShape(new Vector3f(c.x, c.y, c.z),
                    Math.max(0.01f, ring.getOuterRadius()));
            }
            break;
        }
        case ParticleGeometry.ET_LINE: {
            com.jme.math.Line line = geom.getLine();
            if (line != null) {
                // a thin box spanning the line's origin..(origin+direction)
                com.jme.math.Vector3f o = line.getOrigin(), d = line.getDirection();
                Vector3f min = new Vector3f(
                    Math.min(o.x, o.x + d.x), Math.min(o.y, o.y + d.y), Math.min(o.z, o.z + d.z));
                Vector3f max = new Vector3f(
                    Math.max(o.x, o.x + d.x), Math.max(o.y, o.y + d.y), Math.max(o.z, o.z + d.z));
                result.notes.add(name(geom, "?") + ": ET_LINE approximated by thin box shape");
                return new EmitterBoxShape(min, max);
            }
            break;
        }
        case ParticleGeometry.ET_POINT:
        default:
            break;
        }
        com.jme.math.Vector3f off = geom.getOriginOffset();
        // originOffset (and the rectangle/ring/line above) can be null on a fork emitter; fall back
        // to the origin rather than NPEing and failing the whole corpus bake.
        return (off == null) ? new EmitterPointShape(new Vector3f(0, 0, 0))
                             : new EmitterPointShape(new Vector3f(off.x, off.y, off.z));
    }

    protected void applyInfluences (
        ParticleGeometry geom, ParticleEmitter emitter, Result result, float motionScale)
    {
        List<ParticleInfluence> infs = geom.getInfluences();
        if (infs == null) {
            return;
        }
        Vector3f gravity = new Vector3f(0, 0, 0);
        boolean haveGravity = false;
        for (ParticleInfluence inf : infs) {
            if (inf instanceof SimpleParticleInfluenceFactory.BasicGravity g) {
                com.jme.math.Vector3f f = g.getGravityForce();
                // jME3 gravity is an acceleration vector SUBTRACTED from velocity each step; the
                // fork gravity force is ADDED. Negate to match (a fork (0,-9.8,0) downward force ->
                // jME3 gravity (0,9.8,0), which jME3 subtracts -> downward).
                gravity.addLocal(-f.x, -f.y, -f.z);
                haveGravity = true;
            } else if (inf instanceof SimpleParticleInfluenceFactory.BasicWind w) {
                // jME3 has no wind force; fold the wind into the gravity vector as a constant
                // lateral acceleration (best-effort, same sign convention as gravity above).
                com.jme.math.Vector3f wd = w.getWindDirection();
                float s = w.getStrength();
                gravity.addLocal(-wd.x * s, -wd.y * s, -wd.z * s);
                haveGravity = true;
                result.notes.add(name(geom, "?") + ": BasicWind folded into gravity (no jME3 wind)");
            } else {
                result.droppedInfluences.add(inf.getClass().getName());
            }
        }
        if (haveGravity) {
            // scale gravity by the same per-second-world motion factor as velocity (1000 * nodeScale
            // for world-space emitters): the fork accumulates gravity into velocity and then drives
            // position through its millisecond integration, so gravity inherits the same x1000.
            emitter.setGravity(gravity.multLocal(motionScale));
        } else {
            // the fork applies no gravity unless a BasicGravity influence is present; jME3's default
            // gravity is (0,0.1,0), so zero it to match a no-gravity fork effect.
            emitter.setGravity(0, 0, 0);
        }
    }

    protected void applyMaterial (ParticleGeometry geom, ParticleEmitter emitter, Result result)
    {
        Material mat = new Material(_assets, "Common/MatDefs/Misc/Particle.j3md");

        // texture: the fork TextureState's first texture's image location (relative to the effect
        // dir; the fork set a TextureKey location override to that dir, so it's a bare filename).
        String texPath = textureLocation(geom);
        if (texPath != null) {
            String assetPath = resolveTexturePath(texPath);
            try {
                Texture tex = loadParticleTexture(assetPath, result, geom);
                mat.setTexture("Texture", tex);
            } catch (Exception e) {
                result.texturesResolved = false;
                result.notes.add(name(geom, "?") + ": texture not found: " + assetPath);
            }
        } else {
            result.texturesResolved = false;
            result.notes.add(name(geom, "?") + ": no TextureState/texture on emitter");
        }

        // blend mode from the fork AlphaState (additive dst==ONE -> Additive, else Alpha).
        BlendMode blend = BlendMode.Alpha;
        AlphaState astate = (AlphaState)geom.getRenderState(RenderState.RS_ALPHA);
        if (astate != null && astate.isBlendEnabled() &&
                astate.getDstFunction() == AlphaState.DB_ONE) {
            blend = BlendMode.Additive;
        }
        mat.getAdditionalRenderState().setBlendMode(blend);

        // single texture, no sprite sub-image animation in the fork particle system.
        emitter.setImagesX(1);
        emitter.setImagesY(1);
        emitter.setMaterial(mat);
    }

    /**
     * Loads a particle sprite texture, expanding a <em>palette PNG whose transparency lives only in
     * a {@code tRNS} chunk</em> (PNG colortype&nbsp;3 + {@code tRNS}; e.g.
     * {@code effects/frontier_town/lasso/tiny_star.png}, the star sprite the {@code
     * frontier_town/get_bonus} burst uses) into an explicit, <b>keyless</b> RGBA8 texture baked into
     * the {@code .j3o} (BUG-12 / audit M1).
     *
     * <p><b>Why:</b> these sprites are drawn with {@code Blend Additive} ({@code glBlendFunc(ONE,
     * ONE)}), which adds the source RGB to the framebuffer <em>ignoring</em> the fragment alpha — so
     * a background pixel only stays invisible if its RGB is black. A palette PNG's transparent
     * background carries a palette <em>color</em> (often white/gold) under its {@code tRNS} alpha,
     * and whether that survives as alpha=0 depends entirely on how the loading {@code AssetManager}'s
     * PNG decoder treats colortype-3 + {@code tRNS}: jME3's stock {@code AWTLoader} routes an indexed
     * {@code BufferedImage} through a {@code getTransparency()}-keyed branch that drops to opaque
     * RGB8 (no alpha) whenever the platform {@code ImageIO} reports the indexed image OPAQUE — and
     * the additive shader then ADDS the palette background color, painting the opaque white/gold
     * block the audit caught under the unit. (Effects with a BLACK palette background, e.g.
     * {@code explosion.png}, add nothing and looked fine, which is why the regression slipped
     * through.)
     *
     * <p><b>Fix:</b> when the source is palette+{@code tRNS}, decode it ourselves via {@code ImageIO}
     * into a {@code TYPE_INT_ARGB} image (which faithfully expands the {@code tRNS} palette alpha to
     * a real alpha channel, with the transparent background forced to RGB(0,0,0)/alpha=0 so it adds
     * nothing under additive), hand that ARGB image to {@code AWTLoader} (its non-opaque branch
     * deterministically produces RGBA8), and wrap it in a {@code Texture2D} with <b>no
     * {@code TextureKey}</b>. {@code Texture.read} re-loads from the {@code AssetManager} when a key
     * is present and only falls back to the embedded image when the key is null — so a keyless
     * texture bakes the normalized RGBA pixels straight into the {@code .j3o}, making the result
     * independent of the runtime's PNG decoder. Keeping {@code Additive} preserves the faithful
     * glowing-star look (bright stars glow; transparent background contributes nothing). All other
     * textures keep the cheap key-based path (the runtime loads them straight from the shared PNG).
     */
    protected Texture loadParticleTexture (String assetPath, Result result, ParticleGeometry geom)
        throws java.io.IOException
    {
        // Read the raw bytes once (small sprite PNGs) so we can both sniff the PNG header and decode
        // it without depending on the locator type (file vs classpath).
        byte[] png = readAsset(assetPath);
        if (png != null && isPaletteWithTrns(png)) {
            java.awt.image.BufferedImage src =
                javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
            if (src != null) {
                int w = src.getWidth(), h = src.getHeight();
                java.awt.image.BufferedImage argb =
                    new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                // copy with the palette's tRNS alpha expanded; force fully-transparent texels to
                // pure transparent black so additive blend adds nothing regardless of the palette
                // entry's (often white) RGB.
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int p = src.getRGB(x, y); // ARGB; ImageIO honours the tRNS palette alpha
                        argb.setRGB(x, y, ((p >>> 24) == 0) ? 0 : p);
                    }
                }
                com.jme3.texture.Image img =
                    new com.jme3.texture.plugins.AWTLoader().load(argb, false);
                Texture2D tex = new Texture2D(img);
                // keyless: Texture.read uses the embedded RGBA image (a non-null key would make the
                // runtime re-load the raw palette PNG and reintroduce the decoder-dependent bug).
                tex.setKey(null);
                result.notes.add(name(geom, "?") + ": expanded palette+tRNS texture " + assetPath +
                    " to baked RGBA8 (additive-safe)");
                return tex;
            }
        }
        // non-palette (or palette without tRNS): keep the cheap key-based load; the runtime resolves
        // the shared PNG itself.
        return _assets.loadTexture(new TextureKey(assetPath, false));
    }

    /** Reads an AssetManager-relative asset's raw bytes (locator-agnostic), or null if absent. */
    protected byte[] readAsset (String assetPath)
    {
        com.jme3.asset.AssetInfo info = _assets.locateAsset(new com.jme3.asset.AssetKey<>(assetPath));
        if (info == null) {
            return null;
        }
        try (java.io.InputStream in = info.openStream()) {
            return in.readAllBytes();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /**
     * True if {@code png} is a PNG with colortype&nbsp;3 (palette) AND carries a {@code tRNS} chunk
     * (palette-index transparency). Reads the IHDR colortype byte and scans the chunk stream for
     * {@code tRNS} — no image decode needed.
     */
    protected static boolean isPaletteWithTrns (byte[] png)
    {
        // 8-byte signature + IHDR length(4)+type(4); IHDR data starts at 16, colortype is data[9].
        if (png.length < 26) {
            return false;
        }
        if ((png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
            return false;
        }
        boolean palette = (png[25] & 0xFF) == 3; // IHDR colortype byte (offset 8+8+9 == 25)
        if (!palette) {
            return false;
        }
        // walk chunks looking for tRNS before the pixel data (tRNS, if present, precedes IDAT).
        int i = 8;
        while (i + 8 <= png.length) {
            int len = ((png[i] & 0xFF) << 24) | ((png[i + 1] & 0xFF) << 16) |
                      ((png[i + 2] & 0xFF) << 8) | (png[i + 3] & 0xFF);
            String type = new String(png, i + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if ("tRNS".equals(type)) {
                return true;
            }
            if ("IDAT".equals(type) || "IEND".equals(type)) {
                return false;
            }
            i += 12 + len; // length(4) + type(4) + data(len) + CRC(4)
        }
        return false;
    }

    protected String textureLocation (ParticleGeometry geom)
    {
        TextureState ts = (TextureState)geom.getRenderState(RenderState.RS_TEXTURE);
        if (ts == null) {
            return null;
        }
        com.jme.image.Texture tex = ts.getTexture();
        if (tex == null) {
            return null;
        }
        String loc = tex.getImageLocation();
        if ((loc == null || loc.isEmpty()) && tex.getTextureKey() != null &&
                tex.getTextureKey().getLocation() != null) {
            loc = tex.getTextureKey().getLocation().toString();
        }
        return (loc == null || loc.isEmpty()) ? null : loc;
    }

    /**
     * Turns a fork texture image location into an AssetManager-relative path. The fork stored
     * texture references relative to the effect dir, and the build-time {@code TextureKey} location
     * override (mirroring the fork {@code ParticleCache}) resolves them to an absolute
     * {@code file:.../rsrc/<asset path>} URL — and crucially the effects routinely reference
     * <em>shared</em> textures in OTHER effect dirs (e.g. {@code boom_town/barrel_explosion} reuses
     * {@code frontier_town/explosion/explosion.png}). So the correct asset path is everything after
     * the {@code rsrc/} root in the resolved location, not the bare filename under the effect dir.
     *
     * <p>Falls back to {@code effects/<key>/<filename>} for a bare relative location with no
     * {@code rsrc/} marker.
     */
    protected String resolveTexturePath (String loc)
    {
        String s = loc.replace('\\', '/');
        int marker = s.indexOf("/rsrc/");
        if (marker >= 0) {
            return s.substring(marker + "/rsrc/".length());
        }
        if (s.startsWith("rsrc/")) {
            return s.substring("rsrc/".length());
        }
        // bare relative filename: resolve under the effect dir
        int slash = s.lastIndexOf('/');
        String file = slash >= 0 ? s.substring(slash + 1) : s;
        return "effects/" + _typePath + "/" + file;
    }

    protected static void copyTransform (com.jme.scene.Spatial from, Spatial to)
    {
        com.jme.math.Vector3f t = from.getLocalTranslation();
        com.jme.math.Quaternion r = from.getLocalRotation();
        com.jme.math.Vector3f s = from.getLocalScale();
        to.setLocalTranslation(t.x, t.y, t.z);
        to.setLocalRotation(new Quaternion(r.x, r.y, r.z, r.w));
        to.setLocalScale(s.x, s.y, s.z);
    }

    protected static ColorRGBA color (com.jme.renderer.ColorRGBA c)
    {
        return c == null ? new ColorRGBA(1, 1, 1, 1) : new ColorRGBA(c.r, c.g, c.b, c.a);
    }

    protected static String name (com.jme.scene.Spatial s, String dflt)
    {
        String n = s.getName();
        return (n == null || n.isEmpty()) ? dflt : n;
    }

    protected static float clamp01 (float v)
    {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    protected final AssetManager _assets;
    protected final String _typePath;

    /** The effect node's particles.properties scale, baked into per-particle size/velocity/gravity
     * because jME3 ignores the node transform for world-space (non-following) emitters (BUG-7). */
    protected final float _effectScale;

    /** jME3 rejects a zero/near-zero low life. */
    protected static final float MIN_LIFE_SEC = 0.01f;

    /** The fork integrates particle position in milliseconds ({@code position += velocity *
     * secondsPassed * 1000}); jME3 integrates in seconds. So baked world-space velocity/gravity carry
     * this extra factor to reproduce the fork's per-second travel. */
    protected static final float MS_PER_SEC = 1000f;
}
