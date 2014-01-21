//
// $Id$

package com.threerings.bang.client.bui;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.Interval;
import com.samskivert.util.ListUtil;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * A generic dialog class for simple user interactions.
 */
public class OptionDialog extends BDecoratedWindow
    implements ActionListener, BangCodes, BangClient.NonClearablePopup
{
    /** The index of the OK button. */
    public static final int OK_BUTTON = 0;

    /** The index of the Cancel button. */
    public static final int CANCEL_BUTTON = 1;

    /** The callback mechanism for receiving dialog results. */
    public interface ResponseReceiver
    {
        /**
         * Reports the result of the dialog.
         *
         * @param button the index of the button pressed
         * @param result the input obtained from the user, if applicable
         */
        public void resultPosted (int button, Object result);
    }

    /**
     * Shows a confirmation dialog with the given text and the standard OK and cancel buttons.
     *
     * @param bundle the bundle to to use in translating the text
     * @param text the text to display in the center of the dialog
     * @param receiver a receiver to notify with the result
     */
    public static void showConfirmDialog (BangContext ctx, String bundle, String text,
                                          ResponseReceiver receiver)
    {
        showConfirmDialog(ctx, bundle, text, "m.ok", "m.cancel", receiver);
    }

    /**
     * Shows a confirmation dialog with the given text and two buttons with the provided labels.
     *
     * @param bundle the bundle to to use in translating the text
     * @param text the text to display in the center of the dialog
     * @param ok the label for the first (OK) button
     * @param cancel the label for the second (Cancel) button
     * @param receiver a receiver to notify with the result
     */
    public static void showConfirmDialog (BangContext ctx, String bundle, String text,
                                          String ok, String cancel, ResponseReceiver receiver)
    {
        showConfirmDialog(ctx, bundle, text, new String[] { ok, cancel }, receiver);
    }

    /**
     * Shows a confirmation dialog with the given text and buttons with the provided labels.
     *
     * @param bundle the bundle to to use in translating the text
     * @param text the text to display in the center of the dialog
     * @param buttons the labels for the buttons in the dialog
     * @param receiver a receiver to notify with the result
     */
    public static void showConfirmDialog (BangContext ctx, String bundle, String text,
                                          String[] buttons, ResponseReceiver receiver)
    {
        showConfirmDialog(ctx, bundle, null, text, buttons, receiver);
    }

    /**
     * Shows a confirmation dialog with the given text and buttons with the provided labels.
     *
     * @param bundle the bundle to to use in translating the text
     * @param title the title of the dialog
     * @param text the text to display in the center of the dialog
     * @param buttons the labels for the buttons in the dialog
     * @param receiver a receiver to notify with the result
     */
    public static void showConfirmDialog (BangContext ctx, String bundle, String title,
                                          String text, String[] buttons, ResponseReceiver receiver)
    {
        showConfirmDialog(ctx, bundle, title, text, buttons, 0, receiver);
    }

    /**
     * Shows a confirmation dialog with the given text and buttons with the provided labels.
     *
     * @param bundle the bundle to to use in translating the text
     * @param title the title of the dialog
     * @param text the text to display in the center of the dialog
     * @param buttons the labels for the buttons in the dialog
     * @param delay the delay in seconds before the buttons become active
     * @param receiver a receiver to notify with the result
     */
    public static void showConfirmDialog (BangContext ctx, String bundle, String title,
              String text, String[] buttons, int delay, ResponseReceiver receiver)
    {
        OptionDialog dialog = new OptionDialog(ctx, bundle, title, text, buttons, delay, receiver);
        ctx.getBangClient().displayPopup(dialog, true, 400, true);
    }

    /**
     * Shows a dialog that displays the given text and buttons with the provided labels and
     * requests a string input.
     *
     * @param bundle the bundle to to use in translating the text
     * @param text the text to display in the center of the dialog
     * @param buttons the labels for the buttons in the dialog
     * @param width the width of the text input box in pixels
     * @param defaultValue the default value to provide for the string
     * @param receiver a receiver to notify with the result
     */
    public static void showStringDialog (
        BangContext ctx, String bundle, String text, String[] buttons,
        int width, String defaultValue, ResponseReceiver receiver)
    {
        OptionDialog dialog = new OptionDialog(ctx, bundle, text, buttons, receiver);
        dialog.setRequiresString(width, defaultValue);
        ctx.getBangClient().displayPopup(dialog, true, 400, true);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        _ctx.getBangClient().clearPopup(this, true);
        String value = (_input == null) ? null : _input.getText();
        int button;
        if (event.getSource() == _input) {
            button = 0;
        } else {
            button = ListUtil.indexOf(_buttons, event.getSource());
        }
        if (_receiver != null) {
            _receiver.resultPosted(button, value);
        }
    }

    protected OptionDialog (BangContext ctx, String bundle, String text,
                            String[] buttons, ResponseReceiver receiver)
    {
        this(ctx, bundle, null, text, buttons, receiver);
    }

    protected OptionDialog (BangContext ctx, String bundle, String title, String text,
                            String[] buttons, ResponseReceiver receiver)
    {
        this(ctx, bundle, title, text, buttons, 0, receiver);
    }

    protected OptionDialog (BangContext ctx, String bundle, String title, String text,
                            String[] buttons, int delay, ResponseReceiver receiver)
    {
        super(ctx.getStyleSheet(), (title == null) ? null : ctx.xlate(bundle, title));
        ((GroupLayout)getLayoutManager()).setGap(20);
        setModal(true);
        _ctx = ctx;
        _receiver = receiver;
        _delay = delay;

        add(new BLabel(_ctx.xlate(bundle, text)));

        BContainer dpanel = new BContainer(GroupLayout.makeVert(GroupLayout.CENTER));
        BContainer bpanel = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.CENTER));
        _buttons = new BButton[buttons.length];
        for (int ii = 0; ii < buttons.length; ii++) {
            bpanel.add(_buttons[ii] =
                new BButton(ctx.xlate(bundle, buttons[ii])));
            _buttons[ii].addListener(this);
            if (delay > 0) {
                _buttons[ii].setEnabled(false);
            }
        }
        dpanel.add(bpanel);
        if (delay > 0) {
            _epanel = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
            _epanel.add(new BLabel(_ctx.xlate(BangCodes.BANG_MSGS, "m.enabled_in")));
            _epanel.add(_timeout = new BLabel("" + delay));
            dpanel.add(_epanel);
        }
        add(dpanel, GroupLayout.FIXED);
    }

    protected void setRequiresString (int width, String defaultValue)
    {
        add(1, _input = new BTextField(defaultValue,
                    BangUI.TEXT_FIELD_MAX_LENGTH), GroupLayout.FIXED);
        _input.addListener(this);
        _input.setPreferredWidth(width);
        _input.requestFocus();
    }

    @Override // from BComponent
    protected void wasAdded ()
    {
        super.wasAdded();

        if (_input != null && _input.isShowing()) {
            _input.requestFocus();
        }
        if (_delay > 0) {
            new Interval(_ctx.getClient().getRunQueue()) {
                public void expired () {
                    _delay--;
                    if (_delay <= 0) {
                        cancel();
                        _epanel.getParent().remove(_epanel);
                        for (BButton button : _buttons) {
                            button.setEnabled(true);
                        }
                        return;
                    }
                    _timeout.setText("" + _delay);
                }
            }.schedule(1000L, true);
        }
    }

    protected BangContext _ctx;
    protected ResponseReceiver _receiver;
    protected BTextField _input;
    protected BButton[] _buttons;
    protected int _delay;
    protected BLabel _timeout;
    protected BContainer _epanel;
}
