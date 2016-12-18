//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import org.lwjgl.opengl.GL11;

import com.jme.renderer.Renderer;
import com.jmex.bui.event.MouseWheelListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Insets;
import com.jmex.bui.util.Rectangle;

/**
 * Provides a scrollable clipped view on a sub-heirarchy of components.
 */
public class BScrollPane extends BContainer
{
    public BScrollPane (BComponent child)
    {
        this(child, true, false);
    }

    public BScrollPane (BComponent child, boolean vert, boolean horiz)
    {
        this(child, vert, horiz, -1);
    }

    public BScrollPane (
        BComponent child, boolean vert, boolean horiz, int snap)
    {
        super(new BorderLayout(0, 0));

        add(_vport = new BViewport(child, vert, horiz, snap),
            BorderLayout.CENTER);
        if (vert) {
            add(_vbar = new BScrollBar(BScrollBar.VERTICAL,
                    _vport.getVModel()), BorderLayout.EAST);
        }
        if (horiz) {
            add(_hbar = new BScrollBar(BScrollBar.HORIZONTAL,
                    _vport.getHModel()), BorderLayout.SOUTH);
        }
    }

    /**
     * Returns a reference to the child of this scroll pane.
     */
    public BComponent getChild ()
    {
        return _vport.getTarget();
    }

    /**
     * Returns a reference to the vertical scroll bar.
     */
    public BScrollBar getVerticalScrollBar ()
    {
        return _vbar;
    }

    /**
     * Returns a reference to the horizontal scroll bar.
     */
    public BScrollBar getHorizontalScrollBar ()
    {
        return _hbar;
    }

    /**
     * Toggles the scroll bar policy.  If set to true, the bars will always
     * show.  If set to false, the bars will only show when needed.
     */
    public void setShowScrollbarAlways (boolean showAlways)
    {
        if (_showAlways != showAlways) {
            _showAlways = showAlways;
            invalidate();
        }
    }

    /**
     * Configures the style class of the viewport (the non-scrolling container
     * that will hold the scrolling contents).
     */
    public void setViewportStyleClass (String styleClass)
    {
        _vport.setStyleClass(styleClass);
    }

    @Override // documentation inherited
    public void layout ()
    {
        if (_layingOut || _vport.getTarget().isValid()) {
            super.layout();
            return;
        }
        _layingOut = true;
        if (_vbar != null) {
            if (_showAlways && _vbar.getParent() == null) {
                add(_vbar, BorderLayout.EAST);
            } else if (!_showAlways && _vbar.getParent() != null) {
                remove(_vbar);
            }
        }
        if (_hbar != null) {
            if (_showAlways && _hbar.getParent() == null) {
                add(_hbar, BorderLayout.SOUTH);
            } else if (!_showAlways && _hbar.getParent() != null) {
                remove(_hbar);
            }
        }
        validate();
        if (_showAlways) {
            _layingOut = false;
            return;
        }
        boolean hadded = false, vadded = false;
        // Add a horizontal bar if needed
        if (_hbar != null) {
            BoundedRangeModel hmodel = _hbar.getModel();
            if (hmodel.getExtent() != hmodel.getRange()) {
                add(_hbar, BorderLayout.SOUTH);
                validate();
                hadded = true;
            }
        }
        // Add a vertical bar if needed
        if (_vbar != null) {
            BoundedRangeModel vmodel = _vbar.getModel();
            if (vmodel.getExtent() != vmodel.getRange()) {
                add(_vbar, BorderLayout.EAST);
                validate();
                vadded = true;
            }
        }
        // Check if adding the vertical bar now requires the horizontal bar
        // to be added
        if (vadded && !hadded && _hbar != null) {
            BoundedRangeModel hmodel = _hbar.getModel();
            if (hmodel.getExtent() != hmodel.getRange()) {
                add(_hbar, BorderLayout.SOUTH);
                validate();
            }
        }
        _layingOut = false;
    }

    /** Does all the heavy lifting for the {@link BScrollPane}. */
    protected static class BViewport extends BContainer
    {
        public BViewport (
            BComponent target, boolean vert, boolean horiz, int snap)
        {
            if (vert) {
                if (snap > 0) {
                    _vmodel = new BoundedSnappingRangeModel(0, 0, 10, 10, snap);
                } else {
                    _vmodel = new BoundedRangeModel(0, 0, 10, 10);
                }
            }
            if (horiz) {
                if (snap > 0) {
                    _hmodel = new BoundedSnappingRangeModel(0, 0, 10, 10, snap);
                } else {
                    _hmodel = new BoundedRangeModel(0, 0, 10, 10);
                }
            }
            add(_target = target);
        }

        /**
         * Returns a reference to the target of this viewport.
         */
        public BComponent getTarget ()
        {
            return _target;
        }

        /**
         * Returns the range model defined by this viewport's size and the
         * preferred size of its target component.
         */
        public BoundedRangeModel getVModel ()
        {
            return _vmodel;
        }

        /**
         * Returns the range model defined by this viewport's size and the
         * preferred size of its target component.
         */
        public BoundedRangeModel getHModel ()
        {
            return _hmodel;
        }

        // documentation inherited
        public void invalidate ()
        {
            // if we're not attached, don't worry about it
            BWindow window;
            BRootNode root;
            if (!_valid || (window = getWindow()) == null ||
                (root = window.getRootNode()) == null) {
                return;
            }

            _valid = false;
            root.rootInvalidated(this);
        }

        // documentation inherited
        public void layout ()
        {
            // resize our target component to the larger of our size and its
            // preferred size
            Insets insets = getInsets();
            int twidth = getWidth() - insets.getHorizontal();
            int theight = getHeight() - insets.getVertical();
            Dimension d = _target.getPreferredSize(twidth, theight);
            d.width = (_hmodel != null) ?
                Math.max(d.width, twidth) : twidth;
            d.height = (_vmodel != null) ?
                Math.max(d.height, theight) : theight;
            if (_target.getWidth() != d.width ||
                _target.getHeight() != d.height) {
                _target.setBounds(insets.left, insets.bottom, d.width,
                    d.height);
            }

            // lay out our target component
            _target.layout();

            // and recompute our scrollbar range
            if (_vmodel != null) {
                int extent = getHeight() - insets.getVertical();
                int value = Math.max(0,Math.min(_vmodel.getValue(), d.height - extent));
                _vmodel.setRange(0, value, extent, d.height);
            }
            if (_hmodel != null) {
                int extent = getWidth() - insets.getHorizontal();
                int value = Math.max(0,Math.min(_hmodel.getValue(), d.width - extent));
                _hmodel.setRange(0, value, extent, d.width);
            }
        }

        // documentation inherited
        public int getAbsoluteX ()
        {
            return super.getAbsoluteX() + getXOffset();
        }

        // documentation inherited
        public int getAbsoluteY ()
        {
            return super.getAbsoluteY() + getYOffset();
        }

        // documentation inherited
        public BComponent getHitComponent (int mx, int my)
        {
            // if we're not within our bounds, we needn't check our target
            Insets insets = getInsets();
            if ((mx < _x + insets.left) || (my < _y + insets.bottom) ||
                (mx >= _x + _width - insets.right) ||
                (my >= _y + _height - insets.top)) {
                return null;
            }

            // translate the coordinate into our children's coordinates
            mx -= (_x + getXOffset());
            my -= (_y + getYOffset());

            BComponent hit = null;
            for (int ii = 0, ll = getComponentCount(); ii < ll; ii++) {
                BComponent child = getComponent(ii);
                if ((hit = child.getHitComponent(mx, my)) != null) {
                    return hit;
                }
            }
            return this;
        }

        // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();
            if (_vmodel != null) {
                addListener(_wheelListener = _vmodel.createWheelListener());
            } else if (_hmodel != null) {
                addListener(_wheelListener = _hmodel.createWheelListener());
            }
        }

        // documentation inherited
        protected void wasRemoved ()
        {
            super.wasRemoved();
            if (_wheelListener != null) {
                removeListener(_wheelListener);
                _wheelListener = null;
            }
        }

        // documentation inherited
        protected Dimension computePreferredSize (int whint, int hhint)
        {
            return new Dimension(_target.getPreferredSize(whint, hhint));
        }

        // documentation inherited
        protected void renderComponent (Renderer renderer)
        {
            // translate by our offset into the viewport
            Insets insets = getInsets();
            int yoffset = getYOffset();
            int xoffset = getXOffset();
            GL11.glTranslatef(xoffset, yoffset, 0);
            boolean scissored = intersectScissorBox(_srect,
                (getAbsoluteX() + insets.left) - xoffset,
                (getAbsoluteY() + insets.bottom) - yoffset,
                _width - insets.getHorizontal(),
                _height - insets.getVertical());
            try {
                // and then render our target component
                _target.render(renderer);
            } finally {
                restoreScissorState(scissored, _srect);
                GL11.glTranslatef(-xoffset, -yoffset, 0);
            }
        }

        protected final int getYOffset ()
        {
            return _vmodel == null ? 0 : _vmodel.getValue() -
                (_vmodel.getMaximum() - _vmodel.getExtent());
        }

        protected final int getXOffset ()
        {
            return _hmodel == null ? 0 : -_hmodel.getValue();
        }

        protected BoundedRangeModel _vmodel, _hmodel;
        protected BComponent _target;
        protected MouseWheelListener _wheelListener;
        protected Rectangle _srect = new Rectangle();
    }

    protected BViewport _vport;
    protected BScrollBar _vbar, _hbar;
    protected boolean _showAlways = true, _layingOut;
}
