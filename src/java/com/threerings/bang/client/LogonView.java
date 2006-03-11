//
// $Id$

package com.threerings.bang.client;

import java.io.IOException;

import java.net.ConnectException;
import java.net.URL;

import com.jme.renderer.ColorRGBA;
import com.jme.util.TextureManager;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
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

import com.samskivert.servlet.user.Password;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.util.BrowserUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.LogonException;
import com.threerings.presents.net.UsernamePasswordCreds;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.bui.EnablingValidator;
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
        ((GroupLayout)getLayoutManager()).setOffAxisJustification(
            GroupLayout.RIGHT);
        setStyleClass("logon_view");

        _ctx = ctx;
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.black);

        _msgs = ctx.getMessageManager().getBundle(BangAuthCodes.AUTH_MSGS);
        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        ((GroupLayout)row.getLayoutManager()).setOffAxisJustification(
            GroupLayout.BOTTOM);
        BContainer grid = new BContainer(new TableLayout(2, 5, 5));
        grid.add(new BLabel(_msgs.get("m.username"), "logon_label"));
        grid.add(_username = new BTextField(
                     BangPrefs.config.getValue("username", "")));
        _username.setPreferredWidth(150);
        grid.add(new BLabel(_msgs.get("m.password"), "logon_label"));
        grid.add(_password = new BPasswordField());
        _password.addListener(this);
        row.add(grid);

        BContainer col = GroupLayout.makeVBox(GroupLayout.CENTER);
        row.add(col);
        col.add(_logon = new BButton(_msgs.get("m.logon"), this, "logon"));
        _logon.setStyleClass("big_button");
        col.add(_action =
                new BButton(_msgs.get("m.new_account"), this, "new_account"));
        _action.setStyleClass("logon_new");
        add(row);

        // disable the logon button until a password is entered (and until
        // we're initialized)
        new EnablingValidator(_password, _logon) {
            protected boolean checkEnabled (String text) {
                return super.checkEnabled(text) && _initialized;
            }
        };

        add(_status = new StatusLabel(ctx));
        _status.setStyleClass("logon_status");
        _status.setPreferredSize(new Dimension(360, 40));

        row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(new BButton(_msgs.get("m.options"), this, "options"));
        row.add(new BButton(_msgs.get("m.exit"), this, "exit"));
        add(row);

        // add our logon listener
        _ctx.getClient().addClientObserver(_listener);
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        // focus the appropriate textfield
        if (StringUtil.isBlank(_username.getText())) {
            _username.requestFocus();
        } else {
            _password.requestFocus();
        }
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

        } else if ("server_status".equals(event.getAction())) {
            BrowserUtil.browseURL(
                _shownURL = DeploymentConfig.getServerStatusURL(), _browlist);
            _status.setStatus(_msgs.get("m.server_status_launched"), false);

        } else if ("new_account".equals(event.getAction())) {
            BrowserUtil.browseURL(
                _shownURL = DeploymentConfig.getNewAccountURL(), _browlist);
            _status.setStatus(_msgs.get("m.new_account_launched"), false);

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
            _logon.setEnabled(!StringUtil.isBlank(_password.getText()));
            _initialized = true;
        }
    }

    protected void switchToServerStatus ()
    {
        _action.setText(_msgs.get("m.server_status"));
        _action.setAction("server_status");
    }

    protected ClientAdapter _listener = new ClientAdapter() {
        public void clientDidLogon (Client client) {
            _status.setStatus(_msgs.get("m.logged_on"), false);
            BangPrefs.config.setValue("username", _username.getText());
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

                // change the new account button to server status for certain
                // response codes
                if (msg.equals(BangAuthCodes.UNDER_MAINTENANCE)) {
                    switchToServerStatus();
                }

                msg = _msgs.xlate(msg);

            } else {
                if (cause instanceof ConnectException) {
                    msg = _msgs.xlate("m.failed_to_connect");

                } else if (cause instanceof IOException) {
                    String cmsg = cause.getMessage();
                    // foolery to detect a problem where Windows Connection
                    // Sharing will allow a connection to complete and then
                    // disconnect it after the first normal packet is sent
                    if (cmsg != null && cmsg.indexOf("forcibly closed") != -1) {
                        msg = "m.failed_to_connect";
                    } else {
                        msg = "m.network_error";
                    }

                } else {
                    msg = "m.network_error";
                }

                // change the new account button to server status
                switchToServerStatus();
            }

            _status.setStatus(msg, true);
        }
    };

    protected ResultListener _browlist = new ResultListener() {
        public void requestCompleted (Object result) {
        }
        public void requestFailed (Exception cause) {
            _status.setStatus(_msgs.get("m.browser_launch_failed",
                                        _shownURL.toString()), true);
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected BTextField _username;
    protected BPasswordField _password;
    protected BButton _logon, _action;

    protected StatusLabel _status;
    protected boolean _initialized;
    protected URL _shownURL;
}
