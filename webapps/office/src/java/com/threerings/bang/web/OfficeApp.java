//
// $Id$

package com.threerings.bang.web;

import java.io.File;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.servlet.JDBCTableSiteIdentifier;
import com.samskivert.servlet.SiteIdentifier;
import com.samskivert.servlet.util.RequestUtils;
import com.samskivert.util.ServiceUnavailableException;
import com.samskivert.util.StringUtil;
import com.samskivert.velocity.Application;
import com.samskivert.velocity.Logic;

import com.threerings.user.OOOUserManager;

import com.threerings.bang.data.StatType;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.BangStatRepository;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PlayerStatRepository;

import static com.threerings.bang.Log.log;

/**
 * The Sheriff's Office: the admin website for Bang! Howdy.
 */
public class OfficeApp extends Application
{
    /**
     * Returns a reference to our user manager.
     */
    public OOOUserManager getUserManager ()
    {
        return _usermgr;
    }

    /**
     * Returns a reference to the player repository.
     */
    public PlayerStatRepository getPlayerRepository ()
    {
        return _playrepo;
    }

    /**
     * Returns a reference to the stat repository.
     */
    public BangStatRepository getStatRepository ()
    {
        return _statrepo;
    }

    /**
     * Returns a reference to the item repository.
     */
    public ItemRepository getItemRepository ()
    {
        return _itemrepo;
    }

    @Override // from Application
    public void shutdown ()
    {
        super.shutdown();
        _perCtx.shutdown();
    }

    @Override // documentation inherited
    protected void willInit (ServletConfig config)
    {
        super.willInit(config);

	try {
            // create a static connection provider
            _conprov = new StaticConnectionProvider(ServerConfig.getJDBCConfig());
            _perCtx = new PersistenceContext("bangdb", _conprov, null);

            // create our user manager
            _usermgr = new OOOUserManager(
                ServerConfig.config.getSubProperties("oooauth"), _conprov);

            // create our repositories
            _playrepo = new PlayerStatRepository(_conprov);
            _statrepo = new BangStatRepository(_perCtx);
            _itemrepo = new ItemRepository(_conprov);

            // initialize our repositories, run any migrations, etc.
            _perCtx.initializeRepositories(true);

	    log.info("Sheriff's Office initialized.");

	} catch (Throwable t) {
	    log.warning("Error initializing Sheriff's Office.", t);
	}
    }

    @Override // documentation inherited
    protected void configureVelocity (ServletConfig config, Properties props)
    {
        String scontext = config.getServletContext().getServletContextName();
        if (StringUtil.isBlank(scontext)) {
            log.warning("Unable to determine servlet context name, " +
                        "cannot activate file loader.");
            return;
        }

        File source = new File(ServerConfig.serverRoot, "projects" +
                               File.separator + scontext +
                               File.separator + "src" +
                               File.separator + "xhtml");
        if (source.exists()) {
            props.setProperty("file.resource.loader.path", source.getPath());
            log.info("Velocity loading directly from " + source + ".");
        }
    }

    @Override // document inherited
    protected SiteIdentifier createSiteIdentifier (ServletContext ctx)
    {
        try {
            return new JDBCTableSiteIdentifier(_conprov);
        } catch (PersistenceException pe) {
            throw new ServiceUnavailableException(
                "Can't access site database.", pe);
        }
    }

    // document inherited
    protected String handleException (
        HttpServletRequest req, Logic logic, Exception error)
    {
        log.warning(logic.getClass().getName() + " failed for: " +
                    RequestUtils.reconstructURL(req), error);
        return "error.internal";
    }

    protected ConnectionProvider _conprov;
    protected PersistenceContext _perCtx;
    protected OOOUserManager _usermgr;

    protected PlayerStatRepository _playrepo;
    protected BangStatRepository _statrepo;
    protected ItemRepository _itemrepo;

    /** Pay no attention to the man behind the curtain frobbing static initializers. */
    protected static final StatType INIT_STATS = StatType.UNUSED;
}
