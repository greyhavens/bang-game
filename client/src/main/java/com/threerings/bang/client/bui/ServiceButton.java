//
// $Id$

package com.threerings.bang.client.bui;

import com.google.common.base.Function;

import com.jmex.bui.BButton;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.client.NeedPremiumView;
import com.threerings.bang.util.BangContext;

/**
 * A button that results in a call to an invocation service. The button is automatically disabled
 * while the service call is pending.
 */
public abstract class ServiceButton extends BButton
{
    public ServiceButton (BangContext ctx, String text, Function<String, Void> reporter)
    {
        super(text);
        _ctx = ctx;
        _reporter = reporter;
        addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                setEnabled(!callService());
            }
        });
    }

    /**
     * Creates a service button with the specified label which will report failure via the chat
     * director.
     */
    public ServiceButton (final BangContext ctx, String text, final String errbundle)
    {
        this(ctx, text, new Function<String, Void>() {
            public Void apply (String reason) {
                ctx.getChatDirector().displayFeedback(errbundle, reason);
                return null;
            }
        });
    }

    /**
     * Creates a service button with the specified label which will report failure via the chat
     * director after wrapping it in the supplied container message.
     */
    public ServiceButton (final BangContext ctx, String text,
                          final String errbundle, final String errwrap)
    {
        this(ctx, text, new Function<String, Void>() {
            public Void apply (String reason) {
                ctx.getChatDirector().displayFeedback(
                    errbundle, MessageBundle.compose(errwrap, reason));
                return null;
            }
        });
    }

    /**
     * Creates a service button with the specified label which will report failure via the supplied
     * label.
     */
    public ServiceButton (final BangContext ctx, String text,
                          final String errbundle, final BLabel status)
    {
        this(ctx, text, new Function<String, Void>() {
            public Void apply (String reason) {
                status.setText(ctx.xlate(errbundle, reason));
                return null;
            }
        });
    }

    /**
     * Creates a service button with the specified label which will report failure via the supplied
     * status label.
     */
    public ServiceButton (final BangContext ctx, String text,
                          final String errbundle, final StatusLabel status)
    {
        this(ctx, text, new Function<String, Void>() {
            public Void apply (String reason) {
                status.setStatus(ctx.xlate(errbundle, reason), true);
                return null;
            }
        });
    }

    /**
     * Called by the button when it is clicked. This should perform any desired checks and then
     * call the desired invocation service using either {@link #getConfirmListener} or {@link
     * #getResultListener} to listen for the response.
     *
     * @return true if the service was called, false if the call was not made for some reason
     * (necessary data not provided in other interface elements, for example).
     */
    protected abstract boolean callService ();

    /**
     * Called when we receive a result from our invocation listener.
     *
     * @param result the result returned by the result listener or null if a confirm listener was
     * used.
     *
     * @return true if the button should be reenabled now that the call has succeeded, false if it
     * should remain disabled.
     */
    protected abstract boolean onSuccess (Object result);

    /**
     * Called when we receive failure from our invocation listener.
     *
     * @param reason the reason for failure provided by the listener.
     *
     * @return true if the button should be reenabled, false if it should remain disabled.
     */
    protected boolean onFailure (String reason)
    {
        _reporter.apply(reason);
        // potentially show our need coins or need onetime dialog
        NeedPremiumView.maybeShowNeedPremium(_ctx, reason);
        return true;
    }

    /**
     * Creates a result listener for use by {@link #callService}.
     */
    protected InvocationService.ResultListener createResultListener ()
    {
        return new InvocationService.ResultListener() {
            public void requestProcessed (Object result) {
                setEnabled(onSuccess(result));
            }
            public void requestFailed (String reason) {
                setEnabled(onFailure(reason));
            }
        };
    }

    /**
     * Creates a confirm listener for use by {@link #callService}.
     */
    protected InvocationService.ConfirmListener createConfirmListener ()
    {
        return new InvocationService.ConfirmListener() {
            public void requestProcessed () {
                setEnabled(onSuccess(null));
            }
            public void requestFailed (String reason) {
                setEnabled(onFailure(reason));
            }
        };
    }

    protected BangContext _ctx;
    protected Function<String, Void> _reporter;
}
