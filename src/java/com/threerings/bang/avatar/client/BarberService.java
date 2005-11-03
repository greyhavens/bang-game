//
// $Id$

package com.threerings.bang.avatar.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.avatar.data.Look;

/**
 * Provides Barber-related functionality.
 */
public interface BarberService extends InvocationService
{
    /**
     * Requests that the specified look be purchased.
     *
     * @param name the (player entered) name of the new look.
     * @param an array of aspect selections that must be in the order specified
     * by {@link AvatarMetrics#ASPECTS}, unused aspects should have a null slot
     * in the array.
     */
    public void purchaseLook (
        Client client, String name, String[] aspects, ConfirmListener cl);

    /**
     * Requests that the specified look be configured with the specified set of
     * articles.
     *
     * @param name the name of the look to be configured.
     * @param articles item ids of the article inventory items to be used to
     * configure the look.
     */
    public void configureLook (
        Client client, String name, int[] articles, ConfirmListener cl);
}
