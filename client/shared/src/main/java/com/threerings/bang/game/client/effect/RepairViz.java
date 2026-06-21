//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme3.bounding.BoundingVolume;
import com.jme3.effect.ParticleEmitter;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.BackTextureRenderer;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the effect when a unit is repaired.
 *
 * <h3>jME3 cutover (Phase 2, cluster 2 — a {@link BackTextureRenderer} consumer)</h3>
 *
 * The fork "glow" rendered the target unit to a texture (back-buffer copy {@code TextureRenderer})
 * then drew many additive, spinning ortho {@code Quad}s of it in screen space, projecting the
 * target's world bounds to screen coordinates via {@code DisplaySystem.getScreenCoordinates}. jME3
 * render-to-texture is the FrameBuffer-backed {@link BackTextureRenderer} (which renders a clean
 * offscreen pass through {@link RenderManager#renderViewPort}), and screen projection is
 * {@code Camera.getScreenCoordinates}.
 *
 * <p>The Glow captures the target offscreen via the {@link BackTextureRenderer} RTT seam each frame
 * and then composites the captured texture as {@code GLOW_PANES} additive, spinning panes. The fork
 * drew those panes in the {@code QUEUE_ORTHO} screen-space queue, projecting the target's world
 * bounds to screen pixels via {@code DisplaySystem.getScreenCoordinates} and sizing/positioning the
 * panes around the projected screen radius. jME3 exposes no GUI/ortho overlay node to effects, so we
 * reproduce the same look in world space: the panes are camera-facing ({@link BillboardControl})
 * additive quads centred on the target, sized to the target's world radius and spun in the
 * billboard's local plane, attached to the same piece-node the fork's Glow used. The additive blend
 * (SRC_ALPHA / ONE, no depth write) and the pane count/spin/brightness ramp match the fork. The
 * sparkle <b>swirl</b> half of the effect is fully ported.
 */
public class RepairViz extends ParticleEffectViz
{
    @Override // documentation inherited
    public void display ()
    {
        // start up the glow effect
        if (_glow != null && _sprite != null) {
            _glow.activate(_sprite);
        }

        // and the swirl effect
        displayParticles(_swirls[0].particles, true);
        displayParticles(_swirls[1].particles, true);
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        if (BangPrefs.isHighDetail()) {
            _glow = new Glow();
        }
        _swirls = new Swirl[] { new Swirl(0f), new Swirl(FastMath.PI) };
    }

    /** Creates a glow effect by rendering the target to a texture and compositing many additive,
     * spinning camera-facing copies of it (the jME3 analogue of the fork's screen-space ortho-pane
     * composite). */
    protected class Glow extends Node
    {
        public Glow ()
        {
            super("glow");

            _trenderer = RenderUtil.createTextureRenderer(_ctx, TEXTURE_SIZE, TEXTURE_SIZE);
            _trenderer.setBackgroundColor(new ColorRGBA(ColorRGBA.Black));
            _texture = _ctx.getTextureCache().createTexture();
            _trenderer.setupTexture(_texture);

            // jME3: the fork attached GLOW_PANES textured Quads to the ortho Glow node, all sharing
            // the RTT texture + addAlpha state. Here the panes are camera-facing (BillboardControl)
            // additive quads on the same shared RTT-textured material; the BillboardControl gives the
            // fork's "always face the viewer" screen-space look. The shared material is additive
            // (SRC_ALPHA / ONE) with no depth write (RenderUtil overlay depth) so the copies
            // accumulate as an emissive halo rather than occluding one another.
            _mat = RenderUtil.createTextureMaterial(_ctx, _texture);
            RenderUtil.applyAddAlpha(_mat);
            RenderUtil.applyOverlayZBuf(_mat);
            _mat.setColor("Color", new ColorRGBA(ColorRGBA.Black));

            _billboard = new Node("glowpanes");
            _billboard.addControl(new BillboardControl());
            _panes = new Geometry[GLOW_PANES];
            for (int ii = 0; ii < _panes.length; ii++) {
                _panes[ii] = new Geometry("pane", new Quad(1f, 1f));
                _panes[ii].setMaterial(_mat);
                _panes[ii].setQueueBucket(Bucket.Transparent);
                _panes[ii].setShadowMode(
                    com.jme3.renderer.queue.RenderQueue.ShadowMode.Off);
                _billboard.attachChild(_panes[ii]);
            }
            attachChild(_billboard);
        }

        public void activate (PieceSprite target)
        {
            _target = target;
            _view.getPieceNode().attachChild(this);
            setLocalTranslation(target.getLocalTranslation().clone());
            addControl(new AbstractControl() {
                @Override protected void controlUpdate (float time) {
                    step(time);
                }
                @Override protected void controlRender (RenderManager rm, ViewPort vp) {
                }
            });
        }

        protected void step (float time)
        {
            if ((_elapsed += time) > GLOW_DURATION) {
                if (getParent() != null) {
                    _view.getPieceNode().detachChild(this);
                }
                _trenderer.cleanup();
                return;
            }
            if (RenderUtil.isOutsideFrustum(_target)) {
                return;
            }
            // point the RTT camera at the target and capture it offscreen.
            com.jme3.renderer.Camera rcam = _ctx.getCameraHandler().getCamera();
            com.jme3.renderer.Camera tcam = _trenderer.getCamera();
            tcam.setLocation(rcam.getLocation().clone());
            tcam.setAxes(rcam.getLeft().clone(), rcam.getUp().clone(),
                rcam.getDirection().clone());
            tcam.setFrustum(rcam.getFrustumNear(), rcam.getFrustumFar(),
                rcam.getFrustumLeft(), rcam.getFrustumRight(),
                rcam.getFrustumTop(), rcam.getFrustumBottom());
            _trenderer.render(_target);

            // Composite the captured texture as GLOW_PANES additive spinning panes. The fork sized
            // the panes to the target's projected screen radius and spun them around the projected
            // centre; in world space we size them to the target's world radius (the BillboardControl
            // reproduces the screen-facing projection) and centre the billboard on the target.
            float radius = 1f;
            BoundingVolume bounds = _target.getWorldBound();
            if (bounds != null) {
                bounds.getCenter(_center);
                _billboard.setLocalTranslation(
                    _center.subtract(getWorldTranslation()));
                radius = boundingRadius(bounds);
            }

            // The fork ramped the spin radius + pane brightness up then down over the glow's life
            // (a = t<0.5 ? t/0.5 : 2-t/0.5) and spun all panes one full revolution; reproduce that.
            float t = _elapsed / GLOW_DURATION,
                a = t < 0.5f ? (t / 0.5f) : (2f - t / 0.5f),
                sep = FastMath.TWO_PI / _panes.length,
                size = radius * 2f,
                dist = a * GLOW_SCALE * radius;
            for (int ii = 0; ii < _panes.length; ii++) {
                float angle = ii * sep + t * FastMath.TWO_PI;
                _panes[ii].setLocalScale(size, size, 1f);
                // centre each quad (its origin is a corner) then offset it around the ring.
                _panes[ii].setLocalTranslation(
                    -size * 0.5f + dist * FastMath.cos(angle),
                    -size * 0.5f + dist * FastMath.sin(angle), 0f);
            }
            // fork: panes interpolate black -> white by a*0.5 (a brightness, not alpha, ramp on the
            // additive material). With the shared additive material we drive the Unshaded Color.
            float b = a * 0.5f;
            _mat.setColor("Color", new ColorRGBA(b, b, b, 1f));
        }

        /** Approximates the fork's "extent length" screen radius from a jME3 bounding volume. */
        protected float boundingRadius (BoundingVolume bounds)
        {
            if (bounds instanceof com.jme3.bounding.BoundingSphere) {
                return ((com.jme3.bounding.BoundingSphere)bounds).getRadius();
            } else if (bounds instanceof com.jme3.bounding.BoundingBox) {
                com.jme3.bounding.BoundingBox bbox = (com.jme3.bounding.BoundingBox)bounds;
                return _extent.set(bbox.getXExtent(), bbox.getYExtent(),
                    bbox.getZExtent()).length();
            }
            return 1f;
        }

        protected BackTextureRenderer _trenderer;
        protected Texture2D _texture;
        protected Material _mat;
        protected Node _billboard;
        protected Geometry[] _panes;
        protected PieceSprite _target;
        protected float _elapsed;

        protected Vector3f _center = new Vector3f(), _extent = new Vector3f();
    }

    /** The swirl of sparkles effect. */
    protected class Swirl
    {
        /** The particle emitter for the swirl. */
        public ParticleEmitter particles;

        public Swirl (final float a0)
        {
            particles = ParticlePool.getSparkles();
            particles.setParticlesPerSec(512f);
            particles.setLocalTranslation(new Vector3f());

            // jME3: the per-frame fork Controller becomes an AbstractControl on the emitter.
            particles.addControl(new AbstractControl() {
                @Override protected void controlUpdate (float time) {
                    // remove swirl if its lifespan has elapsed
                    if ((_elapsed += time) > GLOW_DURATION) {
                        particles.removeControl(this);
                        removeParticles(particles);
                        if (!_displayed) { // report completion
                            effectDisplayed();
                            _displayed = true;
                        }
                        return;

                    } else if (_elapsed > SWIRL_DURATION) {
                        particles.setParticlesPerSec(0f);
                        return;
                    }
                    float t = _elapsed / SWIRL_DURATION,
                        radius = TILE_SIZE / 2,
                        angle = a0 + t * FastMath.TWO_PI * SWIRL_REVOLUTIONS;
                    particles.setLocalTranslation(
                        radius * FastMath.cos(angle),
                        radius * FastMath.sin(angle),
                        TILE_SIZE * t - radius);
                }
                @Override protected void controlRender (RenderManager rm, ViewPort vp) {
                }
                protected float _elapsed;
            });
        }
    }

    /** The glow effect. */
    protected Glow _glow;

    /** The swirls of sparkles. */
    protected Swirl[] _swirls;

    /** Whether or not we have reported ourself as displayed. */
    protected boolean _displayed;

    /** The size of the texture to render. */
    protected static final int TEXTURE_SIZE = 128;

    /** The number of panes in the glow effect. */
    protected static final int GLOW_PANES = 8;

    /** The duration of the glow effect. */
    protected static final float GLOW_DURATION = 1.5f;

    /** The size of the glow as a proportion of the screen radius of the
     * target. */
    protected static final float GLOW_SCALE = 0.0625f;

    /** The duration of the swirl effect. */
    protected static final float SWIRL_DURATION = 1.125f;

    /** The number of revolutions for the swirl to complete. */
    protected static final float SWIRL_REVOLUTIONS = 1.5f;
}
