//
// $Id$

package com.threerings.bang.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.client.BangService;
import com.threerings.bang.data.piece.Piece;

/**
 * Defines the server side of the {@link BangService}.
 */
public interface BangProvider extends InvocationProvider
{
    /** Handles a {@link BangService#purchasePiece} request. */
    public void purchasePieces (ClientObject caller, Piece[] pieces);

    /** Handles a {@link BangService#readyToPlay} request. */
    public void move (ClientObject caller, int pieceId, short x, short y,
                      int targetId, BangService.InvocationListener il)
        throws InvocationException;

    /** Handles a {@link BangService#surprise} request. */
    public void surprise (ClientObject caller, int surpriseId, short x, short y);
}
