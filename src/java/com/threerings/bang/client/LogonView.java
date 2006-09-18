//
// $Id$

package com.threerings.bang.client;

import java.io.IOException;
import java.util.EnumSet;

import java.net.ConnectException;
import java.net.URL;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
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
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.RandomUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.util.BrowserUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.LogonException;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.BangAuthCodes;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * Displays a simple user interface for logging in.
 */
public class LogonView extends BWindow
    implements ActionListener, BasicClient.InitObserver
{
    /**
     * Converts an arbitrary exception into a translatable error string (which
     * should be looked up in the @{link BangAuthCodes#AUTH_MSGS} bundle). If
     * the exception indicates that the client is out of date, the process of
     * updating the client <em>will be started</em>; the client will exit a few
     * seconds later, so be sure to display the returned error message.
     *
     * <p> An additional boolean paramater will be returned indicating whether
     * or not the returned error message is indicative of a connection failure,
     * in which case the caller may wish to direct the user to the server
     * status page so they can find out if we are in the middle of a sceduled
     * downtime.
     */
    public static Tuple<String,Boolean> decodeLogonException (
        BangContext ctx, Exception cause)
    {
        String msg = cause.getMessage();
        boolean connectionFailure = false;

        if (cause instanceof LogonException) {
            // if the failure is due to a need for a client update, check for
            // that and take the appropriate action
            if (BangClient.checkForUpgrade(ctx, msg)) {
                // mogrify the logon failed message to let the client know that
                // we're going to automatically restart
                msg = "m.version_mismatch_auto";
            }

            // change the new account button to server status for certain
            // response codes
            if (msg.equals(BangAuthCodes.UNDER_MAINTENANCE)) {
                connectionFailure = true;
            }

        } else {
            if (cause instanceof ConnectException) {
                msg = "m.failed_to_connect";

            } else if (cause instanceof IOException) {
                String cmsg = cause.getMessage();
                // foolery to detect a problem where Windows Connection Sharing
                // will allow a connection to complete and then disconnect it
                // after the first normal packet is sent
                if (cmsg != null && cmsg.indexOf("forcibly closed") != -1) {
                    msg = "m.failed_to_connect";
                } else {
                    msg = "m.network_error";
                }

            } else {
                msg = "m.network_error";
            }

            // change the new account button to server status
            connectionFailure = true;
        }

        return new Tuple<String,Boolean>(msg, connectionFailure);
    }

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
        // use a special sound effect for logon which is the ricochet that we
        // also use for window open
        _logon.setProperty("feedback_sound", BangUI.FeedbackSound.WINDOW_OPEN);
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

        // TODO: pick from the town they most recently logged into
        UnitConfig[] units = UnitConfig.getTownUnits(
            BangCodes.FRONTIER_TOWN,
            EnumSet.of(UnitConfig.Rank.BIGSHOT, UnitConfig.Rank.NORMAL));
        if (units.length > 0) {
            _unitIcon = BangUI.getUnitIcon(RandomUtil.pickRandom(units));
        }

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

            // configure the client to connect to the town lobby server that
            // this player last accessed
            String townId = BangPrefs.getLastTownId(username);
            // but make sure this town has been activated on this client
            if (!BangClient.isTownActive(townId)) {
                // fall back to frontier town if it has not
                townId = BangCodes.FRONTIER_TOWN;
            }
            _ctx.getClient().setServer(DeploymentConfig.getServerHost(townId),
                                       DeploymentConfig.getServerPorts(townId));

            // configure the client with the supplied credentials
            _ctx.getClient().setCredentials(
                _ctx.getBangClient().createCredentials(
                    new Name(username), password));

            // now we can log on
            _ctx.getClient().logon();

        } else if ("options".equals(event.getAction())) {
            _ctx.getBangClient().displayPopup(
                new OptionsView(_ctx, this), true);

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

            // if we already have credentials (set on the command line during
            // testing), auto-logon
            if (_ctx.getClient().getCredentials() != null) {
                _ctx.getClient().logon();
            }
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // focus the appropriate textfield
        if (StringUtil.isBlank(_username.getText())) {
            _username.requestFocus();
        } else {
            _password.requestFocus();
        }

        if (_unitIcon != null) {
            _unitIcon.wasAdded();
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        if (_unitIcon != null) {
            _unitIcon.wasRemoved();
        }
    }

    @Override // documentation inherited
    protected void renderBackground (Renderer renderer)
    {
        super.renderBackground(renderer);

        if (_unitIcon != null) {
            _unitIcon.render(renderer, 50, 380, _alpha);
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
            Tuple<String,Boolean> msg = decodeLogonException(_ctx, cause);
            if (msg.right) {
                switchToServerStatus();
            }
            _status.setStatus(_msgs.xlate(msg.left), true);
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
    protected BIcon _unitIcon;

    protected StatusLabel _status;
    protected boolean _initialized;
    protected URL _shownURL;
}
