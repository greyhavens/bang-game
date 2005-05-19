//
// $Id$

package com.threerings.bang.client;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.jme.bui.event.MouseEvent;
import com.jme.bui.event.MouseListener;
import com.jme.bui.event.MouseMotionListener;
import com.jme.intersection.BoundingPickResults;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.scene.TriMesh;
import com.jmex.effects.ParticleManager;

import com.samskivert.util.StringUtil;

import com.threerings.media.util.MathUtil;
import com.threerings.util.RandomUtil;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Path;
import com.threerings.jme.sprite.PathObserver;
import com.threerings.jme.sprite.Sprite;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.client.sprite.MobileSprite;
import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.client.sprite.ShotSprite;
import com.threerings.bang.client.sprite.UnitSprite;
import com.threerings.bang.data.BangConfig;
import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.effect.Effect;
import com.threerings.bang.data.effect.ShotEffect;
import com.threerings.bang.data.piece.BigPiece;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.data.surprise.Surprise;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.PieceSet;
import com.threerings.bang.util.PointSet;
import com.threerings.bang.util.VisibilityState;

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
    }

    /**
     * Requests that the specified surprise be enabled for placement. The
     * area of effect of the surprise will be rendered around the cursor
     * and a left click will activate the surprise at the specified
     * coordinates while a right click will cancel the placement.
     */
    public void placeSurprise (Surprise s)
    {
        // clear any current selection
        clearSelection();

        // set up the display of our surprise attack set
        _surprise = s;
        _attackSet = new PointSet();
        _bangobj.board.computeAttacks(
            _surprise.getRadius(), _mouse.x, _mouse.y, _attackSet);
        targetTiles(_attackSet);
        log.info("Placing " + _surprise);
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
            // button 3 (right button) creates or extends a path
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
//             dirtyScreenRect(new Rectangle(0, 0, getWidth(), getHeight()));
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
//             dirtyScreenRect(new Rectangle(0, 0, getWidth(), getHeight()));
        }
    }

//     @Override // documentation inherited
//     protected void paintMouseTile (Graphics2D gfx, int mx, int my)
//     {
//         // only highlight the mouse coordinates while we're in play
//         if (_bangobj != null && _bangobj.isInPlay()) {
//             super.paintMouseTile(gfx, mx, my);
//         }
//     }

//     @Override // documentation inherited
//     protected void paintInFront (Graphics2D gfx, Rectangle dirtyRect)
//     {
//         super.paintInFront(gfx, dirtyRect);

//         // render our possible moves
//         if (_moveSet.size() > 0) {
//             renderSet(gfx, dirtyRect, _moveSet, _whiteRenderer);
//         }

//         // render the necessary tiles as dimmed if it is not "visible"
//         if (_board != null && _vstate != null) {
//             Composite ocomp = gfx.getComposite();
//             gfx.setComposite(SET_ALPHA);
//             gfx.setColor(Color.black);
//             _pr.setLocation(0, 0);
//             for (int yy = 0, hh = _board.getHeight(); yy < hh; yy++) {
//                 _pr.x = 0;
//                 int xoff = yy * _board.getWidth();
//                 for (int xx = 0, ww = _board.getWidth(); xx < ww; xx++) {
//                     if (!_vstate.getVisible(xoff+xx) &&
//                         dirtyRect.intersects(_pr)) {
//                         gfx.fill(_pr);
//                     }
//                     _pr.x += SQUARE;
//                 }
//                 _pr.y += SQUARE;
//             }
//             gfx.setComposite(ocomp);
//         }
//     }

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

        // if we are placing a surprise, activate it
        if (_surprise != null) {
//             log.info("activating " + _surprise);
            _ctrl.activateSurprise(_surprise.surpriseId, _mouse.x, _mouse.y);
            _surprise = null;
            clearAttackSet();
            return;
        }

        // select the piece under the mouse if it meets our various and
        // sundry conditions
        if (piece != null &&  sprite != null && piece.owner == _pidx &&
            sprite.isSelectable()) {
            selectPiece(piece);
            return;
        }

        // if we have a selection
        if (_selection != null) {
            // and we have an attack set
            if (_attackSet != null) {
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

            if (handleClickToMove(_mouse.x, _mouse.y)) {
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
        _attackSet = new PointSet();
        pruneAttackSet(attacks, _attackSet);

        // if there are no valid attacks, assume they're just moving (but
        // do nothing if they're not even moving)
        if (_attackSet.size() == 0 &&
            (_action[0] != _selection.x || _action[1] != _selection.y)) {
            executeAction();
            _attackSet = null;
        } else {
//             dirtySet(_attackSet);
        }
        return true;
    }

    /** Handles a click that indicates the coordinates of a piece we'd
     * like to attack. */
    protected boolean handleClickToAttack (Piece piece, int tx, int ty)
    {
        if (piece != null) {
            log.info("Clicking to attack " + piece.info());
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
                ((UnitSprite)getPieceSprite(p)).setTargeted(true);
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

        // if we are placing a surprise, clear it out
        if (_surprise != null) {
            log.info("Clearing " + _surprise);
            _surprise = null;
            clearAttackSet();
            return;
        }

        // if there is a piece under the cursor, show their possible shots
        PieceSprite sprite = null;
        if (_hover instanceof PieceSprite) {
            sprite = (PieceSprite)_hover;
            Piece piece = (Piece)_bangobj.pieces.get(sprite.getPieceId());
            if (sprite instanceof MobileSprite && piece.isAlive()) {
                clearSelection();
                PointSet moveSet = new PointSet();
                _attackSet = new PointSet();
                _bangobj.board.computeMoves(piece, moveSet, _attackSet);
                for (int ii = 0; ii < moveSet.size(); ii++) {
                    _attackSet.add(moveSet.get(ii));
                }
//                 dirtySet(_attackSet);
            }
        }
    }

    protected void selectPiece (Piece piece)
    {
        log.info("Selecting " + piece.info());
        boolean deselect = (piece == _selection);
        clearSelection();
        if (!deselect && piece.isAlive()) {
            _selection = piece;
            getPieceSprite(_selection).setSelected(true);
            PointSet attacks = new PointSet();
            _bangobj.board.computeMoves(piece, _moveSet, attacks);
            _attackSet = new PointSet();
            pruneAttackSet(_moveSet, _attackSet);
            pruneAttackSet(attacks, _attackSet);
            highlightTiles(_moveSet);
//             dirtySet(_moveSet);
//             dirtySet(_attackSet);
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
        if (_attackSet != null) {
            clearHighlights();
            _attackSet = null;
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
        // if we have an active surprise, update its area of effect
        if (_surprise != null) {
            clearHighlights();
            _attackSet.clear();
            _bangobj.board.computeAttacks(
                _surprise.getRadius(), tx, ty, _attackSet);
            targetTiles(_attackSet);
        }
    }

    @Override // documentation inherited
    protected void pieceUpdated (Piece opiece, Piece npiece)
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
                 ((_attackSet != null &&
                   _attackSet.contains(opiece.x, opiece.y)) ||
                  sel.getDistance(opiece) < sel.getMoveDistance())) ||
                (npiece != null &&
                 sel.getDistance(npiece) < sel.getMoveDistance())) {
                int[] oaction = _action;
                clearSelection();
                selectPiece(sel);
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
            new ShotHandler((ShotEffect)effect);
        } else {
            effect.apply(_bangobj, _effector);
        }
    }

    /** Adjusts the visibility settings for the tiles of the board. */
    protected void adjustBoardVisibility ()
    {
        // if we're out of the game, just reveal everything
        if (!_bangobj.hasLivePieces(_pidx)) {
            _vstate.reveal();
//             dirtyScreenRect(new Rectangle(0, 0, getWidth(), getHeight()));
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

//         // now dirty any tiles whose visibility changed
//         for (int yy = 0, ly = _board.getHeight(); yy < ly; yy++) {
//             for (int xx = 0, lx = _board.getHeight(); xx < lx; xx++) {
//                 if (_vstate.visibilityChanged(xx, yy)) {
//                     dirtyTile(xx, yy);
//                 }
//             }
//         }
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
        // currently just update the piece in question immediately
        Piece opiece = (Piece)_bangobj.pieces.get(piece.pieceId);
        pieceUpdated(opiece, piece);

        ParticleManager pmgr = null;
        if (effect.equals("bang")) {
            pmgr = ParticleFactory.getExplosion();
            pmgr.getParticles().setLocalScale(0.65f);
        } else if (effect.equals("howdy")) {
            pmgr = ParticleFactory.getSmallExplosion();
            pmgr.getParticles().setLocalScale(0.65f);
        } else if (effect.equals("repaired")) {
            pmgr = ParticleFactory.getGlow();
            // pmgr.getParticles().setLocalScale(0.65f);
        }

        if (pmgr != null) {
            PieceSprite sprite = getPieceSprite(piece);
            Vector3f spos = sprite.getLocalTranslation();
            pmgr.getParticles().setLocalTranslation(
                new Vector3f(spos.x + TILE_SIZE/2, spos.y + TILE_SIZE/2,
                             spos.z + TILE_SIZE/2));
            pmgr.forceRespawn();
            _pnode.attachChild(pmgr.getParticles());
            pmgr.getParticles().setForceView(true);
        }

//         // and create a simple label animation naming the effect
//         Label label = new Label(effect);
//         label.setTextColor(Color.white);
//         label.setAlternateColor(Color.black);
//         label.setStyle(Label.OUTLINE);
//         LabelSprite lsprite = new LabelSprite(label);
//         lsprite.setRenderOrder(100);
//         int px = piece.x * SQUARE, py = piece.y * SQUARE;
//         LinePath path = new LinePath(px, py, px, py - 25, 1000L);
//         addAnimation(new SpriteAnimation(_spritemgr, lsprite, path));
    }

    /** Waits for all sprites involved in a shot to stop moving and then
     * animates the fired shot. */
    protected class ShotHandler
        implements PathObserver
    {
        public ShotHandler (ShotEffect shot) {
            _shot = shot;
            _shooter = (Piece)_bangobj.pieces.get(shot.shooterId);
            if (_shooter == null) {
                log.warning("Missing shooter? [shot=" + shot + "].");
                // abandon ship, we're screwed
                return;
            }
            _target = (Piece)_bangobj.pieces.get(shot.targetId);
            if (_target == null) {
                log.warning("Missing target? [shot=" + shot + "].");
                // abandon ship, we're screwed
                return;
            }

            // figure out which sprites we need to wait for
            considerPiece(_shooter);
            considerPiece(_target);

            // if no one was managed, it's a shot fired from an invisible
            // piece at invisible pieces, ignore it
            if (_managed == 0) {
                log.info("Tree feel in the woods, no one was around.");

            } else if (_sprites == 0) {
                // if we're not waiting for any sprites to finish moving,
                // fire the shot immediately
                fireShot();
            }
        }

        public void pathCompleted (Sprite sprite, Path path) {
            sprite.removeObserver(this);
            if (sprite == _ssprite) {
                applyShot();
                removeSprite(sprite);
            } else if (--_sprites == 0) {
                fireShot();
            }
        }

        public void pathCancelled (Sprite sprite, Path path) {
            sprite.removeObserver(this);
            if (sprite == _ssprite) {
                applyShot();
                removeSprite(sprite);
            } else if (--_sprites == 0) {
                fireShot();
            }
        }

        protected void considerPiece (Piece piece) {
            PieceSprite sprite = null;
            if (piece != null) {
                sprite = _pieces.get(piece.pieceId);
            }
            if (sprite == null) {
                return;
            }
            if (isManaged(sprite)) {
                _managed++;
                if (sprite.isMoving()) {
                    sprite.addObserver(this);
                    _sprites++;
                }
            }
        }

        protected void fireShot ()
        {
            Vector3f start = new Vector3f(_shooter.x * TILE_SIZE + TILE_SIZE/2,
                                          _shooter.y * TILE_SIZE + TILE_SIZE/2,
                                          TILE_SIZE/2);
            Vector3f end = new Vector3f(_target.x * TILE_SIZE + TILE_SIZE/2,
                                        _target.y * TILE_SIZE + TILE_SIZE/2,
                                        TILE_SIZE/2);
            _ssprite = new ShotSprite();
            float duration = start.distance(end) / 30f;
            _ssprite.setLocalTranslation(start);
            _ssprite.addObserver(this);
            addSprite(_ssprite);
            _ssprite.move(new LinePath(_ssprite, start, end, duration));
        }

        protected void applyShot ()
        {
            // apply the shot
            _shot.apply(_bangobj, _effector);
        }

        protected ShotEffect _shot;
        protected ShotSprite _ssprite;
        protected Piece _shooter, _target;
        protected int _sprites, _managed;
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
    protected PointSet _attackSet;

    protected int _pidx;
    protected int _downButton = -1;

    protected int[] _action;
    protected Surprise _surprise;

    /** Tracks coordinate visibility. */
    protected VisibilityState _vstate;
}
