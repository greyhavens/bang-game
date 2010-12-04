//
// $Id: AdminPage.java 2812 2008-03-22 01:46:14Z mdb $

package client;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.underwire.gwt.client.AdminPanel;
import com.threerings.underwire.gwt.client.UnderwireEntryPoint;

/**
 * The main entry point for the Underwire admin client.
 */
public class admin extends UnderwireEntryPoint
    implements ValueChangeHandler<String>
{
    // @Override // from UnderwireEntryPoint
    public void onModuleLoad ()
    {
        super.onModuleLoad();

        History.addValueChangeHandler(this);

        _panel = new AdminPanel(_ctx);
        RootPanel.get("appcontent").add(_panel);
        _panel.init();

        _ctx.frame.navigate(History.getToken());
    }

    // documentation inherited from interface ValueChangeHandler
    public void onValueChange (ValueChangeEvent<String> event)
    {
        _panel.reset();
        _ctx.frame.navigate(event.getValue());
    }

    // @Override // from UnderwireEntryPoint
    protected String getServletPath ()
    {
        return "/bangsupport/";
    }

    protected AdminPanel _panel;
}
