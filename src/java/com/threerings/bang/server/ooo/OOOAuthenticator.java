//
// $Id$

package com.threerings.bang.server.ooo;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserManager;
import com.threerings.user.OOOUserRepository;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;
import com.threerings.presents.net.UsernamePasswordCreds;

import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.crowd.data.TokenRing;

import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.data.BangAuthCodes.*;

/**
 * Delegates authentication to the OOO user manager.
 */
public class OOOAuthenticator extends Authenticator
{
    public OOOAuthenticator ()
    {
        try {
            // we get our user manager configuration from the ocean config
            _usermgr = new OOOUserManager(
                ServerConfig.config.getSubProperties("oooauth"),
                BangServer.conprov);
            _authrep = (OOOUserRepository)_usermgr.getRepository();
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to initialize OOO authenticator. " +
                    "Users will be unable to log in.", pe);
        }
    }

    // documentation inherited
    public void authenticateConnection (final AuthingConnection conn)
    {
        // fire up an invoker unit that will load the user object just to
        // make sure they exist
        String name = "auth:" + conn.getAuthRequest().getCredentials();
        BangServer.invoker.postUnit(new Invoker.Unit(name) {
            public boolean invoke () {
                processAuthentication(conn);
                return false;
            }
        });
    }

    /**
     * Here we do the actual authentication processing while running
     * happily on the invoker thread.
     */
    protected void processAuthentication (AuthingConnection conn)
    {
        AuthRequest req = conn.getAuthRequest();
        AuthResponseData rdata = new AuthResponseData();
        AuthResponse rsp = new AuthResponse(rdata);

        try {
            // make sure we were properly initialized
            if (_authrep == null) {
                rdata.code = SERVER_ERROR;
                return;
            }

            // make sure they've sent valid credentials
            if (!(req.getCredentials() instanceof UsernamePasswordCreds)) {
                rdata.code = SERVER_ERROR;
                log.warning("Invalid credentials: " + req);
                // note that the finally block will be executed and
                // communicate our auth response back to the conmgr
                return;
            }
            UsernamePasswordCreds creds = (UsernamePasswordCreds)
                req.getCredentials();

            // make sure they've got the correct version
            long cvers = 0L;
            long svers = DeploymentConfig.getVersion();
            try {
                cvers = Long.parseLong(req.getVersion());
            } catch (Exception e) {
                // ignore it and fail below
            }
            if (svers != cvers) {
                if (cvers > svers) {
                    rdata.code = NEWER_VERSION;
                } else {
                    rdata.code =
                        MessageBundle.tcompose(VERSION_MISMATCH, "" + svers);
                }
                log.info("Refusing wrong version [creds=" + creds +
                         ", cvers=" + cvers + ", svers=" + svers + "].");
                return;
            }

            // load up their user account record
            String username = creds.getUsername().toString();
            OOOUser user = (OOOUser)_authrep.loadUser(username);
            if (user == null) {
                rdata.code = NO_SUCH_USER;
                return;
            }

            // check whether we're restricting non-insider login
            if (!RuntimeConfig.server.openToPublic &&
                !user.holdsToken(OOOUser.INSIDER) &&
                !user.holdsToken(OOOUser.TESTER) &&
                !user.isSupportPlus()) {
                rdata.code = NON_PUBLIC_SERVER;
                return;
            }

            // check whether we're restricting non-admin login
            if (!RuntimeConfig.server.nonAdminsAllowed &&
                !user.isSupportPlus()) {
                rdata.code = UNDER_MAINTENANCE;
                return;
            }

            // now check their password
            if (!user.password.equals(creds.getPassword())) {
                rdata.code = INVALID_PASSWORD;
                return;
            }

            // configure a token ring for this user
            int tokens = 0;
            if (user.holdsToken(OOOUser.ADMIN)) {
                tokens |= TokenRing.ADMIN;
            }
            rsp.authdata = new TokenRing(tokens);

            // replace the username in their credentials with the
            // canonical name in their user record as that username will
            // later be stuffed into their user object
            creds.setUsername(new Name(user.username));

            log.info("User logged on [user=" + user.username + "].");
            rdata.code = AuthResponseData.SUCCESS;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Error authenticating user " +
                    "[areq=" + req + "].", pe);
            rdata.code = SERVER_ERROR;

        } finally {
            // let the powers that be know that we're done authenticating
            connectionWasAuthenticated(conn, rsp);
        }
    }

    protected OOOUserRepository _authrep;
    protected OOOUserManager _usermgr;
}
