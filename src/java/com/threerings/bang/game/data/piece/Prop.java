//
// $Id$

package com.threerings.bang.game.data.piece;

import java.io.IOException;
import java.util.logging.Level;

import com.threerings.io.ObjectInputStream;

import com.threerings.bang.data.PropConfig;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.PropSprite;

import static com.threerings.bang.Log.log;

/**
 * A piece representing a prop.
 */
public class Prop extends BigPiece
{
    /**
     * Instantiates a prop of the specified type.
     */
    public static Prop getProp (String type)
    {
        PropConfig config = PropConfig.getConfig(type);
        Prop prop = null;
        try {
            if (config.propClass != null) {
                prop = (Prop)
                    Class.forName(config.propClass).newInstance();
            } else {
                prop = new Prop();
            }
            prop.init(config);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create prop [type=" + type +
                    ", class=" + config.propClass + "].", e);
        }
        return prop;
    }

    /** Returns the type of the prop. */
    public String getType ()
    {
        return _type;
    }

    /** Configures the instance after unserialization. */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        init(PropConfig.getConfig(_type));
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new PropSprite(_type);
    }

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return !lapper.isFlyer();
    }

    /**
     * Provides the prop with its configuration.
     */
    protected void init (PropConfig config)
    {
        _config = config;
        _type = config.type;
        _width = _config.width;
        _height = _config.height;
        recomputeBounds();
    }

    protected String _type;
    protected transient PropConfig _config;
}
