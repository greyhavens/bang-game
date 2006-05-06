//
// $Id$

package com.threerings.bang.game.client;

import com.samskivert.util.ArrayIntSet;

import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.SoundUtil;

import com.threerings.bang.game.client.effect.EffectViz;
import com.threerings.bang.game.client.effect.ExplosionViz;
import com.threerings.bang.game.client.effect.IconViz;
import com.threerings.bang.game.client.effect.PlaySoundViz;
import com.threerings.bang.game.client.effect.RepairViz;
import com.threerings.bang.game.client.effect.WreckViz;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;

import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.effect.RepairEffect;
import com.threerings.bang.game.data.effect.ResurrectEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Handles the visual representation of a complex effect on the client.
 */
public class EffectHandler extends BoardView.BoardAction
    implements Effect.Observer
{
    /** Initializes the handler. */
    public void init (BangContext ctx, BangObject bangobj, BangBoardView view,
                      SoundGroup sounds, Effect effect)
    {
        _ctx = ctx;
        _tick = bangobj.tick;
        _bangobj = bangobj;
        _view = view;
        _sounds = sounds;
        _effect = effect;
        pieceIds = effect.getAffectedPieces();
        waiterIds = effect.getWaitPieces();

        // if this is a move effect, note the pending move
        if (_effect instanceof MoveEffect) {
            for (int ii = 0; ii < pieceIds.length; ii++) {
                _view.notePendingMove(pieceIds[ii]);
            }
        }
    }

    @Override // documentation inherited
    public boolean execute ()
    {
        // don't allow the effect to complete until we're done applying
        // everything
        _applying = true;

        // applying the effect may result in calls to pieceAffected() which may
        // trigger visualizations or animations which we will then notice and
        // report that the view should wait
        _effect.apply(_bangobj, this);
        _applying = false;

        // now determine whether or not anything remained pending
        return !isCompleted();
    }

    // documentation inherited from interface Effect.Observer
    public void pieceAdded (Piece piece)
    {
        _view.queuePieceCreate(piece, _tick);
    }

    // documentation inherited from interface Effect.Observer
    public void pieceAffected (Piece piece, String effect)
    {
        final PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            log.warning("Missing sprite for effect [piece=" + piece +
                        ", effect=" + effect + "].");
            return;
        }

        // create the appropriate visual effect
        boolean wasDamaged = false;
        if (effect.equals(ShotEffect.DAMAGED)) {
            wasDamaged = true;
        } else if (effect.equals(ShotEffect.EXPLODED)) {
            wasDamaged = true;
            _effviz = new ExplosionViz(false);
        } else if (effect.equals(AreaDamageEffect.MISSILED)) {
            wasDamaged = true;
            _effviz = new ExplosionViz(true);
        } else if (effect.equals(RepairEffect.REPAIRED) ||
                   effect.equals(NuggetEffect.NUGGET_ADDED) ||
                   effect.equals(ResurrectEffect.RESURRECTED)) {
            _effviz = new RepairViz();
        }

        // if they were damaged, go ahead and clear any pending shot
        if (wasDamaged) {
            ((UnitSprite)sprite).setPendingShot(false);
        }

        // add wreck effect for steam-powered units
        if (wasDamaged && piece instanceof Unit &&
            ((Unit)piece).getConfig().make == UnitConfig.Make.STEAM &&
            (_effviz instanceof ExplosionViz || !piece.isAlive())) {
            _effviz = new WreckViz(_effviz);
        } 

        // queue the effect up on the piece sprite
        if (_effviz != null) {
            queueEffect(sprite, piece, _effviz);

        } else if (effect.equals(ShotEffect.ROTATED)) {
            // if we're rotating someone in preparation for a shot; just update
            // their orientation immediately for now
            ((MobileSprite)sprite).faceTarget();

        } else if (effect.equals(ShotEffect.SHOT_NOMOVE)) {
            // if they shot without moving, we'll need to clear out any advance
            // order now because we normally do it after they move
            _view.clearAdvanceOrder(piece.pieceId);
            // and update them to reset their tick display
            sprite.updated(piece, _tick);

        } else {
            // since we're not displaying an effect, we update immediately
            sprite.updated(piece, _tick);
        }

        // if this piece was shot, trigger the reacting or dying animation
        if (wasDamaged && sprite instanceof MobileSprite) {
            MobileSprite msprite = (MobileSprite)sprite;
            if (piece.isAlive()) {
                queueAction(msprite, "reacting");
            } else if (msprite.hasAction("dying")) {
                queueAction(msprite,  "dying");
            } else {
                // units with no dying animation will react while the explosion
                // is going off and then switch to their dead model
                queueAction(msprite, "reacting");
                queueAction(msprite, MobileSprite.DEAD);
            }
        }

        // perhaps show an icon animation indicating what happened
        IconViz iviz = IconViz.createIconViz(piece, effect);
        // TODO: make this not an effect viz
        if (iviz != null) {
            iviz.init(_ctx, _view, piece, null);
            iviz.display(sprite);
        }

        // also play a sound along with any visual effect
        String path = "rsrc/" + effect + ".wav";
        if (SoundUtil.haveSound(path)) {
            // TODO: make this not an effect viz
            new PlaySoundViz(_sounds, path).display(sprite);
        }

        // report the effect to the view who will report it to the tutorial
        // controller if appropriate
        _view.pieceWasAffected(piece, effect);
    }

    // documentation inherited from interface Effect.Observer
    public void pieceMoved (Piece piece)
    {
        // clear the pending moves we set earlier
        if (_effect instanceof MoveEffect) {
            for (int ii = 0; ii < pieceIds.length; ii++) {
                _view.clearPendingMove(pieceIds[ii]);
            }
        }

        PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            return;
        }

        // update the sprite (TODO: is this needed?)
        sprite.updated(piece, _tick);

        // let the board view know that this piece is on the move
        _view.pieceDidMove(piece);

        // and do the actual move
        if (!sprite.updatePosition(_bangobj.board)) {
            return;
        }

        final int penderId = ++_nextPenderId;
        _penders.add(penderId);
        sprite.addObserver(new PathObserver() {
            public void pathCancelled (Sprite sprite, Path path) {
                sprite.removeObserver(this);
                maybeComplete(penderId);
            }
            public void pathCompleted (Sprite sprite, Path path) {
                sprite.removeObserver(this);
                maybeComplete(penderId);
            }
        });
    }

    // documentation inherited from interface Effect.Observer
    public void pieceKilled (Piece piece)
    {
        _view.pieceWasKilled(piece.pieceId);
    }

    // documentation inherited from interface Effect.Observer
    public void pieceRemoved (Piece piece)
    {
        _view.removePieceSprite(piece.pieceId, "deathByEffect");
    }

    // documentation inherited from interface Effect.Observer
    public void tickDelayed (long extraTime)
    {
    }

    @Override // documentation inherited
    public String toString ()
    {
        return super.toString() + ":" + _effect;
    }

    protected void queueEffect (
        final PieceSprite sprite, final Piece piece, EffectViz viz)
    {
        final int penderId = notePender();
        if (BoardView.ACTION_DEBUG) {
            log.info("Queueing effect " + this +
                     " [viz=" + viz + ", pid=" + penderId + "].");
        }
        viz.init(_ctx, _view, piece, new EffectViz.Observer() {
            public void effectDisplayed () {
                sprite.updated(piece, _tick);
                maybeComplete(penderId);
            }
        });
        viz.display(sprite);
    }

    /**
     * Queues up an action on a sprite and sets up the necessary observation to
     * ensure that we wait until the action is completed to complete our
     * effect.
     */
    protected void queueAction (MobileSprite sprite, String action)
    {
        final int penderId = notePender();
        if (BoardView.ACTION_DEBUG) {
            log.info("Queueing effect " + this +
                     " [action=" + action + ", pid=" + penderId + "].");
        }
        sprite.addObserver(new MobileSprite.ActionObserver() {
            public void actionCompleted (Sprite sprite, String action) {
                sprite.removeObserver(this);
                maybeComplete(penderId);
            }
        });
        sprite.queueAction(action);
    }

    protected boolean isCompleted ()
    {
        return (!_applying && _penders.size() == 0);
    }

    protected int notePender ()
    {
        _penders.add(++_nextPenderId);
        return _nextPenderId;
    }

    protected void maybeComplete (int penderId)
    {
        _penders.remove(penderId);
        if (isCompleted()) {
            if (BoardView.ACTION_DEBUG) {
                log.info("Completing " + this);
            }
            _view.actionCompleted(this);
        } else {
            if (BoardView.ACTION_DEBUG) {
                log.info("Not completing " + this +
                         " [penders=" + _penders + "].");
            }
        }
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected BangBoardView _view;
    protected short _tick;
    protected boolean _applying;

    protected SoundGroup _sounds;

    protected Effect _effect;
    protected EffectViz _effviz;

    protected ArrayIntSet _penders = new ArrayIntSet();
    protected int _nextPenderId;
}
