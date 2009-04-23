//
// $Id$

package com.threerings.underwire.web.server;

import com.samskivert.util.Config;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;

import com.samskivert.servlet.JDBCTableSiteIdentifier;

import com.threerings.underwire.server.BangGameActionHandler;
import com.threerings.underwire.server.BangGameInfoProvider;
import com.threerings.underwire.server.OOOUserLogic;
import com.threerings.underwire.server.UnderContext;
import com.threerings.underwire.server.UserLogic;

import com.threerings.underwire.server.persist.UnderwireRepository;

/**
 * An underwire servlet which uses a static connection provider.
 */
public class BangUnderwireServlet extends UnderwireServlet
{
    @Override // from UnderwireServlet
    protected UnderContext createContext ()
    {
        try {
            return new UnderContext(new JDBCTableSiteIdentifier(_conprov),
                                    new BangGameInfoProvider(_conprov),
                                    new BangGameActionHandler(_conprov),
                                    new UnderwireRepository(_conprov));
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    @Override // from UnderwireServlet
    protected UserLogic createUserLogic (UnderContext ctx)
    {
        return new OOOUserLogic(ctx, _conprov, _config.getSubProperties("oooauth"));
    }

    /** Contains our configuration. */
    protected Config _config = new Config("server");

    /** Provides JDBC connections. */
    protected ConnectionProvider _conprov =
        new StaticConnectionProvider(_config.getSubProperties("db"));
}
