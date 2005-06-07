//
// $Id$

package com.threerings.bang.data.piece;

import java.io.IOException;
import java.util.logging.Level;

import com.threerings.io.ObjectInputStream;

import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.client.sprite.UnitSprite;
import com.threerings.bang.data.Terrain;
import com.threerings.bang.data.UnitConfig;

import static com.threerings.bang.Log.log;

/**
 * A base piece type for player units.
 */
public class Unit extends Piece
{
    /**
     * Instantiates a unit of the specified type.
     */
    public static Unit getUnit (String type)
    {
        UnitConfig config = UnitConfig.getConfig(type);
        Unit unit = null;
        try {
            if (config.unitClass != null) {
                unit = (Unit)Class.forName(config.unitClass).newInstance();
            } else {
                unit = new Unit();
            }
            unit.init(config);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create unit [type=" + type +
                    ", class=" + config.unitClass + "].", e);
        }
        return unit;
    }

    /** Returns the type of the unit. */
    public String getType ()
    {
        return _type;
    }

    /** Configures the instance after unserialization. */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        _config = UnitConfig.getConfig(_type);
    }

    @Override // documentation inherited
    public int getSightDistance ()
    {
        return _config.sightDistance;
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return _config.moveDistance;
    }

    @Override // documentation inherited
    public int getFireDistance ()
    {
        return _config.fireDistance;
    }

    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return _config.make == UnitConfig.HUMAN;
    }

    @Override // documentation inherited
    public boolean validTarget (Piece target)
    {
        boolean valid = super.validTarget(target);
        // TODO: make sure our damage to the target is >0
        return valid;
    }

    @Override // documentation inherited
    public boolean isFlyer ()
    {
        return _config.mode == UnitConfig.AIR;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite(_type);
    }

    @Override // documentation inherited
    public int traversalCost (Terrain terrain)
    {
        // TODO:
        return 10;
    }

    /**
     * Provides the unit with its configuration.
     */
    protected void init (UnitConfig config)
    {
        _config = config;
        _type = config.type;
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        // TODO:
        return 10;
    }

    protected String _type;
    protected transient UnitConfig _config;
}
