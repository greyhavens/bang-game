//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import java.util.Iterator;

import com.jme.bui.event.MouseEvent;
import com.jme.bui.event.MouseListener;
import com.jme.input.KeyInput;

import com.threerings.media.util.MathUtil;
import com.threerings.util.RandomUtil;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.client.util.EscapeListener;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.client.effect.EffectViz;
import com.threerings.bang.game.client.effect.ExplosionViz;
import com.threerings.bang.game.client.effect.RepairViz;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.game.util.VisibilityState;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays the main game board.
 */
public class BangBoardView extends BoardView
    implements MouseListener
{
    public BangBoardView (BangContext ctx, BangController ctrl)
    {
        super(ctx);
        _ctrl = ctrl;
        addListener(this);

        addListener(new EscapeListener() {
            public void keyPressed (int keyCode) {
                switch (keyCode) {
                case KeyInput.KEY_SPACE:
                    _ctrl.startChat();
                    break;
                default:
                    super.keyPressed(keyCode);
                }
            }
            public void escapePressed () {
                InGameOptionsView oview = new InGameOptionsView(_ctx);
                _ctx.getInputDispatcher().addWindow(oview);
                oview.pack();
                oview.center();
            }
        });
    }

    /**
     * Requests that the specified card be enabled for placement. The area
     * of effect of the card will be rendered around the cursor and a left
     * click will activate the card at the specified coordinates while a
     * right click will cancel the placement.
     */
    public void placeCard (Card card)
    {
        // clear any current selection
        clearSelection();

        // set up the display of our card attack set
        _card = card;
        _attackSet.clear();
        _bangobj.board.computeAttacks(
            _card.getRadius(), _mouse.x, _mouse.y, _attackSet);
        targetTiles(_attackSet);
        log.info("Placing " + _card);
    }

    /**
     * Called by the controller if we requested to take a shot at another
     * piece but it was rejected because the piece moved or something else
     * prevented it.
     */
    public void shotFailed (int targetId)
    {
        // for now just clear the target indicator but perhaps display
        // something fancier in the future
        Piece piece = (Piece)_bangobj.pieces.get(targetId);
        if (piece != null) {
            getUnitSprite(piece).setPendingShot(false);
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseClicked (MouseEvent e)
    {
        // nothing doing, we handle this ourselves
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent e)
    {
        switch (_downButton = e.getButton()) {
        case MouseEvent.BUTTON2:
            handleRightPress(e.getX(), e.getY());
            break;

        case MouseEvent.BUTTON1:
            handleLeftPress(e.getX(), e.getY());
            break;
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (MouseEvent e)
    {
        if (_downButton == MouseEvent.BUTTON3) {
            clearAttackSet();
        }
        _downButton = -1;
    }

    // documentation inherited from interface MouseListener
    public void mouseEntered (MouseEvent e)
    {
        // nada
    }

    // documentation inherited from interface MouseListener
    public void mouseExited (MouseEvent e)
    {
        // nada
    }

    @Override // documentation inherited
    public void startGame (BangObject bangobj, BangConfig cfg, int pidx)
    {
        super.startGame(bangobj, cfg, pidx);

        _pidx = pidx;
        _bangobj.addListener(_ticker);

        // set up the starting visibility if we're using it
        if (cfg.fog) {
            _vstate = new VisibilityState(_bbounds.width, _bbounds.height);
            adjustBoardVisibility();
            adjustEnemyVisibility();
        }
    }

    @Override // documentation inherited
    public void pieceUpdated (Piece opiece, Piece npiece)
    {
        super.pieceUpdated(opiece, npiece);

        // update the shadow we use to do path finding and whatnot
        _bangobj.board.updateShadow(opiece, npiece);

        // if this piece was inside our attack set or within range to be
        // inside our move set, recompute the selection as it may have
        // changed
        if (_selection != null) {
            Piece sel = _selection;
            if ((opiece != null &&
                 (_attackSet.contains(opiece.x, opiece.y) ||
                  sel.getDistance(opiece) < sel.getMoveDistance())) ||
                (npiece != null &&
                 sel.getDistance(npiece) < sel.getMoveDistance())) {
                int[] oaction = _action;
                clearSelection();
                selectPiece((Unit)sel);
                // if we had already selected a movement, reconfigure that
                // (it might no longer be valid but handleClickToMove will
                // ignore us in that case
                if (oaction != null) {
                    handleClickToMove(oaction[0], oaction[1]);
                }
            }
        }

        // update board and enemy visibility
        if (_vstate != null) {
            adjustBoardVisibility();
            adjustEnemyVisibility();
        }
    }

    @Override // documentation inherited
    public void endRound ()
    {
        super.endRound();
        clearSelection();

        // remove our event listener
        _bangobj.removeListener(_ticker);

        // allow everything to be visible
        if (_vstate != null) {
            _vstate.reveal();
            adjustEnemyVisibility();
        }
    }

    @Override // documentation inherited
    public void endGame ()
    {
        super.endGame();
        clearSelection();

        // remove our event listener
        _bangobj.removeListener(_ticker);

        // allow everything to be visible
        if (_vstate != null) {
            _vstate.reveal();
            adjustEnemyVisibility();
        }
    }

    @Override // documentation inherited
    protected boolean isHoverable (Sprite sprite)
    {
        if (!super.isHoverable(sprite)) {
            return false;
        }
        if (!(sprite instanceof UnitSprite)) {
            return false;
        }
        UnitSprite usprite = (UnitSprite)sprite;
        Piece piece = usprite.getPiece();
        if (!piece.isAlive()) {
            return false;
        }
        boolean oursAndMovable =
            (piece.owner == _pidx) && usprite.isSelectable();
        if (_attackSet.size() > 0) {
            return _attackSet.contains(piece.x, piece.y) || oursAndMovable;
        }
        return oursAndMovable;
    }

    /** Handles a left mouse button click. */
    protected void handleLeftPress (int mx, int my)
    {
        // nothing doing if the game is not in play or we're not a player
        if (_bangobj == null || !_bangobj.isInPlay() || _pidx == -1) {
            return;
        }

        // check for a piece under the mouse
        PieceSprite sprite = null;
        Piece piece = null;
        if (_hover instanceof PieceSprite) {
            sprite = (PieceSprite)_hover;
            piece = (Piece)_bangobj.pieces.get(sprite.getPieceId());
            // we currently don't do anything with non-player pieces
            if (piece != null && piece.owner == -1) {
                sprite = null;
                piece = null;
            }
        }

        if (piece != null) {
            log.info("Clicked " + piece.info());
        }

        // if we are placing a card, activate it
        if (_card != null) {
            log.info("Activating " + _card);
            _ctrl.activateCard(_card.cardId, _mouse.x, _mouse.y);
            _card = null;
            clearAttackSet();
            return;
        }

        // select the piece under the mouse if it meets our various and
        // sundry conditions
        if (piece != null &&  sprite != null && piece.owner == _pidx &&
            sprite.isSelectable()) {
            selectPiece((Unit)piece);
            return;
        }

        // if we have a selection
        if (_selection != null) {
            // and we have an attack set
            if (_attackSet.size() > 0) {
                // if they clicked on a piece, use its coordinates,
                // otherwise use the coordinates over which the mouse is
                // hovering
                int ax = _mouse.x, ay = _mouse.y;
                if (piece != null) {
                    ax = piece.x;
                    ay = piece.y;
                }
                if (handleClickToAttack(piece, ax, ay)) {
                    return;
                }
            }

            if (handleClickToMove(_high.x, _high.y)) {
                return;
            }
        }
    }

    /** Handles a click that should select a potential move location. */
    protected boolean handleClickToMove (int tx, int ty)
    {
        // or if we're clicking in our move set or on our selected piece
        if (!_moveSet.contains(tx, ty) &&
            (_selection == null || _selection.x != tx || _selection.y != ty)) {
            log.info("Rejecting move +" + tx + "+" + ty + " moves:" + _moveSet);
            return false;
        }

        // store the coordinates toward which we wish to move
        _action = new int[] { _selection.pieceId, tx, ty, -1 };

        // clear any previous attack set
        clearAttackSet();

        // display our potential attacks
        PointSet attacks = new PointSet();
        _bangobj.board.computeAttacks(
            _selection.getFireDistance(), tx, ty, attacks);
        _attackSet.clear();
        pruneAttackSet(attacks, _attackSet);

        // if there are no valid attacks, assume they're just moving (but
        // do nothing if they're not even moving)
        if (_attackSet.size() == 0 &&
            (_action[0] != _selection.x || _action[1] != _selection.y)) {
            executeAction();
            _attackSet.clear();
        } else {
            log.info("Waiting for attack selection (" + tx + ", " + ty + ")");
        }
        return true;
    }

    /** Handles a click that indicates the coordinates of a piece we'd
     * like to attack. */
    protected boolean handleClickToAttack (Piece piece, int tx, int ty)
    {
        if (piece != null) {
            log.info("Clicking to attack " + piece.info());
        } else {
            log.info("Clicking to attack +" + tx + "+" + ty);
        }

        // maybe we're clicking on a piece that is in our attack set
        if (_attackSet.contains(tx, ty) &&
            piece != null && piece.owner != _pidx) {
            // if we have not yet selected move coordinates, reverse
            // engineer those from the piece we would like to attack
            if (_action == null) {
                // locate the position in our move set that has the
                // smallest move distance but is still within attack range
                Point spot = _selection.computeShotLocation(piece, _moveSet);
                if (spot == null) {
                    log.warning("Unable to find place from which to shoot? " +
                                "[piece=" + _selection.info() +
                                ", target=" + piece.info() +
                                ", moveSet=" + _moveSet + "].");
                    return true;
                }
                _action = new int[] { _selection.pieceId, spot.x, spot.y, -1 };
            }
            // note the piece we desire to fire upon
            _action[3] = piece.pieceId;
            getUnitSprite(piece).setPendingShot(true);
            executeAction();
            return true;
        }

        // maybe we're clicking a second time on our desired move location
        if (_action != null && tx == _action[1] && ty == _action[2]) {
            if (_attackSet.size() > 0) {
                // select a random target and request to shoot it
                int idx = RandomUtil.getInt(_attackSet.size());
                Piece target = _bangobj.getPlayerPiece(
                    _attackSet.getX(idx), _attackSet.getY(idx));
                if (target != null && _selection.validTarget(target)) {
                    log.info("randomly targeting " + target.info());
                    _action[3] = target.pieceId;
                    getUnitSprite(target).setPendingShot(true);
                }
            }
            executeAction();
            return true;
        }

        return false;
    }

    /**
     * Adds all positions in the source set that reference a valid attack
     * target to the supplied destination set.
     */
    protected void pruneAttackSet (PointSet source, PointSet dest)
    {
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (_selection.validTarget(p) && source.contains(p.x, p.y)) {
                getUnitSprite(p).setTargeted(true);
                dest.add(p.x, p.y);
            }
        }
    }

    /** Handles a right mouse button click. */
    protected void handleRightPress (int mx, int my)
    {
        // nothing doing if the game is not in play
        if (_bangobj == null || !_bangobj.isInPlay()) {
            return;
        }

        // if we are placing a card, clear it out
        if (_card != null) {
            log.info("Clearing " + _card);
            _card = null;
            clearAttackSet();
            return;
        }

        // if there is a piece under the cursor, show their possible shots
        PieceSprite sprite = null;
        if (_hover instanceof PieceSprite) {
            sprite = (PieceSprite)_hover;
            Piece piece = (Piece)_bangobj.pieces.get(sprite.getPieceId());
            if (sprite instanceof UnitSprite && piece.isAlive()) {
                clearSelection();
                PointSet moveSet = new PointSet();
                _attackSet.clear();
                _bangobj.board.computeMoves(piece, moveSet, _attackSet);
                for (int ii = 0; ii < moveSet.size(); ii++) {
                    _attackSet.add(moveSet.get(ii));
                }
            }
        }
    }

    /**
     * Convenience method for getting the sprite for a piece we know to be
     * a unit.
     */
    protected UnitSprite getUnitSprite (Piece piece)
    {
        return (UnitSprite)getPieceSprite(piece);
    }

    protected void selectPiece (Unit piece)
    {
        log.info("Selecting " + piece.info());
        boolean deselect = (piece == _selection);
        clearSelection();
        if (!deselect && piece.isAlive()) {
            _selection = piece;
            getPieceSprite(_selection).setSelected(true);
            PointSet attacks = new PointSet();
            _bangobj.board.computeMoves(piece, _moveSet, attacks);
            _attackSet.clear();
            pruneAttackSet(_moveSet, _attackSet);
            pruneAttackSet(attacks, _attackSet);
            highlightTiles(_moveSet, piece.isFlyer());
        }
    }

    protected void executeAction ()
    {
        // enact the move/fire combination
        _ctrl.moveAndFire(_action[0], _action[1], _action[2], _action[3]);
        // and clear everything out
        clearSelection();
    }

    protected void clearMoveSet ()
    {
        clearHighlights();
        _moveSet.clear();
    }

    protected void clearAttackSet ()
    {
        if (_attackSet.size() > 0) {
            clearHighlights();
            _attackSet.clear();
        }
        for (PieceSprite s : _pieces.values()) {
            if (s instanceof UnitSprite) {
                ((UnitSprite)s).setTargeted(false);
            }
        }
    }

    protected void clearSelection ()
    {
        if (_selection != null) {
            getPieceSprite(_selection).setSelected(false);
            _selection = null;
        }
        clearMoveSet();

        // clear out any pending action
        _action = null;
        clearAttackSet();
    }

    @Override // documentation inherited
    protected void hoverTileChanged (int tx, int ty)
    {
        // if we have an active card, update its area of effect
        if (_card != null) {
            clearHighlights();
            _attackSet.clear();
            _bangobj.board.computeAttacks(
                _card.getRadius(), tx, ty, _attackSet);
            targetTiles(_attackSet);
        }
    }

    /**
     * Called every time the board ticks.
     */
    protected void ticked (short tick)
    {
        // allow pieces to tick down and possibly die
        Piece[] pieces = _bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p.isAlive() && p.tick(tick)) {
                // if they died, possibly remove them from the board
                if (!p.isAlive() && p.removeWhenDead()) {
                    _bangobj.removePieceDirect(p);
                    removePieceSprite(p.pieceId);
                }
            }
        }

        // update all of our piece sprites
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            PieceSprite sprite = _pieces.get(p.pieceId);
            if (sprite == null) {
                continue;
            }
            sprite.updated(_bangobj.board, p, tick);
        }
    }

    /**
     * Called when an effect is applied to the board.
     */
    protected void applyEffect (Effect effect)
    {
        if (effect instanceof ShotEffect) {
            ShotEffect seffect = (ShotEffect)effect;
            Unit shooter = (Unit)_bangobj.pieces.get(seffect.shooterId);
            ShotHandler handler;
            if (shooter.getConfig().mode == UnitConfig.Mode.RANGE) {
                handler = new BallisticShotHandler();
            } else {
                handler = new InstantShotHandler();
            }
            handler.init(_ctx, _bangobj, this, seffect);

        } else {
            effect.apply(_bangobj, _effector);
        }
    }

    protected void applyShot (ShotEffect shot)
    {
        // apply the shot
        shot.apply(_bangobj, _effector);
    }

    /** Adjusts the visibility settings for the tiles of the board. */
    protected void adjustBoardVisibility ()
    {
        // if we're out of the game, just reveal everything
        if (!_bangobj.hasLivePieces(_pidx)) {
            _vstate.reveal();
            return;
        }

        // swap our visibility state to the fresh set
        _vstate.swap();

        // update the board visibility based on our piece's new position
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece.owner == -1 || (_pidx != -1 && _pidx != piece.owner)) {
                continue; // skip non-player pieces in this pass
            }

            int dist = piece.getSightDistance(), dist2 = dist * dist;
            Rectangle rect = new Rectangle(
                piece.x - dist, piece.y - dist, 2*dist+1, 2*dist+1);
            rect = rect.intersection(
                new Rectangle(0, 0, _board.getWidth(), _board.getHeight()));
            for (int yy = rect.y, ly = yy + rect.height; yy < ly; yy++) {
                for (int xx = rect.x, lx = xx + rect.width; xx < lx; xx++) {
                    int tdist = MathUtil.distanceSq(xx, yy, piece.x, piece.y);
                    if (tdist < dist2) {
                        _vstate.setVisible(xx, yy);
                    }
                }
            }
        }
    }

    /** Makes enemy pieces visible or invisible based on _vstate. */
    protected void adjustEnemyVisibility ()
    {
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece.owner == -1 || (_pidx == -1 || _pidx == piece.owner)) {
                continue; // skip unowned and player pieces in this pass
            }

            PieceSprite sprite = _pieces.get(piece.pieceId);
            if (sprite != null) {
                boolean viz = _vstate.getVisible(piece.x, piece.y);
                if (viz && !isManaged(sprite)) {
                    sprite.updated(_bangobj.board, piece, _bangobj.tick);
                    addSprite(sprite);
                } else if (!viz && isManaged(sprite)) {
                    removeSprite(sprite);
                }
            }
        }
    }

    /** Called to display something useful when an effect is applied. */
    protected void createEffectAnimation (Piece piece, String effect)
    {
        // create the appropriate effect
        EffectViz viz = null;
        if (effect.equals("bang")) {
            viz = new ExplosionViz(false);
        } else if (effect.equals("howdy")) {
            viz = new ExplosionViz(true);
        } else if (effect.equals("repaired")) {
            viz = new RepairViz();
        }

        // queue the effect up on the piece sprite
        if (viz != null) {
            Piece opiece = (Piece)_bangobj.pieces.get(piece.pieceId);
            viz.init(_ctx, this, opiece, piece);
            PieceSprite sprite = getPieceSprite(piece);
            sprite.queueEffect(viz);

            // if they just got shot, clear any pending shot
            if (effect.equals("bang")) {
                ((UnitSprite)sprite).setPendingShot(false);
            }
        }
    }

    /** Used to remove shot sprites when they reach their target. */
    protected PathObserver _remover = new PathObserver() {
        public void pathCompleted (Sprite sprite, Path path) {
            removeSprite(sprite);
        }
        public void pathCancelled (Sprite sprite, Path path) {
            removeSprite(sprite);
        }
    };

    /** Handles the results of effects. */
    protected Effect.Observer _effector = new Effect.Observer() {
        public void pieceAdded (Piece piece) {
            // this will create and add the sprite to the board
            getPieceSprite(piece);
        }

        public void pieceAffected (Piece piece, String effect) {
            createEffectAnimation(piece, effect);
        }

        public void pieceRemoved (Piece piece) {
            removePieceSprite(piece.pieceId);
        }
    };

    /** Listens for ticks and effects and does the right thing. */
    protected AttributeChangeListener _ticker = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(BangObject.TICK)) {
                ticked(_bangobj.tick);
            } else if (event.getName().equals(BangObject.EFFECT)) {
                applyEffect((Effect)event.getValue());
            }
        }
    };

    protected BangController _ctrl;

    protected Piece _selection;

    protected PointSet _moveSet = new PointSet();
    protected PointSet _attackSet = new PointSet();

    protected int _pidx;
    protected int _downButton = -1;

    protected int[] _action;
    protected Card _card;

    /** Tracks coordinate visibility. */
    protected VisibilityState _vstate;
}
