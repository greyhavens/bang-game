//
// $Id$

package com.threerings.bang.util;

import java.net.URL;

import com.samskivert.util.Config;

import com.threerings.presents.client.Client;

import com.threerings.bang.data.BangCredentials;

import static com.threerings.bang.Log.log;

/**
 * Properties that are specific to a particular Bang! game deployment (client and server code plus
 * game media) are accessed via this class.
 */
public class DeploymentConfig
{
    /** Provides access to our config properties. <em>Do not</em> modify these properties! */
    public static Config config = new Config("deployment");

    /** Contains our deployment version information. */
    public static Config build = new Config("build");

    /**
     * Returns the version associated with this build of the deployment's code.
     */
    public static long getVersion ()
    {
        return build.getValue("version", 0L);
    }

    /**
     * Returns the default locale for this server. This isn't the actual locale setting of the
     * server or client (use {@link java.util.Locale#getDefault} to obtain that), but is used to
     * determine how the server will handle generation and validation of non-translated proper
     * names.
     */
    public static String getDefaultLocale ()
    {
        return config.getValue("default_locale", "en");
    }

    /**
     * Returns the hostname of the server to which we should connect when logging in to the
     * specified town.
     */
    public static String getServerHost (String townId)
    {
        return config.getValue(townId + ".server_host", "localhost");
    }

    /**
     * Returns the port on which we should connect to the specified town server.
     *
     * @see #getServerHost
     */
    public static int[] getServerPorts (String townId)
    {
        int[] ports = config.getValue("server_ports", Client.DEFAULT_SERVER_PORTS);
        return config.getValue(townId + ".server_ports", ports);
    }

    /**
     * Returns the URL from which HTML content is loaded.
     */
    public static URL getDocBaseURL ()
    {
        return getURL("doc_base_url", null);
    }

    /**
     * Returns the URL to which bug reports should be submitted.
     */
    public static URL getBugSubmitURL ()
    {
        return getURL("bug_submit_url", null);
    }

    /**
     * Returns the URL to send players to create a new account.
     */
    public static URL getNewAccountURL (String suffix)
    {
        return getURL("new_account_url", suffix);
    }

    /**
     * Returns the URL for the server status page.
     */
    public static URL getServerStatusURL ()
    {
        return getURL("server_status_url", null);
    }

    /**
     * Returns the URL for the terms of service page.
     */
    public static URL getTosURL ()
    {
        return getURL("terms_of_service_url", null);
    }

    /**
     * Returns the URL for the billing page.
     */
    public static URL getBillingURL (BangContext ctx)
    {
        return getCredentialedURL(ctx, "billing_url");
    }

    /**
     * Returns the URL for the gold pass billing page.
     */
    public static URL getBillingPassURL (BangContext ctx, String pass)
    {
        return getCredentialedURL(ctx, "pass_url", pass);
    }

    /**
     * Returns the payment type in use by this deployment.
     */
    public static PaymentType getPaymentType ()
    {
        return Enum.valueOf(
            PaymentType.class, config.getValue("payment_type", "coins").toUpperCase());
    }

    /**
     * Returns true if this deployment uses coins, false if not.
     */
    public static boolean usesCoins ()
    {
        return getPaymentType() == PaymentType.COINS;
    }

    /**
     * Returns true if this deployment uses a one-time payment, false if not.
     */
    public static boolean usesOneTime ()
    {
        return getPaymentType() == PaymentType.ONETIME;
    }

    /** Helper function for getting URL properties. */
    protected static URL getURL (String key, String args)
    {
        String url = config.getValue(key, "not_specified");
        String sep = (url.indexOf("?") == -1) ? "?" : "&";
        try {
            if (args != null) {
                url = url + sep + args;
            }
            return new URL(url);
        } catch (Exception e) {
            log.warning("Failed to parse " + key + ": " + url, e);
            return null;
        }
    }

    protected static URL getCredentialedURL (BangContext ctx, String key)
    {
        return getCredentialedURL(ctx, key, "");
    }

    protected static URL getCredentialedURL (BangContext ctx, String key, String postfix)
    {
        BangCredentials creds = (BangCredentials)
            ctx.getClient().getCredentials();
        String url = config.getValue(key, "not_specified") + postfix;
        url = url.replace("USERNAME", creds.getUsername().toString());
        url = url.replace("PASSWORD", creds.getPassword().toString());
        try {
            return new URL(url);
        } catch (Exception e) {
            log.warning("Failed to parse " + key + ": " + url, e);
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
