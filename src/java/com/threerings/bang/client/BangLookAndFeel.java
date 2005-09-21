//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BLookAndFeel;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.background.BBackground;
import com.jmex.bui.background.TiledBackground;
import com.jmex.bui.border.BBorder;

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
        case BToggleButton.SELECTED:
        case BButton.DISABLED:
        case BButton.DOWN:
            path = "rsrc/ui/button_down.png";
            dx = -1;
            dy = -1;
            break;
        case BButton.OVER: path = "rsrc/ui/button_up.png"; break;
        default:
        case BButton.UP: path = "rsrc/ui/button_up.png"; break;
        }
        return new TiledBackground(getResource(path),
                                   5 + dx, 4 + dy, 5 - dx, 2 - dy);
    }

    public BBackground createPopupBackground ()
    {
        return new TiledBackground(
            getResource("rsrc/ui/button_up.png"), 5, 4, 5, 2);
    }

    public BBorder createPopupBorder ()
    {
        return null;
    }

    public BBackground createComboBoxBackground ()
    {
        return new TiledBackground(
            getResource("rsrc/ui/button_up.png"), 5, 4, 5, 2);
    }

    public BBackground createWindowBackground ()
    {
        return new TiledBackground(
            getResource("rsrc/ui/window_background.png"), 15, 15, 15, 15);
    }

    public BBorder createWindowBorder ()
    {
        return null;
    }
}
