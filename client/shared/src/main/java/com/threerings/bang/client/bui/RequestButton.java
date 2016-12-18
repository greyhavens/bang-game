//
// $Id$

package com.threerings.bang.client.bui;

import com.jmex.bui.BButton;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.util.BangContext;

/**
 * A button that, when pressed, disables itself and makes a service request.
 * When the server reports back, the button reenables itself and reports the
 * error message (if any) to a provided status label.  Allows inserting a
 * confirmation dialog in the process to double-check that the user wants the
 * request to occur.
 */
public abstract class RequestButton extends BButton
    implements OptionDialog.ResponseReceiver, InvocationService.ConfirmListener
{
    /**
     * Creates a request button with no confirmation dialog.
     */
    public RequestButton (
        BangContext ctx, String bundle, String text, StatusLabel status)
    {
        this(ctx, bundle, text, null, status);
    }
    
    /**
     * Creates a request button with the provided confirmation message.
     */
    public RequestButton (
        BangContext ctx, String bundle, String text, String confirm,
        StatusLabel status)
    {
        super(ctx.xlate(bundle, text));
        _ctx = ctx;
        _bundle = bundle;
        _confirm = confirm;
        _status = status;
    }

    // documentation inherited from interface OptionDialog.ResponseReceiver
    public void resultPosted (int button, Object result)
    {
        if (button == OptionDialog.OK_BUTTON) {
            fireRequest();
        } else { // button == OptionDialog.CANCEL_BUTTON
            setEnabled(true);
        }
    }
    
    // documentation inherited from interface InvocationService.ConfirmListener
    public void requestProcessed ()
    {
        setEnabled(true);
    }
    
    // documentation inherited from interface InvocationService.ConfirmListener
    public void requestFailed (String cause)
    {
        setEnabled(true);
        _status.setStatus(_bundle, cause, true);
    }
    
    @Override // documentation inherited
    protected void fireAction (long when, int modifiers)
    {
        super.fireAction(when, modifiers);
        setEnabled(false);
        if (_confirm != null) {
            OptionDialog.showConfirmDialog(_ctx, _bundle, _confirm, this);
        } else {
            fireRequest();
        }
    }
 
    /**
     * Fires off the service request with this button as its listener.
     */
    protected abstract void fireRequest ();

    protected BangContext _ctx;
    protected String _bundle, _confirm;
    protected StatusLabel _status;
}
