//
// $Id$

package com.threerings.bang.client;

import com.jme.bui.BButton;
import com.jme.bui.BContainer;
import com.jme.bui.BLabel;
import com.jme.bui.BPasswordField;
import com.jme.bui.BTextField;
import com.jme.bui.BWindow;
import com.jme.bui.TintedBackground;
import com.jme.bui.event.ActionEvent;
import com.jme.bui.event.ActionListener;
import com.jme.bui.layout.BorderLayout;
import com.jme.bui.layout.TableLayout;
import com.jme.renderer.ColorRGBA;

import com.samskivert.servlet.user.Password;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.net.UsernamePasswordCreds;

import com.threerings.bang.util.BangContext;

/**
 * Displays a simple user interface for logging in.
 */
public class LogonView extends BWindow
    implements ActionListener
{
    public LogonView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), new BorderLayout(5, 5));
        _ctx = ctx;
        setBackground(new TintedBackground(0, 0, 0, 0, ColorRGBA.darkGray));

        _msgs = ctx.getMessageManager().getBundle("logon");
        BContainer cont = new BContainer(new TableLayout(3, 5, 5));
        cont.add(new BLabel(_msgs.get("m.username")));
        cont.add(_username = new BTextField());
        _username.setPreferredWidth(150);
        cont.add(new BLabel(""));
        cont.add(new BLabel(_msgs.get("m.password")));
        cont.add(_password = new BPasswordField());

        BButton logon = new BButton(_msgs.get("m.logon"));
        logon.addListener(this);
        cont.add(logon);
        add(cont, BorderLayout.CENTER);

        add(_status = new BLabel(""), BorderLayout.SOUTH);

        // add our logon listener
        _ctx.getClient().addClientObserver(_listener);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String username = _username.getText();
        String password = _password.getText();
        _status.setText(_msgs.get("m.logging_on"));
        _ctx.getClient().setCredentials(
            new UsernamePasswordCreds(
                new Name(username),
                Password.makeFromClear(password).getEncrypted()));
        _ctx.getClient().logon();
    }

    protected ClientAdapter _listener = new ClientAdapter() {
        public void clientDidLogon (Client client) {
            _status.setText(_msgs.get("m.logged_on"));
        }

        public void clientFailedToLogon (Client client, Exception cause) {
            String msg = _msgs.get("m.logon_failed", cause.toString());
            _status.setText(msg);
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BTextField _username;
    protected BPasswordField _password;
    protected BLabel _status;
}
