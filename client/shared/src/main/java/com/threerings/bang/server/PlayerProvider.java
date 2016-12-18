//
// $Id$

package com.threerings.bang.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

/**
 * Defines the server-side of the {@link PlayerService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from PlayerService.java.")
public interface PlayerProvider extends InvocationProvider
{
    /**
     * Handles a {@link PlayerService#bootPlayer} request.
     */
    void bootPlayer (PlayerObject caller, Handle arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#createAccount} request.
     */
    void createAccount (PlayerObject caller, String arg1, String arg2, String arg3, String arg4, long arg5, InvocationService.ConfirmListener arg6)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#destroyItem} request.
     */
    void destroyItem (PlayerObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#getPosterInfo} request.
     */
    void getPosterInfo (PlayerObject caller, Handle arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#invitePardner} request.
     */
    void invitePardner (PlayerObject caller, Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#noteFolk} request.
     */
    void noteFolk (PlayerObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playBountyGame} request.
     */
    void playBountyGame (PlayerObject caller, String arg1, String arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playComputer} request.
     */
    void playComputer (PlayerObject caller, int arg1, String[] arg2, String arg3, boolean arg4, InvocationService.InvocationListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playPractice} request.
     */
    void playPractice (PlayerObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#playTutorial} request.
     */
    void playTutorial (PlayerObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#prepSongForDownload} request.
     */
    void prepSongForDownload (PlayerObject caller, String arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#removePardner} request.
     */
    void removePardner (PlayerObject caller, Handle arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#respondToNotification} request.
     */
    void respondToNotification (PlayerObject caller, Comparable<?> arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PlayerService#updatePosterInfo} request.
     */
    void updatePosterInfo (PlayerObject caller, int arg1, String arg2, int[] arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;
}
