//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.io.IOException;

import java.util.HashMap;
import java.util.Properties;

import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Spatial;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jmex.effects.particles.ParticleGeometry;

import com.threerings.jme.model.Model;
import com.threerings.jme.util.SpatialVisitor;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.util.ParticleCache;
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
    public boolean shouldAccumulate ()
    {
        return false;
    }
    
    @Override // documentation inherited
    public void setSpriteRefs (
        BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        super.setSpriteRefs(ctx, view, sprite);
        if (_effect != null && BangPrefs.isHighDetail()) {
            _ctx.loadParticles(_effect,
                new ResultAttacher<Spatial>(_model.getEmissionNode()) {
                public void requestCompleted (Spatial result) {
                    super.requestCompleted(result);
                    _particles = result;
                    if (_windInfluenced) {
                        _view.addWindInfluence(_particles);
                    }
                    if (!isActive()) {
                        setReleaseRates();
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
            setReleaseRates();
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
    public void read (JMEImporter im)
        throws IOException
    {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        _effect = capsule.readString("effect", null);
        _windInfluenced = capsule.readBoolean("windInfluenced", false);
    }
    
    @Override // documentation inherited
    public void write (JMEExporter ex)
        throws IOException
    {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(_effect, "effect", null);
        capsule.write(_windInfluenced, "windInfluenced", false);
    }
    
    // documentation inherited
    public void update (float time)
    {
        if (!isActive() || _particles == null) {
            return;
        }
        _particles.getLocalTranslation().set(_target.getWorldTranslation());
        _target.getWorldRotation().mult(ParticleCache.Z_UP_ROTATION,
            _particles.getLocalRotation());
    }
    
    /**
     * Recursively sets or clears particle release rates based on whether or
     * not the emission is active.
     */
    protected void setReleaseRates ()
    {
        // store the release rates the first time they're set
        if (_releaseRates == null) {
            _releaseRates = new HashMap<ParticleGeometry, Integer>();
            new SpatialVisitor<ParticleGeometry>(ParticleGeometry.class) {
                protected void visit (ParticleGeometry geom) {
                    _releaseRates.put(geom, geom.getReleaseRate());
                }
            }.traverse(_particles);
        }
        new SpatialVisitor<ParticleGeometry>(ParticleGeometry.class) {
            protected void visit (ParticleGeometry geom) {
                geom.setReleaseRate(isActive() ? _releaseRates.get(geom) : 0);
            }
        }.traverse(_particles);
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
