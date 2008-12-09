//
// $Id$

package com.threerings.bang.server;

import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.Handle;
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
     * Handles a {@link PlayerService#bootPlayer} request.
     */
    void bootPlayer (ClientObject caller, Handle arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#createAccount} request.
     */
    void createAccount (ClientObject caller, String arg1, String arg2, String arg3, String arg4, long arg5, InvocationService.ConfirmListener arg6)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#destroyItem} request.
     */
    void destroyItem (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#getPosterInfo} request.
     */
    void getPosterInfo (ClientObject caller, Handle arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#invitePardner} request.
     */
    void invitePardner (ClientObject caller, Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#noteFolk} request.
     */
    void noteFolk (ClientObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#pickFirstBigShot} request.
     */
    void pickFirstBigShot (ClientObject caller, String arg1, Name arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playBountyGame} request.
     */
    void playBountyGame (ClientObject caller, String arg1, String arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playComputer} request.
     */
    void playComputer (ClientObject caller, int arg1, String[] arg2, String arg3, boolean arg4, InvocationService.InvocationListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playPractice} request.
     */
    void playPractice (ClientObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playTutorial} request.
     */
    void playTutorial (ClientObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#prepSongForDownload} request.
     */
    void prepSongForDownload (ClientObject caller, String arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#registerComplaint} request.
     */
    void registerComplaint (ClientObject caller, Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#removePardner} request.
     */
    void removePardner (ClientObject caller, Handle arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#respondToNotification} request.
     */
    void respondToNotification (ClientObject caller, Comparable<?> arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#updatePosterInfo} request.
     */
    void updatePosterInfo (ClientObject caller, int arg1, String arg2, int[] arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;
}
