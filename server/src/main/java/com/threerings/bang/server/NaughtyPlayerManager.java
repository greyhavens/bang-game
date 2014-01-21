//
// $Id$

package com.threerings.bang.server;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Throttle;

import com.google.inject.Singleton;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.data.PlayerObject;

import static com.threerings.bang.Log.log;

/**
 * Players behave naughtily in a variety of ways and we encapsulate all the code that attempts to
 * deal with those players into this manager.
 */
@Singleton
public class NaughtyPlayerManager
{
    /**
     * Returns true if we should grant scrip to the specified player on completion of the specified
     * game. This is called at the start of the game so that we can notify the player that they're
     * not going to get any reward before they take the time to play the game.
     */
    public boolean shouldGrantScrip (PlayerObject user, BangConfig config)
    {
        // non saloon games can always grant scrip because they have their own intrinsic anti-abuse
        // mechanism (you have to win the bounty games to get anything, tutorials only award scrip
        // the first time, practice games never award scrip)
        if (config.type != BangConfig.Type.SALOON) {
            return true;
        }

        // rated saloon games also always award scrip (for now)
        if (config.rated) {
            return true;
        }

        // check the unrated payouts throttle for this player
        Throttle throttle = _unratedPerDay.get(user.playerId);
        if (throttle == null) {
            _unratedPerDay.put(
                user.playerId, throttle = new Throttle(UNRATED_PAYOUTS_PER_DAY, ONE_DAY));
        }

        if (throttle.throttleOp()) {
            // TEMP: remove this when we're satisfied that nothing funny is going on
            log.info("Preventing unrated payout", "who", user.who());
            return false;
        }

        return true;
    }

    protected HashIntMap<Throttle> _unratedPerDay = new HashIntMap<Throttle>();

    /** We allow 10 unrated games to pay out per day and then cut the player off. */
    protected static final int UNRATED_PAYOUTS_PER_DAY = 10;

    /** One (non-daylight-savings-time adjusted) day in glorious milliseconds. */
    protected static final long ONE_DAY = 24 * 60 * 60 * 1000L;
}
