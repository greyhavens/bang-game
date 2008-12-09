//
// $Id$

package com.threerings.bang.bounty.data;

import com.threerings.presents.dobj.DSet;

/**
 * Contains a list of members who recently completed a bounty.
 */
public class RecentCompleters
    implements DSet.Entry
{
    /** The maximum number of recent completers shown. */
    public static final int MAX_COMPLETERS = 9;

    /** The id of the bounty for which we maintain a list. */
    public String bountyId;

    /** The recent completers of this bounty, in order. */
    public String[] handles;
    
    /**
     * Adds a recent completer to the end of the list, pushing the oldest completer off the front
     * if we exceed the maximum size.
     */
    public void addCompleter (String handle)
    {
        if (handles == null) {
            handles = new String[] { handle };
        } else if (handles.length < MAX_COMPLETERS) {
            String[] nhandles = new String[handles.length+1];
            System.arraycopy(handles, 0, nhandles, 0, handles.length);
            nhandles[handles.length] = handle;
            handles = nhandles;
        } else {
            System.arraycopy(handles, 1, handles, 0, handles.length-1);
            handles[handles.length-1] = handle;
        }
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return bountyId;
    }
}
