//
// $Id$

package com.samskivert.bang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.samskivert.bang.data.piece.Piece;

/**
 * Defines the requests that the client can make to the server.
 */
public interface BangService extends InvocationService
{
    /**
     * Used to purchase a piece during the pre-game buying phase.
     */
    public void purchasePiece (Client client, Piece piece);

    /**
     * Indicates that the player is ready to proceed from the pre-game
     * buying phase to the main game phase.
     */
    public void readyToPlay (Client client);

    /**
     * Requests that a piece be moved to the specified location.
     */
    public void move (Client client, int pieceId, short x, short y);

    /**
     * Requests that the specified piece fire at the specified piece.
     */
    public void fire (Client client, int pieceId, int targetId);
}
