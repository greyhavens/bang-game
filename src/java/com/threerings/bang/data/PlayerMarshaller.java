//
// $Id$

package com.threerings.bang.data;

import com.threerings.bang.client.PlayerService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.util.Name;

/**
 * Provides the implementation of the {@link PlayerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PlayerMarshaller extends InvocationMarshaller
    implements PlayerService
{
    /** The method id used to dispatch {@link #bootPlayer} requests. */
    public static final int BOOT_PLAYER = 1;

    // from interface PlayerService
    public void bootPlayer (Client arg1, Handle arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, BOOT_PLAYER, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #createAccount} requests. */
    public static final int CREATE_ACCOUNT = 2;

    // from interface PlayerService
    public void createAccount (Client arg1, String arg2, String arg3, String arg4, String arg5, long arg6, InvocationService.ConfirmListener arg7)
    {
        InvocationMarshaller.ConfirmMarshaller listener7 = new InvocationMarshaller.ConfirmMarshaller();
        listener7.listener = arg7;
        sendRequest(arg1, CREATE_ACCOUNT, new Object[] {
            arg2, arg3, arg4, arg5, Long.valueOf(arg6), listener7
        });
    }

    /** The method id used to dispatch {@link #destroyItem} requests. */
    public static final int DESTROY_ITEM = 3;

    // from interface PlayerService
    public void destroyItem (Client arg1, int arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DESTROY_ITEM, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getPosterInfo} requests. */
    public static final int GET_POSTER_INFO = 4;

    // from interface PlayerService
    public void getPosterInfo (Client arg1, Handle arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_POSTER_INFO, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #invitePardner} requests. */
    public static final int INVITE_PARDNER = 5;

    // from interface PlayerService
    public void invitePardner (Client arg1, Handle arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, INVITE_PARDNER, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #noteFolk} requests. */
    public static final int NOTE_FOLK = 6;

    // from interface PlayerService
    public void noteFolk (Client arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, NOTE_FOLK, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #pickFirstBigShot} requests. */
    public static final int PICK_FIRST_BIG_SHOT = 7;

    // from interface PlayerService
    public void pickFirstBigShot (Client arg1, String arg2, Name arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, PICK_FIRST_BIG_SHOT, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #playBountyGame} requests. */
    public static final int PLAY_BOUNTY_GAME = 8;

    // from interface PlayerService
    public void playBountyGame (Client arg1, String arg2, String arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, PLAY_BOUNTY_GAME, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #playComputer} requests. */
    public static final int PLAY_COMPUTER = 9;

    // from interface PlayerService
    public void playComputer (Client arg1, int arg2, String[] arg3, String arg4, boolean arg5, InvocationService.InvocationListener arg6)
    {
        ListenerMarshaller listener6 = new ListenerMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, PLAY_COMPUTER, new Object[] {
            Integer.valueOf(arg2), arg3, arg4, Boolean.valueOf(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #playPractice} requests. */
    public static final int PLAY_PRACTICE = 10;

    // from interface PlayerService
    public void playPractice (Client arg1, String arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, PLAY_PRACTICE, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #playTutorial} requests. */
    public static final int PLAY_TUTORIAL = 11;

    // from interface PlayerService
    public void playTutorial (Client arg1, String arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, PLAY_TUTORIAL, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #prepSongForDownload} requests. */
    public static final int PREP_SONG_FOR_DOWNLOAD = 12;

    // from interface PlayerService
    public void prepSongForDownload (Client arg1, String arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, PREP_SONG_FOR_DOWNLOAD, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #registerComplaint} requests. */
    public static final int REGISTER_COMPLAINT = 13;

    // from interface PlayerService
    public void registerComplaint (Client arg1, Handle arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, REGISTER_COMPLAINT, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #removePardner} requests. */
    public static final int REMOVE_PARDNER = 14;

    // from interface PlayerService
    public void removePardner (Client arg1, Handle arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, REMOVE_PARDNER, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #respondToNotification} requests. */
    public static final int RESPOND_TO_NOTIFICATION = 15;

    // from interface PlayerService
    public void respondToNotification (Client arg1, Comparable<?> arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, RESPOND_TO_NOTIFICATION, new Object[] {
            arg2, Integer.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #updatePosterInfo} requests. */
    public static final int UPDATE_POSTER_INFO = 16;

    // from interface PlayerService
    public void updatePosterInfo (Client arg1, int arg2, String arg3, int[] arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, UPDATE_POSTER_INFO, new Object[] {
            Integer.valueOf(arg2), arg3, arg4, listener5
        });
    }
}
