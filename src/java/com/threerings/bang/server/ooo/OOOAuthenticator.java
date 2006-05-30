//
// $Id$

package com.threerings.bang.server.ooo;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.util.IdentUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserManager;
import com.threerings.user.OOOUserRepository;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;

import com.threerings.presents.server.Authenticator;
import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.bang.admin.server.RuntimeConfig;

import com.threerings.bang.data.BangAuthResponseData;
import com.threerings.bang.data.BangCredentials;
import com.threerings.bang.data.BangTokenRing;
import com.threerings.bang.server.BangClientResolver;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.Player;
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
        BangAuthResponseData rdata = new BangAuthResponseData();
        AuthResponse rsp = new AuthResponse(rdata);

        try {
            // make sure we were properly initialized
            if (_authrep == null) {
                rdata.code = SERVER_ERROR;
                return;
            }

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
                    // TEMP: force the use of the old auth response data to
                    // avoid freaking out older clients
                    rsp = new AuthResponse(new AuthResponseData());
                    rsp.getData().code = MessageBundle.tcompose(
                        VERSION_MISMATCH, "" + svers);
                }
                log.info("Refusing wrong version " +
                         "[creds=" + req.getCredentials() +
                         ", cvers=" + cvers + ", svers=" + svers + "].");
                return;
            }

            // make sure they've sent valid credentials
            if (!(req.getCredentials() instanceof BangCredentials)) {
                log.warning("Invalid creds " + req.getCredentials() + ".");
                rdata.code = SERVER_ERROR;
                return;
            }

            // check their provided machine identifier
            BangCredentials creds = (BangCredentials)req.getCredentials();
            String username = creds.getUsername().toString();
            if (StringUtil.isBlank(creds.ident)) {
                log.warning("Received blank ident [creds=" +
                            req.getCredentials() + "].");
                BangServer.generalLog(
                    "refusing_spoofed_ident " + username +
                    " ip:" + conn.getInetAddress());
                rdata.code = SERVER_ERROR;
                return;
            }

            // if they supplied a known non-unique machine identifier, create
            // one for them
            if (IdentUtil.isBogusIdent(creds.ident.substring(1))) {
                String sident = StringUtil.md5hex(
                    "" + Math.random() + System.currentTimeMillis());
                creds.ident = "S" + IdentUtil.encodeIdent(sident);
                BangServer.generalLog("creating_ident " + username +
                                      " ip:" + conn.getInetAddress() +
                                      " id:" + creds.ident);
                rdata.ident = creds.ident;
            }

            // convert the encrypted ident to the original MD5 hash
            try {
                String prefix = creds.ident.substring(0, 1);
                creds.ident = prefix +
                    IdentUtil.decodeIdent(creds.ident.substring(1));
            } catch (Exception e) {
                log.warning("Received spoofed ident [who=" + username +
                            ", err=" + e.getMessage() + "].");
                BangServer.generalLog("refusing_spoofed_ident " + username +
                                      " ip:" + conn.getInetAddress() +
                                      " id:" + creds.ident);
                rdata.code = SERVER_ERROR;
                return;
            }

            // load up their user account record
            OOOUser user = (OOOUser)_authrep.loadUser(username);
            if (user == null) {
                rdata.code = NO_SUCH_USER;
                return;
            }

            // we need to find out if this account has ever logged in so that
            // we can decide how to handle tainted idents; so we load up the
            // player record for this account; if this player makes it through
            // the gauntlet, we'll stash this away in a place that the client
            // resolver can get its hands on it so that we can avoid loading
            // the record twice during authentication
            Player prec = BangServer.playrepo.loadPlayer(username);

            // check to see whether this account has been banned or if this is
            // a first time user logging in from a tainted machine
            int vc = _authrep.validateUser(user, creds.ident, prec == null);
            switch (vc) {
                // various error conditions
                case OOOUserRepository.ACCOUNT_BANNED:
                   rdata.code = BANNED;
                   return;
                case OOOUserRepository.NEW_ACCOUNT_TAINTED:
                   rdata.code = MACHINE_TAINTED;
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
                tokens |= BangTokenRing.ADMIN;
                tokens |= BangTokenRing.INSIDER;
            }
            if (user.holdsToken(OOOUser.INSIDER)) {
                tokens |= BangTokenRing.INSIDER;
            }
            rsp.authdata = new BangTokenRing(tokens);

            // replace the username in their credentials with the
            // canonical name in their user record as that username will
            // later be stuffed into their user object
            creds.setUsername(new Name(user.username));

            // log.info("User logged on [user=" + user.username + "].");
            rdata.code = BangAuthResponseData.SUCCESS;

            // pass their player record to the client resolver for retrieval
            // later in the logging on process
            if (prec != null) {
                BangClientResolver.stashPlayer(prec);
            }

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
