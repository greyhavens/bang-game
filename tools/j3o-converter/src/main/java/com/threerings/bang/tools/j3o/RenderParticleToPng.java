//
// $Id$

package com.threerings.bang.tools.j3o;

import java.io.File;
import java.util.Properties;

import com.jme3.asset.ModelKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Offscreen render-to-PNG harness for a baked {@code effects/<key>/particles.j3o} (Phase&nbsp;6a).
 *
 * <p>Particle emitters are time-based, so a single first-frame snapshot (the static model harness's
 * mode) shows nothing. This harness steps the emitter a configurable number of logical-state frames
 * (in {@link #buildScene}, before the base frames the camera) so particles are alive and spread and
 * the bounding-box camera frames the live cloud rather than an empty bound. It reproduces the
 * runtime {@code ParticleCache} framing — the
 * {@code particles.properties} {@code scale} and the y-up&rarr;z-up rotation — so the look matches
 * the live board, then renders against a dark backdrop so additive/alpha particles read clearly.
 *
 * <p>Stays on the isolated jME3 + LWJGL3 + {@code project(":app")} classpath (no fork, no LWJGL2,
 * no gdx), exactly like the model render harnesses.
 *
 * <p>Usage: {@code RenderParticleToPng <rsrc root> <effect key> <output png> [frames]}, e.g.
 * {@code RenderParticleToPng .../staging/rsrc boom_town/barrel_explosion /tmp/barrel.png 40}.
 */
public class RenderParticleToPng extends OffscreenRenderApp
{
    public static void main (String[] args)
    {
        if (args.length < 3) {
            System.err.println("Usage: RenderParticleToPng <rsrc root> <effect key> " +
                "<output png> [frames]");
            System.exit(1);
        }
        int frames = args.length > 3 ? Integer.parseInt(args[3]) : 40;
        launch(new RenderParticleToPng(args[0], args[1], args[2], frames));
    }

    public RenderParticleToPng (String rsrcRoot, String effect, String outPng, int frames)
    {
        super(new File(outPng));
        _rsrcRoot = new File(rsrcRoot).getAbsoluteFile();
        _effect = effect;
        _frames = frames;
    }

    @Override protected Node buildScene ()
    {
        assetManager.registerLocator(_rsrcRoot.getPath(), FileLocator.class);

        String assetPath = "effects/" + _effect + "/particles.j3o";
        Spatial particles = assetManager.loadModel(new ModelKey(assetPath));

        // reproduce ParticleCache's framing: scale from particles.properties + y-up -> z-up rotation.
        float scale = 0.025f;
        File props = new File(_rsrcRoot, "effects/" + _effect + "/particles.properties");
        if (props.isFile()) {
            try (java.io.FileInputStream in = new java.io.FileInputStream(props)) {
                Properties p = new Properties();
                p.load(in);
                scale = Float.parseFloat(p.getProperty("scale", "0.025"));
            } catch (Exception e) {
                System.out.println("  (could not read particles.properties: " + e + ")");
            }
        }
        Quaternion zUp = new Quaternion();
        zUp.fromAngleNormalAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        particles.setLocalScale(scale);
        particles.setLocalRotation(zUp);
        particles.setQueueBucket(RenderQueue.Bucket.Transparent);

        System.out.println("RenderParticleToPng: " + _effect + " (scale=" + scale +
            ", warmup frames=" + _frames + ")");

        Node scene = new Node("scene");
        scene.attachChild(particles);
        lightLikeBoard(scene);

        // Particle emitters are time-based and empty at t=0, so step the simulation here — BEFORE
        // the base frames the camera on the scene's bounding box — so the camera frames the live
        // particle cloud rather than a degenerate empty bound. The base then renders one frame,
        // capturing the populated cloud.
        //
        // jME3 ParticleEmitter draws emission position/velocity/lifetime from the process-wide,
        // time-seeded FastMath.rand, so the same effect rendered twice yields different clouds. Seed
        // it to a fixed value so the render is reproducible — that is what lets the Phase-7d visual-
        // regression goldens diff a particle render against a committed PNG without flaking.
        FastMath.rand.setSeed(PARTICLE_SEED);
        scene.updateGeometricState();
        // Mirror the live spawn path (ParticlePool.getParticles -> ParticleUtil.forceRespawn ->
        // emitAllParticles): fire each emitter's pool-sized burst once. Continuous/ambient emitters
        // (positive particles-per-sec) keep refilling as the scene steps; one-shot/burst emitters
        // (baked particles-per-sec == 0 for an RT_CLAMP effect) fire exactly this single burst and
        // then fade -- so the harness shows the same fire-and-fade the live game does, instead of an
        // empty cloud for a one-shot or (the old bug) a runaway continuously-emitting bloom.
        scene.depthFirstTraversal(new com.jme3.scene.SceneGraphVisitorAdapter() {
            @Override public void visit (com.jme3.scene.Geometry g) {
                if (g instanceof com.jme3.effect.ParticleEmitter emitter) {
                    emitter.emitAllParticles();
                }
            }
        });
        float tpf = 1f / 30f;
        for (int ii = 0; ii < _frames; ii++) {
            scene.updateLogicalState(tpf);
        }
        scene.updateGeometricState();
        return scene;
    }

    @Override protected ColorRGBA backgroundColor ()
    {
        // a near-black backdrop so additive particles (and faint alpha smoke) read clearly.
        return new ColorRGBA(0.05f, 0.05f, 0.07f, 1f);
    }

    @Override protected Camera createCamera (Spatial scene)
    {
        // frame the live particle cloud's bounding box (the base frames at 2.4x radius, which packs a
        // small soft cloud edge-to-edge); pull back further so individual particles read rather than
        // saturating the frame as one blob, and keep the board's Z-up convention.
        BoundingBox bb = (BoundingBox)scene.getWorldBound();
        Vector3f center = bb.getCenter();
        float radius =
            Math.max(bb.getXExtent(), Math.max(bb.getYExtent(), bb.getZExtent())) * 4.5f;
        radius = Math.max(radius, 1f);
        Vector3f eye = center.add(new Vector3f(radius, -radius, radius * 0.9f));
        Camera cam = new Camera(WIDTH, HEIGHT);
        cam.setFrustumPerspective(45f, (float)WIDTH / HEIGHT, 0.05f, radius * 10f);
        cam.setLocation(eye);
        cam.lookAt(center, Vector3f.UNIT_Z);
        return cam;
    }

    protected final File _rsrcRoot;
    protected final String _effect;
    protected final int _frames;

    /** Fixed seed for jME3's particle RNG ({@link FastMath#rand}) so warmed particle renders are
     * reproducible — required for the Phase-7d golden diff to be stable run-to-run. */
    protected static final long PARTICLE_SEED = 0xBA56BA11L;
}
