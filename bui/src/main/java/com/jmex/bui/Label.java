//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import org.lwjgl.opengl.GL11;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jmex.bui.icon.BIcon;
import com.jmex.bui.text.BText;
import com.jmex.bui.text.BTextFactory;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Insets;
import com.jmex.bui.util.Rectangle;

/**
 * Handles the underlying layout and rendering for {@link BLabel} and {@link BButton}.
 */
public class Label
    implements BConstants
{
    public Label (BTextComponent container)
    {
        _container = container;
    }

    /**
     * Updates the text displayed by this label.
     */
    public void setText (String text)
    {
        if (_value != null && _value.equals(text)) {
            return;
        }
        _value = text;

        // clear out our old text config and texture
        releaseText();

        // our size may have changed so we need to revalidate
        _container.invalidate();
    }

    /**
     * Returns the text currently being displayed by this label.
     */
    public String getText ()
    {
        return _value;
    }

    /**
     * Configures the label to display the specified icon.
     */
    public void setIcon (BIcon icon)
    {
        if (_icon == icon) {
            return;
        }
        int owidth = 0, oheight = 0, nwidth = 0, nheight = 0;
        if (_icon != null) {
            owidth = _icon.getWidth();
            oheight = _icon.getHeight();
            if (_container.isAdded()) {
                _icon.wasRemoved();
            }
        }

        _icon = icon;
        if (_icon != null) {
            nwidth = _icon.getWidth();
            nheight = _icon.getHeight();
            if (_container.isAdded()) {
                _icon.wasAdded();
            }
        }

        if (owidth != nwidth || oheight != nheight) {
            // reset our config so that we force a text reflow to account for the changed icon size
            releaseText();
            _container.invalidate();
        } else if (_container.isValid()) {
            _container.layout();
        }
    }

    /**
     * Returns the icon being displayed by this label.
     */
    public BIcon getIcon ()
    {
        return _icon;
    }

    /**
     * Configures the gap between the icon and the text.
     */
    public void setIconTextGap (int gap)
    {
        _gap = gap;
    }

    /**
     * Returns the gap between the icon and the text.
     */
    public int getIconTextGap ()
    {
        return _gap;
    }

    /**
     * Sets the orientation of this label with respect to its icon. If the horizontal (the default)
     * the text is displayed to the right of the icon, if vertical the text is displayed below it.
     */
    public void setOrientation (int orient)
    {
        _orient = orient;
        if (_container.isAdded()) {
            _container.layout();
        }
    }

    /**
     * Configures whether this label will wrap, truncate or scale if it cannot fit text into its
     * allotted width. The default is to wrap.
     */
    public void setFit (BLabel.Fit mode)
    {
        _fit = mode;
    }

    /**
     * Called by our containing component when it was added to the interface hierarchy.
     */
    public void wasAdded ()
    {
        if (_icon != null) {
            _icon.wasAdded();
        }
        if (_config != null && _config.glyphs != null) {
            _config.glyphs.wasAdded();
        }
    }

    /**
     * Called by our containing component when it was removed from the interface hierarchy.
     */
    public void wasRemoved ()
    {
        if (_icon != null) {
            _icon.wasRemoved();
        }
        if (_config != null && _config.glyphs != null) {
            _config.glyphs.wasRemoved();
        }
    }

    /**
     * Computes the preferred size of the label.
     */
    public Dimension computePreferredSize (int whint, int hhint)
    {
        // if our cached preferred size is not valid, recompute it
        Config prefconfig = layoutConfig(_prefconfig, whint > 0 ? whint : Short.MAX_VALUE-1);
        _prefsize = computeSize(_prefconfig = prefconfig);
        prefconfig.glyphs = null; // we don't need to retain these
        return new Dimension(_prefsize);
    }

    /**
     * Lays out the label text and icon.
     */
    public void layout (Insets insets, int contWidth, int contHeight)
    {
        // compute any offsets needed to center or align things
        Config config = layoutConfig(_config, contWidth - insets.getHorizontal());
        Dimension size = computeSize(config);
        int xoff = 0, yoff = 0;
        switch (_orient) {
        case HORIZONTAL:
            if (_icon != null) {
                _ix = getXOffset(insets, contWidth, size.width);
                _iy = getYOffset(insets, contHeight, _icon.getHeight());
                xoff = (_icon.getWidth() + _gap);
            }
            if (config.glyphs != null) {
                _tx = getXOffset(insets, contWidth, size.width) + xoff;
                _ty = getYOffset(insets, contHeight, config.glyphs.size.height);
            }
            break;

        case VERTICAL:
            if (config.glyphs != null) {
                _tx = getXOffset(insets, contWidth, config.glyphs.size.width);
                _ty = getYOffset(insets, contHeight, size.height);
                yoff = (config.glyphs.size.height + _gap);
            }
            if (_icon != null) {
                _ix = getXOffset(insets, contWidth, _icon.getWidth());
                _iy = getYOffset(insets, contHeight, size.height) + yoff;
            }
            break;

        case OVERLAPPING:
            if (_icon != null) {
                _ix = getXOffset(insets, contWidth, _icon.getWidth());
                _iy = getYOffset(insets, contHeight, _icon.getHeight());
            }
            if (config.glyphs != null) {
                _tx = getXOffset(insets, contWidth, config.glyphs.size.width);
                _ty = getYOffset(insets, contHeight, config.glyphs.size.height);
            }
            break;
        }

        useConfig(config);
    }

    /**
     * Releases any underlying texture resources created by this label.
     */
    public void releaseText ()
    {
        if (_config != null && _config.glyphs != null) {
            if (_container.isAdded()) {
                _config.glyphs.wasRemoved();
            }
            _config = null;
        }
    }

    /**
     * Renders the label text and icon.
     */
    public void render (Renderer renderer, int x, int y, int contWidth, int contHeight, float alpha)
    {
        GL11.glTranslatef(x, y, 0);
        try {
            if (_icon != null) {
                _icon.render(renderer, _ix, _iy, alpha);
            }
            if (_config != null && _config.glyphs != null) {
                renderText(renderer, contWidth, contHeight, alpha);
            }
        } finally {
            GL11.glTranslatef(-x, -y, 0);
        }
    }

    protected void renderText (Renderer renderer, int contWidth, int contHeight, float alpha)
    {
        if (_fit == BLabel.Fit.WRAP) {
            _config.glyphs.render(
                renderer, _tx, _ty, _container.getHorizontalAlignment(), alpha, _config.spacing);
            return;
        }

        Insets insets = _container.getInsets();
        int width = contWidth - insets.getHorizontal();
        int height = contHeight - insets.getVertical();
        if (width <= 0 || height <= 0) {
            return;
        }

        if (_fit == BLabel.Fit.SCALE) {
            _config.glyphs.render(
                renderer, _tx, _ty, width, height, _container.getHorizontalAlignment(), alpha);
            return;
        }

        boolean scissored = BComponent.intersectScissorBox(
            _srect, _container.getAbsoluteX() + insets.left,
            _container.getAbsoluteY() + insets.bottom, width, height);
        try {
            _config.glyphs.render(
                renderer, _tx, _ty, _container.getHorizontalAlignment(), alpha, _config.spacing);
        } finally {
            BComponent.restoreScissorState(scissored, _srect);
        }
    }

    protected Dimension computeSize (Config config)
    {
        int iwidth = 0, iheight = 0, twidth = 0, theight = 0, gap = 0;
        if (_icon != null) {
            iwidth = _icon.getWidth();
            iheight = _icon.getHeight();
        }
        if (config.glyphs != null) {
            if (_icon != null) {
                gap = _gap;
            }
            twidth = config.glyphs.size.width;
            theight = config.glyphs.size.height;
        }

        int width, height;
        switch (_orient) {
        default:
        case HORIZONTAL:
            width = iwidth + gap + twidth;
            height = Math.max(iheight, theight);
            break;
        case VERTICAL:
            width = Math.max(iwidth, twidth);
            height = iheight + gap + theight;
            break;
        case OVERLAPPING:
            width = Math.max(iwidth, twidth);
            height = Math.max(iheight, theight);
            break;
        }

        return new Dimension(width, height);
    }

    protected int getXOffset (Insets insets, int contWidth, int width)
    {
        switch (_container.getHorizontalAlignment()) {
        default:
        case LEFT: return insets.left;
        case RIGHT: return contWidth - width - insets.right;
        case CENTER: return (contWidth - insets.getHorizontal() - width) / 2 + insets.left;
        }
    }

    protected int getYOffset (Insets insets, int contHeight, int height)
    {
        switch (_container.getVerticalAlignment()) {
        default:
        case TOP: return contHeight - height - insets.top;
        case BOTTOM: return insets.bottom;
        case CENTER: return (contHeight - insets.getVertical() - height) / 2 + insets.bottom;
        }
    }

    /**
     * Creates glyphs for the current text at the specified target width.
     */
    protected Config layoutConfig (Config oconfig, int twidth)
    {
        // if we're not wrapping, force our target width
        if (_fit != BLabel.Fit.WRAP) {
            twidth = Short.MAX_VALUE-1;
        }

        if (_value != null) {
            // account for the space taken up by the icon
            if (_icon != null && _orient == HORIZONTAL) {
                twidth -= _gap;
                twidth -= _icon.getWidth();
            }
        }

        // no need to recreate our glyphs if our config hasn't changed
        Config config = _container.getLabelConfig(this, twidth);
        if (oconfig != null && oconfig.glyphs != null && oconfig.matches(config, twidth)) {
            return oconfig;
        }

        // if we have no text, we're done
        if (_value == null || _value.equals("")) {
            return config;
        }

        // sanity check
        if (twidth < 0) {
            Log.log.warning("Requested to layout with negative target width [text=" + _value +
                            ", twidth=" + twidth + "].");
            Thread.dumpStack();
            return config;
        }

        // render up some new text
        BTextFactory tfact = _container.getTextFactory(this);
        Text text = new Text();
        text.lines = tfact.wrapText(
            _value, config.color, config.effect, config.effectSize, config.effectColor, twidth);
        for (int ii = 0; ii < text.lines.length; ii++) {
            text.size.width = Math.max(text.size.width, text.lines[ii].getSize().width);
            text.size.height += text.lines[ii].getSize().height + (ii > 0 ? config.spacing : 0);
        }
        config.glyphs = text;

        // if our old config is the same number of lines as our new config, expand the width region
        // that this configuration will match
        if (oconfig != null && oconfig.glyphs != null &&
            oconfig.glyphs.lines.length == config.glyphs.lines.length) {
            config.minwidth = Math.min(config.minwidth, oconfig.minwidth);
            config.maxwidth = Math.max(config.maxwidth, oconfig.maxwidth);
        }

        return config;
    }

    protected void useConfig (Config config)
    {
        // make sure it's not the one we're already using
        if (_config == config) {
            return;
        }

        // clear out any previous rendered text
        releaseText();

        // note our new config
        _config = config;

        if (_config != null && _config.glyphs != null && _container.isAdded()) {
            _config.glyphs.wasAdded();
        }
    }

    protected static class Config
    {
        public String text;
        public ColorRGBA color;
        public int effect;
        public int effectSize;
        public ColorRGBA effectColor;
        public int minwidth, maxwidth;
        public Text glyphs;
        public int spacing;

        public boolean matches (Config other, int twidth) {
            if (other == null) {
                return false;
            }

            // if any of the styles are different, we don't match
            if (effect != other.effect) {
                return false;
            }
            if (text != other.text && (text == null || !text.equals(other.text))) {
                return false;
            }
            if (!color.equals(other.color)) {
                return false;
            }
            if (effectColor != other.effectColor &&
                (effectColor == null || !effectColor.equals(other.effectColor))) {
                return false;
            }
            if (effectSize != other.effectSize || spacing != other.spacing) {
                return false;
            }

            // if we are only one line we are fine as long as we're less than or equal to the
            // target width (only if it is smaller than our minwidth might it cause us to wrap)
            if (glyphs != null && glyphs.lines.length == 1 && minwidth <= twidth) {
                return true;
            }

            // otherwise the new target width has to fall within our handled range
            return (minwidth <= twidth) && (twidth <= maxwidth);
        }

        public String toString () {
            return text + "(" + toString(color) + "," + effect + "," + toString(effectColor) + "," +
                minwidth + "<>" + maxwidth + ")";
        }

        protected String toString (ColorRGBA color) {
            return color == null ? "null" : Integer.toHexString(color.hashCode());
        }
    }

    protected static class Text
    {
        public BText[] lines;
        public Dimension size = new Dimension();

        public void render (Renderer renderer, int tx, int ty, int halign,
                            float alpha, int spacing) {
            // render the lines from the bottom up
            for (int ii = lines.length-1; ii >= 0; ii--) {
                int lx = tx;
                if (halign == RIGHT) {
                    lx += size.width - lines[ii].getSize().width;
                } else if (halign == CENTER) {
                    lx += (size.width - lines[ii].getSize().width)/2;
                }
                lines[ii].render(renderer, lx, ty, alpha);
                ty += lines[ii].getSize().height + (ii > 0 ? spacing : 0);
            }
        }

        public void render (Renderer renderer, int tx, int ty,
                int width, int height, int halign, float alpha) {
            // render only the first line
            float scale = 1f;
            if (size.width > width) {
                scale = (float)width/size.width;
            }
            if (size.height > height) {
                scale = Math.min(scale, (float)height/size.height);
            }
            width = (int)(size.width * scale);
            height = (int)(size.height * scale);
            if (height < size.height) {
                ty += (size.height - height)/2;
            }
            if (halign == RIGHT) {
                tx += size.width - width;
            } else if (halign == CENTER) {
                tx += (size.width - width)/2;
            }
            lines[0].render(renderer, tx, ty, width, height, alpha);
        }

        public void wasAdded () {
            for (int ii = 0; ii < lines.length; ii++) {
                lines[ii].wasAdded();
            }
        }

        public void wasRemoved () {
            for (int ii = 0; ii < lines.length; ii++) {
                lines[ii].wasRemoved();
            }
        }
    }

    protected BTextComponent _container;
    protected String _value;

    protected int _orient = HORIZONTAL;
    protected int _gap = 3;
    protected BLabel.Fit _fit = BLabel.Fit.WRAP;

    protected BIcon _icon;
    protected int _ix, _iy;

    protected Config _config;
    protected int _tx, _ty;
    protected float _alpha = 1f;

    protected Config _prefconfig;
    protected Dimension _prefsize;

    protected Rectangle _srect = new Rectangle();
}
