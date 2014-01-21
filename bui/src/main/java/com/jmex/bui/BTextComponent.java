//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.text.BTextFactory;

/**
 * Defines methods and mechanisms common to components that render a string of
 * text.
 */
public abstract class BTextComponent extends BComponent
{
    /**
     * Updates the text displayed by this component.
     */
    public abstract void setText (String text);

    /**
     * Returns the text currently being displayed by this component.
     */
    public abstract String getText ();

    /**
     * Returns a text factory suitable for creating text in the style defined
     * by the component's current state.
     */
    public BTextFactory getTextFactory ()
    {
        BTextFactory textfact = _textfacts[getState()];
        return (textfact != null) ? textfact : _textfacts[DEFAULT];
    }

    /**
     * Returns the horizontal alignment for this component's text.
     */
    public int getHorizontalAlignment ()
    {
        if (_haligns != null) {
            int halign = _haligns[getState()];
            return (halign != -1) ? halign : _haligns[DEFAULT];
        }
        return BConstants.LEFT;
    }

    /**
     * Returns the vertical alignment for this component's text.
     */
    public int getVerticalAlignment ()
    {
        if (_valigns != null) {
            int valign = _valigns[getState()];
            return (valign != -1) ? valign : _valigns[DEFAULT];
        }
        return BConstants.CENTER;
    }

    /**
     * Returns the effect for this component's text.
     */
    public int getTextEffect ()
    {
        if (_teffects != null) {
            int teffect = _teffects[getState()];
            return (teffect != -1) ? teffect : _teffects[DEFAULT];
        }
        return BConstants.NORMAL;
    }

    /**
     * Returns the effect size for this component's text.
     */
    public int getEffectSize ()
    {
        if (_effsizes != null) {
            int effsize = _effsizes[getState()];
            return (effsize > 0) ? effsize : _effsizes[DEFAULT];
        }
        return BConstants.DEFAULT_SIZE;
    }

    /**
     * Returns the color to use for our text effect.
     */
    public ColorRGBA getEffectColor ()
    {
        if (_effcols != null) {
            ColorRGBA effcol = _effcols[getState()];
            return (effcol != null) ? effcol : _effcols[DEFAULT];
        }
        return ColorRGBA.white;
    }

    /**
     * Returns the line spacing for our text.
     */
    public int getLineSpacing ()
    {
        if (_lineSpacings != null) {
            return _lineSpacings[getState()];
        }
        return BConstants.DEFAULT_SPACING;
    }


    // documentation inherited
    protected void configureStyle (BStyleSheet style)
    {
        super.configureStyle(style);

        int[] haligns = new int[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            haligns[ii] = style.getTextAlignment(this, getStatePseudoClass(ii));
        }
        _haligns = checkNonDefault(haligns, BConstants.LEFT);

        int[] valigns = new int[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            valigns[ii] = style.getVerticalAlignment(
                this, getStatePseudoClass(ii));
        }
        _valigns = checkNonDefault(valigns, BConstants.CENTER);

        int[] teffects = new int[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            teffects[ii] = style.getTextEffect(this, getStatePseudoClass(ii));
        }
        _teffects = checkNonDefault(teffects, BConstants.NORMAL);

        int[] effsizes = new int[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            effsizes[ii] = style.getEffectSize(this, getStatePseudoClass(ii));
        }
        _effsizes = checkNonDefault(effsizes, BConstants.DEFAULT_SIZE);

        int[] lineSpacings = new int[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            lineSpacings[ii] = style.getLineSpacing(this, getStatePseudoClass(ii));
        }
        _lineSpacings = checkNonDefaultInt(lineSpacings, BConstants.DEFAULT_SPACING);

        ColorRGBA[] effcols = new ColorRGBA[getStateCount()];
        boolean nondef = false;
        for (int ii = 0; ii < getStateCount(); ii++) {
            effcols[ii] = style.getEffectColor(this, getStatePseudoClass(ii));
            nondef = nondef || (effcols[ii] != null);
            _textfacts[ii] =
                style.getTextFactory(this, getStatePseudoClass(ii));
        }
        if (nondef) {
            _effcols = effcols;
        }
    }

    /**
     * Returns the text factory that should be used by the supplied label (for which we are by
     * definition acting as container) to generate its text.
     */
    protected BTextFactory getTextFactory (Label forLabel)
    {
        return getTextFactory();
    }

    /**
     * Creates a text configuration for the supplied label (for which we are by definition acting
     * as container).
     */
    protected Label.Config getLabelConfig (Label forLabel, int twidth)
    {
        Label.Config config = new Label.Config();
        config.text = forLabel.getText();
        config.color = getColor();
        config.effect = getTextEffect();
        config.effectSize = getEffectSize();
        config.effectColor = getEffectColor();
        config.spacing = getLineSpacing();
        config.minwidth = config.maxwidth = twidth;
        return config;
    }

    protected int[] checkNonDefault (int[] styles, int defval)
    {
        return checkNonDefaultVal(styles, defval, -1);
    }

    protected int[] checkNonDefaultInt (int[] styles, int defval)
    {
        return checkNonDefaultVal(styles, defval, defval);
    }

    protected int[] checkNonDefaultVal (int[] styles, int defval1, int defval2)
    {
        for (int ii = 0; ii < styles.length; ii++) {
            if (styles[ii] != defval1 && styles[ii] != defval2) {
                return styles;
            }
        }
        return null;
    }

    protected int[] _haligns;
    protected int[] _valigns;
    protected int[] _teffects;
    protected int[] _effsizes;
    protected int[] _lineSpacings;
    protected ColorRGBA[] _effcols;
    protected BTextFactory[] _textfacts = new BTextFactory[getStateCount()];
}
