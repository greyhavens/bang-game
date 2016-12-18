//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import com.jme.renderer.Renderer;

import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Insets;

/**
 * Displays a track with a little frob somewhere along its length that allows a user to select a
 * smoothly varying value between two bounds.
 */
public class BSlider extends BComponent
    implements BConstants
{
    /**
     * Creates a slider with the specified orientation, range and value.
     *
     * @param orient either {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    public BSlider (int orient, int min, int max, int value)
    {
        this(orient, new BoundedRangeModel(min, value, 0, max));
    }

    /**
     * Creates a slider with the specified orientation and range model. Note that the extent must
     * be set to zero.
     *
     * @param orient either {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    public BSlider (int orient, BoundedRangeModel model)
    {
        _orient = orient;
        _model = model;
    }

    /**
     * Returns a reference to the slider's range model.
     */
    public BoundedRangeModel getModel ()
    {
        return _model;
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return ((_orient == HORIZONTAL) ? "h" : "v") + "slider";
    }

    // documentation inherited
    protected void configureStyle (BStyleSheet style)
    {
        super.configureStyle(style);

        // load up our frobs
        for (int ii = 0; ii < getStateCount(); ii++) {
            _frobs[ii] = style.getIcon(this, getStatePseudoClass(ii));
        }
    }

    // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        for (int ii = 0; ii < _frobs.length; ii++) {
            if (_frobs[ii] != null) {
                _frobs[ii].wasAdded();
            }
        }
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        for (int ii = 0; ii < _frobs.length; ii++) {
            if (_frobs[ii] != null) {
                _frobs[ii].wasRemoved();
            }
        }
    }

    // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        Dimension psize =
            new Dimension(getFrob().getWidth(), getFrob().getHeight());
        if (_orient == HORIZONTAL) {
            psize.width *= 2;
        } else {
            psize.height *= 2;
        }
        return psize;
    }

    // documentation inherited
    public boolean dispatchEvent (BEvent event)
    {
        if (isEnabled() && event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            int mx = mev.getX() - getAbsoluteX(), my = mev.getY() - getAbsoluteY();
            switch (mev.getType()) {
            case MouseEvent.MOUSE_PRESSED:
                if (mev.getButton() == 0) {
                    // move the slider based on the current mouse position
                    updateValue(mx, my);
                }
                break;

            case MouseEvent.MOUSE_DRAGGED:
                // move the slider based on the current mouse position
                updateValue(mx, my);
                break;

            case MouseEvent.MOUSE_WHEELED:
                // move by 1/10th if we're wheeled
                int delta = _model.getRange()/10, value = _model.getValue();
                _model.setValue(mev.getDelta() > 0 ? value + delta : value - delta);
                break;

            default:
                return super.dispatchEvent(event);
            }

            return true;
        }

        return super.dispatchEvent(event);
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        // render our frob at the appropriate location
        Insets insets = getInsets();
        BIcon frob = getFrob();
        int x, y, range = _model.getRange();
        int offset = _model.getValue() - _model.getMinimum();
        if (_orient == HORIZONTAL) {
            y = (getHeight() - frob.getHeight())/2;
            x = insets.left + (getWidth() - insets.getHorizontal() -
                               frob.getWidth()) * offset / range;
        } else {
            x = (getWidth() - frob.getWidth())/2;
            y = insets.bottom + (getHeight() - insets.getVertical() -
                                 frob.getHeight()) * offset / range;
        }
        frob.render(renderer, x, y, _alpha);
    }

    protected void updateValue (int mx, int my)
    {
        Insets insets = getInsets();
        BIcon frob = getFrob();
        if (_orient == HORIZONTAL) {
            int fwid = frob.getWidth();
            _model.setValue((mx - fwid/2) * _model.getRange() /
                            (getWidth() - insets.getHorizontal() - fwid));
        } else {
            int fhei = frob.getHeight();
            _model.setValue((my - fhei/2) * _model.getRange() /
                            (getHeight() - insets.getVertical() - fhei));
        }
    }

    protected BIcon getFrob ()
    {
        BIcon frob = _frobs[getState()];
        return (frob != null) ? frob : _frobs[DEFAULT];
    }

    protected int _orient;
    protected BoundedRangeModel _model;
    protected BIcon[] _frobs = new BIcon[getStateCount()];
}
