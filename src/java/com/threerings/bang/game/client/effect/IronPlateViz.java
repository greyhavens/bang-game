//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.image.Texture;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Controller;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;

import com.threerings.jme.model.ModelMesh;
import com.threerings.jme.util.SpatialVisitor;

import com.threerings.bang.game.client.sprite.ActiveSprite;
import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

/**
 * A visualization for the iron plate.  When activated and when the unit is
 * shot, temporarily gives the unit a metallic appearance.
 */
public class IronPlateViz extends InfluenceViz
{
    @Override // documentation inherited
    public void init (BasicContext ctx, PieceSprite target)
    {
        super.init(ctx, target);
        if (_sphereMap == null) {
            _sphereMap = RenderUtil.createTextureState(ctx,
                "textures/environ/spheremap.png");
            _sphereMap.getTexture().setEnvironmentalMapMode(Texture.EM_SPHERE);
        }
        _mstate = ctx.getRenderer().createMaterialState();
        _mstate.getDiffuse().set(1f, 1f, 1f, 0f);
        _mstate.getEmissive().set(ColorRGBA.white);
        _mstate.getAmbient().set(ColorRGBA.white);
        _overlay = new RenderState[] { _sphereMap, _mstate,
            RenderUtil.blendAlpha, RenderUtil.overlayZBuf };
            
        new SpatialVisitor<ModelMesh>(ModelMesh.class) {
            protected void visit (ModelMesh mesh) {
                mesh.addOverlay(_overlay);
            }
        }.traverse(_target.getModelNode());
        
        ((MobileSprite)_target).addActionHandler(_handler);
        flashOverlay(0.5f);
    }
    
    @Override // documentation inherited
    public void destroy ()
    {
        ((MobileSprite)_target).removeActionHandler(_handler);
        
        new SpatialVisitor<ModelMesh>(ModelMesh.class) {
            protected void visit (ModelMesh mesh) {
                mesh.removeOverlay(_overlay);
            }
        }.traverse(_target.getModelNode());
    }
    
    /**
     * Flashes the overlay state over the specified duration (which includes
     * the time it takes to ramp in and out).
     */
    protected void flashOverlay (final float duration)
    {
        _target.addController(new Controller() {
            public void update (float time) {
                _elapsed = Math.min(_elapsed + time, duration);
                if (_elapsed < FADE_DURATION) {
                    _mstate.getDiffuse().a = _elapsed / FADE_DURATION;
                } else if (_elapsed <= duration - FADE_DURATION) {
                    _mstate.getDiffuse().a = 1f;
                } else {
                    _mstate.getDiffuse().a = 
                        (duration - _elapsed) / FADE_DURATION;
                }
                if (_elapsed >= duration) {
                    _target.removeController(this);
                }
            }
            protected float _elapsed;
        });
    }
    
    /** Flashes the overlay when the reacting animation plays. */
    protected ActiveSprite.ActionHandler _handler =
        new ActiveSprite.ActionHandler() {
        public String handleAction (ActiveSprite sprite, String action) {
            if ("reacting".equals(action)) {
                flashOverlay(sprite.getAction(action).getDuration());
                return action;
            } else {
                return null;
            }
        }
    };
    
    /** The sphere map overlay states. */
    protected RenderState[] _overlay;
    
    /** The material state used to manipulate the alpha value. */
    protected MaterialState _mstate;
    
    /** The shared sphere map texture state. */
    protected static TextureState _sphereMap;

    /** The time to take fading in and out of the overlay. */    
    protected static final float FADE_DURATION = 0.1f;
}
