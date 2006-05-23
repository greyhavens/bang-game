//
// $Id$

package com.threerings.bang.client.bui;

import com.jme.renderer.Renderer;
import com.jmex.bui.Label;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Insets;

/**
 * Displays an icon of a standard size with text done the way we like it.
 */
public class PaletteIcon extends SelectableIcon
{
    public static final Dimension ICON_SIZE = new Dimension(136, 156);

    public PaletteIcon ()
    {
        setStyleClass("palette_icon");
        _text = new Label(this);
    }

    public void setText (String text)
    {
        _text.setText(text);
    }

    public String getText ()
    {
        return _text.getText();
    }

    public Dimension getPreferredSize (int whint, int hhint)
    {
        return ICON_SIZE;
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _text.wasAdded();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _text.wasRemoved();
    }

    @Override // documentation inherited
    protected void stateDidChange ()
    {
        super.stateDidChange();
        _text.stateDidChange();
    }

    @Override // documentation inherited
    protected void layout ()
    {
        super.layout();

        // we need to do some jiggery pokery to force the label in a bit from
        // the edges
        _text.layout(getTextInsets());
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        _text.render(renderer, _alpha);
    }

    /**
     * Provides custom insets for our special label.
     */
    protected Insets getTextInsets ()
    {
        return new Insets(5, 10, 5, 0);
    }

    protected Label _text;
}
