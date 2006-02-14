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
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.RepairEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;
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
    }

    @Override // documentation inherited
    public boolean execute ()
    {
        // applying the effect may result in calls to pieceAffected() which may
        // trigger visualizations or animations which we will then notice and
        // report that the view should wait
        _effect.apply(_bangobj, this);
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
        // if this piece is now dead, clear out any queued move
        if (!piece.isAlive()) {
            _view.clearQueuedMove(piece.pieceId);
        }

        final PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            log.warning("Missing sprite for effect [piece=" + piece +
                        ", effect=" + effect + "].");
            return;
        }

        // create the appropriate visual effect
        boolean wasShot = false;
        if (effect.equals(ShotEffect.DAMAGED)) {
            wasShot = true;
        } else if (effect.equals(ShotEffect.EXPLODED)) {
            wasShot = true;
            _effviz = new ExplosionViz(false);
            // they just got shot, clear any pending shot
            ((UnitSprite)sprite).setPendingShot(false);
        } else if (effect.equals(AreaDamageEffect.MISSILED)) {
            wasShot = true;
            _effviz = new ExplosionViz(true);
        } else if (effect.equals(RepairEffect.REPAIRED)) {
            _effviz = new RepairViz();
        }

        // add wreck effect for steam-powered units
        if (wasShot && piece instanceof Unit &&
            ((Unit)piece).getConfig().make == UnitConfig.Make.STEAM &&
            (_effviz instanceof ExplosionViz || !piece.isAlive())) {
            _effviz = new WreckViz(_effviz);
        } 

        // queue the effect up on the piece sprite
        if (_effviz != null) {
            queueEffect(sprite, piece, _effviz);
        } else {
            // since we're not displaying an effect, we update immediately
            sprite.updated(piece, _tick);
        }

        // if we're rotating someone in preparation for a shot; just update
        // their orientation immediately for now
        if (effect.equals(ShotEffect.ROTATED)) {
            ((MobileSprite)sprite).faceTarget();
        }

        // if this piece was shot, trigger the reacting or dying animation
        if (wasShot && sprite instanceof MobileSprite) {
            MobileSprite msprite = (MobileSprite)sprite;
            if (piece.isAlive()) {
                queueAction(msprite, "reacting");
                
            } else {
                queueAction(msprite, msprite.hasAction("dying") ?
                    "dying" : "dead");
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
    }

    // documentation inherited from interface Effect.Observer
    public void pieceMoved (Piece piece)
    {
        PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            return;
        }

        sprite.updated(piece, _tick);
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
    public void pieceRemoved (Piece piece)
    {
        _view.removePieceSprite(piece.pieceId, "deathByEffect");
    }

    // documentation inherited from interface Effect.Observer
    public void tickDelayed (long extraTime)
    {
    }

    protected void queueEffect (
        final PieceSprite sprite, final Piece piece, EffectViz viz)
    {
        final int penderId = notePender();
//         log.info("Queueing effect " + this +
//                  " [viz=" + viz + ", pid=" + penderId + "].");
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
//         log.info("Queueing effect " + this +
//                  " [action=" + action + ", pid=" + penderId + "].");
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
        return (_penders.size() == 0);
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
//             log.info("Completing " + this);
            _view.actionCompleted(this);
//         } else {
//             log.info("Not completing " + this +
//                      " [penders=" + _penders + "].");
        }
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected BangBoardView _view;
    protected short _tick;

    protected SoundGroup _sounds;

    protected Effect _effect;
    protected EffectViz _effviz;

    protected ArrayIntSet _penders = new ArrayIntSet();
    protected int _nextPenderId;
}
