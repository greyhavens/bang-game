//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Defines the requests that the client can make to the server.
 */
public interface BangService extends InvocationService
{
    /**
     * Used to select a player's big shot and starting hand in the
     * pre-game phase.
     */
    public void selectStarters (Client client, int bigShotId, int[] cardIds);

    /**
     * Used to purchase pieces during the pre-game buying phase.
     */
    public void purchasePieces (Client client, Piece[] pieces);

    /**
     * Requests that a piece be moved to the specified location.
     *
     * @param targetId the id of the piece on which to fire after moving
     * or -1 if no firing is desired.
     */
    public void move (Client client, int pieceId, short x, short y,
                      int targetId, InvocationListener listener);

    /**
     * Requests that the specified card be activated at the specified
     * location.
     */
    public void playCard (Client client, int cardId, short x, short y);
}
