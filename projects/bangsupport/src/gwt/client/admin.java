//
// $Id: AdminPage.java 2812 2008-03-22 01:46:14Z mdb $

package client;

import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.underwire.gwt.client.AdminPanel;
import com.threerings.underwire.gwt.client.UnderwireEntryPoint;

/**
 * The main entry point for the Underwire admin client.
 */
public class admin extends UnderwireEntryPoint
{
    // @Override // from UnderwireEntryPoint
    public void onModuleLoad ()
    {
        super.onModuleLoad();

        AdminPanel panel = new AdminPanel(_ctx);
        RootPanel.get("appcontent").add(panel);
        panel.init();
    }

    // @Override // from UnderwireEntryPoint
    protected String getServletPath ()
    {
        return "/bangsupport/";
    }
}
