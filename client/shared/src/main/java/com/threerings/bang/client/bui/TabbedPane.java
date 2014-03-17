//
// $Id$

package com.threerings.bang.client.bui;

import com.jme.renderer.Renderer;
import com.jmex.bui.BTabbedPane;
import com.jmex.bui.background.BBackground;

/**
 * We customize the tabbed pane to do some background adjustments.
 */
public class TabbedPane extends BTabbedPane
{
    public TabbedPane (boolean leaveRoomForScrollbar)
    {
        _leaveRoomForScrollbar = leaveRoomForScrollbar;
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        // shrink the tab background so that the tabs overlap the top edge and
        // the scroll bar falls outside
        BBackground background = getBackground();
        if (background != null) {
            int width = _width;
            if (_leaveRoomForScrollbar) {
                width -= 18;
            }
            background.render(renderer, 0, 0, width, _height - 30, _alpha);
        }
    }

    protected boolean _leaveRoomForScrollbar;
}
