//
// $Id$

package com.threerings.bang.saloon.server;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Rating;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.RatingRepository;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.game.data.scenario.ScenarioInfo;

import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.ParlorConfig;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonMarshaller;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.saloon.data.TopRankObject;
import com.threerings.bang.saloon.data.TopRankedList;

import static com.threerings.bang.Log.log;

/**
 * Implements the server side of the Saloon.
 */
@Singleton
public class SaloonManager extends MatchHostManager
    implements SaloonProvider
{
    /**
     * Refreshes the top-ranked lists for all scenarios (plus the overall rankings) in the
     * specified object.
     *
     * @param scenarios the scenarios to include (in addition to the overall scenario).
     * @param join an additional table to join, or <code>null</code> for none.
     * @param where additions to the where clause for the database query, or <code>null</code> for
     * none.
     * @param count the number of entries desired in each list.
     */
    public void refreshTopRanked (final TopRankObject rankobj, final String[] scenarios,
                                         final String join, final String where, final int count)
    {
        BangServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                String[] scens = ArrayUtil.append(scenarios, ScenarioInfo.OVERALL_IDENT);
                try {
                    _lists = _ratingrepo.loadTopRanked(scens, join, where, count, null);

                    // we don't start showing this week's top ranked until a couple days
                    // have passed
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -2);
                    Date thisWeek = Rating.thisWeek();
                    if (thisWeek.before(cal.getTime())) {
                        _lists.addAll(_ratingrepo.loadTopRanked(
                                scens, join, where, count, thisWeek));
                    } else {
                        _clearThisWeek = true;
                    }
                    _lists.addAll(_ratingrepo.loadTopRanked(
                            scens, join, where, count, Rating.getWeek(1)));
                    return true;

                } catch (PersistenceException pe) {
                    log.warning("Failed to load top-ranked players.", pe);
                    return false;
                }
            }

            public void handleResult () {
                // make sure we weren't shutdown while we were off invoking
                if (!((DObject)rankobj).isActive()) {
                    return;
                }
                if (_clearThisWeek) {
                    for (TopRankedList list : rankobj.getTopRanked().toArrayList()) {
                        if (list.period != TopRankedList.LIFETIME) {
                            rankobj.removeFromTopRanked(list.getKey());
                        }
                    }
                }
                for (TopRankedList list : _lists) {
                    commitTopRanked(rankobj, list);
                }
            }

            protected ArrayList<TopRankedList> _lists;
            protected boolean _clearThisWeek;
        });
    }

    // documentation inherited from interface SaloonProvider
    public void createParlor (PlayerObject caller, ParlorInfo.Type type, String password,
                              boolean matched, SaloonService.ResultListener rl)
        throws InvocationException
    {
        PlayerObject user = requireShopEnabled(caller);

        // creating back parlors requires the onetime pass
        if (DeploymentConfig.usesOneTime() && !user.holdsOneTime()) {
            throw new InvocationException(BangCodes.E_LACK_ONETIME);
        }

        // recruiting gangs are named after the gang
        Handle creator;
        int id = -1;
        if (type == ParlorInfo.Type.RECRUITING) {
            if (!user.canRecruit()) {
                log.warning("Non-recruiter tried to create recruiting parlor", "who", user.who());
                throw new InvocationException(INTERNAL_ERROR);
            }
            creator = BangServer.gangmgr.requireGang(user.gangId).getGangObject().name;
            id = user.gangId;
        } else {
            creator = user.handle;
        }

        // make sure they doesn't already have a parlor created
        if (_parlors.containsKey(creator)) {
            throw new InvocationException(ALREADY_HAVE_PARLOR);
        }

        createParlor(creator, type, password, matched, id, false, rl);
    }

    // documentation inherited from interface SaloonProvider
    public void joinParlor (PlayerObject caller, Handle creator, String password,
                            SaloonService.ResultListener rl)
        throws InvocationException
    {
        PlayerObject user = requireShopEnabled(caller);

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

    @Override // from ShopManager
    protected String getIdent ()
    {
        return "saloon";
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new SaloonObject();
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _salobj = (SaloonObject)_plobj;
        _salobj.setService(BangServer.invmgr.registerProvider(this, SaloonMarshaller.class));

        // create our default parlor
        createParlor(new Handle("!!!SERVER!!!"), ParlorInfo.Type.SOCIAL, null, true, 0, true, null);

        // start up our top-ranked list refresher interval
        _rankval = new Interval(BangServer.omgr) {
            public void expired () {
                refreshTopRanked(
                    _salobj, ScenarioInfo.getScenarioIds(ServerConfig.townId, false),
                    null, null, TOP_RANKED_LIST_SIZE);
            }
        };
        _rankval.schedule(1000L, RANK_REFRESH_INTERVAL);
    }

    @Override // from PlaceManager
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

    protected void createParlor (Handle creator, ParlorInfo.Type type, final String password,
            boolean matched, int gangId, boolean server, final SaloonService.ResultListener rl)
    {
        // create the new parlor
        final ParlorInfo info = new ParlorInfo();
        info.creator = creator;
        info.type = type;
        info.matched = matched;
        info.server = server;
        info.gangId = gangId;

        try {
            ParlorManager parmgr = (ParlorManager)BangServer.plreg.createPlace(new ParlorConfig());
            ParlorObject parobj = (ParlorObject)parmgr.getPlaceObject();
            parmgr.init(SaloonManager.this, info, password);
            _parlors.put(info.creator, parmgr);
            _salobj.addToParlors(info);
            if (rl != null) {
                rl.requestProcessed(parobj.getOid());
            }

        } catch (Exception e) {
            log.warning("Failed to create parlor " + info + ".", e);
            if (rl != null) {
                rl.requestFailed(INTERNAL_ERROR);
            }
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

        // if the parlor is shutting down during server shutdown we may not have our saloon object
        // anymore, in which case we need not worry about updating it
        if (_salobj != null) {
            _salobj.removeFromParlors(creator);
        }
    }

    protected static void commitTopRanked (final TopRankObject rankobj, final TopRankedList list)
    {
        int topRankId = (list.playerIds == null || list.playerIds.length == 0) ?
            0 : list.playerIds[0];
        if (list.week != null) {
            list.period = list.week.equals(Rating.thisWeek()) ?
                TopRankedList.THIS_WEEK : TopRankedList.LAST_WEEK;
        }
        BangServer.barbermgr.getSnapshot(topRankId, new ResultListener<AvatarInfo>() {
            public void requestCompleted (AvatarInfo snapshot) {
                list.topDogSnapshot = snapshot;
                commitList();
            }
            public void requestFailed (Exception cause) {
                log.warning("Failed to obtain top-ranked player snapshot", "list", list, cause);
                // ah well, we'll have no avatar
                commitList();
            }
            protected void commitList () {
                if (rankobj.getTopRanked().containsKey(list.getKey())) {
                    rankobj.updateTopRanked(list);
                } else {
                    rankobj.addToTopRanked(list);
                }
            }
        });
    }

    protected SaloonObject _salobj;
    protected Interval _rankval;
    protected Map<Handle,ParlorManager> _parlors = Maps.newHashMap();

    // dependencies
    @Inject protected RatingRepository _ratingrepo;

    /** The frequency with which we update the top-ranked player lists. */
    protected static final long RANK_REFRESH_INTERVAL = 60 * 60 * 1000L;

    /** The size of the top-ranked player lists. */
    protected static final int TOP_RANKED_LIST_SIZE = 10;
}
