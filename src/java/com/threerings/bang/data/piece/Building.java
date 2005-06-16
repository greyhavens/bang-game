//
// $Id$

package com.threerings.bang.data.piece;

import java.io.IOException;
import java.util.logging.Level;

import com.threerings.io.ObjectInputStream;

import com.threerings.bang.client.sprite.BuildingSprite;
import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.data.BuildingConfig;

import static com.threerings.bang.Log.log;

/**
 * A piece representing a building.
 */
public class Building extends BigPiece
{
    /**
     * Instantiates a building of the specified type.
     */
    public static Building getBuilding (String type)
    {
        BuildingConfig config = BuildingConfig.getConfig(type);
        Building bldg = null;
        try {
            if (config.buildingClass != null) {
                bldg = (Building)
                    Class.forName(config.buildingClass).newInstance();
            } else {
                bldg = new Building();
            }
            bldg.init(config);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create building [type=" + type +
                    ", class=" + config.buildingClass + "].", e);
        }
        return bldg;
    }

    /** Returns the type of the building. */
    public String getType ()
    {
        return _type;
    }

    /** Configures the instance after unserialization. */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        init(BuildingConfig.getConfig(_type));
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new BuildingSprite(_type);
    }

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return !lapper.isFlyer();
    }

    /**
     * Provides the building with its configuration.
     */
    protected void init (BuildingConfig config)
    {
        _config = config;
        _type = config.type;
        _width = _config.width;
        _height = _config.height;
        recomputeBounds();
    }

    protected String _type;
    protected transient BuildingConfig _config;
}
