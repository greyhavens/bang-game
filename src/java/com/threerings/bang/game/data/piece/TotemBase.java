//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.util.StreamableArrayList;

import com.threerings.bang.game.client.sprite.MarkerSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TotemBaseSprite;

import com.threerings.bang.game.data.effect.TotemEffect;

/**
 * A totem base can have totem pieces added to it and toped off by a 
 * totem crown.
 */
public class TotemBase extends Prop
{
    /**
     * Returns true if a totem piece can be added to the base.
     */
    public boolean canAddPiece ()
    {
        // the only time you can't is if a crown is on top
        int idx = _pieces.size() - 1;
        return !(idx > -1 && 
                _pieces.get(idx).equals(TotemEffect.TOTEM_CROWN_BONUS));
    }

    /**
     * Add a totem piece to the base.
     */
    public void addPiece (String type)
    {
        _pieces.add(type);
    }

    /** 
     * Returns the height of the totem.
     */
    public int getTotemHeight ()
    {
        return _pieces.size();
    }

    /**
     * Returns the type of piece on the top.
     */
    public String getTopPiece ()
    {
        if (_pieces.isEmpty()) {
            return null;
        }
        return _pieces.get(_pieces.size() - 1);
    }

    @Override // documentation inherited
    public boolean isTargetable ()
    {
        return true;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new TotemBaseSprite();
    }

    @Override // documentation inherited
    public Object clone ()
    {
        TotemBase base = (TotemBase)super.clone();
        @SuppressWarnings("unchecked") StreamableArrayList<String> npieces =
            (StreamableArrayList<String>)base._pieces.clone();
        base._pieces = npieces;
        return base;
    }
    
    protected transient StreamableArrayList<String> _pieces = 
        new StreamableArrayList<String>();
}
