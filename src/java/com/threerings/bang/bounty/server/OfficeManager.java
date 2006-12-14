//
// $Id$

package com.threerings.bang.bounty.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;

import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.BoardRecord;

import com.threerings.bang.game.data.BangAI;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.server.BangManager;

import com.threerings.bang.bounty.client.OfficeService;
import com.threerings.bang.bounty.data.BoardInfo;
import com.threerings.bang.bounty.data.OfficeCodes;
import com.threerings.bang.bounty.data.OfficeMarshaller;
import com.threerings.bang.bounty.data.OfficeObject;

import static com.threerings.bang.Log.log;

/**
 * Handles the server side of the Sheriff's Office.
 */
public class OfficeManager extends PlaceManager
    implements OfficeCodes, OfficeProvider
{
    // from interface OfficeProvider
    public void playBountyGame (ClientObject caller, String ident,
                                OfficeService.InvocationListener listener)
        throws InvocationException
    {
        log.info("TODO: start " + ident);
    }

    // from interface OfficeProvider
    public void testBountyGame (ClientObject caller, BangConfig config,
                                OfficeService.InvocationListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject)caller;
        if (!player.tokens.isSupport()) {
            throw new InvocationException(ACCESS_DENIED);
        }

        HashSet<String> names = new HashSet<String>();
        names.add(player.getVisibleName().toString());

        // configure our AIs and the player names array
        config.type = BangConfig.Type.BOUNTY;
        config.players = new Name[config.teams.size()];
        config.ais = new BangAI[config.teams.size()];
        config.players[0] = player.getVisibleName();
        for (int ii = 1; ii < config.players.length; ii++) {
            BangAI ai = BangAI.createAI(1, 50, names);
            config.players[ii] = ai.handle;
            config.ais[ii] = ai;
        }

        try {
            BangServer.plreg.createPlace(config);

        } catch (InstantiationException ie) {
            log.log(Level.WARNING, "Error instantiating bounty game [for=" + player.who() +
                    ", config=" + config + "].", ie);
            throw new InvocationException(INTERNAL_ERROR);
        }
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
