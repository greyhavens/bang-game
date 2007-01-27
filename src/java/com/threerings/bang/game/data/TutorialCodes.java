//
// $Id$

package com.threerings.bang.game.data;

import com.threerings.bang.game.data.scenario.CattleRustlingInfo;
import com.threerings.bang.game.data.scenario.ClaimJumpingInfo;
import com.threerings.bang.game.data.scenario.ForestGuardiansInfo;
import com.threerings.bang.game.data.scenario.GoldRushInfo;
import com.threerings.bang.game.data.scenario.LandGrabInfo;
import com.threerings.bang.game.data.scenario.TotemBuildingInfo;
import com.threerings.bang.game.data.scenario.WendigoAttackInfo;

/**
 * Codes and constants related to the in-game tutorials
 */
public interface TutorialCodes
{
    /** A prefix for tutorials that are not tutorials but rather two player
     * practice games versus an AI. */
    public static final String PRACTICE_PREFIX = "practice_";

    /** Enumerates the identifiers for our tutorials and the order in which
     * they should be displayed and completed. */
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
        } , {
			"robo_rampage",
			PRACTICE_PREFIX + "rr",
		}
    };

    /** An event message sent to the server to let the tutorial scenario know
     * that we've processed a particular action. The index of the action will
     * be passed along with the event. */
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
    public static final String UNIT_ORDERED_MOVE_ATTACK =
        "unit_ordered_move_attack";

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
