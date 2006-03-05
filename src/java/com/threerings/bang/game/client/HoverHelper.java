//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.event.MouseAdapter;
import com.jmex.bui.event.MouseEvent;

/**
 * Used to report a context help item to the controller when the mouse hovers
 * over a UI component.
 */
public class HoverHelper extends MouseAdapter
{
    public HoverHelper (BangController ctrl, String item)
    {
        _ctrl = ctrl;
        _item = item;
    }

    @Override // documentation inherited
    public void mouseEntered (MouseEvent event)
    {
        _ctrl.setHoveredItem(computeHoveredItem());
    }

    @Override // documentation inherited
    public void mouseExited (MouseEvent event)
    {
        _ctrl.setHoveredItem(null);
    }

    protected String computeHoveredItem ()
    {
        return _item;
    }

    protected BangController _ctrl;
    protected String _item;
}
