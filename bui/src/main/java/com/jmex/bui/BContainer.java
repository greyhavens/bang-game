//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.util.ArrayList;
import java.util.logging.Level;

import com.jme.renderer.Renderer;

import com.jmex.bui.layout.BLayoutManager;
import com.jmex.bui.util.Dimension;

import static com.jmex.bui.Log.log;

/**
 * A user interface element that is meant to contain other interface
 * elements.
 */
public class BContainer extends BComponent
{
    /**
     * Creates a container with no layout manager. One should subsequently
     * be set via a call to {@link #setLayoutManager}.
     */
    public BContainer ()
    {
    }

    /**
     * Creates a container with the supplied layout manager.
     */
    public BContainer (BLayoutManager layout)
    {
        setLayoutManager(layout);
    }

    /**
     * Configures this container with an entity that will set the size and
     * position of its children.
     */
    public void setLayoutManager (BLayoutManager layout)
    {
        _layout = layout;
    }

    /**
     * Returns the layout manager configured for this container.
     */
    public BLayoutManager getLayoutManager ()
    {
        return _layout;
    }

    /**
     * Adds a child to this container.
     */
    public void add (BComponent child)
    {
        add(child, null);
    }

    /**
     * Adds a child to this container at the specified position.
     */
    public void add (int index, BComponent child)
    {
        add(index, child, null);
    }

    /**
     * Adds a child to this container with the specified layout
     * constraints.
     */
    public void add (BComponent child, Object constraints)
    {
        add(_children.size(), child, constraints);
    }

    /**
     * Adds a child to this container at the specified position, with the
     * specified layout constraints.
     */
    public void add (int index, BComponent child, Object constraints)
    {
        if (_layout != null) {
            _layout.addLayoutComponent(child, constraints);
        }
        _children.add(index, child);
        child.setParent(this);

        // if we're already part of the hierarchy, call wasAdded() on our
        // child; otherwise when our parent is added, everyone will have
        // wasAdded() called on them
        if (isAdded()) {
            child.wasAdded();
        }

        // we need to be relayed out
        invalidate();
    }

    /**
     * Removes the child at a specific position from this container.
     */
    public void remove (int index)
    {
	BComponent child = getComponent(index);
	_children.remove(index);
        if (_layout != null) {
            _layout.removeLayoutComponent(child);
        }
        child.setParent(null);

        // if we're part of the hierarchy we call wasRemoved() on the
        // child now (which will be propagated to all of its children)
        if (isAdded()) {
            child.wasRemoved();
        }

        // we need to be relayed out
        invalidate();
    }

    /**
     * Replaces a given old component with a new component (if the old component exits).
     *
     * @return true if the old component was replaced, false otherwise.
     */
    public boolean replace (BComponent oldc, BComponent newc)
    {
        int idx = _children.indexOf(oldc);
        if (idx >= 0) {
            remove(idx);
            add(idx, newc);
            return true;
        }
        return false;
    }

    /**
     * Removes the specified child from this container.
     */
    public void remove (BComponent child)
    {
        if (!_children.remove(child)) {
            // if the component was not our child, stop now
            return;
        }
        if (_layout != null) {
            _layout.removeLayoutComponent(child);
        }
        child.setParent(null);

        // if we're part of the hierarchy we call wasRemoved() on the
        // child now (which will be propagated to all of its children)
        if (isAdded()) {
            child.wasRemoved();
        }

        // we need to be relayed out
        invalidate();
    }

    /**
     * Returns the number of components contained in this container.
     */
    public int getComponentCount ()
    {
        return _children.size();
    }

    /**
     * Returns the <code>index</code>th component from this container.
     */
    public BComponent getComponent (int index)
    {
        return _children.get(index);
    }

    /**
     * Returns the index of the specified component in this container or -1 if the component count
     * not be found.
     */
    public int getComponentIndex (BComponent component)
    {
        return _children.indexOf(component);
    }

    /**
     * Removes all children of this container.
     */
    public void removeAll ()
    {
        for (int ii = getComponentCount() - 1; ii >= 0; ii--) {
            remove(getComponent(ii));
        }
    }

    // documentation inherited
    public void setAlpha (float alpha)
    {
        super.setAlpha(alpha);

        // set our children's alpha values accordingly
        for (int ii = getComponentCount() - 1; ii >= 0; ii--) {
            getComponent(ii).setAlpha(alpha);
        }
    }

    // documentation inherited
    public void setEnabled (boolean enabled)
    {
        super.setEnabled(enabled);

        // enable or disable our children accordingly
        for (int ii = getComponentCount() - 1; ii >= 0; ii--) {
            getComponent(ii).setEnabled(enabled);
        }
    }

    // documentation inherited
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);

        // show or hide our children accordingly
        for (int ii = getComponentCount() - 1; ii >= 0; ii--) {
            getComponent(ii).setVisible(visible);
        }
    }

    // documentation inherited
    public BComponent getHitComponent (int mx, int my)
    {
        // if we're not within our bounds, we don't need to check our children
        if (super.getHitComponent(mx, my) != this) {
            return null;
        }

        // translate the coordinate into our children's coordinates
        mx -= _x;
        my -= _y;

        BComponent hit = null;
        for (int ii = getComponentCount() - 1; ii >= 0; ii--) {
            BComponent child = getComponent(ii);
            if ((hit = child.getHitComponent(mx, my)) != null) {
                return hit;
            }
        }
        return this;
    }

    // documentation inherited
    public void validate ()
    {
        if (!_valid) {
            layout(); // lay ourselves out

            // now validate our children
            applyOperation(new ChildOp() {
                public void apply (BComponent child) {
                    child.validate();
                }
            });

            _valid = true; // finally mark ourselves as valid
        }
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "container";
    }

    // documentation inherited
    protected void layout ()
    {
        if (_layout != null) {
            _layout.layoutContainer(this);
        }
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        // render our children
        for (int ii = 0, ll = getComponentCount(); ii < ll; ii++) {
            getComponent(ii).render(renderer);
        }
    }

    // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        if (_layout != null) {
            return _layout.computePreferredSize(this, whint, hhint);
        } else {
            return super.computePreferredSize(whint, hhint);
        }
    }

    // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // call wasAdded() on all of our existing children; if they are added later (after we are
        // added), they will automatically have wasAdded() called on them at that time
        applyOperation(new ChildOp() {
            public void apply (BComponent child) {
                child.wasAdded();
            }
        });
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // call wasRemoved() on all of our children
        applyOperation(new ChildOp() {
            public void apply (BComponent child) {
                child.wasRemoved();
            }
        });
    }

    /**
     * Returns the next component that should receive focus in this container given the current
     * focus owner. If the supplied current focus owner is null, the container should return its
     * first focusable component. If the container has no focusable components following the
     * current focus, it should call {@link #getNextFocus()} to search further up the hierarchy.
     */
    protected BComponent getNextFocus (BComponent current)
    {
        boolean foundCurrent = (current == null);
        for (int ii = 0, ll = getComponentCount(); ii < ll; ii++) {
            BComponent child = getComponent(ii);
            if (!foundCurrent) {
                if (child == current) {
                    foundCurrent = true;
                }
                continue;
            }
            if (child.acceptsFocus()) {
                return child;
            }
        }
        return getNextFocus();
    }

    /**
     * Returns the previous component that should receive focus in this container given the current
     * focus owner. If the supplied current focus owner is null, the container should return its
     * last focusable component. If the container has no focusable components before the current
     * focus, it should call {@link #getPreviousFocus()} to search further up the hierarchy.
     */
    protected BComponent getPreviousFocus (BComponent current)
    {
        boolean foundCurrent = (current == null);
        for (int ii = getComponentCount()-1; ii >= 0; ii--) {
            BComponent child = getComponent(ii);
            if (!foundCurrent) {
                if (child == current) {
                    foundCurrent = true;
                }
                continue;
            }
            if (child.acceptsFocus()) {
                return child;
            }
        }
        return getPreviousFocus();
    }

    /**
     * Applies an operation to all of our children.
     */
    protected void applyOperation (ChildOp op)
    {
        BComponent[] children = _children.toArray(new BComponent[_children.size()]);
        for (BComponent child : children) {
            try {
                op.apply(child);
            } catch (Exception e) {
                log.log(Level.WARNING, "Child operation choked [op=" + op +
                        ", child=" + child + "].", e);
            }
        }
    }

    /** Used in {@link #wasAdded} and {@link #wasRemoved}. */
    protected static interface ChildOp
    {
        public void apply (BComponent child);
    }

    protected ArrayList<BComponent> _children = new ArrayList<BComponent>();
    protected BLayoutManager _layout;
}
