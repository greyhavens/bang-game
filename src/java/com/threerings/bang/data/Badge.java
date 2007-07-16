//
// $Id$

package com.threerings.bang.data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.jmex.bui.BImage;
import com.jmex.bui.icon.ImageIcon;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.threerings.util.MessageBundle;

import com.threerings.stats.data.StatSet;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.scenario.CattleRustlingInfo;
import com.threerings.bang.game.data.scenario.ClaimJumpingInfo;
import com.threerings.bang.game.data.scenario.ForestGuardiansInfo;
import com.threerings.bang.game.data.scenario.GoldRushInfo;
import com.threerings.bang.game.data.scenario.LandGrabInfo;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.game.data.scenario.TotemBuildingInfo;
import com.threerings.bang.game.data.scenario.WendigoAttackInfo;

import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Represents a badge earned by the player by meeting some specified
 * criterion.
 */
public class Badge extends Item
{
    /** Defines the various badge types. */
    public static enum Type
    {
        DAILY_HIGH_SCORER, // daily high scorer
        WEEKLY_HIGH_SCORER, // weekly
        MONTHLY_HIGH_SCORER, // monthly

        // games played badges
        GAMES_PLAYED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.GAMES_PLAYED) >= 5;
            }
        },
        GAMES_PLAYED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.GAMES_PLAYED) >= 50;
            }
        },
        GAMES_PLAYED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.GAMES_PLAYED) >= 500;
            }
        },
        GAMES_PLAYED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.GAMES_PLAYED) >= 2000;
            }
        },
        GAMES_PLAYED_5 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.GAMES_PLAYED) >= 5000;
            }
        },

        // units killed badges
        UNITS_KILLED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.UNITS_KILLED) >= 50;
            }
        },
        UNITS_KILLED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.UNITS_KILLED) >= 500;
            }
        },
        UNITS_KILLED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.UNITS_KILLED) >= 5000;
            }
        },
        UNITS_KILLED_4 {
            public boolean qualifies (PlayerObject user) {
                float killRatio = user.stats.getIntStat(StatType.UNITS_KILLED) /
                    Math.max(1f, user.stats.getIntStat(StatType.UNITS_LOST));
                return UNITS_KILLED_2.qualifies(user) && (killRatio >= 1.7f);
            }
        },
        UNITS_KILLED_5 {
            public boolean qualifies (PlayerObject user) {
                float killRatio = user.stats.getIntStat(StatType.UNITS_KILLED) /
                    Math.max(1f, user.stats.getIntStat(StatType.UNITS_LOST));
                return UNITS_KILLED_3.qualifies(user) && (killRatio >= 1.5f);
            }
        },

        // highest points badges
        HIGHEST_POINTS_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.HIGHEST_POINTS) >= 500;
            }
        },
        HIGHEST_POINTS_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.HIGHEST_POINTS) >= 800;
            }
        },

        // consecutive kills badges
        CONSEC_KILLS_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CONSEC_KILLS) >= 5;
            }
        },
        CONSEC_KILLS_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CONSEC_KILLS) >= 10;
            }
        },
        CONSEC_KILLS_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CONSEC_KILLS) >= 15;
            }
        },

        // consecutive wins badges (wins means first place)
        CONSEC_WINS_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CONSEC_WINS) >= 5;
            }
        },
        CONSEC_WINS_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CONSEC_WINS) >= 15;
            }
        },
        CONSEC_WINS_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CONSEC_WINS) >= 30;
            }
        },
        CONSEC_WINS_4 {
            public boolean qualifies (PlayerObject user) {
                int wins = user.stats.getIntStat(StatType.GAMES_WON);
                int losses = Math.max(user.stats.getIntStat(StatType.GAMES_PLAYED) - wins, 1);
                return CONSEC_WINS_2.qualifies(user) && ((wins / (float)losses) >= 2f);
            }
        },
        CONSEC_WINS_5 {
            public boolean qualifies (PlayerObject user) {
                int wins = user.stats.getIntStat(StatType.GAMES_WON);
                int losses = Math.max(user.stats.getIntStat(StatType.GAMES_PLAYED) - wins, 1);
                return CONSEC_WINS_3.qualifies(user) && ((wins / (float)losses) >= 3f);
            }
        },

        // you suck badges
        UNITS_LOST_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.UNITS_LOST) >= 100;
            }
        },
        UNITS_LOST_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.UNITS_LOST) >= 10000;
            }
        },

        // consecutive losses (fourth place only, not non-first place or even
        // last place)
        CONSEC_LOSSES_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CONSEC_LOSSES) >= 5;
            }
        },
        CONSEC_LOSSES_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CONSEC_LOSSES) >= 15;
            }
        },

        // shots fired badges
        SHOTS_FIRED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.SHOTS_FIRED) >= 1000;
            }
        },
        SHOTS_FIRED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.SHOTS_FIRED) >= 25000;
            }
        },

        // distance moved badges
        DISTANCE_MOVED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.DISTANCE_MOVED) >= 1000;
            }
        },
        DISTANCE_MOVED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.DISTANCE_MOVED) >= 50000;
            }
        },
        DISTANCE_MOVED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.DISTANCE_MOVED) >= 500000;
            }
        },

        // cards played badges
        CARDS_PLAYED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CARDS_PLAYED) >= 50;
            }
        },
        CARDS_PLAYED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CARDS_PLAYED) >= 500;
            }
        },
        CARDS_PLAYED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CARDS_PLAYED) >= 5000;
            }
        },
        CARDS_PLAYED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.BLUFF_CARD_WINS) >= 30;
            }
        },
        CARDS_PLAYED_5 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.PACK_CARD_WINS) >= 250;
            }
        },

        // bonuses collected badges
        BONUSES_COLLECTED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.BONUSES_COLLECTED) >= 100;
            }
        },
        BONUSES_COLLECTED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.BONUSES_COLLECTED) >= 1000;
            }
        },
        BONUSES_COLLECTED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.BONUSES_COLLECTED) >= 10000;
            }
        },
        BONUSES_COLLECTED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMaxIntStat(StatType.BONUSES_COLLECTED) >= 6;
            }
        },
        BONUSES_COLLECTED_5 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMaxIntStat(StatType.BONUSES_COLLECTED) >= 10;
            }
        },

        // cash earned badges
        CASH_EARNED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CASH_EARNED) >= 10000;
            }
        },
        CASH_EARNED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CASH_EARNED) >= 100000;
            }
        },
        CASH_EARNED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CASH_EARNED) >= 1000000;
            }
        },

        // nuggets claimed badges
        NUGGETS_CLAIMED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.NUGGETS_CLAIMED) >= 10;
            }
        },
        NUGGETS_CLAIMED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.NUGGETS_CLAIMED) >= 100;
            }
        },
        NUGGETS_CLAIMED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.NUGGETS_CLAIMED) >= 1000;
            }
        },
        NUGGETS_CLAIMED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.NUGGETS_CLAIMED) >= 10000;
            }
        },
        NUGGETS_CLAIMED_5 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMaxIntStat(StatType.NUGGETS_CLAIMED) >= 10;
            }
        },

        // cattle rustled badges
        CATTLE_RUSTLED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CATTLE_RUSTLED) >= 10;
            }
        },
        CATTLE_RUSTLED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CATTLE_RUSTLED) >= 100;
            }
        },
        CATTLE_RUSTLED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CATTLE_RUSTLED) >= 1000;
            }
        },
        CATTLE_RUSTLED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CATTLE_RUSTLED) >= 10000;
            }
        },
        CATTLE_RUSTLED_5 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.MOST_CATTLE) >= 9;
            }
        },

        // homesteads claimed badges
        STEADS_CLAIMED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.STEADS_CLAIMED) >= 10;
            }
        },
        STEADS_CLAIMED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.STEADS_CLAIMED) >= 100;
            }
        },
        STEADS_DESTROYED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.STEADS_DESTROYED) >= 10;
            }
        },
        STEADS_DESTROYED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.STEADS_DESTROYED) >= 100;
            }
        },
        STEADS_CLAIMED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.LONE_STEADER) >= 20;
            }
        },

        // totems stacked badges
        TOTEMS_STACKED_1 {
            public boolean qualifies (PlayerObject user) {
                return getTotemsStacked(user.stats) >= 10;
            }
        },
        TOTEMS_STACKED_2 {
            public boolean qualifies (PlayerObject user) {
                return getTotemsStacked(user.stats) >= 100;
            }
        },
        TOTEMS_STACKED_3 {
            public boolean qualifies (PlayerObject user) {
                return getTotemsStacked(user.stats) >= 1000;
            }
        },
        TOTEMS_STACKED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.TOTEMS_LARGE) >= 100;
            }
        },
        TOTEMS_STACKED_5 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.TOTEMS_CROWN) >= 100;
            }
        },

        // trees saved badges
        TREES_SAVED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.PERFECT_WAVES) >= 5;
            }
        },
        TREES_SAVED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.TREES_ELDER) >= 250;
            }
        },
        TREES_SAVED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMaxIntStat(StatType.TREES_ELDER) >= 20;
            }
        },
        TREES_SAVED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMaxIntStat(StatType.HARD_ROBOT_KILLS) >= 8;
            }
        },
        TREES_SAVED_5 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.HIGHEST_SAWS) >= 10;
            }
        },

        // wendigo survival badges
        WENDIGO_SURVIVALS_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.WENDIGO_SURVIVALS) >= 30;
            }
        },
        WENDIGO_SURVIVALS_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.WENDIGO_SURVIVALS) >= 500;
            }
        },
        WENDIGO_SURVIVALS_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.WENDIGO_SURVIVALS) >= 5000;
            }
        },
        WENDIGO_SURVIVALS_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.TALISMAN_SPOT_SURVIVALS) >= 100;
            }
        },
        WENDIGO_SURVIVALS_5 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.WHOLE_TEAM_SURVIVALS) >= 50;
            }
        },

        // frontier town unit usage badges
        CAVALRY_USER {
            public boolean qualifies (PlayerObject user) {
                return checkBigShotUsage(user.stats, "frontier_town/cavalry");
            }
        },
        TACTICIAN_USER {
            public boolean qualifies (PlayerObject user) {
                return checkBigShotUsage(user.stats, "frontier_town/tactician");
            }
        },
        CODGER_USER {
            public boolean qualifies (PlayerObject user) {
                return checkBigShotUsage(user.stats, "frontier_town/codger");
            }
        },
        FT_BIGSHOT_USER {
            public boolean qualifies (PlayerObject user) {
                return checkUnitUsage(user.stats, StatType.BIGSHOT_WINS,
                                      BangCodes.FRONTIER_TOWN, BIGSHOT_UNITS, 10);
            }
        },
        FT_ALLUNIT_USER {
            public boolean qualifies (PlayerObject user) {
                return checkUnitUsage(user.stats, StatType.UNITS_USED,
                                      BangCodes.FRONTIER_TOWN, ALL_UNITS, 1);
            }
        },

        // indian trading post unit usage badges
        STORM_CALLER_USER {
            public boolean qualifies (PlayerObject user) {
                return checkBigShotUsage(user.stats, "indian_post/stormcaller");
            }
        },
        TRICKSTER_RAVEN_USER {
            public boolean qualifies (PlayerObject user) {
                return checkBigShotUsage(user.stats, "indian_post/tricksterraven");
            }
        },
        REVOLUTIONARY_USER {
            public boolean qualifies (PlayerObject user) {
                return checkBigShotUsage(user.stats, "indian_post/revolutionary");
            }
        },
        ITP_BIGSHOT_USER {
            public boolean qualifies (PlayerObject user) {
                return checkUnitUsage(user.stats, StatType.BIGSHOT_WINS,
                                      BangCodes.INDIAN_POST, BIGSHOT_UNITS, 10);
            }
        },
        ITP_ALLUNIT_USER {
            public boolean qualifies (PlayerObject user) {
                return checkUnitUsage(user.stats, StatType.UNITS_USED,
                                      BangCodes.INDIAN_POST, ALL_UNITS, 1);
            }
        },

        // social badges
        GAMES_HOSTED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.GAMES_HOSTED) >= 50;
            }
        },
        GAMES_HOSTED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.GAMES_HOSTED) >= 250;
            }
        },
        CHAT_SENT_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CHAT_SENT) >= 1000;
            }
        },
        CHAT_SENT_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CHAT_SENT) >= 5000;
            }
        },
        CHAT_SENT_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CHAT_SENT) >= 15000;
            }
        },
        CHAT_RECEIVED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CHAT_RECEIVED) >= 5000;
            }
        },
        CHAT_RECEIVED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.CHAT_RECEIVED) >= 50000;
            }
        },

        // avatar-related badges
        LOOKS_BOUGHT_1 {
            public boolean qualifies (PlayerObject user) {
                return user.looks.size() > 5;
            }
        },
        LOOKS_BOUGHT_2 {
            public boolean qualifies (PlayerObject user) {
                return user.looks.size() > 15;
            }
        },
        DUDS_BOUGHT_1 {
            public boolean qualifies (PlayerObject user) {
                return user.getDudsCount() >= 5;
            }
        },
        DUDS_BOUGHT_2 {
            public boolean qualifies (PlayerObject user) {
                return user.getDudsCount() >= 20;
            }
        },
        DUDS_BOUGHT_3 {
            public boolean qualifies (PlayerObject user) {
                return user.getDudsCount() >= 50;
            }
        },

        // wacky badges
        IRON_HORSE {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.SESSION_GAMES_PLAYED) >= 25;
            }
        },
        SAINT_NICK {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.MYSTERY_TWO) >= 1;
            }
        },
        BETA_TESTER {
            public boolean qualifies (PlayerObject user) {
                return user.playerId <= BangCodes.BETA_PLAYER_CUTOFF;
            }
        },
        NIGHT_OWL {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.LATE_NIGHTS) >= 5;
            }
        },
        HIGH_NOON {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.MYSTERY_ONE) >= 1;
            }
        },
        NEW_SHERRIF_IN_TOWN {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(StatType.MYSTERY_THREE) >= 1;
            }
        },

        // frontier town bounty badges
        BOUNTY_DYNAMITE,
        BOUNTY_SANCHO,
        BOUNTY_MAUDE,
        BOUNTY_CALAVERA,
        BOUNTY_MUSTACHE,
        BOUNTY_SHARK,
        BOUNTY_ALL_FT_TOWN {
            public boolean qualifies (PlayerObject user) {
                return hasCompletedBounties(
                    user.stats, BangCodes.FRONTIER_TOWN, BountyConfig.Type.TOWN);
            }
        },
        BOUNTY_ALL_FT {
            public boolean qualifies (PlayerObject user) {
                return user.holdsBadge(BOUNTY_ALL_FT_TOWN) &&
                    hasCompletedBounties(
                        user.stats, BangCodes.FRONTIER_TOWN, BountyConfig.Type.MOST_WANTED);
            }
        },

        // indian post bounty badges
        BOUNTY_ZERO3,
        BOUNTY_LETRAPPE,
        BOUNTY_CLOUD,
        BOUNTY_ALL_ITP_TOWN {
            public boolean qualifies (PlayerObject user) {
                return hasCompletedBounties(
                        user.stats, BangCodes.INDIAN_POST, BountyConfig.Type.TOWN);
            }
        },
        BOUNTY_ALL_ITP {
            public boolean qualifies (PlayerObject user) {
                return false; // don't enable until all most wanted bounties are released
                //return user.holdsBadge(BOUNTY_ALL_ITP_TOWN) &&
                //    hasCompletedBounties(
                //            user.stats, BandCodes.INDIAN_POST, BountyConfig.Type.MOST_WANTED);
            }
        },

        // tutorial badges
        TUTORIAL_ALL_FT {
            public boolean qualifies (PlayerObject user) {
                return hasCompletedTutorials(user.stats, BangCodes.FRONTIER_TOWN);
            }
        },
        TUTORIAL_ALL_ITP {
            public boolean qualifies (PlayerObject user) {
                return hasCompletedTutorials(user.stats, BangCodes.INDIAN_POST);
            }
        },

        // top ranked badges
        WEEKLY_TOP10_OA {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_TOP10, ScenarioInfo.OVERALL_IDENT);
            }
        },
        WEEKLY_TOP10_GR {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_TOP10, GoldRushInfo.IDENT);
            }
        },
        WEEKLY_TOP10_LG {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_TOP10, LandGrabInfo.IDENT);
            }
        },
        WEEKLY_TOP10_CR {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_TOP10, CattleRustlingInfo.IDENT);
            }
        },
        WEEKLY_TOP10_CJ {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_TOP10, ClaimJumpingInfo.IDENT);
            }
        },
        WEEKLY_TOP10_WA {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_TOP10, WendigoAttackInfo.IDENT);
            }
        },
        WEEKLY_TOP10_TB {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_TOP10, TotemBuildingInfo.IDENT);
            }
        },
        WEEKLY_TOP10_FG {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_TOP10, ForestGuardiansInfo.IDENT);
            }
        },

        WEEKLY_WINNER_OA {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_WINNER, ScenarioInfo.OVERALL_IDENT);
            }
        },
        WEEKLY_WINNER_GR {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_WINNER, GoldRushInfo.IDENT);
            }
        },
        WEEKLY_WINNER_LG {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_WINNER, LandGrabInfo.IDENT);
            }
        },
        WEEKLY_WINNER_CR {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_WINNER, CattleRustlingInfo.IDENT);
            }
        },
        WEEKLY_WINNER_CJ {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_WINNER, ClaimJumpingInfo.IDENT);
            }
        },
        WEEKLY_WINNER_WA {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_WINNER, WendigoAttackInfo.IDENT);
            }
        },
        WEEKLY_WINNER_TB {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_WINNER, TotemBuildingInfo.IDENT);
            }
        },
        WEEKLY_WINNER_FG {
            public boolean qualifies (PlayerObject user) {
                return user.stats.containsValue(StatType.WEEKLY_WINNER, ForestGuardiansInfo.IDENT);
            }
        },

        UNUSED;

        /** Returns a new blank stat instance of the specified type. */
        public Badge newBadge ()
        {
            return new Badge(code());
        }

        /** Overridden by each badge type to indicate whether the supplied
         * user qualifies for this badge. */
        public boolean qualifies (PlayerObject user) {
            return false;
        }

        /** Returns the translation key used by this badge. */
        public String key () {
            return "m.badge_" + Integer.toHexString(_code);
        }

        /** Returns the unique code for this badge type, which is a function of
         * its name. */
        public int code () {
            return _code;
        }

        Type () {
            // compute our unique code
            StringBuilder codestr = new StringBuilder();
            _code = BangUtil.crc32(name());

            if (_codeToType == null) {
                _codeToType = new HashIntMap<Type>();
            }
            if (_codeToType.containsKey(_code)) {
                log.warning("Badge type collision! " + this + " and " + _codeToType.get(_code) +
                            " both map to '" + _code + "'.");
            } else {
                _codeToType.put(_code, this);
            }
        }

        protected int _code;
    };

    /** Defines the layout of the badge table. */
    public static Type[] LAYOUT =
    {
        // general series badges
        Type.GAMES_PLAYED_1, Type.GAMES_PLAYED_2, Type.GAMES_PLAYED_3,
        Type.GAMES_PLAYED_4, Type.GAMES_PLAYED_5,

        Type.UNITS_KILLED_1, Type.UNITS_KILLED_2, Type.UNITS_KILLED_3,
        Type.UNITS_KILLED_4, Type.UNITS_KILLED_5,

        Type.HIGHEST_POINTS_1, Type.HIGHEST_POINTS_2,
        Type.CONSEC_KILLS_1, Type.CONSEC_KILLS_2, Type.CONSEC_KILLS_3,

        Type.CONSEC_WINS_1, Type.CONSEC_WINS_2, Type.CONSEC_WINS_3,
        Type.CONSEC_WINS_4, Type.CONSEC_WINS_5,

        Type.SHOTS_FIRED_1, Type.SHOTS_FIRED_2,
        Type.DISTANCE_MOVED_1, Type.DISTANCE_MOVED_2, Type.DISTANCE_MOVED_3,

        Type.CARDS_PLAYED_1, Type.CARDS_PLAYED_2, Type.CARDS_PLAYED_3,
        Type.CARDS_PLAYED_4, Type.CARDS_PLAYED_5,

        Type.BONUSES_COLLECTED_1, Type.BONUSES_COLLECTED_2, Type.BONUSES_COLLECTED_3,
        Type.BONUSES_COLLECTED_4, Type.BONUSES_COLLECTED_5,

        Type.CASH_EARNED_1, Type.CASH_EARNED_2, Type.CASH_EARNED_3,
        Type.GAMES_HOSTED_1, Type.GAMES_HOSTED_2,

        Type.CHAT_SENT_1, Type.CHAT_SENT_2, Type.CHAT_SENT_3,
        Type.CHAT_RECEIVED_1, Type.CHAT_RECEIVED_2,

        Type.LOOKS_BOUGHT_1, Type.LOOKS_BOUGHT_2,
        Type.DUDS_BOUGHT_1, Type.DUDS_BOUGHT_2, Type.DUDS_BOUGHT_3,

//         DAILY_HIGH_SCORER, WEEKLY_HIGH_SCORER, MONTHLY_HIGH_SCORER,

        // frontier town badges
        Type.NUGGETS_CLAIMED_1, Type.NUGGETS_CLAIMED_2, Type.NUGGETS_CLAIMED_3,
        Type.NUGGETS_CLAIMED_4, Type.NUGGETS_CLAIMED_5,

        Type.CATTLE_RUSTLED_1, Type.CATTLE_RUSTLED_2, Type.CATTLE_RUSTLED_3,
        Type.CATTLE_RUSTLED_4, Type.CATTLE_RUSTLED_5,

        Type.STEADS_CLAIMED_1, Type.STEADS_CLAIMED_2,
        Type.STEADS_DESTROYED_1, Type.STEADS_DESTROYED_2, Type.STEADS_CLAIMED_3,

        Type.CAVALRY_USER, Type.TACTICIAN_USER, Type.CODGER_USER,
        Type.FT_BIGSHOT_USER, Type.FT_ALLUNIT_USER,

        Type.WEEKLY_TOP10_GR, Type.WEEKLY_TOP10_CR, Type.WEEKLY_TOP10_CJ,
        Type.WEEKLY_TOP10_LG, Type.WEEKLY_TOP10_OA,

        Type.WEEKLY_WINNER_GR, Type.WEEKLY_WINNER_CR, Type.WEEKLY_WINNER_CJ,
        Type.WEEKLY_WINNER_LG, Type.WEEKLY_WINNER_OA,

        // indian trading post badges
        Type.TOTEMS_STACKED_1, Type.TOTEMS_STACKED_2, Type.TOTEMS_STACKED_3,
        Type.TOTEMS_STACKED_4, Type.TOTEMS_STACKED_5,

        Type.TREES_SAVED_1, Type.TREES_SAVED_2, Type.TREES_SAVED_3,
        Type.TREES_SAVED_4, Type.TREES_SAVED_5,

        Type.WENDIGO_SURVIVALS_1, Type.WENDIGO_SURVIVALS_2, Type.WENDIGO_SURVIVALS_3,
        Type.WENDIGO_SURVIVALS_4, Type.WENDIGO_SURVIVALS_5,

        Type.STORM_CALLER_USER, Type.TRICKSTER_RAVEN_USER,
        Type.REVOLUTIONARY_USER, Type.ITP_BIGSHOT_USER, Type.ITP_ALLUNIT_USER,

        Type.WEEKLY_TOP10_TB, Type.WEEKLY_TOP10_WA, Type.WEEKLY_TOP10_FG, null, null,

        Type.WEEKLY_WINNER_TB, Type.WEEKLY_WINNER_WA, Type.WEEKLY_WINNER_FG, null, null,

        // you suck badges
        Type.UNITS_LOST_1, Type.UNITS_LOST_2, null, Type.CONSEC_LOSSES_1, Type.CONSEC_LOSSES_2,

        // frontier town bounty badges
        Type.BOUNTY_DYNAMITE, Type.BOUNTY_SANCHO, null, null, Type.BOUNTY_ALL_FT_TOWN,
        Type.BOUNTY_MAUDE, Type.BOUNTY_CALAVERA, Type.BOUNTY_MUSTACHE, Type.BOUNTY_SHARK,
        Type.BOUNTY_ALL_FT,

        // indian post bounty badges
        Type.BOUNTY_LETRAPPE, Type.BOUNTY_CLOUD, null, null, Type.BOUNTY_ALL_ITP_TOWN,
        Type.BOUNTY_ZERO3, null, null, null, Type.BOUNTY_ALL_ITP,

        // tutorial badges
        Type.TUTORIAL_ALL_FT, Type.TUTORIAL_ALL_ITP, null, null, null,

        // general non-series (wacky) badges
        Type.IRON_HORSE, Type.SAINT_NICK,
        Type.NIGHT_OWL, Type.HIGH_NOON, Type.NEW_SHERRIF_IN_TOWN,

        // more wacky badges
        Type.BETA_TESTER, null, null, null, null,
    };

    /**
     * Used to check certain badge bits.
     */
    public static void main (String[] args)
    {
        if (args.length > 0 && "check".indexOf(args[0]) != -1) {
            HashSet<Type> laidout = new HashSet<Type>();
            for (Type type : LAYOUT) {
                laidout.add(type);
            }
            for (Type type : Type.values()) {
                if (!laidout.contains(type) && type != Type.UNUSED) {
                    System.err.println("Not in layout: " + type);
                }
            }

        } else if (args.length > 0 && "dump".indexOf(args[0]) != -1) {
            for (Type type : Type.values()) {
                System.err.println(type + " = " + type.code());
            }

        } else if (args.length > 0 && "xlate".indexOf(args[0]) != -1) {
            for (Type type : Type.values()) {
                System.err.println(type.key() + " = " + type);
            }

        } else if (args.length > 0 && "stat".indexOf(args[0]) != -1) {
            for (StatType type : StatType.values()) {
                System.err.println(type + " = " + type.code());
            }

        } else {
            System.err.println("Usage: Badge [check|dump|xlate]");
        }
    }

    /**
     * Determines whether this player qualifies for any new badges and returns
     * the first badge for which they qualify or null if they qualify for no
     * new badges.
     */
    public static Badge checkQualifies (PlayerObject user)
    {
        // first enumerate the badges they already hold
        _badgeCodes.clear();
        for (Item item : user.inventory) {
            if (item instanceof Badge) {
                _badgeCodes.add(((Badge)item).getCode());
            }
        }

        // now check each type in turn for qualification
        for (Type type : Type.values()) {
            if (_badgeCodes.contains(type.code())) {
                continue;
            }
            if (!type.qualifies(user)) {
                continue;
            }
            Badge badge = type.newBadge();
            badge.setOwnerId(user.playerId);
            return badge;
        }

        return null;
    }

    /** Creates a blank instance for serialization. */
    public Badge ()
    {
    }

    /**
     * Maps a {@link Type}'s code code back to a {@link Type} instance.
     */
    public static Type getType (int code)
    {
        return _codeToType.get(code);
    }

    /**
     * Returns the type of this badge.
     */
    public Type getType ()
    {
        return getType(_code);
    }

    /**
     * Returns the reward associated with this badge or null if the badge does
     * not confer any reward (other than the joy of collecting it).
     */
    public String getReward ()
    {
        if (_rewards.size() == 0) {
            registerRewards();
        }
        return _rewards.get(getType());
    }

    /**
     * Returns the integer code to which this badge's type maps.
     */
    public int getCode ()
    {
        return _code;
    }

    /**
     * Configures this badge to display a silhouette image and no name or
     * tooltip.  Used when displaying unearned badges in the badge list.
     */
    public void setSilhouette (boolean silhouette)
    {
        _silhouette = silhouette;
    }

    /**
     * Creates a new, unowned badge with the specified type code.
     */
    protected Badge (int code)
    {
        _code = code;
    }

    @Override // documentation inherited
    public String getName ()
    {
        if (_silhouette) {
            return null;
        }
        return MessageBundle.qualify(BangCodes.BADGE_MSGS, getType().key());
    }

    @Override // documentation inherited
    public String getTooltip (PlayerObject user)
    {
        if (_silhouette) {
            return null;
        }
        String reward = getReward(), msg;
        if (reward == null) {
            msg = MessageBundle.compose("m.badge_icon_nil", getType().key());
        } else {
            msg = MessageBundle.compose("m.badge_icon", getType().key(), reward);
        }
        return MessageBundle.qualify(BangCodes.BADGE_MSGS, msg);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        String id = Integer.toHexString(getType().code());
        return "badges/" + id.substring(0,1) + "/" + id + ".png";
    }

    @Override // documentation inherited
    public ImageIcon createIcon (BasicContext ctx, String iconPath)
    {
        BImage bimage;
        if (_silhouette) {
            bimage = ctx.getImageCache().getSilhouetteBImage(iconPath, true);
            if (bimage == null) {
                bimage = ctx.getImageCache().getSilhouetteBImage("badges/noimage.png", true);
            }
        } else {
            bimage = ctx.getImageCache().getBImage(iconPath, 1f, true);
            if (bimage == null) {
                bimage = ctx.loadImage("badges/noimage.png");
            }
        }
        return new ImageIcon(bimage);
    }

    @Override // documentation inherited
    public boolean isEquivalent (Item other)
    {
        return super.isEquivalent(other) &&
            ((Badge)other)._code == _code;
    }

    @Override // documentation inherited
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", type=").append(getType());
    }

    /**
     * Registers a message used to report the reward associated with obtaining
     * the specified type of badge.
     *
     * @param type the type of badge with which the reward is associated.
     * @param message the translatable string used to report the reward to the
     * player when they earn the badge.
     */
    protected static void registerReward (Type type, String message)
    {
        String old = _rewards.put(type, message);
        if (old != null) {
            log.warning("Badge registered for duplicate rewards [type=" + type + ", old=" + old +
                        ", new=" + message + "].");
        }
    }

    /** Used by Totem Building badges. */
    protected static int getTotemsStacked (StatSet stats)
    {
        return stats.getIntStat(StatType.TOTEMS_SMALL) +
            stats.getIntStat(StatType.TOTEMS_MEDIUM) + stats.getIntStat(StatType.TOTEMS_LARGE);
    }

    /** Used by unit usage badges. */
    protected static boolean checkUnitUsage (
        StatSet stats, StatType stat, String townId, EnumSet<UnitConfig.Rank> which, int usages)
    {
        for (UnitConfig cfg : UnitConfig.getTownUnits(townId, which)) {
            if (stats.getMapValue(stat, cfg.type) < usages) {
                return false;
            }
        }
        return true;
    }

    /** Used by Big Shot usage badges. */
    protected static boolean checkBigShotUsage (StatSet stats, String type)
    {
        return stats.getMapValue(StatType.UNITS_USED, type) >= 100 ||
            stats.getMapValue(StatType.BIGSHOT_WINS, type) >= 30;
    }

    /** Used by the all bounty badges. */
    protected static boolean hasCompletedBounties (
        StatSet stats, String townId, BountyConfig.Type type)
    {
        for (String bounty : BountyConfig.getBountyIds(townId, type)) {
            if (!stats.containsValue(StatType.BOUNTIES_COMPLETED, bounty)) {
                return false;
            }
        }
        return true;
    }

    /** Used by the all tutorials badges. */
    protected static boolean hasCompletedTutorials (StatSet stats, String townId)
    {
        int townIdx = BangUtil.getTownIndex(townId);
        for (String tutorial : TutorialCodes.NEW_TUTORIALS[townIdx]) {
            if (!stats.containsValue(StatType.TUTORIALS_COMPLETED, tutorial)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Registers all badge awards. We have to do this on demand rather than in
     * a static initializer because of a twisty maze of already interdependent
     * static initializers.
     */
    protected static void registerRewards ()
    {
        String key = "m.hair_color_enabled", msg;
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_blue");
        registerReward(Type.UNITS_KILLED_2, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_green");
        registerReward(Type.SHOTS_FIRED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_navyBlue");
        registerReward(Type.GAMES_PLAYED_3, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_orange");
        registerReward(Type.DISTANCE_MOVED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_purple");
        registerReward(Type.CONSEC_WINS_2, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_violet");
        registerReward(Type.GAMES_PLAYED_2, MessageBundle.compose(key, msg));

        key = "m.eye_color_enabled";
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_violet");
        registerReward(Type.GAMES_PLAYED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_orange");
        registerReward(Type.CASH_EARNED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_purple");
        registerReward(Type.BONUSES_COLLECTED_2, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_red");
        registerReward(Type.UNITS_KILLED_3, MessageBundle.compose(key, msg));

        key = "m.duds_color_enabled";
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_black");
        registerReward(Type.CONSEC_WINS_3, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_leather");
        registerReward(Type.CARDS_PLAYED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_orange");
        registerReward(Type.CARDS_PLAYED_2, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_pink");
        registerReward(Type.LOOKS_BOUGHT_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_violet");
        registerReward(Type.DUDS_BOUGHT_2, MessageBundle.compose(key, msg));

        key = "m.unit_enabled";
        msg = MessageBundle.qualify(BangCodes.UNITS_MSGS, "m.sharpshooter");
        registerReward(Type.CONSEC_WINS_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(BangCodes.UNITS_MSGS, "m.steamgunman");
        registerReward(Type.HIGHEST_POINTS_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(BangCodes.UNITS_MSGS, "m.dogsoldier");
        registerReward(Type.CONSEC_KILLS_2, MessageBundle.compose(key, msg));

        key = "m.cards_enabled";
        for (Card card : Card.getCards()) {
            Type qual = card.getQualifier();
            if (qual == null) {
                continue;
            }
            msg = MessageBundle.qualify(BangCodes.CARDS_MSGS, "m." + card.getType());
            registerReward(qual, MessageBundle.compose(key, msg));
        }
    }

    /** The unique code for the type of this badge. */
    protected int _code;

    /** Used when displaying unearned badges. */
    protected transient boolean _silhouette;

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap<Type> _codeToType;

    /** Trigger the loading of the enum when we load this class. */
    protected static Type _trigger = Type.UNUSED;

    /** Used by {@link #checkQualifies}. */
    protected static ArrayIntSet _badgeCodes = new ArrayIntSet();

    /** Used to report rewards associated with badges. */
    protected static HashMap<Type,String> _rewards = new HashMap<Type,String>();

    /** Used by unit usage badges. */
    protected static final EnumSet<UnitConfig.Rank> BIGSHOT_UNITS =
        EnumSet.of(UnitConfig.Rank.BIGSHOT);

    /** Used by unit usage badges. */
    protected static final EnumSet<UnitConfig.Rank> ALL_UNITS =
        EnumSet.of(UnitConfig.Rank.BIGSHOT, UnitConfig.Rank.NORMAL);
}
