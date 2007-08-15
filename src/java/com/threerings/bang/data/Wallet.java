//
// $Id$

package com.threerings.bang.data;

/**
 * An interface for an object that has a coin and scrip amount.
 */
public interface Wallet
{
    /**
     * Returns the amount of scrip in the wallet.
     */
    public int getScrip ();

    /**
     * Returns the amount of coins in the wallet.
     */
    public int getCoins ();
}
