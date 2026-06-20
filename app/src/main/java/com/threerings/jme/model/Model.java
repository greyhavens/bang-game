//
// $Id$

package com.threerings.jme.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;

import com.samskivert.util.ObserverList;
import com.samskivert.util.StringUtil;

import com.threerings.jme.util.SpatialVisitor;

import static com.threerings.jme.Log.log;

/**
 * The fork-free, jME3-native app-side facade over a {@code .j3o}-loaded model.
 *
 * <p>jME3 cutover decision (recorded in {@code docs/jme3-cutover-plan.md}): the fork's bespoke
 * model framework ({@code com.jme.*}-coupled {@code Model}/{@code ModelMesh}/{@code SkinMesh})
 * is <b>retired</b>. {@code model.dat} is baked to jME3-native {@code .j3o} at build time by
 * {@code tools/j3o-converter} ({@code ModelConverter}/{@code ModelToJ3o}), and the runtime client
 * loads {@code .j3o} through stock jME3. This {@code Model} is a thin {@link Node} that wraps the
 * loaded {@link Spatial} (its {@link AnimComposer} / {@link com.jme3.anim.SkinningControl} ride
 * along as controls on the content) and re-exposes the exact client-facing surface the
 * sprite/effect framework calls, so {@code ModelCache.getModel(...)} keeps returning a
 * {@code Model} and Phase-2 churn over the model API is localized here rather than spread as a
 * tree-wide {@code Model}-{@literal >}{@code Spatial} rewrite.
 *
 * <p>The converter preserves fork metadata as user data on the loaded root:
 * <ul>
 *   <li>{@code bang.properties} — the model {@code Properties} (one {@code key = value} per line)</li>
 *   <li>{@code bang.animations} — comma-separated animation names</li>
 *   <li>{@code bang.frameRates} — comma-separated {@code name=fps} pairs</li>
 *   <li>{@code bang.loopModes} — comma-separated {@code name=LoopMode} pairs</li>
 *   <li>{@code bang.type} — the model type path</li>
 * </ul>
 * which this facade reads back to reconstruct {@link #getProperties}, {@link #getAnimation},
 * {@link #getVariantNames}, etc.
 *
 * <p><b>Phase boundary.</b> The facade's <em>type surface</em> is the Phase-2 foundation; the
 * playback fidelity behind a few methods (frame-accurate {@link #fastForwardAnimation},
 * pause-resume, the emission-controller {@link CloneCreator} wiring) is finished at the Phase-3
 * host flip / Phase-4 effects port. Where a body cannot be faithfully implemented before the
 * live {@code AssetManager}/host exists, it drives the {@link AnimComposer} where present and is
 * marked accordingly — the <em>shape</em> the fan-out codes against is stable.
 */
public class Model extends Node
{
    /** The supported types of animation in decreasing order of complexity.
     * Retained for client API compatibility; jME3 skinning is uniform (the {@code .j3o} carries
     * a {@code SkinningControl}), so the mode is informational on jME3. */
    public enum AnimationMode {
        SKIN, MORPH, FLIPBOOK
    };

    /** Lets listeners know when animations are completed (which only happens for non-repeating
     * animations) or cancelled. */
    public interface AnimationObserver
    {
        /**
         * Called when an animation has started.
         *
         * @return true to remain on the observer list, false to remove self
         */
        public boolean animationStarted (Model model, String anim);

        /**
         * Called when a non-repeating animation has finished.
         *
         * @return true to remain on the observer list, false to remove self
         */
        public boolean animationCompleted (Model model, String anim);

        /**
         * Called when an animation has been cancelled.
         *
         * @return true to remain on the observer list, false to remove self
         */
        public boolean animationCancelled (Model model, String anim);
    }

    /**
     * A lightweight descriptor of one of the model's animations, re-exposing the fields the
     * client reads off the fork's {@code Model.Animation}: {@link #frameRate} (the sprite code
     * computes per-frame durations as {@code 1f/frameRate}) and {@link #getDuration} (used to
     * schedule shot/move handlers and paths). Backed by the {@code .j3o}'s {@link AnimClip}.
     */
    public static class Animation
    {
        /** The rate of the animation in frames per second. */
        public int frameRate;

        public Animation (String name, int frameRate, float duration)
        {
            this.name = name;
            this.frameRate = frameRate;
            _duration = duration;
        }

        /** Returns this animation's duration in seconds. */
        public float getDuration ()
        {
            return _duration;
        }

        /** Returns this animation's name. */
        public String getName ()
        {
            return name;
        }

        protected final String name;
        protected final float _duration;
    }

    /**
     * Customized clone creator for models. The fork passed this to each controller's
     * {@code putClone}; the emission controllers (effects port) still take it, so the type
     * survives. On jME3, instance creation is a {@link Spatial#clone()} of the content, so the
     * creator mainly carries the shared random seed used to pick consistently among multi-valued
     * (variant) textures.
     */
    public static class CloneCreator
    {
        /** A shared seed used to select textures consistently across an instance's geometries. */
        public int random;

        public CloneCreator (Model toCopy)
        {
            _toCopy = toCopy;
        }

        /** Creates a new copy of the target model. */
        public Model createCopy ()
        {
            random = (int)(Math.random() * Integer.MAX_VALUE);
            return _toCopy.cloneModel(this);
        }

        // The fork CloneCreator carried a per-property "what to share vs. copy" set and an
        // original->copy node map used by ModelMesh.putClone. jME3 cloning handles mesh/buffer
        // sharing itself, so those are no-ops kept only so existing callers compile.
        public void addProperty (String name) { /* no-op on jME3 */ }
        public void removeProperty (String name) { /* no-op on jME3 */ }
        public boolean isSet (String name) { return false; }

        protected final Model _toCopy;
    }

    /**
     * Wraps a freshly {@code .j3o}-loaded {@link Spatial} as a {@code Model} prototype. The
     * loaded content becomes this node's sole child; its {@link AnimComposer}/{@code
     * SkinningControl} controls ride along.
     */
    public Model (Spatial content)
    {
        super(content.getName());
        _content = content;
        attachChild(content);
        readMetadata(content);
    }

    /** No-arg constructor for cloning. */
    protected Model (String name)
    {
        super(name);
    }

    /**
     * Returns a reference to the properties of the model (reconstructed from the
     * {@code bang.properties} user data the converter preserved).
     */
    public Properties getProperties ()
    {
        return _props;
    }

    /**
     * Returns the names of the model's variant configurations.
     */
    public String[] getVariantNames ()
    {
        return StringUtil.parseStringArray(_props.getProperty("variants", ""));
    }

    /**
     * Sets the animation mode to use for this model. Informational on jME3 (skinning is uniform);
     * retained for client API compatibility.
     */
    public void setAnimationMode (AnimationMode mode)
    {
        _animMode = mode;
    }

    /** Returns the animation mode configured for this model. */
    public AnimationMode getAnimationMode ()
    {
        return _animMode;
    }

    /** Returns the names of the model's animations. */
    public String[] getAnimationNames ()
    {
        return _anims.keySet().toArray(new String[_anims.size()]);
    }

    /** Checks whether the model has an animation with the given name. */
    public boolean hasAnimation (String name)
    {
        return _anims.containsKey(name);
    }

    /**
     * Gets the descriptor for the animation with the given name, or {@code null} if there is no
     * such animation.
     */
    public Animation getAnimation (String name)
    {
        Animation anim = _anims.get(name);
        if (anim == null) {
            log.warning("Requested unknown animation [name=" + name + "].");
        }
        return anim;
    }

    /**
     * Returns the descriptor for the currently running animation, or {@code null} if no animation
     * is running.
     */
    public Animation getAnimation ()
    {
        return _anim;
    }

    /**
     * Starts the named animation.
     *
     * @return the duration of the started animation (one cycle for looping animations), or -1 if
     * the animation was not found.
     */
    public float startAnimation (String name)
    {
        Animation anim = getAnimation(name);
        if (anim == null) {
            return -1f;
        }
        if (_anim != null) {
            _animObservers.apply(new AnimCancelledOp(_animName));
        }
        _anim = anim;
        _animName = name;
        _paused = false;
        AnimComposer composer = getComposer();
        if (composer != null && composer.getAnimClip(name) != null) {
            // The fork carried a per-animation repeat type (RT_CLAMP one-shot vs. RT_WRAP/RT_CYCLE
            // loop); the converter preserves it as the bang.loopModes user data. jME3's
            // AnimComposer.setCurrentAction(name) defaults to LoopMode.Loop, so without honouring
            // this every action (including the death/"dying" one-shot) loops forever (BUG-28: the
            // death animation replays until the sprite's _nextAction timer swaps in the dead model).
            // Map DontLoop -> non-looping (the AnimLayer clamps on the final frame and clears the
            // action after one cycle, matching RT_CLAMP); Loop/Cycle stay looping.
            boolean loop = !"DontLoop".equals(_loopModes.get(name));
            composer.setCurrentAction(name, AnimComposer.DEFAULT_LAYER, loop);
        }
        _animObservers.apply(new AnimStartedOp(name));
        return anim.getDuration() / _animSpeed;
    }

    /** Stops the currently running animation. */
    public void stopAnimation ()
    {
        if (_anim == null) {
            return;
        }
        AnimComposer composer = getComposer();
        if (composer != null) {
            composer.removeCurrentAction();
        }
        _paused = false;
        String name = _animName;
        _anim = null;
        _animName = null;
        _animObservers.apply(new AnimCancelledOp(name));
    }

    /** Sets the pause state of the animation. */
    public void pauseAnimation (boolean pause)
    {
        _paused = pause;
        AnimComposer composer = getComposer();
        if (composer != null) {
            composer.setGlobalSpeed(pause ? 0f : _animSpeed);
        }
    }

    /** Returns the pause state of the animation. */
    public boolean isAnimationPaused ()
    {
        return _paused;
    }

    /** Causes the animation to start running in reverse. */
    public void reverseAnimation ()
    {
        _animSpeed = -_animSpeed;
        AnimComposer composer = getComposer();
        if (composer != null) {
            composer.setGlobalSpeed(_paused ? 0f : _animSpeed);
        }
    }

    /** Fast-forwards the current animation by the given number of seconds. */
    public void fastForwardAnimation (float time)
    {
        AnimComposer composer = getComposer();
        if (composer != null && _anim != null) {
            composer.setTime(composer.getTime() + time);
        }
    }

    /**
     * Sets the animation speed, a multiplier on each animation's frame rate.
     */
    public void setAnimationSpeed (float speed)
    {
        _animSpeed = speed;
        AnimComposer composer = getComposer();
        if (composer != null && !_paused) {
            composer.setGlobalSpeed(speed);
        }
    }

    /** Returns the currently configured animation speed. */
    public float getAnimationSpeed ()
    {
        return _animSpeed;
    }

    /** Adds an animation observer. */
    public void addAnimationObserver (AnimationObserver obs)
    {
        _animObservers.add(obs);
    }

    /** Removes an animation observer. */
    public void removeAnimationObserver (AnimationObserver obs)
    {
        _animObservers.remove(obs);
    }

    /**
     * Returns a reference to the node that contains this model's emissions, in world space so the
     * emissions do not move with the model. Created and attached on first call.
     */
    public Node getEmissionNode ()
    {
        if (_emissionNode == null) {
            attachChild(_emissionNode = new Node("emissions"));
        }
        return _emissionNode;
    }

    /**
     * Returns the model's controllers — the procedural / emission controllers the client iterates
     * (the {@code SpriteEmission} subclasses get sprite refs wired here). On jME3 these are app /
     * effect-side {@link Control}s collected from the content; the effects port (Phase 4) populates
     * this list as it rebuilds the emission controllers. Never {@code null}.
     */
    public List<Control> getControllers ()
    {
        return _controllers;
    }

    /** Adds a controller to this model. */
    public void addController (Control control)
    {
        _controllers.add(control);
    }

    /**
     * Re-resolves the model's textures for this instance (variant / colorization), walking the
     * content's geometries and rebinding the material textures the supplied provider returns. The
     * provider is driven by {@code ModelCache} from the {@code ModelTextureResolver}; the candidate
     * texture list is preserved as {@code bang.textures} user data on each geometry.
     */
    public void resolveTextures (final TextureProvider tprov)
    {
        new SpatialVisitor<Geometry>(Geometry.class) {
            protected void visit (Geometry geom) {
                Object names = geom.getUserData("bang.textures");
                if (names == null) {
                    return;
                }
                String first = names.toString().split(",")[0];
                com.jme3.texture.Texture tex = tprov.getTexture(geom, first);
                if (tex != null && geom.getMaterial() != null) {
                    geom.getMaterial().setTexture("DiffuseMap", tex);
                }
            }
        }.traverse(this);
    }

    /**
     * Creates a new prototype using the given variant configuration. Use {@link #createInstance}
     * on the returned prototype to create additional instances of the variant.
     */
    public Model createPrototype (String variant)
    {
        // a variant prototype is a clone whose properties are re-filtered by the variant prefix;
        // the per-mesh texture re-pick is applied via resolveTextures at the ModelCache seam.
        Model prototype = cloneModel(new CloneCreator(this));
        if (variant != null) {
            Properties filtered = new Properties();
            String prefix = variant + ".";
            for (String key : _props.stringPropertyNames()) {
                if (key.startsWith(prefix)) {
                    filtered.setProperty(key.substring(prefix.length()), _props.getProperty(key));
                } else if (!filtered.containsKey(key)) {
                    filtered.setProperty(key, _props.getProperty(key));
                }
            }
            prototype._props = filtered;
        }
        return prototype;
    }

    /** Creates and returns a new instance of this model. */
    public Model createInstance ()
    {
        return new CloneCreator(this).createCopy();
    }

    /**
     * Locks the transforms and bounds of this model in the expectation that it will never be moved
     * from its current position. jME3 has no fork {@code lockMeshes} batching; this is a no-op hook
     * kept for API compatibility (see migration map §2.2 — leave dynamic / use {@code BatchNode}).
     */
    public void lockInstance ()
    {
        // no-op on jME3
    }

    /** Clones this model (used by {@link CloneCreator}). */
    protected Model cloneModel (CloneCreator creator)
    {
        Spatial contentClone = _content.clone();
        Model instance = new Model(getName());
        instance._content = contentClone;
        instance.attachChild(contentClone);
        instance._props = _props;
        instance._anims = _anims;
        instance._animMode = _animMode;
        instance.readMetadata(contentClone);
        return instance;
    }

    /** Reconstructs the facade state from the converter-preserved user data on the content root. */
    protected void readMetadata (Spatial content)
    {
        _props = new Properties();
        Object props = content.getUserData("bang.properties");
        if (props != null) {
            for (String line : props.toString().split("\n")) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    _props.setProperty(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        }

        Map<String, Integer> frameRates = parsePairs(content.getUserData("bang.frameRates"));
        _loopModes = new HashMap<String, String>();
        Object loopModes = content.getUserData("bang.loopModes");
        if (loopModes != null) {
            for (String pair : loopModes.toString().split(",")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    _loopModes.put(pair.substring(0, eq), pair.substring(eq + 1));
                }
            }
        }
        _anims = new HashMap<String, Animation>();
        AnimComposer composer = getComposer(content);
        Object names = content.getUserData("bang.animations");
        if (names != null && composer != null) {
            for (String name : names.toString().split(",")) {
                if (name.isEmpty()) {
                    continue;
                }
                AnimClip clip = composer.getAnimClip(name);
                if (clip == null) {
                    continue;
                }
                int fps = frameRates.getOrDefault(name, 30);
                _anims.put(name, new Animation(name, fps, (float)clip.getLength()));
            }
        }
    }

    /** Parses a comma-separated {@code name=intvalue} user-data string into a map. */
    protected Map<String, Integer> parsePairs (Object data)
    {
        Map<String, Integer> map = new HashMap<String, Integer>();
        if (data != null) {
            for (String pair : data.toString().split(",")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    try {
                        map.put(pair.substring(0, eq), Integer.parseInt(pair.substring(eq + 1)));
                    } catch (NumberFormatException nfe) {
                        // ignore malformed entry
                    }
                }
            }
        }
        return map;
    }

    /** Returns the {@link AnimComposer} driving this model's content, or {@code null}. */
    protected AnimComposer getComposer ()
    {
        return getComposer(_content);
    }

    protected static AnimComposer getComposer (Spatial content)
    {
        return (content == null) ? null : content.getControl(AnimComposer.class);
    }

    /** The loaded {@code .j3o} content this facade wraps. */
    protected Spatial _content;

    /** The model properties (from {@code bang.properties} user data). */
    protected Properties _props = new Properties();

    /** The model animations by name (from {@code bang.animations} + the content's clips). */
    protected Map<String, Animation> _anims = new HashMap<String, Animation>();

    /** Per-animation jME3 loop-mode names (from {@code bang.loopModes}: {@code Loop}, {@code Cycle},
     * or {@code DontLoop}). A {@code DontLoop} entry makes its playback action one-shot. */
    protected Map<String, String> _loopModes = new HashMap<String, String>();

    /** The currently running animation, or {@code null} for none. */
    protected Animation _anim;

    /** The name of the currently running animation, if any. */
    protected String _animName;

    /** The animation mode (informational on jME3). */
    protected AnimationMode _animMode = AnimationMode.SKIN;

    /** The current animation speed multiplier. */
    protected float _animSpeed = 1f;

    /** Whether the animation is paused. */
    protected boolean _paused;

    /** The child node that contains the model's emissions in world space. */
    protected Node _emissionNode;

    /** The procedural / emission controllers attached to the model. */
    protected List<Control> _controllers = new ArrayList<Control>();

    /** Animation completion listeners. */
    protected ObserverList<AnimationObserver> _animObservers = ObserverList.newFastUnsafe();

    /** Notifies observers of animation initiation. */
    protected class AnimStartedOp
        implements ObserverList.ObserverOp<AnimationObserver>
    {
        public AnimStartedOp (String name) { _name = name; }
        public boolean apply (AnimationObserver obs) {
            return obs.animationStarted(Model.this, _name);
        }
        protected String _name;
    }

    /** Notifies observers of animation completion. */
    protected class AnimCompletedOp
        implements ObserverList.ObserverOp<AnimationObserver>
    {
        public AnimCompletedOp (String name) { _name = name; }
        public boolean apply (AnimationObserver obs) {
            return obs.animationCompleted(Model.this, _name);
        }
        protected String _name;
    }

    /** Notifies observers of animation cancellation. */
    protected class AnimCancelledOp
        implements ObserverList.ObserverOp<AnimationObserver>
    {
        public AnimCancelledOp (String name) { _name = name; }
        public boolean apply (AnimationObserver obs) {
            return obs.animationCancelled(Model.this, _name);
        }
        protected String _name;
    }
}
