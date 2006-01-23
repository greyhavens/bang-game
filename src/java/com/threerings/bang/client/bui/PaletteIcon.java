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
        _text = new Label(this);
    }

    public void setText (String text)
    {
        _text.setText(text);
    }

    public Dimension getPreferredSize (int whint, int hhint)
    {
        return ICON_SIZE;
    }

    // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _text.stateDidChange();
    }

    // documentation inherited
    protected void stateDidChange ()
    {
        super.stateDidChange();
        _text.stateDidChange();
    }

    // documentation inherited
    protected void layout ()
    {
        super.layout();

        // we need to do some jiggery pokery to force the label in a bit from
        // the edges
        Insets insets = new Insets(getInsets());
        insets.left += 5;
        insets.top += 10;
        insets.right += 5;
        _text.layout(insets);
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        _text.render(renderer);
    }

    protected Label _text;
}
