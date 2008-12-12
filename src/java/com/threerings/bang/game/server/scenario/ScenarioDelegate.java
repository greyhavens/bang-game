//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.List;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.server.BangManager;

/**
 * Handles aspects of a scenario's logic in a way that can be combined with other scenario
 * delegates.
 *
 * <p><em>Note:</em> this class is intentionally minimal. As we discover other aspects of scenarios
 * that need to be shared among various scenarios, we can expand the delegate to support them.
 */
public class ScenarioDelegate
{
    /**
     * Configures this delegate with its parent scenario.
     */
    public void init (BangManager bangmgr, Scenario parent)
    {
        _bangmgr = bangmgr;
        _parent = parent;
    }

    /**
     * Allows a delegate to filter out custom marker pieces and props and
     * adjust prop states prior to the start of the round.
     *
     * @param updates a list to populate with any pieces that were updated
     * during the filter process
     */
    public void filterPieces (BangObject bangobj, Piece[] starts, List<Piece> pieces,
                              List<Piece> updates)
    {
    }

    /**
     * Called at the start of every round. See {@link Scenario#roundWillStart}.
     */
    public void roundWillStart (BangObject bangobj)
        throws InvocationException
    {
    }

    /**
     * Called at the start of every game tick. See {@link Scenario#tick}.
     *
     * @return true if we should re-validate advance orders
     */
    public boolean tick (BangObject bangobj, short tick)
    {
        return false;
    }

    /**
     * Called when a piece is moved. See {@link Scenario#pieceMoved}.
     */
    public void pieceMoved (BangObject bangobj, Piece piece)
    {
    }

    /**
     * Called when a piece was killed. See {@link Scenario#pieceWasKilled}.
     */
    public void pieceWasKilled (BangObject bangobj, Piece piece, int shooter, int sidx)
    {
    }

    /**
     * Called when a piece was killed. See {@link Scenario#pieceWasRemoved}.
     */
    public void pieceWasRemoved (BangObject bangobj, Piece piece)
    {
    }

    /**
     * Called when a piece is affected.
     */
    public void pieceAffected (Piece piece, String effect)
    {
    }

    /**
     * Called after each round ends. See {@link Scenario#roundDidEnd}.
     */
    public void roundDidEnd (BangObject bangobj)
    {
    }

    /** A reference to the game manager we're working for. */
    protected BangManager _bangmgr;

    /** A reference to the scenario for whom we're delegating. */
    protected Scenario _parent;
}
