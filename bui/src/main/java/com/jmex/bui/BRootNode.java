//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;

import com.jmex.bui.Log;
import com.jmex.bui.backend.BackendProvider;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.EventListener;
import com.jmex.bui.event.FocusEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.layout.BorderLayout;

/**
 * Connects the BUI system into the rendering host.
 *
 * <p> Since the jME3 cutover (Phase 1) this is no longer a scene-graph node. The host drives
 * BUI by (a) calling {@link #updateRootState} once per frame (tooltip/timer logic + geom-view
 * updates) and (b) calling {@link #renderWindows} on the render thread, bracketed by the
 * backend's {@code beginFrame}/{@code endFrame} (see {@code docs/jme3-bui-host.md}). The
 * concrete jME3 root node ({@code Jme3RootNode}) and its input path arrive in Phase 3.
 */
public abstract class BRootNode
{
    public BRootNode ()
    {
    }

    /**
     * Returns the current timestamp used to stamp event times.
     */
    public abstract long getTickStamp ();

    /**
     * Returns the host's jME3 {@link RenderManager}, or null if this root node has no live render
     * host (e.g. a headless/test node). {@link BGeomView} uses this to create the per-component
     * {@code ViewPort} that renders embedded 3D geometry. The default returns null; the concrete
     * jME3 root node ({@code Jme3RootNode}) overrides it.
     */
    public RenderManager getRenderManager ()
    {
        return null;
    }

    /**
     * Returns the current display (framebuffer) width in pixels, read from the installed backend.
     */
    public int getDisplayWidth ()
    {
        return BackendProvider.get().getDisplayWidth();
    }

    /**
     * Returns the current display (framebuffer) height in pixels, read from the installed backend.
     */
    public int getDisplayHeight ()
    {
        return BackendProvider.get().getDisplayHeight();
    }

    /**
     * Notifies the root that the host display (framebuffer) has been resized to the supplied
     * pixel dimensions. The host ({@code JmeApp.reshape}) calls this on the render thread once the
     * jME3 viewport cameras have been re-fit to the new size, having already updated the backend's
     * recorded display size (so {@code BackendProvider.get().getDisplayWidth/Height} reports the
     * new size synchronously here).
     *
     * <p>The fork drove this from a single {@code DisplaySystem}-recorded size: new windows,
     * popups and tooltips lay out from that size (so they pick the new size up automatically), and
     * the fork's {@code OptionsView} re-bounded the active full-screen view explicitly. Here we
     * mirror that: tooltips/popups already read the (now-updated) backend size, and every existing
     * top-level window is invalidated so its layout re-runs against the new size on the next tick
     * (re-anchoring e.g. a bottom-right-anchored button inside a window that is itself re-bounded
     * to the new size by the host). Re-binding the full-screen views to the new bounds is left to
     * the host's resize listener (which knows which views fill the screen vs. which are centered).
     */
    public void displayResized (int width, int height)
    {
        for (int ii = 0, ll = _windows.size(); ii < ll; ii++) {
            _windows.get(ii).invalidate();
        }
    }

    /**
     * Registers a top-level window with the input system.
     */
    public void addWindow (BWindow window)
    {
        addWindow(window, false);
    }

    /**
     * Registers a top-level window with the input system.
     *
     * @param topLayer if true, will set the window layer to the top most layer if it's current
     * layer is less than that.
     */
    public void addWindow (BWindow window, boolean topLayer)
    {
        // make a note of the current top window
        BWindow curtop = null;
        if (_windows.size() > 0) {
            curtop = _windows.get(_windows.size()-1);
        }

        if (topLayer && curtop != null) {
            window.setLayer(Math.max(window.getLayer(), curtop.getLayer()));
        }

        // add this window into the stack and resort
        _windows.add(window);
        resortWindows();

        // if this window is now the top window, we need to transfer the focus to it (and save the
        // previous top window's focus)
        BComponent pendfocus = null;
        if (_windows.get(_windows.size()-1) == window && !window.isOverlay()) {
            // store the previous top window's focus and clear it
            if (_focus != null && curtop != null) {
                curtop._savedFocus = _focus;
                setFocus(null);
            }
            // make a note of the window's previous saved focus
            pendfocus = window._savedFocus;
            window._savedFocus = null;
        }

        // add this window to the hierarchy (which may set a new focus)
        window.setRootNode(this);

        // if no new focus was set when we added the window, give the focus to the previously
        // pending focus component
        if (_focus == null && pendfocus != null) {
            setFocus(pendfocus);
        }

        // recompute the hover component; the window may be under the mouse
        updateHoverComponent(_mouseX, _mouseY);
    }

    /**
     * Returns true if the specified window is on top.
     */
    public boolean isOnTop (BWindow window)
    {
        return (_windows.size() > 0 &&
                _windows.get(_windows.size()-1) == window);
    }

    /**
     * Called when an added window's layer is changed. Adjusts the ordering of the windows in the
     * stack.
     */
    public void resortWindows ()
    {
        Collections.sort(_windows);
    }

    /**
     * Removes all windows from the root node.
     */
    public void removeAllWindows ()
    {
        setFocus(null);
        for (int ii = _windows.size() - 1; ii >= 0; ii--) {
            BWindow window = _windows.remove(ii);
            // remove the window from the interface heirarchy
            window.setRootNode(null);
        }

        // then remove the hover component (which may result in a mouse exited even being
        // dispatched to the window or one of its children)
        updateHoverComponent(_mouseX, _mouseY);
    }

    /**
     * Removes a window from participation in the input system.
     */
    public void removeWindow (BWindow window)
    {
        // if our focus is in this window, clear it
        if (_focus != null && _focus.getWindow() == window) {
            setFocus(null);
        }

        // clear any saved focus reference
        window._savedFocus = null;

        // first remove the window from our list
        if (!_windows.remove(window)) {
            Log.log.warning("Requested to remove unmanaged window [window=" + window + "].");
            Thread.dumpStack();
            return;
        }

        // then remove the hover component (which may result in a mouse exited even being
        // dispatched to the window or one of its children)
        updateHoverComponent(_mouseX, _mouseY);

        // remove the window from the interface heirarchy
        window.setRootNode(null);

        // remove any associated popup windows
        for (BWindow bwindow : _windows.toArray(new BWindow[_windows.size()])) {
            if (bwindow.getParentWindow() == window) {
                removeWindow(bwindow);
            }
        }

        // finally restore the focus to the new top-most window if it has a saved focus
        if (_windows.size() > 0) {
            BWindow top = _windows.get(_windows.size()-1);
            top.gotFocus();
        }
    }

    /**
     * Configures the number of seconds that the mouse must rest over a component to trigger a
     * tooltip. If the value is set to zero, tips will appear immediately.
     */
    public void setTooltipTimeout (float seconds)
    {
        _tipTime = seconds;
    }

    /**
     * Returns the tool tip timeout. See {@link #setTooltipTimeout} for details.
     */
    public float getTooltipTimeout ()
    {
        return _tipTime;
    }

    /**
     * Sets the preferred width of tooltip windows. The default is to prefer a width slightly less
     * wide that the entire window.
     */
    public void setTooltipPreferredWidth (int width)
    {
        _tipWidth = width;
    }

    /**
     * Registers a listener that will be notified of all events prior to their being dispatched
     * normally.
     */
    public void addGlobalEventListener (EventListener listener)
    {
        _globals.add(listener);
    }

    /**
     * Removes a global event listener registration.
     */
    public void removeGlobalEventListener (EventListener listener)
    {
        _globals.remove(listener);
    }

    /**
     * This is called by a window or a scroll pane when it has become invalid.  The root node
     * should schedule a revalidation of this component on the next tick or the next time an event
     * is processed.
     */
    public abstract void rootInvalidated (BComponent root);

    /**
     * Configures a component to receive all events that are not sent to some other component. When
     * an event is not consumed during normal processing, it is sent to the default event targets,
     * most recently registered to least recently registered.
     */
    public void pushDefaultEventTarget (BComponent component)
    {
        _defaults.add(component);
    }

    /**
     * Pops the default event target off the stack.
     */
    public void popDefaultEventTarget (BComponent component)
    {
        _defaults.remove(component);
    }

    /**
     * Requests that the specified component be given the input focus.  Pass null to clear the
     * focus.
     */
    public void requestFocus (BComponent component)
    {
        setFocus(component);
    }

    /**
     * Returns the component that currently has the focus, or null.
     */
    public BComponent getFocus ()
    {
        return _focus;
    }

    /**
     * Returns the total number of windows added to this node.
     */
    public int getWindowCount ()
    {
        return _windows.size();
    }

    /**
     * Returns the window at the specified index.
     */
    public BWindow getWindow (int index)
    {
        return _windows.get(index);
    }

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        return "BRootNode@" + hashCode();
    }

    /**
     * A large component that changes its tooltip while it is the hover component in the normal
     * course of events can call this method to force an update to the tooltip window.
     */
    public void tipTextChanged (BComponent component)
    {
        if (component == _hcomponent) {
            clearTipWindow();
        }
    }

    /**
     * Drives per-frame BUI logic: geometry-view updates plus tooltip timing. The host calls
     * this once per frame with the elapsed time. (Replaces the fork scene node's
     * {@code updateGeometricState} hook.)
     */
    public void updateRootState (float time)
    {
        // update our geometry views if we have any
        for (int ii = 0, ll = _geomviews.size(); ii < ll; ii++) {
            _geomviews.get(ii).update(time);
        }

        // check to see if we need to pop up a tooltip
        _lastMoveTime += time;
        _lastTipTime += time;
        String tiptext;
        if (_hcomponent == null || _tipwin != null ||
            (_lastMoveTime < getTooltipTimeout() &&
             _lastTipTime > TIP_MODE_RESET) ||
            (tiptext = _hcomponent.getTooltipText()) == null) {
            if (_tipwin != null) {
                _lastTipTime = 0;
            }
            return;
        }

        // make sure the hover component is in a window and wants a tooltip
        BWindow hwin = _hcomponent.getWindow();
        BComponent tcomp = _hcomponent.createTooltipComponent(tiptext);
        if (hwin == null || tcomp == null) {
            return;
        }

        // create, set up and show the tooltip window
        _tipwin = new BWindow(hwin.getStyleSheet(), new BorderLayout()) {
            public boolean isOverlay () {
                return true; // don't steal input focus
            }
        };
        _tipwin.setLayer(Integer.MAX_VALUE/2);
        _tipwin.setStyleClass("tooltip_window");
        _tipwin.add(tcomp, BorderLayout.CENTER);
        addWindow(_tipwin);

        // it's possible that adding the tip window will cause it to immediately be removed, so
        // make sure we don't NPE
        if (_tipwin == null) {
            return;
        }

        // if it's still here, lay it out
        int width = BackendProvider.get().getDisplayWidth();
        int height = BackendProvider.get().getDisplayHeight();
        _tipwin.pack(_tipWidth == -1 ? width-10 : _tipWidth, height-10);
        int tx = 0, ty = 0;
        if (_hcomponent.isTooltipRelativeToMouse()) {
            tx = _mouseX - _tipwin.getWidth()/2;
            ty = _mouseY + 10;
        } else {
            tx = _hcomponent.getAbsoluteX() +
                (_hcomponent.getWidth() - _tipwin.getWidth()) / 2;
            ty = _hcomponent.getAbsoluteY() + _hcomponent.getHeight() + 10;
        }
        tx = Math.max(5, Math.min(tx, width-_tipwin.getWidth()-5));
        ty = Math.min(ty, height- _tipwin.getHeight() - 5);
        _tipwin.setLocation(tx, ty);
        // we need to validate here because we're adding a window in the middle of our normal frame
        // processing
        _tipwin.validate();
    }

    /**
     * Renders all registered windows, back-to-front, through the installed
     * {@link BackendProvider} backend. The host calls this on the render thread, bracketed by
     * the backend's {@code beginFrame}/{@code endFrame}. The {@code RenderManager} argument is
     * a pass-through token threaded into BUI's render tree; the jME3 backend renders through
     * its own render manager and ignores it.
     */
    public void renderWindows (RenderManager renderer)
    {
        BWindow modalWin = null;
        if (_modalShade != null) {
            for (int ii = _windows.size() - 1; ii >= 0; ii--) {
                BWindow win = _windows.get(ii);
                if (win.shouldShadeBehind()) {
                    modalWin = win;
                    break;
                }
            }
        }

        // render all of our windows
        for (int ii = 0, ll = _windows.size(); ii < ll; ii++) {
            BWindow win = _windows.get(ii);
            try {
                if (win == modalWin) {
                    renderModalShade();
                }
                win.render(renderer);
            } catch (Throwable t) {
                Log.log.log(Level.WARNING, win + " failed in render()", t);
            }
        }
    }

    /**
     * Sets the color of the shade behind the first active modal window.
     */
    public void setModalShade (ColorRGBA color)
    {
        _modalShade = color;
    }

    /**
     * Dispatches an event to the specified target (which may be null). If the target is null, or
     * did not consume the event, it will be passed on to the most recently opened modal window if
     * one exists (and the supplied target component was not a child of that window) and then to
     * the default event targets if the event is still unconsumed.
     *
     * @return true if the event was dispatched, false if not.
     */
    protected boolean dispatchEvent (BComponent target, BEvent event)
    {
        // notify our global listeners if we have any
        for (int ii = 0, ll = _globals.size(); ii < ll; ii++) {
            try {
                _globals.get(ii).eventDispatched(event);
            } catch (Exception e) {
                Log.log.log(Level.WARNING, "Global event listener choked " +
                            "[listener=" + _globals.get(ii) + "].", e);
            }
        }

        // first try the "natural" target of the event if there is one
        BWindow sentwin = null;
        if (target != null) {
            if (target.dispatchEvent(event)) {
                return true;
            }
            sentwin = target.getWindow();
        }

        // next try the most recently opened modal window, if we have one
        for (int ii = _windows.size()-1; ii >= 0; ii--) {
            BWindow window = _windows.get(ii);
            if (window.isModal()) {
                if (window != sentwin) {
                    if (window.dispatchEvent(event)) {
                        return true;
                    }
                }
                break;
            }
        }

        // finally try the default event targets
        for (int ii = _defaults.size()-1; ii >= 0; ii--) {
            BComponent deftarg = _defaults.get(ii);
            if (deftarg.dispatchEvent(event)) {
                return true;
            }
        }

        // let our caller know that the event was not dispatched by anyone
        return false;
    }

    /**
     * Configures the component that has keyboard focus.
     */
    protected void setFocus (BComponent focus)
    {
        // allow the component we clicked on to adjust the focus target
        if (focus != null) {
            focus = focus.getFocusTarget();
        }

        // if the focus is changing, dispatch an event to report it
        if (_focus != focus) {
            if (_focus != null) {
                _focus.dispatchEvent(new FocusEvent(this, getTickStamp(), FocusEvent.FOCUS_LOST));
            }
            _focus = focus;
            if (_focus != null) {
                _focus.dispatchEvent(new FocusEvent(this, getTickStamp(), FocusEvent.FOCUS_GAINED));
            }
        }
    }

    /**
     * Registers a {@link BGeomView} with the root node. This is called automatically from {@link
     * BGeomView#wasAdded}.
     */
    protected void registerGeomView (BGeomView nview)
    {
        _geomviews.add(nview);
    }

    /**
     * Clears out a node view registration. This is called automatically from {@link
     * BGeomView#wasRemoved}.
     */
    protected void unregisterGeomView (BGeomView nview)
    {
        _geomviews.remove(nview);
    }

    /**
     * Called by a window when its position changes. This triggers a recomputation of the hover
     * component as the window may have moved out from under or under the mouse.
     */
    protected void windowDidMove (BWindow window)
    {
        updateHoverComponent(_mouseX, _mouseY);
    }

    protected void mouseDidMove (int mouseX, int mouseY)
    {
        // update some tracking bits
        _mouseX = mouseX;
        _mouseY = mouseY;

        // calculate our new hover component
        updateHoverComponent(_mouseX, _mouseY);

        if (_tipwin == null) {
            _lastMoveTime = 0;
        }
    }

    /**
     * Recomputes the component over which the mouse is hovering, generating mouse exit and entry
     * events as necessary.
     */
    protected void updateHoverComponent (int mx, int my)
    {
        // check for a new hover component starting with each of our root components
        BComponent nhcomponent = null;
        for (int ii = _windows.size()-1; ii >= 0; ii--) {
            BWindow comp = _windows.get(ii);
            nhcomponent = comp.getHitComponent(mx, my);
            if (nhcomponent != null && nhcomponent.getWindow() != _tipwin) {
                break;
            }
            // if this window is modal, stop here
            if (comp.isModal()) {
                break;
            }
        }

        // generate any necessary mouse entry or exit events
        if (_hcomponent != nhcomponent) {
            // inform the previous component that the mouse has exited
            if (_hcomponent != null) {
                _hcomponent.dispatchEvent(new MouseEvent(this, getTickStamp(), _modifiers,
                                                         MouseEvent.MOUSE_EXITED, mx, my));
            }
            // inform the new component that the mouse has entered
            if (nhcomponent != null) {
                nhcomponent.dispatchEvent(new MouseEvent(this, getTickStamp(), _modifiers,
                                                         MouseEvent.MOUSE_ENTERED, mx, my));
            }
            _hcomponent = nhcomponent;

            // clear out any tooltip business in case the hover component changed as a result of a
            // window popping up
            if (_hcomponent == null || _hcomponent.getWindow() != _tipwin) {
                clearTipWindow();
            }
        }
    }

    protected void clearTipWindow ()
    {
        _lastMoveTime = 0;
        if (_tipwin != null) {
            BWindow tipwin = _tipwin;
            _tipwin = null;
            // this might trigger a recursive call to clearTipWindow
            removeWindow(tipwin);
        }
    }

    protected void renderModalShade ()
    {
        BComponent.applyDefaultStates();
        BackendProvider.get().applyBlendState();

        int width = BackendProvider.get().getDisplayWidth();
        int height = BackendProvider.get().getDisplayHeight();

        BackendProvider.get().fillRect(0, 0, width, height,
            _modalShade.r, _modalShade.g, _modalShade.b, _modalShade.a);
    }

    protected int _modifiers;
    protected int _mouseX, _mouseY;

    protected BWindow _tipwin;
    protected float _lastMoveTime, _tipTime = 1f, _lastTipTime;
    protected int _tipWidth = -1;
    protected ColorRGBA _modalShade;

    protected ArrayList<BWindow> _windows = new ArrayList<BWindow>();
    protected BComponent _hcomponent, _ccomponent;
    protected BComponent _focus;
    protected ArrayList<BComponent> _defaults = new ArrayList<BComponent>();
    protected ArrayList<BGeomView> _geomviews = new ArrayList<BGeomView>();
    protected ArrayList<EventListener> _globals = new ArrayList<EventListener>();

    protected static final float TIP_MODE_RESET = 0.6f;
}
