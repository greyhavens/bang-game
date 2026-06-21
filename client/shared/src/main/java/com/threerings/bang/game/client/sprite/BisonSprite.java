//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.awt.Point;
import java.util.List;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

import com.threerings.jme.model.Model;

import com.threerings.bang.game.client.StampedeHandler;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.StampedeEffect;

/**
 * Used by {@link StampedeHandler} to display the stampeding bison.
 */
public class BisonSprite extends MobileSprite
{
    public BisonSprite (float angle, float distance, List<Point> path)
    {
        super("extras", "frontier_town/bison");
        _offset = new Vector3f(FastMath.cos(angle) * distance,
            FastMath.sin(angle) * distance, 0f);
        _pendingPath = path;
    }
    
    @Override // documentation inherited
    public void move (BangBoard board, List<Point> path, float speed)
    {
        super.move(board, path, speed);
        
        // fade the bison in and out at the beginning and end of the path
        final float duration = (path.size() - 1) / speed;
        final SpriteMaterialState mstate = new SpriteMaterialState();
        mstate.getAmbient().set(ColorRGBA.White);
        mstate.getDiffuse().set(ColorRGBA.White);
        addControl(new AbstractControl() {
            protected void controlUpdate (float time) {
                if ((_elapsed += time) >= duration) {
                    spatial.removeControl(this);
                    return;
                } else if (_elapsed > duration - FADE_TIME) {
                    mstate.getDiffuse().a = (duration - _elapsed) / FADE_TIME;
                } else if (_elapsed >= FADE_TIME) {
                    mstate.getDiffuse().a = 1f;
                } else {
                    mstate.getDiffuse().a = _elapsed / FADE_TIME;
                }
                mstate.apply(BisonSprite.this);
            }
            protected void controlRender (RenderManager rm, ViewPort vp) {}
            protected float _elapsed;
        });
    }
    
    @Override // documentation inherited
    protected void setCoord (BangBoard board, Vector3f[] coords, int idx, 
                             int nx, int ny, boolean moving)
    {
        super.setCoord(board, coords, idx, nx, ny, moving);
        coords[idx].addLocal(_offset);
    }
    
    @Override // documentation inherited
    protected void modelLoaded (Model model)
    {
        // set the bison on its stored path when it's finished loading
        super.modelLoaded(model);
        if (_pendingPath != null) {
            move(_view.getBoard(), _pendingPath, StampedeEffect.BISON_SPEED);
            _pendingPath = null;
        }
    }
    
    /** The offset of this bison from the center of the herd. */
    protected Vector3f _offset;

    /** The path to take after loading the model. */
    protected List<Point> _pendingPath;
    
    /** The time it takes the bison to fade in and out (seconds). */
    protected static float FADE_TIME = 2 / StampedeEffect.BISON_SPEED;
}
