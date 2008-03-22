//
// $Id: UserPage.java 2812 2008-03-22 01:46:14Z mdb $

package client;

import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.underwire.gwt.client.UnderwireEntryPoint;
import com.threerings.underwire.gwt.client.UserPanel;

/**
 * The main entry point for the Underwire user client.
 */
public class user extends UnderwireEntryPoint
{
    // @Override // from UnderwireEntryPoint
    public void onModuleLoad ()
    {
        super.onModuleLoad();

        UserPanel panel = new UserPanel(_ctx);
        RootPanel.get("appcontent").add(panel);
        panel.init();
    }

    // @Override // from UnderwireEntryPoint
    protected String getServletPath ()
    {
        return "/bangsupport/";
    }
}
