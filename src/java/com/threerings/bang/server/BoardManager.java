//
// $Id$

package com.threerings.bang.server;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Invoker;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.ResultListenerList;
import com.samskivert.util.StringUtil;

import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.server.persist.BoardRecord;
import com.threerings.bang.server.persist.BoardRepository.BoardList;
import com.threerings.bang.server.persist.BoardRepository;
import com.threerings.bang.util.BangUtil;

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
        _byname = new BoardMap[GameCodes.MAX_PLAYERS-1];
        for (int ii = 0; ii < _byname.length; ii++) {
            _byname[ii] = new BoardMap();
        }

        // we'll use this later to filter out boards that are not allowed in this town
        int townIndex = BangUtil.getTownIndex(ServerConfig.townId);

        // load up and map all of our boards by scenario and player count
        for (BoardRecord record : _brepo.loadBoards()) {
            // sanity check boards as creators are known to fuck up
            if (record.players < 2 || record.players > GameCodes.MAX_PLAYERS) {
                log.warning("Invalid board record [record=" + record + "].");
                continue;
            }

            // if this board uses scenarios from a later town, skip it
            if (record.getMinimumTownIndex() > townIndex) {
                continue;
            }

            int pidx = record.players-2;
            String[] scenarios = StringUtil.split(record.scenarios, ",");
            for (int ii = 0; ii < scenarios.length; ii++) {
                BoardList[] lists = _byscenario.get(scenarios[ii]);
                if (lists == null) {
                    _byscenario.put(scenarios[ii], lists = new BoardList[GameCodes.MAX_PLAYERS-1]);
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
     * Randomly selects a set of boards for play given the required number of players and the
     * specified sequence of scenarios.
     */
    public BoardRecord[] selectBoards (int players, String[] scenarios, int[] prevBoardIds)
    {
        ArrayIntSet prevIds = new ArrayIntSet(prevBoardIds);
        BoardRecord[] choices = new BoardRecord[scenarios.length];
        for (int ii = 0; ii < scenarios.length; ii++) {
            if (choices[ii] != null) {
                continue;
            }

            // select the set of boards that work for this scenario and this number of players;
            // then shuffle that list
            String scenario = scenarios[ii];
            BoardList[] candvec = _byscenario.get(scenario);
            BoardList candidates = (candvec == null) ? null : candvec[players-2];
            if (candidates == null) {
                log.warning("Aiya! Missing boards [players=" + players +
                            ", scenario=" + scenario + "].");
                continue;
            }

            // we'll remove previously played boards, so we can't do that to the canonical list
            candidates = (BoardList)candidates.clone();

            // remove boards in our previous board list unless it is the last board available
            for (Iterator<BoardRecord> iter = candidates.iterator(); iter.hasNext(); ) {
                if (candidates.size() <= 1) {
                    break;
                }
                BoardRecord brec = iter.next();
                if (prevIds.contains(brec.boardId)) {
                    iter.remove();
                }
            }
            Collections.shuffle(candidates);

            // now fill in all instances of this scenario with (non-duplicate) selections from the
            // shuffled list
            int bidx = 0;
            for (int bb = ii; bb < scenarios.length; bb++) {
                if (scenarios[bb].equals(scenario)) {
                    choices[bb] = candidates.get(bidx++ % candidates.size());
                }
            }
        }
        return choices;
    }

    /**
     * Returns the version of the specified named board appropriate for the specified number of
     * players, or null if no such board exists.
     */
    public BoardRecord getBoard (int pcount, String name)
    {
        return _byname[pcount-2].get(name);
    }

    /**
     * Returns the set of all boards with the specified player count.
     */
    public Collection<BoardRecord> getBoards (int pcount)
    {
        return _byname[pcount-2].values();
    }

    /**
     * Loads the board data for the specified board, notifying the given result listener when
     * finished.
     */
    public void loadBoardData (final BoardRecord brec, ResultListener<BoardRecord> listener)
    {
        // if there's already a list of listeners waiting for the data, put the listener on it and
        // return; otherwise, create and map the list and post an invoker to load the data
        ResultListenerList<BoardRecord> rll = _boardDataListeners.get(brec);
        if (rll != null) {
            rll.add(listener);
            return;
        }

        final ResultListenerList<BoardRecord> list = new ResultListenerList<BoardRecord>();
        _boardDataListeners.put(brec, list);
        list.add(listener);
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _brepo.loadBoardData(brec);
                } catch (PersistenceException pe) {
                    _error = pe;
                }
                return true;
            }

            public void handleResult () {
                if (_error == null) {
                    list.requestCompleted(brec);
                } else {
                    list.requestFailed(_error);
                }
                _boardDataListeners.remove(brec);
            }
            protected Exception _error;
        });
    }

    /** Used for our name to board mapping. */
    protected static class BoardMap extends HashMap<String,BoardRecord>
    {
    }

    /** Provides access to the board database. */
    protected BoardRepository _brepo;

    /** A mapping from scenario name to a list of boards playable with that scenario (which are
     * broken out by player count). */
    protected HashMap<String,BoardList[]> _byscenario = new HashMap<String,BoardList[]>();

    /** A mapping by board name, broken out by player count. */
    protected BoardMap[] _byname;

    /** Maps board records to lists of {@link ResultListener}s waiting for the invoker to load the
     * record's board data from the database. */
    protected HashMap<BoardRecord, ResultListenerList<BoardRecord>> _boardDataListeners =
        new HashMap<BoardRecord, ResultListenerList<BoardRecord>>();
}
