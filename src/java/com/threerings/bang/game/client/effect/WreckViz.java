//
// $Id$

package com.threerings.bang.game.client.effect;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.state.MaterialState;
import com.jmex.effects.particles.ParticleMesh;

import com.samskivert.util.RandomUtil;

import com.threerings.jme.model.Model;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.game.client.BangBoardView;
import com.threerings.bang.game.client.sprite.ActiveSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.RenderUtil;

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
    public void init (BangContext ctx, BangBoardView view, PieceSprite sprite, Observer obs)
    {
        super.init(ctx, view, sprite, obs);
        if (_wrapviz != null) {
            _wrapviz.init(ctx, view, sprite, obs);
        }
    }

    @Override // documentation inherited
    public void display ()
    {
        // set up and add the steam cloud
        if (_steamcloud != null) {
            displayParticles(_steamcloud, true);
        }

        // and the wreckage
        if (_wreckage != null) {
            String[] wtypes = ((ActiveSprite)_sprite).getWreckageTypes();
            if (wtypes != null && wtypes.length > 0) {
                for (int i = 0; i < _wreckage.length; i++) {
                    _wreckage[i].bind(RandomUtil.pickRandom(wtypes));
                    _sprite.attachChild(_wreckage[i]);
                    _wreckage[i].updateRenderState();
                }
            }
        }

        // display the wrapped effect viz
        if (_wrapviz != null) {
            _wrapviz.display();

        } else {
            effectDisplayed();
        }
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        // create the steam cloud for wrecks without explosions
        if (!(_wrapviz instanceof ExplosionViz)) {
            _steamcloud = ParticlePool.getSteamCloud();
        }

        // create a few pieces of wreckage to be thrown from the wreck
        if (BangPrefs.isHighDetail()) {
            _wreckage = new Wreckage[NUM_WRECKAGE_AVG +
                RandomUtil.getInRange(-NUM_WRECKAGE_DEV, 1+NUM_WRECKAGE_DEV)];
            for (int i = 0; i < _wreckage.length; i++) {
                _wreckage[i] = new Wreckage();
            }
        }
    }

    /** A piece of wreckage thrown from the machine. */
    public class Wreckage extends Node
    {
        public Wreckage ()
        {
            super("wreckage");

            // initialize the render state for transparency control
            setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
            setRenderState(RenderUtil.blendAlpha);
            _mstate = _ctx.getRenderer().createMaterialState();
            _mstate.getDiffuse().set(ColorRGBA.white);
            _mstate.getAmbient().set(ColorRGBA.white);
            setRenderState(_mstate);

            // fire the piece in a random direction
            float azimuth = RandomUtil.getFloat(FastMath.TWO_PI),
                elevation = RandomUtil.getFloat(FastMath.PI * 0.45f);
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
            _ctx.loadModel("units", "wreckage/" + type,
                new ResultAttacher<Model>(this));
        }

        public void updateWorldVectors ()
        {
            // the first time we update with the parent, get the relative vecs
            if (!_wvinit && getParent() != null) {
                super.updateWorldVectors();
                localTranslation.set(worldTranslation);
                localScale.set(worldScale);
                worldRotation.multLocal(_linear);
                _wvinit = true;

            } else {
                worldTranslation.set(localTranslation);
                worldRotation.set(localRotation);
                worldScale.set(localScale);
            }
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

            // have the wreckage bounce if it reaches the terrain
            float height = _view.getTerrainNode().getHeightfieldHeight(
                loc.x, loc.y);
            if (loc.z < height) {
                loc.z = height;
                Vector3f normal = _view.getTerrainNode().getHeightfieldNormal(
                    loc.x, loc.y);
                _linear.negateLocal();
                normal.multLocal(_linear.dot(normal) * 2f);
                normal.subtract(_linear, _linear).multLocal(0.75f);
                _angular.multLocal(0.75f);
            }

            // fade it out over time
            float alpha = 1f - (_age / WRECKAGE_LIFESPAN);
            _mstate.getDiffuse().a = alpha;
            _mstate.getAmbient().a = alpha;

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

        /** The piece's material state, used to control alpha. */
        protected MaterialState _mstate;

        /** Set when the piece has its initial world vectors. */
        protected boolean _wvinit;

        /** Temporary quaternion representing spin. */
        protected Quaternion _spin = new Quaternion();

        /** The piece's age in seconds. */
        protected float _age;
    }

    protected EffectViz _wrapviz;
    protected ParticleMesh _steamcloud;
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
    protected static final float WRECKAGE_LIFESPAN = 3f;
}
