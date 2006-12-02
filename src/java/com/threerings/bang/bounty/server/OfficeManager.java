//
// $Id$

package com.threerings.bang.bounty.server;

import java.util.ArrayList;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.BoardRecord;

import com.threerings.bang.bounty.data.BoardInfo;
import com.threerings.bang.bounty.data.BountyGameConfig;
import com.threerings.bang.bounty.data.OfficeMarshaller;
import com.threerings.bang.bounty.data.OfficeObject;

import static com.threerings.bang.Log.log;

/**
 * Handles the server side of the Sheriff's Office.
 */
public class OfficeManager extends PlaceManager
    implements OfficeProvider
{
    // from interface OfficeProvider
    public void testBountyGame (ClientObject caller, BountyGameConfig config)
    {
        log.info("TODO: start " + config);
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new OfficeObject();
    }

    @Override // documentation inherited
    protected long idleUnloadPeriod ()
    {
        // we don't want to unload
        return 0L;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();

        // register our invocation service
        _offobj = (OfficeObject)_plobj;
        _offobj.setService((OfficeMarshaller)BangServer.invmgr.registerDispatcher(
                               new OfficeDispatcher(this), false));

        // publish all known boards as board info records
        ArrayList<BoardInfo> infos = new ArrayList<BoardInfo>();
        for (int pp = 2; pp <= GameCodes.MAX_PLAYERS; pp++) {
            for (BoardRecord brec : BangServer.boardmgr.getBoards(pp)) {
                BoardInfo info = new BoardInfo();
                info.name = brec.name;
                info.players = brec.players;
                info.scenarios = brec.getScenarios();
                infos.add(info);
            }
        }
        _offobj.setBoards(new DSet<BoardInfo>(infos.iterator()));
    }

    protected OfficeObject _offobj;
}
