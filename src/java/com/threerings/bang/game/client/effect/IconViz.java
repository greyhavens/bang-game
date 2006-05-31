//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.BillboardNode;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.LightState;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.RepairEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * An effect visualization that floats an icon above the sprite, letting users
 * know what happened in terms of gaining or losing health, switching sides,
 * etc., with consistent symbology.
 */
public class IconViz extends EffectViz
{
    /**
     * Creates an icon visualization for the given effect identifier, or
     * returns <code>null</code> if no icon is necessary.
     */
    public static IconViz createIconViz (Piece piece, String effect)
    {
        if (effect.equals(ShotEffect.DAMAGED) ||
            effect.equals(AreaDamageEffect.MISSILED)) {
            return new IconViz(piece.isAlive() ? "damaged" : "killed");
        
        } else if (effect.equals(RepairEffect.REPAIRED)) {
            return new IconViz("repaired");
            
        } else {
            return null;
        }
    }
    
    protected IconViz (String iname)
    {
        _iname = iname;   
    }
    
    @Override // documentation inherited
    protected void didInit ()
    {
        final Quad icon = RenderUtil.createIcon(ICON_SIZE, ICON_SIZE);
        icon.setRenderState(RenderUtil.createTextureState(_ctx,
            "textures/effects/" + _iname + ".png"));
        icon.setRenderState(RenderUtil.blendAlpha);
        icon.setRenderState(RenderUtil.alwaysZBuf);
        icon.updateRenderState();
        icon.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        icon.setLightCombineMode(LightState.OFF);
        icon.setDefaultColor(new ColorRGBA(JPIECE_COLORS[_target.owner]));
        
        _billboard = new BillboardNode("billboard") {
            public void updateWorldData (float time) {
                super.updateWorldData(time);
                float alpha;
                if ((_elapsed += time) >= RISE_DURATION + LINGER_DURATION +
                    FADE_DURATION) {
                    parent.detachChild(this);
                    return;
                    
                } else if (_elapsed >= RISE_DURATION + LINGER_DURATION) {
                    alpha = 1f - (_elapsed - RISE_DURATION - LINGER_DURATION) /
                        FADE_DURATION;
                    
                } else if (_elapsed >= RISE_DURATION) {
                    alpha = 1f;
                    localTranslation.z = TILE_SIZE * 1; 
                    
                } else {
                    alpha = _elapsed / RISE_DURATION;
                    localTranslation.z = TILE_SIZE * (0.5f + alpha * 0.5f);
                }
                icon.getBatch(0).getDefaultColor().a = alpha;
            }
            protected float _elapsed;
        };
        _billboard.attachChild(icon);
    }
    
    @Override // documentation inherited
    public void display (PieceSprite target)
    {
        target.attachChild(_billboard);
    }
    
    /** The name of the icon to display. */
    protected String _iname;
    
    /** The icon billboard. */
    protected BillboardNode _billboard;
    
    /** The size of the icon. */
    protected static final float ICON_SIZE = TILE_SIZE / 2;
    
    /** The length of time it takes for the icon to rise up and fade in. */
    protected static final float RISE_DURATION = 0.5f;
    
    /** The length of time the icon lingers before fading out. */
    protected static final float LINGER_DURATION = 1.25f;
    
    /** The length of time it takes for the icon to fade out. */
    protected static final float FADE_DURATION = 0.25f;
}
