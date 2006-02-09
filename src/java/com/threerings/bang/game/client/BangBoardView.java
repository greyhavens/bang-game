//
// $Id$

package com.threerings.bang.game.client;

import java.awt.Point;
import java.awt.Rectangle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.event.MouseListener;

import com.samskivert.util.StringUtil;
import com.threerings.media.util.MathUtil;
import com.threerings.util.RandomUtil;

import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.SoundUtil;

import com.threerings.bang.game.client.sprite.BonusSprite;
import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.TutorialCodes;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.game.data.effect.Effect;
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
        _ctx = ctx;
        _ctrl = ctrl;
        addListener(this);
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
            0, _card.getRadius(), _mouse.x, _mouse.y, _attackSet);
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

    /**
     * Returns true if the specified piece is OK to be selected, false if it is
     * currently animating or has a pending animation.
     */
    public boolean isSelectable (Piece piece)
    {
        PieceSprite psprite;
        return !(pieceUpdatePending(piece.pieceId) ||
                 (psprite = getPieceSprite(piece)) == null ||
                 psprite.isMoving());
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
    public void prepareForRound (BangObject bangobj, BangConfig cfg, int pidx)
    {
        super.prepareForRound(bangobj, cfg, pidx);

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

        // clear out queued moves
        _queuedMoves.clear();
    }

    @Override // documentation inherited
    public void addSprite (Sprite sprite)
    {
        super.addSprite(sprite);
        if (sprite instanceof MobileSprite) {
            sprite.addObserver(_bonusClearer);
        }
    }

    @Override // documentation inherited
    public void removeSprite (Sprite sprite)
    {
        super.removeSprite(sprite);
        if (sprite instanceof MobileSprite) {
            sprite.removeObserver(_bonusClearer);
        }
    }

    @Override // documentation inherited
    protected TerrainNode createTerrainNode (BasicContext ctx)
    {
        // for now at least, terrain heightfields do not change in the course
        // of a game
        return new TerrainNode(ctx, this) {
            protected boolean isHeightfieldStatic () {
                return true;
            }
        };
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
        boolean oursAndMovable = (piece.owner == _pidx) && isSelectable(piece);
        if (_attackSet.size() > 0) {
            return _attackSet.contains(piece.x, piece.y) || oursAndMovable;
        }
        return oursAndMovable;
    }

    @Override // documentation inherited
        protected boolean pieceUpdated (
            BoardAction action, Piece opiece, Piece npiece, short tick)
    {
        boolean wait = super.pieceUpdated(action, opiece, npiece, tick);

        // update the shadow we use to do path finding and whatnot
        _bangobj.board.updateShadow(opiece, npiece);

        // if this piece was inside our attack set or within range to be inside
        // our move set, recompute the selection as it may have changed
        if (_selection != null) {
            Piece sel = _selection;
            if ((opiece != null &&
                 (_attackSet.contains(opiece.x, opiece.y) ||
                  sel.getDistance(opiece) < sel.getMoveDistance())) ||
                (npiece != null &&
                 sel.getDistance(npiece) < sel.getMoveDistance())) {
                int[] oaction = _action;
                clearSelection();
                selectUnit((Unit)sel, false);
                // if we had already selected a movement, reconfigure that
                // (it might no longer be valid but handleClickToMove will
                // ignore us in that case
                if (oaction != null) {
                    log.info("Reissuing click to move +" +
                             oaction[1] + "+" + oaction[2]);
                    handleClickToMove(oaction[1], oaction[2]);
                }
            }
        }

        // update board and enemy visibility
        if (_vstate != null) {
            adjustBoardVisibility();
            adjustEnemyVisibility();
        }

        // make sure all of our queued moves are still valid
        if (_queuedMoves.size() > 0) {
            PointSet moves = new PointSet();
            for (Iterator<Integer> iter = _queuedMoves.keySet().iterator();
                 iter.hasNext(); ) {
                Integer pieceId = iter.next();
                QueuedMove move = _queuedMoves.get(pieceId);
                Unit unit = (Unit)_bangobj.pieces.get(pieceId);
                if (unit == null) {
                    move.clear();
                    iter.remove();
                    continue;
                }
                _bangobj.board.computeMoves(unit, moves, null);

                // if no specific location was specified, make sure we can
                // still determine a location from which to fire
                if (move.mx == Short.MAX_VALUE) {
                    Piece target = (Piece)_bangobj.pieces.get(move.targetId);
                    if (target != null) {
                        Point spot = unit.computeShotLocation(target, moves);
                        if (spot != null) {
                            continue;
                        }
                    }

                // if a specific location was specified, make sure we can
                // still reach it
                } else if (moves.contains(move.mx, move.my)) {
                    continue;
                }

                // the move is no longer valid, so clear and remove it
                // TODO: play a sound?
                move.clear();
                iter.remove();
            }
        }

        return wait;
    }

    /** Handles a left mouse button click. */
    protected void handleLeftPress (int mx, int my)
    {
        // nothing doing if the game is not in play or we're not a player
        if (_bangobj == null || _bangobj.state != BangObject.IN_PLAY ||
            _pidx == -1) {
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

//         if (piece != null) {
//             log.info("Clicked " + piece.info());
//         } else {
//             log.info("Clicked +" + _high.x + "+" + _high.y);
//         }

        // if we are placing a card, activate it
        if (_card != null) {
            log.info("Activating " + _card);
            _ctrl.activateCard(_card.cardId, _mouse.x, _mouse.y);
            _card = null;
            clearAttackSet();
            return;
        }

        // select the piece under the mouse if it meets our sundry conditions
        if (piece != null &&  sprite != null && piece.owner == _pidx &&
            isSelectable(piece)) {
            selectUnit((Unit)piece, false);
            return;
        }

        // if we have a selection
        if (_selection != null) {
            // and we have an attack set
            if (_attackSet.size() > 0) {
                // if they clicked on a piece, use its coordinates, otherwise
                // use the coordinates of the highlight tile over which the
                // mouse is hovering
                int ax = _high.x, ay = _high.y;
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

        // reduce the highlighted move tiles to just the selected tile
        PointSet moves = new PointSet();
        moves.add(tx, ty);
        highlightTiles(moves, _selection.isFlyer());

        // display our potential attacks
        PointSet attacks = new PointSet();
        _bangobj.board.computeAttacks(_selection.getMinFireDistance(),
                                      _selection.getMaxFireDistance(),
                                      tx, ty, attacks);
        _attackSet.clear();
        pruneAttackSet(attacks, moves, _attackSet);

        // if there are no valid attacks, assume they're just moving (but
        // do nothing if they're not even moving)
        if (_attackSet.size() == 0 &&
            (_action[1] != _selection.x || _action[2] != _selection.y)) {
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
            // if we have not yet selected move coordinates, we'll let the
            // server figure it out for us when it processes our move
            if (_action == null) {
                _action = new int[] { _selection.pieceId,
                                      Short.MAX_VALUE, Short.MAX_VALUE, -1 };
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
                // TODO: prefer targets that are guaranteed to be there
                // when we make our move versus those that may be gone
                if (target != null && _selection.validTarget(target, false)) {
                    log.info("Randomly targeting " + target.info());
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
     * Scans all pieces that are in the specified range set and those to
     * which we can compute a valid firing location adds to the supplied
     * destination set, and marks their sprite as being targeted as well.
     */
    protected void pruneAttackSet (
        PointSet range, PointSet moves, PointSet dest)
    {
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p instanceof Unit && range.contains(p.x, p.y) &&
                _selection.validTarget(p, false) && 
                _selection.computeShotLocation(p, moves) != null) {
                getUnitSprite(p).setTargeted(true);
                dest.add(p.x, p.y);
            }
        }
    }

    /** Handles a right mouse button click. */
    protected void handleRightPress (int mx, int my)
    {
        // nothing doing if the game is not in play
        if (_bangobj == null || _bangobj.state == BangObject.IN_PLAY) {
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

    protected void selectUnit (Unit piece, boolean scrollCamera)
    {
        boolean deselect = (piece == _selection);
        clearSelection();
        if (deselect || !piece.isAlive()) {
            return;
        }
        _selection = piece;

        // select the sprite and center it in the view
        PieceSprite sprite = getPieceSprite(_selection);
        if (sprite == null) {
            return; // we might still be setting up
        }
        sprite.setSelected(true);
        if (scrollCamera) {
            ((GameInputHandler)_ctx.getInputHandler()).aimCamera(
                sprite.getWorldTranslation());
        }

        // highlight our potential moves and attackable pieces
        PointSet attacks = new PointSet();
        _bangobj.board.computeMoves(piece, _moveSet, attacks);
        _attackSet.clear();
        pruneAttackSet(_moveSet, _moveSet, _attackSet);
        pruneAttackSet(attacks, _moveSet, _attackSet);
        highlightTiles(_moveSet, piece.isFlyer());

        // report that the user took an action
        _ctrl.postEvent(TutorialCodes.UNIT_SELECTED);
    }

    protected void executeAction ()
    {
        // look up the unit we're "acting" with
        Unit actor = (Unit)_bangobj.pieces.get(_action[0]);
        if (actor == null) {
            log.warning("No actor? [action=" +
                        StringUtil.toString(_action) + "].");
            clearSelection();
            return;
        }

        // if this unit is not yet movable, "queue" up their action
        UnitSprite sprite = getUnitSprite(actor);
        if (sprite.getPiece().ticksUntilMovable(_bangobj.tick) > 0) {
            clearQueuedMove(_action[0]);
            _queuedMoves.put(_action[0], new QueuedMove(sprite, _action));
        } else {
            // otherwise enact the move/fire combination immediately
            _ctrl.moveAndFire(_action[0], _action[1], _action[2], _action[3]);
        }
        // and clear everything out
        clearSelection();
    }

    protected boolean hasQueuedMove (int pieceId)
    {
        return _queuedMoves.containsKey(pieceId);
    }

    protected void clearQueuedMove (int pieceId)
    {
        QueuedMove move = _queuedMoves.remove(pieceId);
        if (move != null) {
            move.clear();
        }
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
            _ctrl.postEvent(TutorialCodes.UNIT_DESELECTED);
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
                0, _card.getRadius(), tx, ty, _attackSet);
            targetTiles(_attackSet);
        }
    }

    @Override // documentation inherited
    protected PieceSprite removePieceSprite (int pieceId, String why)
    {
        PieceSprite sprite = _pieces.get(pieceId);

        // bonus sprites are not removed immediately; instead they are
        // shifted to a secondary table and they will be removed when the
        // piece that activated the bonus finally comes to rest on the
        // bonus piece's location
        if (sprite instanceof BonusSprite) {
            // and stick it into the pending bonuses table
            Piece piece = sprite.getPiece();
            _pendingBonuses.put(new Point(piece.x, piece.y), sprite);
            _pieces.remove(pieceId);
            return sprite;

        } else if (sprite instanceof MobileSprite) {
            MobileSprite msprite = (MobileSprite)sprite;
            // if this mobile sprite is animating it will have to wait
            // until it's finished before being removed
            if (msprite.isAnimating()) {
                _pieces.remove(pieceId);
                msprite.addObserver(_deadRemover);
                msprite.queueAction(MobileSprite.REMOVED);
                return sprite;
            }
        }

        return super.removePieceSprite(pieceId, why);
    }

    /**
     * Called every time the board ticks.
     */
    protected void ticked (short tick)
    {
        // fire off any queued moves
        for (Iterator<Map.Entry<Integer,QueuedMove>> iter =
                 _queuedMoves.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<Integer,QueuedMove> move = iter.next();
            Piece piece = (Piece)_bangobj.pieces.get(move.getKey());
            if (piece == null || !piece.isAlive()) {
                // our piece up and died, clear their queued action
                iter.remove();
                continue;
            }
            if (piece.ticksUntilMovable(tick) == 0) {
                QueuedMove qmove = move.getValue();
                _ctrl.moveAndFire(qmove.unitId, qmove.mx, qmove.my,
                                  qmove.targetId);
                qmove.clear();
                iter.remove();
            }
        }

        // update all of our units
        for (Iterator iter = _bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece piece = (Piece)iter.next();
            if (piece instanceof Unit) {
                queuePieceUpdate(null, piece);
            }
        }
    }

    /**
     * Called when an effect is applied to the board.
     */
    protected void applyEffect (Effect effect)
    {
        EffectHandler handler = effect.createHandler(_bangobj);
        handler.init(_ctx, _bangobj, this, _sounds, effect);
        executeAction(handler);
    }

    /** Adjusts the visibility settings for the tiles of the board. */
    protected void adjustBoardVisibility ()
    {
        // if we're out of the game, just reveal everything
        if (!_bangobj.hasLiveUnits(_pidx)) {
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
                    sprite.updated(piece, _bangobj.tick);
                    addSprite(sprite);
                } else if (!viz && isManaged(sprite)) {
                    removeSprite(sprite);
                }
            }
        }
    }

    protected class QueuedMove
    {
        public int unitId, mx, my, targetId;

        public QueuedMove (UnitSprite unit, int[] action)
        {
            unitId = action[0];
            mx = action[1];
            my = action[2];
            targetId = action[3];

            int x = mx, y = my;
            if (mx == Short.MAX_VALUE) {
                Piece target = (Piece)_bangobj.pieces.get(targetId);
                if (target == null) {
                    target = _unit.getPiece();
                }
                x = target.x;
                y = target.y;
            }

            _highlight = _tnode.createHighlight(x, y, true);
            _unit = unit;
            _unit.setPendingNode(_highlight);
            _highlight.updateRenderState();
            _pnode.attachChild(_highlight);
        }

        public void clear ()
        {
            _pnode.detachChild(_highlight);
            _unit.setPendingNode(null);
        }

        protected UnitSprite _unit;
        protected TerrainNode.Highlight _highlight;
    }

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

    /** Clears bonuses from tiles when units finally land on them. */
    protected PathObserver _bonusClearer = new PathObserver() {
        public void pathCancelled (Sprite sprite, Path path) {
            pathCompleted(sprite, path);
        }
        public void pathCompleted (Sprite sprite, Path path) {
            if (sprite instanceof MobileSprite) {
                Piece p = ((MobileSprite)sprite).getPiece();
                PieceSprite bsprite =
                    _pendingBonuses.remove(new Point(p.x, p.y));
                if (bsprite != null) {
                    removeSprite(bsprite);
                }
            }
        }
    };

    /** Used to remove unit sprites that have completed their death
     * animations. */
    protected MobileSprite.ActionObserver _deadRemover =
        new MobileSprite.ActionObserver() {
        public void actionCompleted (Sprite sprite, String action) {
            if (action.equals(MobileSprite.REMOVED)) {
                if (!((MobileSprite)sprite).isAnimating()) {
                    removeSprite(sprite);
                }
            }
        }
    };

    protected BangContext _ctx;
    protected BangController _ctrl;

    protected Piece _selection;

    protected PointSet _moveSet = new PointSet();
    protected PointSet _attackSet = new PointSet();

    protected int _pidx;
    protected int _downButton = -1;

    protected int[] _action;
    protected Card _card;

    protected HashMap<Integer,QueuedMove> _queuedMoves =
        new HashMap<Integer,QueuedMove>();

    /** Tracks coordinate visibility. */
    protected VisibilityState _vstate;

    /** Contains bonus sprites that are pending removal. */
    protected HashMap<Point,PieceSprite> _pendingBonuses =
        new HashMap<Point,PieceSprite>();

    /** The color of the queued movement highlights. */
    protected static final ColorRGBA QMOVE_HIGHLIGHT_COLOR =
        new ColorRGBA(1f, 0.5f, 0.5f, 0.5f);
}
