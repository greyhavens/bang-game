//
// $Id$

package com.threerings.bang.saloon.data;

import java.lang.Comparable;

import com.threerings.presents.dobj.DSet;

/**
 * Provides access to an object's lists of top-ranked players.
 */
public interface TopRankObject
{
    /**
     * Returns a reference to the set of top-ranked lists.
     */
    public DSet<TopRankedList> getTopRanked ();

    /**
     * Adds an entry to the set of top-ranked lists.
     */
    public void addToTopRanked (TopRankedList list);

    /**
     * Removes an entry from the set of top-ranked lists.
     */
    public void removeFromTopRanked (Comparable<?> key);

    /**
     * Updates an entry in the set of top-ranked lists.
     */
    public void updateTopRanked (TopRankedList list);
}
