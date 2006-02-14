//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jmex.effects.ParticleManager;

import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays a wreck with (optional) steam cloud and flying pieces of wreckage.
 */
public class WreckViz extends ParticleEffectViz
{
    public WreckViz (EffectViz wrapviz)
    {
        _wrapviz = wrapviz;   
    }
    
    @Override // documentation inherited
    public void init (BangContext ctx, BangBoardView view, Piece target,
                      Observer obs)
    {
        super.init(ctx, view, target, obs);
        if (_wrapviz != null) {
            _wrapviz.init(ctx, view, target, obs);
        }
    }
    
    @Override // documentation inherited
    public void display (PieceSprite target)
    {
        // set up and add the steam cloud
        if (_steamcloud != null) {
            displayParticleManager(target, _steamcloud, true);
        }
        
        // and the wreckage
        String[] wtypes = ((MobileSprite)target).getWreckageTypes();
        for (int i = 0; i < _wreckage.length; i++) {
            _wreckage[i].bind((String)RandomUtil.pickRandom(wtypes));
            target.attachChild(_wreckage[i]);
        }
        
        // display the wrapped effect viz
        if (_wrapviz != null) {
            _wrapviz.display(target);
            
        } else {
            effectDisplayed();
        }
    }
    
    @Override // documentation inherited
    protected void didInit ()
    {
        // create the steam cloud for wrecks without explosions
        if (!(_wrapviz instanceof ExplosionViz)) {
            _steamcloud = ParticleFactory.getSteamCloud();
        }
        
        // create a few pieces of wreckage to be thrown from the wreck
        _wreckage = new Wreckage[NUM_WRECKAGE_AVG +
            RandomUtil.getInt(+NUM_WRECKAGE_DEV, -NUM_WRECKAGE_DEV)];
        for (int i = 0; i < _wreckage.length; i++) {
            _wreckage[i] = new Wreckage();
        }
    }
    
    /** A piece of wreckage thrown from the machine. */
    protected class Wreckage extends Node
    {
        public Wreckage ()
        {
            super("wreckage");
            
            // fire the piece in a random direction
            float azimuth = RandomUtil.getFloat(FastMath.TWO_PI),
                elevation = getRandomFloat(FastMath.HALF_PI);
            _linear = new Vector3f(
                FastMath.cos(azimuth) * FastMath.cos(elevation),
                FastMath.sin(azimuth) * FastMath.cos(elevation),
                FastMath.sin(elevation));
            _linear.mult(TILE_SIZE / 2, getLocalTranslation());
            _linear.multLocal(WRECKAGE_INIT_SPEED);
            
            // pick a random starting rotation using Euler angles
            getLocalRotation().fromAngles(new float[] {
                RandomUtil.getFloat(FastMath.TWO_PI),
                RandomUtil.getFloat(FastMath.TWO_PI),
                RandomUtil.getFloat(FastMath.TWO_PI) });
            
            // initialize the angular velocity as principally around
            // the local up axis but with some wobble
            _angular = new Vector3f(getRandomFloat(FastMath.TWO_PI),
                getRandomFloat(FastMath.TWO_PI),
                FastMath.PI*8f + getRandomFloat(FastMath.TWO_PI));
            getLocalRotation().multLocal(_angular);
        }

        public void bind (String type)
        {
            _ctx.loadModel("units",
                "wreckage/" + type).getAnimation("normal").bind(this,
                    RandomUtil.getInt(Integer.MAX_VALUE), null);
        }
        
        public void updateWorldData (float time)
        {
            super.updateWorldData(time);
            
            // update the position, rotation, and velocity of the wreckage
            Vector3f loc = getLocalTranslation();
            loc.scaleAdd(time, _linear, loc);
            _linear.scaleAdd(time, WRECKAGE_ACCEL, _linear);
            _spin.set(_angular.x, _angular.y, _angular.z, 0f);
            _spin.multLocal(getLocalRotation()).multLocal(time * 0.5f);
            getLocalRotation().addLocal(_spin);
            getLocalRotation().normalize();
            
            // remove streamer if its lifespan has elapsed
            if ((_age += time) > WRECKAGE_LIFESPAN) {
                getParent().detachChild(this);
            }
        }
        
        protected float getRandomFloat (float twiceMax)
        {
            return RandomUtil.getFloat(twiceMax) - twiceMax * 0.5f;
        }
        
        /** The piece's linear and angular velocities. */
        protected Vector3f _linear, _angular;
        
        /** Temporary quaternion representing spin. */
        protected Quaternion _spin = new Quaternion();
        
        /** The piece's age in seconds. */
        protected float _age;
    }
    
    protected EffectViz _wrapviz;
    protected ParticleManager _steamcloud;
    protected Wreckage[] _wreckage;
    
    /** The average number of pieces of wreckage to throw. */
    protected static final int NUM_WRECKAGE_AVG = 8;
    
    /** The deviation of the number of pieces of wreckage. */
    protected static final int NUM_WRECKAGE_DEV = 2;
    
    /** The initial speed of the pieces of wreckage. */
    protected static final float WRECKAGE_INIT_SPEED = 25f;
    
    /** The acceleration of the pieces of wreckage. */
    protected static final Vector3f WRECKAGE_ACCEL =
        new Vector3f(0f, 0f, -100f);
    
    /** The amount of time in seconds to keep the wreckage alive. */
    protected static final float WRECKAGE_LIFESPAN = 5f;
}
