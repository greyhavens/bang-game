//
// $Id$

package com.threerings.bang.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Invoker;
import com.samskivert.util.ListUtil;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.server.SpeakProvider;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.bang.avatar.data.BarberObject;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.bank.data.BankObject;
import com.threerings.bang.ranch.data.RanchCodes;
import com.threerings.bang.ranch.data.RanchObject;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.store.data.StoreObject;

import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.TutorialConfig;
import com.threerings.bang.game.util.TutorialUtil;

import com.threerings.bang.client.PlayerDecoder;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.server.persist.PardnerRepository;
import com.threerings.bang.server.persist.Player;

import static com.threerings.bang.Log.log;

/**
 * Handles general player business, implements {@link PlayerProvider}.
 */
public class PlayerManager
    implements PlayerProvider, BangCodes
{
    /**
     * Initializes the player manager, and registers its invocation service.
     */
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        _pardrepo = new PardnerRepository(conprov);

        // register ourselves as the provider of the (bootstrap) PlayerService
        BangServer.invmgr.registerDispatcher(new PlayerDispatcher(this), true);
    }

    // documentation inherited from interface PlayerProvider
    public void pickFirstBigShot (ClientObject caller, String type, Name name,
                                  final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        final PlayerObject user = (PlayerObject)caller;

        // sanity check: make sure they don't already have a big shot
        if (user.hasBigShot()) {
            log.warning("Player requested free big shot but already has one " +
                        "[who=" + user.who() + ", inventory=" + user.inventory +
                        ", type=" + type + "].");
            throw new InvocationException(RanchCodes.INTERNAL_ERROR);
        }

        // sanity check: make sure the big shot is valid
        UnitConfig config = UnitConfig.getConfig(type);
        if (config == null ||
            ListUtil.indexOf(RanchCodes.STARTER_BIGSHOTS, config.type) == -1) {
            log.warning("Player requested invalid free big shot " +
                        "[who=" + user.who() + ", type=" + type + "].");
            throw new InvocationException(RanchCodes.INTERNAL_ERROR);
        }

        // create the BigShot item and stuff it on into their inventory
        final BigShotItem bsitem = new BigShotItem(user.playerId, config.type);
        bsitem.setName(name);

        // stick the new item in the database and in their inventory
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                BangServer.itemrepo.insertItem(bsitem);
            }
            public void handleSuccess () {
                user.addToInventory(bsitem);
                listener.requestProcessed();
            }
            public String getFailureMessage () {
                return "Failed to add first big shot to repository " +
                    "[who=" + user.who() + ", item=" + bsitem + "]";
            }
        });
    }

    /**
     * Populates the identified player's set of pardners, performing any
     * notifications and updates that were being held until the player
     * logged on.  This is run on the invoker thread.
     */
    public void loadPardners (final PlayerObject player)
        throws PersistenceException
    {
        // set list of active pardners in dset, collect list of inviters
        ArrayList<PardnerEntry> pardners = new ArrayList<PardnerEntry>();
        ArrayList<PardnerRepository.PardnerRecord> records =
            _pardrepo.getPardnerRecords(player.playerId);

        // TEMP: sanity check since I've seen duplicates
        HashSet<Handle> temp = new HashSet<Handle>();
        Iterator<PardnerRepository.PardnerRecord> iter = records.iterator();
        while (iter.hasNext()) {
            PardnerRepository.PardnerRecord record = iter.next();
            if (temp.contains(record.handle)) {
                log.warning("Player has duplicate pardner record "  +
                            "[pid=" + player.playerId +
                            ", record=" + record + "].");
                iter.remove();
            } else {
                temp.add(record.handle);
            }
        }
        // END TEMP

        final ArrayList<PardnerRepository.PardnerRecord> inviters =
            new ArrayList<PardnerRepository.PardnerRecord>();
        for (int ii = 0, nn = records.size(); ii < nn; ii++) {
            PardnerRepository.PardnerRecord record = records.get(ii);
            if (record.active) {
                pardners.add(
                    getPardnerEntry(record.handle, record.lastSession));
            } else {
                inviters.add(record);
            }
        }

        player.pardners = new DSet(pardners.iterator());
        if (player.getOnlinePardnerCount() > 0) {
            new PardnerEntryUpdater(player).updatePardnerEntries();
        }

        // send invitations as soon as the receiver is registered
        player.addListener(new SetAdapter() {
            public void entryAdded (EntryAddedEvent eae) {
                if (!eae.getName().equals(PlayerObject.RECEIVERS) ||
                    !eae.getEntry().getKey().equals(
                        PlayerDecoder.RECEIVER_CODE)) {
                    return;
                }
                for (PardnerRepository.PardnerRecord inviter : inviters) {
                    sendPardnerInvite(player, inviter.handle,
                        inviter.lastSession, true);
                }
                player.removeListener(this);
            }
        });
    }

    // documentation inherited from interface PlayerProvider
    public void invitePardner (ClientObject caller, final Name handle,
        final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure it's not the player himself, that it's not already
        // a pardner, and that the player is under the limit
        final PlayerObject inviter = (PlayerObject)caller;
        if (inviter.handle.equals(handle)) {
            throw new InvocationException("e.pardner_self");

        } else if (inviter.pardners.containsKey(handle)) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.already_pardner", handle));

        } else if (inviter.pardners.size() >= MAX_PARDNERS) {
            throw new InvocationException(MessageBundle.tcompose(
                "e.too_many_pardners", String.valueOf(MAX_PARDNERS)));
        }

        // if the invitee is online, send the invite directly; if not, store
        // the invite in the db
        PlayerObject invitee = (PlayerObject)BangServer.lookupBody(handle);
        if (invitee != null) {
            String error = sendPardnerInvite(invitee, inviter.handle,
                new Date(), false);
            if (error == null) {
                listener.requestProcessed();

            } else {
                listener.requestFailed(error);
            }
            return;
        }
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                _error = _pardrepo.addPardners(inviter.playerId, handle,
                    false);
            }
            public void handleSuccess () {
                if (_error == null) {
                    listener.requestProcessed();

                } else {
                    listener.requestFailed(_error);
                }
            }
            public String getFailureMessage () {
                return "Failed to invite pardner [who=" + inviter.who() +
                    ", handle=" + handle + "]";
            }
            protected String _error;
        });
    }

    // documentation inherited from interface PlayerProvider
    public void respondToPardnerInvite (ClientObject caller,
        final Name inviter, final boolean resp,
        final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure the invite exists
        final PlayerObject player = (PlayerObject)caller;
        final Invite invite = _invites.get(
            new InviteKey(player.playerId, inviter));
        if (invite == null) {
            throw new InvocationException(INTERNAL_ERROR);
        }

        // if we're rejecting a non-db invite, there's no need to update the
        // db; otherwise, we must add, update, or remove pardners
        if (!resp && !invite.fromdb) {
            invite.reject(listener);
            return;
        }
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                if (resp) {
                    if (invite.fromdb) {
                        _pardrepo.updatePardners(player.playerId, inviter,
                            true);

                    } else {
                        _pardrepo.addPardners(player.playerId, inviter, true);
                    }

                } else {
                    _pardrepo.removePardners(player.playerId, inviter);
                }
            }
            public void handleSuccess () {
                if (resp) {
                    invite.accept(listener);

                } else {
                    invite.reject(listener);
                }
            }
            public String getFailureMessage () {
                return "Failed to respond to invite [who=" + player.who() +
                    ", inviter=" + inviter + ", resp=" + resp + "]";
            }
            protected String _error;
        });
    }

    // documentation inherited from interface PlayerProvider
    public void removePardner (ClientObject caller, final Name pardner,
        final PlayerService.ConfirmListener listener)
        throws InvocationException
    {
        // make sure the pardner entry is present
        final PlayerObject player = (PlayerObject)caller;
        PardnerEntry entry = (PardnerEntry)player.pardners.get(pardner);
        if (entry == null) {
            throw new InvocationException(INTERNAL_ERROR);
        }

        // remove from database and notify affected party on success
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                _pardrepo.removePardners(player.playerId, pardner);
            }
            public void handleSuccess () {
                player.removeFromPardners(pardner);
                PlayerObject pardobj =
                    (PlayerObject)BangServer.lookupBody(pardner);
                if (pardobj != null) {
                    pardobj.removeFromPardners(player.handle);
                    SpeakProvider.sendInfo(pardobj, BANG_MSGS,
                        MessageBundle.tcompose("m.pardner_ended",
                            player.handle));
                }
                listener.requestProcessed();
            }
            public String getFailureMessage () {
                return "Failed to remove pardner [who=" + player.who() +
                    ", pardner=" + pardner + "]";
            }
        });
    }

    // documentation inherited from interface PlayerProvider
    public void playTutorial (
        ClientObject caller, String tutid, PlayerService.InvocationListener il)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject)caller;

        // load up the tutorial configuration
        TutorialConfig tconfig =
            TutorialUtil.loadTutorial(BangServer.rsrcmgr, tutid);

        // create our AI opponent
        HashSet<String> names = new HashSet<String>();
        names.add(player.getVisibleName().toString());
        BangAI ai = BangAI.createAI(1, 50, names);

        // create a game configuration from that
        BangConfig config = new BangConfig();
        config.rated = false;
        config.players = new Name[] { player.getVisibleName(), ai.handle };
        config.ais = new BangAI[] { null, ai };
        config.scenarios = new String[] { tconfig.ident };
        config.tutorial = true;
        config.board = tconfig.board;

        // create the tutorial game manager and it will handle the rest
        try {
            BangServer.plreg.createPlace(config, null);
        } catch (InstantiationException ie) {
            log.log(Level.WARNING, "Error instantiating tutorial " +
                "[for=" + player.who() + ", config=" + config + "].", ie);
            throw new InvocationException(INTERNAL_ERROR);
        }
    }

    // documentation inherited from interface PlayerProvider
    public void playComputer (
        ClientObject caller, int players, String scenario, String board,
        PlayerService.InvocationListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject)caller;

        // sanity check the parameters
        if (players < 2 || players > GameCodes.MAX_PLAYERS) {
            throw new InvocationException(INTERNAL_ERROR);
        }
        // TODO: sanity check scenario type (make sure it corresponds to town)

        // we'll use this when creating our AIs
        HashSet<String> names = new HashSet<String>();
        names.add(player.getVisibleName().toString());

        // create a game configuration from that
        BangConfig config = new BangConfig();
        config.rated = false;
        config.players = new Name[players];
        config.ais = new BangAI[players];
        config.players[0] = player.getVisibleName();
        for (int ii = 1; ii < players; ii++) {
            BangAI ai = BangAI.createAI(1, 50, names);
            config.players[ii] = ai.handle;
            config.ais[ii] = ai;
        }
        config.scenarios = new String[] { scenario };
        config.board = board;

        // create the game manager and it will handle the rest
        try {
            BangServer.plreg.createPlace(config, null);
        } catch (InstantiationException ie) {
            log.log(Level.WARNING, "Error instantiating game " +
                    "[for=" + player.who() + ", config=" + config + "].", ie);
            throw new InvocationException(INTERNAL_ERROR);
        }
    }

    /**
     * Creates (if the pardner is offline) or retrieves (if the pardner is
     * online) the up-to-date {@link PardnerEntry} for the named pardner.
     * If the pardner is online and no {@link PardnerEntryUpdater} exists
     * for the pardner, one will be created, mapped, and used to keep the
     * {@link PardnerEntry} up-to-date.
     */
    protected PardnerEntry getPardnerEntry (Name handle, Date lastSession)
    {
        PardnerEntryUpdater updater = _updaters.get(handle);
        if (updater != null) {
            return updater.entry;
        }
        PlayerObject player = (PlayerObject)BangServer.lookupBody(handle);
        if (player != null) {
            return (new PardnerEntryUpdater(player)).entry;

        } else {
            return new PardnerEntry(handle, lastSession);
        }
    }

    /**
     * Sends a pardner invite to the specified player from the named inviter.
     *
     * @param fromdb if false, the invitation comes directly from an online
     * player; if true, the invitation comes from a stored entry in the
     * database
     * @return null if the invitation was sent successfully, otherwise a
     * translatable error message indicating what went wrong
     */
    protected String sendPardnerInvite (PlayerObject invitee, Name inviter,
        Date lastSession, boolean fromdb)
    {
        InviteKey key = new InviteKey(invitee.playerId, inviter);
        if (_invites.containsKey(key)) {
            return MessageBundle.tcompose("e.already_invited", invitee.handle);
        }
        PlayerSender.sendPardnerInvite(invitee, inviter);
        new Invite(key, lastSession, invitee, fromdb).add();
        return null;
    }

    /** Pairs inviter and invitee identification for use as a map key. */
    protected static class InviteKey
    {
        /** The player id of the invitee. */
        public int playerId;

        /** The name of the inviter. */
        public Name inviter;

        public InviteKey (int playerId, Name inviter)
        {
            this.playerId = playerId;
            this.inviter = inviter;
        }

        public boolean equals (Object other)
        {
            InviteKey okey = (InviteKey)other;
            return okey.playerId == playerId && okey.inviter.equals(inviter);
        }

        public int hashCode ()
        {
            return playerId + inviter.hashCode();
        }
    }

    /** Represents a standing invitation. */
    protected class Invite
        implements ObjectDeathListener
    {
        /** The key of this invitation. */
        public InviteKey key;

        /** The time at which the inviter was last on. */
        public Date lastSession;

        /** The invitee player object. */
        public PlayerObject invitee;

        /** Whether or not this invitation originated from the database. */
        public boolean fromdb;

        public Invite (InviteKey key, Date lastSession, PlayerObject invitee,
            boolean fromdb)
        {
            this.key = key;
            this.lastSession = lastSession;
            this.invitee = invitee;
            this.fromdb = fromdb;
        }

        public void add ()
        {
            invitee.addListener(this);
            _invites.put(key, this);
        }

        public void accept (PlayerService.ConfirmListener listener)
        {
            invitee.addToPardners(getPardnerEntry(key.inviter, lastSession));
            PlayerObject invobj =
                (PlayerObject)BangServer.lookupBody(key.inviter);
            if (invobj != null) {
                invobj.addToPardners(getPardnerEntry(invitee.handle, null));
                SpeakProvider.sendInfo(invobj, BANG_MSGS,
                    MessageBundle.tcompose("m.pardner_accepted",
                        invitee.handle));
            }
            remove();
            listener.requestProcessed();
        }

        public void reject (PlayerService.ConfirmListener listener)
        {
            PlayerObject invobj =
                (PlayerObject)BangServer.lookupBody(key.inviter);
            if (invobj != null) {
                SpeakProvider.sendInfo(invobj, BANG_MSGS,
                    MessageBundle.tcompose("m.pardner_rejected",
                        invitee.handle));
            }
            remove();
            listener.requestProcessed();
        }

        public void objectDestroyed (ObjectDestroyedEvent ode)
        {
            remove();
        }

        protected void remove ()
        {
            invitee.removeListener(this);
            _invites.remove(key);
        }
    }

    /** Listens to users with pardners, updating their pardner list entries. */
    protected class PardnerEntryUpdater extends SetAdapter
        implements AttributeChangeListener, ObjectDeathListener
    {
        /** The up-to-date entry for the player. */
        public PardnerEntry entry;

        public PardnerEntryUpdater (PlayerObject player)
        {
            _player = player;
            _player.addListener(this);
            _updaters.put(player.handle, this);

            entry = new PardnerEntry(player.handle);
            updateAvatar();
            updateStatus();
        }

        public void attributeChanged (AttributeChangedEvent ace)
        {
            String name = ace.getName();
            if (name.equals(PlayerObject.LOCATION)) {
                updateStatus();
                updatePardnerEntries();

            } else if (name.equals(PlayerObject.LOOK)) {
                updateAvatar();
                updatePardnerEntries();
            }
        }

        public void entryUpdated (EntryUpdatedEvent eue)
        {
            // if the current look is updated, update the avatar
            String name = eue.getName();
            if (name.equals(PlayerObject.LOOKS)) {
                Look look = (Look)eue.getEntry();
                if (look.name.equals(_player.look)) {
                    updateAvatar();
                    updatePardnerEntries();
                }

            } else if (name.equals(PlayerObject.PARDNERS) &&
                _player.getOnlinePardnerCount() == 0) {
                remove();
            }
        }

        public void entryRemoved (EntryRemovedEvent ere)
        {
            // if the last pardner is removed, clear out the updater
            if (ere.getName().equals(PlayerObject.PARDNERS) &&
                _player.getOnlinePardnerCount() == 0) {
                remove();
            }
        }

        public void objectDestroyed (ObjectDestroyedEvent ode)
        {
            updateStatus();
            updatePardnerEntries();
            remove();
        }

        public void updatePardnerEntries ()
        {
            for (Iterator it = _player.pardners.iterator(); it.hasNext(); ) {
                PlayerObject pardner = (PlayerObject)BangServer.lookupBody(
                    ((PardnerEntry)it.next()).handle);
                if (pardner != null) {
                    pardner.updatePardners(entry);
                }
            }
        }

        protected void remove ()
        {
            _player.removeListener(this);
            _updaters.remove(_player.handle);
        }

        protected void updateAvatar ()
        {
            Look look = (Look)_player.looks.get(_player.look);
            entry.avatar = look.getAvatar(_player);
        }

        protected void updateStatus ()
        {
            if (!_player.isActive()) {
                entry.status = PardnerEntry.OFFLINE;
                entry.avatar = null;
                entry.setLastSession(new Date());
                return;
            }
            Object plobj = BangServer.omgr.getObject(_player.location);
            if (plobj instanceof BangObject) {
                entry.status = PardnerEntry.IN_GAME;
            } else if (plobj instanceof SaloonObject) {
                entry.status = PardnerEntry.IN_SALOON;
            } else {
                entry.status = PardnerEntry.ONLINE;
            }
        }

        protected PlayerObject _player;
        protected PardnerEntry _entry;
    }

    /** Provides access to the pardner database. */
    protected PardnerRepository _pardrepo;

    /** Maps the names of users to updaters responsible for keeping their
     * {@link PardnerEntry}s up-to-date. */
    protected HashMap<Name, PardnerEntryUpdater> _updaters =
        new HashMap<Name, PardnerEntryUpdater>();

    /** The currently standing pardner invitations. */
    protected HashMap<InviteKey, Invite> _invites =
        new HashMap<InviteKey, Invite>();
}
