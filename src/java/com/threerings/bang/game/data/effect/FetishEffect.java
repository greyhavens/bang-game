//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.*;

/**
 * Handles the effects of various fetishes: bonuses that grant special powers
 * when held.
 */
public class FetishEffect extends HoldEffect
{
    /** Doubles tree growth when the holding unit is present. */
    public static final String FROG_FETISH = "indian_post/fetish_frog";

    /** Halves damage against the holding unit. */
    public static final String TURTLE_FETISH = "indian_post/fetish_turtle";

    /** Doubles attack power of the holding unit. */
    public static final String BEAR_FETISH = "indian_post/fetish_bear";

    /** Hides the holding unit from logging robots. */
    public static final String FOX_FETISH = "indian_post/fetish_fox";

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // determine the type of the bonus if picking up
        if (!dropping) {
            Bonus bonus = (Bonus)bangobj.pieces.get(bonusId);
            if (bonus != null) {
                type = bonus.getConfig().type;
            }
        }

        super.prepare(bangobj, dammap);
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        // add or remove the influence as appropriate
        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit == null) {
            return false;
        }
        unit.setHoldingInfluence(dropping ? null : createHoldingInfluence());
        return true;
    }

    /**
     * Creates and returns the influence associated with the bonus.
     */
    protected Influence createHoldingInfluence ()
    {
        if (type.equals(FROG_FETISH) || type.equals(FOX_FETISH)) {
            return null; // these are handled as special cases

        } else if (type.equals("indian_post/fetish_turtle")) {
            return new Influence() {
                public String getName() {
                    return "turtle_fetish";
                }
                public int adjustDefend (Piece shooter, int damage) {
                    return damage/2;
                }
                public boolean didAdjustDefend () {
                    return true;
                }
            };
        } else if (type.equals("indian_post/fetish_bear")) {
            return new Influence() {
                public String getName() {
                    return "bear_fetish";
                }
                public int adjustAttack (Piece target, int damage) {
                    return damage*2;
                }
                public boolean showClientAdjust () {
                    return true;
                }
                public boolean didAdjustAttack () {
                    return true;
                }
            };
        } else {
            log.warning("Fetish effect encountered unknown type", "type", type);
            return null;
        }
    }
}
