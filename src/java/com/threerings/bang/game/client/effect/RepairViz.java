//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.bounding.BoundingBox;
import com.jme.bounding.BoundingSphere;
import com.jme.bounding.BoundingVolume;
import com.jme.bounding.OrientedBoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Matrix4f;
import com.jme.math.Vector3f;
import com.jme.renderer.AbstractCamera;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.renderer.TextureRenderer;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.*;

/**
 * Displays the effect when a unit is repaired.
 */
public class RepairViz extends EffectViz
{
    @Override // documentation inherited
    public void display (PieceSprite target)
    {
        // start up the glow effect
        _glow.activate(target);
        
        // note that the effect was displayed
        effectDisplayed();
    }
    
    @Override // documentation inherited
    protected void didInit ()
    {
        _glow = new Glow();
    }
    
    /** Creates a glow effect by rendering the target to a texture and
     * rendering lots of copies of it using additive blending. */
    protected class Glow extends Node
    {
        public Glow ()
        {
            super("glow");
            
            _trenderer = _ctx.getDisplay().createTextureRenderer(TEXTURE_SIZE,
                TEXTURE_SIZE, true, false, false, false,
                TextureRenderer.RENDER_TEXTURE_2D, 0);
            _trenderer.setBackgroundColor(ColorRGBA.black);
            _trenderer.setupTexture(_texture = new Texture());
            
            TextureState tstate = _ctx.getRenderer().createTextureState();
            tstate.setTexture(_texture);
            setRenderState(tstate);
            setRenderState(RenderUtil.addAlpha);
            setRenderQueueMode(Renderer.QUEUE_ORTHO);
            setLightCombineMode(LightState.OFF);
            
            _panes = new Quad[GLOW_PANES];
            _locs = new Vector3f[GLOW_PANES];
            for (int ii = 0; ii < _panes.length; ii++) {
                _panes[ii] = new Quad("pane", 1f, 1f);
                _panes[ii].setDefaultColor(new ColorRGBA());
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
                return;
            }

            // if the target is on screen, determine its location and size in
            // screen space
            AbstractCamera rcam =
                (AbstractCamera)_ctx.getRenderer().getCamera();
            BoundingVolume bounds = _target.getWorldBound();
            if (rcam.contains(bounds) == Camera.OUTSIDE_FRUSTUM) {
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
            _target.setCullMode(CULL_NEVER);
            setCullMode(CULL_ALWAYS);
            _trenderer.render(_target, _texture);
            setCullMode(CULL_INHERIT);
            _target.setCullMode(CULL_INHERIT);
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
                _panes[ii].getDefaultColor().interpolate(ColorRGBA.black,
                    ColorRGBA.white, a * 0.5f);
            }
        }
    
        protected Quad[] _panes;
        protected TextureRenderer _trenderer;
        protected Texture _texture;
        protected PieceSprite _target;
        protected float _elapsed;
        
        protected Vector3f _tmp = new Vector3f(), _extent = new Vector3f(),
            _loc = new Vector3f(), _scale = new Vector3f();
        protected Vector3f[] _locs;
    }
    
    /** The glow effect. */
    protected Glow _glow;
    
    /** The size of the texture to render. */
    protected static final int TEXTURE_SIZE = 128;
    
    /** The number of panes in the glow effect. */
    protected static final int GLOW_PANES = 8;
    
    /** The duration of the glow effect. */
    protected static final float GLOW_DURATION = 1.5f;
    
    /** The size of the glow as a proportion of the screen radius of the
     * target. */
    protected static final float GLOW_SCALE = 0.0625f;
}
