//
// $Id$

package com.threerings.bang.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.util.BoardFile;

import static com.threerings.bang.Log.log;

/**
 * Manages the boards available to the Bang server.
 */
@Singleton
public class BoardManager
{
    /**
     * Prepares the board manager for operation.
     */
    public void init ()
        throws PersistenceException
    {
        _byname = new BoardMap[GameCodes.MAX_PLAYERS-1];
        for (int ii = 0; ii < _byname.length; ii++) {
            _byname[ii] = new BoardMap();
        }
        // load up all the boards in the data/boards directory
        log.info("Loading boards...");
        mapBoards(new File(ServerConfig.serverRoot, "data/boards"));
    }

    protected void mapBoards (File dir) {
        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(".board")) mapBoard(file);
            else if (file.isDirectory()) mapBoards(file);
        }
    }

    protected void mapBoard (File source) {
        try {
            BoardFile file = BoardFile.loadFrom(source);
            // sanity check boards as creators are known to fuck up
            if (file.players < 2 || file.players > GameCodes.MAX_PLAYERS) {
                log.warning("Invalid number of players", "players", file.players, "file", file);
                return;
            }

            // if this board uses scenarios from a later town, skip it
            if (file.getMinimumTownIndex() > ServerConfig.townIndex) return;

            // all boards are registered by name
            int pidx = file.players-2;
            _byname[pidx].put(file.name, file);

            // private boards are not registered by scenario so that they are not included in the
            // random selection done for match made games
            if (file.privateBoard) return;

            for (String scen : file.scenarios) {
                BoardList[] lists = _byscenario.get(scen);
                if (lists == null) {
                    _byscenario.put(scen, lists = new BoardList[GameCodes.MAX_PLAYERS-1]);
                }
                if (lists[pidx] == null) {
                    lists[pidx] = new BoardList();
                }
                lists[pidx].add(file);
            }
        } catch (IOException ioe) {
            log.warning("Error reading board", "file", source, ioe);
        }
    }

    /**
     * Randomly selects a set of boards for play given the required number of players and the
     * specified sequence of scenarios.
     */
    public BoardFile[] selectBoards (int players, List<BangConfig.Round> rounds, Set<String> prevs)
    {
        BoardFile[] choices = new BoardFile[rounds.size()];
        for (int ii = 0; ii < choices.length; ii++) {
            BangConfig.Round round = rounds.get(ii);

            // if we already selected a choice when picking for an earlier round, skip it
            if (choices[ii] != null) continue;

            // if this round has board data provided, unserialize it
            if (round.bdata != null) {
                try {
                    choices[ii] = BoardFile.loadFrom(round.bdata);
                    continue;
                } catch (Exception e) {
                    log.warning("Failed to load board data", "round", round, e);
                }
            }

            // if this round has a board speciifed, load it
            if (round.board != null) {
                choices[ii] = getBoard(players, round.board);
                if (choices[ii] != null) {
                    continue;
                }
            }

            // otherwise, select the set of boards that work for this scenario and this number of
            // players; then shuffle that list
            String scenario = round.scenario;
            BoardList[] candvec = _byscenario.get(scenario);
            BoardList candidates = (candvec == null) ? null : candvec[players-2];
            if (candidates == null) {
                log.warning("Aiya! Missing boards", "players", players, "scenario", scenario);
                continue;
            }

            // we'll remove previously played boards, so we can't do that to the canonical list
            candidates = (BoardList)candidates.clone();

            // remove boards in our previous board list unless it is the last board available
            for (Iterator<BoardFile> iter = candidates.iterator(); iter.hasNext(); ) {
                if (candidates.size() <= 1) {
                    break;
                }
                BoardFile bfile = iter.next();
                if (prevs.contains(bfile.name)) {
                    iter.remove();
                }
            }
            Collections.shuffle(candidates);

            // now fill in all instances of this scenario with (non-duplicate) selections from the
            // shuffled list for rounds that also require a randomly selected board
            int bidx = 0;
            for (int bb = ii; bb < rounds.size(); bb++) {
                BangConfig.Round fr = rounds.get(bb);
                if (fr.board == null && fr.bdata == null && fr.scenario.equals(scenario)) {
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
    public BoardFile getBoard (int pcount, String name)
    {
        return _byname[pcount-2].get(name);
    }

    /**
     * Returns the set of all boards with the specified player count.
     */
    public Collection<BoardFile> getBoards (int pcount)
    {
        return _byname[pcount-2].values();
    }

    /** Used for our name to board mapping. */
    protected static class BoardList extends ArrayList<BoardFile> {}

    /** Used for our name to board mapping. */
    protected static class BoardMap extends HashMap<String,BoardFile> {}

    /** A mapping from scenario name to a list of boards playable with that scenario (which are
     * broken out by player count). */
    protected HashMap<String,BoardList[]> _byscenario = new HashMap<String,BoardList[]>();

    /** A mapping by board name, broken out by player count. */
    protected BoardMap[] _byname;
}
