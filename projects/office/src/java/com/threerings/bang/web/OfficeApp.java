//
// $Id$

package com.threerings.bang.web;

import java.util.logging.Level;

import javax.servlet.ServletConfig;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.velocity.Application;

import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.server.persist.StatRepository;

import static com.threerings.bang.Log.log;

/**
 * The Sheriff's Office: the admin website for Bang! Howdy.
 */
public class OfficeApp extends Application
{
    @Override // documentation inherited
    protected void willInit (ServletConfig config)
    {
        super.willInit(config);

	try {
            // create a static connection provider
            _conprov = new StaticConnectionProvider(
                ServerConfig.getJDBCConfig());

            // create our repositories
            _playrepo = new PlayerRepository(_conprov);
            _statrepo = new StatRepository(_conprov);

	    log.info("Sheriff's Office initialized.");

	} catch (Throwable t) {
	    log.log(Level.WARNING, "Error initializing Sheriff's Office.", t);
	}
    }

    protected ConnectionProvider _conprov;
    protected PlayerRepository _playrepo;
    protected StatRepository _statrepo;
}
