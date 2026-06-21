//
// $Id$

package com.threerings.bang.game.client.effect;

import java.util.ArrayList;

import com.jme3.effect.ParticleEmitter;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;

import com.samskivert.util.RandomUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.util.SpatialVisitor;

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

    /**
     * A piece of wreckage thrown from the machine.
     *
     * <p>jME3 cutover: the fork {@code Node} subclass drove its own flight/spin/bounce/fade via
     * overridden {@code updateWorldVectors}/{@code updateWorldData} hooks and a shared
     * {@code MaterialState}. jME3 has no such per-node scene hooks; the motion is an
     * {@link AbstractControl} and the fade is applied to the loaded model geometries' material
     * {@code Color} alpha.
     */
    public class Wreckage extends Node
    {
        public Wreckage ()
        {
            super("wreckage");
            setQueueBucket(Bucket.Transparent);

            // fire the piece in a random direction
            float azimuth = RandomUtil.getFloat(FastMath.TWO_PI),
                elevation = RandomUtil.getFloat(FastMath.PI * 0.45f);
            _linear = new Vector3f(
                FastMath.cos(azimuth) * FastMath.cos(elevation),
                FastMath.sin(azimuth) * FastMath.cos(elevation),
                FastMath.sin(elevation));
            setLocalTranslation(_linear.mult(TILE_SIZE / 2));
            _linear.multLocal(WRECKAGE_INIT_SPEED);

            // pick a random starting rotation using Euler angles
            Quaternion rot = new Quaternion();
            rot.fromAngles(
                RandomUtil.getFloat(FastMath.TWO_PI),
                RandomUtil.getFloat(FastMath.TWO_PI),
                RandomUtil.getFloat(FastMath.TWO_PI));
            setLocalRotation(rot);

            // initialize the angular velocity as principally around
            // the local up axis but with some wobble
            _angular = new Vector3f(getRandomFloat(FastMath.TWO_PI),
                getRandomFloat(FastMath.TWO_PI),
                FastMath.PI*8f + getRandomFloat(FastMath.TWO_PI));
            rot.multLocal(_angular);

            addControl(new AbstractControl() {
                @Override protected void controlUpdate (float time) {
                    step(time);
                }
                @Override protected void controlRender (RenderManager rm, ViewPort vp) {
                }
            });
        }

        public void bind (String type)
        {
            _ctx.loadModel("units", "wreckage/" + type,
                new ResultAttacher<Model>(this) {
                public void requestCompleted (Model model) {
                    super.requestCompleted(model);
                    // gather the loaded geometries' materials so we can fade their alpha; apply the
                    // alpha-blend preset so the fade is visible.
                    new SpatialVisitor<Geometry>(Geometry.class) {
                        protected void visit (Geometry geom) {
                            if (geom.getMaterial() != null) {
                                RenderUtil.applyBlendAlpha(geom.getMaterial());
                                _mats.add(geom.getMaterial());
                            }
                        }
                    }.traverse(model);
                }
            });
        }

        /** Advances the wreckage motion + fade by the supplied elapsed time. */
        protected void step (float time)
        {
            Vector3f loc = getLocalTranslation();
            loc.scaleAdd(time, _linear, loc);
            _linear.scaleAdd(time, WRECKAGE_ACCEL, _linear);
            Quaternion rot = getLocalRotation();
            _spin.set(_angular.x, _angular.y, _angular.z, 0f);
            _spin.multLocal(rot).multLocal(time * 0.5f);
            rot.addLocal(_spin);
            rot.normalizeLocal();
            setLocalRotation(rot);

            // have the wreckage bounce if it reaches the terrain
            float height = _view.getTerrainNode().getHeightfieldHeight(loc.x, loc.y);
            if (loc.z < height) {
                loc.z = height;
                Vector3f normal = _view.getTerrainNode().getHeightfieldNormal(loc.x, loc.y);
                _linear.negateLocal();
                normal.multLocal(_linear.dot(normal) * 2f);
                normal.subtract(_linear, _linear).multLocal(0.75f);
                _angular.multLocal(0.75f);
            }
            setLocalTranslation(loc);

            // fade it out over time
            float alpha = 1f - (_age / WRECKAGE_LIFESPAN);
            for (Material mat : _mats) {
                if (mat.getParam("Color") != null) {
                    mat.setColor("Color", new ColorRGBA(1f, 1f, 1f, alpha));
                }
            }

            // remove the wreckage once its lifespan has elapsed
            if ((_age += time) > WRECKAGE_LIFESPAN && getParent() != null) {
                getParent().detachChild(this);
            }
        }

        protected float getRandomFloat (float twiceMax)
        {
            return RandomUtil.getFloat(twiceMax) - twiceMax * 0.5f;
        }

        /** The piece's linear and angular velocities. */
        protected Vector3f _linear, _angular;

        /** The loaded model geometries' materials, faded by alpha. */
        protected ArrayList<Material> _mats = new ArrayList<Material>();

        /** Temporary quaternion representing spin. */
        protected Quaternion _spin = new Quaternion();

        /** The piece's age in seconds. */
        protected float _age;
    }

    protected EffectViz _wrapviz;
    protected ParticleEmitter _steamcloud;
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
