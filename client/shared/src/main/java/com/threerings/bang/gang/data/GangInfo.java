//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.Handle;

/**
 * Contains the contents of a gang's info page.
 */
public class GangInfo extends SimpleStreamableObject
{
    /** Contains information on a single gang member. */
    public static class Member extends SimpleStreamableObject
    {
        /** The member's name. */
        public Handle handle;

        /** Whether or not the member is currently active. */
        public boolean active;

        public Member (Handle handle, boolean active)
        {
            this.handle = handle;
            this.active = active;
        }

        public Member ()
        {
        }
    }

    /** The name of the gang. */
    public Handle name;

    /** The day on which this gang was founded. */
    public long founded;

    /** The gang's weight class. */
    public byte weightClass;

    /** The gang's notoriety level. */
    public byte notoriety;

    /** The gang's statement. */
    public String statement;

    /** The gang's URL. */
    public String url;

    /** The gang's buckle. */
    public BuckleInfo buckle;

    /** The gang leader's avatar. */
    public AvatarInfo avatar;

    /** The presorted leaders of the gang. */
    public Member[] leaders;

    /** The presorted normal members of the gang. */
    public Member[] members;
}
