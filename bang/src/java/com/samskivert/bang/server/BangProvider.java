//
// $Id$

package com.samskivert.bang.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationProvider;

import com.samskivert.bang.client.BangService;
import com.samskivert.bang.data.PiecePath;
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

    /** Handles a {@link BangService#setPath} request. */
    public void setPath (ClientObject caller, PiecePath path);
}
