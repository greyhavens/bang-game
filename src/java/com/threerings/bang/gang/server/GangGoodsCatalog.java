//
// $Id$

package com.threerings.bang.gang.server;

import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.WeightClassUpgrade;
import com.threerings.bang.server.BangServer;

import com.threerings.bang.avatar.util.AvatarLogic;
import com.threerings.bang.avatar.util.BucklePartCatalog;

import com.threerings.bang.gang.data.BucklePartGood;
import com.threerings.bang.gang.data.BuckleUpgradeGood;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.WeightClassUpgradeGood;

import static com.threerings.bang.Log.log;

/**
 * Enumerates the various goods that can be purchased from the gang store and associates them
 * with providers that are used to actually create and deliver the goods when purchased.
 */
@Singleton
public class GangGoodsCatalog
    implements GangCodes
{
    /**
     * Creates a gang goods catalog, loading up the various bits necessary to create buckle
     * parts and such.
     */
    @Inject public GangGoodsCatalog (AvatarLogic alogic)
    {
        _alogic = alogic;

        // use the buckle part catalog to create goods for all buckle parts
        ProviderFactory pf = new BucklePartProviderFactory();
        BucklePartCatalog partcat = alogic.getBucklePartCatalog();
        for (String pclass : partcat.getClassNames()) {
            for (BucklePartCatalog.Part part : partcat.getParts(pclass)) {
                registerGood(new BucklePartGood(
                    pclass + "/" + part.name, part.scrip, part.coins, part.aces), pf);
            }
        }

        // add the weight class upgrades
        pf = new WeightClassUpgradeProviderFactory();
        for (byte ii = 0; ii < WEIGHT_CLASSES.length; ii++) {
            WeightClass wclass = WEIGHT_CLASSES[ii];
            registerGood(new WeightClassUpgradeGood(ii, 0, wclass.coins, wclass.aces), pf);
        }

        // and the buckle upgrades
        pf = new ItemProviderFactory();
        for (int ii = 0; ii < BUCKLE_UPGRADE_COSTS.length; ii++) {
            int[] costs = BUCKLE_UPGRADE_COSTS[ii];
            registerGood(new BuckleUpgradeGood(
                DEFAULT_MAX_BUCKLE_ICONS + ii + 1, 0, costs[0], costs[1]), pf);
        }
    }

    /**
     * Returns an array containing all available goods.
     */
    public GangGood[] getGoods ()
    {
        return _goods.toArray(new GangGood[_goods.size()]);
    }

    /**
     * Requests that a {@link GangGoodProvider} be created to provide the specified good to the
     * specified gang. Returns null if no provider is registered for the good in question.
     */
    public GangGoodProvider getProvider (
        GangObject gang, Handle handle, boolean admin, GangGood good, Object[] args)
        throws InvocationException
    {
        ProviderFactory factory = _providers.get(good);
        if (factory != null) {
            return factory.createProvider(gang, handle, admin, good, args);
        }
        return null;
    }

    /**
     * Registers a GangGood -> ProviderFactory mapping.
     */
    protected void registerGood (GangGood good, ProviderFactory factory)
    {
        _providers.put(good, factory);
        _goods.add(good);
    }

    /** Used to create a {@link Provider} for a particular {@link GangGood}. */
    protected abstract class ProviderFactory {
        public abstract GangGoodProvider createProvider (
            GangObject gang, Handle handle, boolean admin, GangGood good, Object[] args)
            throws InvocationException;
    }

    /** Used for generic items. */
    protected class ItemProviderFactory extends ProviderFactory {
        public GangGoodProvider createProvider (
            GangObject gang, Handle handle, boolean admin, GangGood good, Object[] args)
            throws InvocationException
        {
            return new GangItemProvider(gang, handle, admin, good, args);
        }
    }

    /** Used for {@link WeightClassUpgrade}s. */
    protected class WeightClassUpgradeProviderFactory extends ProviderFactory {
        public GangGoodProvider createProvider (
            GangObject gang, Handle handle, boolean admin, GangGood good, Object[] args)
            throws InvocationException
        {
            WeightClassUpgradeGood wgood = (WeightClassUpgradeGood)good;
            return new WeightClassUpgradeProvider(gang, handle, admin, wgood,
                    BangServer.hideoutmgr.upgradeCost(gang, wgood.getWeightClass()));
        }
    }

    /** Used for {@link BucklePartGood}s. */
    protected class BucklePartProviderFactory extends ProviderFactory {
        public GangGoodProvider createProvider (
            GangObject gang, Handle handle, boolean admin, GangGood good, Object[] args)
            throws InvocationException
        {
            return new GangItemProvider(gang, handle, admin, good, args) {
                protected Item createItem () throws InvocationException {
                    BucklePartGood pgood = (BucklePartGood)_good;
                    BucklePartCatalog.Part part = _alogic.getBucklePartCatalog().getPart(
                        pgood.getPartClass(), pgood.getPartName());
                    if (part == null) {
                        log.warning("Requested to create buckle part for unknown catalog entry",
                                    "gang", _gang, "good", _good);
                        throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
                    }
                    // our arguments are colorization ids
                    int zations = AvatarLogic.composeZations(
                        (Integer)_args[0], (Integer)_args[1], (Integer)_args[2]);
                    Item item = _alogic.createBucklePart(_gang.gangId, part, zations);
                    if (item == null) {
                        throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
                    }
                    return item;
                }
            };
        }
    }

    protected class WeightClassUpgradeProvider extends GangItemProvider
    {
        public WeightClassUpgradeProvider (GangObject gang, Handle handle, boolean admin,
                WeightClassUpgradeGood good, int[] cost)
            throws InvocationException
        {
            super(gang, handle, admin, good, null);
            _coinCost += cost[0];
            _scripCost += cost[1];
            _oldWeightClass = gang.getWeightClass();
            _oldWeightUpgrades = new ArrayIntSet();
            byte weightClass = good.getWeightClass();
            if (weightClass < _oldWeightClass) {
                _pct = (float)GangCodes.WEIGHT_CLASSES[weightClass].maxMembers /
                    (float)GangCodes.WEIGHT_CLASSES[_oldWeightClass].maxMembers;
            }
            for (Item item : gang.inventory) {
                if (item instanceof WeightClassUpgrade) {
                    _oldWeightUpgrades.add(item.getItemId());
                }
            }
        }
        protected String persistentAction () throws PersistenceException {
            String result = super.persistentAction();
            if (result == null) {
                if (!_oldWeightUpgrades.isEmpty()) {
                    _itemrepo.deleteItems(_oldWeightUpgrades, "replaced upgrade");
                }
                if (_pct > 0 && _pct < 1) {
                    _gangrepo.reduceNotoriety(_gang.gangId, _pct);
                }
                _gangrepo.updateWeightClass(
                    _gang.gangId, ((WeightClassUpgrade)_item).getWeightClass());
            }
            return result;
        }
        protected void rollbackPersistentAction () throws PersistenceException {
            super.rollbackPersistentAction();
            _gangrepo.updateWeightClass(_gang.gangId, _oldWeightClass);
        }
        protected void actionCompleted ()
        {
            for (int itemId : _oldWeightUpgrades) {
                _gang.removeFromInventory(itemId);
            }
            if (_pct > 0 && _pct < 1) {
                try {
                    GangHandler handler = BangServer.gangmgr.requireGang(_gang.gangId);
                    handler.setNotoriety((int)(_gang.notoriety * _pct));
                } catch (InvocationException ie) {
                    // should never happen
                }
            }
            super.actionCompleted();
        }

        protected byte _oldWeightClass;
        protected ArrayIntSet _oldWeightUpgrades;
        protected float _pct;
    }

    protected AvatarLogic _alogic;

    /** All available gang goods. */
    protected ArrayList<GangGood> _goods = new ArrayList<GangGood>();

    /** Contains mappings from {@link GangGood} to {@link ProviderFactory} for all salable goods. */
    protected Map<GangGood, ProviderFactory> _providers = Maps.newHashMap();

    /** The cost in coins/aces for each of the buckle upgrades. */
    protected static final int[][] BUCKLE_UPGRADE_COSTS = {
        { 5, 200 }, { 10, 400 }, { 20, 700 }, { 40, 1000 }, { 80, 2000 } };
}
