//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.effect.SetInfluenceEffect;

import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * An effect that causes the piece in question to adjust it's move distance
 * for some number of ticks, or until it is killed and respawned. 
 */
public class RandomInfluenceEffect extends SetInfluenceEffect
{
    public enum Kind { NONE, UP_MOVE, UP_ATTACK, UP_DEFENSE, EXPLODE };
    
    public RandomInfluenceEffect ()
    {
        influenceType = Unit.InfluenceType.SPECIAL;
    }

    public RandomInfluenceEffect (int pieceId, Kind kind)
    {
        influenceType = Unit.InfluenceType.SPECIAL;
        this.pieceId = pieceId;
        _kind = kind;
    }

    protected Influence createInfluence (Unit target) {
        Influence randomInfluence = null;
        switch (_kind) {
        case UP_MOVE:
            randomInfluence = createUpMove();
            break;
        case UP_ATTACK:
            randomInfluence = createUpAttack();
            break;
        case UP_DEFENSE:
            randomInfluence = createUpDefense();
            break;
        default:
            log.warning(this + "bad influence kind");
            break;
        }
        return randomInfluence;
    }
    
    /** Returns the name of the effect that will be reported. */
    protected String getEffectName () {
        return "random_influence";
    }
    
    protected Influence createUpMove () {
        return new Influence() { 
            public String getName () {
                  return "increase_move_distance";
            }    
            public int adjustMoveDistance (int moveDistance) {
                return moveDistance + 1;
            }
        };
    }
    
    protected Influence createUpAttack () {
        return new Influence() {
            public String getName () {
                  return "increase_attack";
            }    
            public int adjustAttack (Piece target, int damage) {
                return Math.round(1.3f * damage);
            }
            public boolean didAdjustAttack () {
                return true;
            }
            public boolean showClientAdjust () {
                return true;
            }
        };
    }    
    
    protected Influence createUpDefense() {
        return new Influence() {
            public String getName () {
                  return "increase_defense";
            }    
            public int adjustDefend (Piece shooter, int damage) {
                return Math.round(0.7f * damage);
            }
            public int adjustProxDefend (Piece shooter, int damage) {
                return adjustDefend(shooter, damage);
            }
            public boolean didAdjustDefend () {
                return true;
            }
        };
    }
    
    protected Kind _kind;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.SetInfluenceEffect.class, "influenceType", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.RandomInfluenceEffect.class, "_kind", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.SetInfluenceEffect.class, "influenceType", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.RandomInfluenceEffect.class, "_kind", this, out);
    }
}
