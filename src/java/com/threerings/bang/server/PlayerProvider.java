//
// $Id$

package com.threerings.bang.server;

import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.Handle;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.util.Name;

/**
 * Defines the server-side of the {@link PlayerService}.
 */
public interface PlayerProvider extends InvocationProvider
{
    /**
     * Handles a {@link PlayerService#getPosterInfo} request.
     */
    public void getPosterInfo (ClientObject caller, Handle arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#invitePardner} request.
     */
    public void invitePardner (ClientObject caller, Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#noteFolk} request.
     */
    public void noteFolk (ClientObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#pickFirstBigShot} request.
     */
    public void pickFirstBigShot (ClientObject caller, String arg1, Name arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playComputer} request.
     */
    public void playComputer (ClientObject caller, int arg1, String[] arg2, String arg3, boolean arg4, InvocationService.InvocationListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playPractice} request.
     */
    public void playPractice (ClientObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playTutorial} request.
     */
    public void playTutorial (ClientObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#removePardner} request.
     */
    public void removePardner (ClientObject caller, Handle arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#respondToNotification} request.
     */
    public void respondToNotification (ClientObject caller, Comparable arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#updatePosterInfo} request.
     */
    public void updatePosterInfo (ClientObject caller, int arg1, String arg2, int[] arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;
}
