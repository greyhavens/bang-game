//
// $Id$

package com.threerings.bang.server;

import java.util.ArrayList;
import java.util.HashMap;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.StringUtil;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.server.persist.BoardRecord;
import com.threerings.bang.server.persist.BoardRepository.BoardList;
import com.threerings.bang.server.persist.BoardRepository;

/**
 * Manages the boards available to the Bang server.
 */
public class BoardManager
{
    public BoardManager (ConnectionProvider conprov)
        throws PersistenceException
    {
        _brepo = new BoardRepository(conprov);

        // load up and map all of our boards by scenario and player count
        for (BoardRecord record : _brepo.loadBoards()) {
            BoardList list = _byplayers[record.players-1];
            if (list == null) {
                _byplayers[record.players-1] =
                    (list = new BoardList());
            }
            list.add(record);

            String[] scenarios = StringUtil.split(record.scenarios, ",");
            for (int ii = 0; ii < scenarios.length; ii++) {
                list = _byscenario.get(scenarios[ii]);
                if (list == null) {
                    _byscenario.put(
                        scenarios[ii], list = new BoardList());
                }
                list.add(record);
            }
        }
    }

    /** Provides access to the board database. */
    protected BoardRepository _brepo;

    /** A mapping from scenario name to a list of boards playable with
     * that scenario. */
    protected HashMap<String,BoardList> _byscenario =
        new HashMap<String,BoardList>();

    /** Lists of boards playable with <code>index+1</code> players. */
    protected BoardList[] _byplayers = new BoardList[BangCodes.MAX_PLAYERS];
}
