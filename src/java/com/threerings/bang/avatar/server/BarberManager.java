//
// $Id$

package com.threerings.bang.avatar.server;

import java.io.IOException;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;
import com.threerings.util.CompiledConfig;
import com.threerings.util.MessageBundle;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.cast.CharacterComponent;
import com.threerings.cast.NoSuchComponentException;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.avatar.client.BarberService;
import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberMarshaller;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.AspectCatalog;
import com.threerings.bang.avatar.util.AvatarMetrics;

import static com.threerings.bang.Log.log;

/**
 * Provides Barber-related services.
 */
public class BarberManager extends PlaceManager
    implements BarberCodes, BarberProvider
{
    // documentation inherited from interface BarberProvider
    public void purchaseLook (ClientObject caller, String name, String[] aspects,
                              BarberService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;

        if (StringUtil.blank(name)) {
            log.warning("Requested to create look with blank name " +
                        "[who=" + user.who() + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }

        String gender = user.isMale ? "male/" : "female/";
        int scrip = BarberCodes.BASE_LOOK_SCRIP_COST,
            coins = BarberCodes.BASE_LOOK_COIN_COST;
        ArrayIntSet compids = new ArrayIntSet();
        for (int ii = 0; ii < aspects.length; ii++) {
            AvatarMetrics.Aspect aclass = AvatarMetrics.ASPECTS[ii];
            String acname = gender + aclass.name;
            if (aspects[ii] == null) {
                if (!aclass.optional) {
                    log.warning("Requested to purchase look that is missing " +
                                "a non-optional aspect [who=" + user.who() +
                                ", class=" + acname + "].");
                    throw new InvocationException(INTERNAL_ERROR);
                }
                continue;
            }

            AspectCatalog.Aspect aspect = _aspcat.getAspect(acname, aspects[ii]);
            if (aspect == null) {
                log.warning("Requested to purchase a look with unknown aspect " +
                            "[who=" + user.who() + ", class=" + acname +
                            ", choice=" + aspects[ii] + "].");
                throw new InvocationException(INTERNAL_ERROR);
            }

            // add the cost to the total cost
            scrip += aspect.scrip;
            coins += aspect.coins;

            // look up the aspect's components
            for (int cc = 0; cc < aclass.classes.length; cc++) {
                String cclass = gender + aclass.classes[cc];
                try {
                    CharacterComponent ccomp = BangServer.comprepo.getComponent(
                        cclass, aspect.name);
                    compids.add(ccomp.componentId);
                } catch (NoSuchComponentException nsce) {
                    // no problem, some of these are optional
                }
            }
        }

        Look look = new Look();
        look.name = name;
        look.aspects = new int[compids.size()+1];
        // TODO: add proper hair and skin colorizations
        look.aspects[0] = (7 << 5) | 3;
        compids.toIntArray(look.aspects, 1);
        // TODO: copy articles from "active" look
        look.articles = new int[0];

        // the buy look action takes care of the rest (including checking that
        // they have sufficient funds)
        new BuyLookAction(user, look, scrip, coins, cl).start();
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
        if (articles == null || articles.length != AvatarMetrics.SLOTS.length) {
            log.warning("Requested to configure invalid articles array " +
                        "[who=" + user.who() + ", look=" + name +
                        ", articles=" + StringUtil.toString(articles) + "].");
            return;
        }

        // make sure all articles in the list are valid
        for (int ii = 0; ii < articles.length; ii++) {
            if (articles[ii] == 0) {
                if (AvatarMetrics.SLOTS[ii].optional) {
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
                !article.getSlot().equals(AvatarMetrics.SLOTS[ii].name)) {
                log.warning("Requested to configure look with invalid article " +
                            "[who=" + user.who() + ", article=" + article +
                            ", slot=" + AvatarMetrics.SLOTS[ii].name + "].");
                return;
            }
        }

        // put the new articles in place and update the user object
        look.articles = articles;
        look.modified = true;
        user.updateLooks(look);
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

        try {
            // load up our aspect catalog; this only happens at server startup,
            // so we don't mind doing disk I/O directly on the dobj thread
            _aspcat = (AspectCatalog)CompiledConfig.loadConfig(
                BangServer.rsrcmgr.getResource(AspectCatalog.CONFIG_PATH));
        } catch (IOException ioe) {
            log.log(Level.WARNING, "Failed to load aspect catalog.", ioe);
        }
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
    protected AspectCatalog _aspcat;
}
