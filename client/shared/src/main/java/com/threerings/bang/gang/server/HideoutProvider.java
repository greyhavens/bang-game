//
// $Id$

package com.threerings.bang.gang.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.gang.client.HideoutService;
import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.saloon.data.Criterion;

/**
 * Defines the server-side of the {@link HideoutService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from HideoutService.java.")
public interface HideoutProvider extends InvocationProvider
{
    /**
     * Handles a {@link HideoutService#addToCoffers} request.
     */
    void addToCoffers (PlayerObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#broadcastToMembers} request.
     */
    void broadcastToMembers (PlayerObject caller, String arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#buyGangGood} request.
     */
    void buyGangGood (PlayerObject caller, String arg1, Object[] arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#buyOutfits} request.
     */
    void buyOutfits (PlayerObject caller, OutfitArticle[] arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#changeMemberRank} request.
     */
    void changeMemberRank (PlayerObject caller, Handle arg1, byte arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#changeMemberTitle} request.
     */
    void changeMemberTitle (PlayerObject caller, Handle arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#expelMember} request.
     */
    void expelMember (PlayerObject caller, Handle arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#findMatch} request.
     */
    void findMatch (PlayerObject caller, Criterion arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#formGang} request.
     */
    void formGang (PlayerObject caller, Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#getHistoryEntries} request.
     */
    void getHistoryEntries (PlayerObject caller, int arg1, String arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#getOutfitQuote} request.
     */
    void getOutfitQuote (PlayerObject caller, OutfitArticle[] arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#getUpgradeQuote} request.
     */
    void getUpgradeQuote (PlayerObject caller, GangGood arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#leaveGang} request.
     */
    void leaveGang (PlayerObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#leaveMatch} request.
     */
    void leaveMatch (PlayerObject caller, int arg1);

    /**
     * Handles a {@link HideoutService#renewGangItem} request.
     */
    void renewGangItem (PlayerObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#rentGangGood} request.
     */
    void rentGangGood (PlayerObject caller, String arg1, Object[] arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#setBuckle} request.
     */
    void setBuckle (PlayerObject caller, BucklePart[] arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#setStatement} request.
     */
    void setStatement (PlayerObject caller, String arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;
}
