//
// $Id$

package com.threerings.bang.data.piece;

import java.util.HashMap;
import java.util.logging.Level;

import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.client.sprite.UnitSprite;
import com.threerings.bang.data.BangCodes;

import static com.threerings.bang.Log.log;

/**
 * A base piece type for player units.
 */
public abstract class Unit extends Piece
{
    /** Returns the type of the unit. */
    public abstract String getType ();

    /**
     * Returns an array of all unit types available in the specified town.
     */
    public static String[] getUnitTypes (String townId)
    {
        return _unitTypes.get(townId);
    }

    /**
     * Creates a unit instance of the specified type.
     */
    public static Unit createUnit (String type)
    {
        try {
            return (Unit)_unitMap.get(type).newInstance();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to instantiate unit '" +
                    type + "'.", e);
            return null;
        }
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite(getType());
    }

    protected static void registerUnit (String townId, Unit proto)
    {
        // add the unit to a town set if appropriate
        if (townId != null) {
            String[] units = _unitTypes.get(townId);
            if (units == null) {
                units = new String[0];
            }
            String[] nunits = new String[units.length+1];
            System.arraycopy(units, 0, nunits, 0, units.length);
            nunits[units.length] = proto.getType();
            _unitTypes.put(townId, nunits);
        }

        // map the type to the class
        _unitMap.put(proto.getType(), proto.getClass());
    }

    /** A mapping from town to available units. */
    protected static HashMap<String,String[]> _unitTypes =
        new HashMap<String,String[]>();

    /** A mapping from unit type to class. */
    protected static HashMap<String,Class> _unitMap =
        new HashMap<String,Class>();

    static {
        // register the Frontier Town units
        registerUnit(BangCodes.FRONTIER_TOWN, new Gunslinger());
        registerUnit(BangCodes.FRONTIER_TOWN, new Dirigible());
        registerUnit(BangCodes.FRONTIER_TOWN, new Artillery());
        registerUnit(BangCodes.FRONTIER_TOWN, new SteamGunman());
        registerUnit(BangCodes.BOOM_TOWN, new WindupSlinger());
    }
}
