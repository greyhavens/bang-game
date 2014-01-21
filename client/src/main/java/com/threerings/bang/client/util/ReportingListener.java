//
// $Id$

package com.threerings.bang.client.util;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.util.BangContext;

/**
 * Displays a feedback message to the chat window when the server reports success or failure.
 */
public class ReportingListener
    implements InvocationService.ConfirmListener
{
    /**
     * Creates a listener that will report on failure.
     *
     * @param bundle the message bundle to use for translation.
     * @param errwrap the wrapper message for the error report.
     */
    public ReportingListener (BangContext ctx, String bundle, String errwrap)
    {
        this(ctx, bundle, null, errwrap);
    }

    /**
     * Creates a listener that will report on success or failure.
     *
     * @param bundle the bundle to use for translation.
     * @param success the message to report on success.
     * @param errwrap the wrapper message for the error report.
     */
    public ReportingListener (BangContext ctx, String bundle, String success, String errwrap)
    {
        _ctx = ctx;
        _bundle = bundle;
        _success = success;
        _errwrap = errwrap;
    }

    // from interface InvocationService.ConfirmListener
    public void requestProcessed ()
    {
        if (_success != null) {
            _ctx.getChatDirector().displayFeedback(_bundle, _success);
        }
    }

    // from interface InvocationService.ConfirmListener
    public void requestFailed (String cause)
    {
        _ctx.getChatDirector().displayFeedback(_bundle, MessageBundle.compose(_errwrap, cause));
    }

    protected BangContext _ctx;
    protected String _bundle, _success, _errwrap;
}
