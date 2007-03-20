//
// $Id$

package com.threerings.bang.server.ooo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;
import com.samskivert.util.RandomUtil;
import com.samskivert.servlet.JDBCTableSiteIdentifier;
import com.samskivert.servlet.user.InvalidUsernameException;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.UserExistsException;
import com.samskivert.servlet.user.Username;

import com.threerings.util.IdentUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserManager;
import com.threerings.user.OOOUserRepository;
import com.threerings.user.RewardInfo;
import com.threerings.user.RewardRecord;
import com.threerings.user.RewardRepository;

import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;
import com.threerings.presents.net.AuthResponseData;

import com.threerings.presents.server.net.AuthingConnection;

import com.threerings.bang.admin.server.RuntimeConfig;

import com.threerings.bang.data.BangAuthResponseData;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangCredentials;
import com.threerings.bang.data.BangTokenRing;
import com.threerings.bang.server.BangAuthenticator;
import com.threerings.bang.server.BangClientResolver;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.PlayerRecord;
import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.data.BangAuthCodes.*;

/**
 * Delegates authentication to the OOO user manager.
 */
public class OOOAuthenticator extends BangAuthenticator
{
    public OOOAuthenticator ()
    {
        try {
            _siteident = new JDBCTableSiteIdentifier(BangServer.conprov);

            // we get our user manager configuration from the ocean config
            _usermgr = new OOOUserManager(ServerConfig.config.getSubProperties("oooauth"),
                                          BangServer.conprov);
            _authrep = (OOOUserRepository)_usermgr.getRepository();
            _rewardrep = new RewardRepository(BangServer.conprov);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to initialize OOO authenticator. " +
                    "Users will be unable to log in.", pe);
        }
    }

    // from abstract BangAuthenticator
    public void setAccountIsActive (String username, boolean isActive)
        throws PersistenceException
    {
        // pass the word on to the user repository
        _authrep.updateUserIsActive(username, OOOUser.IS_ACTIVE_BANG_PLAYER, isActive);
    }

    // from abstract BangAuthenticator
    public String createAccount (
            String username, String password, String email, String affiliate, String machIdent)
        throws PersistenceException
    {
        // check if their username already exists
        if (_authrep.loadUser(username) != null) {
            return NAME_IN_USE;
        }

        // figure out the sideId
        int siteId;
        try {
            siteId = Integer.decode(affiliate);
        } catch (Exception e) {
            siteId = _siteident.getSiteId(affiliate);
        }

        try {
            // make sure that this machien identifier is allowed to create a new account
            int rv = _authrep.checkCanCreate(machIdent);
            switch(rv) {
            case OOOUserRepository.NEW_ACCOUNT_TAINTED: return MACHINE_TAINTED;
            case OOOUserRepository.ACCESS_GRANTED:
                // we're grooving right along
                break;
            default:
                log.warning("Unhandled checkCanCreate() response code " +
                            "[ident=" + machIdent + ", rv=" + rv + "].");
                return SERVER_ERROR;
            }

            Username uname = new Username(username);
            Password pass = Password.makeFromCrypto(password);

            // create the account
            _authrep.createUser(uname, pass, email, siteId, 0);

            return null;

        } catch (InvalidUsernameException iue) {
            // the client shouldn't allow invalid names, so we just give a generic exception here
            log.warning("User submitted invalid username? [username=" + username + "].");
            return SERVER_ERROR;

        } catch (UserExistsException uee) {
            return NAME_IN_USE;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Error creating arround [username=" + username + ", password=" +
                    password + ", siteId=" + siteId + ", ident=" + machIdent + "].", pe);
            return SERVER_ERROR;
        }
    }

    @Override // from Authenticator
    protected AuthResponseData createResponseData ()
    {
        return new BangAuthResponseData();
    }

    // from abstract Authenticator
    protected void processAuthentication (AuthingConnection conn, AuthResponse rsp)
        throws PersistenceException
    {
        AuthRequest req = conn.getAuthRequest();
        BangAuthResponseData rdata = (BangAuthResponseData) rsp.getData();

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
            rdata.code = (cvers > svers) ? NEWER_VERSION :
                MessageBundle.tcompose(VERSION_MISMATCH, "" + svers);
            log.info("Refusing wrong version [creds=" + req.getCredentials() +
                     ", cvers=" + cvers + ", svers=" + svers + "].");
            return;
        }

        // make sure they've sent valid credentials
        BangCredentials creds;
        try {
            creds = (BangCredentials) req.getCredentials();
        } catch (ClassCastException cce) {
            log.warning("Invalid creds " + req.getCredentials() + ".");
            rdata.code = SERVER_ERROR;
            return;
        }

        // check their provided machine identifier
        String username = creds.getUsername().toString();
        if (StringUtil.isBlank(creds.ident)) {
            log.warning("Received blank ident [creds=" + creds + "].");
            BangServer.generalLog("refusing_spoofed_ident " + username +
                                  " ip:" + conn.getInetAddress());
            rdata.code = SERVER_ERROR;
            return;
        }

        // if they supplied a known non-unique machine identifier, create one for them
        if (IdentUtil.isBogusIdent(creds.ident.substring(1))) {
            String sident = StringUtil.md5hex("" + Math.random() + System.currentTimeMillis());
            creds.ident = "S" + IdentUtil.encodeIdent(sident);
            BangServer.generalLog("creating_ident " + username + " ip:" + conn.getInetAddress() +
                                  " id:" + creds.ident);
            rdata.ident = creds.ident;
        }

        // convert the encrypted ident to the original MD5 hash
        try {
            String prefix = creds.ident.substring(0, 1);
            creds.ident = prefix + IdentUtil.decodeIdent(creds.ident.substring(1));
        } catch (Exception e) {
            log.warning("Received spoofed ident [who=" + username +
                        ", err=" + e.getMessage() + "].");
            BangServer.generalLog("refusing_spoofed_ident " + username +
                                  " ip:" + conn.getInetAddress() + " id:" + creds.ident);
            rdata.code = SERVER_ERROR;
            return;
        }

        // load up their user account record
        OOOUser user = _authrep.loadUser(username, true);

        // we need to find out if this account has ever logged in so that we can decide how to
        // handle tainted idents; we load up the player record for this account; if this player
        // makes it through the gauntlet, we'll stash this away in a place that the client resolver
        // can get it so that we can avoid loading the record twice during authentication
        PlayerRecord prec = BangServer.playrepo.loadPlayer(username);
        String password = creds.getPassword();

        if (user == null && prec == null && !StringUtil.isBlank(username) &&
                !StringUtil.isBlank(password)) {
            rdata.code = NO_SUCH_USER;
            return;
        }

        boolean anonymous = user == null;

        if (anonymous && StringUtil.isBlank(username)) {
            // we're a new anonymous client, so better make up fake username
            int attempts = 0;
            do {
                username = StringUtil.md5hex(Integer.toString(
                            RandomUtil.getInt(Integer.MAX_VALUE)));
                prec = BangServer.playrepo.loadPlayer(username);
                attempts++;
            } while (prec != null && attempts < MAX_LOOP);

            // well crap, can't find them a username
            if (prec != null) {
                rdata.code = SERVER_ERROR;
                return;
            }
        }

        if (user == null && prec != null && !prec.isSet(PlayerRecord.IS_ANONYMOUS)) {
            rdata.code = INVALID_PASSWORD;
            return;
        }

        // make sure this player has access to this server's town
        int serverTownIdx = ServerConfig.townIndex;
        if (!anonymous && RuntimeConfig.server.freeIndianPost &&
            serverTownIdx == BangUtil.getTownIndex(BangCodes.INDIAN_POST)) {
            // free access
            serverTownIdx = -1;
        }
        if (serverTownIdx > 0) {
            String townId = BangCodes.FRONTIER_TOWN;
            int townidx = BangUtil.getTownIndex(townId);
            if (prec != null && prec.townId != null) {
                townId = prec.townId;
                townidx = BangUtil.getTownIndex(townId);
                // if their nextTown timestamp hasn't expired they can access the next town
                if (prec.nextTown != null &&
                        prec.nextTown.compareTo(new Timestamp(System.currentTimeMillis())) > 0) {
                    townidx++;
                }
            }

            if (townidx < serverTownIdx && !user.isAdmin()) {
                log.warning("Rejecting access to town server by non-ticket-holder " +
                            "[who=" + username + ", stownId=" + ServerConfig.townId +
                            ", ptownId=" + townId + "].");
                rdata.code = NO_TICKET;
                return;
            }
        }

        // check to see whether this account has been banned or if this is a first time user
        // logging in from a tainted machine
        int vc = anonymous ? _authrep.validateMachIdent(creds.ident, prec == null) :
                _authrep.validateUser(OOOUser.BANGHOWDY_SITE_ID, user, creds.ident, prec == null);
        switch (vc) {
        case OOOUserRepository.ACCOUNT_BANNED:
            log.info("Rejecting banned account [who=" + username + "].");
            rdata.code = BANNED;
            return;
        case OOOUserRepository.DEADBEAT:
            log.info("Rejecting deadbeat account [who=" + username + "].");
            rdata.code = DEADBEAT;
            return;
        case OOOUserRepository.NEW_ACCOUNT_TAINTED:
            log.info("Rejecting tainted machine [who=" + username +
                     ", ident=" + creds.ident + "].");
            rdata.code = MACHINE_TAINTED;
            return;
        }

        // check whether we're restricting non-insider login
        if (!RuntimeConfig.server.openToPublic && (anonymous ||
                    (!user.holdsToken(OOOUser.INSIDER) && !user.holdsToken(OOOUser.TESTER) &&
                     !user.isSupportPlus()))) {
            rdata.code = NON_PUBLIC_SERVER;
            return;
        }

        // check whether we're restricting non-admin login
        if (!RuntimeConfig.server.nonAdminsAllowed && (anonymous || !user.isSupportPlus())) {
            rdata.code = UNDER_MAINTENANCE;
            return;
        }

        // now check their password
        if (!anonymous && !user.password.equals(password)) {
            rdata.code = INVALID_PASSWORD;
            return;
        }

        // configure a token ring for this user
        int tokens = 0;
        if (!anonymous) {
            if (user.holdsToken(OOOUser.ADMIN)) {
                tokens |= BangTokenRing.ADMIN;
                tokens |= BangTokenRing.SUPPORT;
                tokens |= BangTokenRing.INSIDER;
            }
            if (user.holdsToken(OOOUser.SUPPORT)) {
                tokens |= BangTokenRing.SUPPORT;
                tokens |= BangTokenRing.INSIDER;
            }
            if (user.holdsToken(OOOUser.INSIDER)) {
                tokens |= BangTokenRing.INSIDER;
            }
        }
        rsp.authdata = new BangTokenRing(tokens);

        // replace the username in their credentials with the canonical name in their user record
        // as that username will later be stuffed into their user object
        if (!anonymous) {
            creds.setUsername(new Name(user.username));
        } else {
            creds.setUsername(new Name(username));
        }

        // log.info("User logged on [user=" + user.username + "].");
        rdata.code = BangAuthResponseData.SUCCESS;

        if (prec != null) {
            if (!anonymous) {
                // redeem any rewards for which they have become eligible, but don't let this stick
                // a fork in the logon process
                prec.rewards = new ArrayList<String>();
                try {
                    ArrayList<RewardRecord> rewards =
                        _rewardrep.loadActivatedRewards(prec.accountName, creds.ident);
                    for (RewardRecord record : rewards) {
                        if (record.account.equals(user.username) &&
                            StringUtil.isBlank(record.redeemerIdent)) {
                            maybeRedeemReward(prec, creds.ident, record, rewards);
                        }
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to redeem rewards for account " +
                            "[who=" + prec.accountName + "].", e);
                }
            }

            // pass their player record to the client resolver for use later
            BangClientResolver.stashPlayer(prec);
        }
    }

    /**
     * Ensures that this account is eligible for the reward in question and tacks it onto their
     * rewards list if so.
     */
    protected void maybeRedeemReward (
        PlayerRecord prec, String machIdent, RewardRecord record, ArrayList<RewardRecord> records)
        throws PersistenceException
    {
        // if too many associated accounts (two) have already redeemed this reward, sorry charlie
        int otherRedeemers = 0;
        for (RewardRecord rrec : records) {
            if (rrec.rewardId == record.rewardId && !rrec.account.equals(prec.accountName)) {
                otherRedeemers++;
            }
        }
        if (otherRedeemers > MAX_RELATED_REDEEMERS) {
            return;
        }

        // otherwise load up the reward info
        RewardInfo info = _rewards.get(record.rewardId);
        if (info == null) {
            // update our cached rewards
            for (RewardInfo ninfo : _rewardrep.loadActiveRewards()) {
                _rewards.put(ninfo.rewardId, ninfo);
            }
            info = _rewards.get(record.rewardId);
        }
        if (info == null || info.data == null || !info.data.toLowerCase().startsWith("bang:")) {
            return; // reward is expired (and purged) or not bang related
        }

        // note this reward as redeemed
        _rewardrep.redeemReward(record, machIdent);

        // finally tack it's game data onto their list
        prec.rewards.add(info.data);
    }

    protected JDBCTableSiteIdentifier _siteident;
    protected OOOUserManager _usermgr;
    protected OOOUserRepository _authrep;
    protected RewardRepository _rewardrep;
    protected HashIntMap<RewardInfo> _rewards = new HashIntMap<RewardInfo>();

    /** We only allow two accounts with the same machine ident to redeem a reward. */
    protected static final int MAX_RELATED_REDEEMERS = 2;

    /** Max times we'll try to generate an anonymous username before giving up. */
    protected static final int MAX_LOOP = 1000;
}
