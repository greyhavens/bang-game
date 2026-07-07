//
// $Id$

package com.threerings.bang.tools.particles;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.jme.scene.state.AlphaState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureKey;
import com.jme.util.export.binary.BinaryImporter;
import com.jmex.effects.particles.ParticleController;
import com.jmex.effects.particles.ParticleGeometry;
import com.jmex.effects.particles.ParticleInfluence;
import com.jmex.effects.particles.SimpleParticleInfluenceFactory;
import com.jmex.effects.particles.SwarmInfluence;
import com.jmex.effects.particles.WanderInfluence;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.influencers.DefaultParticleInfluencer;
import com.jme3.effect.shapes.EmitterBoxShape;
import com.jme3.effect.shapes.EmitterPointShape;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.texture.Texture;

import com.threerings.bang.jme3.model.ParticleConverter;
import com.threerings.bang.tools.j3o.ParticlesToJ3o;
import com.threerings.bang.util.ParticleInfluenceControl;

/**
 * Epic&nbsp;4 Phase&nbsp;4&frac12;a <b>enrichment extractor</b> — the one-time, fork-classpath dump
 * that must run <em>before</em> the Phase-5 fork deletion (risk R1 of the particles re-authoring
 * plan). For every committed fork-binary {@code assets/rsrc/effects/<key>/particles.jme} it writes
 * a sibling committed {@code particles-src.json} holding <b>everything the future effect-by-effect
 * re-authoring needs</b>, merging two views of the effect:
 *
 * <ul>
 *   <li><b>The converted jME3 parameter set</b> — the exact emitter tree
 *       {@link ParticleConverter} bakes into the frozen {@code particles.j3o} (same importer, same
 *       converter, same values; dumped from the in-memory conversion rather than transcribed), in
 *       the schema of the Phase-4&frac12; groundwork prototype ({@code DumpParticleJ3o}).</li>
 *   <li><b>{@code forkExtras}</b> — per-emitter fork-source data the baked {@code .j3o} can NOT
 *       provide: the raw pre-conversion parameters (pre-scale sizes, per-ms velocities, ms
 *       lifetimes, annular emission cone incl. the converter-ignored {@code minimumAngle}, signed
 *       spin, {@code releaseVariance}, {@code alphaFalloff}, raw emit-shape geometry incl. the
 *       ring's converter-dropped inner radius/up axis), the FULL parameter set of every fork
 *       influence including the dropped {@code BasicVortex}/{@code SwarmInfluence} (E15-17 bounded
 *       remainder), the raw {@code AlphaState} blend functions, and the <b>texture source path</b>
 *       (raw stored location + resolved rsrc asset path + existence) — critical for the 22 effects
 *       whose palette+tRNS sprites are baked into the {@code .j3o} as <em>keyless</em> embedded
 *       RGBA textures with no recoverable provenance.</li>
 * </ul>
 *
 * <p>Reads the COMMITTED {@code assets/rsrc} tree (identical bytes to what {@code copyTownBits}
 * stages) through the same fork {@code BinaryImporter} + {@code TextureKey} location-override path
 * as {@link ParticlesToJ3o#bakeOne}; extends {@link ParticlesToJ3o} to reuse those helpers
 * verbatim. JSON uses the repo's lossless-float house style ({@code Float.toString} — shortest
 * decimal that round-trips bit-exact; see {@code MxmlToGltf}).
 *
 * <p>Usage: {@code ExtractParticleSources <rsrc root> [effect key]}.
 */
public class ExtractParticleSources extends ParticlesToJ3o
{
    public static void main (String[] args)
        throws Exception
    {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: ExtractParticleSources <rsrc root> [effect key]");
            System.exit(1);
        }
        File rsrcRoot = new File(args[0]).getAbsoluteFile();
        if (!rsrcRoot.isDirectory()) {
            System.err.println("rsrc root must exist: " + rsrcRoot);
            System.exit(1);
        }
        ensureDisplaySystem();
        DesktopAssetManager am = createAssetManager(rsrcRoot);

        List<Path> jmes = new ArrayList<>();
        if (args.length == 2) {
            jmes.add(rsrcRoot.toPath().resolve("effects").resolve(args[1]).resolve("particles.jme"));
        } else {
            Path effects = rsrcRoot.toPath().resolve("effects");
            if (Files.isDirectory(effects)) {
                try (Stream<Path> walk = Files.walk(effects)) {
                    walk.filter(p -> p.getFileName().toString().equals("particles.jme"))
                        .sorted()
                        .forEach(jmes::add);
                }
            }
        }

        int ok = 0, fail = 0, emitters = 0, embedded = 0, missingTex = 0;
        for (Path jme : jmes) {
            File jmeFile = jme.toFile();
            String key = effectKey(rsrcRoot, jmeFile);
            try {
                Stats stats = extractOne(am, rsrcRoot, jmeFile, key);
                ok++;
                emitters += stats.emitters;
                embedded += stats.embeddedTextures;
                missingTex += stats.missingTextures;
                System.out.println("Extracted " + key + ": " + stats.emitters + " emitter(s)" +
                    (stats.embeddedTextures > 0 ?
                        ", " + stats.embeddedTextures + " embedded-texture source path(s)" : ""));
            } catch (Throwable t) {
                fail++;
                System.err.println("FAILED to extract " + jmeFile + ": " + t);
                t.printStackTrace();
            }
        }
        System.out.println("Extracted " + ok + " particles-src.json (" + fail + " failed) under " +
            rsrcRoot + "/effects: " + emitters + " emitters, " + embedded +
            " embedded-texture source paths recorded, " + missingTex + " texture file(s) missing");
        System.exit(fail == 0 ? 0 : 2);
    }

    protected static class Stats
    {
        public int emitters;
        public int embeddedTextures;
        public int missingTextures;
    }

    /**
     * {@link ParticleConverter} with its protected texture-resolution helpers exposed, so the
     * enrichment records the SAME raw-location &rarr; asset-path &rarr; palette+tRNS-embed
     * decisions the bake makes (this instance also performs the actual conversion we dump).
     */
    protected static class ProbeConverter extends ParticleConverter
    {
        public ProbeConverter (AssetManager am, String key, float scale) {
            super(am, key, scale);
        }
        public String rawLocation (ParticleGeometry geom) {
            return textureLocation(geom);
        }
        public String assetPath (String rawLocation) {
            return resolveTexturePath(rawLocation);
        }
        public byte[] assetBytes (String assetPath) {
            return readAsset(assetPath);
        }
        public static boolean paletteTrns (byte[] png) {
            return isPaletteWithTrns(png);
        }
    }

    /** Imports one fork particles.jme, converts it in memory, and writes particles-src.json. */
    protected static Stats extractOne (DesktopAssetManager am, File rsrcRoot, File jmeFile,
                                       String key)
        throws Exception
    {
        final File parent = jmeFile.getParentFile();
        // identical import path to ParticlesToJ3o.bakeOne: fork BinaryImporter with the fork
        // ParticleCache's TextureKey location override (bare filenames resolve to the effect dir).
        TextureKey.setLocationOverride(new TextureKey.LocationOverride() {
            public URL getLocation (String file) throws MalformedURLException {
                return new URL(parent.toURI().toURL(), file);
            }
        });
        com.jme.scene.Spatial forkRoot;
        try (InputStream in = new FileInputStream(jmeFile)) {
            forkRoot = (com.jme.scene.Spatial)BinaryImporter.getInstance().load(in);
        } finally {
            TextureKey.setLocationOverride(null);
        }

        float scale = readEffectScale(parent);
        ProbeConverter converter = new ProbeConverter(am, key, scale);
        ParticleConverter.Result result = converter.convert(forkRoot);
        if (result.root == null) {
            throw new IllegalStateException("no convertible particle geometry in " + jmeFile);
        }

        Stats stats = new Stats();
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("format", 1);
        doc.put("generator",
            "ExtractParticleSources (Epic 4 Phase 4½a fork-classpath enrichment)");
        doc.put("effect", key);
        doc.put("effectScale", scale);
        doc.put("root", dumpPair(forkRoot, result.root, converter, rsrcRoot, stats));

        StringBuilder sb = new StringBuilder();
        writeJson(doc, sb, 0);
        sb.append('\n');
        Files.write(new File(parent, "particles-src.json").toPath(),
            sb.toString().getBytes(StandardCharsets.UTF_8));
        return stats;
    }

    /**
     * Walks the fork spatial and its converted jME3 counterpart in lockstep (the converter maps
     * ParticleGeometry &rarr; ParticleEmitter and Node &rarr; Node 1:1 in child order, skipping
     * only non-particle leaf spatials), emitting the converted parameter set plus per-emitter
     * {@code forkExtras}.
     */
    protected static Map<String, Object> dumpPair (com.jme.scene.Spatial fork, Spatial conv,
                                                   ProbeConverter converter, File rsrcRoot,
                                                   Stats stats)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", conv.getName());
        out.put("translation", vec(conv.getLocalTranslation()));
        out.put("rotation", quat(conv.getLocalRotation()));
        out.put("scale", vec(conv.getLocalScale()));
        if (fork instanceof ParticleGeometry geom && conv instanceof ParticleEmitter em) {
            out.put("type", "emitter");
            dumpEmitter(geom, em, out, converter, rsrcRoot, stats);
            stats.emitters++;
        } else if (fork instanceof com.jme.scene.Node fnode && conv instanceof Node cnode) {
            out.put("type", "node");
            List<Object> children = new ArrayList<>();
            int cursor = 0;
            for (int ii = 0, nn = fnode.getQuantity(); ii < nn; ii++) {
                com.jme.scene.Spatial fchild = fnode.getChild(ii);
                // the converter produces a child for ParticleGeometry and Node fork children only
                if (fchild instanceof ParticleGeometry || fchild instanceof com.jme.scene.Node) {
                    children.add(dumpPair(
                        fchild, cnode.getChild(cursor++), converter, rsrcRoot, stats));
                }
            }
            if (cursor != cnode.getQuantity()) {
                throw new IllegalStateException("fork/converted child mismatch under " +
                    fork.getName() + ": paired " + cursor + " of " + cnode.getQuantity());
            }
            out.put("children", children);
        } else {
            throw new IllegalStateException("fork/converted type mismatch: " +
                fork.getClass().getName() + " vs " + conv.getClass().getName());
        }
        return out;
    }

    /** Dumps the converted jME3 emitter parameters (the frozen .j3o content) + forkExtras. */
    protected static void dumpEmitter (ParticleGeometry geom, ParticleEmitter em,
                                       Map<String, Object> out, ProbeConverter converter,
                                       File rsrcRoot, Stats stats)
    {
        out.put("meshType", em.getMeshType().toString());
        out.put("numParticles", em.getMaxNumParticles());
        out.put("particlesPerSec", em.getParticlesPerSec());
        out.put("lowLife", em.getLowLife());
        out.put("highLife", em.getHighLife());
        out.put("startSize", em.getStartSize());
        out.put("endSize", em.getEndSize());
        out.put("startColor", col(em.getStartColor()));
        out.put("endColor", col(em.getEndColor()));
        out.put("gravity", vec(em.getGravity()));
        out.put("rotateSpeed", em.getRotateSpeed());
        out.put("randomAngle", (Boolean)reflect(em, "randomAngle"));
        out.put("facingVelocity", em.isFacingVelocity());
        out.put("inWorldSpace", em.isInWorldSpace());
        out.put("imagesX", em.getImagesX());
        out.put("imagesY", em.getImagesY());
        if (em.getParticleInfluencer() instanceof DefaultParticleInfluencer inf) {
            out.put("initialVelocity", vec(inf.getInitialVelocity()));
            out.put("velocityVariation", inf.getVelocityVariation());
        }
        out.put("shape", shape(em.getShape()));
        out.put("material", material(em));
        List<Object> controls = new ArrayList<>();
        for (int ii = 0; ii < em.getNumControls(); ii++) {
            controls.add(control(em.getControl(ii)));
        }
        out.put("controls", controls);
        out.put("forkExtras", forkExtras(geom, converter, rsrcRoot, stats));
    }

    /**
     * The per-emitter fork-source block: everything the baked {@code .j3o} cannot provide —
     * raw pre-conversion parameters, converter-dropped/ignored fields, full influence parameter
     * sets (incl. BasicVortex/SwarmInfluence), raw blend state, and the texture source path.
     */
    protected static Map<String, Object> forkExtras (ParticleGeometry geom,
                                                     ProbeConverter converter, File rsrcRoot,
                                                     Stats stats)
    {
        Map<String, Object> fx = new LinkedHashMap<>();
        fx.put("emitType", emitTypeName(geom.getEmitType()));
        fx.put("particleType", particleTypeName(geom.getParticleType()));
        fx.put("numParticles", geom.getNumParticles());

        ParticleController ctrl = geom.getParticleController();
        Map<String, Object> controller = null;
        if (ctrl != null) {
            controller = new LinkedHashMap<>();
            controller.put("repeatType", repeatTypeName(ctrl.getRepeatType()));
            controller.put("controlFlow", ctrl.isControlFlow());
            controller.put("releaseVariance", ctrl.getReleaseVariance());
            // converter-ignored simulation-clock multiplier (currentTime += dt * speed): a
            // non-default speed ran the whole fork effect faster/slower than the converted one
            controller.put("speed", ctrl.getSpeed());
            // emission window: update only runs while minTime <= currentTime <= maxTime
            controller.put("minTime", ctrl.getMinTime());
            controller.put("maxTime", ctrl.getMaxTime());
            controller.put("precision", ctrl.getPrecision());
            controller.put("iterations", ctrl.getIterations());
        }
        fx.put("controller", controller);
        fx.put("releaseRate", geom.getReleaseRate());

        // lifetimes/sizes/velocity in RAW fork units (ms, pre-effectScale, per-ms velocity scalar)
        fx.put("minimumLifeTime", geom.getMinimumLifeTime());
        fx.put("maximumLifeTime", geom.getMaximumLifeTime());
        fx.put("startSize", geom.getStartSize());
        fx.put("endSize", geom.getEndSize());
        fx.put("emissionDirection", vec(geom.getEmissionDirection()));
        fx.put("initialVelocity", geom.getInitialVelocity());
        // annular emission cone: the converter maps only maximumAngle (to velocityVariation);
        // minimumAngle (min..max annulus) is converter-IGNORED and unrecoverable from the .j3o
        fx.put("minimumAngle", geom.getMinimumAngle());
        fx.put("maximumAngle", geom.getMaximumAngle());
        // SIGNED spin (the converter bakes only the magnitude, with a random start angle)
        fx.put("particleSpinSpeed", geom.getParticleSpinSpeed());
        fx.put("velocityAligned", geom.isVelocityAligned());
        fx.put("transformParticles", geom.isTransformParticles());
        fx.put("rotateWithScene", geom.isRotateWithScene());
        // alphaFalloff: converter-ignored fork alpha-ramp tweak (non-default in 2 effects)
        fx.put("alphaFalloff", geom.getAlphaFalloff());
        fx.put("particleOrientation", geom.getParticleOrientation());
        fx.put("cameraFacing", geom.isCameraFacing());

        // raw emit-shape geometry (the converter's box/sphere mappings lose the ring's inner
        // radius + up axis and the line's direction form)
        fx.put("originOffset", vec(geom.getOriginOffset()));
        switch (geom.getEmitType()) {
        case ParticleGeometry.ET_RING: {
            com.jme.math.Ring ring = geom.getRing();
            if (ring != null) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("center", vec(ring.getCenter()));
                r.put("up", vec(ring.getUp()));
                r.put("innerRadius", ring.getInnerRadius());
                r.put("outerRadius", ring.getOuterRadius());
                fx.put("ring", r);
            }
            break;
        }
        case ParticleGeometry.ET_LINE: {
            com.jme.math.Line line = geom.getLine();
            if (line != null) {
                Map<String, Object> l = new LinkedHashMap<>();
                l.put("origin", vec(line.getOrigin()));
                l.put("direction", vec(line.getDirection()));
                fx.put("line", l);
            }
            break;
        }
        case ParticleGeometry.ET_RECTANGLE: {
            com.jme.math.Rectangle rect = geom.getRectangle();
            if (rect != null) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("a", vec(rect.getA()));
                r.put("b", vec(rect.getB()));
                r.put("c", vec(rect.getC()));
                fx.put("rectangle", r);
            }
            break;
        }
        default:
            break;
        }

        // FULL fork influence parameter sets (incl. the converter-dropped BasicVortex/Swarm)
        List<Object> influences = new ArrayList<>();
        List<ParticleInfluence> infs = geom.getInfluences();
        if (infs != null) {
            for (ParticleInfluence inf : infs) {
                influences.add(influence(inf));
            }
        }
        fx.put("influences", influences);

        // raw fork AlphaState (the converter collapses this to Additive-vs-Alpha)
        AlphaState astate = (AlphaState)geom.getRenderState(RenderState.RS_ALPHA);
        Map<String, Object> alpha = null;
        if (astate != null) {
            alpha = new LinkedHashMap<>();
            alpha.put("blendEnabled", astate.isBlendEnabled());
            alpha.put("srcFunction", srcBlendName(astate.getSrcFunction()));
            alpha.put("dstFunction", dstBlendName(astate.getDstFunction()));
        }
        fx.put("alphaState", alpha);

        // texture SOURCE path: raw stored location (rsrc-relativized when possible, so committed
        // JSON stays machine-independent) + the converter-resolved asset path + existence +
        // whether the bake embeds it keyless (palette+tRNS RGBA expansion) — the R1 hard-deadline
        // data for the 22 embedded-texture effects.
        Map<String, Object> texture = null;
        String raw = converter.rawLocation(geom);
        if (raw != null) {
            texture = new LinkedHashMap<>();
            texture.put("location", relativizeLocation(raw));
            String assetPath = converter.assetPath(raw);
            texture.put("assetPath", assetPath);
            boolean exists = new File(rsrcRoot, assetPath).isFile();
            texture.put("exists", exists);
            byte[] png = exists ? converter.assetBytes(assetPath) : null;
            boolean embedded = png != null && ProbeConverter.paletteTrns(png);
            texture.put("embeddedInJ3o", embedded);
            if (embedded) {
                stats.embeddedTextures++;
            }
            if (!exists) {
                stats.missingTextures++;
            }
        }
        fx.put("texture", texture);
        return fx;
    }

    protected static Map<String, Object> influence (ParticleInfluence inf)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("class", inf.getClass().getSimpleName());
        out.put("enabled", inf.isEnabled());
        if (inf instanceof SimpleParticleInfluenceFactory.BasicGravity g) {
            out.put("gravityForce", vec(g.getGravityForce()));
            out.put("rotateWithScene", g.isRotateWithScene());
        } else if (inf instanceof SimpleParticleInfluenceFactory.BasicWind w) {
            out.put("windDirection", vec(w.getWindDirection()));
            out.put("strength", w.getStrength());
            out.put("random", w.isRandom());
            out.put("rotateWithScene", w.isRotateWithScene());
        } else if (inf instanceof SimpleParticleInfluenceFactory.BasicDrag d) {
            out.put("dragCoefficient", d.getDragCoefficient());
        } else if (inf instanceof SimpleParticleInfluenceFactory.BasicVortex v) {
            out.put("type", v.getType() == SimpleParticleInfluenceFactory.BasicVortex.VT_TORUS ?
                "torus" : "cylinder");
            out.put("strength", v.getStrength());
            out.put("divergence", v.getDivergence());
            com.jme.math.Line axis = v.getAxis();
            Map<String, Object> a = null;
            if (axis != null) {
                a = new LinkedHashMap<>();
                a.put("origin", vec(axis.getOrigin()));
                a.put("direction", vec(axis.getDirection()));
            }
            out.put("axis", a);
            out.put("height", v.getHeight());
            out.put("radius", v.getRadius());
            out.put("random", v.isRandom());
            out.put("transformWithScene", v.isTransformWithScene());
        } else if (inf instanceof WanderInfluence w) {
            out.put("wanderRadius", w.getWanderRadius());
            out.put("wanderDistance", w.getWanderDistance());
            out.put("wanderJitter", w.getWanderJitter());
        } else if (inf instanceof SwarmInfluence s) {
            out.put("swarmOffset", vec(s.getSwarmOffset()));
            out.put("swarmRange", s.getSwarmRange());
            out.put("deviance", s.getDeviance());
            out.put("turnSpeed", s.getTurnSpeed());
            out.put("speedBump", s.getSpeedBump());
            out.put("maxSpeed", s.getMaxSpeed());
        }
        return out;
    }

    // ---- converted-side helpers (mirror the groundwork prototype's schema) ----

    protected static Map<String, Object> shape (EmitterShape sh)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        if (sh instanceof EmitterPointShape p) {
            out.put("kind", "point");
            out.put("point", vec(p.getPoint()));
        } else if (sh instanceof EmitterBoxShape b) {
            out.put("kind", "box");
            out.put("min", vec((Vector3f)reflect(b, "min")));
            out.put("len", vec((Vector3f)reflect(b, "len")));
        } else if (sh instanceof EmitterSphereShape s) {
            out.put("kind", "sphere");
            out.put("center", vec((Vector3f)reflect(s, "center")));
            out.put("radius", (Float)reflect(s, "radius"));
        } else {
            out.put("kind", sh.getClass().getSimpleName());
        }
        return out;
    }

    protected static Map<String, Object> material (ParticleEmitter em)
    {
        com.jme3.material.Material m = em.getMaterial();
        if (m == null) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("def", m.getMaterialDef().getAssetName());
        out.put("blend", m.getAdditionalRenderState().getBlendMode().toString());
        var tp = m.getTextureParam("Texture");
        if (tp != null) {
            Texture t = tp.getTextureValue();
            Object k = (t != null) ? t.getKey() : null;
            // a keyless texture is the embedded palette+tRNS RGBA bake; its SOURCE path is in
            // forkExtras.texture (the whole point of this enrichment)
            out.put("texture", (k == null) ? "<embedded RGBA, keyless>" : k.toString());
        }
        return out;
    }

    protected static Map<String, Object> control (Control c)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("class", c.getClass().getSimpleName());
        if (c instanceof ParticleInfluenceControl pic) {
            ParticleInfluenceControl.Type type = pic.getInfluenceType();
            out.put("type", type.toString());
            switch (type) {
            case DRAG:
                out.put("dragCoefficient", (Float)reflect(pic, "_dragCoefficient"));
                break;
            case WANDER:
                out.put("wanderRadius", (Float)reflect(pic, "_wanderRadius"));
                out.put("wanderDistance", (Float)reflect(pic, "_wanderDistance"));
                out.put("wanderJitter", (Float)reflect(pic, "_wanderJitter"));
                out.put("emissionDirection", vec((Vector3f)reflect(pic, "_emissionDirection")));
                break;
            default:
                break;
            }
        }
        return out;
    }

    // ---- fork enum decoding (the constants die with the fork in Phase 5) ----

    protected static String emitTypeName (int et)
    {
        switch (et) {
        case ParticleGeometry.ET_POINT: return "point";
        case ParticleGeometry.ET_LINE: return "line";
        case ParticleGeometry.ET_RECTANGLE: return "rectangle";
        case ParticleGeometry.ET_RING: return "ring";
        case ParticleGeometry.ET_FRUSTUM: return "frustum";
        default: return "unknown(" + et + ")";
        }
    }

    protected static String particleTypeName (int pt)
    {
        switch (pt) {
        case ParticleGeometry.PT_QUAD: return "quad";
        case ParticleGeometry.PT_TRIANGLE: return "triangle";
        case ParticleGeometry.PT_POINT: return "point";
        case ParticleGeometry.PT_LINE: return "line";
        default: return "unknown(" + pt + ")";
        }
    }

    protected static String repeatTypeName (int rt)
    {
        switch (rt) {
        case ParticleController.RT_CLAMP: return "clamp";
        case ParticleController.RT_WRAP: return "wrap";
        case ParticleController.RT_CYCLE: return "cycle";
        default: return "unknown(" + rt + ")";
        }
    }

    protected static String srcBlendName (int sb)
    {
        switch (sb) {
        case AlphaState.SB_ZERO: return "ZERO";
        case AlphaState.SB_ONE: return "ONE";
        case AlphaState.SB_DST_COLOR: return "DST_COLOR";
        case AlphaState.SB_ONE_MINUS_DST_COLOR: return "ONE_MINUS_DST_COLOR";
        case AlphaState.SB_SRC_ALPHA: return "SRC_ALPHA";
        case AlphaState.SB_ONE_MINUS_SRC_ALPHA: return "ONE_MINUS_SRC_ALPHA";
        case AlphaState.SB_DST_ALPHA: return "DST_ALPHA";
        case AlphaState.SB_ONE_MINUS_DST_ALPHA: return "ONE_MINUS_DST_ALPHA";
        case AlphaState.SB_SRC_ALPHA_SATURATE: return "SRC_ALPHA_SATURATE";
        default: return "unknown(" + sb + ")";
        }
    }

    protected static String dstBlendName (int db)
    {
        switch (db) {
        case AlphaState.DB_ZERO: return "ZERO";
        case AlphaState.DB_ONE: return "ONE";
        case AlphaState.DB_SRC_COLOR: return "SRC_COLOR";
        case AlphaState.DB_ONE_MINUS_SRC_COLOR: return "ONE_MINUS_SRC_COLOR";
        case AlphaState.DB_SRC_ALPHA: return "SRC_ALPHA";
        case AlphaState.DB_ONE_MINUS_SRC_ALPHA: return "ONE_MINUS_SRC_ALPHA";
        case AlphaState.DB_DST_ALPHA: return "DST_ALPHA";
        case AlphaState.DB_ONE_MINUS_DST_ALPHA: return "ONE_MINUS_DST_ALPHA";
        default: return "unknown(" + db + ")";
        }
    }

    /**
     * Keeps committed JSON machine-independent: a fork-stored texture location that resolved to an
     * absolute {@code file:...} URL is recorded from its {@code rsrc/} marker on; anything else
     * (the common bare relative filename) is recorded as-is.
     */
    protected static String relativizeLocation (String loc)
    {
        String s = loc.replace('\\', '/');
        int marker = s.indexOf("/rsrc/");
        if (marker >= 0) {
            return "rsrc/" + s.substring(marker + "/rsrc/".length());
        }
        return s;
    }

    // ---- small value helpers ----

    protected static float[] vec (Vector3f v)
    {
        return (v == null) ? null : new float[] { v.x, v.y, v.z };
    }

    protected static float[] vec (com.jme.math.Vector3f v)
    {
        return (v == null) ? null : new float[] { v.x, v.y, v.z };
    }

    protected static float[] quat (Quaternion q)
    {
        return (q == null) ? null : new float[] { q.getX(), q.getY(), q.getZ(), q.getW() };
    }

    protected static float[] col (ColorRGBA c)
    {
        return (c == null) ? null : new float[] { c.r, c.g, c.b, c.a };
    }

    protected static Object reflect (Object o, String field)
    {
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException(field));
    }

    // ---- JSON writer (the repo's lossless-float house style; see MxmlToGltf's writer) ----

    protected static void writeJson (Object o, StringBuilder sb, int indent)
    {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof Map<?, ?> m) {
            if (m.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                pad(sb, indent + 1);
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(": ");
                writeJson(e.getValue(), sb, indent + 1);
                if (++i < m.size()) sb.append(',');
                sb.append('\n');
            }
            pad(sb, indent);
            sb.append('}');
        } else if (o instanceof List<?> list) {
            if (list.isEmpty()) { sb.append("[]"); return; }
            sb.append("[\n");
            for (int j = 0; j < list.size(); j++) {
                pad(sb, indent + 1);
                writeJson(list.get(j), sb, indent + 1);
                if (j + 1 < list.size()) sb.append(',');
                sb.append('\n');
            }
            pad(sb, indent);
            sb.append(']');
        } else if (o instanceof float[] fa) {
            sb.append('[');
            for (int j = 0; j < fa.length; j++) {
                if (j > 0) sb.append(", ");
                sb.append(fmt(fa[j]));
            }
            sb.append(']');
        } else if (o instanceof Float f) {
            sb.append(fmt(f));
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o.toString());
        } else if (o instanceof String s) {
            writeString(s, sb);
        } else {
            throw new IllegalArgumentException("cannot serialize " + o.getClass());
        }
    }

    /** Lossless float text: Float.toString produces the shortest decimal that parses back to the
     * identical float (the same convention as the glTF toolchain's writer). */
    protected static String fmt (float f)
    {
        if (Float.isNaN(f) || Float.isInfinite(f)) {
            throw new IllegalArgumentException("non-finite float cannot be JSON-encoded: " + f);
        }
        return Float.toString(f);
    }

    protected static void writeString (String s, StringBuilder sb)
    {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    protected static void pad (StringBuilder sb, int indent)
    {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
    }
}
