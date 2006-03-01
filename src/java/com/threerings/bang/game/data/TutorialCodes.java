//
// $Id$

package com.threerings.bang.game.data;

/**
 * Codes and constants related to the in-game tutorials
 */
public interface TutorialCodes
{
    /** An event message sent to the server to let the tutorial scenario know
     * that we've processed a particular action. The index of the action will
     * be passed along with the event. */
    public static final String ACTION_PROCESSED = "ActionProcessed";

    /** A user interface action monitored by the tutorial system. */
    public static final String TEXT_CLICKED = "text_clicked";

    /** A user interface action monitored by the tutorial system. */
    public static final String UNIT_SELECTED = "unit_selected";

    /** A user interface action monitored by the tutorial system. */
    public static final String UNIT_DESELECTED = "unit_deselected";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_ADDED = "unit_added";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_MOVED = "unit_moved";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_ORDERED = "unit_ordered";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_ATTACKED = "unit_attacked";

    /** A game state action monitored by the tutorial system. */
    public static final String UNIT_MOVE_ATTACKED = "unit_move_attacked";

    /** A game state action monitored by the tutorial system. */
    public static final String BONUS_ACTIVATED = "bonus_activated";

    /** A game state action monitored by the tutorial system. */
    public static final String CARD_SELECTED = "card_selected";

    /** A game state action monitored by the tutorial system. */
    public static final String CARD_PLAYED = "card_played";
}
