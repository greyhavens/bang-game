//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangCodes;

/**
 * Pops up when users click on the names of gangs.
 */
public class GangPopupMenu extends BPopupMenu
    implements ActionListener
{
    /**
     * Checks for a mouse click and pops up the specified gang's context menu if appropriate.
     */
    public static boolean checkPopup (BangContext ctx, BWindow parent, BEvent event, Handle name)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            if (mev.getType() == MouseEvent.MOUSE_PRESSED) {
                GangPopupMenu menu = new GangPopupMenu(ctx, parent, name);
                menu.popup(mev.getX(), mev.getY(), false);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a popup menu for the specified gang.
     */
    public GangPopupMenu (BangContext ctx, BWindow parent, Handle name)
    {
        super(parent);
        _ctx = ctx;
        _name = name;
        addListener(this);
        setLayer(BangUI.POPUP_MENU_LAYER);

        // add the name as a non-menu item
        String title = "@=u(" + name.toString() + ")";
        add(new BLabel(title, "popupmenu_title"));

        // add an item for viewing their info
        addMenuItem(new BMenuItem(ctx.xlate(GangCodes.GANG_MSGS, "m.view_info"), "view_info"));
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (event.getAction().equals("view_info")) {
            GangInfoDialog.display(_ctx, _name);
        }
    }

    protected BangContext _ctx;
    protected Handle _name;
}
