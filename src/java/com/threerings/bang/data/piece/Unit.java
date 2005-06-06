//
// $Id$

package com.threerings.bang.data.piece;

import java.io.IOException;

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
     * A blank constructor used for unserialization.
     */
    public Unit ()
    {
    }

    /**
     * Creates a unit of the specified type.
     */
    public Unit (String type)
    {
        _type = type;
        _config = UnitConfig.getConfig(type);
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

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        // TODO:
        return 10;
    }

    protected String _type;
    protected transient UnitConfig _config;
}
