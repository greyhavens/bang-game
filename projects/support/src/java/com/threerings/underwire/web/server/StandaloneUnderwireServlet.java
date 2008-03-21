//
// $Id$

package com.threerings.underwire.web.server;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import com.samskivert.util.Tuple;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;

import com.samskivert.servlet.JDBCTableSiteIdentifier;
import com.samskivert.servlet.SiteIdentifier;

import com.samskivert.servlet.user.AuthenticationFailedException;
import com.samskivert.servlet.user.InvalidPasswordException;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.User;
import com.samskivert.servlet.user.UserManager;
import com.samskivert.servlet.user.UserRepository;

import com.threerings.util.OOOConfig;

import com.threerings.underwire.server.BangGameActionHandler;
import com.threerings.underwire.server.BangGameInfoProvider;
import com.threerings.underwire.server.DummyGameActionHandler;
import com.threerings.underwire.server.DummyGameInfoProvider;
import com.threerings.underwire.server.GameActionHandler;
import com.threerings.underwire.server.GameInfoProvider;

import com.threerings.underwire.server.persist.OOOUserSupportRepository;
import com.threerings.underwire.server.persist.SupportRepository;
import com.threerings.underwire.server.persist.UnderwireRepository;

import com.threerings.user.OOOUserManager;
import java.util.logging.Level;

import static com.threerings.underwire.Log.log;

/**
 * An underwire servlet which uses a static connection provider.
 */
public class StandaloneUnderwireServlet extends UnderwireServlet
{
    // documentation inherited from UnderwireServlet
    public SiteIdentifier createSiteIdentifier ()
        throws PersistenceException
    {
        return new JDBCTableSiteIdentifier(getConnectionProvider());
    }

    // documentation inherited from UnderwireServlet
    public SupportRepository createSupportRepository ()
        throws PersistenceException
    {
        return (OOOUserSupportRepository)getUserManager().getRepository();
    }

    // documentation inherited from UnderwireServlet
    public UnderwireRepository createUnderwireRepository ()
        throws PersistenceException
    {
        return new UnderwireRepository(_conprov);
    }

    // documentation inherited from UnderwireServlet
    public Tuple<User,String> userLogin (String username, String password, int expireDays)
        throws PersistenceException, AuthenticationFailedException, InvalidPasswordException
    {
        return getUserManager().login(username, Password.makeFromCrypto(password), expireDays,
                                      OOOUserManager.AUTH_PASSWORD);
    }

    /**
     * Returns the connection provider, creating it if necessary.
     */
    protected ConnectionProvider getConnectionProvider ()
    {
        if (_conprov == null) {
            _conprov = new StaticConnectionProvider(OOOConfig.getJDBCConfig());
        }
        return _conprov;
    }

    /**
     * Returns the user manager, creating it if necessary.
     */
    protected UserManager getUserManager ()
        throws PersistenceException
    {
        if (_usermgr == null) {
            _usermgr = new OOOUserManager(
                    OOOConfig.getUserManagerConfig(), getConnectionProvider()) {
                protected UserRepository createRepository (ConnectionProvider conprov)
                    throws PersistenceException
                {
                    return new OOOUserSupportRepository(conprov);
                }
            };
        }
        return _usermgr;
    }

    // documentation inherited from UnderwireServlet
    protected GameInfoProvider getInfoProvider ()
        throws PersistenceException
    {
        HttpServletRequest req = getThreadLocalRequest();
        String serverName = req.getServerName();
        Class<?> ipClass;
        if (serverName != null && serverName.endsWith("banghowdy.com")) {
            ipClass = BangGameInfoProvider.class;
        } else if (serverName != null && serverName.endsWith("localhost")) {
            ipClass = BangGameInfoProvider.class;
        } else if (serverName != null && serverName.endsWith("threerings.net")) {
            ipClass = DummyGameInfoProvider.class;
        } else {
            log.log(Level.WARNING, "Unknown Underwire domain '" + serverName + "'.");
            ipClass = DummyGameInfoProvider.class;
        }

        // check to see if we've already got it resolved
        GameInfoProvider inprov = _inprovs.get(ipClass);
        if (inprov == null) {
            try {
                inprov = (GameInfoProvider)ipClass.newInstance();
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to instantiate info provider '" + ipClass + "'.", e);
                inprov = new DummyGameInfoProvider();
            }
            inprov.init(_conprov);
            _inprovs.put(ipClass, inprov);
        }
        return inprov;
    }

    // documentation inherited from UnderwireServlet
    protected GameActionHandler getActionHandler ()
        throws PersistenceException
    {
        HttpServletRequest req = getThreadLocalRequest();
        String serverName = req.getServerName();
        Class<?> ahClass;
        if (serverName != null && serverName.endsWith("banghowdy.com")) {
            ahClass = BangGameActionHandler.class;
        } else if (serverName != null && serverName.endsWith("localhost")) {
            ahClass = BangGameActionHandler.class;
        } else if (serverName != null && serverName.endsWith("threerings.net")) {
            ahClass = DummyGameActionHandler.class;
        } else {
            log.log(Level.WARNING, "Unknown Underwire domain '" + serverName + "'.");
            ahClass = DummyGameActionHandler.class;
        }

        // check to see if we've already got it resolved
        GameActionHandler ahandler = _ahandlers.get(ahClass);
        if (ahandler == null) {
            try {
                ahandler = (GameActionHandler)ahClass.newInstance();
            } catch (Exception e) {
                log.log(Level.WARNING,
                        "Failed to instantiate action handler '" + ahClass + "'.", e);
                ahandler = new DummyGameActionHandler();
            }
            ahandler.init(_conprov);
            _ahandlers.put(ahClass, ahandler);
        }
        return ahandler;
    }

    /** Provides JDBC connections. */
    protected ConnectionProvider _conprov;

    /** Handles user authentication. */
    protected OOOUserManager _usermgr;

    /** Maps domains to game info providers. */
    protected HashMap<Class<?>,GameInfoProvider> _inprovs =
        new HashMap<Class<?>,GameInfoProvider>();

    /** Maps domains to game action handlers. */
    protected HashMap<Class<?>,GameActionHandler> _ahandlers =
        new HashMap<Class<?>,GameActionHandler>();
}
