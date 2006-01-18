//
// $Id$

package com.threerings.bang.util;

import java.net.URL;
import java.util.logging.Level;

import com.samskivert.util.Config;

import static com.threerings.bang.Log.log;

/**
 * Properties that are specific to a particular Bang! game deployment (client
 * and server code plus game media) are accessed via this class.
 */
public class DeploymentConfig
{
    /** Provides access to our config properties. <em>Do not</em> modify these
     * properties! */
    public static Config config = new Config("deployment");

    /** Contains our deployment version information. */
    public static Config build = new Config("build");

    /**
     * Returns the deployment version currently in effect for this deployment.
     * On the client, this value will not change (except when the client is
     * updated), on the server, we reread the build.properties file every
     * minute to allow for updates on a running server.
     */
    public static long getDeploymentVersion ()
    {
        long now = System.currentTimeMillis();
        if (now - _lastVersionCheck > VERSION_CHECK_INTERVAL) {
            // reload our build.properties file
            build = new Config("build");
            // and check for a version update
            long version = build.getValue("config_version", 0L);
            if (version != _deploymentVersion) {
                if (_deploymentVersion > 0L) {
                    log.info("Updating deployment version: " + version);
                }
                _deploymentVersion = version;
            }
            _lastVersionCheck = now;
        }
        return _deploymentVersion;
    }

    /**
     * Returns the version associated with this build of the deployment's
     * code. The deployment version may change if the configuration changes,
     * but the build version will only change with a code change.
     */
    public static long getBuildVersion ()
    {
        return build.getValue("version", 0L);
    }

    /**
     * Returns the default locale for this server. This isn't the actual locale
     * setting of the server or client (use {@link java.util.Locale#getDefault}
     * to obtain that), but is used to determine how the server will handle
     * generation and validation of non-translated proper names.
     */
    public static String getDefaultLocale ()
    {
        return config.getValue("default_locale", "en");
    }

    /**
     * Returns the URL to which bug reports should be submitted.
     */
    public static URL getBugSubmitURL ()
    {
        String url = config.getValue("bug_submit_url", "not_specified");
        try {
            return new URL(url);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to parse bug_submit_url: " + url, e);
            return null;
        }
    }

    /** Our most recent cached deployment version. */
    protected static long _deploymentVersion;

    /** The time at which we last cached our deployment version. */
    protected static long _lastVersionCheck;

    /** We recheck our deployment version every minute. */
    protected static final long VERSION_CHECK_INTERVAL = 60 * 1000L;
}
