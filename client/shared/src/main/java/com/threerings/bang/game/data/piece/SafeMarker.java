//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.SafeMarkerSprite;
import com.threerings.bang.game.data.BangObject;

/**
 * Marker piece of safety tiles in Wendigo Attack.
 */
public class SafeMarker extends Marker
{
    public SafeMarker (int type)
    {
        super(type);
        _on = (type == SAFE);
    }

    public SafeMarker ()
    {
    }

    public SafeMarker (Marker marker)
    {
        this(marker._type);
        x = marker.x;
        y = marker.y;
        orientation = marker.orientation;
    }

    @Override // documentation inherited
    public void init ()
    {
        super.init();
        _on = (_type == SAFE);
    }

    @Override // documentation inherited
    public int getGoalRadius (BangObject bangobj, Piece mover)
    {
        return (_on) ? 0 : -1;
    }

    @Override // documentation inherited
    public boolean addSprite ()
    {
        return true;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new SafeMarkerSprite(_type);
    }

    public boolean isOn ()
    {
        return _on;
    }

    public void setSquare (boolean square)
    {
        if (_type == Marker.SAFE) {
            _on = square;
        } else {
            _on = !square;
        }
    }

    protected boolean _on;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        super.readObject(ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.piece.SafeMarker.class, "_on", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        super.writeObject(out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.piece.SafeMarker.class, "_on", this, out);
    }
}
