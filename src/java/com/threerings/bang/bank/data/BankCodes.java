//
// $Id$

package com.threerings.bang.bank.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the Bank.
 */
public interface BankCodes extends InvocationCodes
{
    /** The identifier for our message bundle. */
    public static final String BANK_MSGS = "bank";

    /** An error response returned by the bank service. */
    public static final String NO_SUCH_OFFER = "m.no_such_offer";

    /** An event name for a modified offer. */
    public static final String OFFER_MODIFIED = "offer_modified";

    /** An event name for a destroyed offers. */
    public static final String OFFERS_DESTROYED = "offers_destroyed";
}
