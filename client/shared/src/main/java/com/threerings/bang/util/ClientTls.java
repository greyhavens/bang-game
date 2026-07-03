//
// $Id$

package com.threerings.bang.util;

import com.threerings.presents.client.Client;
import com.threerings.presents.net.TlsContextFactory;

import static com.threerings.bang.Log.log;

/**
 * Configures client-side transport TLS (Epic 3 Phase 4) from the shipped deployment config. All
 * client entry points (the game client, the editor, the headless bot) call {@link #configure} on
 * their {@link Client} before logon so they speak TLS to a TLS-enabled server.
 *
 * <p>When {@code server_tls} is not set in deployment.properties this is a no-op and the client
 * connects in plaintext exactly as before. When it is set, the client trusts only the pinned
 * certificate in the truststore (no public CA for a self-hosted deployment).
 */
public class ClientTls
{
    /**
     * If {@code server_tls} is enabled in the deployment config, builds the pinned client
     * {@link javax.net.ssl.SSLContext} and installs it on the supplied client. Must be called
     * before {@link Client#logon}. A failure to build the context is logged but not fatal — logon
     * will simply fail against the TLS server, which is the correct fail-closed behavior.
     */
    public static void configure (Client client)
    {
        if (!DeploymentConfig.config.getValue("server_tls", false)) {
            return;
        }
        String truststore = DeploymentConfig.config.getValue("server_tls_truststore", "");
        // the truststore holds only the server's public cert, so this password is NOT the secret
        // keystore password — it is safe to ship in deployment.properties (see gen-tls-certs)
        String password = DeploymentConfig.config.getValue("server_tls_truststore_password", "");
        try {
            client.setSSLContext(
                TlsContextFactory.clientContext(truststore, password.toCharArray()));
            log.info("Transport TLS enabled.", "truststore", truststore);
        } catch (Exception e) {
            log.warning("Failed to initialize client TLS; logon will fail.",
                        "truststore", truststore, e);
        }
    }
}
