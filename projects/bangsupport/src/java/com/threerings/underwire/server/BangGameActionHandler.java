//
// $Id$

package com.threerings.underwire.server;

import java.sql.Timestamp;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.bang.server.persist.PlayerRepository;

/**
 * Provides bang game-specific action handling.
 */
public class BangGameActionHandler extends GameActionHandler
{
    public BangGameActionHandler (ConnectionProvider conprov)
    {
        try {
            _playrepo = new PlayerRepository(conprov);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    @Override // from GameActionHandler
    public void ban (String accountName)
    {
        // nothing doing
    }

    @Override // from GameActionHandler
    public void tempBan (String accountName, Timestamp expires, String warning)
    {
        try {
            _playrepo.setTempBan(accountName, expires, warning);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    @Override // from GameActionHandler
    public void warn (String accountName, String warning)
    {
        try {
            _playrepo.setWarning(accountName, warning);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    @Override // from GameActionHandler
    public void sendMessage (String senderAccount, String recipAccount, String message)
    {
        // nothing doing
    }

    protected PlayerRepository _playrepo;
}
