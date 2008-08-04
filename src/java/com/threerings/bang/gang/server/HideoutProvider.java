//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.HideoutService;
import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link HideoutService}.
 */
public interface HideoutProvider extends InvocationProvider
{
    /**
     * Handles a {@link HideoutService#addToCoffers} request.
     */
    void addToCoffers (ClientObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#broadcastToMembers} request.
     */
    void broadcastToMembers (ClientObject caller, String arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#buyGangGood} request.
     */
    void buyGangGood (ClientObject caller, String arg1, Object[] arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#buyOutfits} request.
     */
    void buyOutfits (ClientObject caller, OutfitArticle[] arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#changeMemberRank} request.
     */
    void changeMemberRank (ClientObject caller, Handle arg1, byte arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#changeMemberTitle} request.
     */
    void changeMemberTitle (ClientObject caller, Handle arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#expelMember} request.
     */
    void expelMember (ClientObject caller, Handle arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#findMatch} request.
     */
    void findMatch (ClientObject caller, Criterion arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#formGang} request.
     */
    void formGang (ClientObject caller, Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#getHistoryEntries} request.
     */
    void getHistoryEntries (ClientObject caller, int arg1, String arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#getOutfitQuote} request.
     */
    void getOutfitQuote (ClientObject caller, OutfitArticle[] arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#getUpgradeQuote} request.
     */
    void getUpgradeQuote (ClientObject caller, GangGood arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#leaveGang} request.
     */
    void leaveGang (ClientObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#leaveMatch} request.
     */
    void leaveMatch (ClientObject caller, int arg1);

    /**
     * Handles a {@link HideoutService#postOffer} request.
     */
    void postOffer (ClientObject caller, int arg1, int arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#renewGangItem} request.
     */
    void renewGangItem (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#rentGangGood} request.
     */
    void rentGangGood (ClientObject caller, String arg1, Object[] arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#setBuckle} request.
     */
    void setBuckle (ClientObject caller, BucklePart[] arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#setStatement} request.
     */
    void setStatement (ClientObject caller, String arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;
}
