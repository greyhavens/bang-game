//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import com.jme.renderer.Renderer;

import com.jmex.bui.icon.BIcon;
import com.jmex.bui.util.Dimension;

/**
 * A simple component for displaying a textual label.
 */
public class BLabel extends BTextComponent
    implements BConstants
{
    /** Configures the label's strategy when it does not fit into its allocated space. */
    public enum Fit { WRAP, TRUNCATE, SCALE };

    /**
     * Creates a label that will display the supplied text.
     */
    public BLabel (String text)
    {
        this(text, null);
    }

    /**
     * Creates a label that will display the supplied text using the specified style class.
     */
    public BLabel (String text, String styleClass)
    {
    	this(null, text, styleClass);
    }

    /**
     * Creates a label that will display the supplied icon.
     */
    public BLabel (BIcon icon)
    {
        this(icon, null, null);
    }

    /**
     * Creates a label that will display the supplied icon using the specified style class.
     */
    public BLabel (BIcon icon, String styleClass)
    {
    	this(icon, null, styleClass);
    }

    /**
     * Creates a label that will display the supplied text and icon using the specified style
     * class. All arguments can be null.
     */
    public BLabel (BIcon icon, String text, String styleClass)
    {
        _label = new Label(this);
        if (icon != null) {
            setIcon(icon);
        }
        if (text != null) {
            setText(text);
        }
        if (styleClass != null) {
            setStyleClass(styleClass);
        }
    }

    /**
     * Configures the label to display the specified icon.
     */
    public void setIcon (BIcon icon)
    {
        _label.setIcon(icon);
    }

    /**
     * Returns the icon being displayed by this label.
     */
    public BIcon getIcon ()
    {
        return _label.getIcon();
    }

    /**
     * Configures the gap between the icon and the text.
     */
    public void setIconTextGap (int gap)
    {
        _label.setIconTextGap(gap);
    }

    /**
     * Returns the gap between the icon and the text.
     */
    public int getIconTextGap ()
    {
        return _label.getIconTextGap();
    }

    /**
     * Sets the orientation of this label with respect to its icon. If the
     * horizontal (the default) the text is displayed to the right of the icon,
     * if vertical the text is displayed below it.
     */
    public void setOrientation (int orient)
    {
        _label.setOrientation(orient);
    }

    /**
     * Configures whether this label will wrap, truncate or scale if it cannot
     * fit text into its allotted width. The default is to wrap.
     */
    public void setFit (Fit mode)
    {
        _label.setFit(mode);
    }

    /**
     * Returns the current fit mode for this label.
     */
    public Fit getFit ()
    {
        return _label._fit;
    }

    // documentation inherited
    public void setText (String text)
    {
        _label.setText(text);
    }

    // documentation inherited
    public String getText ()
    {
        return _label.getText();
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "label";
    }

    // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _label.wasAdded();
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _label.wasRemoved();
    }

    // documentation inherited
    protected void layout ()
    {
        super.layout();
        _label.layout(getInsets(), getWidth(), getHeight());
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        _label.render(renderer, 0, 0, getWidth(), getHeight(), _alpha);
    }

    // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return _label.computePreferredSize(whint, hhint);
    }

    protected Label _label;
}
