//
// $Id$

package com.threerings.bang.data;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.stats.data.ByteByteStringMapStat;
import com.threerings.stats.data.ByteStringSetStat;
import com.threerings.stats.data.IntArrayStat;
import com.threerings.stats.data.IntStat;
import com.threerings.stats.data.MaxIntStat;
import com.threerings.stats.data.ShortStringSetStat;
import com.threerings.stats.data.Stat;

import com.threerings.bang.data.BangCodes;

/**
 * Enumerates the various stats used in Bang! Howdy.
 */
public enum StatType implements Stat.Type
{
    // general statistics
    GAMES_PLAYED(new IntStat(), false, false),
    UNRANKED_GAMES_PLAYED(new IntStat(), false, false),
    GAMES_WON(new IntStat(), false, false),
    GAME_TIME(new MaxIntStat(), false, false),
    CONSEC_WINS(new IntStat(), false, false),
    CONSEC_LOSSES(new IntStat(), false, false),
    CONSEC_KILLS(new IntStat(), true, true), // most consec kills by one unit in one round
    LATE_NIGHTS(new IntStat(), false, false, true, true),
    TUTORIALS_COMPLETED(new ByteStringSetStat(), false, false, true, true),

    // transient (per-session) statistics
    SESSION_GAMES_PLAYED(new IntStat(), false, false, false, true),

    // stats accumulated during a game (not persisted)
    DAMAGE_DEALT(new MaxIntStat(), true, false, false, false),
    BONUS_POINTS(new MaxIntStat(), true, false, false, false),
    PACK_CARDS_PLAYED(new IntStat(), false, true, false, false),

    // stats accumulated during a game (persisted)
    UNITS_KILLED(new MaxIntStat(), true, true),
    UNITS_LOST(new MaxIntStat(), true, true),
    BONUSES_COLLECTED(new MaxIntStat(), true, false),
    CARDS_PLAYED(new MaxIntStat(), true, false),
    POINTS_EARNED(new IntStat(), true, false), // accumulated for whole game
    HIGHEST_POINTS(new IntStat(), false, true), // max points in single round
    CASH_EARNED(new MaxIntStat(), false, false),
    DISTANCE_MOVED(new MaxIntStat(), true, false),
    SHOTS_FIRED(new MaxIntStat(), true, false),
    UNITS_USED(new ByteByteStringMapStat(), false, false, true, true),
    BIGSHOT_WINS(new ByteByteStringMapStat(), false, false, true, true),

    PACK_CARD_WINS(new IntStat(), false, true), // brought cards into game and won
    BLUFF_CARD_WINS(new IntStat(), false, true), // brought 3 cards into game, played none, won

    CATTLE_RUSTLED(new MaxIntStat(), true, false),
    BRAND_POINTS(new MaxIntStat(), true, false),
    MOST_CATTLE(new IntStat(), true, false), // most cattle branded any time during round

    NUGGETS_CLAIMED(new MaxIntStat(), true, false),

    STEADS_CLAIMED(new MaxIntStat(), true, false),
    STEADS_DESTROYED(new MaxIntStat(), true, false),
    STEAD_POINTS(new MaxIntStat(), true, false),
    LONE_STEADER(new IntStat(), false, false), // all claimed steads are yours

    TOTEMS_SMALL(new MaxIntStat(), true, false),
    TOTEMS_MEDIUM(new MaxIntStat(), true, false),
    TOTEMS_LARGE(new MaxIntStat(), true, false),
    TOTEMS_CROWN(new MaxIntStat(), true, false),
    TOTEM_POINTS(new MaxIntStat(), true, false),

    WENDIGO_SURVIVALS(new MaxIntStat(), true, false),
    TALISMAN_POINTS(new MaxIntStat(), true, false),
    TALISMAN_SPOT_SURVIVALS(new MaxIntStat(), true, false),
    WHOLE_TEAM_SURVIVALS(new IntStat(), true, false),

    TREES_SAPLING(new MaxIntStat(), true, false),
    TREES_MATURE(new MaxIntStat(), true, false),
    TREES_ELDER(new MaxIntStat(), true, false),
    TREE_POINTS(new MaxIntStat(), true, false),
    WAVE_SCORES(new IntArrayStat(), false, false),
    WAVE_POINTS(new MaxIntStat(), true, false),
    HARD_ROBOT_KILLS(new MaxIntStat(), true, false),
    PERFECT_WAVES(new IntStat(), true, false),
    HIGHEST_SAWS(new IntStat(), false, false),

    HERO_LEVEL(new MaxIntStat(), true, false),
    TOP_LEVEL(new IntStat(), true, false),
    HERO_KILLING(new IntStat(), true, false),

    // stats accumulated outside a game
    CHAT_SENT(new IntStat(), false, false),
    CHAT_RECEIVED(new IntStat(), false, false),
    GAMES_HOSTED(new IntStat(), false, false),

    // bounty related stats
    BOUNTIES_COMPLETED(new ShortStringSetStat(), false, false),
    BOUNTY_GAMES_COMPLETED(new ShortStringSetStat(), false, false),

    // free ticket related stats
    FREE_TICKETS(new ShortStringSetStat(), false, false),
    ACTIVATED_TICKETS(new ShortStringSetStat(), false, false),

    // weekly top ranked stats
    WEEKLY_TOP10(new ShortStringSetStat(), false, false),
    WEEKLY_WINNER(new ShortStringSetStat(), false, false),

    // stats that are meant to by mysterious
    MYSTERY_ONE(new IntStat(), false, false, false, true), // high noon logon
    MYSTERY_TWO(new IntStat(), false, false, false, true), // christmas morning game
    MYSTERY_THREE(new IntStat(), false, false, true, true), // new sheriff in town

    UNUSED(new IntStat(), false, false);

    /** Returns the translation key used by this stat. */
    public String key ()
    {
        return MessageBundle.qualify(
            BangCodes.STATS_MSGS, "m.stat_" + StringUtil.toUSLowerCase(name()));
    }

    /** Returns true if this stat is pertinent to bounty games. */
    public boolean isBounty ()
    {
        return _bounty;
    }

    /** Returns true if this stat should only be tracked in non-coop games. */
    public boolean isCompetitiveOnly ()
    {
        return _competitiveOnly;
    }

    /** Returns true if this stat is not shown in the stats display. */
    public boolean isHidden ()
    {
        return _hidden;
    }

    // from interface Stat.Type
    public Stat newStat ()
    {
        return _prototype.clone();
    }

    // from interface Stat.Type
    public int code ()
    {
        return _code;
    }

    // from interface Stat.Type
    public boolean isPersistent () {
        return _persist;
    }

    // most stats are persistent and not hidden
    StatType (Stat prototype, boolean bounty, boolean compOnly)
    {
        this(prototype, bounty, compOnly, true, false);
    }

    StatType (Stat prototype, boolean bounty, boolean compOnly, boolean persist, boolean hidden)
    {
        _bounty = bounty;
        _competitiveOnly = compOnly;
        _persist = persist;
        _hidden = hidden;

        _prototype = prototype;

        // configure our prototype and map ourselves into the Stat system
        _code = Stat.initType(this, _prototype);
    }

    protected Stat _prototype;
    protected int _code;
    protected boolean _bounty, _competitiveOnly, _persist, _hidden;
}
