//
// $Id$

package com.threerings.bang.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.client.PlayerService;

/**
 * Provides the implementation of the {@link PlayerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from PlayerService.java.")
public class PlayerMarshaller extends InvocationMarshaller<PlayerObject>
    implements PlayerService
{
    /** The method id used to dispatch {@link #bootPlayer} requests. */
    public static final int BOOT_PLAYER = 1;

    // from interface PlayerService
    public void bootPlayer (Handle arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(BOOT_PLAYER, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #createAccount} requests. */
    public static final int CREATE_ACCOUNT = 2;

    // from interface PlayerService
    public void createAccount (String arg1, String arg2, String arg3, String arg4, long arg5, InvocationService.ConfirmListener arg6)
    {
        InvocationMarshaller.ConfirmMarshaller listener6 = new InvocationMarshaller.ConfirmMarshaller();
        listener6.listener = arg6;
        sendRequest(CREATE_ACCOUNT, new Object[] {
            arg1, arg2, arg3, arg4, Long.valueOf(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #destroyItem} requests. */
    public static final int DESTROY_ITEM = 3;

    // from interface PlayerService
    public void destroyItem (int arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(DESTROY_ITEM, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #getPosterInfo} requests. */
    public static final int GET_POSTER_INFO = 4;

    // from interface PlayerService
    public void getPosterInfo (Handle arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_POSTER_INFO, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #invitePardner} requests. */
    public static final int INVITE_PARDNER = 5;

    // from interface PlayerService
    public void invitePardner (Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(INVITE_PARDNER, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #noteFolk} requests. */
    public static final int NOTE_FOLK = 6;

    // from interface PlayerService
    public void noteFolk (int arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(NOTE_FOLK, new Object[] {
            Integer.valueOf(arg1), Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #playBountyGame} requests. */
    public static final int PLAY_BOUNTY_GAME = 7;

    // from interface PlayerService
    public void playBountyGame (String arg1, String arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(PLAY_BOUNTY_GAME, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #playComputer} requests. */
    public static final int PLAY_COMPUTER = 8;

    // from interface PlayerService
    public void playComputer (int arg1, String[] arg2, String arg3, boolean arg4, InvocationService.InvocationListener arg5)
    {
        ListenerMarshaller listener5 = new ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(PLAY_COMPUTER, new Object[] {
            Integer.valueOf(arg1), arg2, arg3, Boolean.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #playPractice} requests. */
    public static final int PLAY_PRACTICE = 9;

    // from interface PlayerService
    public void playPractice (String arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(PLAY_PRACTICE, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #playTutorial} requests. */
    public static final int PLAY_TUTORIAL = 10;

    // from interface PlayerService
    public void playTutorial (String arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(PLAY_TUTORIAL, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #prepSongForDownload} requests. */
    public static final int PREP_SONG_FOR_DOWNLOAD = 11;

    // from interface PlayerService
    public void prepSongForDownload (String arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(PREP_SONG_FOR_DOWNLOAD, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #removePardner} requests. */
    public static final int REMOVE_PARDNER = 12;

    // from interface PlayerService
    public void removePardner (Handle arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(REMOVE_PARDNER, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #respondToNotification} requests. */
    public static final int RESPOND_TO_NOTIFICATION = 13;

    // from interface PlayerService
    public void respondToNotification (Comparable<?> arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(RESPOND_TO_NOTIFICATION, new Object[] {
            arg1, Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #updatePosterInfo} requests. */
    public static final int UPDATE_POSTER_INFO = 14;

    // from interface PlayerService
    public void updatePosterInfo (int arg1, String arg2, int[] arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(UPDATE_POSTER_INFO, new Object[] {
            Integer.valueOf(arg1), arg2, arg3, listener4
        });
    }
}
