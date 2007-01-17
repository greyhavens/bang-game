//
// $Id$

package com.threerings.bang.saloon.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.server.BangManager;

import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.ParlorConfig;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonMarshaller;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.saloon.data.TopRankObject;
import com.threerings.bang.saloon.data.TopRankedList;

import static com.threerings.bang.Log.log;

/**
 * Implements the server side of the Saloon.
 */
public class SaloonManager extends MatchHostManager
    implements SaloonProvider
{
    /**
     * Refreshes the top-ranked lists for all scenarios (plus the overall rankings) in the
     * specified object.
     *
     * @param join an additional table to join, or <code>null</code> for none
     * @param where additions to the where clause for the database query, or <code>null</code> for
     * none
     * @param count the number of entries desired in each list
     */
    public static void refreshTopRanked (
        final TopRankObject rankobj, final String join, final String where, final int count)
    {
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                ArrayList<String> scens = new ArrayList<String>();
                CollectionUtil.addAll(
                    scens, ScenarioInfo.getScenarioIds(ServerConfig.townId, false));
                scens.add(0, ScenarioInfo.OVERALL_IDENT);

                try {
                    _lists = BangServer.ratingrepo.loadTopRanked(
                        scens.toArray(new String[scens.size()]), join, where, count);
                    return true;

                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to load top-ranked players.", pe);
                    return false;
                }
            }

            public void handleResult () {
                // make sure we weren't shutdown while we were off invoking
                if (!((DObject)rankobj).isActive()) {
                    return;
                }
                for (TopRankedList list : _lists) {
                    commitTopRanked(rankobj, list);
                }
            }

            protected ArrayList<TopRankedList> _lists;
        });
    }

    // documentation inherited from interface SaloonProvider
    public void createParlor (ClientObject caller, boolean pardnersOnly, final String password,
                              final SaloonService.ResultListener rl)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;

        // make sure this player doesn't already have a parlor created
        if (_parlors.containsKey(user.handle)) {
            throw new InvocationException(ALREADY_HAVE_PARLOR);
        }

        // create the new parlor
        final ParlorInfo info = new ParlorInfo();
        info.creator = user.handle;
        info.pardnersOnly = pardnersOnly;
        info.passwordProtected = !StringUtil.isBlank(password);

        try {
            ParlorManager parmgr = (ParlorManager)BangServer.plreg.createPlace(new ParlorConfig());
            ParlorObject parobj = (ParlorObject)parmgr.getPlaceObject();
            parmgr.init(SaloonManager.this, info, password);
            _parlors.put(info.creator, parmgr);
            _salobj.addToParlors(info);
            rl.requestProcessed(parobj.getOid());

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create parlor " + info + ".", e);
            rl.requestFailed(INTERNAL_ERROR);
        }
    }

    // documentation inherited from interface SaloonProvider
    public void joinParlor (ClientObject caller, Handle creator, String password,
                            SaloonService.ResultListener rl)
        throws InvocationException
    {
        PlayerObject user = (PlayerObject)caller;

        // locate the parlor in question
        ParlorManager parmgr = _parlors.get(creator);
        if (parmgr == null) {
            throw new InvocationException(NO_SUCH_PARLOR);
        }

        // make sure they meet the entry requirements
        parmgr.ratifyEntry(user, password);

        // they've run the gauntlet, let 'em in
        rl.requestProcessed(parmgr.getPlaceObject().getOid());
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new SaloonObject();
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _salobj = (SaloonObject)_plobj;
        _salobj.setService((SaloonMarshaller)
                           BangServer.invmgr.registerDispatcher(new SaloonDispatcher(this), false));

        // start up our top-ranked list refresher interval
        _rankval = new Interval(BangServer.omgr) {
            public void expired () {
                refreshTopRanked(_salobj, null, null, TOP_RANKED_LIST_SIZE);
            }
        };
        _rankval.schedule(1000L, RANK_REFRESH_INTERVAL);
    }

    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();

        // clear out our invocation service
        if (_salobj != null) {
            BangServer.invmgr.clearDispatcher(_salobj.service);
            _salobj = null;
        }

        // stop our top-ranked list refresher
        if (_rankval != null) {
            _rankval.cancel();
            _rankval = null;
        }
    }

    protected void parlorUpdated (ParlorInfo info)
    {
        _salobj.updateParlors(info);
    }

    protected void parlorDidShutdown (ParlorManager parmgr)
    {
        ParlorObject parobj = (ParlorObject)parmgr.getPlaceObject();
        Handle creator = parobj.info.creator;
        _parlors.remove(creator);
        _salobj.removeFromParlors(creator);
    }

    protected static void commitTopRanked (final TopRankObject rankobj, final TopRankedList list)
    {
        list.criterion = MessageBundle.qualify(GameCodes.GAME_MSGS, "m.scenario_" + list.criterion);
        int topRankId = (list.playerIds == null || list.playerIds.length == 0) ?
            0 : list.playerIds[0];
        BangServer.barbermgr.getSnapshot(
            topRankId, new ResultListener<int[]>() {
            public void requestCompleted (int[] snapshot) {
                list.topDogSnapshot = snapshot;
                commitList();
            }
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Failed to obtain top-ranked player snapshot " +
                        "[list=" + list + "].", cause);
                // ah well, we'll have no avatar
                commitList();
            }
            protected void commitList () {
                if (rankobj.getTopRanked().containsKey(list.criterion)) {
                    rankobj.updateTopRanked(list);
                } else {
                    rankobj.addToTopRanked(list);
                }
            }
        });
    }

    protected SaloonObject _salobj;
    protected Interval _rankval;
    protected HashMap<Handle,ParlorManager> _parlors = new HashMap<Handle,ParlorManager>();

    /** The frequency with which we update the top-ranked player lists. */
    protected static final long RANK_REFRESH_INTERVAL = 60 * 60 * 1000L;

    /** The size of the top-ranked player lists. */
    protected static final int TOP_RANKED_LIST_SIZE = 10;
}
