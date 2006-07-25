//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.HashMap;
import java.util.Properties;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jmex.effects.particles.ParticleGeometry;

import com.threerings.jme.model.Model;

import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.client.BoardView;

/**
 * An emission that displays a particle effect.
 */
public class ParticleEmission extends SpriteEmission
{
    @Override // documentation inherited
    public void configure (Properties props, Spatial target)
    {
        super.configure(props, target);
        _effect = props.getProperty("effect");
        _windInfluenced = Boolean.parseBoolean(
            props.getProperty("wind_influenced"));
    }
    
    @Override // documentation inherited
    public void init (Model model)
    {
        super.init(model);
        _model = model;
    }
    
    @Override // documentation inherited
    public void setSpriteRefs (
        BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        super.setSpriteRefs(ctx, view, sprite);
        if (_effect != null) {
            _ctx.loadEffect(_effect,
                new ResultAttacher<Spatial>(_model.getEmissionNode()) {
                public void requestCompleted (Spatial result) {
                    super.requestCompleted(result);
                    _particles = result;
                    if (_windInfluenced) {
                        _view.addWindInfluence(_particles);
                    }
                    if (!isActive()) {
                        setReleaseRates(_particles);
                    }
                }
            });
        }
    }
    
    @Override // documentation inherited
    public void setActive (boolean active)
    {
        super.setActive(active);
        if (_particles != null) {
            setReleaseRates(_particles);
        }
    }
    
    @Override // documentation inherited
    public Controller putClone (
        Controller store, Model.CloneCreator properties)
    {
        ParticleEmission pstore;
        if (store == null) {
            pstore = new ParticleEmission();
        } else {
            pstore = (ParticleEmission)store;
        }
        super.putClone(pstore, properties);
        pstore._effect = _effect;
        pstore._windInfluenced = _windInfluenced;
        return pstore;
    }
    
    @Override // documentation inherited
    public void writeExternal (ObjectOutput out)
        throws IOException
    {
        super.writeExternal(out);
        out.writeObject(_effect);
        out.writeBoolean(_windInfluenced);
    }
    
    @Override // documentation inherited
    public void readExternal (ObjectInput in)
        throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        _effect = (String)in.readObject();
        _windInfluenced = in.readBoolean();
    }
    
    // documentation inherited
    public void update (float time)
    {
        if (!isActive() || _particles == null) {
            return;
        }
        getEmitterLocation(_particles.getLocalTranslation());
        getEmitterDirection(_dir);
        float angle = _dir.angleBetween(Vector3f.UNIT_Y);
        if (angle == 0f) {
            _particles.getLocalRotation().set(Quaternion.IDENTITY);
        } else if (angle == FastMath.PI) {
            _particles.getLocalRotation().fromAngleNormalAxis(
                FastMath.PI, Vector3f.UNIT_X);
        } else {
            _particles.getLocalRotation().fromAngleAxis(
                -angle, _dir.crossLocal(Vector3f.UNIT_Y));
        }
    }
    
    /**
     * Recursively sets or clears particle release rates based on whether or
     * not the emission is active.
     */
    protected void setReleaseRates (Spatial spatial)
    {
        if (_releaseRates == null) {
            _releaseRates = new HashMap<ParticleGeometry, Integer>();
            storeReleaseRates(spatial);
        }
        if (spatial instanceof ParticleGeometry) {
            ParticleGeometry geom = (ParticleGeometry)spatial;
            geom.setReleaseRate(isActive() ? _releaseRates.get(geom) : 0);
            
        } else if (spatial instanceof Node) {
            Node node = (Node)spatial;
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                setReleaseRates(node.getChild(ii));
            }
        }
    }
    
    /**
     * Recursively stores the original particle release rates.
     */
    protected void storeReleaseRates (Spatial spatial)
    {
        if (spatial instanceof ParticleGeometry) {
            ParticleGeometry geom = (ParticleGeometry)spatial;
            _releaseRates.put(geom, geom.getReleaseRate());
            
        } else if (spatial instanceof Node) {
            Node node = (Node)spatial;
            for (int ii = 0, nn = node.getQuantity(); ii < nn; ii++) {
                storeReleaseRates(node.getChild(ii));
            }
        }
    }
    
    /** The type of effect to create. */
    protected String _effect;
    
    /** Whether or not the effect should be influenced by the board wind. */
    protected boolean _windInfluenced;
    
    /** The model to which we're attached. */
    protected Model _model;
    
    /** The particle node. */
    protected Spatial _particles;
    
    /** The original release rates for each particle layer. */
    protected HashMap<ParticleGeometry, Integer> _releaseRates;
    
    protected Vector3f _dir = new Vector3f();
    
    private static final long serialVersionUID = 1;
}
