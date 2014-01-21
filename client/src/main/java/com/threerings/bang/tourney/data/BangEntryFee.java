//
// $Id$

package com.threerings.bang.tourney.data;

import com.samskivert.util.ResultListener;

import com.threerings.crowd.data.BodyObject;

import com.threerings.parlor.tourney.data.EntryFee;

/**
 * Handles the entree fees for a bang tourney.
 */
public class BangEntryFee extends EntryFee
{
    // documentation inherited
    public String getDescription ()
    {
        return "";
    }

    // documentation inherited
    public boolean hasFee (BodyObject body)
    {
        return true;
    }

    // documentation inherited
    public void reserveFee (BodyObject body, ResultListener<Void> listener)
    {
    }

    // documentation inherited
    public void returnFee (BodyObject body)
    {
    }
}
