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
import com.threerings.presents.client.LogonException;
import com.threerings.presents.net.UsernamePasswordCreds;

import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

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

        add(_status = new StatusLabel(ctx));
        _status.setPreferredSize(new Dimension(420, 40));

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
            _status.setStatus(_msgs.get("m.logging_on"), false);
            _ctx.getClient().setCredentials(
                new UsernamePasswordCreds(
                    new Name(username),
                    Password.makeFromClear(password).getEncrypted()));
            _ctx.getClient().setVersion("" + DeploymentConfig.getVersion());
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
            _status.setStatus(_msgs.get("m.init_progress", ""+percent), false);
        } else {
            _status.setStatus(_msgs.get("m.init_complete"), false);
            _initialized = true;
        }
    }

    protected ClientAdapter _listener = new ClientAdapter() {
        public void clientDidLogon (Client client) {
            _status.setStatus(_msgs.get("m.logged_on"), false);
        }

        public void clientFailedToLogon (Client client, Exception cause) {
            String msg = cause.getMessage();
            if (cause instanceof LogonException) {
                // if the failure is due to a need for a client update, check
                // for that and take the appropriate action
                if (BangClient.checkForUpgrade(_ctx, msg)) {
                    // mogrify the logon failed message to let the client know
                    // that we're going to automatically restart
                    msg = "m.version_mismatch_auto";
                }
                msg = _msgs.xlate(msg);
            }
            _status.setStatus(_msgs.get("m.logon_failed", msg), true);
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BTextField _username;
    protected BPasswordField _password;
    protected StatusLabel _status;
    protected boolean _initialized;
}
