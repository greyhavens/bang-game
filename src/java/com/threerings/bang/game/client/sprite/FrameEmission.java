//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.HashMap;
import java.util.Properties;

import com.jme.scene.Controller;
import com.jme.scene.Spatial;

import com.samskivert.util.StringUtil;

import com.threerings.jme.model.Model;

/**
 * An emission that fires at configured animation frames.
 */
public abstract class FrameEmission extends SpriteEmission
{
    @Override // documentation inherited
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        if (_animations == null) {
            return;
        }
        _animFrames = new HashMap<String, int[]>();
        for (String anim : _animations) {
            _animFrames.put(anim, StringUtil.parseIntArray(
                props.getProperty(anim + ".frames", "")));
        }
    }
    
    @Override // documentation inherited
    public void init (Model model)
    {
        super.init(model);
        _model = model;
    }
    
    @Override // documentation inherited
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
    {
        if (store == null) {
            return null;
        }
        FrameEmission fstore = (FrameEmission)store;
        super.putClone(fstore, properties);
        fstore._animFrames = _animFrames;
        return fstore;
    }
    
    // documentation inherited from interface Externalizable
    public void writeExternal (ObjectOutput out)
        throws IOException
    {
        super.writeExternal(out);
        out.writeObject(_animFrames);
    }
    
    // documentation inherited from interface Externalizable
    public void readExternal (ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        @SuppressWarnings("unchecked") HashMap<String,int[]> casted =
            (HashMap<String,int[]>)in.readObject();
        _animFrames = casted;
    }
    
    // documentation inherited
    public void update (float time)
    {
        if (!isActive() || !isActiveEmission() || 
            _frames == null || _idx >= _frames.length) {
            return;
        }
        int frame = (int)((_elapsed += time) / _frameDuration);
        if (frame >= _frames[_idx]) {
            fireEmission();
            _idx++;
        }
    }
    
    /**
     * Fires off the emission now that the target has reached one of the
     * configured animation frames.
     */
    protected abstract void fireEmission ();
    
    @Override // documentation inherited
    protected void animationStarted (String name)
    {
        super.animationStarted(name);
        if (!isActiveEmission()) {
            return;
        }
        
        // get the frames at which emissions go off, if any
        _frames = (_animFrames == null) ? null : _animFrames.get(name);
        if (_frames == null) {
            return;
        }
        
        // set initial animation state
        _frameDuration = 1f / _model.getAnimation(name).frameRate;
        _idx = 0;
        _elapsed = 0f;
    }
    
    /** For each animation, the frames at which the emission goes off. */
    protected HashMap<String, int[]> _animFrames;
    
    /** The model to which this emission is bound. */
    protected Model _model;
    
    /** The frames at which the emission goes off for the current animation. */
    protected int[] _frames;
    
    /** The duration of a single frame in seconds. */
    protected float _frameDuration;
    
    /** The index of the current frame. */
    protected int _idx;
    
    /** The time elapsed since the start of the animation. */
    protected float _elapsed;
    
    private static final long serialVersionUID = 1;
}
