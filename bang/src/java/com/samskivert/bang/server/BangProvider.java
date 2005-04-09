//
// $Id$

package com.samskivert.bang.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.samskivert.bang.client.BangService;
import com.samskivert.bang.data.piece.Piece;

/**
 * Defines the server side of the {@link BangService}.
 */
public interface BangProvider extends InvocationProvider
{
    /** Handles a {@link BangService#purchasePiece} request. */
    public void purchasePiece (ClientObject caller, Piece piece);

    /** Handles a {@link BangService#readyToPlay} request. */
    public void readyToPlay (ClientObject caller);

    /** Handles a {@link BangService#readyToPlay} request. */
    public void move (ClientObject caller, int pieceId, short x, short y,
                      int targetId, BangService.InvocationListener il)
        throws InvocationException;

    /** Handles a {@link BangService#surprise} request. */
    public void surprise (ClientObject caller, int surpriseId, short x, short y);
}
