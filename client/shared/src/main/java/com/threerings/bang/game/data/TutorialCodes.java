//
// $Id$

package com.threerings.bang.game.data;

import java.util.HashMap;

import com.threerings.bang.game.data.scenario.CattleRustlingInfo;
import com.threerings.bang.game.data.scenario.ClaimJumpingInfo;
import com.threerings.bang.game.data.scenario.ForestGuardiansInfo;
import com.threerings.bang.game.data.scenario.GoldRushInfo;
import com.threerings.bang.game.data.scenario.HeroBuildingInfo;
import com.threerings.bang.game.data.scenario.LandGrabInfo;
import com.threerings.bang.game.data.scenario.TotemBuildingInfo;
import com.threerings.bang.game.data.scenario.WendigoAttackInfo;

/**
 * Codes and constants related to the in-game tutorials
 */
public class TutorialCodes
{
    /** A prefix for tutorials that are not tutorials but rather two player practice games versus
     * an AI. */
    public static final String PRACTICE_PREFIX = "practice_";

    /** Enumerates the identifiers for our tutorials and the order in which they should be
     * displayed and completed. */
    public static final String[][] TUTORIALS = {
        { // frontier town tutorials
            "controls",
            "bonuses_cards",
            "claim_jumping",
            PRACTICE_PREFIX + ClaimJumpingInfo.IDENT,
            "cattle_rustling",
            PRACTICE_PREFIX + CattleRustlingInfo.IDENT,
            "gold_rush",
            PRACTICE_PREFIX + GoldRushInfo.IDENT,
            "land_grab",
            PRACTICE_PREFIX + LandGrabInfo.IDENT,
        }, { // indian post tutorials
            "wendigo_attack",
            PRACTICE_PREFIX + WendigoAttackInfo.IDENT,
            "totem_building",
            PRACTICE_PREFIX + TotemBuildingInfo.IDENT,
            "forest_guardians",
            PRACTICE_PREFIX + ForestGuardiansInfo.IDENT,
        } , { // boom town tutorials
            "boom_units", ""// TEMP: test tutorial
        }
    };

    /** Enumerates the identifiers for our new tutorials and the order in which they should be
     * displayed and completed. */
    public static final String[][] NEW_TUTORIALS = {
        { // frontier town tutorials
            "new_controls",
            "new_bonuses_cards",
            "new_claim_jumping",
            PRACTICE_PREFIX + ClaimJumpingInfo.IDENT,
            "new_cattle_rustling",
            PRACTICE_PREFIX + CattleRustlingInfo.IDENT,
            "unit_tactics",
            "new_gold_rush",
            PRACTICE_PREFIX + GoldRushInfo.IDENT,
            "new_land_grab",
            PRACTICE_PREFIX + LandGrabInfo.IDENT,
        }, { // indian post tutorials
            "new_units",
            "new_totem_building",
            PRACTICE_PREFIX + TotemBuildingInfo.IDENT,
            "new_bigshots",
            "new_wendigo_attack",
            PRACTICE_PREFIX + WendigoAttackInfo.IDENT,
            "new_forest_guardians",
            PRACTICE_PREFIX + ForestGuardiansInfo.IDENT,
            "hero_building",
            PRACTICE_PREFIX + HeroBuildingInfo.IDENT,
        }, { // boom town tutorials
            "boom_units",
        }
    };

    /** Our unit portrait for each town. */
    public static final String[] TUTORIAL_UNIT = {
        "units/frontier_town/codger/portrait.png",
        "units/indian_post/tricksterraven/portrait.png",
        "units/indian_post/tricksterraven/portrait.png",
    };

    /** Our practice game configurations. */
    public static class PracticeConfig {
        public String board;
        public String bigshot;
        public String[] units;
        public String[] cards;

        public PracticeConfig (String board, String bigshot, String[] units, String[] cards)
        {
            this.board = board;
            this.bigshot = bigshot;
            this.units = units;
            this.cards = cards;
        }
    }

    public static final HashMap<String, PracticeConfig> PRACTICE_CONFIGS =
        new HashMap<String, PracticeConfig>();
    static {
        PRACTICE_CONFIGS.put(ClaimJumpingInfo.IDENT, new PracticeConfig(
                    "Hard Luck Rock", "frontier_town/codger",
                    new String[] {"frontier_town/shotgunner", "frontier_town/artillery"},
                    new String[0]));
        PRACTICE_CONFIGS.put(CattleRustlingInfo.IDENT, new PracticeConfig(
                    "Riverbed Ranch", "frontier_town/cavalry",
                    new String[] {"frontier_town/dirigible", "frontier_town/gunslinger"}, null));
        PRACTICE_CONFIGS.put(GoldRushInfo.IDENT, new PracticeConfig(
                    "Sandy Canyon", "frontier_town/tactician", null, null));
        PRACTICE_CONFIGS.put(LandGrabInfo.IDENT, new PracticeConfig(
                    "Desert Crossing", "frontier_town/tactician", null, null));
        PRACTICE_CONFIGS.put(TotemBuildingInfo.IDENT, new PracticeConfig(
                    "Echo Bluff", "indian_post/revolutionary", null, null));
        PRACTICE_CONFIGS.put(WendigoAttackInfo.IDENT, new PracticeConfig(
                    "Dead Wind Refuge", "indian_post/stormcaller", null, null));
        PRACTICE_CONFIGS.put(ForestGuardiansInfo.IDENT, new PracticeConfig(
                    "Honeycomb Copse", "indian_post/tricksterraven", null, null));
        PRACTICE_CONFIGS.put(HeroBuildingInfo.IDENT, new PracticeConfig(
                    "Frost Maw", "indian_post/stormcaller", null, null));
    }

    /** An event message sent to the server to let the tutorial scenario know that we've processed
     * a particular action. The index of the action will be passed along with the event. */
    public static final String ACTION_PROCESSED = "actionProcessed";

    /** A user interface action monitored by the tutorial system. */
    public static final String TEXT_CLICKED = "text_clicked";

    /** A user interface action monitored by the tutorial system. */
    public static final String HELP_TOGGLED = "help_toggled";

    /** A user interface action monitored by the tutorial system. */
    public static final String UNIT_SELECTED = "unit_selected";

    /** A user interface action monitored by the tutorial system. */
    public static final String UNIT_DESELECTED = "unit_deselected";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_ADDED = "unit_added";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_MOVED = "unit_moved";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_ATTACKED = "unit_attacked";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_MOVE_ATTACKED = "unit_move_attacked";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_ORDERED_MOVE = "unit_ordered_move";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_ORDERED_ATTACK = "unit_ordered_attack";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_ORDERED_MOVE_ATTACK = "unit_ordered_move_attack";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_KILLED = "unit_killed";

    /** A game state action monitored by the tutorial system. */
    public static final String PIECE_ADDED = "piece_added";

    /** A game state action monitored by the tutorial system. */
    public static final String BONUS_ACTIVATED = "bonus_activated";

    /** A game state action monitored by the tutorial system. */
    public static final String CARD_SELECTED = "card_selected";

    /** A game state action monitored by the tutorial system. */
    public static final String CARD_PLAYED = "card_played";

    /** Used to report piece effects. */
    public static final String EFFECT_PREFIX = "effect:";
}
