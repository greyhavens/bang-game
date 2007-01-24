//
// $Id$

package com.threerings.bang.client.bui;

import com.jme.renderer.Renderer;

import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.util.BasicContext;

/**
 * A popup window with the fancy steel background that has a special slot for a
 * title and for buttons at the bottom.
 */
public class SteelWindow extends BDecoratedWindow
{
    public SteelWindow (BasicContext ctx, String title)
    {
        super(ctx.getStyleSheet(), null);
        setStyleClass("steelwindow");
        ((GroupLayout)getLayoutManager()).setGap(20);

        add(_header = new BLabel(title, "window_title"), GroupLayout.FIXED);
        add(_contents = new BContainer());
        add(_buttons = GroupLayout.makeHBox(GroupLayout.CENTER), GroupLayout.FIXED);

        // load up our custom header image
        _headimg = new ImageBackground(
            ImageBackground.FRAME_X, ctx.loadImage("ui/window/header_steel.png"));
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _headimg.wasAdded();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _headimg.wasRemoved();
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        // we don't call super because we need to jimmy things up a bit
        int hwidth = _header.getWidth() + 2*HEADER_EDGE;
        int hheight = _headimg.getMinimumHeight();
        getBackground().render(renderer, 0, 0, _width, _height-hheight+OVERLAP, _alpha);
        _headimg.render(renderer, (_width - hwidth)/2, _height - hheight, hwidth, hheight, _alpha);
    }

    protected BLabel _header;
    protected BContainer _contents;
    protected BContainer _buttons;
    protected ImageBackground _headimg;

    protected static final int OVERLAP = 23;
    protected static final int HEADER_EDGE = 90;
}
