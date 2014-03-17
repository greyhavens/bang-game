//
// $Id$

package com.threerings.bang.client.bui;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.util.BangContext;

/**
 * Displays a dialog allowing a player to confirm their desire to do something
 * before firing off a request.
 */
public abstract class RequestDialog extends OptionDialog
    implements OptionDialog.ResponseReceiver, InvocationService.ConfirmListener
{
    /**
     * Creates a new request dialog.
     *
     * @param success the translatable message to display on successful return
     */
    public RequestDialog (
        BangContext ctx, String bundle, String text, String ok,
        String cancel, String success, StatusLabel status)
    {
        super(ctx, bundle, text, new String[] { ok, cancel }, null);
        _bundle = bundle;
        _success = success;
        _status = status;
        _receiver = this;
    }

    // from interface OptionDialog.ResponseReceiver
    public void resultPosted (int button, Object result)
    {
        if (button == OK_BUTTON) {
            fireRequest(result);
        }
    }

    // documentation inherited from interface InvocationService.ConfirmListener
    public void requestProcessed ()
    {
        displayStatus(_success, false);
    }
    
    // documentation inherited from interface InvocationService.ConfirmListener
    public void requestFailed (String cause)
    {
        displayStatus(cause, true);
    }
    
    /**
     * Fires off the service request with this dialog as its listener.
     */
    protected abstract void fireRequest (Object result);
    
    protected void displayStatus (String message, boolean flash)
    {
        if (_status == null) {
            _ctx.getChatDirector().displayFeedback(_bundle, message);
        } else {
            _status.setStatus(_bundle, message, flash);
        }
    }

    protected String _bundle, _success;
    protected StatusLabel _status;
}
