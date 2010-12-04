//
// $Id: BangGameInfoProvider.java 2804 2008-03-20 21:33:29Z mjohnson $

package com.threerings.underwire.server;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.bang.data.Handle;
import com.threerings.bang.server.persist.PlayerRecord;
import com.threerings.bang.server.persist.PlayerRepository;

import com.threerings.underwire.web.data.Account;

/**
 * Provides game-specific info for Bang! Howdy.
 */
public class BangGameInfoProvider extends GameInfoProvider
{
    public BangGameInfoProvider (ConnectionProvider conprov)
    {
        try {
            _playrepo = new PlayerRepository(conprov);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    @Override // from GameInfoProvider
    public Map<String,List<String>> resolveGameNames (Set<String> names)
    {
        try {
            return _playrepo.resolveHandles(names);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    @Override // from GameInfoProvider
    public String[] lookupAccountNames (String gameName)
    {
        try {
            PlayerRecord player = _playrepo.loadByHandle(new Handle(gameName));
            return (player == null) ? null : new String[] { player.accountName };
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    @Override // from GameInfoProvider
    public void populateAccount (Account account)
    {
        try {
            PlayerRecord player = _playrepo.loadPlayer(account.name.accountName);
            if (player != null) {
                account.firstSession = new Date(player.created.getTime());
                account.lastSession = new Date(player.lastSession.getTime());
                if (player.banExpires != null) {
                    account.tempBan = new Date(player.banExpires.getTime());
                }
                account.warning = player.warning;
            }
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    protected PlayerRepository _playrepo;
}
