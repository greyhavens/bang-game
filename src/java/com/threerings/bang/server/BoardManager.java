//
// $Id$

package com.threerings.bang.server;

import java.util.Collections;
import java.util.HashMap;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.StringUtil;

import com.threerings.util.RandomUtil;

import com.threerings.bang.data.BangCodes;
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
        _byname = new BoardMap[BangCodes.MAX_PLAYERS-1];
        for (int ii = 0; ii < _byname.length; ii++) {
            _byname[ii] = new BoardMap();
        }

        // load up and map all of our boards by scenario and player count
        for (BoardRecord record : _brepo.loadBoards()) {
            int pidx = record.players-1;
            String[] scenarios = StringUtil.split(record.scenarios, ",");
            for (int ii = 0; ii < scenarios.length; ii++) {
                BoardList[] lists = _byscenario.get(scenarios[ii]);
                if (lists == null) {
                    _byscenario.put(
                        scenarios[ii],
                        lists = new BoardList[BangCodes.MAX_PLAYERS]);
                }
                if (lists[pidx] == null) {
                    lists[pidx] = new BoardList();
                }
                lists[pidx].add(record);
            }
            _byname[pidx].put(record.name, record);
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
            if (choices[ii] != null) {
                continue;
            }

            // select the set of boards that work for this scenario and this
            // number of players; then shuffle that list
            String scenario = scenarios[ii];
            BoardList[] candvec = _byscenario.get(scenario);
            BoardList candidates = (candvec == null) ? null : candvec[players-1];
            if (candidates == null) {
                log.warning("Aiya! Missing boards [players=" + players +
                            ", scenario=" + scenario + "].");
                continue;
            }
            Collections.shuffle(candidates);

            // now fill in all instances of this scenario with (non-duplicate)
            // selections from the shuffled list
            int bidx = 0;
            for (int bb = ii; bb < scenarios.length; bb++) {
                if (scenarios[ii].equals(scenario)) {
                    choices[ii] = candidates.get(bidx++);
                }
            }
        }
        return choices;
    }

    /**
     * Returns the version of the specified named board appropriate for the
     * specified number of players, or null if no such board exists.
     */
    public BoardRecord getBoard (int pcount, String name)
    {
        return _byname[pcount-1].get(name);
    }

    /** Used for our name to board mapping. */
    protected static class BoardMap extends HashMap<String,BoardRecord>
    {
    }

    /** Provides access to the board database. */
    protected BoardRepository _brepo;

    /** A mapping from scenario name to a list of boards playable with
     * that scenario (which are broken out by player count). */
    protected HashMap<String,BoardList[]> _byscenario =
        new HashMap<String,BoardList[]>();

    /** A mapping by board name, broken out by player count. */
    protected BoardMap[] _byname;
}
