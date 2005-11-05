//
// $Id$

package com.threerings.bang.avatar.server;

import java.io.IOException;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

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

import static com.threerings.bang.Log.log;

/**
 * Provides Barber-related services.
 */
public class BarberManager extends PlaceManager
    implements BarberCodes, BarberProvider, AvatarProvider
{
    // documentation inherited from interface BarberProvider
    public void purchaseLook (ClientObject caller, LookConfig config,
                              BarberService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;

        if (StringUtil.blank(config.name) ||
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

        // copy the articles from their "active" look
        look.articles = user.getLook().articles;

        // the buy look action takes care of the rest (including checking that
        // they have sufficient funds)
        new BuyLookAction(user, look, cost[0], cost[1], cl).start();
    }

    // documentation inherited from interface BarberProvider
    public void configureLook (ClientObject caller, String name, int[] articles)
    {
        PlayerObject user = (PlayerObject)caller;

        // locate the look in question
        Look look = (Look)user.looks.get(name);
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
                } else {
                    log.warning("Requested to configure look with missing " +
                                "non-optional articles [who=" + user.who() +
                                ", idx=" + ii + ", look=" + name +
                                ", art=" + StringUtil.toString(articles) + "].");
                    return;
                }
            }

            Article article = (Article)user.inventory.get(articles[ii]);
            if (article == null ||
                !article.getSlot().equals(AvatarLogic.SLOTS[ii].name)) {
                log.warning("Requested to configure look with invalid article " +
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
        ClientObject caller, Handle handle, boolean isMale, LookConfig config,
        final AvatarService.ConfirmListener cl)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject)caller;

        // sanity check
        if (user.handle != null && !user.handle.isBlank() &&
            user.getLook() != null) {
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

        // discourage the kiddies from being obviously vulgar
        if (NameFactory.getValidator().isVulgarHandle(handle)) {
            throw new InvocationException(AvatarCodes.ERR_VULGAR_HANDLE);
        }

        // go ahead and set the handle and gender in the user object
        user.setHandle(handle);
        user.setIsMale(isMale);

        // create their default look based on the supplied configuration
        int[] cost = new int[2];
        final Look look = BangServer.alogic.createLook(user, config, cost);
        if (look == null) {
            // an error will already have been logged
            throw new InvocationException(INTERNAL_ERROR);
        }

        // the client should prevent selection of components that have cost
        if (cost[0] > AvatarCodes.BASE_LOOK_SCRIP_COST ||
            cost[1] > AvatarCodes.BASE_LOOK_COIN_COST) {
            log.warning("Tried to create avatar with a non-zero cost look " +
                        "[who=" + user.who() + ", look=" + look +
                        ", scrip=" + cost[0] + ", coin=" + cost[1] + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        // do the deed!
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                // store the look in the database
                try {
                    BangServer.lookrepo.insertLook(user.playerId, look);
                    BangServer.playrepo.configurePlayer(
                        user.playerId, user.handle, user.isMale);
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Error creating avatar " +
                            "[for=" + user.who() + ", look=" + look + "].", pe);
                    _error = pe;
                }
                return true;
            }

            public void handleResult () {
                if (_error != null) {
                    cl.requestFailed(INTERNAL_ERROR);
                } else {
                    cl.requestProcessed();
                }
            }

            protected PersistenceException _error;
        });
    }

    // documentation inherited from interface AvatarProvider
    public void selectLook (ClientObject caller, String name)
    {
        PlayerObject user = (PlayerObject)caller;

        // sanity check
        Look look = (Look)user.looks.get(name);
        if (look == null) {
            log.warning("Player requested to select unknown look " +
                        "[who=" + user.who() + ", look=" + name + "].");
            return;
        }

        user.setLook(name);
    }

    @Override // documentation inherited
    protected Class getPlaceObjectClass ()
    {
        return BarberObject.class;
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
            _user.addToLooks(_look);
            _user.setLook(_look.name);
            _listener.requestProcessed();
        }
        protected void actionFailed () {
            _listener.requestFailed(INTERNAL_ERROR);
        }

        protected Look _look;
        protected BarberService.ConfirmListener _listener;
    }

    protected BarberObject _bobj;
}
