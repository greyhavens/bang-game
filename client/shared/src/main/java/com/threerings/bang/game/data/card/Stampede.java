//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.StampedeEffect;
import com.threerings.bang.game.util.PointSet;

/**
 * Causes a herd of buffalo to stampede across the board, damaging any
 * units in their path.
 */
public class Stampede extends AreaCard
{
    @Override // documentation inherited
    public String getType ()
    {
        return "stampede";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 1;
    }

    @Override // documentation inherited
    public boolean isValidLocation (BangObject bangobj, int tx, int ty)
    {
        PointSet points = new PointSet();
        bangobj.board.computeAttacks(0, getRadius(), tx, ty, points);
        for (int idx = 0, size = points.size(); idx < size; idx++) {
            if (bangobj.board.isTraversable(
                        points.getX(idx), points.getY(idx))) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 30;
    }

    @Override // documentation inherited
    public Badge.Type getQualifier ()
    {
        return Badge.Type.TOTEMS_STACKED_2;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        return new StampedeEffect(owner, coords[0], coords[1], getRadius());
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 40;
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.card.Card.class, "cardId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.card.Card.class, "owner", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.card.Card.class, "found", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.card.Card.class, "cardId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.card.Card.class, "owner", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.card.Card.class, "found", this, out);
    }
}
