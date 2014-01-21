//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Point;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.state.MaterialState;

import com.threerings.jme.sprite.BallisticPath;
import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Sprite;

import com.threerings.bang.util.RenderUtil;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.ShotSprite;
import com.threerings.bang.game.client.sprite.Spinner;
import com.threerings.bang.game.client.sprite.TotemBaseSprite;
import com.threerings.bang.game.data.effect.HoldEffect;
import com.threerings.bang.game.data.effect.FoolsNuggetEffect;
import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.effect.TotemEffect;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.*;

/**
 * Handles the visualization of {@link HoldEffect}s not embedded within another
 * effect.
 */
public class HoldHandler extends EffectHandler
{
    @Override // documentation inherited
    public boolean execute ()
    {
        if (_effect instanceof MoveEffect) {
            Piece piece = _bangobj.pieces.get(((MoveEffect)_effect).pieceId);
            if (piece != null) {
                _dropTrans = getHoldingTranslation(piece);
            }
        }
        return super.execute();
    }

    @Override // documentation inherited
    public void pieceAffected (Piece piece, String effect)
    {
        // if removing from claim, postpone the piece update until the
        // nugget flight completes
        if (effect.equals(NuggetEffect.PICKED_UP_NUGGET) &&
            _effect instanceof NuggetEffect &&
            ((NuggetEffect)_effect).claimId > 0) {
            return;
        } else if (effect.equals(((HoldEffect)_effect).getPickedUpEffect()) &&
            _pickedUp != null) {
            flyPickedUpBonus(piece, _pickedUp);
            return;
        }
        super.pieceAffected(piece, effect);
        if (effect.equals(NuggetEffect.NUGGET_ADDED)) {
            flyNugget(piece, true, false);
        } else if (effect.equals(NuggetEffect.NUGGET_REMOVED)) {
            flyNugget(piece, false, false);
        } else if (effect.equals(FoolsNuggetEffect.FOOLS_NUGGET_REJECTED)) {
            flyNugget(piece, true, true);
        } else if (effect.equals(TotemEffect.TOTEM_ADDED)) {
            flyTotem(piece);
        }
    }

    @Override // documentation inherited
    public void pieceMoved (Piece piece)
    {
        super.pieceMoved(piece);
        if (piece instanceof Bonus && _dropTrans != null) {
            flyDroppedBonus(_dropTrans, _view.getPieceSprite(piece), false); 
        }
    }

    @Override // documentation inherited
    public void pieceRemoved (Piece piece)
    {
        if (piece instanceof Bonus) {
            PieceSprite sprite = _view.getPieceSprite(piece);
            if (sprite != null) {
                _pickedUp = sprite;
                return;
            }
        }
        super.pieceRemoved(piece);
    }
    
    /**
     * Flies a nugget from a unit to a claim or vice-versa (possibly flying it
     * back out and onto the board if rejected).
     */
    protected void flyNugget (
        final Piece claim, final boolean added, final boolean rejected)
    {
        NuggetEffect neffect = (NuggetEffect)_effect;
        final Piece unit = _bangobj.pieces.get(neffect.pieceId);
        if (unit == null) {
            return;
        }
        Vector3f ctrans = getHoldingTranslation(claim),
            utrans = getHoldingTranslation(unit),
            from = added ? utrans : ctrans;
        
        ShotSprite sprite = new ShotSprite(
            _ctx, "bonuses/frontier_town/nugget", null);
        final MaterialState mstate = _ctx.getRenderer().createMaterialState();
        mstate.getAmbient().set(ColorRGBA.white);
        mstate.getDiffuse().set(ColorRGBA.white);
        sprite.setRenderState(mstate);
        sprite.setRenderState(RenderUtil.blendAlpha);
        sprite.addController(new Spinner(sprite, FastMath.PI/2));
        _view.addSprite(sprite);
        
        BallisticShotHandler.PathParams pparams =
            BallisticShotHandler.computePathParams(
                from, added ? ctrans : utrans);
        final int penderId = notePender();
        sprite.move(new BallisticPath(sprite, from, pparams.velocity,
            BallisticShotHandler.GRAVITY_VECTOR, pparams.duration) {
            public void update (float time) {
                super.update(time);
                float alpha = Math.min(_accum / _duration, 1f);
                if (added) {
                    alpha = 1f - alpha;
                }
                mstate.getDiffuse().a = alpha;
            }
            public void wasRemoved () {
                super.wasRemoved();
                _view.removeSprite(_sprite);
                if (!added) {
                    HoldHandler.super.pieceAffected(unit,
                        NuggetEffect.PICKED_UP_NUGGET);
                } else if (rejected) {
                    rejectNugget(claim);
                }
                maybeComplete(penderId);
            }
        });
    }
    
    /**
     * Flies the nugget from the claim back onto the board, scaling it to
     * its normal size and bouncing it a few times before fading it into
     * the ground.
     */
    protected void rejectNugget (Piece claim)
    {
        // create a dummy piece sprite at an occupiable spot
        Point spot = _bangobj.board.getOccupiableSpot(claim.x, claim.y, 3);
        if (spot == null) {
            return;
        }
        Bonus dummy = Bonus.createBonus("frontier_town/fools_nugget");
        dummy.pieceId = -1;
        dummy.position(spot.x, spot.y);
        PieceSprite sprite = dummy.createSprite();
        sprite.init(_ctx, _view, _bangobj.board, _sounds, dummy, _tick);
        _view.addSprite(sprite);
        
        // fly the sprite from the claim to its position, scaling it and
        // fading it in
        flyDroppedBonus(claim, sprite, true);
    }
    
    /**
     * Flies a picked-up bonus into the hands of the unit picking it up.
     */
    protected void flyPickedUpBonus (final Piece picker, PieceSprite sprite)
    {
        Vector3f start = new Vector3f(sprite.getWorldTranslation()),
            end = getHoldingTranslation(picker);
        final int penderId = notePender();
        sprite.move(new LinePath(sprite, start, end, PICK_UP_DURATION) {
            public void update (float time) {
                super.update(time);
                float alpha = Math.min(_accum / _duration, 1f);
                _sprite.setLocalScale(FastMath.LERP(alpha, 1f, 0.5f));
            }
            public void wasRemoved () {
                super.wasRemoved();
                HoldHandler.super.pieceRemoved(
                    ((PieceSprite)_sprite).getPiece());
                HoldHandler.super.pieceAffected(picker,
                    ((HoldEffect)_effect).getPickedUpEffect());
                maybeComplete(penderId);
            }    
        });
    }
    
    /**
     * Flies a totem from a unit to the totem base.
     */
    protected void flyTotem (final Piece base)
    {
        TotemEffect teffect = (TotemEffect)_effect;
        final Piece unit = _bangobj.pieces.get(teffect.pieceId);
        if (unit == null) {
            return;
        }
        TotemBaseSprite bsprite = (TotemBaseSprite)_view.getPieceSprite(base);
        if (bsprite == null) {
            return;
        }
        Sprite psprite = bsprite.getTopPiece();
        if (psprite == null) {
            log.warning("Totem base sprite missing top piece", "base", base);
            return;
        }
        Vector3f from = bsprite.worldToLocal(
            getHoldingTranslation(unit), new Vector3f());
        BallisticShotHandler.PathParams pparams =
            BallisticShotHandler.computePathParams(from, Vector3f.ZERO);
        final int penderId = notePender();
        psprite.move(new BallisticPath(psprite, from, pparams.velocity,
            BallisticShotHandler.GRAVITY_VECTOR, pparams.duration) {
            public void update (float time) {
                super.update(time);
                float alpha = Math.min(_accum / _duration, 1f);
                _sprite.setLocalScale(FastMath.LERP(alpha, 0.5f, 1f));
            }
            public void wasRemoved () {
                super.wasRemoved();
                _sprite.getLocalTranslation().set(Vector3f.ZERO);
                maybeComplete(penderId);
            }
        });
    }
    
    /** The sprite for the last bonus picked up. */
    protected PieceSprite _pickedUp;

    /** The initial translation for a moved bonus. */
    protected Vector3f _dropTrans;
    
    /** The duration of the pick-up flight. */
    protected static final float PICK_UP_DURATION = 0.1f;
}
