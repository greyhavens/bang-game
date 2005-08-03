//
// $Id$

package com.threerings.bang.server;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.StringUtil;

import com.threerings.util.RandomUtil;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.server.scenario.ScenarioFactory;
import com.threerings.bang.server.persist.BoardRecord;
import com.threerings.bang.server.persist.BoardRepository.BoardList;
import com.threerings.bang.server.persist.BoardRepository;

import static com.threerings.bang.Log.log;

/**
 * Manages the boards available to the Bang server.
 */
public class BoardManager
{
    /**
     * Prepares the board manager for operation.
     */
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        _brepo = new BoardRepository(conprov);

        // load up and map all of our boards by scenario and player count
        for (BoardRecord record : _brepo.loadBoards()) {
            int pidx = record.players-1;
            String[] scenarios = StringUtil.split(record.scenarios, ",");
            for (int ii = 0; ii < scenarios.length; ii++) {
                BoardList[] lists = _map.get(scenarios[ii]);
                if (lists == null) {
                    _map.put(scenarios[ii],
                             lists = new BoardList[BangCodes.MAX_PLAYERS]);
                }
                if (lists[pidx] == null) {
                    lists[pidx] = new BoardList();
                }
                lists[pidx].add(record);
            }
        }
    }

    /**
     * Randomly selects a set of boards for play given the required number
     * of players and the specified sequence of scenarios.
     */
    public BoardRecord[] selectBoards (int players, String[] scenarios)
    {
        BoardRecord[] choices = new BoardRecord[scenarios.length];
        for (int ii = 0; ii < scenarios.length; ii++) {
            BoardList candidates = _map.get(scenarios[ii])[players-1];
            if (candidates == null) {
                log.warning("Aiya! Missing boards [players=" + players +
                            ", scenario=" + scenarios[ii] + "].");
            } else {
                choices[ii] = (BoardRecord)RandomUtil.pickRandom(candidates);
            }
        }
        return choices;
    }

    /** Provides access to the board database. */
    protected BoardRepository _brepo;

    /** A mapping from scenario name to a list of boards playable with
     * that scenario (which are broken out by player count). */
    protected HashMap<String,BoardList[]> _map =
        new HashMap<String,BoardList[]>();
}
