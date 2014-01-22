//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.data.BoardData;

/**
 * Defines the requests that the client can make to the server.
 */
public interface BangService extends InvocationService<PlayerObject>
{
    /** Used to respond to a {@link #getBoard} request. */
    public interface BoardListener extends InvocationListener
    {
        /**
         * A response to a {@link #getBoard} request.
         */
        public void requestProcessed (BoardData bdata);
    }

    /**
     * Requests a copy of the board data.
     */
    public void getBoard (BoardListener listener);

    /**
     * Used to configure a player's team during the pre-game phase.
     */
    public void selectTeam (int bigShotId, String[] units, int[] cardIds);

    /**
     * Issues an order to a particular unit to do some combination of moving
     * and shooting.
     *
     * @param targetId the id of the piece on which to fire after moving
     * or -1 if no firing is desired.
     */
    public void order (int pieceId, short x, short y, int targetId, ResultListener listener);

    /**
     * Requests that the specified units advance order be canceled.
     */
    public void cancelOrder (int pieceId);

    /**
     * Requests that the specified card be activated at the specified
     * location.
     */
    public void playCard (int cardId, Object target, ConfirmListener listener);

    /**
     * Used to report on graphics performance by the client at the end of a
     * round.
     *
     * @param board the name of the board the player was playing on.
     * @param driver the client's graphics driver information.
     * @param perfhisto a histogram of one second samples of frame rate, each
     * bucket represents 10 fps (0-9, 10-19, 20-29, etc.) up to 60 fps for a
     * total of seven buckets. The client samples FPS every second and
     * increments the counter in the appropriate bucket.
     */
    public void reportPerformance (String board, String driver, int[] perfhisto);
}
