//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BPasswordField;
import com.jmex.bui.BTextField;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;
import com.jme.renderer.ColorRGBA;
import com.jme.util.TextureManager;

import com.samskivert.servlet.user.Password;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.net.UsernamePasswordCreds;

import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays a simple user interface for logging in.
 */
public class LogonView extends BWindow
    implements ActionListener, BasicClient.InitObserver
{
    public LogonView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), GroupLayout.makeVert(GroupLayout.TOP));
        setStyleClass("logon_view");

        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.white);

        _msgs = ctx.getMessageManager().getBundle(BangAuthCodes.AUTH_MSGS);
        BContainer cont = new BContainer(new TableLayout(3, 5, 5));
        cont.add(new BLabel(_msgs.get("m.username")));
        cont.add(_username = new BTextField());
        _username.setPreferredWidth(150);
        cont.add(new BLabel(""));
        cont.add(new BLabel(_msgs.get("m.password")));
        cont.add(_password = new BPasswordField());
        _password.addListener(this);

        BButton logon = new BButton(_msgs.get("m.logon"), "logon");
        logon.addListener(this);
        cont.add(logon);
        add(cont);

        add(_status = new BLabel(""));

        cont = new BContainer(new TableLayout(2, 5, 5));
        BButton btn;
        cont.add(btn = new BButton(_msgs.get("m.options"), "options"));
        btn.addListener(this);
        cont.add(btn = new BButton(_msgs.get("m.exit"), "exit"));
        btn.addListener(this);
        add(cont);

        // add our logon listener
        _ctx.getClient().addClientObserver(_listener);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (event.getSource() == _password ||
            "logon".equals(event.getAction())) {
            if (!_initialized) {
                log.warning("Not finished initializing. Hang tight.");
                return;
            }

            String username = _username.getText();
            String password = _password.getText();
            _status.setText(_msgs.get("m.logging_on"));
            _ctx.getClient().setCredentials(
                new UsernamePasswordCreds(
                    new Name(username),
                    Password.makeFromClear(password).getEncrypted()));
            _ctx.getClient().logon();

        } else if ("options".equals(event.getAction())) {
            OptionsView oview = new OptionsView(_ctx, this);
            _ctx.getRootNode().addWindow(oview);
            oview.pack();
            oview.center();

        } else if ("exit".equals(event.getAction())) {
            _ctx.getApp().stop();
        }
    }

    // documentation inherited from interface BasicClient.InitObserver
    public void progress (int percent)
    {
        if (percent < 100) {
            _status.setText(_msgs.get("m.init_progress", ""+percent));
        } else {
            _status.setText(_msgs.get("m.init_complete"));
            _initialized = true;
        }
    }

    protected ClientAdapter _listener = new ClientAdapter() {
        public void clientDidLogon (Client client) {
            _status.setText(_msgs.get("m.logged_on"));
        }

        public void clientFailedToLogon (Client client, Exception cause) {
            String msg = _msgs.get("m.logon_failed", cause.getMessage());
            _status.setText(msg);
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BTextField _username;
    protected BPasswordField _password;
    protected BLabel _status;
    protected boolean _initialized;
}
