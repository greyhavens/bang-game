//
// $Id$

package com.threerings.bang.client;

import com.jme.bui.BButton;
import com.jme.bui.BLookAndFeel;
import com.jme.bui.BToggleButton;
import com.jme.bui.background.BBackground;
import com.jme.bui.background.TiledBackground;

/**
 * Handles the custom look and feel for the Bang! Howdy BUI user interface.
 */
public class BangLookAndFeel extends BLookAndFeel
{
    public BangLookAndFeel ()
    {
        BLookAndFeel.configureDefaultLookAndFeel(this);
    }

    public BBackground createButtonBack (int state)
    {
        String path;
        int dx = 0, dy = 0;
        switch (state) {
        case BButton.DOWN:
            path = "rsrc/ui/button_down.png";
            dx = -1;
            dy = -1;
            break;
        case BButton.OVER: path = "rsrc/ui/button_up.png"; break;
        case BToggleButton.SELECTED: return super.createButtonBack(state);
        default:
        case BButton.UP: path = "rsrc/ui/button_up.png"; break;
        }
        return new TiledBackground(getResource(path),
                                   5 + dx, 4 + dy, 5 - dx, 2 - dy);
    }
}
