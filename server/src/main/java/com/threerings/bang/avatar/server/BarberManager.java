//
// $Id$

package com.threerings.bang.avatar.server;

import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.media.image.ColorPository;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangInvoker;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ShopManager;
import com.threerings.bang.server.persist.FinancialAction;
import com.threerings.bang.server.persist.ItemRepository;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.util.NameFactory;

import com.threerings.bang.avatar.client.AvatarService;
import com.threerings.bang.avatar.client.BarberService;
import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.avatar.data.AvatarMarshaller;
import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberMarshaller;
import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.avatar.server.persist.LookRepository;
import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.avatar.util.ColorConstraints;

import static com.threerings.bang.Log.log;

/**
 * Provides Barber-related services.
 */
@Singleton
public class BarberManager extends ShopManager
    implements BarberCodes, BarberProvider, AvatarProvider
{
    /**
     * Validates a handle requested by the given user, throwing an exception if
     * the handle is invalid.  This is used both for player handles and gang
     * name roots.
     */
    public static void validateHandle (PlayerObject user, Handle handle)
        throws InvocationException
    {
        // this should be prevented by the client
        if (!NameFactory.getValidator().isValidHandle(handle)) {
            log.warning("User tried to use invalid handle", "who", user.who(), "handle", handle);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // prevent the use of reserved names by non-admins
        if (!user.tokens.isSupport() &&
            NameFactory.getValidator().isReservedHandle(handle)) {
            throw new InvocationException(MessageBundle.qualify(
                AvatarCodes.AVATAR_MSGS, AvatarCodes.ERR_RESERVED_HANDLE));
        }

        // discourage the kiddies from being obviously vulgar
        if (NameFactory.getValidator().isVulgarHandle(handle)) {
            throw new InvocationException(MessageBundle.qualify(
                AvatarCodes.AVATAR_MSGS, AvatarCodes.ERR_VULGAR_HANDLE));
        }
    }

    /**
     * Returns an avatar snapshot for the specified player. If they are online, it will be obtained
     * from their loaded player object (and returned immediately), otherwise it will be loaded from
     * the database (and require an asynchronous reply).
     */
    public void getSnapshot (int playerId, ResultListener<AvatarInfo> listener)
    {
        // if they're online it's easy peasy
        PlayerObject user = BangServer.locator.lookupPlayer(playerId);
        if (user != null) {
            listener.requestCompleted(user.getLook(Look.Pose.WANTED_POSTER).getAvatar(user));
            return;
        }

        // otherwise we have to go to the database (TODO: cache these?)
        final int fpid = playerId;
        final ResultListener<AvatarInfo> flist = listener;
        _invoker.postUnit(new Invoker.Unit("loadSnapshot") {
            public boolean invoke () {
                try {
                    _snap = _lookrepo.loadSnapshot(fpid);
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

            protected AvatarInfo _snap;
            protected Exception _error;
        });
    }

    // from interface BarberProvider
    public void purchaseLook (PlayerObject caller, LookConfig config,
                              BarberService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject user = requireShopEnabled(caller);

        if (StringUtil.isBlank(config.name) ||
            config.name.length() > BarberCodes.MAX_LOOK_NAME_LENGTH) {
            log.warning("Requested to create look with blank name", "who", user.who());
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create the look from the specified configuration
        int[] cost = new int[2];
        Look look = _alogic.createLook(user, config, cost);
        if (look == null) {
            // an error will already have been logged
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure a look with the specified name does not already exist
        if (user.looks.contains(look)) {
            throw new InvocationException("m.name_already_used");
        }

        // copy the articles from their "active" look
        Look current = user.getLook(Look.Pose.DEFAULT);
        if (current != null) {
            look.articles = current.articles;
        } else {
            log.warning("Player has no current look from which to copy articles",
                        "who", user.who(), "poses", StringUtil.toString(user.poses));
            look.articles = new int[0];
        }

        // the buy look action takes care of the rest (including checking that
        // they have sufficient funds)
        _invoker.post(new BuyLookAction(user, look, cost[0], cost[1], cl));
    }

    // from interface BarberProvider
    public void configureLook (PlayerObject user, String name, int[] articles)
    {
        // locate the look in question
        Look look = user.looks.get(name);
        if (look == null) {
            log.warning("Asked to configure unknown look", "who", user.who(), "look", name,
                        "articles", StringUtil.toString(articles));
            return;
        }

        // sanity check
        if (articles == null || articles.length != AvatarLogic.SLOTS.length) {
            log.warning("Requested to configure invalid articles array", "who", user.who(),
                        "look", name, "articles", StringUtil.toString(articles));
            return;
        }

        // make sure all articles in the list are valid
        for (int ii = 0; ii < articles.length; ii++) {
            if (articles[ii] == 0) {
                if (AvatarLogic.SLOTS[ii].optional) {
                    continue;
                }
                log.warning("Requested to configure look with missing non-optional articles",
                            "who", user.who(), "idx", ii, "look", name,
                            "art", StringUtil.toString(articles));
                return;
            }

            Article article = (Article)user.inventory.get(articles[ii]);
            if (article == null || !article.isWearable(user) ||
                !article.getSlot().equals(AvatarLogic.SLOTS[ii].name)) {
                log.warning("Asked to configure look with invalid article", "who", user.who(),
                            "article", article, "slot", AvatarLogic.SLOTS[ii].name);
                return;
            }
        }

        // put the new articles in place and update the user object
        look.articles = articles;
        look.modified = true;
        user.updateLooks(look);
    }

    // from interface BarberProvider
    public void changeHandle (PlayerObject caller, Handle handle, BarberService.ConfirmListener cl)
        throws InvocationException
    {
        PlayerObject user = requireShopEnabled(caller);

        // make sure it's actually different
        if (user.handle.equals(handle)) {
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure their handle passes muster
        validateHandle(user, handle);

        // do the deed
        _invoker.post(new ChangeHandleAction(user, handle, cl));
    }

    // from interface AvatarProvider
    public void createAvatar (PlayerObject caller, final Handle handle, boolean isMale,
                              LookConfig config, int zations,
                              final AvatarService.ConfirmListener cl)
        throws InvocationException
    {
        final PlayerObject user = requireShopEnabled(caller);

        // sanity check
        if (user.hasCharacter() && user.getLook(Look.Pose.DEFAULT) != null) {
            log.warning("User tried to recreate avatar", "who", user.who(), "handle", handle);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // make sure their handle passes muster
        validateHandle(user, handle);

        // go ahead and set their gender in the user object
        user.setIsMale(isMale);

        // create their default look based on the supplied configuration
        int[] cost = new int[2];
        final Look look = _alogic.createLook(user, config, cost);
        if (look == null) {
            // an error will already have been logged
            throw new InvocationException(INTERNAL_ERROR);
        }

        // validate the starter article colorizations
        int czp = AvatarLogic.decodePrimary(zations);
        int czs = AvatarLogic.decodeSecondary(zations);
        int czt = AvatarLogic.decodeTertiary(zations);
        ColorPository cpos = _alogic.getColorPository();
        if (!ColorConstraints.isValidColor(cpos, "clothes_p", czp, user) ||
            !ColorConstraints.isValidColor(cpos, "clothes_s", czs, user) ||
            !ColorConstraints.isValidColor(cpos, "clothes_t", czt, user)) {
            log.warning("Tried to create avatar with invalid default article colorizations",
                        "who", user.who(), "look", look, "zations", zations);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // create their default clothing article, we'll fill in its item id after we've inserted
        // the article into the database
        look.articles = new int[AvatarLogic.SLOTS.length];
        final Article article = _alogic.createDefaultClothing(user, isMale, zations);
        if (article == null) {
            throw new InvocationException(INTERNAL_ERROR); // an error will have been logged
        }

        // the client should prevent selection of non-starter components, but we check on the
        // server because we can't trust those bastards
        int maxScrip = AvatarCodes.BASE_LOOK_SCRIP_COST;
        // allow up to the MAX_STARTER_COST for each aspect; this isn't precisely correct, but it
        // doesn't leave too much room for hackery
        maxScrip += AvatarCodes.MAX_STARTER_COST * config.aspects.length;
        if (cost[0] > maxScrip || cost[1] > AvatarCodes.BASE_LOOK_COIN_COST) {
            log.warning("Tried to create avatar with a non-zero cost look", "who", user.who(),
                        "look", look, "scrip", cost[0], "coin", cost[1]);
            throw new InvocationException(INTERNAL_ERROR);
        }

        // compute an avatar snapshot from their starter look
        final AvatarInfo avatar = look.getAvatar(
            user.who(), DSet.newDSet(Collections.<Item>singleton(article)));

        // do the deed!
        _invoker.postUnit(new PersistingUnit("createAvatar", cl) {
            public void invokePersistent () throws Exception {
                // first configure their chosen handle
                if (!_playrepo.configurePlayer(user.playerId, handle, user.isMale)) {
                    throw new InvocationException(AvatarCodes.ERR_DUP_HANDLE);
                }

                // insert their default clothing article into the database
                _itemrepo.insertItem(article);

                // and fill its assigned item id into their default look
                for (int ii = 0; ii < look.articles.length; ii++) {
                    if (AvatarLogic.SLOTS[ii].name.equals(article.getSlot())) {
                        look.articles[ii] = article.getItemId();
                    }
                }

                // insert their default look into the database and update their snapshot
                _lookrepo.insertLook(user.playerId, look);
                _lookrepo.updateSnapshot(user.playerId, avatar.print);
            }

            public void handleSuccess () {
                user.addToLooks(look);
                Handle ohandle = user.handle;
                user.setHandle(handle);
                user.addToInventory(article);
                // register the player with their handle as we didn't do so when they logged on
                BangServer.locator.updatePlayer(user, ohandle);
                super.handleSuccess(); // tell our confirm listener we're all done
            }
        });
    }

    // from interface AvatarProvider
    public void selectLook (PlayerObject user, Look.Pose pose, String name)
    {
        Look look = user.looks.get(name); // sanity check
        if (look == null) {
            log.warning("Player requested to select unknown look", "who", user.who(), "look", name);
            return;
        }
        user.setPosesAt(name, pose.ordinal());
    }

    @Override // from ShopManager
    protected boolean requireHandle ()
    {
        return true;
    }

    @Override // from ShopManager
    protected String getIdent ()
    {
        return "barber";
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new BarberObject();
    }

    @Override // from PlaceManager
    protected void didInit ()
    {
        super.didInit();

        // register ourselves as the AvatarService provider
        BangServer.invmgr.registerProvider(this, AvatarMarshaller.class, GLOBAL_GROUP);
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _bobj = (BarberObject)_plobj;
        _bobj.setService(BangServer.invmgr.registerProvider(this, BarberMarshaller.class));
    }

    /** Used to purchase a new avatar look. */
    protected static final class BuyLookAction extends FinancialAction
    {
        public BuyLookAction (PlayerObject user, Look look, int scripCost, int coinCost,
                              BarberService.ConfirmListener listener) {
            super(user, scripCost, coinCost);
            _look = look;
            _listener = listener;
        }

        @Override public boolean checkStart () throws InvocationException {
            boolean start = super.checkStart();
            // add the look immediately to prevent rapid fire purchase requests from overwriting
            // one another; we will remove the look later if we fail
            _user.addToLooks(_look);
            return start;
        }

        protected String getCoinDescrip () {
            return MessageBundle.compose("m.look_purchase", _look.name);
        }

        protected String persistentAction () throws PersistenceException {
            _lookrepo.insertLook(_user.playerId, _look);
            return null;
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            _lookrepo.deleteLook(_user.playerId, _look.name);
        }

        protected void actionCompleted () {
            _user.setPosesAt(_look.name, Look.Pose.DEFAULT.ordinal());
            _listener.requestProcessed();
            super.actionCompleted();
        }
        protected void actionFailed (String cause) {
            _user.removeFromLooks(_look.getKey());
            _listener.requestFailed(cause);
        }

        protected String getPurchaseType () {
            return "barber";
        }
        protected String getGoodType () {
            return "Look";
        }

        protected Look _look;
        protected BarberService.ConfirmListener _listener;

        @Inject protected LookRepository _lookrepo;
    }

    /** Used to purchase a handle change. */
    protected static final class ChangeHandleAction extends FinancialAction
    {
        public ChangeHandleAction (
            PlayerObject user, Handle handle,
            BarberService.ConfirmListener listener)
        {
            super(user, BarberCodes.HANDLE_CHANGE_SCRIP_COST,
                BarberCodes.HANDLE_CHANGE_COIN_COST);
            _ohandle = user.handle;
            _handle = handle;
            _listener = listener;
        }

        protected String getCoinDescrip () {
            return MessageBundle.compose("m.handle_change", _ohandle, _handle);
        }

        protected String persistentAction () throws PersistenceException {
            return _playrepo.configurePlayer(_user.playerId, _handle, _user.isMale) ?
                null : MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, AvatarCodes.ERR_DUP_HANDLE);
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            _playrepo.configurePlayer(_user.playerId, _ohandle, _user.isMale);
        }

        protected void actionCompleted () {
            Handle ohandle = _user.handle;
            _user.setHandle(_handle);
            BangServer.locator.updatePlayer(_user, ohandle);
            _listener.requestProcessed();
            super.actionCompleted();
        }
        protected void actionFailed (String cause) {
            _listener.requestFailed(cause);
        }

        protected String getPurchaseType () {
            return "barber";
        }
        protected String getGoodType () {
            return "Handle";
        }

        protected Handle _ohandle, _handle;
        protected BarberService.ConfirmListener _listener;
    }

    protected BarberObject _bobj;

    // dependencies
    @Inject protected BangInvoker _invoker;
    @Inject protected AvatarLogic _alogic;
    @Inject protected PlayerRepository _playrepo;
    @Inject protected ItemRepository _itemrepo;
    @Inject protected LookRepository _lookrepo;
}
