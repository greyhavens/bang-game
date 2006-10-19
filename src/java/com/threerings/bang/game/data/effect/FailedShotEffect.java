//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.client.DudShotHandler;
import com.threerings.bang.game.client.EffectHandler;
import com.threerings.bang.game.client.MisfireHandler;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Communicates that a shot failed to fire properly.
 */
public class FailedShotEffect extends ShotEffect
{
    public int animType;

    public FailedShotEffect ()
    {
    }

    public FailedShotEffect (Piece shooter, Piece target, int damage)
    {
        shooterId = shooter.pieceId;
        baseDamage = damage;
        newDamage = Math.min(100, shooter.damage + damage);
        setTarget(target, damage, null, null);
    }

    /**
     * Configures this shot effect with a target and damage amount.  The
     * damage value will be applied to the shooter since the shot failed.
     */
    public void setTarget (Piece target, int damage, 
                      String[] attackIcons, String[] defendIcons)
    {
        targetId = target.pieceId;
        xcoords = append(null, target.x);
        ycoords = append(null, target.y);
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        animType = type;
        type = (short)(baseDamage == 0 ? DUD : MISFIRE);
        Piece shooter = bangobj.pieces.get(shooterId);
        if (shooter != null) {
            dammap.increment(shooter.owner, newDamage - shooter.damage);
            if (newDamage == 100) {
                Effect effect = shooter.willDie(bangobj, shooterId);
                preShotEffects = (effect == null) ?
                    Piece.NO_EFFECTS : new Effect[] { effect };
            }
            for (Effect effect : preShotEffects) {
                effect.prepare(bangobj, dammap);
            }
            
        } else {
            log.warning("FailedShot effect missing shooter [id=" +
                    shooterId + "].");
        }
    }

    @Override // documentation inherited
    public boolean applyTarget (
            BangObject bangobj, Unit shooter, Observer obs)
    {
        if (baseDamage > 0) {
            return damage(
                    bangobj, obs, shooter.owner, shooter, newDamage, DAMAGED);
        }
        Piece target;
        if (targetId != -1 && 
                (target = bangobj.pieces.get(targetId)) != null) {
            reportEffect(obs, target, DUDDED);
        }
        return true;
    }

    @Override // documentation inherited
    public EffectHandler createHandler (BangObject bangobj)
    {
        if (baseDamage == 0) {
            return new DudShotHandler(animType);
        } else {
            return new MisfireHandler(animType);
        }
    }
    
    @Override // documentation inherited
    public int getBaseDamage (Piece piece)
    {
        return (piece.pieceId == shooterId) ? baseDamage : 0;
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(shooterId);
        if (piece == null || piece.owner != pidx || pidx == -1) {
            return null;
        }
        if (baseDamage == 0) {
            return MessageBundle.compose("m.effect_dud", piece.getName());
        } else {
            return MessageBundle.compose("m.effect_misfire", piece.getName(),
                MessageBundle.taint(baseDamage));
        }
    }
}
