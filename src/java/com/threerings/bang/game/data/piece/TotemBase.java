//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.TotemBaseSprite;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.effect.TotemEffect;

import static com.threerings.bang.Log.log;

/**
 * A totem base can have totem pieces added to it and toped off by a
 * totem crown.
 */
public class TotemBase extends Prop
{
    @Override // documentation inherited
    public void init ()
    {
        super.init();
        lastActed = Short.MAX_VALUE;
    }

    /**
     * Returns true if a totem piece can be added to the base.
     */
    public boolean canAddPiece ()
    {
        // the only time you can't is if a crown is on top
        return !TotemEffect.TOTEM_CROWN_BONUS.equals(getTopPiece());
    }

    /**
     * Add a totem piece to the base.
     */
    public void addPiece (BangObject bangobj, String type, int owner)
    {
        int idx = _pieces.size() - 1;
        if (idx > -1) {
            _pieces.get(idx).damage = damage;
        }
        setOwner(bangobj, owner);
        PieceData data = new PieceData(type, owner);
        _pieces.add(data);
        damage = data.type.damage();
    }

    @Override // documentation inherited
    public void wasDamaged (int newDamage)
    {
        super.wasDamaged(newDamage);
        _destroyedOwner = -1;
    }

    @Override // documentation inherited
    public void wasKilled (short tick)
    {
        int idx = _pieces.size() - 1;
        PieceData pd = _pieces.remove(idx--);
        _destroyedOwner = pd.owner;
        _destroyedType = pd.type;
        if (idx > -1) {
            pd = _pieces.get(idx);
            damage = pd.damage;
            owner = pd.owner;
        } else {
            damage = 0;
            owner = -1;
        }
    }

    /**
     * Returns the height of the totem.
     */
    public int getTotemHeight ()
    {
        return _pieces.size();
    }

    /**
     * Returns the number of pieces on the totem.
     */
    public int numPieces ()
    {
        return _pieces.size();
    }

    /**
     * Returns the type of piece on the top.
     */
    public String getTopPiece ()
    {
        if (_pieces.size() > 0) {
            return getType(_pieces.size() - 1).bonus();
        }
        return null;
    }

    /**
     * Returns the owner id of the piece on top.
     */
    public int getTopOwner ()
    {
        return getOwner(_pieces.size() - 1);
    }

    /**
     * Returns the owner of the piece at index idx.
     */
    public int getOwner (int idx)
    {
        return (idx < 0 || idx >= _pieces.size()) ?
            -1 : _pieces.get(idx).owner;
    }

    /**
     * Returns the owner of the last piece destroyed.  The destroyed owner
     * will be reset to -1 after calling this function.
     */
    public int getDestroyedOwner ()
    {
        int owner = _destroyedOwner;
        _destroyedOwner = -1;
        return owner;
    }

    /**
     * Returns the type of the piece at index idx.
     */
    public TotemBonus.Type getType (int idx)
    {
        if (idx < 0 || idx >= _pieces.size()) {
            log.warning("Requested type of OOB totem", "idx", idx, "have", _pieces.size());
            Thread.dumpStack();
            return TotemBonus.Type.TOTEM_SMALL;
        } else {
            return _pieces.get(idx).type;
        }
    }

    /**
     * Returns the type of the last piece destroyed.  The destroyed type
     * will be set to null after calling this function.
     */
    public TotemBonus.Type getDestroyedType ()
    {
        TotemBonus.Type type = _destroyedType;
        _destroyedType = null;
        return type;
    }

    @Override // documentation inherited
    public boolean isTargetable ()
    {
        return _pieces.size() > 0 && canAddPiece();
    }

    @Override // documentation inherited
    public boolean willBeTargetable ()
    {
        return true;
    }

    @Override // documentation inherited
    public int getTicksPerMove ()
    {
        return Integer.MAX_VALUE;
    }

    @Override // documentation inherited
    public int getGoalRadius (BangObject bangobj, Piece mover)
    {
        return (mover instanceof Unit && canAddPiece() &&
            TotemEffect.isTotemBonus(((Unit)mover).holding)) ?
                +1 : -1;
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
        @SuppressWarnings("unchecked") ArrayList<PieceData> npieces =
            (ArrayList<PieceData>)base._pieces.clone();
        base._pieces = npieces;
        return base;
    }

    @Override // documentation inherited
    public String getName ()
    {
        return MessageBundle.qualify(GameCodes.GAME_MSGS, "m.totem");
    }

    protected static class PieceData extends SimpleStreamableObject
    {
        public int owner;
        public TotemBonus.Type type;
        public int damage;

        public PieceData ()
        {
        }

        public PieceData (String type, int owner)
        {
            this.type = TotemBonus.TOTEM_LOOKUP.get(type);
            this.owner = owner;
        }
    }

    protected ArrayList<PieceData> _pieces = new ArrayList<PieceData>();
    protected transient int _destroyedOwner = -1;
    protected transient TotemBonus.Type _destroyedType;
}
