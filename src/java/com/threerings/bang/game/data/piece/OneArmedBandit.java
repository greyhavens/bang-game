//
// $Id$

package com.threerings.bang.game.data.piece;

import com.samskivert.util.ArrayUtil;

import com.threerings.bang.game.data.piece.Unit;


/**
 * Handles some special custom behavior needed for the One Armed Jack.
 */
public class OneArmedBandit extends Unit
{
    /** An additional influence caused by unit's unique ability. */
    /*public transient Influence randomInfluence;	

	@Override // documentation inherited
    public void respawnInit (BangObject bangobj)
    {
		super.respawnInit(bangobj);
		randomInfluence = null;
	}

	@Override // documentation inherited
    public Effect[] willShoot (
            BangObject bangobj, Piece target, ShotEffect shot)
    {
		// concatenate random influence effects
		Effect[] effects = super.willShoot(bangobj, target, shot);
        if (randomInfluence != null) {
			Effect[] randomEffects = randomInfluence.willShoot(bangobj, target, shot);
			effects = ArrayUtil.concatenate(effects, randomEffects);
        }
        return effects;
    }

	@Override // documentation inherited
    public ArrayList<Effect> tick (
            short tick, BangObject bangobj, Piece[] pieces)
    {
	    ArrayList<Effect> effects = super.tick(tick, bangobj, pieces);
	    if (randomInfluence != null && randomInfluence.isExpired(tick)) {
            ExpireInfluenceEffect effect = randomInfluence.createExpireEffect();
            effect.init(this);
            effects.add(effect);
        }
		return effects;
	}

	@Override // documentation inherited
    public String[] attackInfluenceIcons ()
    {
        if (_attackIcons == null) {
            _attackIcons = new ArrayList<String>();
        }	
		if (randomInfluence != null && randomInfluence.didAdjustAttack()) {
            _attackIcons.add(randomInfluence.getName());
        }
        return super.attackInfluenceIcons();
    }

    @Override // documentation inherited
    protected int getTicksPerMove ()
    {
        int ticks = (randomInfluence == null) ? super.getTicksPerMove() :
            influence.adjustTicksPerMove(super.getTicksPerMove());
        ticks = (holdingInfluence == null) ?
            ticks : holdingInfluence.adjustTicksPerMove(ticks);
        return (hindrance == null) ? ticks :
            super.getTicksPerMove();
    }*/

}
