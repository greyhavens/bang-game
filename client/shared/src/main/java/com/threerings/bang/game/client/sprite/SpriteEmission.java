//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

import com.google.common.collect.Sets;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;

import com.samskivert.util.StringUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.model.TextureProvider;

import com.threerings.bang.util.BasicContext;

import com.threerings.bang.game.client.BoardView;

/**
 * The superclass of emissions whose models may be (but aren't necessarily) associated with
 * {@link PieceSprite}s.
 *
 * <h3>jME3 cutover (Phase 2, cluster 1 — the effects-port seam)</h3>
 *
 * The fork hierarchy was {@code com.jme.scene.Controller} {@literal <-} {@code ModelController}
 * {@literal <-} {@code EmissionController} {@literal <-} {@code SpriteEmission}. The first three
 * lived in app and were fork-coupled ({@code Controller}, the {@code com.jme.util.export} Savable
 * read/write, {@code ModelSpatial.putClone}); on jME3 they stayed only in the build-time
 * {@code modeltool} source set and are <b>off the runtime classpath</b>. So {@code SpriteEmission}
 * is re-rooted here as a fork-free jME3 {@link AbstractControl} that absorbs the small slice of the
 * {@code ModelController}/{@code EmissionController} surface the client emissions actually use:
 * {@link #configure}, {@link #getTarget}, {@link #resolveTextures}, {@link #init},
 * {@link #shouldAccumulate}, {@link #putClone}, {@link #setActive}/{@link #isActive}, the
 * animation-frame hooks, and {@code hide_target}.
 *
 * <p>The fork Savable {@code read(JMEImporter)}/{@code write(JMEExporter)} bodies are <b>dropped</b>:
 * the emission controllers are no longer serialized into the model (the converter detects and
 * defers them — {@code docs/jme3-model-loader.md} §3.3); the effects port reconstructs them
 * client-side, so their state is set via {@link #configure}/{@link #putClone}, not deserialized.
 *
 * <p>{@code putClone} now takes a jME3 {@link Control} (not the fork {@code Controller}) plus the
 * facade {@link Model.CloneCreator}; {@code resolveTextures} takes the fork-free facade
 * {@link TextureProvider} ({@code Texture getTexture(Geometry, name)}).
 */
public abstract class SpriteEmission extends AbstractControl
{
    /**
     * Configures this emission from its model (sub-)properties and controller target.
     */
    public void configure (Properties props, Spatial target)
    {
        _target = target;
        String[] anims = StringUtil.parseStringArray(
            props.getProperty("animations", ""));
        if (anims.length > 0) {
            _animations = Sets.newHashSet();
            Collections.addAll(_animations, anims);
        }
        _hideTarget = Boolean.parseBoolean(props.getProperty("hide_target", "true"));
    }

    /**
     * Returns a reference to the controller's target.
     */
    public Spatial getTarget ()
    {
        return _target;
    }

    /**
     * Resolves any textures required by the emission (effects port; jME3 facade
     * {@link TextureProvider}).
     */
    public void resolveTextures (TextureProvider tprov)
    {
    }

    /**
     * Initializes this emission against its model: registers the animation observer and hides the
     * emitter target if so configured.
     */
    public void init (Model model)
    {
        model.addAnimationObserver(_animobs);
        if (_animations != null) {
            setActive(false);
        }
        if (_hideTarget && _target != null) {
            _target.setCullHint(CullHint.Always);
        }
    }

    /**
     * Determines whether the emission's updates should include time accumulated while the model
     * was out of sight.
     */
    public boolean shouldAccumulate ()
    {
        return true;
    }

    /**
     * Creates or populates and returns a clone of this emission.
     *
     * @param store an instance of this class to populate, or {@code null} to create a new instance.
     */
    public Control putClone (Control store, Model.CloneCreator properties)
    {
        if (store == null) {
            return null;
        }
        SpriteEmission estore = (SpriteEmission)store;
        estore._animations = _animations;
        estore._hideTarget = _hideTarget;
        estore._active = _active;
        return estore;
    }

    /**
     * Provides the emission with relevant Bang references.
     */
    public void setSpriteRefs (
        BasicContext ctx, BoardView view, PieceSprite sprite)
    {
        _ctx = ctx;
        _view = view;
        _sprite = sprite;
    }

    /**
     * Sets whether to use this emission during animation.
     */
    public void setActiveEmission (boolean active)
    {
        _active = active;
    }

    /**
     * Returns the active state for emission use.
     */
    public boolean isActiveEmission ()
    {
        return _active;
    }

    /**
     * Sets whether this emission is active (fork {@code Controller.setActive}; delegates to the
     * control's enabled flag).
     */
    public void setActive (boolean active)
    {
        setEnabled(active);
    }

    /**
     * Returns whether this emission is active.
     */
    public boolean isActive ()
    {
        return isEnabled();
    }

    /**
     * Advances this emission (fork {@code Controller.update(float)}); subclasses override.
     */
    public void update (float time)
    {
    }

    @Override // documentation inherited
    protected void controlUpdate (float tpf)
    {
        update(tpf);
    }

    @Override // documentation inherited
    protected void controlRender (RenderManager rm, ViewPort vp)
    {
        // no per-render work
    }

    /**
     * Called when an animation is started on the model.
     */
    protected void animationStarted (String anim)
    {
        if (_animations != null && _animations.contains(anim)) {
            setActive(true);
        }
    }

    /**
     * Called when an animation is stopped on the model.
     */
    protected void animationStopped (String anim)
    {
        if (_animations != null) {
            setActive(false);
        }
    }

    /** The target to control (the emitter node). */
    protected Spatial _target;

    /** The animations for which this emission is active, or {@code null} for all of them. */
    protected HashSet<String> _animations;

    /** Whether the emitter target should be hidden from view. */
    protected boolean _hideTarget = true;

    /** The application context, or <code>null</code> for none. */
    protected BasicContext _ctx;

    /** The board view containing the piece sprite, or <code>null</code> for none. */
    protected BoardView _view;

    /** The piece sprite that loaded the model, or <code>null</code> for none. */
    protected PieceSprite _sprite;

    /** The (emission-use) active state. */
    protected boolean _active = true;

    /** Listens to the model's animation state. */
    protected Model.AnimationObserver _animobs =
        new Model.AnimationObserver() {
        public boolean animationStarted (Model model, String anim) {
            SpriteEmission.this.animationStarted(anim);
            return true;
        }
        public boolean animationCompleted (Model model, String anim) {
            animationStopped(anim);
            return true;
        }
        public boolean animationCancelled (Model model, String anim) {
            animationStopped(anim);
            return true;
        }
    };
}
