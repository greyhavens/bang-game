//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.Properties;

import com.jme.math.Vector3f;
import com.jme.scene.Geometry;
import com.jme.util.geom.BufferUtils;

import com.threerings.bang.client.Model;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.BoardView;

import static com.threerings.bang.Log.*;

/**
 * The superclass of emissive effects defined in code that can be associated
 * with sprites and their animations.  Also acts as a static factory for the
 * various types of emissions.
 */
public abstract class SpriteEmission implements Cloneable
{
    /** A plume of smoke. */
    public static final String SMOKE_PLUME = "smoke_plume";
    
    /** A gunshot. */
    public static final String GUNSHOT = "gunshot";
    
    /**
     * Creates and returns an emission instance.
     *
     * @param props the properties containing the type and configuration of the
     * emission
     */
    public static SpriteEmission create (String name, Properties props)
    {
        String type = props.getProperty("type");
        if (SMOKE_PLUME.equals(type)) {
            return new SmokePlumeEmission(name, props);
            
        } else if (GUNSHOT.equals(type)) {
            return new GunshotEmission(name, props);
            
        } else {
            log.warning("Requested invalid sprite emission [type=" +
                type + "].");
            return null;
        }
    }
    
    public SpriteEmission (String name)
    {
        _name = name;
    }
    
    /**
     * Initializes the emission with the necessary references.
     */
    public void init (BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        _ctx = ctx;
        _view = view;
        _sprite = sprite;
    }
    
    /**
     * Cleans up the emission, removing any associated nodes.
     */
    public void cleanup ()
    {
    }
    
    /**
     * Called when an animation involving this emission is bound to a sprite.
     */
    public void start (Model.Animation anim, Model.Binding binding)
    {
        _anim = anim;
        _binding = binding;
    }
    
    /**
     * Called when the current animation is unbound.
     */
    public void stop ()
    {
        _anim = null;
        _binding = null;
    }
    
    /**
     * Determines whether this emission is running.
     */
    public boolean isRunning ()
    {
        return _anim != null;
    }
    
    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
    
    /**
     * Determines the current location of the emitter by finding the center
     * of the marker geometry.
     *
     * @param world if true, compute the location in world coordinates;
     * otherwise, use model coordinates
     */
    protected void getEmitterLocation (boolean world, Vector3f result)
    {
        Geometry marker = getMarker();
        if (marker == null) {
            result.set(world ? _sprite.getWorldTranslation() : Vector3f.ZERO);
            
        } else if (world) {
            marker.updateWorldVectors();
            marker.updateWorldBound();
            result.set(marker.getWorldBound().getCenter());
            
        } else {
            result.set(marker.getModelBound().getCenter());
        }
    }
    
    /**
     * Determines the current direction by checking the normals of the marker
     * geometry.
     *
     * @param world if true, compute the location in world coordinates;
     * otherwise, use model coordinates
     */
    protected void getEmitterDirection (boolean world, Vector3f result)
    {
        Geometry marker = getMarker();
        if (marker == null) {
            result.set(Vector3f.UNIT_Z);
            
        } else {
            BufferUtils.populateFromBuffer(result,
                marker.getNormalBuffer(), 0);
        }
        if (world) {
            _sprite.getWorldRotation().multLocal(result);
        }
    }
    
    /**
     * Returns a reference to the marker geometry associated with this
     * emission, or <code>null</code> if it could not be found.
     */
    protected Geometry getMarker ()
    {
        if (_binding == null) {
            log.warning("Received request for emission marker when stopped " +
                "[name=" + _name + "].");
            return null;
        }
        Geometry marker = _binding.getMarker(_name);
        if (marker == null) {
            log.warning("Couldn't locate marker geometry [action=" +
                _anim.action + ", name=" + _name + "].");
        }
        return marker;
    }

    protected String _name;
    
    protected BasicContext _ctx;
    protected BoardView _view;
    protected PieceSprite _sprite;
    
    protected Model.Animation _anim;
    protected Model.Binding _binding;
    
    protected boolean _active;
}
