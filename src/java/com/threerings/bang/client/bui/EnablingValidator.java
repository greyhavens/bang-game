//
// $Id$

package com.threerings.bang.client.bui;

import com.jmex.bui.BButton;
import com.jmex.bui.BTextComponent;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;

/**
 * Enables or disables a button based on the changing contents of a text field.
 */
public class EnablingValidator
    implements TextListener
{
    public static void validate (BTextComponent source, BButton button)
    {
        button.setEnabled(checkEnabled(source.getText()));
    }

    /**
     * Creates an enabling validator that will enable or disable the supplied
     * button based on the contents of the supplied text component. The
     * validator will automatically configure itself as a listener on the
     * supplied text component. The button will be initially enabled or
     * disabled by this constructor.
     */
    public EnablingValidator (BTextComponent source, BButton button)
    {
        _source = source;
        _source.addListener(this);
        _button = button;
        validate(source, button);
    }

    // documentation inherited from interface TextListener
    public void textChanged (TextEvent event)
    {
        String text = ((BTextComponent)event.getSource()).getText();
        _button.setEnabled(checkEnabled(text));
    }

    protected static boolean checkEnabled (String text)
    {
        return (text != null) && (text.trim().length() > 0);
    }

    protected BTextComponent _source;
    protected BButton _button;
}
