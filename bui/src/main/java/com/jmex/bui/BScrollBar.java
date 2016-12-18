//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.ChangeEvent;
import com.jmex.bui.event.ChangeListener;
import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseListener;
import com.jmex.bui.event.MouseWheelListener;

import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.util.Insets;

/**
 * Displays a scroll bar for all your horizontal and vertical scrolling
 * needs.
 */
public class BScrollBar extends BContainer
    implements BConstants
{
    /**
     * Creates a vertical scroll bar with the default range, value and
     * extent.
     */
    public BScrollBar ()
    {
        this(VERTICAL);
    }

    /**
     * Creates a scroll bar with the default range, value and extent.
     */
    public BScrollBar (int orientation)
    {
        this(orientation, 0, 100, 0, 10);
    }

    /**
     * Creates a scroll bar with the specified orientation, range, value
     * and extent.
     */
    public BScrollBar (int orientation, int min, int value, int extent, int max)
    {
        this(orientation, new BoundedRangeModel(min, value, extent, max));
    }

    /**
     * Creates a scroll bar with the specified orientation which will
     * interact with the supplied model.
     */
    public BScrollBar (int orientation, BoundedRangeModel model)
    {
        super(new BorderLayout());
        _orient = orientation;
        _model = model;
        _model.addChangeListener(_updater);
    }

    /**
     * Returns a reference to the scrollbar's range model.
     */
    public BoundedRangeModel getModel ()
    {
        return _model;
    }
    
    // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // listen for mouse wheel events
        addListener(_wheelListener = _model.createWheelListener());

        // create our buttons and backgrounds
        String oprefix = "scrollbar_" + ((_orient == HORIZONTAL) ? "h" : "v");
        _well = new BComponent();
        _well.setStyleClass(oprefix + "well");
        add(_well, BorderLayout.CENTER);
        _well.addListener(_wellListener);

        _thumb = new BComponent();
        _thumb.setStyleClass(oprefix + "thumb");
        add(_thumb, BorderLayout.IGNORE);
        _thumb.addListener(_thumbListener);

        _less = new BButton("");
        _less.setStyleClass(oprefix + "less");
        add(_less, _orient == HORIZONTAL ?
            BorderLayout.WEST : BorderLayout.NORTH);
        _less.addListener(_buttoner);
        _less.setAction("less");

        _more = new BButton("");
        _more.setStyleClass(oprefix + "more");
        add(_more, _orient == HORIZONTAL ?
            BorderLayout.EAST : BorderLayout.SOUTH);
        _more.addListener(_buttoner);
        _more.setAction("more");
    }

    // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();

        if (_wheelListener != null) {
            removeListener(_wheelListener);
            _wheelListener = null;
        }
        if (_well != null) {
            remove(_well);
            _well = null;
        }
        if (_thumb != null) {
            remove(_thumb);
            _thumb = null;
        }
        if (_less != null) {
            remove(_less);
            _less = null;
        }
        if (_more != null) {
            remove(_more);
            _more = null;
        }
    }

    // documentation inherited
    public BComponent getHitComponent (int mx, int my)
    {
        // we do special processing for the thumb
        if (_thumb.getHitComponent(mx - _x, my - _y) != null) {
            return _thumb;
        }
        return super.getHitComponent(mx, my);
    }

    /**
     * Recomputes and repositions the scroll bar thumb to reflect the
     * current configuration of the model.
     */
    protected void update ()
    {
        if (!isAdded()) {
            return;
        }
        Insets winsets = _well.getInsets();
        int tx = 0, ty = 0;
        int twidth = _well.getWidth() - winsets.getHorizontal();
        int theight = _well.getHeight() - winsets.getVertical();
        int range = Math.max(_model.getRange(), 1); // avoid div0
        int extent = Math.max(_model.getExtent(), 1); // avoid div0
        if (_orient == HORIZONTAL) {
            int wellSize = twidth;
            tx = _model.getValue() * wellSize / range;
            twidth = extent * wellSize / range;
        } else {
            int wellSize = theight;
            ty = (range-extent-_model.getValue()) * wellSize / range;
            theight = extent * wellSize / range;
        }
        _thumb.setBounds(_well.getX() + winsets.left + tx,
                         _well.getY() + winsets.bottom + ty, twidth, theight);
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "scrollbar";
    }

    // documentation inherited
    protected void layout ()
    {
        super.layout();

        // reposition our thumb
        update();
    }

    protected ChangeListener _updater = new ChangeListener() {
        public void stateChanged (ChangeEvent event) {
            update();
        }
    };

    protected MouseListener _wellListener = new MouseAdapter() {
        public void mousePressed (MouseEvent event) {
            // if we're above the thumb, scroll up by a page, if we're
            // below, scroll down a page
            int mx = event.getX() - getAbsoluteX(),
                my = event.getY() - getAbsoluteY(), dv = 0;
            if (_orient == HORIZONTAL) {
                if (mx < _thumb.getX()) {
                    dv = -1;
                } else if (mx > _thumb.getX() + _thumb.getWidth()) {
                    dv = 1;
                }
            } else {
                if (my < _thumb.getY()) {
                    dv = 1;
                } else if (my > _thumb.getY() + _thumb.getHeight()) {
                    dv = -1;
                }
            }
            if (dv != 0) {
                dv *= Math.max(1, _model.getExtent());
                _model.setValue(_model.getValue() + dv);
            }
        }
    };

    protected MouseAdapter _thumbListener = new MouseAdapter() {
        public void mousePressed (MouseEvent event) {
            _sv = _model.getValue();
            _sx = event.getX() - getAbsoluteX();
            _sy = event.getY() - getAbsoluteY();
        }

        public void mouseDragged (MouseEvent event) {
            int dv = 0;
            if (_orient == HORIZONTAL) {
                int mx = event.getX() - getAbsoluteX();
                dv = (mx - _sx) * _model.getRange() /
                    (_well.getWidth() - _well.getInsets().getHorizontal());
            } else {
                int my = event.getY() - getAbsoluteY();
                dv = (_sy - my) * _model.getRange() /
                    (_well.getHeight() - _well.getInsets().getVertical());
            }

            if (dv != 0) {
                _model.setValue(_sv + dv);
            }
        }

        protected int _sx, _sy, _sv;
    };

    protected ActionListener _buttoner = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            int delta = _model.getScrollIncrement();
            if (event.getAction().equals("less")) {
                _model.setValue(_model.getValue() - delta);
            } else {
                _model.setValue(_model.getValue() + delta);
            }
        }
    };

    protected BoundedRangeModel _model;
    protected int _orient;

    protected BButton _less, _more;
    protected BComponent _well, _thumb;

    protected MouseWheelListener _wheelListener;
}
