//
// $Id$

package com.samskivert.bang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.samskivert.bang.data.PiecePath;
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
     * Requests that the specified piece be moved along the specified path.
     */
    public void setPath (Client client, PiecePath path);
}
