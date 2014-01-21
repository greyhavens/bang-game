//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Rectangle;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Spatial;
import com.jme.scene.state.MaterialState;
import com.jme.scene.state.RenderState;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Interval;
import com.samskivert.util.StringUtil;

import com.threerings.jme.sprite.BallisticPath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;
import com.threerings.openal.Sound;
import com.threerings.openal.SoundGroup;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.util.ResultAttacher;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.SoundUtil;

import com.threerings.bang.game.client.effect.DamageIconViz;
import com.threerings.bang.game.client.effect.EffectViz;
import com.threerings.bang.game.client.effect.ExplosionViz;
import com.threerings.bang.game.client.effect.HealHeroViz;
import com.threerings.bang.game.client.effect.IconViz;
import com.threerings.bang.game.client.effect.ParticlePool;
import com.threerings.bang.game.client.effect.PlaySoundViz;
import com.threerings.bang.game.client.effect.RepairViz;
import com.threerings.bang.game.client.effect.WreckViz;

import com.threerings.bang.game.client.sprite.ActiveSprite;
import com.threerings.bang.game.client.sprite.BonusSprite;
import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.Targetable;
import com.threerings.bang.game.client.sprite.TreeBedSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;

import com.threerings.bang.game.data.effect.AddPieceEffect;
import com.threerings.bang.game.data.effect.AddSpawnedBonusEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.HealHeroEffect;
import com.threerings.bang.game.data.effect.HighNoonEffect;
import com.threerings.bang.game.data.effect.HoldEffect;
import com.threerings.bang.game.data.effect.LevelEffect;
import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.effect.RepairEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.effect.TreeBedEffect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.LoggingRobot;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.TotemBase;
import com.threerings.bang.game.data.piece.TotemBonus;
import com.threerings.bang.game.data.piece.TreeBed;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Breakable;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Handles the visual representation of a complex effect on the client.
 */
public class EffectHandler extends BoardView.BoardAction
    implements Effect.Observer
{
    /** Initializes the handler. */
    public void init (
        BangContext ctx, BangObject bangobj, int pidx, BangBoardView view,
        BangView bview, SoundGroup sounds, Effect effect)
    {
        _ctx = ctx;
        _tick = bangobj.tick;
        _bangobj = bangobj;
        _pidx = pidx;
        _view = view;
        _bview = bview;
        _sounds = sounds;
        _effect = effect;
        pieceIds = effect.getAffectedPieces();
        waiterIds = effect.getWaitPieces();
        moveIds = effect.getMovePieces();
        bounds = effect.getBounds(bangobj);

        // if this is a move effect, note the pending move
        for (int id : moveIds) {
            _view.notePendingMove(id);
        }
    }

    @Override // documentation inherited
    public boolean canExecute (ArrayIntSet penders,
            HashSet<Rectangle> boundset, LinkedList<Integer> syncQueue)
    {
        if (!super.canExecute(penders, boundset, syncQueue)) {
            return false;
        }
        boolean can = true;
        if (_effect instanceof MoveEffect) {
            ArrayList<EffectHandler> movers = _view.getPendingMovers();
            MoveEffect meffect = (MoveEffect)_effect;
            Piece piece = _bangobj.pieces.get(meffect.pieceId);
            if (piece == null) {
                return true;
            }
            byte state = _bangobj.board.shadowPieceTemp(
                    piece, meffect.nx, meffect.ny);
            for (EffectHandler handler : movers) {
                if (!(handler._effect instanceof MoveEffect)) {
                    continue;
                }
                MoveEffect move = (MoveEffect)handler._effect;
                Piece pender = _bangobj.pieces.get(move.pieceId);
                if (pender == null) {
                    continue;
                }
                if (_bangobj.board.computePath(
                        move.ox, move.oy, move.nx, move.ny, pender) == null) {
                    can = false;
                    break;
                }
            }
            _bangobj.board.clearShadowTemp(state, meffect.nx, meffect.ny);
        }
        return can;
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
        apply(_effect);
        _applying = false;

        // now determine whether or not anything remained pending
        return !isCompleted();
    }

    // documentation inherited from interface Effect.Observer
    public void pieceAdded (Piece piece)
    {
        _view.createPieceSprite(piece, _tick);
        if (piece instanceof Bonus && _dropper != null) {
            flyDroppedBonus(_dropper, _view.getPieceSprite(piece), false);
        }
    }

    // documentation inherited from interface Effect.Observer
    public void pieceAffected (Piece piece, String effect)
    {
        // clear the pending moves we set earlier
        if (Effect.UPDATED.equals(effect)) {
            for (int id : moveIds) {
                _view.clearPendingMove(id);
            }
        }

        final PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            log.warning("Missing sprite for effect", "piece", piece, "effect", effect);
            return;
        }

        // create the appropriate visual effect
        boolean wasDamaged = false;
        EffectViz effviz = null;
        if (effect.equals(ShotEffect.DAMAGED)) {
            wasDamaged = true;
        } else if (effect.equals(ShotEffect.EXPLODED)) {
            wasDamaged = true;
            effviz = new ExplosionViz();
        } else if (effect.equals(ShotEffect.ROCKET_BURST)) {
            wasDamaged = true;
            effviz = new ExplosionViz("boom_town/fireworks/fireworks_explosion", false);
        } else if ((_effect instanceof RepairEffect &&
            RepairEffect.isRepairEffect(effect)) ||
                effect.equals(NuggetEffect.NUGGET_ADDED)) {
            effviz = new RepairViz();
        } else if (effect.equals(HealHeroEffect.HEAL_HERO)) {
            effviz = new HealHeroViz();
        }

        // if they were damaged, go ahead and clear any pending shot
        if (wasDamaged && sprite instanceof Targetable) {
            ((Targetable)sprite).setPendingShot(false);
        }

        // display the damage icon/amount
        if (effect.equals(TreeBedEffect.GREW)) {
            TreeBed tree = (TreeBed)piece;
            DamageIconViz.displayDamageIconViz(piece,
                (tree.growth < TreeBed.FULLY_GROWN &&
                    tree.getPercentDamage() == 0f) ? "grew" : "repaired",
                TreeBedSprite.STATUS_COLOR,
                TreeBedSprite.DARKER_STATUS_COLOR, // cyan
                -_effect.getBaseDamage(piece), true, _effect, _ctx, _view);
        } else if (effect.equals(ShotEffect.DAMAGED) &&
            piece instanceof TreeBed) {
            DamageIconViz.displayDamageIconViz(piece,
                new ColorRGBA(1f, 0f, 1f, 1f),
                new ColorRGBA(0.5f, 0f, 0.5f, 1f), // magenta
                true, _effect, _ctx, _view);
        } else if (wasDamaged || effect.equals(ShotEffect.DUDDED)) {
            if (piece instanceof Breakable) {
                DamageIconViz.displayDamageIconViz(piece, false, _effect, _ctx, _view);
            } else {
                DamageIconViz.displayDamageIconViz(piece, true, _effect, _ctx, _view);
            }
        }

        // possibly fly a totem piece off if one was destroyed
        if (wasDamaged && piece instanceof TotemBase) {
            maybeFlyTotemPiece((TotemBase)piece);
        }

        // add wreck effect for steam-powered units
        boolean pieceIsDeadSteamUnit = wasDamaged && piece instanceof Unit &&
            ((Unit)piece).getConfig().make == UnitConfig.Make.STEAM &&
            (effviz instanceof ExplosionViz || !piece.isAlive());

        // add wreck effect for breakables
        boolean pieceIsBreakable =
            wasDamaged && piece instanceof Breakable && !piece.isAlive();

        if (pieceIsDeadSteamUnit || pieceIsBreakable) {
            effviz = new WreckViz(effviz);
        }

        // drop the piece from the sky
        if (effect.equals(AddPieceEffect.DROPPED)) {
            dropPiece(piece);
        }

        // fly the piece in from some specified location
        if (effect.equals(AddSpawnedBonusEffect.SPAWNED_BONUS)) {
            AddSpawnedBonusEffect asbe = (AddSpawnedBonusEffect)_effect;
            Vector3f trans = new Vector3f(
                    asbe.x * TILE_SIZE + TILE_SIZE/2, asbe.y * TILE_SIZE + TILE_SIZE/2,
                    _view.getBoard().getElevation(asbe.x, asbe.y) *
                    _view.getBoard().getElevationScale(TILE_SIZE));
            flyDroppedBonus(trans, sprite, true);
            if (asbe.first) {
                displayParticles("indian_post/hero_death", trans);
                String soundPath = getSoundPath("indian_post/hero_death");
                if (soundPath != null) {
                    _sounds.getSound(soundPath).play(true);
                }
            }
        }

        // queue the effect up on the piece sprite
        if (effviz != null) {
            queueEffect(sprite, piece, effviz);

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

        // remember the last unit to drop a bonus
        if (HoldEffect.isDroppedEffect(effect)) {
            _dropper = piece;
        }

        // queue reacting, dying, or generic effects for active sprites
        if (sprite instanceof ActiveSprite) {
            ActiveSprite asprite = (ActiveSprite)sprite;
            if (wasDamaged) {
                if (piece.isAlive()) {
                    if (asprite.hasAction("reacting")) {
                        queueAction(asprite, "reacting");
                    }
                } else if (asprite.hasAction("dying")) {
                    queueAction(asprite,  "dying");
                    if (sprite instanceof UnitSprite) {
                        Sound dsound =
                            ((UnitSprite)sprite).getDyingSound(_sounds);
                        if (dsound != null) {
                            dsound.play(true);
                        }
                    }
                } else {
                    // units with no dying animation just switch to their
                    // dead model; the wreck viz or explosion should hide
                    // the sudden transition
                    queueAction(asprite, ActiveSprite.DEAD);
                }
            } else if (asprite.hasAction(effect)) {
                queueAction(asprite, effect);
            }
        }

        // perhaps show an icon animation indicating what happened
        IconViz iviz = IconViz.createIconViz(piece, effect);
        if (iviz != null) {
            iviz.init(_ctx, _view, sprite, null);
            iviz.display();
        }

        // perhaps display a generic particle effect
        if (BangPrefs.isMediumDetail()) {
            if (_ctx.getParticleCache().haveParticles(effect)) {
                boolean center = (!LevelEffect.LEVEL_UP.equals(effect));
                sprite.displayParticles(effect, center);
            }
        }

        // perhaps play a sound to go with our visual effect
        String soundPath = getSoundPath(effect);
        if (soundPath != null) {
            new PlaySoundViz(_sounds, soundPath).display();
        }

        // report the effect to the view who will report it to the tutorial
        // controller if appropriate
        _view.pieceWasAffected(piece, effect);
        _bview.pieceWasAffected(piece, effect);
    }

    // documentation inherited from interface Effect.Observer
    public void boardAffected (String effect)
    {
        if (effect != null && effect.startsWith("m.")) {
            _view.fadeMarqueeInOut(effect, 1f);
            notePender(2f);
        } else if (_ctx.getParticleCache().haveParticles(effect)) {
            _view.displayCameraParticles(effect, CAMERA_EFFECT_DURATION);
        } else if (HighNoonEffect.HIGH_NOON.equals(effect)) {
            _view.setHighNoon(true);
        } else if (effect == null && _view.isHighNoon()) {
            _view.setHighNoon(false);
        }

        // perhaps play a sound to go with our visual effect
        String soundPath = getSoundPath(effect);
        if (soundPath != null) {
            _sounds.getSound(soundPath).play(true);
        }
    }

    // documentation inherited from interface Effect.Observer
    public void pieceMoved (Piece piece)
    {
        // clear the pending moves we set earlier
        for (int id : moveIds) {
            _view.clearPendingMove(id);
        }

        PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            return;
        }

        // update the sprite (TODO: is this needed?)
        sprite.updated(piece, _tick);

        // let the board view know that this piece is on the move
        _view.pieceDidMove(piece);

        if (_bangobj.tick - _tick >= FAST_TICK_DELTA) {
            log.info("Performing fast animations", "effect", _effect, "tick", _tick,
                     "bangobj.tick", _bangobj.tick);
            sprite.fastAnimations(true);
        }

        // and do the actual move
        if (!sprite.updatePosition(_bangobj.board)) {
            sprite.fastAnimations(false);
            return;
        }

        final int penderId = notePender();
        sprite.addObserver(new PathObserver() {
            public void pathCancelled (Sprite sprite, Path path) {
                sprite.removeObserver(this);
                ((PieceSprite)sprite).fastAnimations(false);
                maybeComplete(penderId);
            }
            public void pathCompleted (Sprite sprite, Path path) {
                sprite.removeObserver(this);
                ((PieceSprite)sprite).fastAnimations(false);
                maybeComplete(penderId);
            }
        });
    }

    // documentation inherited from interface Effect.Observer
    public void pieceKilled (Piece piece, int shooter, int sidx)
    {
        _view.pieceWasKilled(piece.pieceId);
        // Allow non-ActiveSprites to update themselves when they're killed
        final PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite != null && !(sprite instanceof ActiveSprite)) {
            sprite.updated(piece, _tick);
        }
    }

    // documentation inherited from interface Effect.Observer
    public void pieceRemoved (Piece piece)
    {
        log.info("Piece removed by effect " + piece + ".");
        _view.removePieceSprite(piece.pieceId, "deathByEffect");
    }

    // documentation inherited from interface Effect.Observer
    public void cardAdded (Card card)
    {
        _bview.pstatus[card.owner].cardAdded(card, true);
    }

    // documentation inherited from interface Effect.Observer
    public void cardRemoved (Card card)
    {
        _bview.pstatus[card.owner].cardRemoved(card, true, false);
    }

    // documentation inherited from interface Effect.Observer
    public void cardPlayed (Card card, Object target)
    {
        _bview.pstatus[card.owner].cardRemoved(card, false, true);
        if (!card.shouldShowVisualization(_pidx)) {
            return;
        }

        // play a sound effect, if one exists
        String path = "rsrc/cards/" + card.getTownId() + "/" + card.getType() +
            "/play.ogg";
        if (SoundUtil.haveSound(path)) {
            _sounds.getSound(path).play(true);
        }

        // display the visualization
        IconViz iviz;
        switch (card.getPlacementMode()) {
        case VS_PIECE:
            Piece piece = _bangobj.pieces.get((Integer)target);
            if (piece == null) {
                log.warning("Missing piece for card played effect", "effect", _effect,
                            "pieceId", target);
                return;
            }
            PieceSprite sprite = _view.getPieceSprite(piece);
            if (sprite == null) {
                log.warning("Missing sprite for card played effect", "effect", _effect,
                            "piece", piece);
                return;
            } else {
                iviz = IconViz.createCardViz(card);
                iviz.init(_ctx, _view, sprite, null);
                iviz.display();
            }
            return;

        case VS_AREA:
            int[] coords = (int[])target;
            iviz = IconViz.createCardViz(card);
            iviz.init(_ctx, _view, coords[0], coords[1], null);
            iviz.display();
            return;

        case VS_PLAYER:
            _bview.showCardPlayed(card, (Integer)target);
            return;

        case VS_BOARD:
            _bview.showCardPlayed(card, -1);
            return;

        default:
            break; // nada
        }
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

    protected void apply (Effect effect)
    {
        effect.apply(_bangobj, this);
        String desc = effect.getDescription(_bangobj, _pidx);
        if (!StringUtil.isBlank(desc)) {
            _ctx.getChatDirector().displayInfo(GameCodes.GAME_MSGS, desc);
        }
    }

    protected void queueEffect (
        final PieceSprite sprite, final Piece piece, EffectViz viz)
    {
        final int penderId = notePender();
        if (BoardView.ACTION_DEBUG) {
            log.info("Queueing effect " + this, "viz", viz, "pid", penderId);
        }
        if (sprite != null) {
            viz.init(_ctx, _view, sprite, new EffectViz.Observer() {
                public void effectDisplayed () {
                    sprite.updated(piece, _tick);
                    maybeComplete(penderId);
                }
            });
            viz.display();
        }
    }

    /**
     * Checks for an activation sound for the specified effect in the various
     * places it might exist.
     */
    protected String getSoundPath (String effect)
    {
        for (int pp = 0; pp < SOUND_PREFIXES.length; pp++) {
            for (int ss = 0; ss < SOUND_SUFFIXES.length; ss++) {
                String path = SOUND_PREFIXES[pp] + effect + SOUND_SUFFIXES[ss];
                if (SoundUtil.haveSound(path)) {
                    return path;
                }
            }
        }
        return null;
    }

    /**
     * Returns an array of sounds that contains both the principal shot sound
     * and the sounds of any influences.
     */
    protected Sound[] getShotSounds (Piece shooter, ShotEffect shot)
    {
        if (!(shooter instanceof Unit)) {
            return null;
        }
        UnitSprite usprite = _view.getUnitSprite(shooter);
        if (usprite == null) {
            return null;
        }
        ArrayList<Sound> sounds = new ArrayList<Sound>();
        Sound psound = usprite.getShotSound(_sounds, shot);
        if (psound != null) {
            sounds.add(psound);
        }
        addInfluenceSounds(sounds, shot.attackIcons);
        addInfluenceSounds(sounds, shot.defendIcons);
        return sounds.toArray(new Sound[sounds.size()]);
    }

    /**
     * Looks for sounds representing the named influences, loading them and
     * adding them to the list if present.
     */
    protected void addInfluenceSounds (
        ArrayList<Sound> sounds, String[] influences)
    {
        if (influences == null) {
            return;
        }
        for (String influence : influences) {
            String path = "rsrc/influences/sounds/" + influence + ".ogg";
            if (SoundUtil.haveSound(path)) {
                sounds.add(_sounds.getSound(path));
            }
        }
    }

    /**
     * Plays a group of sounds simultaneously.
     */
    protected void playSounds (Sound[] sounds, boolean allowDefer)
    {
        if (sounds != null) {
            for (Sound sound : sounds) {
                sound.play(allowDefer);
            }
        }
    }

    /**
     * Queues up an action on a sprite and sets up the necessary observation to
     * ensure that we wait until the action is completed to complete our
     * effect.
     */
    protected void queueAction (ActiveSprite sprite, String action)
    {
        final int penderId = notePender();
        if (BoardView.ACTION_DEBUG) {
            log.info("Queueing effect " + this, "action", action, "pid", penderId);
        }
        sprite.addObserver(new ActiveSprite.ActionObserver() {
            public void actionCompleted (Sprite sprite, String action) {
                sprite.removeObserver(this);
                maybeComplete(penderId);
            }
        });
        sprite.queueAction(action);
    }

    /**
     * If a totem piece has been destroyed, flies it from the totem base onto
     * the board, bouncing it a few times before allowing it to sink into the
     * ground.
     */
    protected void maybeFlyTotemPiece (TotemBase base)
    {
        TotemBonus.Type type = base.getDestroyedType();
        if (type == null) {
            return;
        }
        PieceSprite bsprite = _view.getPieceSprite(base);
        if (bsprite == null) {
            return;
        }
        // create a dummy piece sprite at an occupiable spot
        Point spot = _bangobj.board.getOccupiableSpot(base.x, base.y, 3);
        if (spot == null) {
            return;
        }
        Bonus dummy = Bonus.createBonus(type.bonus());
        dummy.pieceId = -1;
        dummy.owner = base.getDestroyedOwner();
        dummy.position(spot.x, spot.y);
        PieceSprite sprite = dummy.createSprite();
        sprite.init(_ctx, _view, _bangobj.board, _sounds, dummy, _tick);
        _view.addSprite(sprite);

        // fly the sprite from the totem to its position
        final int penderId = notePender();
        Vector3f btrans = new Vector3f(bsprite.getWorldTranslation());
        BallisticShotHandler.PathParams pparams =
            BallisticShotHandler.computePathParams(
                btrans, sprite.getWorldTranslation());
        sprite.move(new BallisticPath(sprite, btrans, pparams.velocity,
            BallisticShotHandler.GRAVITY_VECTOR, pparams.duration) {
            public void wasRemoved () {
                super.wasRemoved();
                bounceSprite(_sprite, TILE_SIZE / 4);
                maybeComplete(penderId);
            }
        });
    }

    /**
     * Flies a dropped bonus from the unit that dropped it to its current
     * location (optionally fading it in), then bounces it a few times.
     */
    protected void flyDroppedBonus (
        Piece dropper, PieceSprite sprite, final boolean fadeIn)
    {
        flyDroppedBonus(getHoldingTranslation(dropper), sprite, fadeIn);
    }

    /**
     * Flies a dropped bonus from the unit that dropped it to its current
     * location (optionally fading it in), then bounces it a few times.
     */
    protected void flyDroppedBonus (
        Vector3f htrans, PieceSprite sprite, final boolean fadeIn)
    {
        if (sprite == null) {
            return;
        }
        final MaterialState mstate =
            (MaterialState)sprite.getRenderState(RenderState.RS_MATERIAL);
        BallisticShotHandler.PathParams pparams =
            BallisticShotHandler.computePathParams(
                htrans, sprite.getWorldTranslation());
        final int penderId = notePender();
        sprite.move(new BallisticPath(sprite, htrans, pparams.velocity,
            BallisticShotHandler.GRAVITY_VECTOR, pparams.duration) {
            public void update (float time) {
                super.update(time);
                float alpha = Math.min(_accum / _duration, 1f);
                _sprite.setLocalScale(FastMath.LERP(alpha, 0.5f, 1f));
                if (fadeIn) {
                    mstate.getDiffuse().a = alpha;
                }
            }
            public void wasRemoved () {
                super.wasRemoved();
                bounceSprite(_sprite, TILE_SIZE / 4);
                maybeComplete(penderId);
            }
        });
        sprite.updateGeometricState(0f, true);
    }

    /**
     * Computes and returns the location where bonuses are held for the
     * specified piece, which is one tile length above its base.
     */
    protected Vector3f getHoldingTranslation (Piece piece)
    {
        PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            return new Vector3f();
        }
        Vector3f trans = new Vector3f(0f, 0f, TILE_SIZE);
        sprite.getLocalRotation().multLocal(trans);
        trans.addLocal(sprite.getWorldTranslation());
        return trans;
    }

    /**
     * Drops a piece from the sky to its starting location.
     */
    protected void dropPiece (final Piece piece)
    {
        final PieceSprite sprite = _view.getPieceSprite(piece);
        if (sprite == null) {
            return;
        }
        Vector3f start = new Vector3f(sprite.getWorldTranslation());
        start.z += PIECE_DROP_HEIGHT;
        float duration = FastMath.sqrt(
            -2f * PIECE_DROP_HEIGHT / BallisticShotHandler.GRAVITY);
        final int penderId = notePender();
        sprite.getLocalTranslation().set(start);
        sprite.getShadow().getBatch(0).getDefaultColor().a = 0f;
        sprite.move(new BallisticPath(sprite, start, new Vector3f(),
            BallisticShotHandler.GRAVITY_VECTOR, duration) {
            public void update (float time) {
                super.update(time);
                sprite.getShadow().getBatch(0).getDefaultColor().a =
                    Math.min(_accum / _duration, 1f);
            }
            public void wasRemoved () {
                super.wasRemoved();
                sprite.getShadow().getBatch(0).getDefaultColor().a = 1f;
                sprite.setLocation(piece.x, piece.y, piece.computeElevation(
                    _bangobj.board, piece.x, piece.y));
                sprite.updateWorldVectors();
                if (BangPrefs.isHighDetail()) {
                    sprite.displayDustRing();
                }
                getLandSound(piece).play(true);
                if (piece instanceof Unit) {
                    if (piece instanceof LoggingRobot) {
                        ((UnitSprite)sprite).queueAction("unfolding");
                    } else {
                        pieceDropped(piece);
                    }
                } else {
                    bounceSprite(_sprite, TILE_SIZE / 4);
                }
                maybeComplete(penderId);
            }
        });
        if (piece instanceof LoggingRobot && sprite.getModelNode() != null) {
            // pause on the first frame of the unfolding animation
            sprite.getModelNode().startAnimation("unfolding");
            sprite.getModelNode().pauseAnimation(true);
        }
    }

    /**
     * Called when a piece has been dropped.
     */
    protected void pieceDropped (Piece piece)
    {
        // nothing doing
    }

    /**
     * Find and returns a sound to play on a piece's landing after its drop.
     */
    protected Sound getLandSound (Piece piece)
    {
        if (piece instanceof Unit) {
            String path = "rsrc/units/" + ((Unit)piece).getType() +
                "/land.ogg";
            if (SoundUtil.haveSound(path)) {
                return _sounds.getSound(path);
            }
        }
        return _sounds.getSound(DEFAULT_LAND_SOUND);
    }

    /**
     * Bounces a sprite up to the specified height and schedules another
     * bounce.
     */
    protected void bounceSprite (Sprite sprite, final float height)
    {
        Vector3f start = new Vector3f(sprite.getWorldTranslation());
        float duration = FastMath.sqrt(
            -2f * height / BallisticShotHandler.GRAVITY),
            vel = -BallisticShotHandler.GRAVITY * duration;
        final int penderId = notePender();
        sprite.move(new BallisticPath(sprite, start, new Vector3f(0f, 0f, vel),
            BallisticShotHandler.GRAVITY_VECTOR, duration*2) {
            public void wasRemoved () {
                super.wasRemoved();
                if (height >= TILE_SIZE / 16) {
                    bounceSprite(_sprite, height / 2);
                } else {
                    spriteStoppedBouncing(_sprite);
                }
                maybeComplete(penderId);
            }
        });
    }

    /**
     * Called when the sprite finishes its final bounce.
     */
    protected void spriteStoppedBouncing (Sprite sprite)
    {
        // fade dummy bonuses into the ground and remove them after they finish
        // bouncing
        if (sprite instanceof BonusSprite) {
            BonusSprite bsprite = (BonusSprite)sprite;
            if (bsprite.getPiece().pieceId == -1) {
                queueAction(bsprite, ActiveSprite.REMOVED);
                bsprite.addObserver(new ActiveSprite.ActionObserver() {
                    public void actionCompleted (Sprite sprite, String action) {
                        _view.removeSprite(sprite);
                    }
                });

            // force the sprite to reposition itself to ensure it's in the right spot
            } else {
                bsprite.resetLocation(_bangobj.board);
            }
        }
    }

    protected boolean isCompleted ()
    {
        return (!_applying && _penders.size() == 0);
    }

    protected void notePender (float duration)
    {
        final int penderId = notePender();
        new Interval(_ctx.getApp()) {
            public void expired () {
                maybeComplete(penderId);
            }
        }.schedule((long)(duration * 1000));
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
                log.info("Not completing " + this, "penders", _penders);
            }
        }
    }

    protected void displayParticles (String name, final Vector3f pos)
    {
        ParticlePool.getParticles(name,
            new ResultAttacher<Spatial>(_view.getPieceNode()) {
            public void requestCompleted (Spatial result) {
                super.requestCompleted(result);
                result.getLocalTranslation().set(pos);
            }
        });
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected int _pidx;
    protected BangBoardView _view;
    protected BangView _bview;
    protected short _tick;
    protected boolean _applying;

    protected SoundGroup _sounds;

    protected Effect _effect;

    protected ArrayIntSet _penders = new ArrayIntSet();
    protected int _nextPenderId;

    /** The last piece to drop a bonus. */
    protected Piece _dropper;

    /** The difference between the effect tick and the current tick which
     * causes fast versions of effects. */
    protected static final int FAST_TICK_DELTA = 2;

    /** The duration of particle effects activated on top of the camera. */
    protected static final float CAMERA_EFFECT_DURATION = 2f;

    /** The height from which to drop pieces onto the board. */
    protected static final float PIECE_DROP_HEIGHT = 150f;

    /** Used by {@link #getSoundPath}. */
    protected static final String[] SOUND_PREFIXES = {
        "rsrc/cards/", "rsrc/bonuses/", "rsrc/extras/", "rsrc/effects/"
    };

    /** Used by {@link #getSoundPath}. */
    protected static final String[] SOUND_SUFFIXES = {
        ".ogg", "/activate.ogg"
    };

    /** The backup place to look for a sound to play on a unit's landing, for
     * units that do not have a custom landing sound. */
    protected static final String DEFAULT_LAND_SOUND =
        "rsrc/extras/frontier_town/barricade/land.ogg";
}
