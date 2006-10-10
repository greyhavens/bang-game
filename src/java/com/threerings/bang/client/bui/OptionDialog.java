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

import com.samskivert.util.ListUtil;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * A generic dialog class for simple user interactions.
 */
public class OptionDialog extends BDecoratedWindow
    implements ActionListener, BangCodes
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
     * Shows a confirmation dialog with the given text and the standard OK and
     * cancel buttons.
     *
     * @param bundle the bundle to to use in translating the text
     * @param text the text to display in the center of the dialog
     * @param receiver a receiver to notify with the result
     */
    public static void showConfirmDialog (BangContext ctx, String bundle,
        String text, ResponseReceiver receiver)
    {
        showConfirmDialog(ctx, bundle, text, "m.ok", "m.cancel", receiver);
    }

    /**
     * Shows a confirmation dialog with the given text and two buttons with the
     * provided labels.
     *
     * @param bundle the bundle to to use in translating the text
     * @param text the text to display in the center of the dialog
     * @param ok the label for the first (OK) button
     * @param cancel the label for the second (Cancel) button
     * @param receiver a receiver to notify with the result
     */
    public static void showConfirmDialog (BangContext ctx, String bundle,
        String text, String ok, String cancel, ResponseReceiver receiver)
    {
        showConfirmDialog(ctx, bundle, text, new String[] { ok, cancel },
            receiver);
    }

    /**
     * Shows a confirmation dialog with the given text and buttons with the
     * provided labels.
     *
     * @param bundle the bundle to to use in translating the text
     * @param text the text to display in the center of the dialog
     * @param buttons the labels for the buttons in the dialog
     * @param receiver a receiver to notify with the result
     */
    public static void showConfirmDialog (BangContext ctx, String bundle,
        String text, String[] buttons, ResponseReceiver receiver)
    {
        OptionDialog dialog =
            new OptionDialog(ctx, bundle, text, buttons, receiver);
        ctx.getBangClient().displayPopup(dialog, true, 400);
    }

    /**
     * Shows a dialog that displays the given text and buttons with the
     * provided labels and requests a string input.
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
        OptionDialog dialog =
            new OptionDialog(ctx, bundle, text, buttons, receiver);
        dialog.setRequiresString(width, defaultValue);
        ctx.getBangClient().displayPopup(dialog, true, 400);
    }

    protected OptionDialog (BangContext ctx, String bundle, String text,
        String[] buttons, ResponseReceiver receiver)
    {
        super(ctx.getStyleSheet(), null);
        setModal(true);
        _ctx = ctx;
        _receiver = receiver;

        add(new BLabel(_ctx.xlate(bundle, text)));

        BContainer bpanel = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.CENTER));
        _buttons = new BButton[buttons.length];
        for (int ii = 0; ii < buttons.length; ii++) {
            bpanel.add(_buttons[ii] =
                new BButton(ctx.xlate(bundle, buttons[ii])));
            _buttons[ii].addListener(this);
        }
        add(bpanel, GroupLayout.FIXED);
    }

    protected void setRequiresString (int width, String defaultValue)
    {
        add(1, _input = new BTextField(defaultValue, 
                    BangUI.TEXT_FIELD_MAX_LENGTH), GroupLayout.FIXED);
        _input.addListener(this);
        _input.setPreferredWidth(width);
        _input.requestFocus();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        _ctx.getBangClient().clearPopup(this, false);
        String value = (_input == null) ? null : _input.getText();
        int button;
        if (event.getSource() == _input) {
            button = 0;
        } else {
            button = ListUtil.indexOf(_buttons, event.getSource());
        }
        _receiver.resultPosted(button, value);
    }

    protected BangContext _ctx;
    protected ResponseReceiver _receiver;
    protected BTextField _input;
    protected BButton[] _buttons;
}
