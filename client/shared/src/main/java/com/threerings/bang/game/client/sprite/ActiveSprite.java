//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.ArrayList;
import java.util.HashMap;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.state.RenderState;

import com.samskivert.util.ObserverList;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.jme.model.Model;
import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Sprite;
import com.threerings.jme.sprite.SpriteObserver;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays some sort of animated entity on the board.
 */
public class ActiveSprite extends PieceSprite
{
    /** Used to notify observers of completed animation actions. */
    public interface ActionObserver extends SpriteObserver
    {
        /** Called when an action has been completed by this sprite. */
        public void actionCompleted (Sprite sprite, String action);
    }

    /** Used to handle custom sprite actions. */
    public interface ActionHandler
    {
        /** 
         * Called to handle the specified action.
         * 
         * @return null if the action is not handled, or the name used to
         * pass into setAction
         */
        public String handleAction (ActiveSprite sprite, String action);
    }

    /** A fake action that is queued up to indicate that this sprite
     * should switch to its dead model when all other actions are complete. */
    public static final String DEAD = "__dead__";
    
    /** A fake action that is queued up to indicate that this sprite
     * should be removed when all other actions are completed. */
    public static final String REMOVED = "__removed__";

    /**
     * Creates a mobile sprite with the specified model type and name.
     */
    public ActiveSprite (String type, String name)
    {
        _type = type;
        _name = name;
        addProceduralActions();
    }
    
    /**
     * Returns an array containing the types of wreckage to be thrown from this
     * sprite when it blows up.
     */
    public String[] getWreckageTypes ()
    {
        return _wtypes;
    }    

    /**
     * Returns true if this sprite supports the specified action
     * animation.
     */
    public boolean hasAction (String action)
    {
        return _procActions.containsKey(action) ||
            (_model != null && _model.hasAnimation(action));
    }

    /**
     * Returns the underlying animation for the specified action (if there is
     * one).
     */
    public Model.Animation getAction (String action)
    {
        return (_model == null) ? null : _model.getAnimation(action);
    }

    /**
     * Runs the specified action animation.
     */
    public void queueAction (String action)
    {
        // log.info("Queueing action " + action + " on " + _piece + ".");
        _actions.add(action);
        if (_action == null && !isMoving()) {
            startNextAction();
        }
    }

    /**
     * Returns true if this sprite is currently displaying an action
     * animation.
     */
    public boolean isAnimating ()
    {
        // this might be called between clearing _action and starting our
        // next action, so check both _action and _actions.size()
        return (_action != null) || (_actions.size() > 0);
    }

    @Override // documentation inherited
    public boolean hasTooltip ()
    {
        return true;
    }

    /**
     * Tells the sprite if we're performing a complex action.
     */
    public void startComplexAction ()
    {
        if (_complexAction == ComplexAction.NONE) {
            _complexAction = ComplexAction.ACTIVE;
        }
    }

    /**
     * Tells the sprite we finished a complex action.
     */
    public void stopComplexAction ()
    {
        if (_complexAction == ComplexAction.ACTIVE) {
            _complexAction = ComplexAction.OVER;
        }
    }

    @Override // documentation inherited
    public void updated (Piece piece, short tick)
    {
        super.updated(piece, tick);
        
        // if we were dead but are once again alive, switch back to our rest
        // pose
        if (_dead && piece.isAlive()) {
            log.info("Resurrected " + piece);
            loadModel(_type, _name, _variant);
            _dead = false;
        }
    }
    
    @Override // documentation inherited
    public void updateWorldData (float time)
    {
        // wait until we're done moving to do any actions or idle animations
        if (isMoving() || _complexAction == ComplexAction.ACTIVE) {
            super.updateWorldData(time);
            return;
        }
        
        // expire any actions first before we update our children
        if (_nextAction > 0 || _complexAction == ComplexAction.OVER) {
            _nextAction -= time;
            if (_nextAction <= 0 || _complexAction == ComplexAction.OVER) {
                String action = _action;
                _nextAction = 0;
                _action = null;
                _complexAction = ComplexAction.NONE;

                // report that we completed this action
                if (_observers != null) {
                    _observers.apply(new CompletedOp(this, action));
                }

                // if we were removed, don't bother updating the action
                if (getParent() == null) {
                    return;
                }
                
                // start the next action if we have one, otherwise idle
                startNext();
            }
            
        } else if (_nextIdle > 0) {
            _nextIdle -= time;
            if (_nextIdle <= 0) {
                startNextIdle(false);
            }
        }

        super.updateWorldData(time);
    }

    /** 
     * Adds an action handler to hadle specific actions. 
     */
    public void addActionHandler (ActionHandler handler)
    {
        if (handler != null) {
            if (_actionHandlers.contains(handler)) {
                log.warning("Attempting to add duplicate ActionHandler", "handler", handler);
                return;
            }
            _actionHandlers.add(handler);
        }
    }

    /** 
     * Removes an action handler.
     */
    public boolean removeActionHandler (ActionHandler handler)
    {
        return _actionHandlers.remove(handler);
    }
    
    /**
     * Called when our piece is removed from the board state.
     *
     * @return false if the sprite may be removed immediately; true if it
     * should be removed after it has completed its current set of actions
     */
    public boolean removed ()
    {
        return isAnimating();
    }
    
    /**
     * Adds any procedural actions for this sprite.
     */
    protected void addProceduralActions ()
    {
        _procActions.put(DEAD, new ProceduralAction() {
            public float activate () {
                String oname = _name;
                loadModel(_type, getDeadModel(), _variant);
                _name = oname;
                _dead = true;
                return FastMath.FLT_EPSILON;
            }
        });
        _procActions.put(REMOVED, new ProceduralAction() {
            public float activate () {
                // have the unit sink into the ground and fade away
                startRiseFade(-getRemoveDepth(), false, REMOVAL_DURATION);
                return REMOVAL_DURATION;
            }
        });
    }
    
    /**
     * Returns the depth to which the model sinks when faded out and removed.
     */
    protected float getRemoveDepth ()
    {
        return TILE_SIZE * 0.5f;
    }
    
    /**
     * Returns the name of the sprite's dead model (not including the type,
     * which is assumed to be the same as the current type).
     */
    protected String getDeadModel ()
    {
        return _name + "/dead";
    }
    
    /**
     * Raises or lowers the sprite while fading it in or out.
     */
    protected void startRiseFade (
        float height, final boolean in, final float duration)
    {
        setRenderState(RenderUtil.blendAlpha);
        if (_mstate == null) {
            _mstate = _ctx.getRenderer().createMaterialState();
            _mstate.setAmbient(ColorRGBA.white);
            _mstate.setDiffuse(ColorRGBA.white);
            setRenderState(_mstate);
        }
        updateRenderState();

        Vector3f tin = new Vector3f(localTranslation),
            tout = localRotation.mult(Vector3f.UNIT_Z);
        tout.multLocal(height).addLocal(localTranslation);
        move(new LinePath(this, in ? tout : tin, in ? tin : tout,
            duration) {
            public void update (float time) {
                super.update(time);
                float a = Math.min(Math.max(in ? (_accum / _duration) :
                    (1f - _accum / _duration), 0f), 1f);
                _mstate.getDiffuse().a = a;
                if (_shadow != null) {
                    _shadow.getBatch(0).getDefaultColor().a = a;
                }
            }
            public void wasRemoved () {
                super.wasRemoved();
                if (in) {
                    clearRenderState(RenderState.RS_ALPHA);
                    if (!isShadowable()) {
                        clearRenderState(RenderState.RS_MATERIAL);
                        _mstate = null;
                    }
                    updateRenderState();
                }
            }
        });
    }
    
    @Override // documentation inherited
    protected void createGeometry ()
    {
        super.createGeometry();

        // load our model
        loadModel(_type, _name, _variant);
    }
    
    @Override // documentation inherited
    protected void modelLoaded (Model model)
    {
        super.modelLoaded(model);
        _wtypes = StringUtil.parseStringArray(
            _model.getProperties().getProperty("wreckage", ""));
        _ianims = StringUtil.parseStringArray(
            _model.getProperties().getProperty("idle", ""));
        startNextIdle(true);
    }

    /**
     * Activates one of the model animations.
     *
     * @return the duration of the animation (for looping animations,
     * the duration of one cycle), or -1 if the action was not found
     */
    protected float setAction (String action)
    {
        ProceduralAction paction = _procActions.get(action);
        if (paction != null) {
            return paction.activate();
        }
        return (_model == null || _dead) ?
            -1f : _model.startAnimation(action);
    }
    
    /**
     * Starts the next action or idle animation.
     */
    protected void startNext ()
    {
        if (_action != null) {
            return; // already acting
        } else if (_actions.size() > 0) {
            startNextAction();
        } else {
            startNextIdle(true);
        }
    }

    /**
     * Pulls the next action off of our queue and runs it.
     */
    protected void startNextAction ()
    {
        _action = _actions.remove(0);
        String action = null;
        for (ActionHandler handler : _actionHandlers) {
            if ((action = handler.handleAction(this, _action)) != null) {
                _action = action;
                break;
            }
        }
        // cope when we have units disabled for debugging purposes
        if ((_nextAction = setAction(_action)) < 0f) {
            _nextAction = 0.5f;
        }
    }
    
    /**
     * Starts the next idle animation (if any) and schedules the one after
     * that.
     *
     * @param offset if true, start at a random offset into the animation to
     * make sure idle animations aren't synchronized between units
     */
    protected void startNextIdle (boolean offset)
    {
        // if there's one idle animation, assume that it loops; if there
        // are many, cycle randomly between them
        float duration = -1f;
        String[] ianims = getIdleAnimations();
        if (ianims == null || ianims.length == 0) {
            if (_model != null) {
                _model.stopAnimation();
            }
            _idle = null;
            _nextIdle = Float.MAX_VALUE;
            
        } else if (ianims.length == 1) {
            duration = setAction(_idle = ianims[0]);
            _nextIdle = Float.MAX_VALUE;
            
        } else if (ianims.length > 1) {
            _idle = RandomUtil.pickRandom(ianims, _idle);
            duration = _nextIdle = setAction(_idle);
        }
        if (duration > 0f && offset) {
            float time = RandomUtil.getFloat(duration);
            _nextIdle -= time;
            _model.fastForwardAnimation(time);
        }
        
        // if we are in low detail mode, stop the idle animation at the first frame
        if (!BangPrefs.isMediumDetail() && _idle != null) {
            _model.updateGeometricState(0f, false);
            _model.stopAnimation();
            _nextIdle = Float.MAX_VALUE;
        }
    }
    
    /**
     * Returns the array of idle animations (which by default are those
     * specified by the <code>idle</code> model property).
     */
    protected String[] getIdleAnimations ()
    {
        return _ianims;
    }
    
    /** Used to dispatch {@link ActionObserver#actionCompleted}. */
    protected static class CompletedOp
        implements ObserverList.ObserverOp<SpriteObserver>
    {
        public CompletedOp (Sprite sprite, String action) {
            _sprite = sprite;
            _action = action;
        }

        public boolean apply (SpriteObserver observer) {
            if (observer instanceof ActionObserver) {
                ((ActionObserver)observer).actionCompleted(_sprite, _action);
            }
            return true;
        }

        protected Sprite _sprite;
        protected String _action;
    }

    protected String[] _wtypes;
    protected String[] _ianims;

    protected String _action, _idle;
    protected float _nextAction, _nextIdle;
    protected ArrayList<String> _actions = new ArrayList<String>();
    protected ArrayList<ActionHandler> _actionHandlers = 
        new ArrayList<ActionHandler>();
    protected HashMap<String, ProceduralAction> _procActions =
        new HashMap<String, ProceduralAction>();
    
    /** Whether or not we have switched to the dead model. */
    protected boolean _dead;

    /** Whether we are in a complex action state. */
    protected ComplexAction _complexAction = ComplexAction.NONE;
    
    /** Represents an action defined in code, as opposed to the ones that
     * correspond to model animations. */
    protected interface ProceduralAction
    {
        /**
         * Starts this action.
         *
         * @return the duration of the action in seconds
         */
        public float activate ();
    }
    
    /** The number of seconds it takes dead pieces to fade out. */
    protected static final float REMOVAL_DURATION = 2f;
        
    /** Complex action states. */
    protected static enum ComplexAction { NONE, ACTIVE, OVER };
}
