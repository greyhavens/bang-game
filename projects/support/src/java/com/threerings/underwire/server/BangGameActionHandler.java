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
    @Override // from GameInfoProvider
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        _playrepo = new PlayerRepository(conprov);
    }

    @Override // from GameActionHandler
    public void tempBan (String accountName, Timestamp expires, String warning)
        throws PersistenceException
    {
        _playrepo.setTempBan(accountName, expires, warning);
    }

    @Override // from GameActionHandler
    public void warn (String accountName, String warning)
        throws PersistenceException
    {
        _playrepo.setWarning(accountName, warning);
    }

    protected PlayerRepository _playrepo;
}
