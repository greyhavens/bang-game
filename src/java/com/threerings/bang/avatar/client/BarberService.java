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
     * @param hair the global hair colorization id.
     * @param skin the global skin colorization id.
     * @param aspects an array of aspect selections that must be in the order
     * specified by {@link AvatarMetrics#ASPECTS}, unused aspects should have a
     * null slot in the array.
     * @param colors color ids associated with each of the specified aspects.
     * Many of these will be 0 as those aspects have no special colorization.
     */
    public void purchaseLook (
        Client client, String name, int hair, int skin, String[] aspects,
        int[] colors, ConfirmListener cl);

    /**
     * Requests that the specified look be configured with the specified set of
     * articles. No confirmation is provided as the client will have already
     * applied the changes locally.
     *
     * @param name the name of the look to be configured.
     * @param articles item ids of the article inventory items to be used to
     * configure the look.
     */
    public void configureLook (Client client, String name, int[] articles);
}
