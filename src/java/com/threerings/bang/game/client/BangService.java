//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Defines the requests that the client can make to the server.
 */
public interface BangService extends InvocationService
{
    /** Used to respond to a {@link #getBoard} request. */
    public interface BoardListener extends InvocationListener
    {
        /**
         * A response to a {@link #getBoard} request.
         */
        public void requestProcessed (BangBoard board, Piece[] pieces);
    }
    
    /**
     * Requests a copy of the board data.
     */
    public void getBoard (Client client, BoardListener listener);
    
    /**
     * Used to select a player's big shot and starting hand in the
     * pre-game phase.
     */
    public void selectStarters (Client client, int bigShotId, int[] cardIds);

    /**
     * Used to select the rest of a player's team in the pre-game phase.
     */
    public void selectTeam (Client client, String[] units);

    /**
     * Issues an order to a particular unit to do some combination of moving
     * and shooting.
     *
     * @param targetId the id of the piece on which to fire after moving
     * or -1 if no firing is desired.
     */
    public void order (Client client, int pieceId, short x, short y,
                      int targetId, ResultListener listener);

    /**
     * Requests that the specified units advance order be canceled.
     */
    public void cancelOrder (Client client, int pieceId);

    /**
     * Requests that the specified card be activated at the specified
     * location.
     */
    public void playCard (Client client, int cardId, short x, short y);
}
