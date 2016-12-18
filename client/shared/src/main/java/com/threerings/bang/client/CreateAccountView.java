//
// $Id$

package com.threerings.bang.client;

import java.util.Calendar;
import java.util.Date;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BLabel;
import com.jmex.bui.BPasswordField;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.net.MailUtil;
import com.samskivert.util.StringUtil;
import com.samskivert.servlet.user.InvalidUsernameException;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.Username;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.client.bui.SteelWindow;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Allows a player to create an account.
 */
public class CreateAccountView extends SteelWindow
    implements ActionListener, BangClient.NonClearablePopup
{
    public static void show (BangContext ctx, String customMsg, boolean onExit)
    {
        CreateAccountView cav = new CreateAccountView(ctx, customMsg, onExit);
        cav.setLayer(BangCodes.NEVER_CLEAR_LAYER);
        ctx.getBangClient().displayPopup(cav, true, 650);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("create")) {
            createAccount();
        } else if (cmd.equals("cancel")) {
            if (_onExit) {
                _ctx.getApp().stop();
            } else {
                _ctx.getBangClient().clearPopup(this, true);
                _ctx.getBangClient().resetTownView();
            }
        }
    }

    protected CreateAccountView (BangContext ctx, String customMsg, boolean onExit)
    {
        super(ctx, ctx.xlate(BangCodes.BANG_MSGS, "m.account_title"));
        setModal(true);
        setLayer(BangCodes.NEVER_CLEAR_LAYER);
        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        _onExit = onExit;
        _contents.setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(15));
        _contents.setStyleClass("padded");

        // we may have been provided with m.account_info_loctype for which we may have a custom
        // message explaining why an account is needed for that location
        String infoMsg = customMsg != null && _msgs.exists(customMsg) ? customMsg : "m.account_info";
        _contents.add(new BLabel(_msgs.get(infoMsg), "dialog_text_left"));

        BContainer grid = new BContainer(new TableLayout(2, 5, 5));
        grid.add(new BLabel(_msgs.get("m.username"), "right_label"));
        grid.add(_username = new BTextField(12));
        _username.setPreferredWidth(150);
        grid.add(new BLabel(_msgs.get("m.password"), "right_label"));
        grid.add(_password = new BPasswordField(32));
        _password.setPreferredWidth(150);
        grid.add(new BLabel(_msgs.get("m.repassword"), "right_label"));
        grid.add(_repassword = new BPasswordField(32));
        _repassword.setPreferredWidth(150);
        grid.add(new BLabel(_msgs.get("m.email"), "right_label"));
        grid.add(_email = new BTextField(128));
        _email.setPreferredWidth(150);
        grid.add(new BLabel(_msgs.get("m.birthday"), "right_label"));
        BContainer birthcont = GroupLayout.makeHBox(GroupLayout.LEFT);
        birthcont.add(new BLabel(_msgs.get("m.year")));
        _years = new BComboBox();
        _years.setPreferredColumns(10);
        Calendar cal = Calendar.getInstance();
        cal.roll(Calendar.YEAR, -7);
        int base = cal.get(Calendar.YEAR);
        for (int ii = 0; ii < 100; ii++) {
            _years.addItem(Integer.valueOf(base - ii));
        }
        _years.selectItem(7); // let's assume they're 14
        _years.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                String action = event.getAction();
                if (action.equals("selectionChanged")) {
                    Calendar year = Calendar.getInstance();
                    year.set((Integer)_years.getSelectedItem(), Calendar.DECEMBER, 31);
                    Calendar now = Calendar.getInstance();
                    now.roll(Calendar.YEAR, -BangCodes.COPPA_YEAR);
                    boolean visible = year.compareTo(now) > 0;
                    year.roll(Calendar.YEAR, -1);
                    visible = visible && year.compareTo(now) < 0;
                    _monthL.setVisible(visible);
                    _months.setVisible(visible);
                    _dayL.setVisible(visible);
                    _days.setVisible(visible);
                }
            }
        });
        birthcont.add(_years);
        birthcont.add(_monthL = new BLabel(_msgs.get("m.month")));
        _months = new BComboBox();
        for (int ii = 1; ii <= 12; ii++) {
            _months.addItem(Integer.valueOf(ii));
        }
        _months.selectItem(0);
        birthcont.add(_months);
        birthcont.add(_dayL = new BLabel(_msgs.get("m.day")));
        _days = new BComboBox();
        _days.setPreferredColumns(10);
        for (int ii = 1; ii <= 31; ii++) {
            _days.addItem(Integer.valueOf(ii));
        }
        _days.selectItem(0);
        birthcont.add(_days);
        grid.add(birthcont);
        _monthL.setVisible(false);
        _months.setVisible(false);
        _dayL.setVisible(false);
        _days.setVisible(false);

        _contents.add(grid);
        _contents.add(_status = new StatusLabel(_ctx));

        _buttons.add(_cancel = new BButton(
                    _msgs.get(onExit ? "m.quit" : "m.cancel"), this, "cancel"));
        _buttons.add(_create = new BButton(_msgs.get("m.account_create"), this, "create"));
        _create.setEnabled(false);

        // Create a listener for text entry
        TextListener tlistener = new TextListener () {
            public void textChanged (TextEvent event) {
                _textIn = (!StringUtil.isBlank(_username.getText()) &&
                           !StringUtil.isBlank(_password.getText()) &&
                           !StringUtil.isBlank(_repassword.getText()));
                _create.setEnabled(_over13 && _textIn);
            }
        };
        _username.addListener(tlistener);
        _password.addListener(tlistener);
        _repassword.addListener(tlistener);

        // create a listener for the required age
        ActionListener alistener = new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                if (event.getAction().equals("selectionChanged")) {
                    updateBirthdate();
                }
            }
        };
        _years.addListener(alistener);
        _months.addListener(alistener);
        _days.addListener(alistener);

        // set our status and enablings properly to start
        updateBirthdate();
    }

    protected void updateBirthdate ()
    {
        Calendar year = Calendar.getInstance();
        year.set((Integer)_years.getSelectedItem(),
                 ((Integer)_months.getSelectedItem() - 1),
                 (Integer)_days.getSelectedItem());
        Calendar now = Calendar.getInstance();
        now.roll(Calendar.YEAR, -BangCodes.COPPA_YEAR);
        _over13 = year.compareTo(now) <= 0;
        _status.setStatus(_msgs.get(_over13 ? "m.account_tip" : "m.under_coppa"), false);
        _create.setEnabled(_over13 && _textIn);
        _birthdate = year.getTime();
    }

    /**
     * Validates the account info then attempts to create the account.
     */
    protected void createAccount ()
    {
        Username username;
        try {
            username = new Username(_username.getText());
        } catch (InvalidUsernameException iue) {
            _status.setStatus(_msgs.get(iue.getMessage()), true);
            return;
        }
        final String uname = username.getUsername();

        if (!_password.getText().equals(_repassword.getText())) {
            _status.setStatus(_msgs.get("e.password_no_match"), true);
            return;
        }

        final String email = _email.getText().trim();
        if (email.length() > 0 && !MailUtil.isValidAddress(email)) {
            _status.setStatus(_msgs.get("e.invalid_email"), true);
            return;
        }

        PlayerService psvc = _ctx.getClient().requireService(PlayerService.class);
        PlayerService.ConfirmListener cl = new PlayerService.ConfirmListener() {
            public void requestProcessed () {
                BangPrefs.config.setValue("anonymous", "");
                BangPrefs.config.setValue("username", uname);
                showRestart();
            }
            public void requestFailed (String reason) {
                _status.setStatus(_msgs.xlate(reason), true);
                _cancel.setEnabled(true);
                _create.setEnabled(true);
            }
        };
        _cancel.setEnabled(false);
        _create.setEnabled(false);

        psvc.createAccount(uname, Password.makeFromClear(_password.getText()).getEncrypted(),
                           _email.getText(), BangClient.getAffiliateFromInstallFile(),
                           _birthdate.getTime(), cl);
    }

    /**
     * Shows a success message and forces the client to restart.
     */
    protected void showRestart()
    {
        OptionDialog.ResponseReceiver rr = new OptionDialog.ResponseReceiver() {
            public void resultPosted (int button, Object result) {
                if (!BangClient.relaunchGetdown(_ctx, 500L)) {
                    log.info("Failed to restart Bang, exiting");
                    _ctx.getApp().stop();
                }
            }
        };
        OptionDialog.showConfirmDialog(
                _ctx, BangCodes.BANG_MSGS, "m.account_success", new String[] { "m.restart" }, rr);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BTextField _username, _email;
    protected BPasswordField _password, _repassword;
    protected StatusLabel _status;
    protected BButton _cancel, _create;
    protected BComboBox _years, _months, _days;
    protected BLabel _monthL, _dayL;
    protected boolean _onExit, _over13, _textIn;
    protected Date _birthdate;
}
