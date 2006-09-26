//
// $Id$

package com.threerings.bang.avatar.server;

import java.io.IOException;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.ColorPository;
import com.threerings.util.MessageBundle;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.cast.CharacterComponent;
import com.threerings.cast.NoSuchComponentException;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.avatar.client.AvatarService;
import com.threerings.bang.avatar.client.BarberService;
import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberMarshaller;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.avatar.util.ColorConstraints;

import static com.threerings.bang.Log.log;

/**
 * Provides Barber-related services.
 */
public class BarberManager extends PlaceManager
    implements BarberCodes, BarberProvider, AvatarProvider
{
    /**
     * Returns an avatar snapshot for the specified player. If they are online,
     * it will be obtained from their loaded player object (and returned
     * immediately), otherwise it will be loaded from the database (and require
     * an asynchronous reply).
     */
    public void getSnapshot (int playerId, ResultListener<int[]> listener)
    {
        // if they're online it's easy peasy
        PlayerObject user = BangServer.lookupPlayer(playerId);
        if (user != null) {
            listener.requestCompleted(
                user.getLook(Look.Pose.WANTED_POSTER).getAvatar(user));
            return;
        }

        // otherwise we have to go to the database (TODO: cache these?)
        final int fpid = playerId;
        final ResultListener<int[]> flist = listener;
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _snap = BangServer.lookrepo.loadSnapshot(fpid);
                } catch (PersistenceException pe) {
                    _error = pe;
                }
                return true;
            }

            public void handleResult () {
                if (_error != null) {
                    flist.requestFailed(_error);
                } else {
                    flist.requestCompleted(_snap);
                }
            }

            protected int[] _snap;
            protected Exception _error;
        });
    }

    // documentation inherited from interface BarberProvider
    public void purchaseLook (ClientObject caller, LookConfig config,
                              BarberService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;

        if (StringUtil.isBlank(config.name) ||
            config.name.length() > BarberCodes.MAX_LOOK_NAME_LENGTH) {
            log.warning("Requested to create look with blank name " +
                        "[who=" + user.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create the look from the specified configuration
        int[] cost = new int[2];
        Look look = BangServer.alogic.createLook(user, config, cost);
        if (look == null) {
            // an error will already have been logged
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure a look with the specified name does not already exist
        if (user.looks.contains(look)) {
            throw new InvocationException("m.name_already_used");
        }

        // if they're an admin, zero out the cost as admins get everything for
        // free (they're cheeky like that)
        if (user.tokens.isAdmin()) {
            cost[0] = cost[1] = 0;
        }

        // copy the articles from their "active" look
        Look current = user.getLook(Look.Pose.DEFAULT);
        if (current != null) {
            look.articles = current.articles;
        } else {
            log.warning("Player has no current look from which to copy " +
                        "articles [who=" + user.who() +
                        ", poses=" + StringUtil.toString(user.poses) + "].");
            look.articles = new int[0];
        }

        // the buy look action takes care of the rest (including checking that
        // they have sufficient funds)
        new BuyLookAction(user, look, cost[0], cost[1], cl).start();
    }

    // documentation inherited from interface BarberProvider
    public void configureLook (ClientObject caller, String name, int[] articles)
    {
        PlayerObject user = (PlayerObject)caller;

        // locate the look in question
        Look look = user.looks.get(name);
        if (look == null) {
            log.warning("Asked to configure unknown look [who=" + user.who() +
                        ", look=" + name +
                        ", articles=" + StringUtil.toString(articles) + "].");
            return;
        }

        // sanity check
        if (articles == null || articles.length != AvatarLogic.SLOTS.length) {
            log.warning("Requested to configure invalid articles array " +
                        "[who=" + user.who() + ", look=" + name +
                        ", articles=" + StringUtil.toString(articles) + "].");
            return;
        }

        // make sure all articles in the list are valid
        for (int ii = 0; ii < articles.length; ii++) {
            if (articles[ii] == 0) {
                if (AvatarLogic.SLOTS[ii].optional) {
                    continue;
                }
                log.warning("Requested to configure look with missing " +
                            "non-optional articles [who=" + user.who() +
                            ", idx=" + ii + ", look=" + name +
                            ", art=" + StringUtil.toString(articles) + "].");
                return;
            }

            Article article = (Article)user.inventory.get(articles[ii]);
            if (article == null ||
                !article.getSlot().equals(AvatarLogic.SLOTS[ii].name)) {
                log.warning("Asked to configure look with invalid article " +
                            "[who=" + user.who() + ", article=" + article +
                            ", slot=" + AvatarLogic.SLOTS[ii].name + "].");
                return;
            }
        }

        // put the new articles in place and update the user object
        look.articles = articles;
        look.modified = true;
        user.updateLooks(look);
    }

    // documentation inherited from interface AvatarProvider
    public void createAvatar (
        ClientObject caller, final Handle handle, boolean isMale,
        LookConfig config, int zations, final AvatarService.ConfirmListener cl)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject)caller;

        // sanity check
        if (user.handle != null && !user.handle.isBlank() &&
            user.getLook(Look.Pose.DEFAULT) != null) {
            log.warning("User tried to recreate avatar [who=" + user.who() +
                        ", handle=" + handle + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // this should be prevented by the client
        if (!NameFactory.getValidator().isValidHandle(handle)) {
            log.warning("User tried to use invalid handle [who=" + user.who() +
                        ", handle=" + handle + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // prevent the use of reserved names by non-admins
        if (!user.tokens.isAdmin() &&
            NameFactory.getValidator().isReservedHandle(handle)) {
            throw new InvocationException(AvatarCodes.ERR_RESERVED_HANDLE);
        }

        // discourage the kiddies from being obviously vulgar
        if (NameFactory.getValidator().isVulgarHandle(handle)) {
            throw new InvocationException(AvatarCodes.ERR_VULGAR_HANDLE);
        }

        // go ahead and set their gender in the user object
        user.setIsMale(isMale);

        // create their default look based on the supplied configuration
        int[] cost = new int[2];
        final Look look = BangServer.alogic.createLook(user, config, cost);
        if (look == null) {
            // an error will already have been logged
            throw new InvocationException(INTERNAL_ERROR);
        }

        // validate the starter article colorizations
        int czp = AvatarLogic.decodePrimary(zations);
        int czs = AvatarLogic.decodeSecondary(zations);
        int czt = AvatarLogic.decodeTertiary(zations);
        ColorPository cpos = BangServer.alogic.getColorPository();
        if (!ColorConstraints.isValidColor(cpos, "clothes_p", czp, user) ||
            !ColorConstraints.isValidColor(cpos, "clothes_s", czs, user) ||
            !ColorConstraints.isValidColor(cpos, "clothes_t", czt, user)) {
            log.warning("Tried to create avatar with invalid default article " +
                        "colorizations [who=" + user.who() + ", look=" + look +
                        ", zations=" + zations + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create their default clothing article, we'll fill in its item id
        // after we've inserted the article into the database
        look.articles = new int[AvatarLogic.SLOTS.length];
        final Article article = BangServer.alogic.createDefaultClothing(
            user, isMale, zations);

        // the client should prevent selection of non-starter components, but
        // we check on the server because we can't trust those bastards
        int maxScrip = AvatarCodes.BASE_LOOK_SCRIP_COST;
        // allow up to the MAX_STARTER_COST for each aspect; this isn't
        // precisely correct, but it doesn't leave too much room for hackery
        maxScrip += AvatarCodes.MAX_STARTER_COST * config.aspects.length;
        if (cost[0] > maxScrip || cost[1] > AvatarCodes.BASE_LOOK_COIN_COST) {
            log.warning("Tried to create avatar with a non-zero cost look " +
                        "[who=" + user.who() + ", look=" + look +
                        ", scrip=" + cost[0] + ", coin=" + cost[1] + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // do the deed!
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    // first configure their chosen handle
                    if (!BangServer.playrepo.configurePlayer(
                            user.playerId, handle, user.isMale)) {
                        _error = AvatarCodes.ERR_DUP_HANDLE;
                        return true;
                    }

                    // insert their default clothing article into the database
                    BangServer.itemrepo.insertItem(article);

                    // and fill its assigned item id into their default look
                    for (int ii = 0; ii < look.articles.length; ii++) {
                        if (AvatarLogic.SLOTS[ii].name.equals(
                                article.getSlot())) {
                            look.articles[ii] = article.getItemId();
                        }
                    }

                    // finally insert their default look into the database
                    BangServer.lookrepo.insertLook(user.playerId, look);

                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Error creating avatar " +
                            "[for=" + user.who() + ", look=" + look + "].", pe);
                    _error = INTERNAL_ERROR;
                }
                return true;
            }

            public void handleResult () {
                if (_error != null) {
                    cl.requestFailed(_error);
                } else {
                    user.addToLooks(look);
                    user.setHandle(handle);
                    user.addToInventory(article);
                    // register the player with their handle as we were unable
                    // to do so when they logged on
                    BangServer.registerPlayer(user);
                    cl.requestProcessed();
                }
            }

            protected String _error;
        });
    }

    // documentation inherited from interface AvatarProvider
    public void selectLook (ClientObject caller, Look.Pose pose, String name)
    {
        PlayerObject user = (PlayerObject)caller;

        // sanity check
        Look look = user.looks.get(name);
        if (look == null) {
            log.warning("Player requested to select unknown look " +
                        "[who=" + user.who() + ", look=" + name + "].");
            return;
        }

        user.setPosesAt(name, pose.ordinal());
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new BarberObject();
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // register ourselves as the AvatarService provider
        BangServer.invmgr.registerDispatcher(new AvatarDispatcher(this), true);
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _bobj = (BarberObject)_plobj;
        _bobj.setService((BarberMarshaller)BangServer.invmgr.registerDispatcher(
                             new BarberDispatcher(this), false));
    }

    /** Used to purchase a new avatar look. */
    protected static final class BuyLookAction extends FinancialAction
    {
        public BuyLookAction (
            PlayerObject user, Look look, int scripCost, int coinCost,
            BarberService.ConfirmListener listener) {
            super(user, scripCost, coinCost);
            _look = look;
            _listener = listener;
        }
        public void start () throws InvocationException {
            super.start();
            // add the look immediately to prevent rapid fire purchase requests
            // from overwriting one another; we will remove the look later if
            // we fail
            _user.addToLooks(_look);
        }

        protected int getCoinType () {
            return CoinTransaction.LOOK_PURCHASE;
        }
        protected String getCoinDescrip () {
            return MessageBundle.compose("m.look_purchase", _look.name);
        }

        protected void persistentAction () throws PersistenceException {
            BangServer.lookrepo.insertLook(_user.playerId, _look);
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            BangServer.lookrepo.deleteLook(_user.playerId, _look.name);
        }

        protected void actionCompleted () {
            _user.setPosesAt(_look.name, Look.Pose.DEFAULT.ordinal());
            _listener.requestProcessed();
        }
        protected void actionFailed () {
            _user.removeFromLooks(_look.getKey());
            _listener.requestFailed(INTERNAL_ERROR);
        }

        protected Look _look;
        protected BarberService.ConfirmListener _listener;
    }

    protected BarberObject _bobj;
}
