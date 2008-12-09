//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.bounding.BoundingVolume;
import com.jme.bounding.OrientedBoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.renderer.TextureRenderer;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jmex.effects.particles.ParticleMesh;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the effect when a unit is repaired.
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

    /** Creates a glow effect by rendering the target to a texture and
     * rendering lots of copies of it using additive blending. */
    protected class Glow extends Node
    {
        public Glow ()
        {
            super("glow");

            _trenderer = RenderUtil.createTextureRenderer(_ctx, TEXTURE_SIZE,
                TEXTURE_SIZE);
            _trenderer.setBackgroundColor(ColorRGBA.black);
            _texture = _ctx.getTextureCache().createTexture();
            _texture.setRTTSource(Texture.RTT_SOURCE_RGB);
            _trenderer.setupTexture(_texture);

            _tstate = _ctx.getRenderer().createTextureState();
            _tstate.setTexture(_texture);
            setRenderState(_tstate);
            setRenderState(RenderUtil.addAlpha);
            setRenderQueueMode(Renderer.QUEUE_ORTHO);
            setLightCombineMode(LightState.OFF);

            _panes = new Quad[GLOW_PANES];
            _locs = new Vector3f[GLOW_PANES];
            for (int ii = 0; ii < _panes.length; ii++) {
                _panes[ii] = new Quad("pane", 1f, 1f);
                _panes[ii].getBatch(0).getDefaultColor().set(new ColorRGBA());
                attachChild(_panes[ii]);
                _locs[ii] = new Vector3f();
            }

            updateRenderState();
        }

        public void activate (PieceSprite target)
        {
            _target = target;
            _view.getPieceNode().attachChild(this);

            localTranslation.set(target.getLocalTranslation());
        }

        public void updateWorldData (float time)
        {
            if ((_elapsed += time) > GLOW_DURATION) {
                _view.getPieceNode().detachChild(this);
                _trenderer.cleanup();
                _tstate.deleteAll();
                return;
            }

            // if the target is on screen, determine its location and size in
            // screen space
            BoundingVolume bounds = _target.getWorldBound();
            if (bounds == null) {
                return;
            }
            Camera rcam = _ctx.getCameraHandler().getCamera();
            int pstate = rcam.getPlaneState();
            int isect = rcam.contains(bounds);
            rcam.setPlaneState(pstate);
            if (isect == Camera.OUTSIDE_FRUSTUM) {
                return;
            }
            bounds.getCenter(_tmp);
            DisplaySystem display = _ctx.getDisplay();
            display.getScreenCoordinates(_tmp, _loc);
            float radius = 1f;
            if (bounds instanceof BoundingSphere) {
                radius = ((BoundingSphere)bounds).getRadius();
            } else if (bounds instanceof BoundingBox) {
                BoundingBox bbox = (BoundingBox)bounds;
                radius = _extent.set(bbox.xExtent, bbox.yExtent,
                    bbox.zExtent).length();
            } else if (bounds instanceof OrientedBoundingBox) {
                radius = ((OrientedBoundingBox)bounds).getExtent().length();
            }

            _tmp.scaleAdd(-radius, rcam.getLeft(), _tmp);
            display.getScreenCoordinates(_tmp, _extent);
            radius = _extent.x - _loc.x;
            _scale.set(radius*2, radius*2, 1f);
            int width = display.getWidth(), height = display.getHeight();
            float px = _loc.x / width, py = _loc.y / height,
                psize = radius / width;

            // render a frame consisting of the target, without lighting
            Camera tcam = _trenderer.getCamera();
            tcam.setFrame(rcam.getLocation(), rcam.getLeft(), rcam.getUp(),
                rcam.getDirection());
            float left = rcam.getFrustumLeft(), right = rcam.getFrustumRight(),
                top = rcam.getFrustumTop(), bottom = rcam.getFrustumBottom();
            tcam.setFrustum(rcam.getFrustumNear(), rcam.getFrustumFar(),
                FastMath.LERP(px - psize, left, right),
                FastMath.LERP(px + psize, left, right),
                FastMath.LERP(py + psize, bottom, top),
                FastMath.LERP(py - psize, bottom, top));
            _trenderer.updateCamera();
            _target.setLightCombineMode(LightState.OFF);
            int ocmode = _target.getCullMode();
            _target.setCullMode(CULL_NEVER);
            _trenderer.render(_target, _texture);
            _target.setCullMode(ocmode);
            _target.setLightCombineMode(LightState.INHERIT);
            rcam.update();

            // update the spinning panes
            float t = _elapsed / GLOW_DURATION,
                a = t < 0.5f ? (t / 0.5f) : (2f - t / 0.5f), dist,
                sep = FastMath.TWO_PI / _panes.length, angle;
            for (int ii = 0; ii < _panes.length; ii++) {
                dist = a * GLOW_SCALE * radius;
                angle = ii * sep + t * FastMath.TWO_PI;
                _panes[ii].setLocalScale(_scale);
                _panes[ii].setLocalTranslation(_locs[ii].set(
                    _loc.x + dist * FastMath.cos(angle),
                    _loc.y + dist * FastMath.sin(angle), _loc.z));
                _panes[ii].getBatch(0).getDefaultColor().interpolate(
                    ColorRGBA.black, ColorRGBA.white, a * 0.5f);
            }
        }

        protected Quad[] _panes;
        protected TextureRenderer _trenderer;
        protected Texture _texture;
        protected TextureState _tstate;
        protected PieceSprite _target;
        protected float _elapsed;

        protected Vector3f _tmp = new Vector3f(), _extent = new Vector3f(),
            _loc = new Vector3f(), _scale = new Vector3f();
        protected Vector3f[] _locs;
    }

    /** The swirl of sparkles effect. */
    protected class Swirl
    {
        /** The particle system for the swirl. */
        public ParticleMesh particles;

        public Swirl (final float a0)
        {
            particles = ParticlePool.getSparkles();
            particles.setReleaseRate(512);
            particles.setOriginOffset(new Vector3f());

            particles.addController(new Controller() {
                public void update (float time) {
                    // remove swirl if its lifespan has elapsed
                    if ((_elapsed += time) > GLOW_DURATION) {
                        particles.removeController(this);
                        removeParticles(particles);
                        if (!_displayed) { // report completion
                            effectDisplayed();
                            _displayed = true;
                        }
                        return;

                    } else if (_elapsed > SWIRL_DURATION) {
                        particles.setReleaseRate(0);
                        return;
                    }
                    float t = _elapsed / SWIRL_DURATION,
                        radius = TILE_SIZE / 2,
                        angle = a0 + t * FastMath.TWO_PI * SWIRL_REVOLUTIONS;
                    particles.getOriginOffset().set(
                        radius * FastMath.cos(angle),
                        radius * FastMath.sin(angle),
                        TILE_SIZE * t - radius);
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
